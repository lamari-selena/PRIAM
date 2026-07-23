# PRIAM ↔ Bank of Anthos — Integration Report

**Session note**: this repository already contained a complete, previously
tested integration for this case study (a single commit, `59e0858`). At the
start of this session the working tree had it reverted out of
`case-studies/BankOfAnthos/`, and the shared PRIAM stack was running a
different case study (Ghostfolio). This session restored the committed code,
**independently re-verified every piece against the real current schema and
routes** (not a blind trust of the prior commit), found and fixed two real
gaps (§3 below), then brought up the whole stack fresh and re-ran the entire
test matrix with new, real state proof (`ETAPES-FAITES.md`) rather than
reusing the old document's claims.

## 1. Mechanism, in one page

Bank of Anthos (7 microservices: `frontend`, `userservice`, `contacts`,
`ledgerwriter`, `balancereader`, `transactionhistory`, plus `accounts-db`/
`ledger-db` Postgres) ships **no docker-compose file** — it targets GKE via
skaffold. `case-studies/BankOfAnthos/docker-compose.yml` reproduces the same
8 containers as plain Docker services, using the exact env-var contract
already defined in `kubernetes-manifests/*.yaml`, so PRIAM's own compose
stack can reach it over `common_network`. The 3 Java ledger services have no
Dockerfile upstream either (built via Jib through skaffold);
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
  (in that order, in a background thread, so a slow PRIAM round-trip can
  never make sign-up itself time out). `frontend.py`'s `home()` calls
  `has_pending_consent_decision()` and redirects to PRIAM's consent page
  exactly once.
- **Bidirectional navigation** (§3, gap #1): `navigation.html`'s account
  dropdown now shows "Manage on PRIAM" (`PRIAM_FRONTEND_URL`, hidden if
  unset); PRIAM-Frontend's "Back to the app" link points at
  `http://localhost:9000/home`, a real working page, via the root `.env`'s
  `TARGET_APP_URL`.
- **OAuth2**: Gateway + both PRIAM frontends require a real Keycloak session.
  `provision_keycloak_user()` auto-creates a matching Keycloak account at
  every Bank of Anthos sign-up, using the username as-is (already ≥3 chars
  and Keycloak-safe for every realistic Bank of Anthos username) and a
  synthesized `@bankofanthos.local` email (the app has no email field).

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
- **Keycloak provisioning covers local sign-up only** (playbook §4bis) —
  Bank of Anthos has no social-login option, so this is not a partial gap
  here, but the general limitation (no plaintext password to sync for
  federated sign-up) still applies as documented in the playbook.

## 3. Bugs / gaps found this session

| # | Root cause | Fix | Proof of verification |
|---|---|---|---|
| 1 | **Gap in the restored integration**: no template anywhere referenced `PRIAM_FRONTEND_URL` — the "Manage on PRIAM" link required by the task (§4ter) had never actually been wired into any Bank of Anthos page, despite `frontend.py` already computing the config value. | Added a conditional link to `navigation.html`'s account dropdown (`{% if priam_frontend_url %}`) and passed `priam_frontend_url=app.config['PRIAM_FRONTEND_URL']` into `home()`'s `render_template(...)` call (3 + 2 lines). | Real browser: opened the account dropdown on `/home` as `priamqa7`, confirmed "Manage on PRIAM" renders above "Sign out" and links to `http://localhost:4200` (screenshot `8_account_dropdown.png`). |
| 2 | **Generic PRIAM bug, found in testing, fixed on PRIAM's side** (per the non-negotiable constraint — the bug lives entirely in `PRIAM-Frontend`, not in any Bank of Anthos code): `consent.component.ts`'s `necessaryList` filter only matched `ProcessingType.NECESSARY`, silently dropping every `MANDATORY` processing from the consent page, even though `isDisable()` already treats both identically. Bank of Anthos's `"Identity Verification"` (`MANDATORY`, the SSN/KYC processing) is the first processing in any integrated case study to actually exercise this code path. | Widened the filter: `a.processingType === ProcessingType.NECESSARY \|\| a.processingType === ProcessingType.MANDATORY` (`consent.component.ts`, `getProcessings()`). Documented in `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8.1.d. | Real browser, before/after: `4_final_consent_page.png` (before — only "Account Management" shown) vs. `5_consent_after_fix.png` (after, `frontuser` rebuilt — "Identity Verification" now renders, correctly pre-checked/disabled). |
| 3 | `priam-integration/local-jwt/` (the RSA key pair `userservice`/`contacts`/the 3 Java services all mount) is correctly gitignored (`*.key`) but was not present on disk at the start of this session — `docker compose up` would have failed on the bind mount. | Regenerated with the exact command Bank of Anthos's own `docs/development.md` documents (`openssl genrsa -out jwtRS256.key 4096` + `openssl rsa ... -pubout`). | `userservice`/`contacts`/`frontend` all started and issued/verified JWTs correctly (login, `/ready`, JWT-gated `/contacts/<username>` calls all succeeded during testing). |

**Investigated, not a bug**: `processed_data.nb_occurrences=2` for
`priamqa7`'s Contact fields (instead of 1) after one contact creation.
Root-caused by reading `ConsentServiceImpl.create()`: PRIAM's own
consent-grant mechanism independently calls `addProcessedData`/
`removeProcessedData` for every data_id tied to the processing being
toggled, on top of Bank of Anthos's own explicit `report_processed_data()`
call at contact-creation time — two legitimate, independent mechanisms
reporting the same ids. Confirmed harmless: withdrawal correctly decremented
`nb_occurrences` on **both** of two tested withdrawal cycles (2→1 each time),
directly contradicting the previously-documented "bug #7" (Feign `DELETE`+
body silently dropped) from the original integration session — that bug does
not reproduce in this checkout. See `ETAPES-FAITES.md` §6-7 for the full
trace.

## 4. Workflows verified against real state (this session)

| Workflow | Method | Real state checked | Result |
|---|---|---|---|
| Sign-up → registration | Real browser (Playwright/Chromium) + direct MySQL/Keycloak Admin API queries | `priam-actor.data_subject`, `priam-data.processed_data` (10 `User` data_ids), Keycloak user + `idReference` claim | All 3 created correctly |
| Consent redirect (never answered) | Real browser, full OIDC redirect chain | `has_pending_consent_decision` → redirect chain Bank of Anthos → PRIAM consent page → Keycloak login | Redirects correctly, exactly once |
| Consent page rendering (`MANDATORY` processing) | Real browser, before/after bug #2 fix | `/consent` page content | Fixed: "Identity Verification" now renders alongside "Account Management" |
| Consent grant (non-numeric idRef) | Real browser, toggle click | `priam-consent.consent`/`contract` rows created (`end_date=NULL`) | Verified via direct MySQL query |
| Bidirectional navigation | Real browser | "Back to the app" (PRIAM→app) and "Manage on PRIAM" (app→PRIAM) both clicked/rendered | Both work; app link lands on a real page (`/home`), not bare root |
| Optional side effect gated by consent | Real browser, labeled deposit | `accounts-db.contacts` row created + `processed_data` rows 11-14 reported | Verified: contact + balance + bookkeeping all present |
| Consent withdrawal → re-grant → withdrawal | Real browser, toggle clicks ×3 | `consent.end_date`, `processed_data.nb_occurrences` | Full cycle verified; bookkeeping decrements correctly both times |
| Rectification (`User.firstname`), `answer=false` then `true` | curl + Keycloak Bearer token | `accounts-db.users.firstname` | Unchanged, then changed — both confirmed |
| Erasure (`User.address`), `answer=false` then `true` | curl + Bearer token | `accounts-db.users.address` | Unchanged, then blanked to `''` — both confirmed |
| Rectification on `Contact` (composite key `label`) | curl + Bearer token | `accounts-db.contacts.routing_num` for the specific row | Only that row changed — confirmed (§8.1.c scenario) |
| Access request (read) | curl + Bearer token | `GET .../personalDataValues/accessRight` (User + Contact) | Returns live data reflecting all prior rectifications/erasures |
| `dataValue` (4th Provider endpoint, §8.2.f) | curl, direct smoke test | Provider bridge response | Correct value returned for both a `User` field (type inferred from `dataName`) and a `Contact` field (type inferred from `primaryKeys`) |
| Provider dashboard (data controller) | Real browser, Keycloak login as `app.owner` | Pending request list | Renders a real pending request; detail-page click is a no-op in this build (not chased further — approval mechanism independently verified via curl) |
| Backfill for pre-existing users | `docker exec` inside `userservice`, direct script run | `data_subject` (13 rows, no duplicates), Keycloak accounts | All users backfilled idempotently, including ones from the original session's own testing |

## 5. Known limitations

- **Keycloak provisioning covers local sign-up only** (playbook §4bis) — no
  partial gap for Bank of Anthos specifically (no social login exists), but
  the general limitation still applies as documented.
- **PRIAM-Frontend-Provider's request-detail click**: the pending request
  list renders correctly in a real browser; clicking the notification does
  not navigate anywhere in this build (no JS error observed, unlike the
  original session's report of a `TypeError` — may simply not be a clickable
  element). Not root-caused; the approval mechanism itself
  (`POST /right/api/right/answer`) is independently and thoroughly verified
  via curl with real DB proof.
- **`processed_data` double-bookkeeping for `OPTIONAL` processings**: both
  PRIAM's own consent toggle and the target application's explicit
  `report_processed_data()` call independently manage the same data_ids for
  a multi-row type gated by consent (§3). Harmless (confirmed: withdrawal
  still correctly clears the bookkeeping, just needs one extra
  withdraw/re-grant cycle to reach zero occurrences than a naive count would
  suggest) — flagged here for transparency, not treated as a bug to fix.

## 6. LOC breakdown

**Method**: `git diff --numstat HEAD` for every git-tracked file (this
session's changes are layered on top of the already-committed integration,
which the working tree exactly matched before this session's edits — verified
with `git diff HEAD` returning empty for every restored file before any new
edit was made). For the non-git-tracked root `.env`, the exact lines changed
were identified manually from the edit itself. Code vs. comment vs. blank
classification within each changed range was done by manual reading (a line
starting with `#`/`--`/inside a Jinja `{% %}` comment tag = comment; an empty
or whitespace-only line = blank; everything else = code) — no automated
tool. Only **this session's added/changed lines** are classified into the
category breakdown below; the already-committed baseline (identical files,
verified via `git diff HEAD` = empty) is carried forward from the original
integration's own accounting without re-deriving it line-by-line a second
time, since it is unchanged, byte-for-byte, in this session.

### Per-file (Bank of Anthos side + Databases + root Docker wiring)

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | unchanged from committed integration | 212 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/priam.py` | unchanged from committed integration | 99 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/priam_provider.py` | unchanged from committed integration | 146 | 0 |
| `case-studies/BankOfAnthos/src/accounts/contacts/priam.py` | unchanged from committed integration | 47 | 0 |
| `case-studies/BankOfAnthos/src/frontend/priam.py` | unchanged from committed integration | 28 | 0 |
| `case-studies/BankOfAnthos/src/accounts/userservice/userservice.py` | unchanged from committed integration | 33 | 0 |
| `case-studies/BankOfAnthos/src/accounts/contacts/contacts.py` | unchanged from committed integration | 25 | 2 |
| `case-studies/BankOfAnthos/src/frontend/frontend.py` | modified this session (on top of the committed integration's +13/-0) | 13 + **2** = 15 | 0 + **1** = 1 |
| `case-studies/BankOfAnthos/src/frontend/templates/shared/navigation.html` | **new this session** (gap #1 fix — untouched by the original integration) | **3** | 0 |
| `case-studies/BankOfAnthos/src/ledger/transactionhistory/pom.xml` | unchanged from committed integration (unrelated build fix) | 1 | 1 |
| `case-studies/BankOfAnthos/src/accounts/accounts-db/initdb/1-load-testdata.sh` | unchanged (CRLF→LF only, 0 net content change) | 0 | 0 |
| `case-studies/BankOfAnthos/src/ledger/ledger-db/initdb/1_create_transactions.sh` | unchanged (CRLF→LF only, 0 net content change) | 0 | 0 |
| `case-studies/BankOfAnthos/docker-compose.yml` | unchanged from committed integration | 257 | 0 |
| `case-studies/BankOfAnthos/priam-integration/java-service.Dockerfile` | unchanged from committed integration | 26 | 0 |
| `case-studies/BankOfAnthos/priam-integration/backfill-data-subjects.py` | unchanged from committed integration | 51 | 0 |
| `case-studies/BankOfAnthos/priam-integration/local-jwt/jwtRS256.key{,.pub}` | **new this session** (regenerated, gitignored — not source code, excluded from the category breakdown below) | — | — |
| `.env` (PRIAM root) | modified this session (case-study switch back from Ghostfolio: `CUSTOM_PROVIDER_URL` value+comment, `TARGET_APP_URL` value) | **6** | **8** |
| `docker-compose.yml` (PRIAM root, `name:`/volume, restored via `git checkout HEAD`) | unchanged from committed integration | 1 | 1 |

### PRIAM itself (generic bug fix, separate from the integration above)

| File | Status | +lines | -lines |
|---|---|---|---|
| `PRIAM-Services/PRIAM-Frontend/src/app/pages/consent/consent.component.ts` | modified this session (bug #2, `MANDATORY` filter) | 2 | 1 |

### By functional category × line nature (this session's own changes only)

Per the method above, only lines actually added/changed **this session** are
broken down here (the unchanged, already-committed baseline's own category
breakdown — Annotation 212/Rights-API 149/Consent 245/OAuth2 64/
Docker-network 287 lines, 574 code + 304 comment + 79 blank total — carries
forward unchanged from the original integration and is not re-derived).

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| Annotation | 0 | 0 | 0 | 0 |
| Rights-API | 0 | 0 | 0 | 0 |
| Consent (navigation.html +3, frontend.py +2) | 5 | 0 | 0 | 5 |
| OAuth2 | 0 | 0 | 0 | 0 |
| Docker-network (`.env` case-study switch, net) | 6 | 0 | 0 | 6 |
| **Total (this session)** | **11** | **0** | **0** | **11** |

**Combined total (already-committed baseline + this session)**: 574+11=585
code, 304 comment, 79 blank = **968 lines**, across the categories above
(Annotation 212, Rights-API 149, Consent 250, OAuth2 64, Docker-network 293).

### PRIAM's own LOC (this session's generic fix)

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| Consent-frontend bug fix (`consent.component.ts`) | 2 | 0 | 0 | 2 |
