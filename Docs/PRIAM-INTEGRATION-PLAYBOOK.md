# PRIAM Integration Guide — connecting a new target application

> Intended to be followed by any LLM/agent to reproduce a PRIAM integration
> quickly, without rediscovering pitfalls already encountered. Generic: it does
> not name any specific target application, only patterns to reproduce. Drawn from
> the FastAPI-Healthcare-PRIAM, TeaStore, SportTracker, and Ghostfolio integration
> sessions — the detail specific to each of these case studies lives in its own
> `case-studies/<name>/priam-integration/` folder, not here.
>
> **Validation status**: the 4 GDPR workflows (access, rectification, erasure,
> consent granted/withdrawn/re-granted) have been executed end-to-end against the
> full PRIAM stack (mysqldb, eureka, actor, data, consent, right, provider,
> gateway) plus a real target application, with proof of real state (database,
> message queue) at every step — not just reviewed or documented. Generic OIDC
> authentication (Gateway + both Angular frontends) has also been validated
> end-to-end against a real IdP (Keycloak), including from a real browser. Every
> pitfall in §8 below and in `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` was
> encountered and fixed during real tests, not anticipated in theory. If you find
> one of these bugs on a copy of the repository, your copy is out of date — do not
> fix it a second time without first checking whether the relevant file already
> contains the fix. **Both catalogs grow with every new integration** — if you
> discover a new one, add it to the right thematic group in the right file (§8 of
> the playbook if the fix stays on the target-application side,
> `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` if the bug and its fix live entirely in
> PRIAM's own code) rather than documenting it elsewhere (§4bis in particular notes
> a still-fresh race condition found during the Ghostfolio integration).

## How to use this document

- **§0 to §6 are the contract to read in full** before writing a single line of
  code: this is the stable specification (SQL annotation, Provider bridge, rights,
  consent, registration/forced consent, network, auth). It does not change from
  one integration to the next.
- **§7 (test methodology) should be read before you start testing**, not after.
- **§8 (pitfall catalog) is not meant to be read linearly.** Start with the index
  at the top of the section: one line per pitfall, grouped by the PRIAM component
  concerned. Only open the detail of the group(s) relevant to what you are doing
  (e.g., writing the SQL script → read only the "SQL annotation / seed" group).
  `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` (bugs already fixed in PRIAM's own
  code) does not need to be read at all for a normal integration — only for
  diagnostics if an already-cataloged symptom reappears.
- **The final checklist** is the sequential thread — every step links back to the
  matching detailed section.

## 0. Architecture overview

PRIAM is a set of Spring Boot microservices (`actor`, `data`, `consent`, `right`,
`provider`, `gateway`, `eureka`) plus a MySQL database (`mysqldb`), wired to
**one** target application at a time (single-tenant in the current state of the
code). The target application only needs to know about PRIAM through 2 channels:

- **Outbound** (the target app calls PRIAM): check consent before an optional
  processing activity — see §4.
- **Inbound** (PRIAM calls the target app): execute a GDPR right (access/
  rectification/erasure) via 3 Provider endpoints the target app must expose —
  see §2-3.

The target application never sees Keycloak, Eureka, or the other PRIAM
microservices directly — only `PRIAM_CDP_URL`/`PRIAM_ACTOR_URL` (for consent and
registration) and exposing the 3 Provider endpoints (for rights).

## 1. Annotation (SQL) — modeling the target application inside PRIAM

File: `Databases/db_insertion_script.sql`, copied into the `Databases` Docker
image and executed automatically by MySQL on first startup
(`/docker-entrypoint-initdb.d/`) — **on a virgin MySQL volume only**. This is NOT
an `annotations.json` file called over a REST API — it is direct SQL.

Steps, in order:

1. **Identify the real schema** of the target application: open its code (ORM
   models), never assume. Every `source_details` must cite the real physical
   table/column.
2. **Cross-schema ordering is mandatory**: `INSERT INTO data_subject_category`
   (`priam-actor` schema) **BEFORE** `INSERT INTO data` (`priam-data` schema),
   because `data.data_subject_category_id` references
   `priam-actor.data_subject_category` (a cross-schema FK).
3. `personal_data_category` only ships 10 default rows (see
   `Databases/db_creation_script.sql`) — if the target application has missing
   categories (e.g. "contact", "financial"), add them (plain data rows, no CHECK
   constraint on them).
4. `data_type.data_type_name` must match **literally** the name the target
   application's Provider bridge code compares against (e.g. `"Patient"`, not
   `"patients"` the SQL table name) — verify it in the bridge code, don't guess.
5. `data`: one row per personal column that is actually rectifiable/erasable,
   with `source_details` citing the real table.column. **`is_primary_key=1` on
   the id column of any table with several rows per subject** — see §8.1 (missing
   primary key) for the exact consequence of forgetting it. **Composite key**
   (e.g. `id` + `num`): set `is_primary_key=1` on **each** column that makes up
   the key — the frontend mechanism (`getPrimaryKeys()`) already collects every
   marked column, not just one, and sends them as an array (`primaryKeys:
   [{primaryKeyName, primaryKeyValue, primaryKeyId}, ...]`) **in the request
   submission** (`POST /right/api/right/rectificationRequest`, §3). **This is not
   the format the Provider bridge receives**: `PRIAM-Right-service` rebuilds
   `primaryKeys` into a `Map<String, String>` (`{columnName: value}`) before
   calling `/api/rectification`/`/api/erasure` — see §2, two different formats
   for two different interfaces, do not conflate them.
6. `processing`: PRIAM's metamodel distinguishes 4 types (Lamari et al.,
   *Information and Software Technology*, 2026, 194:108065, §3.1.3) — do not
   limit yourself to the two usually seen in existing case studies:
   - `DEFAULT`: requires no consent (e.g. data subject login/authentication) —
     does not even appear as a toggle in the consent UI.
   - `NECESSARY`: core application functionality, legal basis = contract/
     necessity (Art. 6.1.b) — consent must be given to use the application, not
     revocable without losing use of the app.
   - `MANDATORY`: legal processing, **neither contractual nor consent-based**
     (Art. 6.1.c, legal obligation) — distinct from `NECESSARY` by its legal
     basis, but with the same UI behavior (see below).
   - `OPTIONAL`: legal basis = consent (Art. 6.1.a), the only type the user can
     freely enable/disable without affecting core functionality.
   - UI behavior (`PRIAM-Frontend/.../consent.component.ts`): `NECESSARY` and
     `MANDATORY` are both shown pre-checked and disabled (non-revocable); only
     `OPTIONAL` has an active toggle; `DEFAULT` does not appear in the consent
     list at all.
   - `processing_name` is the human-readable name — resolution by name is
     already generic on PRIAM's side, no need to know a numeric id ahead of
     time.
7. `data_usage` / `purpose`: link `data` to `processing`.
8. `data_subject.id_ref`: must be a **real, stable** id from the target
   application (not an invented placeholder). If the target application seeds no
   default data, add a seed script on the target-application side first (see
   §7), so the `id_ref` is reliable (e.g. `id=1` on a fresh auto-increment
   database).
9. `contract` / `consent`: seed a **pre-granted** consent (`end_date = NULL`) for
   every `Optional` `processing`, so rights tests start from a clean state (the
   consent withdraw/grant test itself happens later through the real PRIAM
   endpoint, not by editing this script — see §4).
10. `processing.processing_type`: the value must match **exactly** (case
    included) a constant of the Java enum `ProcessingType` — see §8.1 (enum
    case), a pitfall hit twice on two different enums.
11. If a `consent` is pre-granted directly by this script (point 9) for an
    `OPTIONAL` `processing`, also add the matching `processed_data` row — see
    §8.1 (missing `processed_data` bookkeeping), otherwise the first consent
    withdrawal through the API fails.
12. **`personal_data_transfer` / `secondary_actor`** (`priam-actor` schema for
    `secondary_actor`/`address`/`secondary_actor_category`, `priam-data` schema
    for `personal_data_transfer`/`personal_data_transfer_data`/
    `personal_data_transfer_secondary_actor`): annotate **if and only if** a
    `Processing` genuinely transfers data to an external third party (e.g. an
    email/notification provider, a subcontractor) — not systematic for every
    `Processing`, unlike `purpose` (point 6/7), which is. The metamodel (Lamari
    et al. 2026, Annex A, GDPR Art. 44-50) treats declaring `safeguardType` as
    **mandatory as soon as a transfer exists** toward a country whose
    `country.adequate = false` (`ADEQUACY_DECISION`, `CONTRACTUAL_CLAUSE`,
    `DEROGATION`, or `BCR` — see `Databases/db_creation_script.sql`). Forgetting
    this annotation does not crash any endpoint — the Access Request UI's
    "Transfer" section simply stays silently empty, easy to mistake for a bug
    rather than a missing annotation.

If several case studies must coexist in the same PRIAM database, namespace the
ids (`1xx`/`2xx`/`3xx`/`4xx` ranges); otherwise a standalone script with simple
ids (`1, 2, 3...`) is more readable.

## 2. The Provider bridge — 4 endpoints to write on the target-application side

Contract verified in
`PRIAM-Services/PRIAM-Right-service/.../openfeign/ProviderRestClient.java`, the
`/provider/**` route of `PRIAM-Gateway` (which **strips only the `/provider`
prefix** then forwards to `CUSTOM_PROVIDER_URL`), and `PRIAM-Frontend-Provider`
(for the 4th endpoint, `dataValue` — absent from the Right-service DTOs, called
directly by this frontend):

```
GET  {CUSTOM_PROVIDER_URL}/api/dataAccessRight?idRef=...&dataTypeName=...&attributes=a,b,c
POST {CUSTOM_PROVIDER_URL}/api/rectification   body: {idRef, dataTypeName, dataName, newValue, primaryKeys}
POST {CUSTOM_PROVIDER_URL}/api/erasure         body: {idRef, dataTypeName, dataName, primaryKeys}
POST {CUSTOM_PROVIDER_URL}/api/dataValue       body: {idRef, dataName, primaryKeys}
```

Important points:
- Mounted on the bare **`/api`** prefix, NOT `/api/priam` (a frequent mistake —
  never caught without a real end-to-end test, since it is documented but never
  wired up in practice).
- **No authentication** — called only machine-to-machine by PRIAM.
- `idRef` = the subject's real primary id in the target application (as a
  string).
- `primaryKeys` disambiguates a record that is not the "subject" table itself
  (e.g. a medical record, an order) — `idRef` alone is not enough. **It is an
  object `{columnName: value}` received by the Provider bridge**
  (`Map<String,String>` on the Java side) — do not confuse it with the array
  `[{primaryKeyName, primaryKeyValue, primaryKeyId}]` used only in the request
  submission (§1 point 5, §3); `PRIAM-Right-service` converts between the two
  before calling the Provider bridge. A composite key (e.g. `id` + `num`)
  arrives as several keys inside this same object.
- **`dataValue` does not send `dataTypeName`** in its body, unlike the other
  three endpoints — `PRIAM-Frontend-Provider`
  (`GetRectificationService`/`GetSuppressionService#getCurrentValue`) never
  forwards it. The Provider bridge must determine the type from `dataName` (a
  per-type whitelist, §1 point 4) and/or the presence of `primaryKeys` (empty for
  a single-row-per-subject type, populated otherwise).
- `attributes`/`dataName` must be restricted to a whitelist of fields allowed for
  a given `dataTypeName`, validated on the target-application side (400 if a
  field is not listed).
- `GET /api/dataAccessRight` **must always answer with a JSON array**, even a
  single-element or empty one (`[]`) — never a bare object.
- **Sorting records in a stable, deterministic way** (e.g. `ORDER BY` the
  annotated primary key) remains good general practice (predictable behavior
  from one call to the next), even though PRIAM now groups all columns of a
  given `dataType` into a single call on the `PRIAM-Data-service` side, which
  removes the risk of misalignment that a per-column call could have
  introduced.
- `attributes` (on `GET /dataAccessRight`) arrives on the target-application
  side as **a single query parameter, comma-separated values**
  (`attributes=a,b,c`), not as repeated parameters.
- `CUSTOM_PROVIDER_URL` must point **directly at the target application** (e.g.
  `http://<target-app-service>:<port>`) if that application implements these 3
  endpoints itself (the common case, described in this guide). Do not leave it
  at its default value (`http://provider:8086`, PRIAM's generic
  `Provider-microservice`) unless that generic component is specifically the one
  acting as the bridge for this target application — otherwise every approved
  rights request silently fails or hits the wrong database.

## 3. The real rights workflow (PRIAM-Right-service)

**Common pitfall**: calling the 3 Provider endpoints directly to "test rights"
bypasses PRIAM's real business mechanism. The real flow goes through
`PRIAM-Right-service`:

1. **Request** — `POST /api/right/accessRequest` (or `/rectificationRequest`,
   `/erasureRequest`) with `{dataSubjectId, dataTypeName, data: {dataId},
   newValue, claim, primaryKeys: []}` → creates an unanswered `DataRequest`,
   notifies the "data controller" (app owner).
2. **Answer** — `POST /api/right/answer` with `{dataRequestId, answer: bool,
   providerClaim, data: []}`:
   - `answer=false` → only records `AnswerType.REFUSED`. **Nothing else
     happens** — no Provider call.
   - `answer=true` (rectification/erasure) → records `AnswerType.FULL` **AND**
     automatically calls the matching Provider endpoint (`ProviderRestClient`,
     via the Gateway) — it is PRIAM that triggers the execution, not the caller
     of `/answer`.
   - Answering the same request twice is blocked (409).
3. For **access** requests, the real read goes through an always-open endpoint
   (`DataAccess`/`personalDataValues/accessRight`), not through the
   auto-execution mechanism above — the answer only records which fields are
   `FULL`/`PARTIAL`/`REFUSED` (`isAccepted` bookkeeping).

**The full test to run** (not a shortcut): a cycle with `answer=false` (verify
that **no** change happens in the target application's database) AND a second
cycle with `answer=true` (verify that the change genuinely happens
automatically) — both, not just one.

## 4. The Consent bridge (CEP) — optional processing

Minimal pattern (see `FastAPI-Healthcare-PRIAM/app/priam/consent.py` as a
reference):

```python
def get_consent(id_ref, processing_id) -> bool:
    if not PRIAM_CDP_URL:
        return True  # PRIAM absent -> preserve pre-PRIAM behavior
    try:
        # GET {PRIAM_CDP_URL}/api/decision/{processing_id}?idRefList={id_ref}, timeout ~3s
        return decision.get(id_ref, False) is True
    except Exception:
        return False  # PRIAM unreachable/error -> deny by default (fail-closed)
```

- **A single function** is enough as long as nothing ever calls it with several
  ids at once — do not add a "batch" variant before a real caller needs it (the
  PRIAM endpoint already supports it natively through a repeated `idRefList`, so
  adding it later only costs a function, not a rewrite).
- `processing_id` can be passed directly as a **human-readable name** (generic
  resolution already in place on PRIAM's side — no need to know a numeric id).
- **Insertion point**: wrap **only** the optional side effect in an `if
  get_consent(...): ...` — never gate mandatory processing. No explicit `else`
  is needed ("do nothing" = don't call the function).
- Do not use a declarative route-level guard (a decorator/middleware that
  blocks the whole request with a 403) — that would wrongly block mandatory
  processing. The inline `if`, in the middle of the business logic, is the
  right level of granularity.

## 4bis. Automatic subject registration + forced consent at sign-up

Completes §1 (SQL annotation, written once for a fixed test subject) for real
production use: every **real new user** of the target application must
automatically become a PRIAM `data_subject`, without a manual script — and must
be asked for an explicit consent decision right at sign-up rather than never. A
complete, tested example lives in `case-studies/Ghostfolio-PRIAM-test1` (see its
`priam-integration/INTEGRATION-REPORT.md` for a file-by-file breakdown).

**Three generic functions**, alongside `get_consent()` (§4):

```python
def register_data_subject(id_ref) -> None:
    if not PRIAM_ACTOR_URL:
        return
    try:
        # POST {PRIAM_ACTOR_URL}/api/DataSubject
        # body: {idRef: id_ref, dataSubjectCategoryId: <constant fixed in §1>}
        ...
    except Exception:
        log.warning(...)  # never raise — must never block sign-up

def has_pending_consent_decision(id_ref, processing_id) -> bool:
    if not PRIAM_CDP_URL:
        return False
    try:
        # GET {PRIAM_CDP_URL}/api/contract/list/consents/{id_ref}/{processing_id}
        # (Consent Information Point — NOT the same endpoint as get_consent()/CDP)
        return len(response_list) == 0  # empty list = never answered
    except Exception:
        return False  # PRIAM unreachable -> don't force a redirect

def report_processed_data(id_ref, data_ids) -> None:
    if not PRIAM_ACTOR_URL or not PRIAM_DATA_URL:
        return
    try:
        # GET {PRIAM_ACTOR_URL}/api/DataSubjectId/{id_ref} -> dataSubjectId (int)
        # POST {PRIAM_DATA_URL}/api/processed-data/add?subjectId={dataSubjectId}
        # body: data_ids (a JSON array of integers, the data_id values from §1
        # point 4 actually held by this subject for the record just created)
        ...
    except Exception:
        log.warning(...)  # never raise
```

- **`report_processed_data`** is **the most frequently forgotten point of the
  entire integration** — without it, the Access Request page stays empty for
  any dynamically registered subject, no matter how careful the rest of the
  work is (§8.1.b). Encountered under real conditions during the Habitica
  integration: the mechanism was entirely absent (none of the 3 functions in
  this section existed for data bookkeeping), symptom "only 1 field out of 7
  annotated shows up" for a normally registered subject. **Call it at every
  point where a personal record is created** — not just at sign-up: any later
  creation of a record of a `data_type` with several rows per subject (e.g. a
  task, an order, an appointment) must also report the `data_id` values of that
  specific type, at the same hook as the creation itself (e.g.
  `libs/tasks/index.js` of a Node app, the task-creation controller, etc.) —
  not only at sign-up time.

- **`register_data_subject`**: called at **every point in the code where the
  target application creates a new user** (there can be several — classic
  sign-up, federated login, etc.; you must find them all). Fire-and-forget, like
  `get_consent()` regarding availability: never couple PRIAM's availability to
  the target application's ability to create accounts. **`POST /api/DataSubject`
  is idempotent on the Actor-service side** (upsert by `idRef` —
  `DataSubjectServiceImpl.saveDataSubject`, verified in the code): replaying it
  with an already-known `idRef` returns the existing row without duplicating it,
  so it is safe to rely on for a periodic reconciliation job or a manual replay.
- **`has_pending_consent_decision`** is **different** from `get_consent()`
  (§4): `get_consent` answers "is it granted?" (false if never answered OR
  explicitly refused), while this function answers "is there already a
  decision at all, whatever it is?" — that distinction lets you tell "never
  asked" (needs a redirect) apart from "already refused" (do not ask again). It
  uses the Consent Information Point
  (`/api/contract/list/consents/{idRef}/{processingId}`), not the Consent
  Decision Point.
- **Flag insertion point**: expose a boolean (e.g. `priamConsentRequired`) in
  the application's already-existing "current user" response (the one the
  client queries after login), computed via
  `has_pending_consent_decision(id_ref, <id of the OPTIONAL processing>)`. Never
  block sign-up itself waiting on this decision — the flag is read and acted on
  **after** the account already exists.
- **Client side**: at the point where the application already handles the
  "current user" response after login, redirect to PRIAM's consent page
  (`{PRIAM_FRONTEND_URL}/consent`) if the flag is true. The redirect happens
  **only once by construction**: `has_pending_consent_decision` becomes false
  as soon as a decision exists, so there is no redirect loop on the next
  refresh.
- **Only force the `OPTIONAL` processing(s)** — never the `NECESSARY` ones.
  GDPR consent must be freely given; blocking use of the application until a
  user has said "yes" to a processing activity that is necessary for the
  contract is not valid consent, it is a false notion of consent applied to a
  legal basis that does not need one.
- **Minimal footprint in DI frameworks** (NestJS, Spring, etc.): register the
  module holding these two functions as a global singleton (`@Global()` in
  NestJS, the Spring equivalent) rather than importing it explicitly into every
  calling module — otherwise every newly discovered sign-up point requires
  editing the module wiring file in addition to the call itself, doubling the
  footprint for no reason.
- **⚠️ Mandatory ordering if the target application also reports "processed"
  data right at sign-up** (e.g. a default account created on signup, annotated
  via `reportProcessedData` on the application side — see §1 point 11): **wait
  for `register_data_subject` to finish before calling anything that resolves
  `idRef → dataSubjectId` internally** (`GET
  {PRIAM_ACTOR_URL}/api/DataSubjectId/{idRef}`, used by several endpoints, see
  §8.6). Two fire-and-forget calls triggered one after another at the same
  moment (sign-up) with no explicit dependency between them create a real race
  condition: if the second one starts before the first has committed to the
  database, the `idRef → dataSubjectId` resolution fails (§8.6). Encountered
  and reproduced under real conditions during the Ghostfolio integration (two
  fire-and-forget calls triggered from the same account-creation flow). Fixed
  on PRIAM's side with a clean 404 instead of a crash (§8.6), but the real data
  (the `processed_data` bookkeeping for that call) is still lost if the race is
  lost — **explicitly `await` `register_data_subject` before firing any call
  that depends on it**, do not rely on the fact that PRIAM no longer crashes.
- **`data_subject.id_ref` is now `varchar(64)`**
  (`Databases/db_creation_script.sql`, widened from `varchar(25)` — a standard
  UUID is 36 characters, and the original value overflowed for any target
  application using UUID ids, including through `register_data_subject`
  above). This fix is generic, applied once for all on PRIAM's side — but it
  requires a virgin MySQL volume or a real migration (`ALTER TABLE`) on a
  database already initialized with the old schema, not just updating the
  creation script file.
- **Users who already existed before the sign-up hook was added**:
  `register_data_subject` (above) only covers **new** accounts created after
  it was added to the code. To catch up on already-existing accounts, write a
  one-off script (not a permanent application endpoint) that walks the
  existing users table and calls `register_data_subject` for each one — a
  complete example lives in
  `case-studies/Ghostfolio-PRIAM-test1/priam-integration/backfill-data-subjects.mts`
  (reuses the application's already-configured ORM directly, not a separate
  database-to-database access).

### Automatic Keycloak identity provisioning at sign-up

**To be done as soon as a Keycloak IdP is wired up (§6) AND the target
application has its own local sign-up (email/password)** — without this, the
"Manage on PRIAM" link from §4ter leads to a Keycloak identity that has **no
relation** to the user's real account (encountered and fixed under real
conditions during the Habitica integration: creating a Habitica account
normally, then clicking "Manage on PRIAM", asked to log in with a completely
disconnected test Keycloak account — no mechanism existed to create the
matching Keycloak account).

**Why this isn't automatic**: unlike an application that logs in *through*
Keycloak (federated OIDC — Keycloak then creates the account itself on first
login), an application with its own local sign-up never routes the user
through Keycloak. Nothing ever creates the matching Keycloak account, at any
point — this is not an occasional oversight, it is structural as long as
nothing is explicitly added.

**Generic pattern**, alongside `register_data_subject()` (same sign-up hook,
same fire-and-forget/never-blocking philosophy):

```python
def provision_keycloak_user(id_ref, username, email, password) -> None:
    if not KEYCLOAK_ADMIN_URL:
        return
    try:
        admin_token = fetch_admin_token(KEYCLOAK_ADMIN_URL, KEYCLOAK_ADMIN_USERNAME, KEYCLOAK_ADMIN_PASSWORD)
        # POST {KEYCLOAK_ADMIN_URL}/admin/realms/{realm}/users
        # body: {username, email, enabled: true, emailVerified: true,
        #        credentials: [{type: 'password', value: password, temporary: false}],
        #        attributes: {idReference: [id_ref]}}
        create_keycloak_user(admin_token, username, email, password, id_ref)
    except Conflict409:
        return  # already provisioned (retry, or a manually-created account) - not an error
    except Exception:
        log.warning(...)  # never raise — must never block sign-up
```

- **The plaintext password is only available at this exact moment** — the
  target application hashes it immediately for its own storage and never
  re-exposes it afterward. This is the only place in the code where it can be
  captured to synchronize it into Keycloak.
- **Covers local sign-up only.** Sign-ups through a social provider (Google/
  Facebook/etc., often handled by a library like Passport) have no equivalent
  secret to synchronize — they remain uncovered by this pattern. Covering them
  would require a real Keycloak bridge (an SPI delegating directly to the
  social provider, "Family 2" of §6) — heavier, not built into PRIAM to date,
  to be documented as a known limitation rather than pretending it works.
- **Idempotent by construction**: a `409 Conflict` from the Keycloak Admin API
  (account already exists) is a normal case (replay, an equivalent backfill
  script on the Keycloak side), not an error to surface.
- **⚠️ `firstName`/`lastName` are mandatory despite appearances.** An account
  created through the Admin API without these two fields is created without
  error, with `requiredActions: []` (nothing signals a problem through the
  API) — but the Direct Grant flow (`grant_type=password`) then systematically
  fails to log in with `invalid_grant: "Account is not fully set up"`, with no
  stored `requiredActions` explaining why. Encountered and reproduced under
  real conditions (Habitica integration, Keycloak 26): the account appeared
  correctly created (right `idReference`, right password) and login kept
  failing on every attempt, until comparison with a working account on the
  same realm that had `firstName`/`lastName` filled in. Fixed by reusing
  `username` for both fields (Habitica has no separate first/last name at
  sign-up) — any target application without distinct first/last name fields
  can do the same. **Generalization confirmed on this same integration**: the
  symptom is not specific to `firstName`/`lastName` — **any** attribute marked
  `required` in the realm's User Profile (by default: `username`, `email`,
  `firstName`, `lastName`) triggers the exact same generic error if it is
  missing at creation through the Admin API, `email` included (encountered
  while re-testing the static test accounts in
  `Keycloak/priam-realm-realm.json`, created without `email`). Do not stop at
  fixing `firstName`/`lastName` and re-testing only those two fields if the
  error persists — check the realm's full list of `required` attributes
  (`GET /admin/realms/{realm}/users/profile`) and make sure
  `provision_keycloak_user()` (or any static test dataset) supplies all of
  them.
- **⚠️ Keycloak `username` has a 3-character minimum — do not reuse the
  target application's username as-is.** The realm validates `username` with
  a minimum length (often 3, configurable in the User Profile); a target
  application that allows shorter identifiers (encountered under real
  conditions with a real Habitica account named `"w"`, a single character)
  causes the creation to fail with a **silent** `400 Bad Request` — the same
  misleading symptom as above: nothing on the target-application side
  indicates it, the PRIAM `data_subject` account does exist, only the
  Keycloak account is missing. Fixed: use **email** as the Keycloak `username`
  rather than the target application's handle — always long/well-formed
  enough to pass validation, and the user already knows it (as a consequence,
  the Keycloak login screen asks for the email, not the app's handle — state
  this clearly if the target application shows a credentials reminder). If
  the target application has no email either (free-form identifier only),
  prefix/pad the handle to guarantee the minimum length rather than passing
  it through unchanged.
- **⚠️ The Admin API silently drops `idReference` (or any other custom
  attribute) unless it is declared in the realm's User Profile.** Keycloak's
  declarative User Profile (on by default since Keycloak 24) validates every
  `POST/PUT /admin/realms/{realm}/users` body against a fixed attribute
  schema; any attribute not declared there (`idReference` included, since
  it is a custom attribute, not a built-in one) is silently stripped from
  the request **before persistence** - no error, no warning, `201 Created`
  as if everything worked. Encountered and reproduced under real conditions
  (Bank of Anthos integration): a user provisioned through
  `provision_keycloak_user()` logged in fine, but its access token carried
  no `idReference` claim at all - confirmed by decoding the JWT, and by
  comparing against a user seeded directly in `priam-realm-realm.json`'s
  `users` array (realm import bypasses this validation entirely and DOES
  keep the attribute), which is why this pitfall is easy to miss if the
  only test performed uses static realm-JSON test accounts rather than a
  freshly Admin-API-provisioned one. Fix: declare `idReference` in the
  realm's User Profile before any user is provisioned through the Admin
  API - either live (`GET`/`PUT
  /admin/realms/{realm}/users/profile`, adding `{"name": "idReference",
  "permissions": {"view": ["admin","user"], "edit": ["admin"]},
  "multivalued": true}` to the `attributes` array) or baked into the realm
  JSON itself (a `components["org.keycloak.userprofile.UserProfileProvider"]`
  entry, `providerId: "declarative-user-profile"`, config key
  `kc.user.profile.config` holding the same profile JSON as a string) so a
  fresh realm import already has it. **If you hit "Account is not fully set
  up" (`invalid_grant`) after manually patching a user's attributes to work
  around this**: a `PUT /admin/realms/{realm}/users/{id}` with a partial
  body **replaces** the whole user representation, not just the fields
  supplied - a `PUT` containing only `{"attributes": {...}}` silently wipes
  `firstName`/`lastName`/`email` (all `required` in the default User
  Profile), immediately reproducing the exact symptom this same section
  already documents for missing required attributes at creation time. Fetch
  the current representation first and PUT back the full object.
- **Configuration**: `KEYCLOAK_ADMIN_URL` (empty = disabled, same fail-open
  philosophy as the rest of this guide), `KEYCLOAK_REALM`, and admin
  credentials — reusing the admin bootstrap already defined in the root
  `docker-compose.yml`'s `keycloak` service (`KEYCLOAK_ADMIN`/
  `KEYCLOAK_ADMIN_PASSWORD`) is enough for development/testing; a dedicated
  Keycloak client with the `manage-users` role (rather than the super-admin
  account) is preferable before any exposure beyond local use.
- **What this does NOT do**: it creates the account and aligns the
  credentials, but the user still sees **one** Keycloak login screen when
  arriving on PRIAM (with the credentials they just chose, so not a real
  obstacle). A truly silent SSO (zero visible login screen) would require
  establishing a browser Keycloak session right after sign-up — a separate,
  more fragile effort, not covered here.

## 4ter. Round-trip navigation between PRIAM and the target application

Two symmetrical links, each optional and driven by an environment variable —
neither adds case-study-specific code inside PRIAM itself:

- **Target application → PRIAM**: a "Manage on PRIAM" button in the target
  application's settings (e.g. Ghostfolio's `user-account-settings`), shown
  only if `PRIAM_FRONTEND_URL` is configured on the application side, pointing
  to `PRIAM-Frontend` (`frontuser`, e.g. `http://localhost:4200`). See
  `case-studies/Ghostfolio-PRIAM-test1/priam-integration/INTEGRATION-REPORT.md`.
- **PRIAM → target application**: a "Back to the app" link in `PRIAM-Frontend`'s
  navbar (visible on every page, not just Home — an initial attempt limited to
  the Home page left a user navigating elsewhere with no way back to the
  application short of the browser's "back" button), shown only if the
  `TARGET_APP_URL` build arg is set (empty by default = link hidden). Wired
  once and for all in `PRIAM-Frontend/Dockerfile` (added to the generated
  `environment.ts`, like the other `API_*`/`OIDC_*` options) and read by
  `navbar.component.ts` (shared by every page); only the **value** changes per
  case study. This value lives in the root `.env`
  (`TARGET_APP_URL=http://localhost:xxxx`), read by the root
  `docker-compose.yml` via
  `frontuser.build.args.TARGET_APP_URL: ${TARGET_APP_URL}` — not hardcoded in
  `docker-compose.yml` (a pitfall experienced firsthand: this was the only
  per-case-study value that lived outside `.env`, so the only one a developer
  — human or AI — working from `.env` alone could forget to change when
  switching case studies). **Do not forget to set it on every case-study
  switch**: `http://localhost:3333` for Ghostfolio, `http://localhost:5173`
  for Habitica, etc.

Both links are plain HTML anchors (`<a href>`), not real application-level
SSO — the Keycloak session already active in the browser (if the user logged
into the target application through its OIDC login option, not through a
Keycloak-independent method) is usually enough to avoid a new login form when
arriving on PRIAM, but this is not guaranteed if the target application offers
several non-equivalent login methods (see the Family 1/Family 2 distinction in
§6).

## 5. Docker network

- PRIAM exposes a shared external network: `common_network` (declared
  `external: true` in the `docker-compose.yml` files that consume it;
  created/owned by PRIAM's root `docker-compose.yml` on the first startup of
  its stack).
- In the target application's `docker-compose.yml`: add
  `PRIAM_CDP_URL: http://consent:8089` to the application service (the real
  service name and port of `PRIAM-Consent-Service`) and attach it to
  `common_network` in addition to its own network.
- **`common_network` must already exist** before an `external: true` reference
  works — start PRIAM's root stack (or whichever stack owns it) at least once
  before the target application's stack.

**⚠️ Never run two different PRIAM checkouts at the same time on the same
machine without changing their Docker Compose project identity.** Every PRIAM
`docker-compose.yml` declares `name: priam` on its first line, plus fixed
`container_name:` values (`priam-actor-ms`, `priam-databases`, etc.). Two
checkouts (e.g. the working repository and a test copy such as
`PRIAM-test1/`) that declare the same thing are, from Docker Compose's point
of view, **the same project**: a `docker compose up`/`down` launched from
either folder silently manages or destroys the other's containers, and bind
mounts (`db-volume/`, `Keycloak/`) stay pinned to the path of the folder that
actually created the container — not the one the last command came from.
Typical symptom: an integration that seemed to work "yesterday" fails "today"
with no apparent code change, because another folder took over the same
containers in the meantime. If several checkouts must coexist, give each one
a distinct `name:` (e.g. `priam` vs `priam-test1`) at the top of its
`docker-compose.yml`, and make sure no fixed `container_name:` collides
(removing them and letting Compose generate the default
`<project>_<service>_1` names is the safest option for multiple checkouts).

**PRIAM's own images are shared across every case study, not rebuilt each time.**
The `actor`, `consent`, `data`, `eureka`, `right`, `provider`, `gateway`, and
`frontprovider` services in the root `docker-compose.yml` carry a fixed `image:`
(e.g. `priam-actor-ms:latest`), independent of the Compose project's `name:` — PRIAM
stays generic across case studies (non-negotiable constraint), so these images only
need (re)building when PRIAM's own code changes, never just because you switched
case study. Without a fixed `image:`, Compose would tag each build
`<project name>-<service>`, forcing a full, redundant rebuild for every case study
even though nothing on PRIAM's side had changed. **Two deliberate exceptions, with
no fixed `image:`**: `mysqldb` (the `./Databases` context contains
`db_insertion_script.sql`, rewritten per case study — §1) and `frontuser`
(`TARGET_APP_URL` is injected at build time via a Docker build arg, so the built
image genuinely differs per case study). For these two services, always use
`--build` when switching case study — a fixed tag there would risk silently reusing
a stale image.

## 6. Gateway authentication — generic OIDC resource server

`SecurityConfig.java` validates a JWT (any OIDC issuer — Keycloak or
otherwise, agnostic by design) on human-facing routes (`/right/**`, `/cdp/**`)
as soon as `CUSTOM_OIDC_ISSUER_URI` is configured; machine-to-machine routes
(`/data/**`, `/actor/**`, `/provider/**`, `/eureka/**`) always stay open,
regardless of that variable's state. If it is absent, the Gateway still
starts (documented fail-open behavior, not an oversight).

`PRIAM-Frontend` receives the same treatment on the client side:
`angular-oauth2-oidc` (neutral, not a proprietary SDK), with an HTTP
interceptor that attaches the token to API calls.

See **`Docs/PRIAM-AUTH-OIDC.md`** for the full detail of the approach (Gateway
+ Frontend), the `issuer-uri`/`jwk-set-uri` Docker network pitfall, the
step-by-step wiring guide, and the three families of target applications (no
own auth / own auth to reuse for SSO / already-native OIDC IdP) — the first
validated end-to-end against a real IdP, the other two documented but not yet
tested.

## 7. Test methodology — proof by real state

**Non-negotiable rule, for both rights and consent**: a `200`/`FULL` from
`POST /right/api/right/answer` (or the consent equivalent) proves **nothing**
about what actually happened on the target-application side — only that PRIAM
*attempted* the call and that the Provider bridge answered without an HTTP
error. Encountered under real conditions (Habitica integration): a
rectification approved with `answer:true` and a `200 {"answer":"FULL"}`
response, without the email actually changing in the MongoDB database — the
cause was not even a logic bug, but a `priam-right-ms` container running code
compiled several days earlier (§8.9, "the service must build from local
source"), never rebuilt after later code changes. **After every approved
rights test OR consent toggle, immediately read the target application's
database directly** (`SELECT`/Mongo equivalent) to confirm the real value —
not just re-reading through PRIAM's own API (which could reflect a state it
believes it wrote without that being true). If a test fails surprisingly,
**check the freshness of the containers involved first** (deployed jar
compile date vs. the date of the last code change, see §8.9) before hunting
for a logic bug — a stale container produces exactly the same symptoms as a
real bug (correct API response, no real effect).

- **Rights**: check the target application's real database state (a direct
  `SELECT` after the call), not just the HTTP response's 200 code.
- **Consent granted**: same, plus check that an observable side effect
  genuinely happened (e.g. a message published to a queue, an email sent) —
  count before/after, not just the absence of an error.
- **Consent refused**: proof of ABSENCE is the tricky part. "No error" proves
  nothing. Count the observable state BEFORE and AFTER the call, confirm it
  has **not changed** (same number of queued messages, no new send-log line).
  Also check that mandatory processing still happened.
- **The observation mechanism itself must be verified first**, before blaming
  PRIAM for a badly applied consent decision. If the observable side effect
  (message queue, email, log) is produced by target-application code that is
  **independent** of PRIAM, that code may have its own bug preventing it from
  running in every case (granted or refused) — in which case the counter will
  never move regardless of the consent state, and one would wrongly conclude
  PRIAM is blocking everything. Test first that the side effect does happen
  when consent is granted (the usual starting state after the §1 point 9
  seed) before testing withdrawal.
- **Target-application prerequisites to check before running tests**
  (generally independent of PRIAM, but blocking for observing anything at
  all):
  - Authentication: if the target application's routes require an
    account/token (the common case), create a test account and use it for
    every call, before even starting the PRIAM tests.
  - Reference data: any business constraint of the target application on the
    tested path (e.g. resource availability, account status, a quota) must be
    satisfied by the seed data — otherwise the target application rejects the
    request before PRIAM's own code (the consent guard, etc.) is ever
    reached, and the failure has nothing to do with PRIAM.
- **Test at least once from a real browser, not just curl** — CORS, the
  `OPTIONS` preflight, token expiry, and the rendering of the Consent/Access
  Request pages only fully show themselves in a browser (several bugs already
  fixed on PRIAM's side in these exact areas, see
  `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.7/§8.8 if a symptom of this kind
  reappears anyway).
- **Test with a non-numeric `idRef`** (a UUID or a free-form string), not
  just a simple auto-incremented id — a numeric `idRef` can coincidentally
  collide with PRIAM's internal id and mask certain bugs (several already
  fixed on PRIAM's side, `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.2/§8.8).

## 8. Catalog of known pitfalls

Each entry = a real bug, encountered and fixed during an end-to-end test, not
anticipated in theory. **This catalog now only contains what remains
actionable for a developer integrating a new target application**: mistakes
you can reproduce yourself while writing the SQL annotation (§8.1), and the
only two pitfalls that still require action on the target-application side on
top of a fix already in place on PRIAM's side (§8.2.f, §8.6).

**Bugs that lived in PRIAM's own code** (the generic Provider bridge,
`PRIAM-Right-service`, `PRIAM-Data-service`, `PRIAM-Consent-Service`,
`PRIAM-Actor-service`, the Gateway, both Angular frontends) are **already
fixed once and for all** in this repository — PRIAM is generic (§0), you have
nothing to learn about them to integrate a new application. They remain
cataloged separately, for provenance or diagnosing an apparent regression
only, in **`PRIAM-Services/PRIAM-INTERNAL-FIXES.md`**.

### Quick index

| # | Group | Pitfall | Observable symptom |
|---|---|---|---|
| 8.1.a | SQL annotation | Wrong case for `processing_type` / `purpose_type` | Hibernate `IllegalArgumentException: No enum constant...` |
| 8.1.b | SQL annotation | Missing `processed_data` bookkeeping | `IllegalArgumentException: Subject not found` on consent withdrawal |
| 8.1.c | SQL annotation | Missing primary key (one-to-many table) | `404 Record not found` on rectification |
| 8.2.f | Provider bridge (to write on the target-application side) | 4th endpoint `dataValue`, absent from the Right-service DTOs so easy to miss | `404` on the rectification/erasure detail page of the Provider dashboard |
| 8.6 | Registration (to sequence on the target-application side) | Race condition between `register_data_subject` and `idRef→id` resolution if misordered | `404` on `GET /api/DataSubjectId/{idRef}` right after sign-up |
| 8.9 | Environment | Docker VPN DNS / parallel builds / resource limits / dev-mode hot reload | See the detailed group |

### 8.1 SQL annotation / seed

**a. Wrong case for `processing_type` / `purpose_type`.** See §1 points 6/10.
The Java enums `ProcessingType`
(`NECESSARY`/`OPTIONAL`/`MANDATORY`/`DEFAULT`) and `PurposeType`
(`MAIN`/`SECONDARY`) only have uppercase constants. Mixed-case seed data
(`'Necessary'`, `'Main'`) silently passes MySQL's `CHECK` constraint (its
collation is case-insensitive) but crashes Hibernate with
`IllegalArgumentException: No enum constant ...` as soon as the entity is
read or touched — an error message that points at the Java enum, not the
faulty SQL, easy to misdiagnose on the target-application side. Two endpoints
on the `PRIAM-Data-service` side are particularly affected by `purpose_type`:
`GET /processing/listProcessings` (the Frontend's Consent page — its 500
failure is not handled on the Angular side, the Consent section silently
disappears) and `GET /data/processedPersonalDataList/purposes/{idRef}` (the
Access Request page, "Purposes" section, same silent symptom).
**Systematically check** that `processing_type` **and** `purpose_type` are
UPPERCASE before loading a seed script.

**b. Missing `processed_data` bookkeeping.** See §1 point 11.
`ConsentServiceImpl.create` (the grant/withdraw toggle,
`POST /api/consent/create/{idRef}`) calls `Data-service.removeProcessedData`
on withdrawal, which expects to find a `processed_data` row for the relevant
`data_id` (normally created by `addProcessedData` when consent was granted
**through the API**). If the initial consent was pre-seeded directly in SQL
(the common case to start tests from a clean "granted" state), that row does
not exist, and the first withdrawal fails with `IllegalArgumentException:
Subject not found with ID: ...` — a misleading message; the real cause is the
missing bookkeeping. **Compounded**:
`DataService.findAllProcessedDataByDataSubjectCategoryAndId` only returns
`Data` rows that have a matching `processed_data` row — a filter used by both
`getProcessedIndirectAndProducedPersonalDataList` and
`getProcessedPersonalDataList`. Every new `data`/`data_usage` annotation
therefore needs its `processed_data` row to appear in the Access Request
lists, not just for consent withdrawal.

**c. Missing primary key on a table with several rows per subject.**
Symptom: approving a rectification on a field of a one-to-many table (e.g.
one appointment among several for the same patient) fails with `404 Record
not found`, reproducible identically in curl (so not a frontend-only
problem). Cause: `access-request.component.ts::getPrimaryKeys(dataType,
rowIndex)` filters the `data` list for the relevant `dataTypeName` for
entries where `isPrimaryKey` is true, to know exactly which record to fix
(§2, `primaryKeys`). If no `id` column was annotated with `is_primary_key=1`
for that `DataType`, the `primaryKeys` sent by the frontend is empty, the
Provider bridge falls back to a default id (often `0`), finds nothing, and
returns `404` — surfaced by `PRIAM-Right-service` as a generic `500`. Fix:
annotate the id column with `is_primary_key=1`, `is_personal=1` (otherwise it
is never included in value lists), `source=DIRECT` (not
`PRODUCED`/`INDIRECT`, which trigger an acceptance guard on PRIAM's side and
would prevent the primary key from appearing until a dedicated access
request has already been accepted); add the matching `data_usage` +
`processed_data` row (same pattern as 8.1.b); and explicitly whitelist the
`"id"` field on the target application's Provider bridge (§2, whitelist of
allowed attributes).

### 8.2 Provider bridge — the 4th endpoint, `dataValue`

The other historical pitfalls of this bridge (bare-object response instead
of an array, incorrectly resolved internal `idRef`, inconsistent field
contract, badly encoded `attributes`, column misalignment — formerly §8.2.a
through §8.2.e) are bugs that lived on PRIAM's side, already fixed once and
for all: see `PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.2 if useful for
diagnostics, otherwise there is nothing to do here. The only point still
actionable on the target-application side:

**f. The Provider bridge's 4th endpoint, never documented or implemented
anywhere — `dataValue`.** `PRIAM-Frontend-Provider` (the
rectification/erasure detail pages,
`GetRectificationService`/`GetSuppressionService#getCurrentValue`) call `POST
{CUSTOM_PROVIDER_URL}/api/dataValue` to show the data controller the current
value before they approve or refuse — an endpoint entirely absent from the
documented contract (§2, only 3 endpoints before this discovery) and
**absent from every existing case study** (Ghostfolio, FastAPI-Healthcare),
not just Habitica: no one had clicked on this specific Provider dashboard
page before. Symptom: `404` on the target-application side (no route
matches), visible only in a real browser on this specific page — never
encountered through the §3 API tests, which do not exercise this page.
Contract quirk: the request body (`{idRef, dataName, primaryKeys}`) **does
not contain `dataTypeName`**, unlike the other 3 endpoints — the Provider
bridge must infer the type from `dataName` (a per-type whitelist) and/or the
presence of `primaryKeys`. Fixed for Habitica (`priamProvider.js`, a new
handler); to be added for any case study that does not yet have this
endpoint, even ones already considered "finished".

### 8.6 PRIAM-Actor-service — race condition at registration (new, fixed)

Encountered during the Ghostfolio integration, under real conditions (not in
a unit test). See §4bis for the full context and the fix on the
target-application side (sequencing the calls).

Symptom: `GET /api/DataSubjectId/{idRef}` returned `500` right after a new
user's sign-up, while the same call succeeded moments later for the same
`idRef`. Cause, confirmed by the `priam-actor-ms` logs:
`DataSubjectServiceImpl.getDataSubjectIdByIdRef` called
`.getDataSubjectId()` on the result of `findDataSubjectByIdRef(idRef)`
without checking it wasn't `null` — a `NullPointerException` as soon as the
requested idRef does not exist yet in the database, which systematically
happens if a second fire-and-forget call (e.g. reporting that a default
account already holds data, §1 point 11) is triggered by the target
application before the first one (`register_data_subject`, §4bis) has
finished committing.

Fixed (`DataSubjectServiceImpl.java`): an explicit `null` guard, now raising
a clear `EntityNotFoundException` instead of a bare `NullPointerException` —
the failure stays distinguishable from a genuine server error, even though
the caller still needs to follow the ordering documented in §4bis (the fix
makes the failure clean, it does not remove the need to sequence the two
calls).

### 8.9 Docker / Windows environment

Generic environment pitfalls, unrelated to PRIAM's code or to any particular
target application's code — relevant to any container you run. (An adjacent
subset, specific to PRIAM's own `Dockerfile`s/build scripts and already
fixed in this repository — CRLF, `gradle build` vs. `assemble`, building the
Gateway from source, the BuildKit heredoc issue — lives in
`PRIAM-Services/PRIAM-INTERNAL-FIXES.md` §8.9-P if useful for diagnostics.)

- **Unstable Docker Desktop DNS behind a VPN (Windows/WSL2)**: if `docker
  pull`/build fails intermittently with `dial tcp: lookup <host>: no such
  host` even though the machine otherwise has internet access, this is
  usually the host's DNS resolver (VPN gateway) timing out. Recommended fix:
  `%USERPROFILE%\.wslconfig`:
  ```ini
  [wsl2]
  networkingMode=mirrored
  dnsTunneling=true
  ```
  then `wsl --shutdown` and restart Docker Desktop. Greatly reduces the
  failure rate without necessarily eliminating it entirely — plan for
  retries rather than giving up on the first network failure.
- **Parallel builds are less reliable than sequential ones on an unstable
  network**: `docker compose build <svc1> <svc2> <svc3>` in a single command
  fails more often (concurrent Maven/Gradle downloads, one timeout fails the
  whole batch) than the same build run service by service.
- **Docker resource limits**: running the whole PRIAM stack ON TOP OF the
  target application's stack can saturate a modest Docker Desktop/WSL2 VM
  (`HikariPool - Thread starvation`, a JVM stuck mid-startup, Docker Desktop
  crashing with `500`s on its own API). Start services one at a time in
  dependency order, allow each Java service 50-90s to finish its health
  check before treating a failure as real. `docker compose up --build
  <service>` can trigger a rebuild of every buildable service in the file at
  once — prefer `COMPOSE_BAKE=false docker compose build <service>` alone,
  then `docker compose up -d --no-build <service>`, to target exactly one
  service.
- **A frontend container in dev mode (`vite`/`webpack-dev-server` with hot
  reload) does not always detect file changes made on the host side over a
  Windows bind mount.** Encountered under real conditions (Habitica
  integration, `website/client` mounted as a volume in the `client`
  container, run via `vite --host 0.0.0.0`): a `.vue` file created/edited
  from the host was indeed present in the container (verified with `docker
  exec <service> cat <file>`), but no `hmr update` message ever appeared in
  the Vite server logs, and the browser never reloaded the component — a
  misleading symptom since nothing signals an error, the change simply seems
  to be ignored. Likely cause: propagating file change events (inotify
  inside the container) across the Windows host → Docker Desktop VM → bind
  mount boundary is not always reliable, depending on the storage driver
  used. Fix applied: `docker compose restart <service>` (or
  `--force-recreate` if a plain `restart` is not enough, e.g. Keycloak, whose
  `start-dev` data lives in the container's writable layer and survives a
  plain `restart`) forces the dev server to re-read the current file state
  from disk, without depending on change events. A more robust fix (the
  watcher's `polling` mode, e.g. `server.watch.usePolling: true` in
  `vite.config.mjs`) was not necessary here but remains an option to
  consider if this manual restart becomes frequent. Only applies to
  target-application PRIAM services with a dev-mode frontend container
  (PRIAM's own frontends do not have this problem, served in dev mode via
  `ng serve` but rebuilt as an image rather than mounted as a volume).

## Quick checklist for a new target application

1. [ ] Identify the target application's real schema (tables/columns).
2. [ ] Write/adapt `db_insertion_script.sql` (§1) — actor before data
   ordering, missing categories, real names, `processing_type` **and**
   `purpose_type` in UPPERCASE (§8.1.a), a `processed_data` row for every
   pre-granted consent (§8.1.b).
3. [ ] Add a seed script on the target-application side if it has no default
   data, for a reliable `idRef` — include every reference data point
   required by the target application's business constraints on the tested
   path (§7), not just the subject itself.
4. [ ] If a `Processing` genuinely transfers data to an external third
   party, annotate `personal_data_transfer`/`secondary_actor` (§1 point 12)
   — conditional, unlike `purpose` which is systematic; `safeguardType` is
   mandatory if the third party's country is not adequate.
5. [ ] Write the 4 Provider endpoints (§2 — `dataAccessRight`,
   `rectification`, `erasure`, `dataValue`, the latter easy to forget since
   it is absent from the Right-service DTOs, §8.2.f), mounted on bare
   `/api`, no auth, `attributes` parsed as a single comma-separated string,
   `dataAccessRight` always answering with a JSON array (§2), `primaryKeys`
   received as an object `{name: value}` not an array (§2).
5bis. [ ] Wire the equivalent of `reportProcessedData` (§4bis) at **every**
   point where a personal record is created (not just sign-up) — without
   this, the Access Request page stays empty for any dynamically registered
   subject, no matter how careful the rest of the integration is (§8.1.b).
6. [ ] Write the CEP `get_consent()` (§4), a single function, fail-open if
   `PRIAM_CDP_URL` is absent, fail-closed otherwise.
6bis. [ ] Wire `register_data_subject()` at every user-creation point, and
   `has_pending_consent_decision()` + a client-side redirect to
   `{PRIAM_FRONTEND_URL}/consent` to force a decision on the `OPTIONAL`
   processing(s) only (§4bis) — without ever blocking sign-up itself on
   PRIAM's availability. If the target application also reports "processed"
   data at sign-up, **wait** for `register_data_subject` to finish before
   calling anything that resolves `idRef → dataSubjectId` (§4bis, §8.6).
7. [ ] Wire `PRIAM_CDP_URL`/`PRIAM_ACTOR_URL` + `common_network` into the
   target application's `docker-compose.yml` (§5), and point
   `CUSTOM_PROVIDER_URL` (root PRIAM `.env`) directly at the target
   application (§2). If another PRIAM checkout is already running on the
   same machine, check for a `name:`/`container_name:` collision (§5).
8. [ ] Start the PRIAM stack, making sure `gateway` builds from local
   source, not a remote image — otherwise fixes already in place on PRIAM's
   side might not be in the image actually running.
9. [ ] Create a test account/token if the target application requires
   authentication on the tested routes (§7).
10. [ ] Test the real rights workflow through `PRIAM-Right-service` (§3),
    not a direct call to the Provider endpoints — a cycle with
    `answer=false` AND `answer=true`, **reading the target application's
    database directly after every `answer:true`** (§7 — a `200`/`FULL`
    proves nothing by itself; if the result is surprising, check the
    freshness of the containers involved before hunting for a logic bug).
11. [ ] Test consent in both directions (granted/refused/re-granted) with
    proof of observable real state (§7) — first checking that the side
    effect does happen when consent is granted, before testing withdrawal.
12. [ ] Wire up authentication (Gateway + Frontend): decide the family
    (with/without its own auth on the target-application side),
    deploy/configure the OIDC IdP, map the `idReference` claim, test
    `401`/`200` depending on the token — see **`Docs/PRIAM-AUTH-OIDC.md`**
    (the full guide, not duplicated here). If the target application has
    its own local sign-up (email/password), also wire up automatic
    Keycloak provisioning at sign-up (§4bis, "Automatic Keycloak identity
    provisioning") — otherwise the "Manage on PRIAM" link (§4ter) leads to
    an identity disconnected from the user's real account.
13. [ ] If one or more human-facing frontends are wired up (Gateway +
    `angular-oauth2-oidc`, see `Docs/PRIAM-AUTH-OIDC.md`): add each origin
    to `CUSTOM_FRONTEND_ORIGINS` (otherwise CORS silently blocks things in
    the browser only, not in curl — already wired on PRIAM's side) and
    check that `setupAutomaticSilentRefresh()` is indeed called in every
    `app.module.ts` (otherwise data disappears after ~5 min of session —
    already in place on PRIAM's side).
14. [ ] Test at least once **from a real browser**, not just curl (§7) — in
    particular, check that the Consent/Access Request/My Requests pages
    show real data for a **non-numeric** `idRef` (§7).
15. [ ] Set `TARGET_APP_URL` in the root `.env` (§4ter) to show a link back
    to the target application on PRIAM-Frontend's Home page — easy to
    forget when switching case studies, not optional if you want the link
    to work.