// PRIAM integration (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4/§4bis). This
// application has no sign-up/login (see priam_provider.go's header comment)
// - session_id (the shop_session-id cookie, always a non-numeric UUID) is
// used as PRIAM's idRef. All 3 env vars below are optional; every function
// here is a documented no-op (fail-open/fail-closed as noted) when its
// required var is unset, so PRIAM's availability never affects the ability
// to browse/shop.
package main

import (
	"bytes"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"strconv"
	"time"
)

const (
	priamDataSubjectCategoryID = 1 // priam-actor.data_subject_category "Shopper"
	priamRecommendationsProcessing = "Product Recommendations" // OPTIONAL processing_id=2
)

// priamCartDataIDs are the priam-data.data rows for the "Cart" data_type
// (product_id, quantity) - see Databases/db_insertion_script.sql.
var priamCartDataIDs = []int{1, 2}

var priamHTTPClient = &http.Client{Timeout: 3 * time.Second}

var (
	priamActorURL   = os.Getenv("PRIAM_ACTOR_URL")
	priamCDPURL     = os.Getenv("PRIAM_CDP_URL")
	priamDataURL    = os.Getenv("PRIAM_DATA_URL")
)

// registerDataSubject creates (or, idempotently, re-upserts) this session's
// PRIAM data_subject row. Called synchronously from ensureSessionID
// (middleware.go) for a brand-new session only, with a short timeout - this
// deliberately blocks that one-time cookie-issuing request rather than
// firing a goroutine, so that any later call in the same request chain that
// needs idRef->dataSubjectId resolution never races it (playbook §4bis
// "mandatory ordering" / §8.6).
func registerDataSubject(idRef string) {
	if priamActorURL == "" {
		return
	}
	body, _ := json.Marshal(map[string]any{
		"idRef":                 idRef,
		"dataSubjectCategoryId": priamDataSubjectCategoryID,
	})
	resp, err := priamHTTPClient.Post(priamActorURL+"/api/DataSubject", "application/json", bytes.NewReader(body))
	if err != nil {
		fmt.Printf("priam: registerDataSubject(%s) failed: %v\n", idRef, err)
		return
	}
	resp.Body.Close()
}

// hasPendingConsentDecision reports whether idRef has never been asked
// about processingName yet (the Consent Information Point - distinct from
// get_consent below, the Consent Decision Point). Used to force the
// one-time redirect to PRIAM's consent page.
func hasPendingConsentDecision(idRef, processingName string) bool {
	if priamCDPURL == "" {
		return false
	}
	reqURL := fmt.Sprintf("%s/api/contract/list/consents/%s/%s", priamCDPURL, url.PathEscape(idRef), url.PathEscape(processingName))
	resp, err := priamHTTPClient.Get(reqURL)
	if err != nil {
		return false
	}
	defer resp.Body.Close()
	var decisions []any
	if err := json.NewDecoder(resp.Body).Decode(&decisions); err != nil {
		return false
	}
	return len(decisions) == 0
}

// getConsent is the CEP (playbook §4): fail-open if PRIAM_CDP_URL is
// absent, fail-closed on any error/unreachable PRIAM.
func getConsent(idRef, processingName string) bool {
	if priamCDPURL == "" {
		return true
	}
	reqURL := fmt.Sprintf("%s/api/decision/%s?idRefList=%s", priamCDPURL, url.PathEscape(processingName), url.QueryEscape(idRef))
	resp, err := priamHTTPClient.Get(reqURL)
	if err != nil {
		return false
	}
	defer resp.Body.Close()
	var decision map[string]bool
	if err := json.NewDecoder(resp.Body).Decode(&decision); err != nil {
		return false
	}
	return decision[idRef]
}

// reportProcessedData tells PRIAM that idRef now holds data for the given
// priam-data.data ids (playbook §4bis - "the most frequently forgotten
// point"). Never blocks the caller on failure.
func reportProcessedData(idRef string, dataIDs []int) {
	if priamActorURL == "" || priamDataURL == "" {
		return
	}
	resp, err := priamHTTPClient.Get(priamActorURL + "/api/DataSubjectId/" + url.PathEscape(idRef))
	if err != nil {
		fmt.Printf("priam: reportProcessedData(%s) id lookup failed: %v\n", idRef, err)
		return
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		fmt.Printf("priam: reportProcessedData(%s) id lookup status %d\n", idRef, resp.StatusCode)
		return
	}
	raw, _ := io.ReadAll(resp.Body)
	dataSubjectID, err := strconv.Atoi(string(bytes.TrimSpace(raw)))
	if err != nil {
		fmt.Printf("priam: reportProcessedData(%s) could not parse dataSubjectId %q: %v\n", idRef, raw, err)
		return
	}

	body, _ := json.Marshal(dataIDs)
	addURL := fmt.Sprintf("%s/api/processed-data/add?subjectId=%d", priamDataURL, dataSubjectID)
	req, err := http.NewRequest(http.MethodPost, addURL, bytes.NewReader(body))
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")
	resp2, err := priamHTTPClient.Do(req)
	if err != nil {
		fmt.Printf("priam: reportProcessedData(%s) add failed: %v\n", idRef, err)
		return
	}
	resp2.Body.Close()
}
