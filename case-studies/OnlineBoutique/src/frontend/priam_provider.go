// PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2). Mounted on
// bare /api (registered in main.go without baseUrl's prefix - the Gateway
// strips only "/provider" then forwards straight to CUSTOM_PROVIDER_URL),
// no auth - called only machine-to-machine by PRIAM-Right-service/
// PRIAM-Frontend-Provider.
//
// Only one DataType exists for this application ("Cart" - see
// Databases/db_insertion_script.sql's header comment for why: no
// sign-up/login, and checkout PII is never persisted anywhere). Reads/
// writes go straight at the same Redis-backed store cartservice itself uses
// (cartservice/src/cartstore/RedisCartStore.cs), not through cartservice's
// gRPC surface: AddItem only increments a quantity and EmptyCart wipes the
// whole cart, neither can set an exact quantity or erase a single product
// row while leaving the rest of the cart intact - both genuinely required
// for rectification/erasure (§8.1.c, a composite/primary-key scenario).
// Confirmed empirically against the real redis-cart container (see
// priam-integration/ETAPES-FAITES.md) that RedisCartStore's underlying
// Microsoft.Extensions.Caching.StackExchangeRedis stores the serialized
// Cart protobuf in a Redis HASH under field "data" - the same wire format
// cartservice itself reads/writes, so this bridge never invents a schema,
// it reuses the real one.
package main

import (
	"context"
	"encoding/json"
	"net/http"
	"os"
	"strconv"

	pb "github.com/GoogleCloudPlatform/microservices-demo/src/frontend/genproto"
	"github.com/redis/go-redis/v9"
	"google.golang.org/protobuf/proto"
)

const cartDataTypeName = "Cart"

// cartAttributes whitelists both the GET dataAccessRight query param and the
// dataName of write requests (playbook §2 - "restricted to a whitelist of
// fields allowed for a given dataTypeName, validated on the target-
// application side").
var cartAttributes = map[string]bool{"product_id": true, "quantity": true}

var priamRedisClient = newPriamRedisClient()

func newPriamRedisClient() *redis.Client {
	addr := os.Getenv("REDIS_ADDR")
	if addr == "" {
		return nil
	}
	return redis.NewClient(&redis.Options{Addr: addr})
}

func priamReadCart(ctx context.Context, idRef string) (*pb.Cart, error) {
	cart := &pb.Cart{UserId: idRef}
	raw, err := priamRedisClient.HGet(ctx, idRef, "data").Bytes()
	if err == redis.Nil {
		return cart, nil
	}
	if err != nil {
		return nil, err
	}
	if err := proto.Unmarshal(raw, cart); err != nil {
		return nil, err
	}
	return cart, nil
}

func priamWriteCart(ctx context.Context, idRef string, cart *pb.Cart) error {
	raw, err := proto.Marshal(cart)
	if err != nil {
		return err
	}
	return priamRedisClient.HSet(ctx, idRef, "data", raw).Err()
}

func priamFindCartItem(cart *pb.Cart, productID string) *pb.CartItem {
	for _, item := range cart.GetItems() {
		if item.GetProductId() == productID {
			return item
		}
	}
	return nil
}

func priamProviderJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(body)
}

// GET /api/dataAccessRight?idRef=...&dataTypeName=Cart&attributes=product_id,quantity
// Always answers with a JSON array (§2), one element per cart item held by
// idRef.
func priamDataAccessRightHandler(w http.ResponseWriter, r *http.Request) {
	idRef := r.URL.Query().Get("idRef")
	dataTypeName := r.URL.Query().Get("dataTypeName")
	attrs := priamParseAttributes(r.URL.Query().Get("attributes"))

	if idRef == "" || dataTypeName != cartDataTypeName || priamRedisClient == nil {
		priamProviderJSON(w, http.StatusOK, []map[string]string{})
		return
	}
	cart, err := priamReadCart(r.Context(), idRef)
	if err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	out := make([]map[string]string, 0, len(cart.GetItems()))
	for _, item := range cart.GetItems() {
		out = append(out, priamPickAttributes(item, attrs))
	}
	priamProviderJSON(w, http.StatusOK, out)
}

type priamWriteRequest struct {
	IdRef        string            `json:"idRef"`
	DataTypeName string            `json:"dataTypeName"`
	DataName     string            `json:"dataName"`
	NewValue     string            `json:"newValue"`
	PrimaryKeys  map[string]string `json:"primaryKeys"`
}

// POST /api/rectification  body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
func priamRectificationHandler(w http.ResponseWriter, r *http.Request) {
	var req priamWriteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.DataTypeName != cartDataTypeName || !cartAttributes[req.DataName] {
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown or non-writable field"})
		return
	}
	if priamRedisClient == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	cart, err := priamReadCart(r.Context(), req.IdRef)
	if err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	item := priamFindCartItem(cart, req.PrimaryKeys["product_id"])
	if item == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	switch req.DataName {
	case "quantity":
		qty, err := strconv.Atoi(req.NewValue)
		if err != nil {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "quantity must be an integer"})
			return
		}
		item.Quantity = int32(qty)
	case "product_id":
		item.ProductId = req.NewValue
	}
	if err := priamWriteCart(r.Context(), req.IdRef, cart); err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	priamProviderJSON(w, http.StatusOK, map[string]bool{"success": true})
}

// POST /api/erasure  body: {idRef, dataTypeName, dataName, primaryKeys}
// Erasing either column of a Cart row removes that row entirely - there is
// no sensible "blank" value for product_id/quantity that would leave a
// meaningful cart entry behind.
func priamErasureHandler(w http.ResponseWriter, r *http.Request) {
	var req priamWriteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || req.DataTypeName != cartDataTypeName || !cartAttributes[req.DataName] {
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown or non-writable field"})
		return
	}
	if priamRedisClient == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	cart, err := priamReadCart(r.Context(), req.IdRef)
	if err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	productID := req.PrimaryKeys["product_id"]
	kept := cart.GetItems()[:0]
	found := false
	for _, item := range cart.GetItems() {
		if item.GetProductId() == productID {
			found = true
			continue
		}
		kept = append(kept, item)
	}
	if !found {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	cart.Items = kept
	if err := priamWriteCart(r.Context(), req.IdRef, cart); err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	priamProviderJSON(w, http.StatusOK, map[string]bool{"success": true})
}

type priamDataValueRequest struct {
	IdRef       string            `json:"idRef"`
	DataName    string            `json:"dataName"`
	PrimaryKeys map[string]string `json:"primaryKeys"`
}

// POST /api/dataValue  body: {idRef, dataName, primaryKeys} - no
// dataTypeName (§2/§8.2.f): only one DataType exists here, so dataName alone
// is enough to know it's a Cart field.
func priamDataValueHandler(w http.ResponseWriter, r *http.Request) {
	var req priamDataValueRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil || !cartAttributes[req.DataName] || priamRedisClient == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	cart, err := priamReadCart(r.Context(), req.IdRef)
	if err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	item := priamFindCartItem(cart, req.PrimaryKeys["product_id"])
	if item == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	var value string
	if req.DataName == "quantity" {
		value = strconv.Itoa(int(item.GetQuantity()))
	} else {
		value = item.GetProductId()
	}
	priamProviderJSON(w, http.StatusOK, map[string]string{"value": value})
}

func priamParseAttributes(raw string) map[string]bool {
	out := map[string]bool{}
	start := 0
	for i := 0; i <= len(raw); i++ {
		if i == len(raw) || raw[i] == ',' {
			if f := raw[start:i]; f != "" {
				out[f] = true
			}
			start = i + 1
		}
	}
	return out
}

func priamPickAttributes(item *pb.CartItem, attrs map[string]bool) map[string]string {
	full := map[string]string{
		"product_id": item.GetProductId(),
		"quantity":   strconv.Itoa(int(item.GetQuantity())),
	}
	if len(attrs) == 0 {
		return full
	}
	out := map[string]string{}
	for k := range attrs {
		if v, ok := full[k]; ok {
			out[k] = v
		}
	}
	return out
}
