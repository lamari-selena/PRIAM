# PRIAM ↔ OnlineBoutique — Integration Report

## 1. Mechanism, in one page

OnlineBoutique (`case-studies/OnlineBoutique`) is Google's microservices-demo:
11 polyglot services (Go frontend/checkout/shipping/productcatalog, C#
cartservice, Node currency/payment, Python email/recommendation/
shoppingassistant), no relational database of its own, no
`docker-compose.yml` upstream (targets GKE via kubernetes-manifests). **This
application has no sign-up, login, or account of any kind** — confirmed by
grepping the whole `src/` tree (zero hits for signup/login/register/
account/auth in application code) and by reading every service's storage
layer: the only durable identity concept is `session_id` (frontend/
middleware.go:85-110, an anonymous UUID cookie, `shop_session-id`), and the
only durably-stored personal data anywhere is the shopping cart
(cartservice's Redis-backed store, keyed by `session_id`, holding
`product_id`+`quantity` pairs). Checkout PII (email, address, credit card,
collected in `placeOrderHandler`) is purely transient — traced through
`checkoutservice/main.go`'s `PlaceOrder`, `paymentservice/charge.js`,
`shippingservice`, and `emailservice`'s dummy mode — never persisted
anywhere, only logged in fragments (email address, last-4 card digits).

- **idRef = `session_id`** — a real, stable (48h cookie), non-numeric UUID
  by construction, satisfying the playbook §7 "non-numeric idRef"
  requirement for every single test in this session, not a special case.
- **1 DataType annotated: `Cart`** (`product_id` is_primary_key=1,
  `quantity`) — the only data this application genuinely persists. Checkout
  PII is deliberately NOT annotated (see §2, Scope decisions) since nothing
  backs it after the HTTP response returns.
- **Provider bridge** (`case-studies/OnlineBoutique/src/frontend/
  priam_provider.go`, new) — a plain Go/gorilla-mux handler set, bare
  `/api/{dataAccessRight,rectification,erasure,dataValue}`, no auth. Reads/
  writes go **directly at cartservice's Redis store** (not through
  cartservice's gRPC surface, which only exposes `AddItem`
  (increment-only), `GetCart`, `EmptyCart` — none of which can set an exact
  quantity or erase a single product row while leaving the rest of the cart
  intact). Confirmed empirically (this session) that
  `Microsoft.Extensions.Caching.StackExchangeRedis` stores the cart's
  protobuf bytes in a Redis **hash**, field `data` — the Go bridge reads/
  writes that same field using the same protobuf wire format
  (`google.golang.org/protobuf`), reusing the real schema rather than
  inventing one.
- **CEP**: `frontend/rpc.go`'s `getRecommendations()` — the one genuinely
  optional processing found in this application's own code (personalizes
  suggested products from the cart's contents; every call site tolerates
  its absence, never required for checkout). Gated by
  `getConsent(idRef, "Product Recommendations")`.
- **Registration**: `ensureSessionID` (middleware.go) calls
  `registerDataSubject(sessionID)` **synchronously** (not a goroutine) the
  one time a session cookie is minted — this single hook covers every
  "user-creation point" there is, since `session_id` minting *is* this
  application's only such point. `homeHandler` (the equivalent of a
  "current user" route — there being no login, the root page is the first
  thing every new session hits) calls `hasPendingConsentDecision` and
  redirects to `{PRIAM_FRONTEND_URL}/consent` on a true flag.
  `addToCartHandler` reports `Cart`'s data_ids on every real cart mutation
  (§4bis, "the most frequently forgotten point") — not just at session
  creation.
- **Bidirectional navigation** (§4ter): "Manage on PRIAM" added to
  `header.html`'s navbar (visible on every page, matching the Habitica
  regression noted in the playbook), gated on `PRIAM_FRONTEND_URL`.
  PRIAM-Frontend's "Back to the app" points at
  `http://localhost:8080/` — OnlineBoutique's root **is** its real
  storefront/home page (no auth wall unlike Mastodon's SPA shell), so bare
  root is the correct, real, working target here — not a cop-out.
- **OAuth2 / Keycloak**: **out of scope**, documented, not silently
  skipped — see §2.

## 2. Scope decisions (documented, not silent)

- **Checkout PII (email, street address, credit card) is not annotated.**
  Traced through every hop of the checkout call chain
  (`checkoutservice/main.go:230-280` → `paymentservice/charge.js` →
  `shippingservice` → `emailservice`'s `DummyEmailService`): none of it is
  persisted anywhere after the response returns. Annotating it would force
  the Provider bridge to fabricate a backing store that doesn't exist,
  which would fail the "never assume something works without testing
  against real state" constraint of this session. This is a genuine
  architectural property of this demo app, not an oversight.
- **No MANDATORY/DEFAULT processing.** Nothing in this application's own
  code is processed under a distinct legal obligation, and there is no
  login/authentication processing to mark DEFAULT (there is no login).
  Only `NECESSARY` (`Cart Management`) and `OPTIONAL`
  (`Product Recommendations`) are used — not invented for the sake of
  covering all 4 `processing_type` values (playbook §1 point 6 lists them
  as available, not mandatory to use all — same precedent as Mastodon).
- **No `personal_data_transfer`/`secondary_actor`.** Both processings stay
  entirely internal to this application (cartservice, recommendationservice
  are internal microservices, not third parties) — conditional annotation,
  correctly left empty.
- **Keycloak provisioning (§4bis "Automatic Keycloak identity
  provisioning") does not apply and was not built.** That mechanism's
  documented trigger condition is "the target application has its own local
  sign-up (email/password)" — OnlineBoutique has neither a password nor any
  concept of an account a human would recognize. There is no plaintext
  credential to ever capture and synchronize into Keycloak. Per the task's
  own instruction, this is documented here as a known limitation rather
  than silently ignored: **`CUSTOM_OIDC_ISSUER_URI`/`CUSTOM_OIDC_JWK_SET_URI`
  are left blank in the root `.env`** (Gateway auth fails open, §6), and no
  `provision_keycloak_user()`-equivalent function was written for this
  case study. A secondary, purely operational reason this session did not
  exercise Keycloak at all: **Keycloak's fixed host port (8080) collides
  with OnlineBoutique's own frontend** (also 8080 by upstream convention,
  kubernetes-manifests/frontend.yaml) — starting `keycloak` alongside a
  running OnlineBoutique stack fails with `port is already allocated`.
  Since OAuth2 is already out of scope for this case study, this was not
  worked around (e.g. remapping Keycloak's host port) — noted here for
  transparency rather than silently left unexplained.
- **No real-browser test performed.** No Playwright/browser-automation tool
  is available in this environment (confirmed via `ToolSearch` — only
  `WebFetch`, which does not render JavaScript/SPAs or click through forms).
  Every workflow below was instead verified via curl against the real
  `PRIAM-Right-service`/`PRIAM-Consent-Service` endpoints, with real backend
  state read directly (Redis, MySQL) after each step — the same
  substitution documented in the Mastodon integration report. `PRIAM-Frontend`
  and `PRIAM-Frontend-Provider` were built and started, and confirmed to
  serve HTTP 200 (Angular compiled successfully, no crash) — but no DOM
  interaction was performed. Per playbook §7 point 14: frontend visual
  validation is not claimed.

## 3. Bugs found this session

**Zero bugs found in PRIAM's own code** this session
(`git diff --stat -- PRIAM-Services/ Docs/PRIAM-INTEGRATION-PLAYBOOK.md` is
empty) — nothing was added to §8 of the playbook, per the non-negotiable
constraint. The issues below all live in this session's own environment/
process, not in PRIAM or in OnlineBoutique's application code:

| # | Root cause | Fix | Proof |
|---|---|---|---|
| 1 | Transient Go module-proxy network failures (`unexpected EOF` fetching `proxy.golang.org` zips) during `go mod download`/`docker compose build frontend|paymentservice|currencyservice` — the exact "unstable Docker Desktop DNS" class of pitfall already documented in playbook §8.9, encountered fresh on 3 separate builds this session. | Retried the same `docker compose build <service>` command unchanged — succeeded on the 2nd attempt every time. | Full build logs for `frontend` (background task `bgrcag4jq` failed with `unexpected EOF`; retry `btpzz27ld` succeeded, confirmed via `docker images | grep onlineboutique-frontend`) and `paymentservice`/`currencyservice` (same pattern). |
| 2 | Host port 8080 collision between PRIAM's `keycloak` service and OnlineBoutique's own `frontend` (both default to 8080 — kubernetes-manifests/frontend.yaml and PRIAM's root docker-compose.yml). | Not fixed — Keycloak is out of scope for this case study (§2), so left unstarted rather than remapping ports. | `docker compose up -d keycloak` → `Error response from daemon: ... Bind for 0.0.0.0:8080 failed: port is already allocated`. |
| 3 | `go mod tidy` run inside the Docker build (to resolve the new `github.com/redis/go-redis/v9` dependency) never persisted its resolved `go.mod`/`go.sum` back to the host — the committed files would have stayed inconsistent with what was actually built. | Built the `builder` stage alone (`docker build --target builder`), `docker cp`'d the resolved `go.mod`/`go.sum` back onto the host, then simplified the Dockerfile back to a plain `go mod download` (net zero diff on `Dockerfile` — confirmed via `git diff`, not part of the final changeset). | `grep redis go.mod go.sum` before the fix: empty. After: `github.com/redis/go-redis/v9 v9.21.0` pinned in both, and a subsequent `docker compose build frontend` succeeds using only `go mod download`. |
| 4 | `docker images` showed no `onlineboutique-currencyservice` image despite an earlier background build reporting "completed (exit code 0)" — the background task wrapper's exit code reflected the shell pipeline (`... \| tail -N`), not the actual `docker compose build` exit status, silently masking a real build failure. | Rebuilt explicitly and confirmed success via the log text ("Image ... Built") and `docker images`, not the task-notification status alone. | First `docker compose up` attempt failed with `No such image: onlineboutique-currencyservice:latest`; rebuild produced the image, confirmed present. |

## 4. Workflows verified against real state (this session)

All curl commands and exact real-state proofs are in `ETAPES-FAITES.md`.
Summary:

| Workflow | Method | Real state checked | Result |
|---|---|---|---|
| Registration (new anonymous session) | `curl -i http://localhost:8080/` (no cookie) | `priam-actor.data_subject` via `GET /actor/api/DataSubject/ref/{idRef}` | Row created synchronously before the response returned; 13 distinct real sessions registered over the course of this session's testing, none duplicated |
| Forced-consent redirect | Same curl, undecided idRef | HTTP response `Location` header | `302` to `http://localhost:4200/consent` for every undecided subject; `200` (no redirect) once a decision exists — no redirect loop confirmed |
| `report_processed_data` on cart mutation | `curl -X POST /cart` (add item) | `priam-data.processed_data` rows for data_id 1/2 | Created with `nb_occurrences=1` immediately after the first add; incremented on a second add |
| Provider bridge `dataAccessRight` (direct + via Gateway) | `curl .../api/dataAccessRight` and `curl localhost:8090/provider/api/dataAccessRight` | JSON array vs. raw `HGET <idRef> data` (protobuf, hand-decoded) | Identical values both ways; array format, string-valued `quantity`, confirmed against raw Redis bytes independent of the bridge's own code |
| Access request, `answer=false` then real approval (`data:[...]` matching all requested ids → `FULL`) | `POST /right/api/right/accessRequest` + `/right/api/right/answer` | `isAccepted` via `GET /right/api/isAccepted` | `REFUSED` when `data` omits ids; `FULL` + `isAccepted=true` once all requested ids are included — always-open read (`personalDataValues/accessRight`) unaffected by the answer either way, as documented in playbook §3 |
| Rectification (`quantity` 2→5), `answer=false` then `true` | `POST /right/api/right/rectificationRequest` + `/answer` | Raw `HGET` protobuf bytes (hand-decoded varint) | Unchanged after refusal; changed to `5` after approval, confirmed in the raw Redis bytes, not just the Provider bridge's own read |
| Erasure of one Cart row (2 items in cart), `answer=false` then `true` | `POST /right/api/right/erasureRequest` + `/answer`, `primaryKeys:{"product_id":"OLJCESPC7Z"}` | Raw `HGET` protobuf bytes | Both items survive refusal; only the targeted row removed on approval, the other (`66VCHSJNUP`) intact — the §8.1.c composite/primary-key precision scenario |
| `dataValue` (4th Provider endpoint, §8.2.f) | `curl .../provider/api/dataValue`, both `product_id` and `quantity`, no `dataTypeName` in body | Provider bridge response | Correct value for both fields, type inferred from `dataName` alone |
| Consent grant (pre-seeded) → observable side effect | Real cart page render | Recommended-product `<a href="/product/...">` links in the rendered HTML | 4 real product links present |
| Consent withdrawal | `POST /cdp/api/consent/create/{idRef}` | `priam-consent.consent.end_date`, `priam-data.processed_data.nb_occurrences`, `GET /cdp/api/decision/...` | `end_date` set; `nb_occurrences` for `product_id` decremented; CEP now returns `false`; recommendation links absent from the rendered page |
| Consent re-grant | Same endpoint, 2nd toggle | New `consent` row (`end_date=NULL`) | CEP returns `true` again; recommendation links reappear (different products, confirming a live call, not cached) |
| Backfill script | `sh priam-integration/backfill-data-subjects.sh` | `priam-actor.data_subject` (no duplicate row), `processed_data.nb_occurrences` (incremented, idempotent) | Found the one real cart key in Redis, re-registered it without creating a duplicate `data_subject` row |

## 5. LOC breakdown

**Method**: `git diff --numstat` (per-file table below) gives raw +/-
counts only — it does not classify code vs. comment vs. blank. That
classification was done with a small `awk` script
(`classify.sh`, scratch, not committed) applying a fixed rule: after
stripping leading whitespace, an empty line = blank; a line starting with
`--` (SQL), `#` (shell), `//` or `*` (Go) = comment; everything else = code.
For **new** files, every line of the final file was classified. For
**modified** files, only lines actually added this session were classified
(`git diff --unified=0`, lines starting with a single `+`, `+++` excluded).
The root `.env` (not git-tracked) was classified manually from the exact
before/after content of the 3 edits made (both blocks piped through the
same script). `go.mod`/`go.sum` are lock/manifest files, not hand-written
source — every added line was still counted as "code" (declarative content,
no comment syntax applies), 0 comment/blank, and assigned to the Rights-API
category since they exist solely to support the Provider bridge's Redis
client. `go.sum`'s 92 deleted vs. 36 added lines is `go mod tidy`
re-resolving the whole dependency graph as a side effect of adding one new
direct dependency (pruning now-unneeded transitive-dependency checksum
entries) — normal `go mod tidy` behavior, not a sign of a larger change
than it appears; only 1 new direct dependency
(`github.com/redis/go-redis/v9`) and 1 new transitive one
(`go.uber.org/atomic`) were actually added, per `go.mod`'s own diff.

`case-studies/OnlineBoutique/src/frontend/Dockerfile` was edited twice this
session (added `go mod tidy` to resolve the new dependency, then reverted
to a plain `go mod download` once `go.mod`/`go.sum` were pinned via
`docker cp` from the builder stage) — **net diff is zero**
(`git diff` on this file is empty), so it does not appear in the table
below.

### Per-file

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | modified (full rewrite for OnlineBoutique) | 150 | 184 |
| `case-studies/OnlineBoutique/docker-compose.yml` | **new** | 182 | 0 |
| `case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh` | **new** | 57 | 0 |
| `case-studies/OnlineBoutique/src/frontend/priam.go` | **new** | 139 | 0 |
| `case-studies/OnlineBoutique/src/frontend/priam_provider.go` | **new** | 267 | 0 |
| `case-studies/OnlineBoutique/src/frontend/go.mod` | modified | 2 | 0 |
| `case-studies/OnlineBoutique/src/frontend/go.sum` | modified | 36 | 92 |
| `case-studies/OnlineBoutique/src/frontend/handlers.go` | modified | 24 | 0 |
| `case-studies/OnlineBoutique/src/frontend/main.go` | modified | 8 | 0 |
| `case-studies/OnlineBoutique/src/frontend/middleware.go` | modified | 9 | 0 |
| `case-studies/OnlineBoutique/src/frontend/rpc.go` | modified | 8 | 0 |
| `case-studies/OnlineBoutique/src/frontend/templates/header.html` | modified | 4 | 0 |
| `docker-compose.yml` (PRIAM root, `name:` field) | modified | 1 | 1 |
| `.env` (PRIAM root, not git-tracked — `CUSTOM_PROVIDER_URL`, `CUSTOM_OIDC_*`, `TARGET_APP_URL`) | modified | 23 | 15 |

`case-studies/OnlineBoutique/priam-integration/INTEGRATION-REPORT.md` and
`ETAPES-FAITES.md` themselves are excluded from this table (documentation
about the session, not integration code — same convention as the Mastodon
report).

### By functional category × line nature

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| **Annotation** (`db_insertion_script.sql`) | 18 | 126 | 6 | 150 |
| **Rights-API** (`priam_provider.go` + `main.go` route registration + `go.mod`/`go.sum`) | 254 | 39 | 20 | 313 |
| **Consent** (`priam.go` + `middleware.go`/`handlers.go`/`rpc.go` diffs + `header.html` + backfill script) | 145 | 74 | 22 | 241 |
| **OAuth2** | 0 | 0 | 0 | 0 |
| **Docker-network** (OnlineBoutique `docker-compose.yml` + root `docker-compose.yml` + `.env`) | 146 | 49 | 11 | 206 |
| **Total** | **563** | **288** | **59** | **910** |

**OAuth2 = 0 across the board, deliberately** — see §2. This is the
documented outcome of a real scope decision (no local sign-up to hook into,
no plaintext password ever available), not an omission.

**Rights-API category, file-by-file** (254/39/20): `priam_provider.go` (new,
full file) 212/36/19 · `main.go` diff (route registration) 4/3/1 ·
`go.mod` diff 2/0/0 · `go.sum` diff 36/0/0.

**Consent category, file-by-file** (145/74/22): `priam.go` (new, full file)
104/25/10 · `backfill-data-subjects.sh` (new, full file) 26/22/9 ·
`handlers.go` diff (redirect + `reportProcessedData` call +
`priamFrontendUrl` template var) 8/14/2 · `middleware.go` diff
(`registerDataSubject` call) 1/8/0 · `rpc.go` diff (CEP gate) 3/5/0 ·
`header.html` diff ("Manage on PRIAM" link) 3/0/1.

**Docker-network category, file-by-file** (146/49/11): OnlineBoutique
`docker-compose.yml` (new, full file) 141/30/11 · root `docker-compose.yml`
`name:` field 1/0/0 · root `.env` (`CUSTOM_PROVIDER_URL`, `CUSTOM_OIDC_*`,
`TARGET_APP_URL` values + comments) 4/19/0.

### PRIAM's own LOC (this session)

**Zero.** `git diff --stat -- PRIAM-Services/ Docs/PRIAM-INTEGRATION-PLAYBOOK.md`
is empty for this session — no generic PRIAM bug was found (§3 lists only
environment/process issues, none in PRIAM's own code), so nothing was added
to playbook §8, per the non-negotiable constraint.
