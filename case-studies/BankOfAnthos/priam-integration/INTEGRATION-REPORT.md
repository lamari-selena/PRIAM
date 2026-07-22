# PRIAM ↔ Bank of Anthos — Integration Report

## 1. Mechanism, in one page

Bank of Anthos (7 microservices: `frontend`, `userservice`, `contacts`,
`ledgerwriter`, `balancereader`, `transactionhistory`, plus `accounts-db`/
`ledger-db` Postgres) ships **no docker-compose file** — it targets GKE via
skaffold. `case-studies/BankOfAnthos/docker-compose.yml` (new, this session)
reproduces the same 8 containers as plain Docker services, using the exact
env-var contract already defined in `kubernetes-manifests/*.yaml`, so PRIAM's
own compose stack can reach it over `common_network`. The 3 Java ledger
services have no Dockerfile upstream either (built via Jib through skaffold);
`priam-integration/java-service.Dockerfile` is a plain multi-stage Maven
build used instead.

- **idRef = `users.username`** (2-15 alphanumeric/underscore), not
  `accountid` (a random 10-digit string) — chosen specifically so every
  subject, seeded or dynamically registered, is non-numeric by construction
  (playbook §7).
- **Provider bridge** (`src/accounts/userservice/priam_provider.py`) lives in
  `userservice` because it already owns the SQLAlchemy connection to
  `accounts-db`, which holds both `users` (single row/subject, data type
  `User`) and `contacts` (several rows/subject, data type `Contact`,
  disambiguated by the `label` primary key — `contacts` has no surrogate id).
- **CEP**: `contacts.py`'s `add_contact()` wraps the DB write in
  `get_consent(username, "Contact Management", ...)` — the only genuinely
  optional side effect in the app (a payment/deposit can be submitted
  without labeling a contact).
- **Registration**: `userservice.py`'s `create_user()` fires
  `register_data_subject` → `report_processed_data` → `provision_keycloak_user`
  (in that order, in a background thread — see Bug table). `frontend.py`'s
  `home()` calls `has_pending_consent_decision()` and redirects to PRIAM's
  consent page exactly once.
- **OAuth2**: Gateway + both PRIAM frontends require a real Keycloak session
  (see §6/Known limitations) — this was **not originally planned** but
  became necessary once real browser testing of PRIAM-Frontend was
  attempted (its `APP_INITIALIZER` hard-blocks without a valid OIDC
  session, no dev-mode fallback — see Bug table). `provision_keycloak_user()`
  auto-creates a matching Keycloak account at every Bank of Anthos sign-up.

## 2. Scope decisions (documented, not silent)

- **`TRANSACTIONS` (ledger-db) is NOT annotated.** The table is genuinely
  immutable at the database level (`CREATE RULE PREVENT_UPDATE`/
  `PREVENT_DELETE` ... `DO INSTEAD NOTHING` — a real SQL `UPDATE`/`DELETE`
  against it silently no-ops). Annotating it for rectification/erasure would
  mean building a Provider bridge that pretends to support rights it cannot
  execute. This also mirrors a real GDPR exception (Art. 17(3)(b), legal/
  audit retention for financial records). `User` and `Contact` are annotated
  and fully functional for access/rectification/erasure.
- **`ssn` and `birthday` are rectifiable but not erasable** — `ssn` because
  its processing (`Identity Verification`) is `MANDATORY`/legal-obligation,
  a real Art. 17(3)(b) exception; `birthday` because it is a `DATE NOT NULL`
  column with no sensible "blank" value. Erasure of VARCHAR fields blanks to
  `''` (all Bank of Anthos personal columns are `NOT NULL`, so a true SQL
  `NULL` is not an option).
- **Erasing `Contact.label` deletes the whole contact row** (not just that
  field) — `contacts` has no surrogate id, `label` is the identifying field
  (unique per subject); blanking it alone would leave an unaddressable row.

## 3. Bugs found this session

| # | Root cause | Fix | Proof of verification |
|---|---|---|---|
| 1 | Windows `git` checkout with `core.autocrlf=true` gave `1-load-testdata.sh`/`1_create_transactions.sh` CRLF line endings, breaking their `#!/bin/bash` shebang (`bad interpreter`) inside the Alpine Postgres containers. | Stripped `\r` from both scripts (content-only fix, `git diff` shows 0 net change after re-normalization). | `accounts-db`/`ledger-db` containers now boot and load demo data; `docker logs` shows `CREATE TABLE`/demo INSERTs succeeding. |
| 2 | `LedgerWriterApplication.stackdriver()#resourceLabels()` does `HOSTNAME.substring(0, HOSTNAME.indexOf("-"))` unconditionally (even with metrics export disabled). Docker's default hostname (a container-id hex string) has no `-`, so `indexOf` returns `-1` → `StringIndexOutOfBoundsException` on startup. Real bug, not GKE-specific, but only externally visible outside a GKE pod hostname. | Set explicit `hostname:` (containing a `-`) for `ledgerwriter`/`balancereader`/`transactionhistory` in `docker-compose.yml` — compose-only fix, no source change. | All 3 Java services reach `healthy` status; `docker logs` shows no crash. |
| 3 | `transactionhistory/pom.xml`'s `spring-boot-maven-plugin` config hardcodes `<mainClass>...TransActionHistoryApplication</mainClass>` (typo — real class is `TransactionHistoryApplication`). Dormant under Jib (auto-detects main class), fatal under a plain `mvn package && java -jar`. Unrelated to GDPR/PRIAM, a pre-existing upstream typo blocking only the build path chosen for local Docker testing. | Fixed the typo in `pom.xml` (1 line). | `java -jar app.jar` boots `transactionhistory` correctly; `/ready` returns `ok`. |
| 4 | **Self-inflicted, found in testing**: `register_data_subject()`/`report_processed_data()`/`provision_keycloak_user()` were called synchronously inline in `userservice.py`'s `create_user()`. Combined round-trip latency (up to ~3s×3) could exceed `frontend.py`'s own `BACKEND_TIMEOUT` (4s) on `POST /users`, surfacing as "Account creation failed" even though the user row had committed. | Moved all 3 calls into a background `threading.Thread` (true fire-and-forget, not just exception-safe) in both `userservice.py` and `contacts.py` (the latter only for `report_processed_data` — `get_consent()`'s boolean result gates the write and must stay synchronous). | Repeated sign-up (`priamqa3`) completed in the browser without the timeout; DB proof (data_subject/processed_data/Keycloak) all present afterward. |
| 5 | **Self-inflicted, found in testing**: chose `"Contact/Payee Management"` as `processing_name` to fit `varchar(25)`. The `/` broke `GET /api/contract/list/consents/{idRef}/{processingId}` and `/api/decision/{processingId}` — Spring does not decode `%2F` inside a path segment, so `/Payee%20Management` was parsed as an extra path segment → `404`. | Renamed to `"Contact Management"` (18 chars, no `/`) in the SQL annotation and both Python constants. | `has_pending_consent_decision`/`get_consent` calls return `200` with correct payloads after the rename; consent grant/withdraw verified end-to-end (see workflow table). |
| 6 | **Generic PRIAM bug, found in testing, documented in `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.4bis** (per the non-negotiable constraint — the bug lives entirely in PRIAM code): `Databases/db_creation_script.sql`'s `processed_data` table was missing the `nb_occurrences` column that `ProcessedData.java`'s JPA entity declares and both `addProcessedData`/`removeProcessedData` read/write as a reference count. Invisible on SQL-seeded data (no Hibernate involved); fatal (`SQLSyntaxErrorException: Unknown column 'processedd0_.nb_occurrences'`) on the first real consent grant/withdrawal through the API. | Added `nb_occurrences int not null default 1` to `db_creation_script.sql`. | Consent grant for `priamqa5` (a genuinely new subject, exercising the real runtime path) succeeded after the fix; `processed_data` rows created correctly with `nb_occurrences=1`. |
| 7 | **Generic PRIAM bug, found in testing, NOT fixed this session — documented in `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.5bis**: `ConsentServiceImpl.create()`'s withdrawal path calls `DataRestClient.removeProcessedData` (`@DeleteMapping` + `@RequestBody`) via Feign's default `HttpURLConnection`-based client, which is known to drop request bodies on `DELETE`. Withdrawal succeeds (`consent.end_date` set, `200`), but `processed_data` bookkeeping is never cleaned up — no visible error. | **Not applied**: the fix requires reconfiguring Feign's HTTP client (a new dependency + property + rebuild + re-test) or changing the `DataRestClient`/`ProcessedDataController` contract — a two-service change not safely verifiable within this session's remaining time. Documented with full reproduction/isolation steps and a suggested fix path instead. | Isolated by calling `DELETE /api/processed-data/remove` directly against `PRIAM-Data-service` (bypassing Consent-service's Feign call) with the same payload — succeeds and deletes the rows, proving `ProcessedDataService` itself is correct and the bug is specifically in how Consent-service invokes it. |
| 8 | **Generic PRIAM bug, found in testing, documented in the playbook §4bis** (target-application-side actionable fix, since the workaround lives in how `provision_keycloak_user()`/any Admin-API caller must behave): Keycloak's declarative User Profile (on by default since Keycloak 24) silently drops any attribute not declared in its schema on `POST/PUT /admin/realms/{realm}/users` — including `idReference`, a custom attribute never declared for `priam-realm`. A user provisioned through the Admin API logged in fine but its token carried no `idReference` claim, breaking every PRIAM page for that subject. Realm-JSON-seeded users bypass this validation entirely, which is why it was never caught before. | Declared `idReference` in the realm's User Profile (live via `PUT /admin/realms/{realm}/users/profile`, and persisted into `Keycloak/priam-realm-realm.json` via a new `UserProfileProvider` component so a fresh realm import has it too). | `priamqa5`'s token, decoded, carries `"idReference": "priamqa5"` after the fix; `priamqa4`'s token (provisioned before the fix) did not. |

## 4. Workflows verified against real state

| Workflow | Method | Real state checked | Result |
|---|---|---|---|
| Standalone Bank of Anthos (no PRIAM) | Real browser (Playwright/Chromium) | Login, dashboard balance/transactions, sign-up | Works; PRIAM calls fail closed/silently (connection refused, logged as `WARNING`, no user-facing error) — proves fail-open design |
| Sign-up → registration | Real browser + direct MySQL/Keycloak Admin API queries | `priam-actor.data_subject`, `priam-data.processed_data` (10 `User` data_ids), Keycloak user + `idReference` claim | All 3 created correctly (after bug #4/#8 fixes) |
| Consent redirect (never answered) | Real browser, full OIDC redirect chain | `has_pending_consent_decision` → 302 chain BankOfAnthos → PRIAM consent page → Keycloak login | Redirects correctly; does not loop once answered |
| Consent grant (real subject, non-numeric idRef) | Real browser, toggle click | `priam-consent.consent`/`contract` rows created (`end_date=NULL`) | Verified via direct MySQL query |
| Optional side effect gated by consent | Real browser, labeled deposit | `accounts-db.contacts` row created + `processed_data` rows 11-14 reported | Verified: contact + balance + bookkeeping all present |
| Consent withdrawal | Real browser, toggle click | `consent.end_date` set to real timestamp | Verified (bug #7: `processed_data` bookkeeping NOT cleaned up, documented not fixed) |
| Rectification (`User.firstname`), `answer=false` | curl + Keycloak Bearer token | `accounts-db.users.firstname` | Unchanged — confirmed |
| Rectification (`User.firstname`), `answer=true` | curl + Bearer token | `accounts-db.users.firstname` | Changed to `RectifiedFirstName` — confirmed |
| Erasure (`User.address`), `answer=false` then `answer=true` | curl + Bearer token | `accounts-db.users.address` | Unchanged, then blanked to `''` — confirmed |
| Rectification on `Contact` (composite key `label`) | curl + Bearer token | `accounts-db.contacts.routing_num` for the specific row (`label='MyExternalBank'`) | Only that row changed — confirmed (§8.1.c scenario) |
| Access request (read) | curl + Bearer token, and real browser (`/access-request` page) | `GET .../personalDataValues/accessRight` | Returns live data reflecting all prior rectifications/erasures; browser page renders the same |
| Provider dashboard (data controller) | Real browser, Keycloak login as `app.owner` | Pending request list | Renders the real pending request (`7 RECTIFICATION BoA Account Holder`) — approval detail-page click hit a navigation error in automated testing, not chased further (see Known limitations) |
| Backfill for pre-existing demo users | `docker exec` inside `userservice`, direct script run | `data_subject`(1-4, unchanged/idempotent), Keycloak accounts for `testuser`/`alice`/`bob`/`eve` | All 4 backfilled without duplicating the SQL-seeded `data_subject` rows |

## 5. Known limitations

- **Keycloak provisioning covers local sign-up only** (playbook §4bis) — Bank
  of Anthos has no social-login option, so this is not a partial gap here,
  but the general limitation (no plaintext password to sync for federated
  sign-up) still applies as documented in the playbook.
- **PRIAM-Frontend-Provider's rectification-detail page**: the pending
  request list rendered correctly in a real browser, but clicking into a
  specific request's detail view hit a client-side navigation error in
  automated (Playwright) testing that was not root-caused within this
  session's time budget — it may be a real frontend bug or an artifact of
  the automated click target. The underlying approval mechanism itself
  (`POST /right/api/right/answer`) is fully verified via curl with real DB
  proof, independent of this specific page.
- **`processed_data` bookkeeping is not cleaned up on consent withdrawal**
  (bug #7 above) — a real, generic PRIAM bug, documented but not fixed this
  session. Consequence: the Access Request page may keep showing a `Contact`
  column for a subject who withdrew consent, until a manual
  `DELETE /api/processed-data/remove` call (or the underlying Feign fix) is
  applied.
- **OAuth2 was not originally in scope for this session** but became
  necessary: PRIAM-Frontend's `APP_INITIALIZER` hard-blocks bootstrap without
  a valid Keycloak session (no route guard bypass, no manual idRef entry —
  confirmed by reading `app.module.ts`), so satisfying the non-negotiable
  "test rights/consent from a real browser" requirement was not possible
  with the Gateway left in fail-open mode. Wiring OAuth2 in turn made
  automatic Keycloak provisioning (§4bis) a hard requirement rather than the
  originally-conditional nice-to-have, per the task's own stated rule.
- **Method used for real-browser testing**: no headless-browser CLI tool was
  pre-installed in this environment; Playwright (Python) was already
  available with a working Chromium binary and was used directly for every
  browser-based test and screenshot in this session (not curl standing in
  for a browser).

## 6. LOC breakdown

**Method**: manual, file-by-file reading of each diff/new file; a small
Python script (not a pre-built tool) tallied `blank` (whitespace-only),
`comment` (`#`/`--`-prefixed lines and Python triple-quote docstring bodies),
and `code` (everything else) within line ranges I assigned by hand to each
functional category. Where a single file mixes categories (e.g.
`userservice/priam.py` mixing Consent and OAuth2 functions), lines were
split by the exact line ranges of the relevant function/block, not
apportioned by guesswork. For modified files, only **added** lines were
classified into the category breakdown (the per-file table below still
reports raw `+`/`-` from `git diff --numstat` or manual counts for
non-git-tracked root files). `git diff --numstat` alone was not treated as
already providing this breakdown, per the task's own caveat — it only
supplied the raw `+`/`-` counts used in the per-file table.

### Per-file

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | new | 212 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/priam.py` | new | 99 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/priam_provider.py` | new | 146 | 0 |
| `case-studies/BankOfAnthos/src/accounts/contacts/priam.py` | new | 47 | 0 |
| `case-studies/BankOfAnthos/src/frontend/priam.py` | new | 28 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/userservice.py` | modified | 33 | 0 |
| `case-studies/BankOfAnthos/src/accounts/contacts/contacts.py` | modified | 25 | 2 |
| `case-studies/BankOfAnthos/src/frontend/frontend.py` | modified | 13 | 0 |
| `case-studies/BankOfAnthos/src/ledger/transactionhistory/pom.xml` | modified (unrelated build fix, bug #3) | 1 | 1 |
| `case-studies/BankOfAnthos/src/accounts/accounts-db/initdb/1-load-testdata.sh` | modified (CRLF→LF only, 0 net content change) | 0 | 0 |
| `case-studies/BankOfAnthos/src/ledger/ledger-db/initdb/1_create_transactions.sh` | modified (CRLF→LF only, 0 net content change) | 0 | 0 |
| `case-studies/BankOfAnthos/docker-compose.yml` | new | 257 | 0 |
| `case-studies/BankOfAnthos/priam-integration/java-service.Dockerfile` | new | 26 | 0 |
| `case-studies/BankOfAnthos/priam-integration/backfill-data-subjects.py` | new | 51 | 0 |
| `.env` (PRIAM root) | modified | 9 | 17 |
| `docker-compose.yml` (PRIAM root, `name:` only) | modified | 1 | 1 |
| `Keycloak/priam-realm-realm.json` (new test user, Bank-of-Anthos-specific) | modified | ~9 | 0 |

### By functional category × line nature (this integration)

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| Annotation | 73 | 124 | 15 | 212 |
| Rights-API | 110 | 27 | 12 | 149 |
| Consent | 119 | 86 | 40 | 245 |
| OAuth2 | 49 | 13 | 2 | 64 |
| Docker-network | 223 | 54 | 10 | 287 |
| **Total** | **574** | **304** | **79** | **957** |

### PRIAM itself (generic fixes, separate from the integration above)

| File | Status | +lines | Category | Code | Comment | Blank |
|---|---|---|---|---|---|---|
| `Databases/db_creation_script.sql` (bug #6, `nb_occurrences`) | modified | 15 | Annotation (schema) | 1 | 14 | 0 |
| `Keycloak/priam-realm-realm.json` (bug #8, `UserProfileProvider` component — generic, benefits every case study) | modified | ~8 | OAuth2 | 8 | 0 | 0 |
| **Total** | | **23** | | **9** | **14** | **0** |

Bug #7 (`removeProcessedData` Feign/DELETE body) was found and documented but
**not fixed** — 0 LOC, see §3/§5 above for why.
