# PRIAM ↔ Habitica — Integration Report

## 1. Mechanism, in one page

Habitica (`case-studies/Habitica/website`) is a Node/Express + MongoDB
(Mongoose) app — `server` (API, port 3000), `client` (Vue 2 SPA via Vite,
port 5173), `mongo` (MongoDB 7, replica set `rs`). No SQL database anywhere,
unlike every prior case study — the annotation script still lives in PRIAM's
own MySQL, it just describes Mongo collections/fields via `source_details`
text instead of real foreign keys.

- **idRef = `User._id`** — Habitica's own primary key is already a UUID v4
  (`server/libs/baseModel.js`, applied to both `User` and `Task`), so every
  subject, seeded or dynamically registered, is non-numeric by construction
  (playbook §7) with zero special-casing needed — unlike case studies whose
  natural primary key is numeric and had to pick a secondary field instead.
- **3 data types annotated**: `User` (single row/subject: username, email,
  displayName), `Task` (several rows/subject: id/text/notes, `is_primary_key`
  on `id` per §8.1.c), `PushDevice` (an embedded subdocument, several
  rows/subject, `regId` as the composite key — no surrogate id, same pattern
  as Bank of Anthos's `Contact.label`).
- **Provider bridge**
  (`server/controllers/top-level/priamProvider.js`, new) — lives directly in
  Habitica's own Express app (auto-discovered by `libs/routes.js`'s
  `walkControllers`, mounted at bare `/`), bare `/api`, no auth. All 4
  endpoints implemented, including `dataValue` (§8.2.f).
- **CEP**: `server/controllers/api-v3/pushNotifications.js`'s
  `addPushDevice()` wraps the Mongo write in
  `if (await priam.getConsent(...))` — registering a push-notification token
  is the only genuinely optional side effect found in the app's own code,
  and a real transfer of personal data to two external processors (Apple
  APNs, Google Firebase — confirmed via real `@parse/node-apn`/
  `firebase-admin` clients in `server/libs/pushNotifications.js`, not mocked),
  hence the `personal_data_transfer`/`secondary_actor` annotation.
- **Registration**: `server/libs/auth/index.js`'s `registerLocal()` and
  `server/libs/auth/social.js`'s `loginSocial()` both call
  `priam.onUserRegistered()` after `.save()` resolves — a single new module,
  `server/libs/priam.js`, sequences `registerDataSubject` →
  `reportProcessedData` (User fields, then once per default Task actually
  created) → `provisionKeycloakUser`, in that exact order (§4bis/§8.6 race),
  fire-and-forget from the caller's perspective. `server/libs/user/index.js`
  injects `priamConsentRequired` into the existing `GET /api/v4/user`
  response; `server/libs/tasks/index.js`'s `createTasks()` reports Task
  fields on every task creation, not just at sign-up (§4bis, "most
  frequently forgotten point").
- **Client-side** (Vue 2, a hand-rolled Vuex-like store, no Vuex/Pinia):
  `client/src/router/index.js`'s existing `router.afterEach` hook (already
  present, previously used for one unrelated thing) now also redirects to
  `{PRIAM_FRONTEND_URL}/consent` once, gated on not being on the
  registration routes (§8.7 pattern) and on the freshest known
  `priamConsentRequired` flag — Habitica's own registration flow reveals no
  one-time secret, so the specific §8.7 failure mode doesn't apply here, but
  the same defensive pattern (gate on route-change completion, not on the
  user-fetch resolving) was still used for robustness against the
  independent two-hop `/register` → `/username` signup race.
- **Bidirectional navigation** (§4ter): a new "Manage on PRIAM" row
  (`client/src/pages/settings/siteDataRows/priamRow.vue`) in Habitica's own
  Site Data settings page, hidden unless `PRIAM_FRONTEND_URL` is baked in
  (`vite.config.mjs`, extending the existing `envVars`-to-`import.meta.env`
  mechanism); PRIAM-Frontend's "Back to the app" link points at
  `http://localhost:5173/static/front` (a real marketing/home page, not bare
  root) via the root `.env`'s `TARGET_APP_URL`.
- **OAuth2**: `provisionKeycloakUser()` in `priam.js` auto-creates a matching
  Keycloak account at every local sign-up, using **email** as the Keycloak
  `username` (never the Habitica handle — real accounts as short as 1
  character exist in this app, below Keycloak's 3-char minimum, §4bis/§8.8),
  `firstName`/`lastName` reused from the email (Habitica has no separate
  name fields), verified end-to-end including a real Direct Grant login
  (§9).

## 2. Bugs found this session (all in my own new code, none in PRIAM)

| # | Root cause | Fix | Proof of verification |
|---|---|---|---|
| 1 | `server/libs/priam.js` called `logger.warn(...)` in 4 places — Habitica's own `server/libs/logger.js` exports a public interface (`loggerInterface`) with only `.info()`/`.error()`, no `.warn()` (the raw winston logger has one, but it's not part of the exported interface). Crashed the entire background PRIAM chain on every registration with `TypeError: _logger.default.warn is not a function`, silently swallowing `registerDataSubject`/`reportProcessedData`/`provisionKeycloakUser` for that user. | All 4 call sites changed to `logger.error(err, 'message')` (or `logger.info(msg, {data})` for the one non-Error case), matching every other file in this codebase. | Re-ran the exact same registration after the fix: `docker logs habitica-server-1` showed a clean `error`-level entry (Keycloak genuinely unreachable at that point) with no crash, no unhandled rejection — see ETAPES-FAITES.md §4. |
| 2 | `priamProvider.js` imported `{ model as Task }` from `server/models/task.js`, copying the exact pattern used for `User` (`server/models/user/index.js` does export `model`) — but `models/task.js` exports the model as a **named** export `Task` (`export const Task = mongoose.model('Task', TaskSchema);`), not `model`. Silently bound to `undefined` (ES module named-import of a non-existent export doesn't error at import time in this Babel/webpack setup), crashing every `dataAccessRight`/`rectification`/`erasure`/`dataValue` call for the `Task` type with `TypeError: Cannot read properties of undefined (reading 'find')`. | `import { Task } from '../../models/task';`. | Re-tested `GET /api/dataAccessRight?...dataTypeName=Task...` directly, then again through the real rights workflow (§7 rectification test) — both confirmed working, see ETAPES-FAITES.md §4-7. |
| 3 | Fresh checkout had no `config.json` (only the gitignored `config.json.example`) — `client/vite.config.mjs`'s `nconf.get('BASE_URL').indexOf(...)` crashes the Docker build with no config file present. Not a PRIAM bug, a target-application prerequisite (§7). | `cp config.json.example config.json` before the first build. | Build succeeded afterward; documented so it isn't rediscovered. |
| 4 | Old BankOfAnthos-session containers survived under project name `priam-bankofanthos`; the root `docker-compose.yml`'s `name:` was changed to `priam-habitica` for this session, so `docker compose down` (scoped by project name) found nothing to remove — the reverse of the playbook's §5 documented pitfall (that one warns about two *simultaneous* checkouts; this is one checkout whose project identity changed mid-lifecycle, orphaning its own previous containers). | Force-removed the old containers by their fixed `container_name` values, cleared `db-volume/`, rebuilt. | Fresh stack came up cleanly under the new project name; `docker ps -a` confirmed no leftover `priam-bankofanthos`-labeled containers remained. |
| 5 | `secondary_actor_name` (`varchar(40)`) overflowed with `'Apple APNs / Google Firebase Cloud Messaging'` (44 chars) — `ERROR 1406 Data too long for column`, caught by MySQL itself on the very first `db_insertion_script.sql` load. | Shortened to `'Apple APNs / Google FCM'` (23 chars). | `docker logs priam-databases` showed the full script complete with no further errors after the fix; healthcheck went `healthy`. |

**No generic PRIAM bug was found this session** — all 5 issues above are in
code I wrote for this integration or in environment/prerequisite setup, not
in PRIAM's own microservices/frontends. Per the non-negotiable constraint,
**0 lines were changed in PRIAM** (`git diff --stat -- PRIAM-Services/` shows
only the pre-existing, already-committed-elsewhere `consent.component.ts`
`MANDATORY`-filter fix from the prior Bank of Anthos session's own report —
untouched by this session).

## 3. Scope decisions (documented, not silent)

- **No `MANDATORY` processing annotated.** Unlike Bank of Anthos (SSN/KYC,
  a genuine Art. 6.1.c legal obligation), nothing in Habitica's own code is
  processed under a legal-obligation basis distinct from contract necessity
  or consent — inventing one purely to exercise all 4 `processing_type`
  values would misrepresent the app's real legal basis. `DEFAULT`
  (Authentication), `NECESSARY` (Account Management, Task Management), and
  `OPTIONAL` (Push Notifications) are all genuinely exercised.
- **`analyticsConsent`** (`user.preferences.analyticsConsent`, already a
  GDPR-style consent flag in Habitica's own code, `client/src/libs/analytics.js`)
  was considered as a second OPTIONAL processing but **not annotated**: the
  gating happens entirely client-side (the browser decides whether to fire
  an analytics event), with no server-side record of "whether this event
  fired" to report back into `processed_data` in the way §1/§4 assume — it
  doesn't fit the CEP pattern (a server-side optional side effect to gate),
  so it was left as an existing, independent client-side privacy mechanism
  rather than force-fit into PRIAM.
- **`email`/`username` full CRUD, not read-only**: unlike Bank of Anthos
  (where `username`/`accountid` were the login identifiers and left
  read-only), Habitica's idRef is the internal UUID `_id`, not `username` —
  so `username`/`email` are ordinary rectifiable/erasable `Account
  Management` fields here, with no identity-collision risk from erasing
  them (only the demo scenario is affected, not the annotation's
  correctness).
- **Erasing a `Task` field blanks it to `''`, it does not delete the whole
  task** — unlike Bank of Anthos's `Contact.label` (which had no separate
  surrogate id, so erasing the identifying field had to delete the row),
  `Task._id` is a genuine separate primary key column, so `text`/`notes`
  erase in place, exactly like a normal single-row field.
- **Erasing `PushDevice.regId` or `.type` removes the whole device** (no
  separate surrogate id on the embedded subdocument — same reasoning as
  Bank of Anthos's `Contact.label`).

## 4. Workflows verified against real state (this session)

| Workflow | Method | Real state checked | Result |
|---|---|---|---|
| Seed subject registration | curl, real `POST /api/v4/user/auth/local/register` | Mongo `users` doc, `_id` a real UUID | Created; `tasksOrder` correctly empty (habitica-web client creates no default tasks at signup, confirmed in `hooks.js`) |
| Provider bridge, all 4 endpoints | curl, direct | Real Mongo `users`/`tasks` documents | `dataAccessRight` (User/Task/PushDevice), `dataValue` (both single-row and composite-key) all confirmed against live data |
| CEP fail-closed (PRIAM unreachable) | curl `addPushDevice` before PRIAM stack started | Mongo `pushDevices` | Correctly denied (stayed `[]`) — proves fail-closed, not just fail-open |
| Auth: no token / valid token | curl through Gateway | HTTP status | `401` with no token, `200` with a real Keycloak Bearer token (`idReference` claim decoded and matched) |
| Access request, `answer=false` then `true` | curl through `PRIAM-Right-service` | `data_request_answer` row, then the always-open `personalDataValues/accessRight` read | `REFUSED` recorded and no further effect; `FULL` recorded and live data read back correctly |
| Rectification (`Task.text`, composite key) | curl, `answer=false` then `true` | Real MongoDB `tasks.text` | Unchanged after refusal; changed after approval — confirmed the correct one of 2 real tasks was touched (§8.1.c scenario) |
| Erasure (`User.displayName`) | curl, `answer=false` then `true` | Real MongoDB `users.profile.name` | Unchanged after refusal; blanked to `''` after approval |
| Consent grant (pre-seeded) → optional side effect | curl `addPushDevice` while granted | Mongo `pushDevices`, MySQL `processed_data` | Device added; `nb_occurrences` incremented 1→2 |
| Consent withdrawal | curl consent toggle | MySQL `consent.end_date`, `processed_data.nb_occurrences` | `end_date` set; `nb_occurrences` decremented 2→1; a further device-add attempt correctly blocked (Mongo count stayed at 2) |
| Consent re-grant | curl consent toggle | MySQL `consent` (new row), Mongo `pushDevices`, `processed_data` | New granted row created; blocked device-add now succeeds; `nb_occurrences` back to 3 |
| Fresh registration, full automatic chain | curl registration, no manual steps | MySQL `data_subject`, `processed_data`; Keycloak Admin API; real Direct Grant login | `data_subject` created; `processed_data` reported (User fields); Keycloak account created with correct `idReference`; login succeeded (no "Account is not fully set up") |
| `priamConsentRequired` flag lifecycle | curl `GET /api/v4/user` before/after a real consent decision | JSON response field | `true` before any decision, `false` immediately after — confirmed no redirect-loop risk |
| Backfill script, genuinely pre-existing user | Direct Mongo insert (bypassing the app) + `node backfill-data-subjects.mjs` inside the server container | MySQL `data_subject`, `processed_data`; re-run idempotency | Created correctly (1 task → `nb_occurrences=1` for Task fields); re-running the script a 2nd time did not duplicate the `data_subject` row |
| Real browser test | **Not run this session** — handed off to the user to run manually (Playwright container image pull repeatedly stalled on this sandbox's Docker Hub connectivity; no browser tooling available locally) | — | See "Known limitations" below |

## 5. Known limitations

- **Real-browser test not completed by the agent.** Every workflow above
  was verified with real curl requests and real database/state proof at
  each step (§7's actual requirement — a `200`/`FULL` proves nothing by
  itself, so every test reads the real MongoDB/MySQL state directly), but
  the specific instruction to test "at least once from a real browser" was
  not independently fulfilled: this sandbox has no browser or Playwright
  installed, and pulling a Playwright container image
  (`mcr.microsoft.com/playwright:v1.48.0-jammy`) repeatedly stalled on the
  same Docker Hub connectivity issue documented in playbook §8.9, well
  beyond the point where retries were still a reasonable use of time. The
  user opted to run this step manually instead. Everything needed is left
  running and ready: Habitica at `http://localhost:5173`, a fresh
  consent-undecided account (`priam-browser-test` /
  `priam-browser-test@example.com` / `BrowserTest123!`, Keycloak-provisioned
  with the same credentials), PRIAM-Frontend at `http://localhost:4200`,
  PRIAM-Frontend-Provider at `http://localhost:4000`.
- **Keycloak provisioning covers local sign-up only** (playbook §4bis) — the
  social/OAuth path (`server/libs/auth/social.js`) has no plaintext password
  to synchronize into Keycloak, so `provisionKeycloakUser()` is a guarded
  no-op there (`if (!password) return;`) by design, not an oversight. A user
  who signs up via Google/Apple gets a PRIAM `data_subject` and full rights/
  consent coverage, just no automatic Keycloak SSO account.
- **`analyticsConsent`** (see §3) is a real, pre-existing GDPR-style consent
  mechanism in Habitica's own code that was deliberately left outside this
  integration — it doesn't fit the CEP's "gate one server-side optional
  side effect" pattern, since the decision and its effect are both
  client-side only.

## 6. LOC breakdown

**Method**: manual reading, file by file. For each **new** file, every line
of the final file is classified as it stands (a line starting with `//`
after trimming whitespace = comment for JS/Vue, `--` for SQL; an empty or
whitespace-only line = blank; everything else = code). For each **modified**
file, only the lines actually added this session are classified, extracted
via `git diff` (lines starting with a single `+`, `+++` excluded) — this
matches the method BankOfAnthos's own report used, and is stated explicitly
here per the task's instruction not to black-box `git diff --numstat` as if
it already gave this breakdown (it only gives raw +/- counts per file, shown
separately in the per-file table below). No automated tool beyond a small
`awk` one-liner applying the prefix rule above — a plain manual method, not
a sophisticated one.

**A note on `Databases/db_insertion_script.sql`**: this file is a full,
line-by-line rewrite for Habitica (the previous content was 100%
Bank-of-Anthos-specific and no longer applies). `git diff --numstat` reports
159 insertions/178 deletions rather than "193 inserted, 213 deleted" because
Git's diff algorithm happens to match a handful of textually identical lines
between the two versions (e.g. bare `USE \`priam-actor\`;` lines) — these are
coincidental text matches, not carried-over content. The category/nature
breakdown below therefore classifies the **entire current file** (193
lines) as this session's Annotation contribution, not just the diff's
"added" subset, since the file's whole content is this session's work.

**`server/libs/priam.js` and the Habitica-side `docker-compose.yml` diff
both span two categories** (Consent and OAuth2) — split by function/line
range as described inline in the category table below, per the rule "one
line = counted in a single functional category, wherever it physically
lives."

### Per-file

| File | Status | +lines | -lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | modified (full rewrite for Habitica) | 159 | 178 |
| `case-studies/Habitica/website/server/libs/priam.js` | **new** | 173 | 0 |
| `case-studies/Habitica/website/server/controllers/top-level/priamProvider.js` | **new** | 193 | 0 |
| `case-studies/Habitica/priam-integration/backfill-data-subjects.mjs` | **new** | 85 | 0 |
| `case-studies/Habitica/website/client/src/pages/settings/siteDataRows/priamRow.vue` | **new** | 30 | 0 |
| `case-studies/Habitica/website/server/libs/auth/index.js` | modified | 9 | 0 |
| `case-studies/Habitica/website/server/libs/auth/social.js` | modified | 7 | 0 |
| `case-studies/Habitica/website/server/libs/user/index.js` | modified | 6 | 0 |
| `case-studies/Habitica/website/server/libs/tasks/index.js` | modified | 9 | 0 |
| `case-studies/Habitica/website/server/controllers/api-v3/pushNotifications.js` | modified | 15 | 8 |
| `case-studies/Habitica/website/client/src/router/index.js` | modified | 14 | 0 |
| `case-studies/Habitica/website/client/vite.config.mjs` | modified | 6 | 0 |
| `case-studies/Habitica/website/client/src/pages/settings/siteData.vue` | modified | 3 | 0 |
| `case-studies/Habitica/website/common/locales/en/settings.json` | modified | 2 | 0 |
| `case-studies/Habitica/docker-compose.yml` | modified | 19 | 0 |
| `docker-compose.yml` (PRIAM root, `name:` field) | modified | 1 | 1 |
| `.env` (PRIAM root, not git-tracked — `CUSTOM_PROVIDER_URL` value+comment, `TARGET_APP_URL` value) | modified | 6 | 6 |
| `case-studies/Habitica/priam-integration/browser-test.mjs` | **new**, test tooling only — excluded from the category table below (not application/PRIAM code) | 42 | 0 |

### By functional category × line nature (this session)

| Category | Code | Comment | Blank | Total |
|---|---|---|---|---|
| **Annotation** (`db_insertion_script.sql`, full file) | 65 | 110 | 18 | 193 |
| **Rights-API** (`priamProvider.js`, full file) | 167 | 14 | 12 | 193 |
| **Consent** (CEP/registration/report/backfill/navigation portions — see breakdown below) | 215 | 71 | 29 | 315 |
| **OAuth2** (Keycloak-provisioning portions of `priam.js` + `docker-compose.yml`) | 47 | 12 | 0 | 59 |
| **Docker-network** (the rest of `docker-compose.yml` ×2 + `.env`) | 5 | 6 | 0 | 11 |
| **Total** | **499** | **213** | **59** | **771** |

**Consent category, file-by-file** (215/71/29):
`priam.js` Consent-portion (`getConsent`/`registerDataSubject`/
`hasPendingConsentDecision`/`reportProcessedData`/`onUserRegistered`'s
Consent-side lines, i.e. the whole file minus the OAuth2 lines counted
below) 86/23/9 · `backfill-data-subjects.mjs` 54/19/12 · `priamRow.vue`
27/2/1 · `siteData.vue` diff 3/0/0 · `router/index.js` diff 10/3/1 ·
`vite.config.mjs` diff 1/4/1 · `settings.json` diff 2/0/0 ·
`pushNotifications.js` diff 9/5/1 · `auth/index.js` diff 6/2/1 ·
`auth/social.js` diff 4/3/0 · `tasks/index.js` diff 4/3/2 ·
`user/index.js` diff 3/2/1 · `docker-compose.yml` (Habitica) Consent-portion
(the `PRIAM_CDP_URL`/`PRIAM_ACTOR_URL`/`PRIAM_DATA_URL`/`PRIAM_FRONTEND_URL`
lines + their header comment) 5/5/0 · `.env` `TARGET_APP_URL` line 1/0/0.

**OAuth2 category, file-by-file** (47/12/0): `priam.js`'s
`provisionKeycloakUser()` function plus the `KEYCLOAK_*` constants plus its
one call site inside `onUserRegistered` 43/12/0 · `docker-compose.yml`
(Habitica) `KEYCLOAK_*` env lines 4/0/0.

**Docker-network category, file-by-file** (5/6/0): `docker-compose.yml`
(Habitica) `common_network` wiring (the `- common_network` network
attachment + the bottom `common_network: external: true` block) 3/2/0 ·
`docker-compose.yml` (root) `name:` field 1/0/0 · `.env`
`CUSTOM_PROVIDER_URL` value+comment 1/4/0.

### PRIAM's own LOC (this session)

**Zero.** No file under `PRIAM-Services/` was opened for editing this
session (verified via `git diff --stat -- PRIAM-Services/`, which shows
only the `PRIAM-Frontend/.../consent.component.ts` `MANDATORY`-filter fix —
already present in the working tree at the start of this session, carried
over uncommitted from a prior Bank of Anthos session per that case study's
own `INTEGRATION-REPORT.md` §3 bug #2, not touched or re-verified here).
