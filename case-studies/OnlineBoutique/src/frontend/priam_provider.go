// PRIAM Provider bridge (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §2). Mounted on
// bare /api (registered in main.go without baseUrl's prefix - the Gateway
// strips only "/provider" then forwards straight to CUSTOM_PROVIDER_URL),
// no auth - called only machine-to-machine by PRIAM-Right-service/
// PRIAM-Frontend-Provider.
//
// Two DataTypes exist (Databases/db_insertion_script.sql): "User" (one row
// per subject - the account itself, store.go's `users` table) and "Order"
// (several rows per subject, `orders` table - `order_id` is the annotated
// primary key, §8.1.c). Both are read/written directly against the same
// SQLite store the rest of this application already uses (store.go) - the
// account/order code already exposes exactly the SQL this bridge needs, no
// separate access path required.
package main

import (
	"database/sql"
	"encoding/json"
	"net/http"
	"strconv"
)

const (
	userDataTypeName  = "User"
	orderDataTypeName = "Order"
)

// Whitelists both the GET dataAccessRight query param and the dataName of
// write requests (playbook §2 - "restricted to a whitelist of fields
// allowed for a given dataTypeName, validated on the target-application
// side").
var (
	userAttributes  = map[string]bool{"email": true}
	orderAttributes = map[string]bool{"order_id": true, "email": true, "street_address": true, "city": true, "state": true, "zip_code": true, "country": true}
)

func priamProviderJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	json.NewEncoder(w).Encode(body)
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

func priamFilterAttrs(full map[string]string, attrs map[string]bool) map[string]string {
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

// priamGetUser reads the single User row for idRef, or nil if it doesn't exist.
func priamGetUser(idRef string) (map[string]string, error) {
	var email string
	err := db.QueryRow(`SELECT email FROM users WHERE id = ?`, idRef).Scan(&email)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return map[string]string{"email": email}, nil
}

// priamListOrders reads every Order row belonging to idRef.
func priamListOrders(idRef string) ([]map[string]string, error) {
	rows, err := db.Query(`SELECT order_id, email, street_address, city, state, zip_code, country
		FROM orders WHERE user_id = ? ORDER BY order_id`, idRef)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var out []map[string]string
	for rows.Next() {
		var orderID, email, street, city, state, country string
		var zip int
		if err := rows.Scan(&orderID, &email, &street, &city, &state, &zip, &country); err != nil {
			return nil, err
		}
		out = append(out, map[string]string{
			"order_id": orderID, "email": email, "street_address": street,
			"city": city, "state": state, "zip_code": strconv.Itoa(zip), "country": country,
		})
	}
	return out, rows.Err()
}

// priamGetOrder reads one Order row, scoped to idRef so a request for one
// subject's order can never touch another subject's data.
func priamGetOrder(idRef, orderID string) (map[string]string, error) {
	var email, street, city, state, country string
	var zip int
	err := db.QueryRow(`SELECT email, street_address, city, state, zip_code, country
		FROM orders WHERE order_id = ? AND user_id = ?`, orderID, idRef).
		Scan(&email, &street, &city, &state, &zip, &country)
	if err == sql.ErrNoRows {
		return nil, nil
	}
	if err != nil {
		return nil, err
	}
	return map[string]string{
		"order_id": orderID, "email": email, "street_address": street,
		"city": city, "state": state, "zip_code": strconv.Itoa(zip), "country": country,
	}, nil
}

// GET /api/dataAccessRight?idRef=...&dataTypeName=User|Order&attributes=a,b,c
// Always answers with a JSON array (§2), even for the single-row User type.
func priamDataAccessRightHandler(w http.ResponseWriter, r *http.Request) {
	idRef := r.URL.Query().Get("idRef")
	dataTypeName := r.URL.Query().Get("dataTypeName")
	attrs := priamParseAttributes(r.URL.Query().Get("attributes"))

	switch dataTypeName {
	case userDataTypeName:
		user, err := priamGetUser(idRef)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if user == nil {
			priamProviderJSON(w, http.StatusOK, []map[string]string{})
			return
		}
		priamProviderJSON(w, http.StatusOK, []map[string]string{priamFilterAttrs(user, attrs)})
	case orderDataTypeName:
		orders, err := priamListOrders(idRef)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		out := make([]map[string]string, 0, len(orders))
		for _, o := range orders {
			out = append(out, priamFilterAttrs(o, attrs))
		}
		priamProviderJSON(w, http.StatusOK, out)
	default:
		priamProviderJSON(w, http.StatusOK, []map[string]string{})
	}
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
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid request body"})
		return
	}

	switch req.DataTypeName {
	case userDataTypeName:
		if !userAttributes[req.DataName] {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown or non-writable field"})
			return
		}
		var conflict int
		if err := db.QueryRow(`SELECT COUNT(1) FROM users WHERE email = ? AND id != ?`, req.NewValue, req.IdRef).Scan(&conflict); err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if conflict > 0 {
			priamProviderJSON(w, http.StatusConflict, map[string]string{"error": "an account with this email address already exists"})
			return
		}
		res, err := db.Exec(`UPDATE users SET email = ? WHERE id = ?`, req.NewValue, req.IdRef)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if n, _ := res.RowsAffected(); n == 0 {
			priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
			return
		}
	case orderDataTypeName:
		if req.DataName == "order_id" {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "primary key is not rectifiable"})
			return
		}
		if !orderAttributes[req.DataName] {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown or non-writable field"})
			return
		}
		orderID := req.PrimaryKeys["order_id"]
		value := any(req.NewValue)
		if req.DataName == "zip_code" {
			zip, err := strconv.Atoi(req.NewValue)
			if err != nil {
				priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "zip_code must be an integer"})
				return
			}
			value = zip
		}
		// req.DataName is checked against orderAttributes above, so this is
		// never attacker-controlled SQL - just the whitelisted column name.
		res, err := db.Exec(`UPDATE orders SET `+req.DataName+` = ? WHERE order_id = ? AND user_id = ?`, value, orderID, req.IdRef)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if n, _ := res.RowsAffected(); n == 0 {
			priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
			return
		}
	default:
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown dataTypeName"})
		return
	}
	priamProviderJSON(w, http.StatusOK, map[string]bool{"success": true})
}

// POST /api/erasure  body: {idRef, dataTypeName, dataName, primaryKeys}
// Erasing a User field deletes the whole account (email is the only User
// field, and it is the login identifier - there is no sensible "blank"
// account left otherwise) together with its orders, so no row references
// the deleted account afterward. Erasing an Order field deletes that whole
// order row (and its items) - same reasoning as the account case, no
// sensible blank address/email would leave a meaningful order behind.
func priamErasureHandler(w http.ResponseWriter, r *http.Request) {
	var req priamWriteRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "invalid request body"})
		return
	}

	switch req.DataTypeName {
	case userDataTypeName:
		if !userAttributes[req.DataName] {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown field"})
			return
		}
		if err := priamEraseUser(req.IdRef); err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
	case orderDataTypeName:
		if !orderAttributes[req.DataName] {
			priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown field"})
			return
		}
		orderID := req.PrimaryKeys["order_id"]
		found, err := priamEraseOrder(req.IdRef, orderID)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if !found {
			priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
			return
		}
	default:
		priamProviderJSON(w, http.StatusBadRequest, map[string]string{"error": "unknown dataTypeName"})
		return
	}
	priamProviderJSON(w, http.StatusOK, map[string]bool{"success": true})
}

func priamEraseUser(idRef string) error {
	tx, err := db.Begin()
	if err != nil {
		return err
	}
	defer tx.Rollback()
	if _, err := tx.Exec(`DELETE FROM order_items WHERE order_id IN (SELECT order_id FROM orders WHERE user_id = ?)`, idRef); err != nil {
		return err
	}
	if _, err := tx.Exec(`DELETE FROM orders WHERE user_id = ?`, idRef); err != nil {
		return err
	}
	if _, err := tx.Exec(`DELETE FROM users WHERE id = ?`, idRef); err != nil {
		return err
	}
	return tx.Commit()
}

func priamEraseOrder(idRef, orderID string) (bool, error) {
	tx, err := db.Begin()
	if err != nil {
		return false, err
	}
	defer tx.Rollback()
	var exists int
	if err := tx.QueryRow(`SELECT COUNT(1) FROM orders WHERE order_id = ? AND user_id = ?`, orderID, idRef).Scan(&exists); err != nil {
		return false, err
	}
	if exists == 0 {
		return false, nil
	}
	// order_items must be deleted before orders: it references
	// orders(order_id) and store.go turns on PRAGMA foreign_keys - deleting
	// the parent row first fails with a foreign key constraint violation
	// (found by testing a real erasure against this exact schema, not by
	// reading the code alone).
	if _, err := tx.Exec(`DELETE FROM order_items WHERE order_id = ?`, orderID); err != nil {
		return false, err
	}
	if _, err := tx.Exec(`DELETE FROM orders WHERE order_id = ? AND user_id = ?`, orderID, idRef); err != nil {
		return false, err
	}
	return true, tx.Commit()
}

type priamDataValueRequest struct {
	IdRef       string            `json:"idRef"`
	DataName    string            `json:"dataName"`
	PrimaryKeys map[string]string `json:"primaryKeys"`
}

// POST /api/dataValue  body: {idRef, dataName, primaryKeys} - no
// dataTypeName (§2/§8.2.f). "email" exists on both User and Order, so the
// type is inferred from primaryKeys' presence, exactly as the playbook
// documents: empty/absent primaryKeys -> the single-row-per-subject type
// (User), a populated primaryKeys (order_id) -> Order.
func priamDataValueHandler(w http.ResponseWriter, r *http.Request) {
	var req priamDataValueRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}

	if orderID, hasOrder := req.PrimaryKeys["order_id"]; hasOrder {
		if !orderAttributes[req.DataName] {
			priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
			return
		}
		order, err := priamGetOrder(req.IdRef, orderID)
		if err != nil {
			priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
			return
		}
		if order == nil {
			priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
			return
		}
		priamProviderJSON(w, http.StatusOK, map[string]string{"value": order[req.DataName]})
		return
	}

	if !userAttributes[req.DataName] {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	user, err := priamGetUser(req.IdRef)
	if err != nil {
		priamProviderJSON(w, http.StatusInternalServerError, map[string]string{"error": err.Error()})
		return
	}
	if user == nil {
		priamProviderJSON(w, http.StatusNotFound, map[string]string{"error": "record not found"})
		return
	}
	priamProviderJSON(w, http.StatusOK, map[string]string{"value": user[req.DataName]})
}
