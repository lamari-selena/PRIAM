// PRIAM integration (Docs/PRIAM-INTEGRATION-PLAYBOOK.md §4/§4bis). idRef is
// the account's UUID (users.id, see store.go) - a real, stable id, only
// known once a subject has signed up (see accounts_handlers.go). Guest
// visitors (no account) have no idRef and are never reported to PRIAM: the
// durable personal data this integration annotates (account email, order
// address) only exists for registered subjects in the first place. All 3
// env vars below are optional; every function here is a documented no-op
// (fail-open/fail-closed as noted) when its required var is unset, so
// PRIAM's availability never affects the ability to sign up/shop.
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
	priamDataSubjectCategoryID     = 1 // priam-actor.data_subject_category "Registered Customer"
	priamRecommendationsProcessing = "Product Recommendations"
)

// priamUserDataIDs / priamOrderDataIDs are the priam-data.data row ids for
// the "User"/"Order" data_types - see Databases/db_insertion_script.sql.
var (
	priamUserDataIDs  = []int{1}
	priamOrderDataIDs = []int{2, 3, 4, 5, 6, 7, 8}
)

var priamHTTPClient = &http.Client{Timeout: 3 * time.Second}

var (
	priamActorURL = os.Getenv("PRIAM_ACTOR_URL")
	priamCDPURL   = os.Getenv("PRIAM_CDP_URL")
	priamDataURL  = os.Getenv("PRIAM_DATA_URL")
)

// Keycloak provisioning (playbook §4bis, "Automatic Keycloak identity
// provisioning at sign-up") - reuses the admin bootstrap account already
// defined on PRIAM's own root docker-compose.yml `keycloak` service, per
// that section's own stated development/testing tradeoff.
var (
	priamKeycloakAdminURL  = os.Getenv("KEYCLOAK_ADMIN_URL")
	priamKeycloakRealm     = os.Getenv("KEYCLOAK_REALM")
	priamKeycloakAdminUser = os.Getenv("KEYCLOAK_ADMIN_USERNAME")
	priamKeycloakAdminPass = os.Getenv("KEYCLOAK_ADMIN_PASSWORD")
)

// registerDataSubject creates (or, idempotently, re-upserts) idRef's PRIAM
// data_subject row. Called synchronously from signupHandler - deliberately
// blocking (bounded by priamHTTPClient's timeout) rather than firing a
// goroutine, so that the reportProcessedData call fired right after it in
// the same handler never races it (playbook §4bis "mandatory ordering" /
// §8.6).
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
// getConsent below, the Consent Decision Point). Used to force the one-time
// redirect to PRIAM's consent page right after sign-up/login.
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

// provisionKeycloakUser creates a Keycloak account mirroring the local
// account just created, so PRIAM's "Manage on PRIAM" link (playbook §4ter)
// logs the user into an identity that actually corresponds to their real
// OnlineBoutique account instead of an unrelated one (playbook §4bis).
// email is reused as Keycloak's username - OnlineBoutique has no separate
// handle, and email is always long enough to clear Keycloak's username
// minimum length (§4bis pitfall). firstName/lastName are set to email too:
// OnlineBoutique collects no separate name at sign-up, and both are
// mandatory in the realm's default User Profile (§4bis pitfall - creation
// otherwise "succeeds" but login then fails with "Account is not fully set
// up"). The plaintext password is only available here, at sign-up time -
// store.go hashes it immediately and never exposes it again. Never blocks
// or fails sign-up: called via `go` from signupHandler, and every failure
// path below only logs.
func provisionKeycloakUser(idRef, email, password string) {
	if priamKeycloakAdminURL == "" {
		return
	}

	token, err := fetchKeycloakAdminToken()
	if err != nil {
		fmt.Printf("priam: provisionKeycloakUser(%s) admin token failed: %v\n", idRef, err)
		return
	}

	body, _ := json.Marshal(map[string]any{
		"username":      email,
		"email":         email,
		"firstName":     email,
		"lastName":      email,
		"enabled":       true,
		"emailVerified": true,
		"credentials": []map[string]any{
			{"type": "password", "value": password, "temporary": false},
		},
		// idReference must already be declared in the realm's User Profile
		// (playbook §4bis pitfall - otherwise silently stripped, 201 either
		// way) - not this application's responsibility to declare it.
		"attributes": map[string][]string{
			"idReference": {idRef},
		},
	})

	reqURL := fmt.Sprintf("%s/admin/realms/%s/users", priamKeycloakAdminURL, url.PathEscape(priamKeycloakRealm))
	req, err := http.NewRequest(http.MethodPost, reqURL, bytes.NewReader(body))
	if err != nil {
		return
	}
	req.Header.Set("Content-Type", "application/json")
	req.Header.Set("Authorization", "Bearer "+token)

	resp, err := priamHTTPClient.Do(req)
	if err != nil {
		fmt.Printf("priam: provisionKeycloakUser(%s) create failed: %v\n", idRef, err)
		return
	}
	defer resp.Body.Close()

	// 409 = already provisioned (retry, or a manually-created account) -
	// not an error, per playbook §4bis "idempotent by construction".
	if resp.StatusCode != http.StatusCreated && resp.StatusCode != http.StatusConflict {
		raw, _ := io.ReadAll(resp.Body)
		fmt.Printf("priam: provisionKeycloakUser(%s) unexpected status %d: %s\n", idRef, resp.StatusCode, raw)
	}
}

// fetchKeycloakAdminToken obtains a short-lived admin access token via the
// Direct Grant flow against Keycloak's own built-in admin-cli client - the
// same flow ETAPES-FAITES.md §1.7 already exercised manually with curl.
func fetchKeycloakAdminToken() (string, error) {
	form := url.Values{
		"client_id":  {"admin-cli"},
		"username":   {priamKeycloakAdminUser},
		"password":   {priamKeycloakAdminPass},
		"grant_type": {"password"},
	}
	resp, err := priamHTTPClient.PostForm(priamKeycloakAdminURL+"/realms/master/protocol/openid-connect/token", form)
	if err != nil {
		return "", err
	}
	defer resp.Body.Close()
	if resp.StatusCode != http.StatusOK {
		raw, _ := io.ReadAll(resp.Body)
		return "", fmt.Errorf("status %d: %s", resp.StatusCode, raw)
	}
	var payload struct {
		AccessToken string `json:"access_token"`
	}
	if err := json.NewDecoder(resp.Body).Decode(&payload); err != nil {
		return "", err
	}
	return payload.AccessToken, nil
}
