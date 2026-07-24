# PRIAM × OnlineBoutique — Integration Report

## 0. Context: this replaces a previous, weaker integration

A first PRIAM integration for OnlineBoutique already existed (commit
`76ab7a6`), using the anonymous `shop_session-id` cookie as `idRef` and
annotating only the Redis-backed Cart (the one durable data type upstream
OnlineBoutique has). Before this session, a separate preparatory change
("`changes for data persistence.md`", not itself a PRIAM change) added a
real account system (email/password, bcrypt-hashed) and a durable SQLite
order history to the frontend service, specifically to give this
application a realistic GDPR surface. This session **replaces** the old
Cart-based integration with one built on that richer model: real accounts
(`User`) and real order history (`Order`), and re-verifies every workflow
end-to-end against a live stack.

## 1. Mechanism, in one page

- **idRef** = `users.id` (a `github.com/google/uuid` string minted at
  sign-up, `store.go createUser`) — non-numeric by construction, so every
  test in this session naturally satisfies the playbook §7 "non-numeric
  idRef" requirement, not just a specially-crafted one. Guest checkouts
  (`user_id = NULL`) have no idRef and are never reported to PRIAM.
- **Annotation** (`Databases/db_insertion_script.sql`): two `DataType`s,
  `User` (one row per subject: `email`) and `Order` (several rows per
  subject: `order_id` [primary key], `email`, `street_address`, `city`,
  `state`, `zip_code`, `country`). Two `NECESSARY` processings (`Account
  Management`, `Order Fulfillment`) and one `OPTIONAL` processing (`Product
  Recommendations`, deliberately zero `data_usage` rows — see §2 below).
- **Provider bridge** (`src/frontend/priam_provider.go`, new file): the 4
  endpoints on bare `/api`, reading/writing the exact same SQLite tables
  `store.go` already manages (no separate access path). `dataValue`
  disambiguates `User.email` vs `Order.email` by the presence of
  `primaryKeys["order_id"]`, per the playbook §2/§8.2.f contract.
- **CEP** (`src/frontend/priam.go`, new file — `getConsent`): wraps only
  `rpc.go`'s `getRecommendations`, gated on the signed-in idRef (guests
  always see recommendations — no identifiable subject involved for them).
- **Registration + forced consent + processed-data reporting**
  (`priam.go`'s `registerDataSubject`/`hasPendingConsentDecision`/
  `reportProcessedData`, wired into `accounts_handlers.go`): `signupHandler`
  synchronously calls `registerDataSubject` before firing
  `reportProcessedData` (fire-and-forget) for `User.email` — this ordering
  is what avoids the §8.6 race. `handlers.go`'s `placeOrderHandler` reports
  `Order`'s data ids after every real order, not just at sign-up. Both
  `signupHandler` and `loginHandler` redirect to `{PRIAM_FRONTEND_URL}/consent`
  exactly once via `priamPostAuthRedirect` if a decision is still pending.
- **Round-trip navigation** (§4ter): `header.html` shows "Manage on PRIAM"
  next to "Log Out" for a signed-in user; the root `.env`'s
  `TARGET_APP_URL=http://localhost:9090/` drives PRIAM-Frontend's own "back
  to the app" link (OnlineBoutique's own root is a real product-catalog
  page, not a placeholder).
- **Docker wiring**: `case-studies/OnlineBoutique/docker-compose.yml` (new)
  reproduces upstream's 10 microservices as plain containers (no
  Kubernetes), attaches `frontend` to `common_network`, and bind-mounts
  `../../onlineboutique-db-volume` at `/data` so the SQLite file — and the
  one-off backfill script's read access to it — survive container
  recreation. Root `.env`: `CUSTOM_PROVIDER_URL=http://frontend:8080`,
  `TARGET_APP_URL=http://localhost:9090/`. Root `docker-compose.yml`:
  `name: priam-onlineboutique`.

## 2. Deliberate scope decisions

- **`Product Recommendations` has zero `data_usage` rows.** It genuinely
  doesn't touch any of the annotated `User`/`Order` fields — it only reads
  the shopping cart's product ids, which are not modeled as personal data
  in this annotation (same treatment TeaStore gave its `OrderItem`
  equivalent). Fabricating a `data_usage` link to make the category table
  below look fuller would contradict every other annotation decision in
  this repository, which is built specifically on verifying against real
  code rather than assuming/inventing.
- **No pre-seeded consent.** The seed subject's `OPTIONAL` consent is
  granted/withdrawn/re-granted for real during testing (§5), not
  pre-inserted — since it has no `data_usage` rows, pre-seeding it would
  exercise none of the §8.1.b bookkeeping anyway.
- **Automatic Keycloak provisioning at sign-up was NOT implemented in this
  session** (see §5, Known limitations, at the time) — a real, deliberate
  scope cut given the time already spent proving the core rights/consent
  workflows, not an oversight. One Keycloak account was provisioned
  **manually** via the Admin API, bound to the real seed `idRef`, purely so
  this session could obtain a real JWT and exercise the authenticated
  `/right`/`/cdp` Gateway routes exactly as a browser session would. **This
  gap was closed in a follow-up, after this session — see §7.**
- **No lines changed in PRIAM's own microservices/frontends** — confirmed
  (`git status --porcelain -- PRIAM-Services PRIAM-Frontend
  PRIAM-Frontend-Provider` returns empty). No generic PRIAM bug was found
  this session, so `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8 was not touched.

## 3. Bugs found and fixed, this session

| # | Root cause | Symptom | Fix | Proof of verification |
|---|---|---|---|---|
| 1 | `accounts_handlers.go`'s `signupHandler`/`loginHandler` set the `shop_user-id` cookie with no explicit `Path` — RFC 6265 default-path then scopes it to `/accounts` (the setting request's own directory), not site-wide. | A signed-in user looked signed-out (`currentUserID(r) == ""`) on every page outside `/accounts/*` — cart, checkout, home. This would have silently broken `placeOrderHandler`'s user↔order linkage and the entire PRIAM registration/report pipeline in any real browser session, since real navigation always crosses out of `/accounts` right after sign-up. | Added `Path: "/"` to both cookie-setting calls (`accounts_handlers.go`). | Reproduced with curl (cookie jar showed `Path=/accounts`, cart page showed `Log In`/`Sign Up` instead of `Log Out` for a just-signed-up account); after the fix, a fresh login→cart flow correctly showed `Log Out`/`Manage on PRIAM`, and `placeOrderHandler`'s `currentUserID(r)` correctly resolved outside `/accounts` — see ETAPES-FAITES.md. |
| 2 | `priam_provider.go`'s `priamEraseOrder` deleted `orders` **before** `order_items`, but `order_items.order_id REFERENCES orders(order_id)` and `store.go` sets `PRAGMA foreign_keys = ON`. | Any real erasure request approved (`answer=true`) for an `Order` field failed with a `500` from `PRIAM-Right-service` and the order was **not** actually deleted — exactly the "a 200/FULL proves nothing" pitfall the playbook (§7) warns about, caught only by reading the target application's real database after the call. | Reordered the transaction: delete `order_items` first, then `orders`. | `POST /api/erasure` directly returned `{"error":"constraint failed: FOREIGN KEY constraint failed (787)"}` before the fix; after rebuilding and redeploying, a full `answer=false`→`answer=true` cycle through the real `PRIAM-Right-service` (`dataRequestId` 6→7) deleted both the `orders` and `order_items` rows for real — see ETAPES-FAITES.md. |
| 3 | `frontuser` (PRIAM-Frontend) has no fixed Docker `image:` tag by design (its build genuinely differs per case study via the `TARGET_APP_URL` build arg) — but `docker compose up -d frontuser` without `--build` silently reused a 2-day-old cached image tagged from the very first OnlineBoutique session, baked with a stale/absent `targetAppUrl`. | PRIAM-Frontend's "back to the app" link would have pointed at a stale or empty target, exactly the playbook §5/§8.9 "stale image" pitfall. | `docker compose build frontuser` explicitly, then `up -d --force-recreate frontuser`. | `curl http://localhost:4200/main.js \| grep 9090` returned nothing before the rebuild, `targetAppUrl: 'http://localhost:9090/'` was visible in the rebuilt bundle afterward. |
| 4 | Environment, not application code: OnlineBoutique's natural port (8080) collides with PRIAM's fixed Keycloak port, and the next candidate (8081) collides with `PRIAM-Data-service`. | `docker compose up` failed with `port is already allocated`. | Published OnlineBoutique's `frontend` on host port **9090** instead (container-internal port, and `CUSTOM_PROVIDER_URL`'s Docker-internal address, are unaffected). Documented in `docker-compose.yml`/`.env` comments. | Full stack came up cleanly on the next attempt; `TARGET_APP_URL`/browser tests below all use `:9090`. |
| 5 | `case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh`'s `set -eu`, combined with `docker run` calls inside the loop, aborted the script silently after the very first command of the first iteration — reproduced repeatedly under this session's Windows/Cygwin `sh`, never fully root-caused (every individual command exits 0 in isolation). Not a PRIAM bug — the script's own shell portability issue. | The backfill script appeared to do nothing beyond registering the first subject, with no error message. | Dropped `-e` (kept `-u`); switched the `echo … \| while read` loop to a `for` loop (a smaller, independently-worthwhile portability fix, though not what actually resolved the `-e` issue). | Full re-run (`sh backfill-data-subjects.sh`) completed both real subjects, printing every step and exiting 0; real `nb_occurrences` bookkeeping confirmed in MySQL afterward — see ETAPES-FAITES.md. |

## 4. Workflows verified against real state

| Workflow | Real proof captured |
|---|---|
| Access request (`answer=true`) | `isAccepted` flipped `false→true`; `GET personalDataValues/accessRight` returned the real `users.email` value from SQLite. |
| Rectification, `answer=false` | SQLite `orders.street_address` unchanged after refusal. |
| Rectification, `answer=true` | SQLite `orders.street_address` changed from `42 Rue de la Paix` to `99 Avenue du Test` — the real Provider bridge call, triggered automatically by `PRIAM-Right-service`, not by curling the bridge directly. |
| Erasure, `answer=false` | Order row still present in SQLite (`orders`/`order_items`) after refusal. |
| Erasure, `answer=true` | Order row and its items both gone from SQLite after approval (bug #2 above found and fixed in the course of this exact test). |
| Consent grant | Real `consent` row (`end_date=NULL`) in `priam-consent`; CDP decision flipped to `true`; the app's own `/cart` page started rendering the "You May Also Like" block for the signed-in subject — a genuine observable side effect, not just an API response. |
| Consent withdrawal | `consent.end_date` set; CDP decision flipped to `false`; "You May Also Like" disappeared from `/cart`. |
| Consent re-grant | A **new** `consent` row created (`ConsentServiceImpl.create`'s documented case-1b behavior); decision flipped back to `true`; block reappeared. |
| Registration + forced redirect (real-time, not backfill) | A brand-new `/accounts/signup` call for a never-seen idRef created a real `data_subject` row (id `2`), reported `processed_data` for `User.email`, and the signup response `302`'d straight to `http://localhost:4200/consent` — no manual step involved. |
| One-off backfill | Ran successfully against real state (see bug #5); in this integration both real accounts had already been registered in real time by the signup hook before the script ran, so it is a correctness demonstration/idempotency check rather than a gap-filling run — there were no genuinely pre-hook accounts to catch up on. |

All of the above were driven through the real `PRIAM-Right-service`/
`PRIAM-Consent-Service` HTTP contract (never the Provider bridge directly,
except the one intentional direct call used to confirm bug #2's fix before
re-running the full workflow), using a real, authenticated JWT
(`idReference` claim decoded and matched against the seed idRef) obtained
from a Keycloak account provisioned specifically for this test — see
ETAPES-FAITES.md for every request/response and every `SELECT`.

**Real browser interaction**: this session's own testing was curl-driven —
no browser-automation tool was available, so rendering, CORS, and the
`OPTIONS` preflight were not directly exercised by this agent. Separately,
real traffic was observed in the database from what appears to be a genuine
browser session (a third `data_subject` row, `id_ref` resolving to a real
account `lam@gmail.com` in `onlineboutique.db`, created outside of any
command run in this session) — consistent with, but not a substitute for,
an explicit browser test. **Do not treat this integration as having been
validated from a real browser by this agent**; the curl-based proof above
is real end-to-end state verification, but rendering/CORS/preflight
specifically remain unverified by this agent.

## 5. Known limitations

- **~~No automatic Keycloak provisioning at sign-up~~ — implemented in a
  follow-up, see §7.** OnlineBoutique now has local sign-up and Keycloak is
  wired up globally (§6), so per the playbook's own guidance this would
  normally be implemented (`provision_keycloak_user`, playbook §4bis). It
  was deliberately not built in this session — the "Manage on PRIAM" link
  led to a Keycloak identity that had no relation to a user's real
  OnlineBoutique account unless that account happened to match the one
  manually provisioned for testing (`priam-seed@example.com`). This was
  the single largest remaining gap versus a fully "production-ready"
  integration at the time this section was written; §7 closes it, but has
  **not** been verified against a live stack the way the rest of this
  report was (no Docker/Go toolchain was available in the follow-up
  session that wrote it) — treat §7 as unverified until someone runs it.
- **Guest checkouts are entirely outside PRIAM's view.** By design (no
  idRef to register), matching the fact that upstream OnlineBoutique's
  guest flow needs no account at all.
- **`order_items` (product_id/quantity/cost) is not annotated** — it
  carries no personal data of its own, only a product reference and a
  price/quantity.
- **Real browser rendering not exercised by this agent** (see §4).
- **`priam-databases`/`priam-api-gateway` etc. were previously running a
  different case study (TeaStore)** in this same shared repository
  checkout; that stack's uncommitted work (SQL annotation, `.env`,
  MySQL data) was preserved (`case-studies/TeaStore/priam-integration/`,
  `db-volume-teastore-backup/`) rather than discarded, per explicit user
  direction, but was not re-verified after this session's changes.

## 6. LOC breakdown

**Method**: per-file line counts via `wc -l` / direct read; code vs.
comment vs. blank classified with a small Python script
(`scratchpad/loc_count.py`, single-line-prefix heuristic — `//` for Go,
`--` for SQL, `#` for shell/YAML; HTML/Go-template files have no
comment-syntax applied, so every non-blank line counts as code) applied to
files authored entirely in this session. For files that mix this session's
PRIAM-integration hunks with pre-existing "account persistence" groundwork
(added in a prior, separate session — see §0), the hunks I personally wrote
this session were counted by hand from the exact edits applied (not by
running the classifier over the whole mixed file) — `git diff --numstat`
against `HEAD` is also shown per tracked file for raw transparency, but
note it is **not** the same number as "this session's contribution" for
those mixed files, since `HEAD` predates the account-persistence groundwork
too.

### Per-file table

| File | Status | +lines (session) | -lines (session) | vs. `HEAD` (`git diff --numstat`, includes pre-existing groundwork) |
|---|---|---|---|---|
| `Databases/db_insertion_script.sql` | modified (full rewrite) | 207 (full file, this session) | 133 (old Cart annotation removed) | +159 / -133 |
| `case-studies/OnlineBoutique/src/frontend/priam.go` | new | 143 | 0 | *(untracked until now)* |
| `case-studies/OnlineBoutique/src/frontend/priam_provider.go` | new | 380 | 0 | *(untracked until now)* |
| `case-studies/OnlineBoutique/docker-compose.yml` | new (recreated, previous version deleted) | 202 | 0 | *(untracked until now)* |
| `case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh` | new (recreated, previous version deleted) | 91 | 0 | *(untracked until now)* |
| `case-studies/OnlineBoutique/src/frontend/main.go` | modified | 0 (see correction below) | 0 | +10 / -0 |
| `case-studies/OnlineBoutique/src/frontend/rpc.go` | modified | 13 | 0 (+3 lines touched in-place at call sites, not net new) | +11 / -7 |
| `case-studies/OnlineBoutique/src/frontend/handlers.go` | modified | 15 | 0 (+3 lines touched in-place) | +26 / -27 |
| `case-studies/OnlineBoutique/src/frontend/accounts_handlers.go` | modified (file itself new this repo state, but its base signup/login/orders logic predates this session) | 48 | 0 | *(untracked until now — whole file is 210 lines, ~44% mine)* |
| `case-studies/OnlineBoutique/src/frontend/templates/header.html` | modified | 3 | 0 | +7 / -0 |
| `.env` (root, gitignored — not tracked) | modified | 9 (current size of the two sections touched) | n/a (not version-controlled) | n/a |
| `docker-compose.yml` (root) | touched, reverted to `HEAD` | 0 | 0 | 0 / 0 |
| `case-studies/OnlineBoutique/src/frontend/go.mod` | modified (dependency fix: added missing `modernc.org/sqlite`) | *(excluded from LOC classification — see method)* | | +11 / -5 |
| `case-studies/OnlineBoutique/src/frontend/go.sum` | modified (generated lockfile) | *(excluded from LOC classification — see method)* | | +47 / -16 |

Not counted as this integration's LOC (pre-existing groundwork from the
separate "changes for data persistence" session, predating this
integration): `store.go` (253 lines, new), the base signup/login/orders
logic in `accounts_handlers.go` (~162 of its 210 lines),
`templates/login.html` (75), `templates/signup.html` (82),
`templates/orders.html` (63), `validator.go`'s `SignupPayload`/
`LoginPayload` (19 lines), and — **correction below** — `main.go`'s
entire diff (10 lines).

**Correction to an earlier draft of this table**: `main.go`'s diff
(`cookieUserID` constant, the `initStore()` call, and the 5
`/accounts/*` route registrations) was initially counted as 8 lines of
this session's own Rights-API work. Re-checked against `git diff --
case-studies/OnlineBoutique/src/frontend/main.go`: every one of those 10
lines is unchanged from the "changes for data persistence" session — this
integration added zero lines to `main.go` (the 4 real Provider-bridge
routes, `/api/dataAccessRight` etc., were already present at `HEAD` from
the original `76ab7a6` integration and were not touched this session
either). `main.go` is moved to the pre-existing-groundwork list above, and
the **Rights-API** row in the category table below is corrected from
`318 | 46 | 24 | 388` to `310 | 46 | 24 | 380` (`380` now equals
`priam_provider.go`'s own line count exactly, with nothing double-counted
from `main.go`); the **Total (classified)** row is corrected accordingly
from `693 | 355 | 71 | 1119` to `685 | 355 | 71 | 1111`.

### By functional category × by line nature

| Category | Code | Comment | Blank | Total |
|---|--:|--:|--:|--:|
| **Annotation** (SQL script) | 48 | 146 | 13 | 207 |
| **Rights-API** (`priam_provider.go`; the 4 real Provider-bridge routes in `main.go` are unchanged from the prior `76ab7a6` integration, not this session's work) | 310 | 46 | 24 | 380 |
| **Consent** (`priam.go`, CEP gate in `rpc.go`, registration/redirect/report hooks in `accounts_handlers.go`+`handlers.go`, "Manage on PRIAM" link, backfill script) | 182 | 108 | 23 | 313 |
| **OAuth2** (§7 follow-up: `provisionKeycloakUser`/`fetchKeycloakAdminToken` in `priam.go`, the `signupHandler` call site, `docker-compose.yml`'s `KEYCLOAK_*` env vars — **added after this session, unverified**, see §5/§7) | 76 | 37 | 9 | 122 |
| **Docker-network** (`docker-compose.yml` new + `.env` sections + root `docker-compose.yml`) | 145 | 55 | 11 | 211 |
| **Total (classified)** | **761** | **392** | **80** | **1233** |

**OAuth2**: 0 during this session itself (§5, deliberate scope decision at
the time) — the Keycloak Admin API calls used for testing (§7 of
ETAPES-FAITES.md) were one-off `curl` commands, not committed code. The 122
lines now in this row were added in a **separate follow-up after this
session** (§7 below) — counted here so the category table reflects the
current state of the repository, not left permanently at 0 once the gap
this same section flagged was actually closed.

### PRIAM's own LOC (for this case study)

**0** across every category and every line-nature bucket — confirmed via
`git status --porcelain -- PRIAM-Services PRIAM-Frontend
PRIAM-Frontend-Provider` (empty output). No PRIAM microservice or frontend
source file was modified in this session, per the non-negotiable
constraint (§0/§2 above).

## 7. Follow-up: automatic Keycloak provisioning at sign-up

Added after the session described in §1-§6 above, in direct response to
§5's "single largest remaining gap." **Written without a Go toolchain or a
running Docker daemon available** (same constraint disclosed in
"changes for data persistence.md") — unlike every workflow in §4, **nothing
below has been run against a live stack**. Treat it as a plausible,
carefully-cross-checked-against-the-playbook implementation, not as a
verified one, until someone with a working Docker environment runs it.

### What was added

- `priam.go`: `provisionKeycloakUser(idRef, email, password)` and
  `fetchKeycloakAdminToken()`, following the playbook §4bis pattern
  exactly: fail-open (no-op) if `KEYCLOAK_ADMIN_URL` is unset, obtains an
  admin token via Direct Grant against Keycloak's built-in `admin-cli`
  client (the same flow ETAPES-FAITES.md §1.7 already exercised manually
  with curl), `POST`s the new user with `username`/`email`/`firstName`/
  `lastName` all set to the account's email (§4bis's two documented
  pitfalls: usernames under Keycloak's minimum length, and missing
  required User Profile attributes both silently break Direct Grant login
  later, not creation itself), and the `idReference` custom attribute set
  to the same `idRef` `registerDataSubject` already uses — the realm
  already declares this attribute (confirmed working in ETAPES-FAITES.md
  §1.7's `GET .../users/profile` output), so no realm change was needed
  here. A `409 Conflict` (already provisioned) is treated as success, per
  §4bis "idempotent by construction." Never blocks or fails sign-up: every
  error path only logs.
- `accounts_handlers.go`'s `signupHandler`: `go provisionKeycloakUser(id,
  payload.Email, payload.Password)`, right after the existing
  `registerDataSubject`/`reportProcessedData` calls — a goroutine is safe
  here (unlike those two, which have the documented §8.6 ordering
  constraint against each other) since nothing downstream depends on
  Keycloak provisioning completing first. `payload.Password` is the
  plaintext form value — the only point in the whole request where it is
  available; `store.go`'s `createUser` (called earlier in the same
  handler) has already bcrypt-hashed it for local storage by this point.
- `docker-compose.yml` (this case study's, not PRIAM's root one):
  `KEYCLOAK_ADMIN_URL: http://keycloak:8080` (the Docker-internal address
  on `common_network` — `frontend` is already attached to it, confirmed by
  reading this same file's `frontend.networks:` list — not the
  host-mapped `localhost:8080` a browser uses), `KEYCLOAK_REALM:
  priam-realm`, `KEYCLOAK_ADMIN_USERNAME`/`KEYCLOAK_ADMIN_PASSWORD: admin`
  (PRIAM root `docker-compose.yml`'s own `keycloak` service bootstrap
  admin — reused per §4bis's own documented dev/test tradeoff; a
  dedicated `manage-users` client is preferable before any real exposure).

### What was deliberately not (re-)done

- **The realm's `idReference` User Profile declaration was not touched.**
  ETAPES-FAITES.md §1.7 already confirmed it exists on `priam-realm` (left
  over from an earlier case study) — re-declaring it would be redundant,
  and this integration has no code that manages realm configuration in the
  first place (out of scope, same as every other case study).
- **No dedicated Keycloak client with a `manage-users` role was created.**
  The playbook itself flags the shared super-admin bootstrap account as a
  "development/testing" tradeoff, not a production one; swapping it for a
  scoped client is a follow-up of its own, not bundled into this one to
  keep the change reviewable.
- **Social-provider sign-up remains uncovered**, per the playbook's own
  stated limit of this exact pattern (§4bis) — moot here since
  OnlineBoutique has no social sign-up at all.

### What still needs to happen before this can be trusted

1. **A real build.** `docker compose build frontend` has not been run
   against this addition — `priam.go`'s two new functions reuse only
   symbols already imported by the rest of that file
   (`net/http`, `net/url`, `encoding/json`, `bytes`, `io`, `fmt`, `os`), so
   no new Go dependency was introduced, but that is not a substitute for
   an actual compile.
2. **A real sign-up against a live stack**, checking:
   - `docker logs ob-frontend` for `priam: provisionKeycloakUser(...)`
     failure lines (wrong admin credentials, realm typo, network reachability).
   - `GET /admin/realms/priam-realm/users?email=...` (with a fresh admin
     token) actually returns the new user, with `idReference` present
     (not silently stripped — §4bis's own documented pitfall class).
   - A real Direct Grant login (`grant_type=password`) against the
     newly-provisioned account succeeds (catches the "Account is not
     fully set up" failure mode §4bis warns is otherwise silent).
   - Clicking "Manage on PRIAM" right after a fresh sign-up lands on a
     Keycloak login that accepts the same email/password just chosen.
3. Once verified, update §5's "unverified" caveat and this section's
   opening paragraph to reflect a real pass/fail, with the same kind of
   real-state proof (`curl` + DB/API state) as §4/ETAPES-FAITES.md — not
   just "it compiled."
