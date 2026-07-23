# PRIAM ↔ Mastodon — Integration Report

## 1. Mechanism, in one page

Mastodon (`case-studies/Mastodon`) is a Ruby on Rails 8 + PostgreSQL +
Redis + Sidekiq application (ActivityPub federated social network),
production `docker-compose.yml` with `web` (Puma, port 3000), `streaming`
(Node websocket relay), `sidekiq` (background jobs), `db` (Postgres 14),
`redis`. Identity is split across two tables: `users` (Devise auth: email,
encrypted_password, sign_up_ip, locale, time_zone) and `accounts`
(ActivityPub profile: username, display_name, note), joined 1:1 via
`users.account_id`.

- **idRef = `accounts.username`** (local accounts only, `domain IS NULL`) —
  chosen because it is a real, stable, human-chosen, non-numeric handle
  (Mastodon does not allow renaming a local account's username after
  creation), unlike `users.id`/`accounts.id` (a plain autoincrement bigint
  and a Snowflake-style `timestamp_id()` bigint respectively — both
  numeric).
- **3 data types annotated**: `User` (single row/subject: email,
  sign_up_ip, locale, time_zone, display_name, note — spanning both
  physical tables, one logical identity), `Status` (posts/toots, several
  rows/subject: `id`/text/spoiler_text/language, `is_primary_key` on `id`
  per §8.1.c), `PushSubscription` (Web Push registrations, several
  rows/subject: `subscriptionId`/endpoint/key_p256dh/key_auth — the
  primary-key field is named `subscriptionId` on the Provider bridge, not
  `id`, specifically to avoid a `dataName` collision with `Status.id` on
  the 4th endpoint, `dataValue`, which receives no `dataTypeName` and must
  infer the type from `dataName` alone).
- **Provider bridge** (`app/controllers/api/priam_provider_controller.rb`,
  new) — a plain `Api::BaseController` subclass (mirrors
  `Api::V2::InstancesController`, the existing no-auth controller pattern
  in this codebase), routed at bare `/api/{dataAccessRight,rectification,
  erasure,dataValue}` inside the existing `namespace :api` block. All 4
  endpoints implemented, including `dataValue` (§8.2.f). Writes use
  `update_column` (bypasses Rails validations/callbacks by design — a GDPR
  rectification/erasure command is expected to override normal user-facing
  validation, e.g. writing an admin-approved value or blanking a field to
  `''`, exactly like prior case studies' Provider bridges).
- **CEP**: `app/controllers/api/web/push_subscriptions_controller.rb#create`
  gates Web Push subscription registration behind
  `Priam.get_consent(idRef, "Push Notifications")` — the only genuinely
  optional side effect in the app's own code that also transfers personal
  data (the push endpoint URL + encryption keys) to an external third
  party (the browser vendor's Web Push relay — Google FCM / Mozilla
  autopush / Apple Push, depending on the browser), hence the
  `personal_data_transfer`/`secondary_actor` annotation.
- **Registration**: a single `after_create_commit :register_with_priam`
  callback on `User` (`app/models/user.rb`, sibling to the existing
  `trigger_webhooks` callback) enqueues `PriamRegisterSubjectWorker`
  (Sidekiq), which calls `register_data_subject` then
  `report_processed_data` **in that exact sequential order inside the same
  job** (§4bis/§8.6 race) — this one callback covers every local
  user-creation code path uniformly (classic web sign-up, API sign-up via
  `AppSignUpService`, OmniAuth, `tootctl`), since they all persist through
  the same `User` model. `HomeController` (root `/`, the SPA shell)
  redirects to `{PRIAM_FRONTEND_URL}/consent` via
  `has_pending_consent_decision?`, gated on `user_signed_in?`.
  `app/services/post_status_service.rb` reports `Status` fields on every
  real toot published, not just at sign-up (§4bis, "the most frequently
  forgotten point").
- **Bidirectional navigation** (§4ter): "Manage on PRIAM" added to the
  React "More" dropdown menu
  (`app/javascript/mastodon/features/navigation_panel/components/more_link.tsx`)
  — the same menu that already lists "Password and security"/"Import and
  export", hidden unless `PRIAM_FRONTEND_URL` is present, threaded through
  the existing `InitialStateSerializer`/`initial_state.ts` mechanism (the
  same one that already exposes `source_url`/`status_page_url`, no new
  plumbing invented). PRIAM-Frontend's "Back to the app" link points at
  `http://localhost:3000/about` (a real, always-public instance page — not
  bare root) via the root `.env`'s `TARGET_APP_URL`.
- **OAuth2**: `Priam.provision_keycloak_user()` is called from **both**
  local-sign-up code paths that have the user's plaintext password in
  memory: `Auth::RegistrationsController#create` (classic web form, via a
  block passed to Devise's `super`) and `AppSignUpService#create_user!`
  (API sign-up, `POST /api/v1/accounts`, used by 3rd-party apps) — the
  second path was **not** wired in the first pass and was only found by
  actually registering through it during testing (see bug table below).
  Keycloak `username` = email (Mastodon usernames can be as short as 1
  character, below Keycloak's 3-char minimum).

## 2. Scope decisions (documented, not silent)

- **`email` is rectifiable but not erasable** — Devise requires a present,
  valid-format email for login (`validates :email, presence: true,
  email_address: true`); blanking it would break the account's own ability
  to authenticate, not a realistic "erase while keeping the account
  usable" scenario (mirrors Bank of Anthos's `ssn`/`birthday` precedent).
- **`sign_up_ip` is read-only** (not rectifiable/erasable) — a
  registration-time security/abuse-prevention record
  (`sign_up_from_ip_requires_approval?` in `app/models/user.rb`), not a
  field a data subject edits in place.
- **`PushSubscription`'s fields are erasable but not rectifiable** — mirrors
  Habitica's `PushDevice` precedent: nothing about a push endpoint/key is
  meant to be hand-edited by a data controller, only created (by the
  browser), read, or erased.
- **No `MANDATORY` processing annotated.** Unlike Bank of Anthos (SSN/KYC,
  Art. 6.1.c), nothing in Mastodon's own code is processed under a
  distinct legal obligation rather than contract necessity or consent —
  not invented here for the sake of covering all 4 `processing_type`
  values.
- **Keycloak provisioning covers local sign-up only** (both of Mastodon's
  two local-sign-up code paths, see §1) — OmniAuth/SSO sign-up
  (CAS/SAML/OIDC via `omniauth-*` gems, already present in the Gemfile but
  **disabled by default**, `CAS_ENABLED`/`SAML_ENABLED`/`OIDC_ENABLED` all
  default to `'false'`) has no plaintext password to synchronize and was
  not exercised in this session (not enabled in this environment) — a
  `provision_keycloak_user()` call there would be a guarded no-op by
  construction (`return if ... password.blank?`), same documented
  limitation as every prior case study.

## 3. Bugs found this session

All four bugs below live in this session's own new/modified code or in
Mastodon's existing Rails configuration — **none live in PRIAM's own
microservices/frontends** (`git diff --stat -- PRIAM-Services/` is empty
for this session). Per the non-negotiable constraint, nothing was added to
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8 (reserved for generic PRIAM bugs).

| # | Root cause | Fix | Proof of verification |
|---|---|---|---|
| 1 | **`ActionDispatch::HostAuthorization` blocked every Provider bridge call with `403 Forbidden`.** PRIAM-Gateway calls `http://web:3000/api/...` using the internal Docker service name/ephemeral container IP as the `Host` header (`web:3000`, `172.18.0.8:8090`), neither of which is in Rails' `config.hosts` allow-list (only `LOCAL_DOMAIN=localhost:3000` by default). Confirmed via `docker logs mastodon-web-1`: `[ActionDispatch::HostAuthorization::DefaultResponseApp] Blocked hosts: web:3000, 172.18.0.8:8090`. | `config/initializers/1_hosts.rb`: extended the existing `config.host_authorization = { exclude: ... }` lambda (already excluding `/health`) to also exclude the 4 Provider bridge paths by prefix — robust regardless of the caller's ephemeral container IP, unlike adding entries to `config.hosts`. | Before: `POST /right/api/right/answer` (approving a rectification) returned `500` (Feign `403 Forbidden` from the Gateway's call to the Provider bridge). After rebuild: same call chain returns `200`/`FULL`, confirmed against real Postgres state (§4/§7 below). |
| 2 | **`config.force_ssl = true` (hardcoded in `config/environments/production.rb`) 308-redirected every Provider bridge call to HTTPS.** PRIAM-Gateway's Feign client calls over plain HTTP and does not set `X-Forwarded-Proto`, and does not follow redirects — surfaced on the PRIAM-Right-service side as `feign.FeignException: [308 Permanent Redirect]`, and as a `500` to the original caller. | Extended the file's existing `config.ssl_options.redirect.exclude` lambda (already excluding `/health` and `.onion`/`.i2p` hosts) to also exclude the 4 Provider bridge paths — same pattern as bug #1, reusing an already-established Mastodon exclusion mechanism rather than inventing a new one. | Before: `rectificationRequest` → `answer:true` → `500` (`priam-right-ms` logs: `FeignException: [308 Permanent Redirect] ... /provider/api/rectification`). After rebuild: `200 {"answer":"FULL"}`, `users.locale` genuinely changed `en` → `fr` in Mastodon's own Postgres. |
| 3 | **`Priam.provision_keycloak_user()` was only wired into `Auth::RegistrationsController` (classic web sign-up), not `AppSignUpService` (API sign-up, `POST /api/v1/accounts`)** — the seed account was registered through the API path (curl-scriptable, unlike the Devise web form), and its Keycloak account was silently never created. Both paths equally have the plaintext password in memory right after `User.create!`/`resource.save`. | Added the same `Priam.provision_keycloak_user(@user.account&.username, @user.email, @user.password)` call to `AppSignUpService#create_user!`, right after `User.create!`. | Before: `GET /admin/realms/priam-realm/users?username=priam-seed-test@gmail.com` → `[]`. After fix + manual replay for the already-created seed account: real Keycloak user created, `attributes.idReference: ["priam_seed"]` confirmed via the Admin API; a real Direct Grant login (`grant_type=password`, `client_id=Data-client`) succeeded and the returned JWT decodes to `idReference: "priam_seed"`. |
| 4 | **Consent-loss bug: a denied push-resubscribe attempt silently destroyed the existing (already-consented) subscription.** `Api::Web::PushSubscriptionsController` already had `before_action :destroy_previous_subscriptions, only: :create, if: :prior_subscriptions?` — this Rails `before_action` runs **before** the `create` method body, so the CEP check placed inline inside `create` (as §4 instructs: "the inline `if`, in the middle of the business logic, is the right level of granularity") ran **after** the pre-existing subscription had already been destroyed, regardless of the consent decision. Found only through a real test sequence (grant → subscribe → withdraw → attempt resubscribe), not visible from reading the code alone. | Converted the inline check into `before_action :check_priam_consent!, only: :create`, declared **before** `destroy_previous_subscriptions` in the callback chain — Rails runs `before_action`s in declaration order, so a denial now halts the whole chain (via `render` in a `before_action`) before the pre-existing subscription is ever touched. The CEP itself is still an imperative check gating one specific side effect, not a declarative route-level guard blocking the whole controller (§4's actual requirement) — only its position in the callback chain changed. | Before fix: consent granted → subscription #2 created → consent withdrawn → resubscribe attempt (different endpoint) → `403` returned, but `SELECT * FROM web_push_subscriptions` showed **0 rows** (the existing row #2 was destroyed as a side effect of the denied attempt). After fix: identical sequence — `403` returned, `SELECT * FROM web_push_subscriptions` still shows row #2 intact, endpoint unchanged. |

**Investigated, not a bug**: `processed_data.nb_occurrences` for
`PushSubscription` fields went from 1→2 after creating one real
subscription (not 0→1). Root cause confirmed by reading
`ConsentServiceImpl.create()`: PRIAM's own consent-grant mechanism
independently calls `addProcessedData` for every `data_id` tied to the
processing being toggled, on top of Mastodon's own explicit
`report_processed_data()` call at subscription-creation time — the exact
same "double-bookkeeping" behavior already documented and confirmed
harmless in the Bank of Anthos integration report (withdrawal correctly
decremented `nb_occurrences` 2→1, confirmed in §7 below).

## 4. Workflows verified against real state (this session)

| Workflow | Method | Real state checked | Result |
|---|---|---|---|
| Seed registration (API sign-up, `priam_seed`) | curl, `POST /api/v1/apps` → `POST /oauth/token` (client_credentials) → `POST /api/v1/accounts` | `PriamRegisterSubjectWorker` Sidekiq log; `priam-actor.data_subject` (idempotent upsert onto the pre-seeded row 1); `priam-data.processed_data` (User fields 1-6, `nb_occurrences` 1→2 confirming idempotent re-registration) | Registered; upsert confirmed idempotent (no duplicate `data_subject` row) |
| Keycloak provisioning | Manual replay (`Priam.provision_keycloak_user`) after fixing bug #3, then Direct Grant login | Keycloak Admin API user record + `idReference` attribute; real JWT decoded | Correct `idReference: "priam_seed"` claim; login succeeded with the synced password |
| Gateway auth (no token / valid token) | curl through Gateway | HTTP status | `401` with no token on `/right/**`; `200` with a real Keycloak Bearer token |
| Forced-consent redirect | curl with a real Devise session cookie (`/auth/sign_in` → root `/`) | `Location` header | `302` to `http://localhost:4200/consent`, confirming `has_pending_consent_decision?` fired correctly for an undecided, non-numeric-idRef subject |
| Rectification (`User.locale`), `answer=false` then `true` | curl through `PRIAM-Right-service` | `users.locale` (Postgres) | Unchanged after refusal (`en`); changed to `fr` after approval — confirmed |
| Erasure (`User.note`), `answer=false` then `true` | curl through `PRIAM-Right-service` | `accounts.note` (Postgres) | Unchanged after refusal; blanked to `''` after approval — confirmed |
| Status creation → `report_processed_data` | curl `POST /api/v1/statuses` (real Doorkeeper token) | `priam-data.processed_data` for `data_id` 7-10 | Row created with `nb_occurrences=1` immediately after posting |
| Rectification on `Status.text` (composite key `id`) | curl, `primaryKeys:[{"primaryKeyId":7,"primaryKeyValue":"<real status id>"}]` | `statuses.text` (Postgres), scoped to the exact row | Only that row changed — confirmed (§8.1.c scenario) |
| Access request (read) | `GET /right/api/personalDataValues/accessRight` | Live values for `User` and `Status` | Correctly reflects every prior rectification/erasure |
| CEP fail-closed (before any consent decision) | Real browser session (cookie + CSRF token) attempting `POST /api/web/push_subscriptions` | `web_push_subscriptions` row count | Denied (`403`), count stayed `0` |
| Consent grant → optional side effect | Real PRIAM consent API + real session, valid P-256 web-push keys | `web_push_subscriptions` row + `processed_data` | Subscription created; `nb_occurrences` incremented (consent-grant bookkeeping + explicit report) |
| Consent withdrawal → re-grant | Real PRIAM consent API | `consent.end_date`, `processed_data.nb_occurrences`, `get_consent()` response | `end_date` set; `nb_occurrences` decremented 2→1; `get_consent` returned `false`; re-grant created a new `consent` row (`end_date=NULL`) |
| Consent-loss regression (bug #4) | Real session, denied resubscribe attempt | `web_push_subscriptions` row survival | Confirmed fixed — see bug table |
| `dataValue` (4th Provider endpoint, §8.2.f) | curl, direct smoke test, all 3 `DataType`s | Provider bridge response | Correct value for `User` (no `dataTypeName`, inferred from `dataName`), `Status` (inferred + `primaryKeys.id`), `PushSubscription` (inferred + `primaryKeys.subscriptionId`) |
| Backfill for a subject missing its `data_subject` row | Registered a 2nd real account, deleted its `data_subject`/`processed_data` rows directly (simulating "existed before/lost registration"), ran `bin/rails runner priam-integration/backfill-data-subjects.rb` inside the `web` container | `priam-actor.data_subject`, re-run idempotency | Row recreated correctly; re-running the script a 2nd time did not duplicate it |

## 5. Known limitations

- **Real-browser test not completed by the agent.** No browser automation
  tool (Playwright or equivalent) is available in this environment. Every
  workflow above was instead verified either via curl with real database
  proof at each step, or via curl **simulating a real browser session**
  (a genuine Devise session cookie obtained through `/auth/sign_in`, plus
  the real per-page CSRF token, exercising the exact same code paths a
  browser would — including the CSRF protection Mastodon's own
  `Api::Web::BaseController` enforces) — but the specific instruction to
  test "at least once from a real browser" (rendering the PRIAM-Frontend
  Angular consent page, clicking the toggle in a real DOM, visually
  confirming the "Manage on PRIAM" link) was not independently fulfilled.
  Per playbook §7 point 14: frontend visual validation has not been
  performed and end-to-end browser validation is not claimed. Everything
  needed is left running for a manual pass: Mastodon at
  `http://localhost:3000`, a consent-undecided-then-since-decided account
  `priam_seed` / `priam-seed-test@gmail.com` / `PriamSeed!2026`,
  PRIAM-Frontend at `http://localhost:4200`, PRIAM-Frontend-Provider at
  `http://localhost:4000`.
- **Keycloak provisioning covers local sign-up only** (playbook §4bis) —
  OmniAuth/SSO sign-up has no plaintext password to synchronize and was
  not exercised (disabled by default in this environment); a real
  Keycloak-backed OmniAuth login for Mastodon itself (playbook §6 "Family
  3", already-native OIDC IdP) was **not** attempted this session — only
  Keycloak *provisioning at local sign-up* (§4bis) was implemented, which
  is what the task requires. A stray comment in the root
  `docker-compose.yml`'s `keycloak` service (`KC_HOSTNAME`, mentioning "see
  case-studies/Mastodon/priam-integration/INTEGRATION-REPORT.md, Family 3
  test") predates this session and refers to work that was never actually
  done in this repository (verified via `git log --all` — the only
  existing commit is the Bank of Anthos integration) — left untouched
  since editing PRIAM's own shared `docker-compose.yml` comments is out of
  this session's scope, but flagged here for transparency.
- **`streaming`'s host port was remapped 4000→4001** (Mastodon's own
  `docker-compose.yml`) — a real port collision with
  `PRIAM-Frontend-Provider` (which already owns host port 4000). Does not
  affect any GDPR rights/consent mechanism; `streaming` is Mastodon's
  websocket relay for live timeline updates, untouched by this
  integration otherwise.
- **`processed_data` double-bookkeeping for `OPTIONAL` processings**
  (`PushSubscription`) — both PRIAM's own consent toggle and Mastodon's
  explicit `report_processed_data()` call independently manage the same
  `data_id`s (see §3, "Investigated, not a bug"). Harmless, same
  documented behavior as Bank of Anthos.

## 6. LOC breakdown

**Method**: `git diff --numstat` (per-file table below) gives raw +/-
counts only, as expected — it does **not** classify code vs. comment vs.
blank. That classification was done by a small Python script
(`priam-integration/` scratch, not committed) applying a fixed rule per
language: after stripping whitespace, a line starting with `#` (Ruby/YAML/
SQL uses `--`) or `//`/`/*`/`*` (TypeScript/TSX) = comment; an empty line =
blank; everything else = code. For **new** files, every line of the final
file is classified. For **modified** files, only lines actually added this
session are classified (extracted via `git diff --unified=0`, lines
starting with a single `+`, `+++` excluded) — a manual-rule script, not a
sophisticated tool, applied consistently across every file. The root
`.env` (not git-tracked) was classified manually from the two edits made
(exact before/after content known from the edit itself, not reconstructed).

**`app/lib/priam.rb` and `case-studies/Mastodon/docker-compose.yml` both
span two categories** (Consent and OAuth2) — split by function/line range,
detailed inline below the category table, per the rule "one line = counted
in a single functional category, wherever it physically lives."
`config/initializers/1_hosts.rb` and `config/environments/production.rb`
are counted under **Rights-API**, not Docker-network: both changes exist
solely to make the 4 Provider bridge endpoints (§2/§3) reachable at all
(bug fixes #1/#2 above), not general Docker/network wiring.

### Per-file

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | modified (full rewrite for Mastodon) | 184 | 181 |
| `case-studies/Mastodon/app/controllers/api/priam_provider_controller.rb` | **new** | 181 | 0 |
| `case-studies/Mastodon/app/lib/priam.rb` | **new** | 171 | 0 |
| `case-studies/Mastodon/app/workers/priam_register_subject_worker.rb` | **new** | 22 | 0 |
| `case-studies/Mastodon/priam-integration/backfill-data-subjects.rb` | **new** | 29 | 0 |
| `case-studies/Mastodon/app/controllers/api/web/push_subscriptions_controller.rb` | modified | 16 | 0 |
| `case-studies/Mastodon/app/controllers/auth/registrations_controller.rb` | modified | 7 | 1 |
| `case-studies/Mastodon/app/controllers/home_controller.rb` | modified | 13 | 0 |
| `case-studies/Mastodon/app/javascript/mastodon/features/navigation_panel/components/more_link.tsx` | modified | 10 | 0 |
| `case-studies/Mastodon/app/javascript/mastodon/initial_state.ts` | modified | 2 | 0 |
| `case-studies/Mastodon/app/models/user.rb` | modified | 9 | 0 |
| `case-studies/Mastodon/app/serializers/initial_state_serializer.rb` | modified | 1 | 0 |
| `case-studies/Mastodon/app/services/app_sign_up_service.rb` | modified | 5 | 0 |
| `case-studies/Mastodon/app/services/post_status_service.rb` | modified | 4 | 0 |
| `case-studies/Mastodon/config/environments/production.rb` | modified (bug #2 fix) | 8 | 2 |
| `case-studies/Mastodon/config/initializers/1_hosts.rb` | modified (bug #1 fix) | 9 | 1 |
| `case-studies/Mastodon/config/routes/api.rb` | modified | 7 | 0 |
| `case-studies/Mastodon/docker-compose.yml` | modified | 40 | 7 |
| `docker-compose.yml` (PRIAM root, `name:` field) | modified | 1 | 1 |
| `.env` (PRIAM root, not git-tracked — `CUSTOM_PROVIDER_URL`, `TARGET_APP_URL`) | modified | 13 | 12 |
| `case-studies/Mastodon/.env.production` (generated local secrets, gitignored, not source code) | **new**, excluded from the breakdown below | — | — |

### By functional category × line nature (this session)

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| **Annotation** (`db_insertion_script.sql`) | 58 | 118 | 8 | 184 |
| **Rights-API** (`priam_provider_controller.rb` + `routes/api.rb` + the two bug-fix files) | 129 | 46 | 30 | 205 |
| **Consent** (CEP/registration/report/backfill/navigation — see breakdown below) | 128 | 72 | 38 | 238 |
| **OAuth2** (Keycloak-provisioning portions) | 51 | 18 | 3 | 72 |
| **Docker-network** (the rest of `docker-compose.yml` ×2 + `.env`) | 12 | 21 | 0 | 33 |
| **Total** | **378** | **275** | **79** | **732** |

**Consent category, file-by-file** (128/72/38): `priam.rb` Consent-portion
(the whole file minus the OAuth2 lines counted below) 72/26/21 ·
`priam_register_subject_worker.rb` (new, full file) 10/9/3 ·
`backfill-data-subjects.rb` (new, full file) 11/12/6 ·
`push_subscriptions_controller.rb` diff 6/8/2 · `home_controller.rb` diff
6/3/4 · `user.rb` diff 4/4/1 · `post_status_service.rb` diff 1/3/0 ·
`initial_state_serializer.rb` diff 1/0/0 · `initial_state.ts` diff 2/0/0 ·
`more_link.tsx` diff 7/2/1 · `docker-compose.yml` (Mastodon) Consent-portion
(the `PRIAM_CDP_URL`/`PRIAM_ACTOR_URL`/`PRIAM_DATA_URL`/`PRIAM_FRONTEND_URL`
lines + their header comments, both `web` and `sidekiq` services) 8/5/0.

**OAuth2 category, file-by-file** (51/18/3): `priam.rb`'s
`provision_keycloak_user()` + `keycloak_admin_token()` methods 39/10/3 ·
`registrations_controller.rb` diff 3/4/0 · `app_sign_up_service.rb` diff
1/4/0 · `docker-compose.yml` (Mastodon) `KEYCLOAK_*` env lines (both
services) 8/0/0.

**Rights-API category, file-by-file** (129/46/30):
`priam_provider_controller.rb` (new, full file) 123/29/29 ·
`routes/api.rb` diff 4/2/1 · `1_hosts.rb` diff (bug #1) 1/8/0 ·
`environments/production.rb` diff (bug #2) 1/7/0.

**Docker-network category, file-by-file** (12/21/0): `docker-compose.yml`
(Mastodon) — `build: .` + `environment:` keys + `common_network` network
attachments + the port remap + the bottom `common_network: external: true`
block, both services 9/10/0 · `docker-compose.yml` (root) `name:` field
1/0/0 · `.env` (root) `CUSTOM_PROVIDER_URL`/`TARGET_APP_URL` values +
comments 2/11/0.

### PRIAM's own LOC (this session)

**Zero.** No file under `PRIAM-Services/` was opened for editing this
session (`git diff --stat -- PRIAM-Services/` is empty) — all 4 bugs found
(§3) live in this session's own Mastodon-side code or in Mastodon's
pre-existing Rails security configuration, not in PRIAM. Per the
non-negotiable constraint, nothing was added to
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8.
