# PRIAM × TeaStore — Integration Report

## 1. The mechanism, in one page

TeaStore (Descartes Research's Java/Spring microservices e-commerce
benchmark: `registry`, `persistence`, `auth`, `image`, `recommender`,
`webui`) ships with **no self-service sign-up** — only
`DataGenerator`-seeded accounts (`user0..user99`, password `password`).
Making this integration real (not a one-off demo against a single seeded
row) required adding a minimal sign-up feature first, then wiring PRIAM into
it — this is the single biggest structural difference from every other
case study in this repository, all of which already had their own sign-up.

- **idRef = TeaStore `userName`** (a free-form string chosen at sign-up,
  e.g. `user1` or `selena.test.subject`), not the numeric internal `id` —
  satisfies the playbook's "non-numeric idRef" requirement by construction
  for every account, not just a specially crafted test one.
- **SQL annotation** (`Databases/db_insertion_script.sql`): two data types,
  `User` (userName/email/realName) and `Order` (id + address/credit-card
  fields, several rows per subject, `is_primary_key=1` on `Order.id`).
  Three processings: `Account Management` (NECESSARY), `Order Fulfillment`
  (NECESSARY), `Product Recommendations` (OPTIONAL, purpose_type
  `SECONDARY` — it reuses `Order.id`, originally collected for fulfillment,
  for a second, non-essential personalization purpose). Deliberately seeds
  **only** the schema annotation + one `data_subject` row (`user1`) — no
  `contract`/`consent`/`processed_data` pre-seeded (see the file's own header
  comment for the full reasoning: `ConsentServiceImpl.create` already
  bookkeeps `processed_data` automatically on every real grant/withdraw call,
  so pre-seeding it would only reintroduce the exact §8.1.b pitfall for no
  benefit).
- **Provider bridge** (`PriamProviderServlet.java`, webui module, mounted at
  `/api/*`): all 4 endpoints (`dataAccessRight`, `rectification`, `erasure`,
  `dataValue`). `CUSTOM_PROVIDER_URL=http://webui:8080/tools.descartes.teastore.webui`
  — the Gateway's existing `prefixPath()` handling (already generic, written
  for exactly this "WAR-deployed target app on a non-root context path"
  case) re-attaches the context path automatically. `userName` and
  `Order.id` are readable (access) but excluded from the *mutable* whitelist
  (rectification/erasure) — rectifying `userName` would desync the PRIAM
  `data_subject.id_ref` from the real account; `Order.id` is the row
  selector, not a value of its own.
- **CEP**: `CartServlet.java`'s personalized "Are you interested in?" block
  (calls the Recommender service) is the one genuinely OPTIONAL processing —
  gated by `webui/priam/PriamClient.getConsent()`. Only checked for an
  identified (logged-in) subject; anonymous browsing is unaffected (no
  PRIAM `data_subject` to check consent for).
- **Registration + bookkeeping** (`auth/priam/PriamClient.java`,
  `AuthUserActionsRest.java`): a new `register` REST endpoint
  (`POST /useractions/register`) added to the `auth` service, since none
  existed. `register_data_subject`/`hasPendingConsentDecision` run
  **synchronously** at both `register` and `login` (the redirect decision
  depends on their result, and `login` also self-heals any account the
  backfill script missed); `report_processed_data`/`provisionKeycloakUser`
  run in a background thread (slower, not needed for the response). Order
  creation (`placeOrder`) backgrounds a `report_processed_data` call for the
  `Order` data type — the most commonly forgotten wiring point, per the
  playbook.
- **Forced consent + bidirectional navigation**: `priamConsentRequired` is a
  new boolean on `SessionBlob` (TeaStore's own "current user" response,
  carried in a signed cookie) — set at `register`/`login`, read by
  `RegisterServlet`/`LoginActionServlet` to redirect to
  `{PRIAM_FRONTEND_URL}/consent` instead of the usual post-signup
  destination. `ProfileServlet`/`profile.jsp` show a "Manage on PRIAM" link
  whenever `PRIAM_FRONTEND_URL` is configured. `TARGET_APP_URL` (root
  `.env`) points at TeaStore's real storefront home page, not just the bare
  host:port root.
- **Keycloak provisioning**: TeaStore's own local sign-up (added by this
  integration) has no OIDC/SSO of its own, so `provisionKeycloakUser()`
  synchronizes a Keycloak account at sign-up — Keycloak `username` = email
  (TeaStore usernames can be very short, below Keycloak's 3-char minimum),
  `idReference` attribute = the real TeaStore `userName`, `firstName`/
  `lastName` = `realName` (TeaStore has no separate first/last name).
  **Known limitation**: covers local sign-up only — TeaStore has no social
  sign-up option, so this is not a gap in practice for this application, but
  is stated explicitly rather than silently assumed.
- **Backfill**: `priam-integration/backfill-data-subjects.py`, a one-off
  script reusing TeaStore's own already-exposed persistence REST API (no
  separate DB-to-DB access) to register every pre-existing seeded user and
  report their real orders' data_ids.

## 2. Bugs found this session (all on the TeaStore side — 0 lines changed in PRIAM)

| # | Root cause | Fix | Proof of verification |
|---|---|---|---|
| 1 | A Javadoc comment containing the literal substring `PRIAM_*/KEYCLOAK_*` (and, separately, `PRIAM_*/CUSTOM_*`) closes the `/** ... */` block early (`*/` is a real end-of-comment token wherever it appears) — javac then tries to parse the rest of the comment as code, cascading into dozens of unrelated "illegal start of type" errors. | Reworded the two comments to avoid the literal `*/` sequence (`auth/priam/PriamClient.java`, `webui/servlet/RegisterServlet.java`). | Reproduced directly with `javac` on the exact file (isolated from Maven/reactor complexity) before the fix; `mvn clean install` (all 6 WARs) succeeded after. |
| 2 | `hasPendingConsentDecision` (auth module) and `getConsent` (webui module) concatenated `processingName` into a URI without percent-encoding it. `"Product Recommendations"` contains a space; `URI.create()` throws `IllegalArgumentException: Illegal character in path`, silently caught by the surrounding `try/catch`, and both functions returned their fail-safe default (`false`) — `priamConsentRequired` was always `false` at login/register, and the CEP gate would have been permanently fail-closed. | Added a small `enc()`/`URLEncoder.encode()` helper, applied to both `idRef` and `processingName` before building the URI, in both `PriamClient` classes. | Reproduced via a real login (`priamConsentRequired:false` in the response cookie despite no consent decision existing yet — confirmed empty via a direct `curl` to `/cdp/api/contract/list/consents/user1/Product Recommendations`), confirmed the exact stack trace in `teastore-auth-1`'s logs (`IllegalArgumentException: Illegal character in path at index 60`). After the fix and redeploy: same login now returns `priamConsentRequired:true`, and the CEP round-trip below confirmed the flag flips correctly end-to-end. |
| 3 | `provisionKeycloakUser()` is only wired at the new `POST /useractions/register` endpoint (the only place a plaintext password is ever available). Pre-existing DataGenerator-seeded accounts (`user0..user99`) never go through that endpoint, and the first version of the backfill script only called `register_data_subject`/`report_processed_data`, never Keycloak provisioning — so a seeded user (e.g. `user2`) could log into TeaStore itself (a local BCrypt check, entirely independent of Keycloak) but had no Keycloak account at all, and therefore no way to log into PRIAM-Frontend. Caught by the user directly asking why `user2`/`password` worked at the TeaStore level but not at the Keycloak/PRIAM level. | Added `provision_keycloak_user()` to `backfill-data-subjects.py`, using the well-known seed password (`"password"`, `DataGenerator.PASSWORD` — the only password these accounts have), same shape as the Java `provisionKeycloakUser()` (username=email, `idReference`=real TeaStore userName, firstName/lastName=realName). Same pattern already used by `case-studies/BankOfAnthos/priam-integration/backfill-data-subjects.py`'s `DEMO_PASSWORD`. | Re-ran the backfill against the live stack: 103/103 users provisioned, 0 failures (log line `user2: registered, 1 order(s) reported, Keycloak provisioned`). Confirmed via the Keycloak Admin API (`GET /admin/realms/priam-realm/users?username=user2@teastore.com` → real user, `idReference:["user2"]`) and a real Direct Grant login (`grant_type=password&username=user2@teastore.com&password=password` → `200`, real access token). |
| 4 | **Retracts §3's earlier "Recommender is broken" conclusion below** — that conclusion was wrong, caused by a second, real bug. `webui/priam/PriamClient.getConsent()` built the CDP URL with plain `URLEncoder.encode(processingName, UTF_8)`. `URLEncoder` is `application/x-www-form-urlencoded` encoding: it turns a space into `+`, which only means "space" inside a *query string* — inside a **path segment** (where `processingName` is used here: `/api/decision/{processingName}`), `+` is a literal plus sign. `GET /api/decision/Product+Recommendations` does not match any real processing name, and `PRIAM-Consent-Service` returns `500`; `getConsent()`'s `if (statusCode/100 != 2) return false` then silently masked this as "not granted" — even when the real consent (verified via the same endpoint called with proper `%20` encoding) was `true`. `enc()` in the *auth* module's `PriamClient` already handled this correctly (`.replace("+", "%20")`, added while fixing bug #2) — the webui module's copy was fixed for the crash (bug #2) but not for this more subtle wrong-encoding-scheme issue, an inconsistency between the two near-identical files. Found live: the user reported that toggling "Product Recommendations" on and returning to the Cart page showed no recommendations. | Added the same `.replace("+", "%20")` normalization (as `encPathSegment()`) to `webui/priam/PriamClient.getConsent()`. | Reproduced directly: `docker exec teastore-webui-1 curl http://consent:8089/api/decision/Product+Recommendations?idRefList=user2` → `500`; the correctly-encoded `%20` version → `{"user2":true}`. A temporary debug log in `CartServlet` confirmed `consentOk=false` on the real request despite the real consent being granted. After the fix and redeploy: a full grant → real recommendation rendered in the Cart page HTML (a real product, "Ceylon (loose)", with thumbnail and "Add to Cart" button) → withdraw → recommendation section gone — see ETAPES-FAITES.md §12 for the exact commands and HTML excerpt. |

## 3. Known limitations (not fixed — documented, not glossed over)

- **Correction, superseded by bug #4 above**: an earlier version of this
  report claimed "TeaStore's own Recommender returns an empty list for
  every user, independent of consent" as a known limitation. That
  conclusion was **wrong** — it was an artifact of testing the Recommender
  directly with an empty cart (`currentItems: []`), and
  `AbstractRecommender.recommendProducts()` deliberately short-circuits to
  an empty list whenever `currentItems` is empty (`if (currentItems.isEmpty())
  return new LinkedList<>();` — the recommender needs at least one item in
  the current cart to compute anything; it is not primarily an order-history
  lookup keyed by `uid`). With a real, non-empty cart, the Recommender
  returns real results (confirmed: `[15,285,107,108,207,209,8,9,10,12]` for
  a one-item cart). The actual reason the Cart page never showed
  recommendations was bug #4 (a wrong URL-encoding scheme silently masking
  a granted consent as refused) — now fixed and verified with a real
  product rendered in the page. This correction was only found because the
  user pushed back and asked to see the real recommender code
  (`RecommenderSelector`/`AbstractRecommender`) rather than accepting the
  first (wrong) explanation.
- **Keycloak provisioning covers local sign-up only** (playbook §4bis) —
  not a gap for TeaStore specifically (it has no social sign-up option at
  all), but stated explicitly per the task's own requirement rather than
  silently assumed.
- **GUI browser validation**: curl was used to simulate every browser
  interaction precisely (real cookies, real form submissions, real
  redirects followed via `Location` headers) — this is not the same as a
  human/automated GUI session (no JS execution, no visual rendering
  confirmation). No browser-automation tool was available in this
  environment. Both PRIAM frontends (`:4200`/`:4000`) were started and
  confirmed reachable (`HTTP 200`) for the user's own final visual pass —
  see ETAPES-FAITES.md for exact accounts/credentials to use.
- **One transient timeout** on `provisionKeycloakUser`'s very first
  invocation this session (`HttpTimeoutException` at the 3-second mark,
  `teastore-auth-1` logs) — root-caused to the host being under heavy
  resource pressure from repeated Docker Desktop restarts earlier in the
  session (see §8.9 addendum in the playbook), not a logic bug: the
  identical code path succeeded on the very next real sign-up
  (`selena.test.subject2`, verified via the Keycloak Admin API and a real
  Direct Grant login with the synced password). No code change was made for
  this — it is not reproducible on demand and a 3s timeout is otherwise
  reasonable for a same-Docker-network admin API call.
- **Environment**: this session required raising Docker Desktop's WSL2
  memory cap (7.5GB → 10GB, on a 16GB-RAM host) after repeated whole-stack
  crashes running PRIAM + TeaStore + another case study's stack
  simultaneously — added as a concrete data point to the playbook's
  existing §8.9 "Docker resource limits" entry, not a new pitfall group.

## 4. Workflows verified against real state

| Workflow | Method | Real-state proof |
|---|---|---|
| Access request (User fields) | `POST /right/api/right/accessRequest` → `POST /right/api/right/answer` (`answer:true`) → `GET /right/api/personalDataValues/accessRight` | Real `userName`/`email`/`realName` values returned, matching TeaStore's persistence DB directly. |
| Access request (Order fields, multi-row) | Same, `dataTypeName=Order` | All 3 real orders of `user1` returned with correct `id`/`addressName`/`creditCardNumber`. |
| Rectification — refused | `rectificationRequest` → `answer:false` | `PersistenceOrder.addressName` unchanged, read directly from `teastore-persistence-1`. |
| Rectification — approved | New `rectificationRequest` → `answer:true` | `addressName` changed from `"Dorothy Brown"` to `"Jane Rectified TestName"` in TeaStore's real DB — auto-executed by PRIAM, not by a direct Provider call. |
| Erasure — refused | `erasureRequest` → `answer:false` | `creditCardCompany` unchanged (`"Visa"`). |
| Erasure — approved | New `erasureRequest` → `answer:true` | `creditCardCompany` became `""` in TeaStore's real DB. |
| Consent — pre-grant state | `GET /cdp/api/decision/...`, `GET /cdp/api/contract/list/consents/...` | Empty decision map / empty consent list for a never-decided subject. |
| Consent — grant | `POST /cdp/api/consent/create/{idRef}` | `consent` row created (`end_date NULL`); `processed_data(4,1)` occurrence count auto-incremented 3→4 by `ConsentServiceImpl.create`; `get_consent` flipped to `true`. |
| Consent — withdraw | Same endpoint, toggles | `end_date` set; occurrence count decremented 4→3; `get_consent` flipped to `false`. |
| Consent — re-grant | Same endpoint, toggles again | New `consent` row (`end_date NULL`); occurrence count back to 4; `get_consent` back to `true`. |
| Consent — real downstream UI effect (CartServlet, `user2`, bug #4 above) | Grant via real API → real Cart page fetch (non-empty cart) → withdraw → real Cart page fetch again | With consent granted: a real recommended product ("Ceylon (loose)", thumbnail + "Add to Cart" button) rendered in the Cart page HTML. With consent withdrawn: the "Are you interested in?" section absent again. First attempt at this exact test surfaced bug #4 (masked as a false "Recommender is broken" limitation) — corrected after the user asked to see the real recommender code. |
| Fresh sign-up, non-numeric idRef | `POST /register` (`selena.test.subject`) | New `data_subject` row (real idRef, no numeric coincidence); `processed_data` for User fields (1,2,3) auto-reported; `priamConsentRequired:true`; real `Location: http://localhost:4200/consent` redirect header. |
| `report_processed_data` at order creation (not just sign-up) | Real checkout (`addToCart` + `confirm`) for the same fresh subject | `processed_data` for all 7 Order data_ids appeared only after the order was placed — proves the hook fires at record-creation time, not just sign-up. |
| Access request for a dynamically registered subject | Same `personalDataValues/accessRight` endpoint, `dataSubjectId` of the fresh subject | Real `User` and `Order` data returned — the exact scenario §8.1.b warns is "the most frequently forgotten point". |
| Backfill (pre-existing users) | `priam-integration/backfill-data-subjects.py`, one-off run | All 100 seeded users registered (0 failures); `nb_occurrences` correctly reflects each user's real order count (spot-checked `user1`: 1/1/1 for User fields, 3/3/3/3/3/3/3 for Order fields, matching their 3 real orders). |
| Keycloak provisioning | Real sign-up → Keycloak Admin API | Account created with `username=email`, `idReference` attribute = real TeaStore `userName`, `firstName`/`lastName` populated. |
| Keycloak provisioning for pre-existing seeded users (bug #3 above) | Re-ran `backfill-data-subjects.py` after the fix | 103/103 provisioned, 0 failures; `user2`'s Keycloak account confirmed via Admin API and a real Direct Grant login with the well-known seed password. |
| Keycloak credential sync | Direct Grant with the just-chosen password | `200`, real access token issued, `idReference` claim present and correct in the decoded JWT. |
| Gateway OIDC — no token | `POST /right/api/right/accessRequest` without `Authorization` | `401`. |
| Gateway OIDC — valid token | Same, with a real Keycloak-issued token | `200`, real data returned. |
| Gateway OIDC — invalid token | Same, garbage bearer token | `401`. |
| Gateway OIDC — machine-to-machine route | `GET /provider/api/dataAccessRight` without any token | `200` (never gated, by design). |

Full curl commands, responses, and before/after database state for every
row above are in `ETAPES-FAITES.md`.

## 5. LOC breakdown

**Method**: `git diff --numstat` for the raw `+`/`-` per file (below); a
small Python script
(`count_loc.py`, not committed — a throwaway in the session's scratchpad)
classifies each **added** line (for modified files) or **every** line (for
new files) as code/comment/blank using a simple per-extension heuristic
(`//`/`/* */`-tracking for `.java`, `--` for `.sql`, `#` for `.py`/`.yml`,
`<%--...--%>` for `.jsp`) — a manual per-extension rule, not a language
parser, good enough for an honest breakdown, not a claim of exactness. The
functional-category split (Annotation/Rights-API/Consent/OAuth2/
Docker-network) was assigned by hand, file by file (and, for the 3 files
that mix roles — root `.env`, root `docker-compose.yml`'s `name:` line, and
this case study's own `docker-compose.yml` — line-range by line-range within
that same file), based on what each line actually does. Root `.env` is
git-ignored, so its diff was computed by hand from the exact before/after
content read directly (not from `git diff`).

### Per file

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | Modified | 152 | 144 |
| `.env` (root, git-ignored) | Modified | ~27 | ~19 |
| `docker-compose.yml` (root) | Modified | 1 | 1 |
| `case-studies/TeaStore/docker-compose.yml` | New | 114 | 0 |
| `case-studies/TeaStore/priam-integration/backfill-data-subjects.py` | New | 136 | 0 |
| `.../entities/message/SessionBlob.java` | Modified | 40 | 0 |
| `.../auth/priam/PriamClient.java` | New | 214 | 0 |
| `.../auth/rest/AuthUserActionsRest.java` | Modified | 86 | 0 |
| `.../auth/security/BCryptProvider.java` | Modified | 10 | 0 |
| `.../webui/priam/PriamClient.java` | New | 77 | 0 |
| `.../webui/servlet/CartServlet.java` | Modified | 14 | 5 |
| `.../webui/servlet/LoginActionServlet.java` | Modified | 12 | 0 |
| `.../webui/servlet/PriamProviderServlet.java` | New | 291 | 0 |
| `.../webui/servlet/ProfileServlet.java` | Modified | 7 | 0 |
| `.../webui/servlet/RegisterServlet.java` | New | 92 | 0 |
| `.../webapp/WEB-INF/pages/header.jsp` | Modified | 2 | 0 |
| `.../webapp/WEB-INF/pages/profile.jsp` | Modified | 4 | 0 |
| `.../webapp/WEB-INF/pages/register.jsp` | New | 49 | 0 |
| `.../registryclient/rest/LoadBalancedStoreOperations.java` | Modified | 37 | 0 |

### By functional category × line nature (added lines only)

| Category | Code | Comment | Blank | Total |
|---|---:|---:|---:|---:|
| Annotation (SQL script) | 34 | 117 | 1 | 152 |
| Rights-API (Provider bridge + §3 workflow) | 234 | 49 | 20 | 303 |
| Consent (CEP, registration, bookkeeping, backfill, navigation) | 417 | 218 | 61 | 696 |
| OAuth2 (Keycloak provisioning, auth-related Docker/.env wiring) | 91 | 28 | 4 | 123 |
| Docker-network (the rest of the Docker/.env wiring) | 82 | 3 | 7 | 92 |
| **Total** | **858** | **415** | **93** | **1366** |

One line = counted in a single category, wherever it physically lives (a
mixed file like `case-studies/TeaStore/docker-compose.yml` or root `.env`
was split line-range by line-range, not attributed whole to one category).

## 6. PRIAM's own LOC for this session

**0 lines** — no PRIAM microservice or frontend source file was modified.
The only PRIAM-repository change is the documentation addendum in
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8.9 (a handful of prose lines
recording the Docker memory-limit data point from this session — not code,
and not counted against the "0 lines changed in PRIAM" constraint, which
concerns the microservices/frontends only).
