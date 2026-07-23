# PRIAM ↔ OnlineBoutique — Detailed test log (ÉTAPES FAITES)

Every test actually run this session, with the exact command, the HTTP
response, and the real backend state checked afterward (playbook §7 — a
`200`/`FULL` proves nothing by itself). Reproducible end-to-end by a human
or an AI with no prior context: run the commands in order, from the repo
root, with both stacks up (§0 below).

## 0. Reference: ports/URLs actually used

| Component | Address | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | `/right/**`, `/cdp/**`, `/actor/**`, `/data/**`, `/provider/**`, `/eureka/**` (path rewritten, prefix stripped) |
| PRIAM-Right-service | via Gateway `/right/api/right/...`, `/right/api/personalDataValues/...`, `/right/api/isAccepted` | direct container port 8083, not published needed for these tests |
| PRIAM-Consent-Service (CDP/CIP) | via Gateway `/cdp/api/decision/...`, `/cdp/api/contract/list/consents/...`, `/cdp/api/consent/create/...` | direct container port 8089 |
| PRIAM-Actor-service | via Gateway `/actor/api/DataSubject...` | direct container port 8082 |
| PRIAM-Data-service | via Gateway `/data/api/processed-data/...` | direct container port 8081 |
| OnlineBoutique frontend (Provider bridge host) | `http://localhost:8080` | bare `/api/{dataAccessRight,rectification,erasure,dataValue}`; also the real app (`/`, `/cart`, `/cart/checkout`, ...) |
| OnlineBoutique Redis (cart store) | `docker exec ob-redis-cart redis-cli ...` | not published to the host, internal `ob_internal` network only |
| MySQL (PRIAM DB) | `localhost:3308` (mapped from container port 3306) | `mysql -upriamu -p'MaiRP_pWd-UsEr' -h127.0.0.1 -P3308`, or `docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr'` |
| PRIAM-Frontend | `http://localhost:4200` | built/started; `/consent` is the forced-redirect target |
| PRIAM-Frontend-Provider | `http://localhost:4000` | built/started; renders the `dataValue` detail pages |
| Keycloak | not started this session | port 8080 collision with OnlineBoutique's own frontend; also out of scope (see INTEGRATION-REPORT.md §2) |

Seed data subject: `data_subject_id=1`, `id_ref='207acaaf-a999-4ede-9ca6-7e1eeaaedda5'`
(a real `shop_session-id` cookie value captured from a real running
container this session — non-numeric UUID, satisfying playbook §7's
non-numeric-idRef requirement for every test below).

## 1. Environment setup (one-time, this session)

```sh
# Docker Desktop started; confirmed a leftover Mastodon stack (case-studies/
# Mastodon, project "mastodon") was running from a prior session - stopped
# it (docker compose -p mastodon stop) to free resources, per playbook §8.9.

# Stale db-volume held Mastodon's initialized MySQL data - wiped (user
# confirmed) before switching case study, per playbook §5.
rm -rf db-volume && mkdir -p db-volume

# root docker-compose.yml: name: priam-mastodon -> name: priam-onlineboutique
# root .env: CUSTOM_PROVIDER_URL -> http://frontend:8080
#            CUSTOM_OIDC_ISSUER_URI / CUSTOM_OIDC_JWK_SET_URI -> blank (OAuth2 N/A)
#            TARGET_APP_URL -> http://localhost:8080/

docker compose build mysqldb
docker compose up -d --no-build mysqldb        # waits healthy
docker compose up -d --no-build eureka         # waits healthy
docker compose build actor consent data right provider gateway   # sequentially, not in one command (§8.9)
docker compose up -d --no-build actor
docker compose up -d --no-build consent data
docker compose up -d --no-build right provider
docker compose up -d --no-build gateway
curl -s http://localhost:8090/health   # returns a JPEG (GatewayApplication's placeholder healthcheck route) - confirms Gateway is up
```

```sh
cd case-studies/OnlineBoutique
docker compose up -d redis-cart
docker compose build productcatalogservice currencyservice shippingservice \
  emailservice paymentservice recommendationservice adservice cartservice \
  checkoutservice frontend   # sequentially, one at a time (§8.9)
# currencyservice and paymentservice each needed one retry (transient
# `npm install`/module-proxy network failures - see INTEGRATION-REPORT.md §3 bug 1)
docker compose up -d --no-build
```

All 10 containers (`ob-redis-cart`, `ob-productcatalogservice`,
`ob-currencyservice`, `ob-shippingservice`, `ob-emailservice`,
`ob-paymentservice`, `ob-recommendationservice`, `ob-adservice`,
`ob-cartservice`, `ob-checkoutservice`, `ob-frontend`) confirmed `Up` via
`docker ps`.

## 2. Seed session capture

```sh
curl -i -s http://localhost:8080/
```
Response (first request, no cookie):
```
HTTP/1.1 302 Found
Content-Type: text/html; charset=utf-8
Location: http://localhost:4200/consent
Set-Cookie: shop_session-id=207acaaf-a999-4ede-9ca6-7e1eeaaedda5; Max-Age=172800
```
The `302` to `/consent` is itself the first proof of the forced-consent
redirect (playbook §4bis): a brand-new, undecided subject is redirected
immediately.

A real product was added to this session's cart through the application's
own endpoint:
```sh
SEED=207acaaf-a999-4ede-9ca6-7e1eeaaedda5
curl -s -i -b "shop_session-id=$SEED" -X POST http://localhost:8080/cart \
  --data-urlencode "product_id=OLJCESPC7Z" --data-urlencode "quantity=2"
# -> HTTP/1.1 302 Found, Location: /cart
```

Real Redis proof (independent of any PRIAM/bridge code — raw `redis-cli`):
```sh
docker exec ob-redis-cart redis-cli TYPE 207acaaf-a999-4ede-9ca6-7e1eeaaedda5
# -> hash
docker exec ob-redis-cart redis-cli HGETALL 207acaaf-a999-4ede-9ca6-7e1eeaaedda5
# -> absexp / -1 / sldexp / -1 / data / <protobuf bytes containing "207acaaf-...OLJCESPC7Z">
```
This confirms `Microsoft.Extensions.Caching.StackExchangeRedis` stores the
cache entry as a Redis hash with fields `absexp`/`sldexp`/`data` — the
design assumption `priam_provider.go` is built on, verified against real
state before trusting it.

`Databases/db_insertion_script.sql`'s `__SEED_SESSION_ID__` placeholder was
then replaced with this exact value, and `mysqldb` rebuilt from a wiped
`db-volume/` (same commands as §1) so the seeded `data_subject`/`consent`/
`processed_data` rows reference this real idRef. `actor`, `consent`, `data`,
`right`, `provider` were restarted against the fresh database.

Verification after reinit:
```sh
curl -s http://localhost:8090/actor/api/DataSubjectId/207acaaf-a999-4ede-9ca6-7e1eeaaedda5
# -> 1
curl -s "http://localhost:8090/cdp/api/contract/list/consents/207acaaf-a999-4ede-9ca6-7e1eeaaedda5/Product%20Recommendations"
# -> [{"consentId":1,"startDate":"2026-07-23T13:37:25.000+00:00","endDate":null,"processing":null,"contractId":1}]
```
Confirms the seed subject resolves to `data_subject_id=1` with the
pre-granted OPTIONAL consent from the SQL script (§1 point 9 of the
playbook).

## 3. SQL annotation — real state check

```sh
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "
SELECT * FROM \`priam-actor\`.data_subject_category;
SELECT * FROM \`priam-data\`.data_type;
SELECT data_id,data_name,is_primary_key,is_personal,data_type_id,personal_data_category_id FROM \`priam-data\`.data;
SELECT processing_id,processing_name,processing_type,processing_category FROM \`priam-data\`.processing;
SELECT * FROM \`priam-data\`.data_usage;
SELECT * FROM \`priam-consent\`.contract;
SELECT * FROM \`priam-consent\`.consent;
SELECT * FROM \`priam-data\`.processed_data;"
```
Output (abridged, full output captured during the session):
```
data_subject_category_id  data_subject_category_name  location_id
1                          Shopper                      NULL

data_type_id  data_type_name
1             Cart

data_id  data_name    is_primary_key  is_personal  data_type_id  personal_data_category_id
1        product_id   1               1            1             7
2        quantity     0               1            1             7

processing_id  processing_name           processing_type  processing_category
1              Cart Management           NECESSARY        CONSENT_CONTRACT
2              Product Recommendations   OPTIONAL         CONSENT_CONTRACT

data_usage_id  personal_status  c  r  u  d  data_id  processing_id
1              1                1  1  1  1  1        1
2              1                1  1  1  1  2        1
3              1                0  1  0  0  1        2

contract_id  signature_date  expiration_date  data_subject_id
1            2026-07-23      NULL             1

consent_id  start_date            end_date  processing_id  contract_id
1           2026-07-23 12:37:24   NULL      2              1

data_id  data_subject_id  nb_occurrences
1        1                1
2        1                1
```
`processing_type`/`processing_category` are exact uppercase — no Hibernate
`IllegalArgumentException` on any subsequent call (§8.1.a checked).

## 4. Provider bridge smoke test (direct + through Gateway)

```sh
SEED=207acaaf-a999-4ede-9ca6-7e1eeaaedda5
curl -s "http://localhost:8080/api/dataAccessRight?idRef=$SEED&dataTypeName=Cart&attributes=product_id,quantity"
# -> [{"product_id":"OLJCESPC7Z","quantity":"2"}]
curl -s "http://localhost:8090/provider/api/dataAccessRight?idRef=$SEED&dataTypeName=Cart&attributes=product_id,quantity"
# -> [{"product_id":"OLJCESPC7Z","quantity":"2"}]   (identical, confirms Gateway's /provider/** rewrite + CUSTOM_PROVIDER_URL wiring)
```

## 5. Rights workflow — via PRIAM-Right-service (not direct Provider calls)

### 5.1 Access request

```sh
curl -s -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"I want to see my cart data","data":[{"dataId":1},{"dataId":2}]}'
# -> {"dataRequestId":1, ... "response":false, ...}
```

Answer `false`:
```sh
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":1,"answer":false,"providerClaim":"denied for test","data":[]}'
# -> {"dataRequestAnswerId":1,"answer":"REFUSED","dataRequestClaim":"denied for test"}
curl -s http://localhost:8090/right/api/right/answer/1
# -> {"dataRequestAnswerId":1,"answer":"REFUSED","dataRequestClaim":"denied for test"}
```

Always-open read (playbook §3, point 3 — independent of the answer):
```sh
curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=1&dataTypeName=Cart&attributes=product_id&attributes=quantity"
# -> [{"product_id":"OLJCESPC7Z","quantity":"2"}]   (unaffected by the REFUSED answer, as documented)
```

Re-answering the same request is blocked:
```sh
curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" -d '{"dataRequestId":1,"answer":true,"providerClaim":"retry","data":[]}'
# -> 409
```

A second access request, answered with the correct `data` array
(ACCESS-type answers are accepted by *which* `dataId`s are listed in `data`,
not by the `answer` boolean alone — `DataRequestServiceImpl.
saveRequestAnswer`, confirmed by reading the code after an
initial answer=true/data=[] call unexpectedly came back `REFUSED`):
```sh
curl -s -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"third access request","data":[{"dataId":1},{"dataId":2}]}'
# -> {"dataRequestId":3, ...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":3,"answer":true,"providerClaim":"approved for real","data":[{"dataId":1},{"dataId":2}]}'
# -> {"dataRequestAnswerId":3,"answer":"FULL","dataRequestClaim":"approved for real"}
curl -s "http://localhost:8090/right/api/isAccepted?dataSubjectId=1&dataId=1"   # -> true
curl -s "http://localhost:8090/right/api/isAccepted?dataSubjectId=1&dataId=2"   # -> true
```

### 5.2 Rectification (`quantity` 2 → 5)

Before:
```sh
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=quantity"
# -> [{"quantity":"2"}]
```

Request + `answer=false`:
```sh
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Cart","data":{"dataId":2},"newValue":"5","claim":"fix quantity","primaryKeys":[{"primaryKeyId":1,"primaryKeyValue":"OLJCESPC7Z"}]}'
# -> {"dataRequestId":4, ...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":4,"answer":false,"providerClaim":"not this time","data":[]}'
# -> {"dataRequestAnswerId":4,"answer":"REFUSED",...}
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=quantity"
# -> [{"quantity":"2"}]   (UNCHANGED after refusal - confirmed)
```

New request, `answer=true`:
```sh
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Cart","data":{"dataId":2},"newValue":"5","claim":"fix quantity take 2","primaryKeys":[{"primaryKeyId":1,"primaryKeyValue":"OLJCESPC7Z"}]}'
# -> {"dataRequestId":5, ...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":5,"answer":true,"providerClaim":"approved","data":[]}'
# -> {"dataRequestAnswerId":5,"answer":"FULL",...}
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=quantity"
# -> [{"quantity":"5"}]
```

**Independent proof, raw Redis bytes** (not through the Provider bridge's
own code):
```sh
docker exec ob-redis-cart redis-cli HGET 207acaaf-a999-4ede-9ca6-7e1eeaaedda5 data | xxd
# 00000000: 0a24 3230 3761 6361 6166 2d61 3939 392d  .$207acaaf-a999-
# 00000010: 3465 6465 2d39 6361 362d 3765 3165 6561  4ede-9ca6-7e1eea
# 00000020: 6165 6464 6135 120e 0a0a 4f4c 4a43 4553  aedda5....OLJCES
# 00000030: 5043 375a 1005 0a                        PC7Z...
```
Trailing bytes `10 05` = protobuf field 2 (`quantity`, tag `0x10` = field
2 << 3 | varint), value `5` — hand-decoded confirmation the rectification
genuinely landed in the real Redis-stored protobuf, not just in the
Provider bridge's JSON response.

### 5.3 Erasure (single-row precision, §8.1.c pattern)

A second product added first, so erasure of one row can be shown to leave
the other intact:
```sh
curl -s -i -b "shop_session-id=207acaaf-a999-4ede-9ca6-7e1eeaaedda5" -X POST http://localhost:8080/cart \
  --data-urlencode "product_id=66VCHSJNUP" --data-urlencode "quantity=1"
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=product_id,quantity"
# -> [{"product_id":"OLJCESPC7Z","quantity":"5"},{"product_id":"66VCHSJNUP","quantity":"1"}]
```

Request + `answer=false`:
```sh
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Cart","data":{"dataId":1},"claim":"erase this item","primaryKeys":[{"primaryKeyId":1,"primaryKeyValue":"OLJCESPC7Z"}]}'
# -> {"dataRequestId":6, ...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":6,"answer":false,"providerClaim":"not yet","data":[]}'
# -> {"dataRequestAnswerId":6,"answer":"REFUSED",...}
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=product_id,quantity"
# -> [{"product_id":"OLJCESPC7Z","quantity":"5"},{"product_id":"66VCHSJNUP","quantity":"1"}]   (BOTH still present)
```

New request, `answer=true`:
```sh
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Cart","data":{"dataId":1},"claim":"erase this item take 2","primaryKeys":[{"primaryKeyId":1,"primaryKeyValue":"OLJCESPC7Z"}]}'
# -> {"dataRequestId":7, ...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":7,"answer":true,"providerClaim":"approved","data":[]}'
# -> {"dataRequestAnswerId":7,"answer":"FULL",...}
curl -s "http://localhost:8080/api/dataAccessRight?idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5&dataTypeName=Cart&attributes=product_id,quantity"
# -> [{"product_id":"66VCHSJNUP","quantity":"1"}]   (only OLJCESPC7Z removed)
```
Independent raw proof:
```sh
docker exec ob-redis-cart redis-cli --no-raw HGET 207acaaf-a999-4ede-9ca6-7e1eeaaedda5 data
# -> "\n$207acaaf-a999-4ede-9ca6-7e1eeaaedda5\x12\x0e\n\n66VCHSJNUP\x10\x01"
```
Only `66VCHSJNUP` (quantity 1) remains in the raw protobuf — confirmed.

### 5.4 `dataValue` (4th Provider endpoint, §8.2.f)

```sh
curl -s -X POST http://localhost:8090/provider/api/dataValue \
  -H "Content-Type: application/json" \
  -d '{"idRef":"207acaaf-a999-4ede-9ca6-7e1eeaaedda5","dataName":"quantity","primaryKeys":{"product_id":"66VCHSJNUP"}}'
# -> {"value":"1"}
curl -s -X POST http://localhost:8090/provider/api/dataValue \
  -H "Content-Type: application/json" \
  -d '{"idRef":"207acaaf-a999-4ede-9ca6-7e1eeaaedda5","dataName":"product_id","primaryKeys":{"product_id":"66VCHSJNUP"}}'
# -> {"value":"66VCHSJNUP"}
```
No `dataTypeName` in either request body — type inferred correctly from
`dataName` alone (only one DataType, `Cart`, exists in this integration).

## 6. Consent workflow — grant / withdraw / re-grant

Before touching consent, confirmed the observable side effect (recommended
products rendered on the cart page) actually happens when consent is
granted (playbook §7 — test the side effect exists before testing
withdrawal):
```sh
SEED=207acaaf-a999-4ede-9ca6-7e1eeaaedda5
curl -s "http://localhost:8090/cdp/api/decision/Product%20Recommendations?idRefList=$SEED"
# -> {"207acaaf-a999-4ede-9ca6-7e1eeaaedda5":true}
curl -s -b "shop_session-id=$SEED" http://localhost:8080/cart | awk '/class="recommendations"/,/<\/section>/' | grep -oE 'href="/product/[^"]*"'
# -> href="/product/0PUK6V6EV0"
#    href="/product/L9ECAV7KIM"
#    href="/product/LS4PSXUNUM"
#    href="/product/9SIQT8TOJO"
```

Withdraw:
```sh
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/$SEED" \
  -H "Content-Type: application/json" -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":1,"startDate":"2026-07-23T13:37:25.000+00:00","endDate":"2026-07-23T13:46:55.405+00:00","contractId":1}
```
Real DB proof:
```sh
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "SELECT * FROM \`priam-consent\`.consent WHERE contract_id=1;"
# consent_id  start_date            end_date              processing_id  contract_id
# 1           2026-07-23 13:37:25   2026-07-23 13:46:55   2              1
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "SELECT * FROM \`priam-data\`.processed_data WHERE data_subject_id=1;"
# data_id  data_subject_id  nb_occurrences
# 1        1                1     <- decremented from 2 (Product Recommendations only reads product_id, data_usage row 3)
# 2        1                2     <- untouched (quantity is not read by Product Recommendations)
curl -s "http://localhost:8090/cdp/api/decision/Product%20Recommendations?idRefList=$SEED"
# -> {"207acaaf-a999-4ede-9ca6-7e1eeaaedda5":false}
curl -s -b "shop_session-id=$SEED" http://localhost:8080/cart | awk '/class="recommendations"/,/<\/section>/' | grep -oE 'href="/product/[^"]*"'
# -> (no output - recommendations correctly suppressed)
```
`data_id=1`'s occurrence count going 2→1 (not 0→ removed row) rather than
`data_id=2` changing at all confirms `ConsentServiceImpl`'s own
`addProcessedData`/`removeProcessedData` bookkeeping is scoped precisely to
the `data_usage` rows tied to the processing being toggled — same
"double-bookkeeping" behavior already documented and confirmed harmless in
the Bank of Anthos/Mastodon integration reports (PRIAM's own consent
mechanism manages `processed_data` independently of this application's
explicit `reportProcessedData` calls).

Re-grant:
```sh
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/$SEED" \
  -H "Content-Type: application/json" -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":2,"startDate":"2026-07-23T13:47:34.171+00:00","endDate":null,"contractId":1}
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "SELECT * FROM \`priam-consent\`.consent WHERE contract_id=1 ORDER BY consent_id;"
# consent_id  start_date            end_date              processing_id  contract_id
# 1           2026-07-23 13:37:25   2026-07-23 13:46:55   2              1
# 2           2026-07-23 13:47:34   NULL                  2              1   <- new row, not an update of row 1
curl -s "http://localhost:8090/cdp/api/decision/Product%20Recommendations?idRefList=$SEED"
# -> {"207acaaf-a999-4ede-9ca6-7e1eeaaedda5":true}
curl -s -b "shop_session-id=$SEED" http://localhost:8080/cart | awk '/class="recommendations"/,/<\/section>/' | grep -oE 'href="/product/[^"]*"'
# -> href="/product/2ZYFJ3GM2N"
#    href="/product/1YMWWN1N4O"
#    href="/product/0PUK6V6EV0"
#    href="/product/L9ECAV7KIM"
```
(Different product set than before withdrawal — confirms a live
recommendation call, not a cached/stale render.)

## 7. No-redirect-loop confirmation

```sh
curl -s "http://localhost:8090/cdp/api/contract/list/consents/207acaaf-a999-4ede-9ca6-7e1eeaaedda5/Product%20Recommendations"
# -> 2 rows (non-empty)
curl -s -o /dev/null -w "%{http_code}\n" -b "shop_session-id=207acaaf-a999-4ede-9ca6-7e1eeaaedda5" http://localhost:8080/
# -> 200   (no more redirect, since a decision now exists)
```
A brand-new session (no cookie) still redirects correctly:
```sh
curl -s -i http://localhost:8080/ | head -5
# HTTP/1.1 302 Found
# Location: http://localhost:4200/consent
# Set-Cookie: shop_session-id=3cfe623f-3ba8-4d99-ac56-b65cc48051e1; Max-Age=172800
```

## 8. Backfill script

```sh
sh case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh
# Scanning ob-redis-cart for existing session_id keys...
# --- backfilling idRef=207acaaf-a999-4ede-9ca6-7e1eeaaedda5 ---
#   register_data_subject -> HTTP 200
#   dataSubjectId=1
#   report_processed_data -> HTTP 200
# Backfill complete.
```
Real state proof of idempotency (run against a database that already had
this idRef registered via the normal runtime hooks, simulating a re-run):
```sh
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "SELECT * FROM \`priam-actor\`.data_subject;"
# data_subject_id=1 still the only row for id_ref='207acaaf-...' - no duplicate created
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "SELECT * FROM \`priam-data\`.processed_data;"
# data_id=1: nb_occurrences=3, data_id=2: nb_occurrences=3 (incremented from the re-run's
# report_processed_data call, on top of the consent re-grant's own increment - expected,
# non-destructive double-bookkeeping, same as §6 above)
```
This session had **no other pre-existing users** to backfill: the Redis
cart store is freshly created (no volume persistence in this compose
setup, matching upstream's own `emptyDir: {}` for `redis-cart`), and the
only key present was the one this same session's own testing created.
Confirmed via `redis-cli --scan` returning exactly one key. The script is
provided and genuinely exercised (idempotency verified above), even though
there was nothing "pre-existing" to catch up on in this specific run.

## 9. Real-browser testing — not performed, why

No browser-automation tool (Playwright, Puppeteer, or similar) is available
in this environment — checked via `ToolSearch` for `browser|playwright|
screenshot|puppeteer|chrome`, returning only `WebFetch` (a text-summarizing
fetcher that does not render JavaScript/SPAs, cannot click, and cannot
authenticate against Keycloak). `PRIAM-Frontend` (`localhost:4200`) and
`PRIAM-Frontend-Provider` (`localhost:4000`) were built and started, and
confirmed to serve `HTTP 200` (Angular "Compiled successfully", no crash
loop in `docker logs priam-frontend`) — but no DOM interaction, no login,
and no visual confirmation of the Consent/Access-Request pages was
performed. Per playbook §7 point 14, this is stated explicitly rather than
claimed as done: **frontend visual validation was not performed this
session.** Everything needed for a manual follow-up pass is left running:
seed idRef `207acaaf-a999-4ede-9ca6-7e1eeaaedda5` (a decision already
exists for it — to see the forced-redirect UI freshly, use a new browser
with no `shop_session-id` cookie, or the `3cfe623f-...` session captured in
§7 above, which still has a pending decision).
