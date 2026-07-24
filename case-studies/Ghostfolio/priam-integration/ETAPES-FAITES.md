# ETAPES-FAITES — PRIAM × Ghostfolio, raw test log

Every test below was actually run against a live Docker stack on this
machine (not simulated). Real database state was checked directly (MySQL
`priam-*` schemas, Postgres `ghostfolio-db`) after every step, per
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §7. See `INTEGRATION-REPORT.md` for the
summary/mechanism/bug list; this file is the detail.

## 0. Reference — URLs/ports used in this integration

| Component | Address | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | `/right/**`, `/cdp/**` require a Keycloak bearer token (`CUSTOM_OIDC_ISSUER_URI` is set); `/data/**`, `/actor/**`, `/provider/**` stay open |
| — Rights (via Gateway) | `http://localhost:8090/right/api/right/...` | `accessRequest`, `rectificationRequest`, `erasureRequest`, `answer`, `dataRequest/{id}`, `answer/{id}` |
| — Access read (via Gateway) | `http://localhost:8090/right/api/personalDataValues/accessRight` | Always-open read, not the auto-execution path |
| — Consent (via Gateway) | `http://localhost:8090/cdp/api/consent/create/{idRef}`, `.../api/decision/{processing}`, `.../api/contract/list/consents/{idRef}/{processing}` | |
| PRIAM Actor (direct, M2M) | `http://localhost:8082` (`actor:8082` in-network) | `POST /api/DataSubject`, `GET /api/DataSubjectId/{idRef}` |
| PRIAM Data (direct, M2M) | `http://localhost:8081` (`data:8081` in-network) | `POST /api/processed-data/add?subjectId=...` |
| Ghostfolio Provider bridge | `http://localhost:3333/api/{dataAccessRight,rectification,erasure,dataValue}` | Bare `/api`, no `/v1/`, no auth — this integration's own code |
| Ghostfolio app (API+client, same process) | `http://localhost:3333` | `POST /api/v1/user` (sign-up), `GET /api/v1/user` (current user), `/api/v1/account`, `/api/v1/activities` |
| PRIAM-Frontend | `http://localhost:4200` | Not exercised from a browser this session (no browser tool available - see INTEGRATION-REPORT.md §3) |
| PRIAM-Frontend-Provider | `http://localhost:4000` | Not exercised this session |
| Keycloak | `http://localhost:8080`, realm `priam-realm` | `admin`/`admin` bootstrap; `Data-client` used for password-grant tests below |
| MySQL (PRIAM) | `docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr'` | Root password from `Databases/Dockerfile`; app user is `DB_USERNAME`/`DB_PASSWORD` from root `.env` |
| Postgres (Ghostfolio) | `docker exec gf-postgres-build psql -U user -d ghostfolio-db` | |

## 1. Environment setup

1. **Windows CRLF fix**: `case-studies/Ghostfolio/docker/entrypoint.sh` was
   checked out with CRLF line endings, breaking its `#!/bin/sh` shebang
   inside the container (`exec: /ghostfolio/entrypoint.sh: not found`,
   `docker logs ghostfolio`, exit 127 in a loop). Fixed with `sed -i
   's/\r$//'` and locked in with a new `case-studies/Ghostfolio/.gitattributes`
   (`*.sh text eol=lf`).
2. **`case-studies/Ghostfolio/.env` created** (gitignored, did not exist):
   base secrets (`ACCESS_TOKEN_SALT`, `JWT_SECRET_KEY`,
   `POSTGRES_PASSWORD`, `REDIS_PASSWORD`, generated via `openssl rand -hex
   24`) plus the PRIAM wiring block (`PRIAM_CDP_URL`, `PRIAM_ACTOR_URL`,
   `PRIAM_DATA_URL`, `PRIAM_FRONTEND_URL`, `KEYCLOAK_ADMIN_URL`,
   `KEYCLOAK_REALM`, `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD`,
   `ENABLE_FEATURE_SUBSCRIPTION=true`, `STRIPE_SECRET_KEY=sk_test_...`
   dummy — see INTEGRATION-REPORT.md bug #3).
3. **`db-volume/` reset**: the bind-mounted MySQL data dir held
   OnlineBoutique's data from the previous case-study session (not a named
   volume scoped by Compose project — this bind mount is shared across
   whichever case study is currently active). Moved aside to
   `db-volume-onlineboutique-backup2/`, replaced with an empty `db-volume/`
   for a virgin init.
4. **Project identity**: root `docker-compose.yml`'s `name:` changed from
   `priam-onlineboutique` to `priam-ghostfolio` (playbook §5). **Collision
   check performed**: `docker ps -a` showed no currently-running container
   from a *different* PRIAM checkout — every PRIAM microservice has a fixed
   `container_name:` regardless of the project `name:`, so the only real
   collision risk is the auto-tagged `mysqldb`/`frontuser` images (no fixed
   `image:`, by design — playbook §5). Pre-existing
   `priam-ghostfolio-mysqldb`/`priam-ghostfolio-frontuser` image tags were
   found (residue from an earlier, code-less session — no
   `case-studies/Ghostfolio/priam-integration/` existed in git before this
   session), confirming the naming choice was already the intended one; they
   were overwritten by this session's own build, which is the expected
   behavior when switching back to a previously-used case study on the same
   checkout.
   ```
   docker compose -p priam-onlineboutique down   # clean teardown of the previous project's containers/networks
   ```
5. **Stack brought up sequentially** (playbook §8.9 — one service at a
   time, health-checked before the next):
   ```
   docker compose build mysqldb && docker compose up -d mysqldb eureka
   docker compose build actor    && docker compose up -d actor
   docker compose build consent data right provider
   docker compose up -d consent && docker compose up -d data && docker compose up -d right provider
   docker compose build gateway  && docker compose up -d gateway   # built from local source, not a remote image
   docker compose up -d keycloak
   ```
   All reached `healthy` (verified via `docker inspect --format
   '{{.State.Health.Status}}'` polling loops, no manual container edits).
6. **Ghostfolio app built from local source** (`docker/docker-compose.build.yml`,
   `build: ../`), attached to `common_network` in addition to its own
   default network (`networks:` added to both `docker/docker-compose.yml`
   and `docker/docker-compose.build.yml` — `extends:` only merges
   service-level fields, so the top-level `networks:`/`external: true`
   declaration had to be repeated in the build-variant file too, same
   reason `volumes:` is already repeated there).

## 2. Seed account capture (playbook §1 point 8)

```
curl -s -X POST http://localhost:3333/api/v1/user -H "Content-Type: application/json"
```
Response:
```json
{"accessToken":"2a92685acaacc51d72a65d0caf20ff56a6d2e4392796a2e2e710b76ea7c3aa4554b9f72a2cdfeff5098e5073252621615d6d4c19008c68fa9e4b9a7423b38374","role":"ADMIN","authToken":"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpZCI6ImI0ZjY0YTZjLTg2ODEtNDQ0NC1iZTMxLTdhMWViYzkzYmI5NyIsImlhdCI6MTc4NDg4ODE1NSwiZXhwIjoxNzg3NDgwMTU1fQ.FGaEC0G5V1hg-r0iJImEvDb9QYWePwgkPKU91PgHCbo"}
```
`authToken`'s JWT payload decodes (base64) to
`{"id":"b4f64a6c-8681-4444-be31-7a1ebc93bb97", ...}` — this is the real,
observed, non-numeric `idRef` seeded in `Databases/db_insertion_script.sql`
(this call was made *before* PRIAM was up, to get a clean id without any
PRIAM side effects racing the seed).

Verified directly in Postgres:
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT id, provider, \"thirdPartyId\", \"createdAt\" FROM \"User\" WHERE id='b4f64a6c-8681-4444-be31-7a1ebc93bb97';"
```
```
                  id                  | provider  | thirdPartyId |        createdAt
--------------------------------------+-----------+--------------+-------------------------
 b4f64a6c-8681-4444-be31-7a1ebc93bb97 | ANONYMOUS |              | 2026-07-24 10:15:55.882
```
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT id, \"userId\", name, currency, balance FROM \"Account\" WHERE \"userId\"='b4f64a6c-8681-4444-be31-7a1ebc93bb97';"
```
```
                  id                  |                userId                |    name    | currency | balance
--------------------------------------+--------------------------------------+------------+----------+---------
 2682684d-1c71-4fc3-a5f9-499efec2b8b9 | b4f64a6c-8681-4444-be31-7a1ebc93bb97 | My Account | USD      |       0
```
(The default `Account` created transactionally by `UserService.createUser()`
— this is the "processed data reported right at sign-up" case §4bis/§8.6
warns about.)

`db_insertion_script.sql` seeded with this real `idRef` and rebuilt
(`docker compose build mysqldb`, virgin `db-volume/`). After stack restart,
confirmed loaded:
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT * FROM \`priam-actor\`.data_subject;"
```
```
data_subject_id  age  id_ref                                data_subject_category_id
1                16   b4f64a6c-8681-4444-be31-7a1ebc93bb97   1
```
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT * FROM \`priam-data\`.processed_data;"
```
8 rows (`data_id` 1-8, `data_subject_id` 1, `nb_occurrences` 1 each) — the
`User`(1-4) + `Account`(5-8) bookkeeping matching `USER_DATA_IDS`/
`ACCOUNT_DATA_IDS` in `priam.service.ts`.

## 3. Registration hook — live, unassisted sign-ups (§4bis)

Once the full PRIAM stack (including `gateway`) was healthy, three more
anonymous sign-ups were performed with **no manual PRIAM calls** — purely
through `POST /api/v1/user`, to prove `PriamService.onUserRegistered()`
fires correctly on its own:

| # | idRef (JWT `id`) | `registerDataSubject`/`reportProcessedData` result |
|---|---|---|
| 2 | `4f0fbabe-4bbb-42e0-9891-8ca82a22677f` | `data_subject` row created; `processed_data` **initially empty** — see INTEGRATION-REPORT.md bug #4 (transient cold-start miss, no error logged). Manually verified the underlying calls work (`docker exec ghostfolio node -e "fetch(...)"` replay — `GET /api/DataSubjectId/{idRef}` → `200 2`; `POST /api/processed-data/add?subjectId=2` → `200 "Processed data added successfully."`), then confirmed fully corrected for free by the backfill run in §7. |
| 3 | `5cfcdf08-930d-44f6-b043-e6ac163864c7` | Fully automatic, first try: all 8 `processed_data` rows present (`SELECT ds.id_ref, pd.data_id, pd.nb_occurrences FROM priam-actor.data_subject ds JOIN priam-data.processed_data pd ON pd.data_subject_id=ds.data_subject_id WHERE ds.id_ref='5cfcdf08-930d-44f6-b043-e6ac163864c7'` → 8 rows, `nb_occurrences=1` each). No warning in `docker logs ghostfolio` except the expected `provisionKeycloakUser failed` (Keycloak not started yet at this point). |
| 4 | `cae25c28-3741-4079-9893-61f8a071d620` | Same as #3, plus Keycloak now up — see §6 below for the provisioning proof. |
| 5 | `9154efd7-8aec-4742-a2b3-88c89acdb8d8` | Used for the `priamConsentRequired` flag test, §5.4 below. |

## 4. Rights workflow — real cycle through PRIAM-Right-service (§3)

All calls below go through the Gateway (`localhost:8090/right/...`), which
requires a bearer token (Gateway auth is on — `CUSTOM_OIDC_ISSUER_URI` set).
Token obtained from the pre-seeded Keycloak test account
`priam-ghostfolio-user@example.com` (found in `Keycloak/priam-realm-realm.json`,
pre-existing static test data):
```
TOKEN=$(curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=Data-client" \
  -d "username=priam-ghostfolio-user@example.com" -d "password=PriamTest123!" \
  | grep -o '"access_token":"[^"]*"' | cut -d'"' -f4)
```

Seed subject: `dataSubjectId=1`, `idRef=b4f64a6c-8681-4444-be31-7a1ebc93bb97`.

### 4.1 Rectification — `answer=false` then `answer=true` (User.thirdPartyId, single-row type)

**Request #1** (will be refused):
```
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "dataSubjectId": 1, "dataTypeName": "User", "data": {"dataId": 3},
  "newValue": "external-oauth-sub-REFUSED-TEST",
  "claim": "Please update my thirdPartyId", "primaryKeys": []
}'
```
→ `{"dataRequestId":1, ...}` (`200`).

**Answer, refused**:
```
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "requestAnswerId": 0, "answer": false, "providerClaim": "Refused for test",
  "dataRequestId": 1, "data": []
}'
```
→ `{"dataRequestAnswerId":1,"answer":"REFUSED", ...}` (`200`).

**Real DB check** — unchanged:
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT id, \"thirdPartyId\" FROM \"User\" WHERE id='b4f64a6c-8681-4444-be31-7a1ebc93bb97';"
```
```
thirdPartyId
(empty)
```

**Request #2** (will be approved):
```
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "dataSubjectId": 1, "dataTypeName": "User", "data": {"dataId": 3},
  "newValue": "external-oauth-sub-APPROVED-12345",
  "claim": "Please update my thirdPartyId", "primaryKeys": []
}'
```
→ `{"dataRequestId":2, ...}`.

**Answer, approved**:
```
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "requestAnswerId": 0, "answer": true, "providerClaim": "Approved",
  "dataRequestId": 2, "data": []
}'
```
→ `{"dataRequestAnswerId":2,"answer":"FULL", ...}`.

**Real DB check — genuinely changed**:
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT id, \"thirdPartyId\" FROM \"User\" WHERE id='b4f64a6c-8681-4444-be31-7a1ebc93bb97';"
```
```
thirdPartyId
external-oauth-sub-APPROVED-12345
```

### 4.2 Erasure — `answer=false` then `answer=true` (Order.comment, one-to-many with real `primaryKeys`)

A real `Order` was created first through the app's own authenticated API
(not a fixture) so the `primaryKeys` test uses a genuine record:
```
SEED_JWT=<the authToken from §2>
curl -s -X POST http://localhost:3333/api/v1/activities \
  -H "Content-Type: application/json" -H "Authorization: Bearer $SEED_JWT" -d '{
  "accountId":"2682684d-1c71-4fc3-a5f9-499efec2b8b9", "currency":"USD",
  "dataSource":"MANUAL", "date":"2026-07-20T00:00:00.000Z", "fee":1.5,
  "quantity":1, "symbol":"CASH", "type":"FEE", "unitPrice":10,
  "comment":"Test PRIAM order"
}'
```
→ created `Order.id = 7382d6de-911e-4464-95f5-35f607107df0`. Confirmed
`processed_data` for `data_id` 9-16 (Order) appeared with `nb_occurrences=1`
right after (real-time `report_processed_data`, §4bis — the "most
frequently forgotten point", verified NOT forgotten here).

**Erasure request #1** (refused):
```
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "dataSubjectId": 1, "dataTypeName": "Order", "data": {"dataId": 16},
  "claim": "Erase my comment on this order",
  "primaryKeys": [{"primaryKeyId": 9, "primaryKeyValue": "7382d6de-911e-4464-95f5-35f607107df0"}]
}'
```
→ `{"dataRequestId":3, "primaryKeys":{"9":"7382d6de-911e-4464-95f5-35f607107df0"}, ...}`
(confirms `PRIAM-Right-service` correctly resolved `primaryKeyId=9` →
`Order.id`, matching `is_primary_key=1` on that `data` row).

```
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "requestAnswerId": 0, "answer": false, "providerClaim": "refused",
  "dataRequestId": 3, "data": []
}'
```
→ `REFUSED`. DB check: `comment = "Test PRIAM order"` (unchanged).

**Erasure request #2** (approved, fresh request — PRIAM blocks answering the
same request twice, `409`):
```
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest ... # same body
```
→ `{"dataRequestId":4, ...}`.
```
curl -s -X POST http://localhost:8090/right/api/right/answer -d '{
  "requestAnswerId": 0, "answer": true, "providerClaim": "approved",
  "dataRequestId": 4, "data": []
}'
```
→ `{"dataRequestAnswerId":4,"answer":"FULL", ...}`.

**Real DB check — genuinely erased**:
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT id, comment FROM \"Order\" WHERE id='7382d6de-911e-4464-95f5-35f607107df0';"
```
```
comment
(empty)
```

### 4.3 Access request — approve, then read via the always-open endpoint

```
curl -s -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{
  "dataSubjectId": 1, "dataRequestClaim": "I want to see my User data",
  "data": [{"dataId":1},{"dataId":2},{"dataId":3},{"dataId":4}]
}'
```
→ `{"dataRequestId":5, "datas":[...4 items...], ...}`.
```
curl -s -X POST http://localhost:8090/right/api/right/answer -d '{
  "requestAnswerId": 0, "answer": true,
  "providerClaim": "approved, showing all fields", "dataRequestId": 5,
  "data": [{"dataId":1},{"dataId":2},{"dataId":3},{"dataId":4}]
}'
```
→ `FULL`.
```
curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  -H "Authorization: Bearer $TOKEN" --data-urlencode "dataSubjectId=1" \
  --data-urlencode "dataTypeName=User" --data-urlencode "attributes=id" \
  --data-urlencode "attributes=provider" --data-urlencode "attributes=thirdPartyId" \
  --data-urlencode "attributes=createdAt"
```
→
```json
[{"id":"b4f64a6c-8681-4444-be31-7a1ebc93bb97","provider":"ANONYMOUS","thirdPartyId":"external-oauth-sub-APPROVED-12345","createdAt":"Fri Jul 24 2026 10:15:55 GMT+0000 (Coordinated Universal Time)"}]
```
`thirdPartyId` reflects the value set by §4.1's approved rectification —
proof this read hits the live Provider bridge/Postgres, not a stale value.

### 4.4 Provider bridge, direct sanity checks (all 4 endpoints — in addition to, not instead of, §4.1-4.3)

```
curl -s "http://localhost:3333/api/dataAccessRight?idRef=b4f64a6c-8681-4444-be31-7a1ebc93bb97&dataTypeName=Order&attributes=id,type,currency,quantity,unitPrice,fee,date,comment"
```
→ `[{"id":"7382d6de-...","type":"FEE","currency":"USD","quantity":"1","unitPrice":"10","fee":"1.5","date":"...","comment":"Test PRIAM order"}]` (a JSON array, per §2).
```
curl -s "http://localhost:3333/api/dataAccessRight?idRef=b4f64a6c-8681-4444-be31-7a1ebc93bb97&dataTypeName=Account&attributes=id,name,currency,balance"
```
→ 2 elements (the default Account + the one created in §5.x below).
```
curl -s -X POST http://localhost:3333/api/dataValue -H "Content-Type: application/json" \
  -d '{"idRef":"b4f64a6c-8681-4444-be31-7a1ebc93bb97","dataName":"thirdPartyId"}'
```
→ `{"value":"external-oauth-sub-APPROVED-12345"}` (§8.2.f's 4th endpoint, no `dataTypeName` in the request body).
```
curl -s -X POST http://localhost:3333/api/dataValue -H "Content-Type: application/json" \
  -d '{"idRef":"b4f64a6c-8681-4444-be31-7a1ebc93bb97","dataName":"comment","primaryKeys":{"id":"7382d6de-911e-4464-95f5-35f607107df0"}}'
```
→ `{"value":""}` (matches the erasure result from §4.2).

## 5. Registration bookkeeping beyond sign-up (§4bis, "most frequently forgotten point")

A **second** `Account` was created for the seed subject through the real
authenticated API:
```
curl -s -X POST http://localhost:3333/api/v1/account -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SEED_JWT" \
  -d '{"balance":1000,"currency":"USD","name":"Second Account","platformId":null}'
```
→ `Account.id = 53ab8228-bd70-4053-9e07-50bd274bbff6`.

**Before/after `processed_data.nb_occurrences`** for `data_subject_id=1`:
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT data_id, nb_occurrences FROM \`priam-data\`.processed_data WHERE data_subject_id=1 ORDER BY data_id;"
```
| data_id | before | after (2nd Account) | after (Order, §4.2) |
|---|---|---|---|
| 1-4 (User) | 1 | 1 | 1 |
| 5-8 (Account) | 1 | **2** | 2 |
| 9-16 (Order) | (absent) | (absent) | **1** |

Confirms `reportProcessedData()` fires — and increments, per
`ProcessedDataService`'s `nb_occurrences` design — at every later personal
record creation, not just at sign-up.

## 6. Consent workflow — grant / withdraw / re-grant (§4), Usage Analytics

Baseline: the seed subject's `Analytics` row already existed (`activityCount:
0`) — created unconditionally by `UserService.createUser()`'s own
pre-existing logic when `ENABLE_FEATURE_SUBSCRIPTION` is on, independent of
consent (a Ghostfolio behavior predating this integration). The
**consent-gated** behavior this integration adds is whether that row's
values ever get *updated* — verified below.

### 6.1 Grant
```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/b4f64a6c-8681-4444-be31-7a1ebc93bb97" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"processingId":"Usage Analytics"}'
```
→ `{"consentId":1,"startDate":"2026-07-24T10:41:02.081+00:00","endDate":null,"contractId":1}`.

Authenticated call as the seed user:
```
curl -s http://localhost:3333/api/v1/user -H "Authorization: Bearer $SEED_JWT" -H "X-Timezone: Europe/Paris"
```
**DB check**:
```
docker exec gf-postgres-build psql -U user -d ghostfolio-db -c "SELECT \"activityCount\", \"lastRequestAt\" FROM \"Analytics\" WHERE \"userId\"='b4f64a6c-8681-4444-be31-7a1ebc93bb97';"
```
```
activityCount | lastRequestAt
1             | 2026-07-24 10:41:24.61
```
`processed_data` for `data_id` 17-19 (Analytics) now present,
`nb_occurrences=1`.

### 6.2 Withdraw (same endpoint toggles the existing consent)
```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/b4f64a6c-8681-4444-be31-7a1ebc93bb97" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"processingId":"Usage Analytics"}'
```
→ `{"consentId":1,"startDate":"...10:41:02...","endDate":"2026-07-24T10:41:49.309+00:00", ...}`.
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT * FROM \`priam-consent\`.consent;"
```
```
consent_id  start_date           end_date              processing_id  contract_id
1           2026-07-24 10:41:02  2026-07-24 10:41:49   3              1
```

**Proof of absence** (§7 — not just "no error"): 3 further authenticated
calls as the seed user:
```
for i in 1 2 3; do curl -s http://localhost:3333/api/v1/user -H "Authorization: Bearer $SEED_JWT" > /dev/null; done
```
**DB check**:
```
activityCount | lastRequestAt
1             | 2026-07-24 10:41:24.61      <- unchanged from §6.1
```

### 6.3 Re-grant
```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/b4f64a6c-8681-4444-be31-7a1ebc93bb97" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"processingId":"Usage Analytics"}'
```
→ `{"consentId":2,"startDate":"2026-07-24T10:42:21.590+00:00","endDate":null,"contractId":1}` (a fresh row).
```
curl -s http://localhost:3333/api/v1/user -H "Authorization: Bearer $SEED_JWT"
```
**DB check**:
```
activityCount | lastRequestAt
2             | 2026-07-24 10:42:22.298     <- resumed incrementing
```

### 6.4 `has_pending_consent_decision` flag (§4bis flag insertion point)

A **fresh** subject (never decided) via a new sign-up:
```
RESP=$(curl -s -X POST http://localhost:3333/api/v1/user -H "Content-Type: application/json")
# idRef = 9154efd7-8aec-4742-a2b3-88c89acdb8d8 (decoded from the JWT)
AUTH_TOKEN=<authToken from $RESP>
curl -s http://localhost:3333/api/v1/user -H "Authorization: Bearer $AUTH_TOKEN" | grep -o '"priamConsentRequired":[a-z]*'
```
→ `"priamConsentRequired":true`.

Grant consent for this subject, then re-check:
```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/9154efd7-8aec-4742-a2b3-88c89acdb8d8" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"processingId":"Usage Analytics"}'
curl -s http://localhost:3333/api/v1/user -H "Authorization: Bearer $AUTH_TOKEN" | grep -o '"priamConsentRequired":[a-z]*'
```
→ `"priamConsentRequired":false`. Confirms the flag flips exactly once a
decision exists (redirect fires at most once by construction, §4bis).

## 7. Keycloak provisioning — real login, real claim

Fresh sign-up (`idRef = cae25c28-3741-4079-9893-61f8a071d620`) after
`keycloak` container was up:
```
curl -s -X POST http://localhost:3333/api/v1/user -H "Content-Type: application/json"
```
`docker logs ghostfolio` around that timestamp: **no** `provisionKeycloakUser
failed` warning (unlike subjects #2/#3 above, provisioned before Keycloak
was up).

**Keycloak side** (`kcadm.sh`, `MSYS_NO_PATHCONV=1` needed on this Git-Bash/Windows host to stop path-mangling `/opt/...`):
```
docker exec priam-keycloak /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 --realm master --user admin --password admin
docker exec priam-keycloak /opt/keycloak/bin/kcadm.sh get users -r priam-realm \
  -q 'username=cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local'
```
```json
[{
  "id": "3e1c541b-c52a-436f-b811-3cb082d28717",
  "username": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local",
  "firstName": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local",
  "lastName": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local",
  "email": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local",
  "emailVerified": true,
  "attributes": {"idReference": ["cae25c28-3741-4079-9893-61f8a071d620"]},
  "enabled": true, "requiredActions": []
}]
```
`requiredActions: []` (empty) — confirms `firstName`/`lastName`/`email`
were all supplied at creation, avoiding the "Account is not fully set up"
pitfall the playbook documents for missing required User Profile
attributes.

**Real login, Direct Grant flow**, using the Ghostfolio `accessToken` from
the sign-up response as the Keycloak password:
```
curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" -d "client_id=Data-client" \
  -d "username=cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local" \
  -d "password=<the sign-up response's accessToken>"
```
→ `200`, a real access/refresh token pair. Decoding the `access_token`
JWT's payload:
```json
{"...": "...", "idReference": "cae25c28-3741-4079-9893-61f8a071d620", "preferred_username": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local", "email": "cae25c28-3741-4079-9893-61f8a071d620@ghostfolio.local"}
```
`idReference` claim matches the Ghostfolio `User.id` exactly — proof the
provisioned Keycloak identity is genuinely linked to the real account, not
a disconnected test identity (the exact failure mode §4bis/§8.8 warn about).

(Realm User Profile note: `idReference` was already declared in
`Keycloak/priam-realm-realm.json`'s `kc.user.profile.config` before this
session — the "Admin API silently drops undeclared custom attributes"
pitbook pitfall was already fixed generically on PRIAM's side; nothing to
do here.)

## 8. Backfill script (§4bis, last point)

Run against the live stack from inside the `ghostfolio` container (host had
no `node`/`npx` on `PATH` in this Git-Bash environment; the script needs
`@prisma/client`/`@prisma/adapter-pg`, resolved from the app's own
`node_modules`):
```
docker cp case-studies/Ghostfolio/priam-integration/backfill-data-subjects.mts ghostfolio:/ghostfolio/apps/api/backfill-data-subjects.mts
docker exec -w /ghostfolio/apps/api \
  -e PRIAM_ACTOR_URL=http://actor:8082 -e PRIAM_DATA_URL=http://data:8081 \
  ghostfolio node --experimental-strip-types /ghostfolio/apps/api/backfill-data-subjects.mts
```
Output:
```
Backfilling 5 existing user(s)...
  - 4f0fbabe-4bbb-42e0-9891-8ca82a22677f: registered, reported User + Account data
  - 5cfcdf08-930d-44f6-b043-e6ac163864c7: registered, reported User + Account data
  - cae25c28-3741-4079-9893-61f8a071d620: registered, reported User + Account data
  - b4f64a6c-8681-4444-be31-7a1ebc93bb97: registered, reported User + Account data
  - 9154efd7-8aec-4742-a2b3-88c89acdb8d8: registered, reported User + Account data
Backfill complete.
```
**Idempotency check** — no duplicate `data_subject` rows despite replaying
`registerDataSubject` for all 5 (including 4 already registered live):
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT COUNT(*) FROM \`priam-actor\`.data_subject;"
```
→ `5` (exactly one row per real user, confirming `POST /api/DataSubject`'s
upsert-by-idRef is genuinely idempotent, as the playbook documents).
`processed_data.nb_occurrences` for the seed subject's User/Account
`data_id`s incremented again on replay (1→2, 2→3) — expected, documented
bookkeeping behavior, not a bug. This same replay is also what completed
the bookkeeping for subject #2 (`4f0fbabe-...`), which had silently missed
its live report at cold-start (INTEGRATION-REPORT.md bug #4).

Test copy removed from the container afterward
(`docker exec ghostfolio rm -f /ghostfolio/apps/api/backfill-data-subjects.mts`)
— the file in the repo is unaffected, this was only a runtime smoke test.

## 9. Client build verification (no browser tool available — see limitation below)

`docker/docker-compose.build.yml` builds `case-studies/Ghostfolio` from
local source (`npm run build:production`, an Nx build producing both the
API and the Angular client). The image built and the container reached
`healthy` — `nx build` fails the whole `docker build` on any TypeScript
error in either project (confirmed twice during this session: a decorator
type error and two missing-symbol errors in the *backend* both failed the
build outright, per INTEGRATION-REPORT.md bugs table), so a successful
build is real proof the **client** compiled too (`app.component.ts`,
`user-account-settings.component.ts`/`.html`, the two shared interface
files).

```
docker exec ghostfolio sh -c "find /ghostfolio/apps/client/en -iname '*.js' | xargs grep -l 'Manage on PRIAM'"
```
→ `/ghostfolio/apps/client/en/chunk-IRMEFY3T.js` (present, `ls -la` timestamp
`Jul 24 11:1x`, i.e. from the corrected build, not a stale one).

`PRIAM-Frontend` was then also built and started (`docker compose build
frontuser && docker compose up -d frontuser`, root `docker-compose.yml`,
`TARGET_APP_URL=http://localhost:3333/` baked in at build time per §4ter) —
reachable at `http://localhost:4200`.

## 10. Real browser session (performed by the user, not this agent — no
browser-automation tool was available; see limitation below)

Two real anonymous sign-ups were performed by the user through an actual
browser against `http://localhost:3333`, confirmed via the running
containers' own state (not simulated):

**Sign-up #1** (`~11:10:30 UTC`) — happened *before* the corrected Ghostfolio
client image had been restarted (see INTEGRATION-REPORT.md bug #5): the
served bundle at that moment predated the "Manage on PRIAM"/"PRIAM Login"
UI, so the user had no way to discover the synthesized Keycloak username in
the browser. `PriamService.onUserRegistered()` still ran correctly
server-side regardless (registration is independent of what the client
happens to render) — confirmed via:
```
docker exec priam-keycloak /opt/keycloak/bin/kcadm.sh get users -r priam-realm --limit 50 | grep -A1 '"username"'
```
→ includes `47692922-ba7e-4f11-beb3-4d26493193d6@ghostfolio.local`,
`createdTimestamp: 1784891430285` (= `2026-07-24 11:10:30 UTC`).

**Sign-up #2** (`~11:27:57 UTC`) — after the container restart with the
fixed client:
→ `1e6a690b-f323-4650-afad-40a2749d3e4f@ghostfolio.local`,
`createdTimestamp: 1784892477973` (= `2026-07-24 11:27:57 UTC`). This
session's Account Settings page is expected to show the "PRIAM Login" field
correctly, since the corrected bundle was already live by this point.

**Real friction point identified, not a code bug**: this session's own
sequencing — telling the user to start testing before confirming the
rebuilt container had actually replaced the running one — produced a
genuinely confusing moment (a Keycloak login screen with no visible
username, for sign-up #1). The fix was operational (verify the deployed
image before inviting a user to test), not a change to
`provisionKeycloakUser()`, which behaved correctly both times.

**Limitation, still accurate**: no browser-automation tool was available to
*this agent* — the two sign-ups and the account/Keycloak state above were
driven by the user directly and cross-checked against real container state
by this agent afterward, not observed first-hand by this agent through a
browser. A full click-through of the Consent page render, the "Manage on
PRIAM" → Keycloak → "Back to the app" round trip, and CORS-from-a-real-origin
behavior is in progress with the user but not yet confirmed complete at the
time of writing.
