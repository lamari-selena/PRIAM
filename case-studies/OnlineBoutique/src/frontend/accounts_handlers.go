// Copyright 2026 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package main

import (
	"net/http"
	"os"

	"github.com/pkg/errors"
	"github.com/sirupsen/logrus"

	"github.com/GoogleCloudPlatform/microservices-demo/src/frontend/validator"
)

// priamPostAuthRedirect sends a freshly signed-in/signed-up account to
// PRIAM's consent page instead of "/" if it has never been asked about the
// OPTIONAL processing yet (playbook §4bis - the "current user" hook this
// application uses for the forced-consent redirect, since it has no
// AJAX "current user" API to piggyback on: the point right after
// signup/login already handles post-auth navigation). Fires at most once by
// construction: hasPendingConsentDecision becomes false as soon as a
// decision exists, so there is no redirect loop on a later visit.
func priamPostAuthRedirect(w http.ResponseWriter, r *http.Request, idRef string) bool {
	priamFrontendURL := os.Getenv("PRIAM_FRONTEND_URL")
	if priamFrontendURL == "" || !hasPendingConsentDecision(idRef, priamRecommendationsProcessing) {
		return false
	}
	http.Redirect(w, r, priamFrontendURL+"/consent", http.StatusFound)
	return true
}

func (fe *frontendServer) signupPageHandler(w http.ResponseWriter, r *http.Request) {
	log := r.Context().Value(ctxKeyLog{}).(logrus.FieldLogger)
	currencies, err := fe.getCurrencies(r.Context())
	if err != nil {
		renderHTTPError(log, r, w, errors.Wrap(err, "could not retrieve currencies"), http.StatusInternalServerError)
		return
	}
	if err := templates.ExecuteTemplate(w, "signup", injectCommonTemplateData(r, map[string]interface{}{
		"show_currency": false,
		"currencies":    currencies,
	})); err != nil {
		log.Error(err)
	}
}

func (fe *frontendServer) signupHandler(w http.ResponseWriter, r *http.Request) {
	log := r.Context().Value(ctxKeyLog{}).(logrus.FieldLogger)

	payload := validator.SignupPayload{
		Email:           r.FormValue("email"),
		Password:        r.FormValue("password"),
		ConfirmPassword: r.FormValue("confirm_password"),
	}
	if err := payload.Validate(); err != nil {
		renderHTTPError(log, r, w, validator.ValidationErrorResponse(err), http.StatusUnprocessableEntity)
		return
	}

	id, err := createUser(payload.Email, payload.Password)
	if err != nil {
		code := http.StatusInternalServerError
		if err == errEmailTaken {
			code = http.StatusConflict
		}
		renderHTTPError(log, r, w, errors.Wrap(err, "could not create account"), code)
		return
	}

	log.WithField("user", id).Info("account created")
	http.SetCookie(w, &http.Cookie{
		// Path must be explicit: both handlers below live at /accounts/*,
		// so an unset Path would default (RFC 6265 default-path) to
		// "/accounts" - the cookie would never be sent on /, /cart, or
		// /cart/checkout, making currentUserID(r) silently return "" (and
		// PRIAM's order-linkage/registration wiring silently never fire)
		// everywhere outside /accounts itself. Found by testing an actual
		// signup->add-to-cart flow with curl, not by reading the code.
		Path:   "/",
		Name:   cookieUserID,
		Value:  id,
		MaxAge: cookieMaxAge,
	})

	// PRIAM registration (playbook §4bis) - synchronous, not a goroutine:
	// reportProcessedData right after resolves idRef->dataSubjectId
	// internally, so it must never race registerDataSubject committing
	// (§4bis "mandatory ordering" / §8.6).
	registerDataSubject(id)
	go reportProcessedData(id, priamUserDataIDs)

	// Automatic Keycloak provisioning (playbook §4bis) - the plaintext
	// password is only ever available here; store.go has already hashed it
	// for local storage by the time createUser() above returns. No ordering
	// dependency on the two calls above, so a goroutine is safe here (unlike
	// registerDataSubject/reportProcessedData's §8.6 ordering constraint).
	go provisionKeycloakUser(id, payload.Email, payload.Password)

	if priamPostAuthRedirect(w, r, id) {
		return
	}
	w.Header().Set("Location", baseUrl+"/")
	w.WriteHeader(http.StatusFound)
}

func (fe *frontendServer) loginPageHandler(w http.ResponseWriter, r *http.Request) {
	log := r.Context().Value(ctxKeyLog{}).(logrus.FieldLogger)
	currencies, err := fe.getCurrencies(r.Context())
	if err != nil {
		renderHTTPError(log, r, w, errors.Wrap(err, "could not retrieve currencies"), http.StatusInternalServerError)
		return
	}
	if err := templates.ExecuteTemplate(w, "login", injectCommonTemplateData(r, map[string]interface{}{
		"show_currency": false,
		"currencies":    currencies,
	})); err != nil {
		log.Error(err)
	}
}

func (fe *frontendServer) loginHandler(w http.ResponseWriter, r *http.Request) {
	log := r.Context().Value(ctxKeyLog{}).(logrus.FieldLogger)

	payload := validator.LoginPayload{
		Email:    r.FormValue("email"),
		Password: r.FormValue("password"),
	}
	if err := payload.Validate(); err != nil {
		renderHTTPError(log, r, w, validator.ValidationErrorResponse(err), http.StatusUnprocessableEntity)
		return
	}

	acct, err := authenticateUser(payload.Email, payload.Password)
	if err != nil {
		code := http.StatusInternalServerError
		if err == errInvalidCredentials {
			code = http.StatusUnauthorized
		}
		renderHTTPError(log, r, w, errors.Wrap(err, "could not log in"), code)
		return
	}

	log.WithField("user", acct.ID).Info("account logged in")
	http.SetCookie(w, &http.Cookie{
		// Path must be explicit: both handlers below live at /accounts/*,
		// so an unset Path would default (RFC 6265 default-path) to
		// "/accounts" - the cookie would never be sent on /, /cart, or
		// /cart/checkout, making currentUserID(r) silently return "" (and
		// PRIAM's order-linkage/registration wiring silently never fire)
		// everywhere outside /accounts itself. Found by testing an actual
		// signup->add-to-cart flow with curl, not by reading the code.
		Path:   "/",
		Name:   cookieUserID,
		Value:  acct.ID,
		MaxAge: cookieMaxAge,
	})

	if priamPostAuthRedirect(w, r, acct.ID) {
		return
	}
	w.Header().Set("Location", baseUrl+"/")
	w.WriteHeader(http.StatusFound)
}

func (fe *frontendServer) ordersHandler(w http.ResponseWriter, r *http.Request) {
	log := r.Context().Value(ctxKeyLog{}).(logrus.FieldLogger)

	uid := currentUserID(r)
	if uid == "" {
		w.Header().Set("Location", baseUrl+"/accounts/login")
		w.WriteHeader(http.StatusFound)
		return
	}

	currencies, err := fe.getCurrencies(r.Context())
	if err != nil {
		renderHTTPError(log, r, w, errors.Wrap(err, "could not retrieve currencies"), http.StatusInternalServerError)
		return
	}

	orders, err := getOrdersForUser(uid)
	if err != nil {
		renderHTTPError(log, r, w, errors.Wrap(err, "could not retrieve orders"), http.StatusInternalServerError)
		return
	}

	if err := templates.ExecuteTemplate(w, "orders", injectCommonTemplateData(r, map[string]interface{}{
		"show_currency": false,
		"currencies":    currencies,
		"orders":        orders,
	})); err != nil {
		log.Error(err)
	}
}

// currentUserID reads the authenticated-account cookie set by signup/login.
// It returns "" for anonymous/guest visitors, distinct from the anonymous
// shop_session-id cookie which every visitor has regardless of login state.
func currentUserID(r *http.Request) string {
	c, err := r.Cookie(cookieUserID)
	if err != nil {
		return ""
	}
	return c.Value
}
