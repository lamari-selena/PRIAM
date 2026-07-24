# PRIAM × Ghostfolio — Integration Report

Ghostfolio ([case-studies/Ghostfolio](..)) is an open-source personal
wealth/portfolio tracker: NestJS + Prisma/PostgreSQL API and an Angular
client, served from a single process on port `3333`. This integration
follows the contract in `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §0-§7; the
raw, reproducible test log lives in
[`ETAPES-FAITES.md`](./ETAPES-FAITES.md).

## 1. Mechanism, in one page

**Real schema annotated** (`prisma/schema.prisma`, verified — no invented
tables/columns):

| `data_type` | Table | Rows/subject | Fields annotated |
|---|---|---|---|
| `User` | `"User"` | 1 | `id`, `provider`, `thirdPartyId`, `createdAt` |
| `Account` | `"Account"` | N (composite PK `[id,userId]`) | `id`, `name`, `currency`, `balance` |
| `Order` | `"Order"` | N | `id`, `type`, `currency`, `quantity`, `unitPrice`, `fee`, `date`, `comment` |
| `Analytics` | `"Analytics"` | 1 | `country`, `activityCount`, `lastRequestAt` |

Three `processing`s, deliberately using 3 of the playbook's 4
`processing_type`s (not invented for coverage — each is a genuine, distinct
mechanism in Ghostfolio's own code):

- **`Authentication`** (`DEFAULT`) — JWT/OAuth login (`JwtStrategy.validate`,
  `AuthService.validateOAuthLogin`). No consent needed, not shown on the
  consent page.
- **`Portfolio Management`** (`NECESSARY`) — the default `Account` created
  transactionally inside `UserService.createUser()`, plus every
  `AccountService.createAccount`/`ActivitiesService.createActivity` call.
  Core functionality, not revocable.
- **`Usage Analytics`** (`OPTIONAL`) — the **only** genuinely optional
  processing found in Ghostfolio's own code: `JwtStrategy.validate` and
  `ApiKeyStrategy.validate` upsert an `Analytics` row (activity count,
  country, last-request time) **only** when `ENABLE_FEATURE_SUBSCRIPTION`
  is on, and authentication succeeds identically whether or not that upsert
  runs (confirmed by reading every call site — the result is never used to
  gate anything else). This is the processing gated by the CEP.

**`idRef` = `User.id`** (Prisma `String @id @default(uuid())`) — always a
non-numeric UUID by construction, satisfying playbook §7's non-numeric-idRef
requirement for *every* real subject, not just a specially-crafted one.

**Provider bridge** (`apps/api/src/app/provider-bridge/provider-bridge.controller.ts`):
one new NestJS controller, `@Version(VERSION_NEUTRAL)` on each of the 4
routes (method-level, matching the codebase's own convention in
`auth.controller.ts`/`sitemap.controller.ts` — a class-level `@Version`
does not type-check with this NestJS version, see bug table below), no
guards (unauthenticated, machine-to-machine). Registered once in
`app.module.ts`. `dataAccessRight` always answers a JSON array;
`rectification`/`erasure` restrict mutation to a `MUTABLE` whitelist
narrower than the read `WHITELISTS` (system/audit fields — `id`,
`provider`, `type`, every required timestamp — stay access-only, mirrored
exactly by the SQL script's `data_usage` `c/r/u/d` flags so PRIAM's UI never
offers an action the bridge would reject); `dataValue` infers `dataTypeName`
from `dataName`'s whitelist (§8.2.f), with no `dataTypeName` in its body.

**CEP** (`apps/api/src/services/priam/priam.service.ts`, `getConsent()`):
wraps *only* the `Analytics` upsert in both `jwt.strategy.ts` and
`api-key.strategy.ts` — authentication itself is never gated.

**Registration** (`PriamService.onUserRegistered()`): called from a
**single choke point**, the end of `UserService.createUser()` — every
sign-up path (anonymous, Google OAuth, OIDC) funnels through this one
function, so one hook covers all of them (minimal-footprint requirement).
Sequenced per §4bis/§8.6: `await registerDataSubject()` → `await
reportProcessedData(User)` → `await reportProcessedData(Account)` (the
default Account created in the same transaction) → `await
provisionKeycloakUser()`, all inside a fire-and-forget IIFE so the HTTP
response is never delayed by PRIAM's availability.

`report_processed_data()` also fires at every **later** personal-record
creation, not just sign-up (§4bis, "the most frequently forgotten point"):
`AccountService.createAccount` and `ActivitiesService.createActivity`
(1-line calls each), plus the `Analytics` upsert itself.

**Forced consent / navigation**: `UserService.getUser()` exposes
`priamConsentRequired` (via `hasPendingConsentDecision`) on the existing
`GET /api/v1/user` response; the Angular client (`app.component.ts`)
triggers the redirect from the `NavigationEnd` router event — not from the
`userService.stateChanged` event that also delivers the registration
wizard's one-time security token (§8.7, see bug table). "Manage on PRIAM"
(`user-account-settings.html`) and PRIAM's own "Back to the app" link
(`TARGET_APP_URL=http://localhost:3333/`, a real working page — Ghostfolio
serves its Angular client from the same process, root path included) are
both wired.

**Keycloak provisioning** (§4bis "Automatic..."): Ghostfolio's anonymous
sign-up produces no email/password, only a one-time `accessToken` — reused
as the Keycloak password (the "plaintext secret only available at this
exact moment" the playbook describes). Username synthesized as
`{userId}@ghostfolio.local` (no email/handle of its own, §8.8) and
surfaced read-only in Account Settings next to "Manage on PRIAM" so the
user knows what to type on Keycloak's login screen. **Limitation, stated
rather than glossed over**: covers the anonymous sign-up flow only — a
Google/OIDC sign-in has no equivalent secret to sync, matching the
playbook's own documented scope for this pattern.

## 2. Bugs found and fixed during this session

All fixes below are **target-application-side** (Ghostfolio/Docker-environment
code), consistent with the non-negotiable "0 lines changed in PRIAM" —
no PRIAM microservice/frontend code was modified. No new *generic* PRIAM bug
was found (everything below is specific to this integration's own code or
this Windows checkout's environment), so nothing was added to
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8.

| # | Root cause | Fix | Verified |
|---|---|---|---|
| 1 | `docker/entrypoint.sh` checked out with CRLF line endings on this Windows host (`#!/bin/sh\r`) → `exec: /ghostfolio/entrypoint.sh: not found` at every container start. Not a PRIAM/Ghostfolio code bug — a Windows-checkout artifact (no `.gitattributes` in upstream Ghostfolio forcing LF). | Converted the file to LF; added `case-studies/Ghostfolio/.gitattributes` (`*.sh text eol=lf`) so it can't silently recur. | Container went from a crash-loop (`docker logs ghostfolio`, exit 127 repeatedly) to `healthy` after rebuild; `od -c` on the file inside the built image confirmed LF. |
| 2 | `@Version(VERSION_NEUTRAL)` applied at **class** level on `ProviderBridgeController` failed to compile (`TS1270: Decorator function return type ... is not assignable`) — this NestJS/TS version's typings don't support it there, even though the class-level form appears in some NestJS docs. | Moved `@Version(VERSION_NEUTRAL)` to **each** of the 4 route methods — the same convention the codebase already uses in `auth.controller.ts`/`sitemap.controller.ts`/`assets.controller.ts`. | `nx build api` (inside the Docker build) compiled clean; the 4 routes resolved at bare `/api/...` as verified in §3/§4 of ETAPES-FAITES.md. |
| 3 | Turning on `ENABLE_FEATURE_SUBSCRIPTION` (required to exercise the OPTIONAL Usage Analytics processing at all) crashes the whole app at DI-instantiation time: `SubscriptionService`'s `new Stripe(apiKey)` throws synchronously when `STRIPE_SECRET_KEY` is empty (its Nest default). Pre-existing Ghostfolio behavior, only surfaced because this integration needed the feature flag on. | Set a dummy, well-formed `STRIPE_SECRET_KEY` in `case-studies/Ghostfolio/.env` — no real billing call is exercised by this integration. | Container reached `healthy`; `docker logs` shows no further Stripe exception on any later boot. |
| 4 | First real-time `report_processed_data()` call after a cold PRIAM stack start (services up ~2 min, still stabilizing their own Eureka self-registration) silently recorded nothing — `GET /api/DataSubjectId/{idRef}` presumably transient-failed and `reportProcessedData()` returns early on a non-OK response **without logging**, so nothing appeared in `docker logs ghostfolio` either. | No code fix needed — this is the exact class of transient warm-up flakiness the playbook's §8.9 already documents (JVM/Eureka stabilization after cold start), not a sequencing bug: `registerDataSubject()` was correctly `await`ed before `reportProcessedData()` throughout (§8.6). Re-tested identically once services had been up a few minutes: worked automatically, no manual replay. The affected subject was later corrected for free by this same integration's own `backfill-data-subjects.mts` run (idempotent replay, see ETAPES-FAITES.md). | Manually replayed the exact same two HTTP calls from inside the `ghostfolio` container immediately after the miss → succeeded (`priam-data-ms` returned `200`, MySQL `processed_data` row inserted). A **third**, fully automatic sign-up minutes later (no manual intervention) reported all 8 `User`+`Account` data_ids correctly on the first try. |
| 5 | `user-account-settings.component.ts` declared `protected priamFrontendUrl: string;` but assigned it from `InfoItem.priamFrontendUrl`, itself `string \| undefined` (optional — undefined when PRIAM isn't wired up) → `TS2322: Type 'string \| undefined' is not assignable to type 'string'`, failing the Angular client's own build (`nx build client`). This did not surface until a genuine `--no-cache` Docker build was forced: an earlier, regular-cache rebuild had silently reused a stale `RUN npm run build:production` image layer from before this bug was introduced, exit 0, serving a client bundle that simply predated the "Manage on PRIAM"/redirect changes entirely rather than reflecting a broken one — a caching trap worth flagging for any future session on this same checkout: **a `0`-exit Docker build after touching Angular source is not proof the client actually rebuilt** on this environment; verify a distinctive string from the new UI is present in the served bundle. | Typed the field `string \| undefined`, matching the source. | `nx build client` compiled clean on the following `--no-cache` rebuild; `grep -rl 'Manage on PRIAM' /ghostfolio/apps/client/en/*.js` inside the running container found the string (see ETAPES-FAITES.md §9 for the exact command/output). |

## 3. Workflows verified against real state

Full request/response/DB detail for every row below is in
[`ETAPES-FAITES.md`](./ETAPES-FAITES.md).

| Workflow | Path | Real state checked | Result |
|---|---|---|---|
| Registration (sign-up) | `POST /api/v1/user` → `PriamService.onUserRegistered` | MySQL `priam-actor.data_subject`, `priam-data.processed_data` (User+Account ids) | Real UUID `idRef`, 8 rows bookkept, confirmed by direct `SELECT` |
| `report_processed_data` at later creation | `POST /api/v1/account`, `POST /api/v1/activities` | `processed_data.nb_occurrences` before/after | Incremented exactly once per creation (Account 1→2, Order 0→1) |
| Rectification — refused | `/right/api/right/rectificationRequest` + `/answer {answer:false}` | Postgres `"User".thirdPartyId` | Unchanged (`REFUSED` recorded, no Provider call) |
| Rectification — approved | same, `{answer:true}` | Postgres `"User".thirdPartyId` | Changed to the submitted `newValue` (`FULL`) |
| Erasure — refused | `/right/api/right/erasureRequest` + `/answer {answer:false}` | Postgres `"Order".comment` | Unchanged |
| Erasure — approved | same, `{answer:true}`, with real `primaryKeys` (`{id: <Order.id>}`) | Postgres `"Order".comment` | Erased to `''` (`FULL`) — proves the one-to-many/`primaryKeys` path (§8.1.c) works with a real record |
| Access request | `/right/api/right/accessRequest` + `/answer` + `GET /right/api/personalDataValues/accessRight` | Response values | Reflects the *live* Postgres state (including the just-approved rectification), not a stale cache |
| Consent — grant | `POST /cdp/api/consent/create/{idRef}` | `Analytics.activityCount`/`lastRequestAt` before/after an authenticated call | Incremented (0→1) |
| Consent — withdraw | same endpoint (toggles) | 3 further authenticated calls | `activityCount` stayed frozen, `lastRequestAt` unchanged — proof of *absence*, not just no error |
| Consent — re-grant | same endpoint | 1 further authenticated call | Resumed incrementing (1→2) |
| `has_pending_consent_decision` flag | `GET /api/v1/user` → `priamConsentRequired` | Response field, before/after a decision | `true` for a never-decided subject → `false` immediately after any decision |
| Keycloak provisioning | Real sign-up → `provisionKeycloakUser` | `kcadm.sh get users` + Direct Grant login | User created with correct `idReference`, `firstName`/`lastName`/`email` (§4bis pitfalls avoided); login succeeded, token's `idReference` claim matched the Ghostfolio `User.id` |
| Backfill (idempotent replay) | `backfill-data-subjects.mts` run against the live stack | `data_subject` row count before/after; `processed_data.nb_occurrences` | 5 users in, 5 `data_subject` rows after (no duplicates — upsert-by-idRef confirmed idempotent); `nb_occurrences` incremented on the already-registered seed subject, as documented |
| Provider bridge, all 4 endpoints | Direct `curl` sanity check (not the official test method, done *in addition to* the §3 workflow above) | `dataAccessRight` (3 types), `dataValue` (2 fields) | Correct JSON array shape, correct live values |

**Browser testing status**: no browser-automation tool is available to this
agent, so the UI was not clicked through by the agent itself. `PRIAM-Frontend`
was built and started (`localhost:4200`, `TARGET_APP_URL` baked in) and two
real anonymous sign-ups were performed by the user against a real browser
(ETAPES-FAITES.md §10) — both correctly registered server-side and
provisioned a matching Keycloak account. One genuine friction point surfaced
this way, not from any automated test: the user's first sign-up landed on a
Keycloak login with no visible username, because it happened moments before
this agent had actually confirmed the rebuilt Ghostfolio image (containing
the "PRIAM Login" display field, bug #5) was the one running — an
operational/communication mistake on the agent's part, not a defect in
`provisionKeycloakUser()` itself, which succeeded identically both times
(confirmed via `kcadm.sh get users`). A full click-through of the Consent
page render and the "Manage on PRIAM" → Keycloak → "Back to the app" round
trip was in progress with the user at the time of writing, not yet confirmed
complete — see ETAPES-FAITES.md §10 for the up-to-date status.

## 4. LOC breakdown

**Method**: `git diff --numstat` for the per-file +/- totals (all changed
files are tracked, except both `.env` files, which are gitignored in this
repo by design — those were diffed manually against a pre-edit snapshot
captured earlier in this session). The code/comment/blank split was done by
a small line-classifier script (`sed`/`while read`, no third-party tool):
blank = empty after trim; comment = starts with `--` (SQL), `//`/`/*` (TS),
or `#` (YAML/.env/.gitattributes); everything else = code. Applied to the
**added** side of each diff (and to the full content of brand-new files).
Removed-line counts are reported per file/category as a single aggregate
(not further split by nature). One file mixing two categories
(`priam.service.ts`: Consent functions + `provisionKeycloakUser`;
`app.module.ts`: two unrelated module registrations) was split at the exact
function/line boundary, not lumped into one bucket. Two lines of pre-existing,
non-PRIAM `.env` bootstrap (Postgres/Redis/JWT secrets, 16 lines, required
just to run vanilla Ghostfolio) were excluded from the Ghostfolio `.env`
count as out of scope for a PRIAM-integration LOC count.

### Per file

| File | Status | +Lines | -Lines |
|---|---|---|---|
| `Databases/db_insertion_script.sql` | modified | 168 | 139 |
| `docker-compose.yml` (PRIAM root) | modified | 1 | 1 |
| `.env` (PRIAM root, gitignored) | modified | 17 | 18 |
| `case-studies/Ghostfolio/.env` (new, gitignored; PRIAM-attributable portion only) | new | 32 | 0 |
| `case-studies/Ghostfolio/.gitattributes` | new | 1 | 0 |
| `case-studies/Ghostfolio/docker/entrypoint.sh` | modified (line-ending only) | 0 | 0 |
| `case-studies/Ghostfolio/docker/docker-compose.yml` | modified | 15 | 0 |
| `case-studies/Ghostfolio/docker/docker-compose.build.yml` | modified | 8 | 0 |
| `case-studies/Ghostfolio/apps/api/src/services/priam/priam.service.ts` | new | 234 | 0 |
| `case-studies/Ghostfolio/apps/api/src/services/priam/priam.module.ts` | new | 13 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/provider-bridge/provider-bridge.controller.ts` | new | 321 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/provider-bridge/provider-bridge.module.ts` | new | 11 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/app.module.ts` | modified | 4 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/user/user.service.ts` | modified | 26 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/account/account.service.ts` | modified | 6 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/activities/activities.service.ts` | modified | 7 | 0 |
| `case-studies/Ghostfolio/apps/api/src/app/auth/jwt.strategy.ts` | modified | 33 | 9 |
| `case-studies/Ghostfolio/apps/api/src/app/auth/api-key.strategy.ts` | modified | 24 | 8 |
| `case-studies/Ghostfolio/apps/api/src/app/info/info.service.ts` | modified | 3 | 1 |
| `case-studies/Ghostfolio/libs/common/src/lib/interfaces/user.interface.ts` | modified | 2 | 0 |
| `case-studies/Ghostfolio/libs/common/src/lib/interfaces/info-item.interface.ts` | modified | 3 | 0 |
| `case-studies/Ghostfolio/apps/client/src/app/app.component.ts` | modified | 12 | 0 |
| `case-studies/Ghostfolio/apps/client/src/app/components/user-account-settings/user-account-settings.component.ts` | modified | 5 | 1 |
| `case-studies/Ghostfolio/apps/client/src/app/components/user-account-settings/user-account-settings.html` | modified | 24 | 0 |
| `case-studies/Ghostfolio/priam-integration/backfill-data-subjects.mts` | new | 84 | 0 |
| **Total** | | **1041** | **177** |

### By functional category × line nature (added lines)

| Category | +Lines | -Lines | Code | Comment | Blank |
|---|---|---|---|---|---|
| Annotation (SQL script) | 168 | 139 | 60 | 107 | 1 |
| Rights-API (Provider bridge + §3 wiring) | 334 | 0 | 258 | 20 | 56 |
| Consent (CEP, registration, `report_processed_data`, backfill, navigation) | 402 | 26 | 277 | 81 | 44 |
| OAuth2 (Keycloak provisioning + auth `.env`/compose wiring) | 90 | 5 | 65 | 19 | 6 |
| Docker-network (remaining `docker-compose.yml`/`.env` wiring) | 47 | 7 | 18 | 25 | 4 |
| **Total** | **1041** | **177** | **678** | **252** | **111** |

**Notes on categorization**: `TARGET_APP_URL`/`PRIAM_FRONTEND_URL` wiring
was counted under **Consent** (the playbook explicitly groups "bidirectional
app↔PRIAM navigation" there), not Docker-network. `CUSTOM_PROVIDER_URL`
wiring was counted under **Docker-network** (transport/network plumbing),
not Rights-API (reserved for the endpoint implementation code itself).
`app.module.ts`'s 4 added lines split 2/2 between Rights-API
(`ProviderBridgeModule`) and Consent (`PriamModule`).

### PRIAM itself, this session

**0 lines changed.** No PRIAM microservice or frontend source file was
modified — every fix above lives in Ghostfolio's own code, this checkout's
Docker/`.env` wiring, or a Windows-environment artifact
(`entrypoint.sh`/`.gitattributes`). No new generic PRIAM bug was discovered
through a real test this session, so `Docs/PRIAM-INTEGRATION-PLAYBOOK.md`
§8 was not extended.
