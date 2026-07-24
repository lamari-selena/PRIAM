# ETAPES-FAITES — raw test log

Every test actually run this session, with the exact command, the real
response, and the real database state observed afterward. Written so a
human or another AI with no prior context can reproduce each test
identically. See `INTEGRATION-REPORT.md` for the summary/bugs/LOC.

## 0. Reference — URLs/ports actually used

| Component | Address | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | `/right`, `/cdp`, `/actor`, `/data`, `/provider` prefixes, per playbook §0 |
| PRIAM-Actor-service (direct) | `http://localhost:8082` | used directly by `priam.go`/backfill script, bypasses Gateway |
| PRIAM-Data-service (direct) | `http://localhost:8081` | same |
| PRIAM-Consent-Service (direct) | `http://localhost:8089` | same — `PRIAM_CDP_URL` in `docker-compose.yml` |
| PRIAM-Right-service | via Gateway only (`/right/**`) | human-facing, JWT required |
| PRIAM-Frontend | `http://localhost:4200` | `PRIAM_FRONTEND_URL` |
| PRIAM-Frontend-Provider | `http://localhost:4000` | |
| Keycloak | `http://localhost:8080`, realm `priam-realm` | |
| MySQL (`priam-databases`) | `localhost:3308` (host) / `mysqldb:3306` (Docker) | root pw `MaiRP_pWd-ToOr` (`.env` `MYSQL_ROOT_PASSWORD`) |
| **OnlineBoutique frontend** | `http://localhost:9090` | container-internal port 8080; host port moved to 9090 because 8080/8081 collide with PRIAM's Keycloak/Data-service — see INTEGRATION-REPORT.md bug #4 |
| OnlineBoutique Provider bridge | same origin, bare `/api/*` (e.g. `http://localhost:9090/api/dataAccessRight`) — reached by PRIAM via `CUSTOM_PROVIDER_URL=http://frontend:8080` on `common_network` | |
| OnlineBoutique SQLite file | `onlineboutique-db-volume/onlineboutique.db` (repo root, bind-mounted) | read directly with a throwaway `keinos/sqlite3` container for proof |

Seed subject: `idRef = 245060b7-c7a8-42e9-b2da-c35dc80ecaac` (real UUID,
captured from a real `POST /accounts/signup`, non-numeric per playbook §7),
`data_subject_id = 1`, email `priam-seed@example.com`.

## 1. Environment setup

### 1.1 Preserve concurrent TeaStore session state

The shared `Databases/db_insertion_script.sql` and root `docker-compose.yml`
were, at the start of this session, mid-way through an uncommitted TeaStore
integration. Per explicit user direction, the TeaStore SQL annotation was
copied aside before being overwritten:

```
cp Databases/db_insertion_script.sql case-studies/TeaStore/priam-integration/db_insertion_script.sql
cp .env case-studies/TeaStore/priam-integration/dot-env-snapshot.txt
mv db-volume db-volume-teastore-backup   # real ~200MB MySQL data dir, preserved
mkdir db-volume                           # fresh, virgin volume for OnlineBoutique
```

### 1.2 Verify the account-persistence code actually compiles

No local Go toolchain was available; used a throwaway container instead
(the pre-existing code had never been built — its own doc said so):

```
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd):/src" -w /src golang:1.26.4-alpine \
  sh -c "go get modernc.org/sqlite@latest && go mod tidy"
MSYS_NO_PATHCONV=1 docker run --rm -v "$(pwd):/src" -w /src -e CGO_ENABLED=0 -e GOOS=linux -e GOARCH=amd64 \
  golang:1.26.4-alpine sh -c "go build -o /tmp/frontend . && echo BUILD_OK && ls -la /tmp/frontend"
```
Result: `modernc.org/sqlite` was missing from `go.mod`/`go.sum` (the prior
session had written code importing it but never run `go mod tidy`) — added
it, then a real build succeeded: `BUILD_OK`, `-rwxr-xr-x ... 33748987 ...
/tmp/frontend`.

### 1.3 Bring up OnlineBoutique's own stack

```
cd case-studies/OnlineBoutique
docker compose build          # all 10 services, including frontend with the new PRIAM code
docker compose up -d
```
All 10 containers reached `Up`.

### 1.4 Capture a real, non-numeric seed idRef

```
curl -s -i -c cookies.txt -X POST http://localhost:8080/accounts/signup \
  -d "email=priam-seed@example.com&password=SuperSecret123&confirm_password=SuperSecret123"
```
Response:
```
HTTP/1.1 302 Found
Location: /
Set-Cookie: shop_session-id=ead8231c-8dae-44e7-be3c-e0f0b0b53467; Max-Age=172800
Set-Cookie: shop_user-id=245060b7-c7a8-42e9-b2da-c35dc80ecaac; Max-Age=172800
```
→ real UUID captured: `245060b7-c7a8-42e9-b2da-c35dc80ecaac`.

**Bug found here** (see INTEGRATION-REPORT.md #1): the `shop_user-id`
cookie above has no `Path` attribute, so it defaulted to `Path=/accounts`
— confirmed by re-running with a home-page visit first:
```
curl -s -i -c cookies2.txt http://localhost:8080/
curl -s -i -b cookies2.txt -c cookies2.txt -X POST http://localhost:8080/accounts/login \
  -d "email=priam-seed@example.com&password=SuperSecret123"
```
→ `Set-Cookie: shop_user-id=245060b7-c7a8-42e9-b2da-c35dc80ecaac; Path=/accounts; Max-Age=172800`
(before fix). After adding `Path: "/"` to both cookie-setting calls in
`accounts_handlers.go`, rebuilding (`docker compose build frontend &&
docker compose up -d frontend`) and re-running the same login: `Path=/`.
Confirmed on the cart page (outside `/accounts`):
```
curl -s -b cookies3.txt http://localhost:8080/cart | grep -o "Log Out\|Log In\|Manage on PRIAM"
```
→ `Log Out` / `Manage on PRIAM` (before the fix this printed `Log In`/`Sign Up`
for an already-logged-in account).

### 1.5 Place a real order for the seed account (needed for Order-type tests)

```
curl -s -i -c c.txt http://localhost:8080/
curl -s -i -b c.txt -c c.txt -X POST http://localhost:8080/accounts/login \
  -d "email=priam-seed@example.com&password=SuperSecret123"
curl -s -i -b c.txt -c c.txt -X POST http://localhost:8080/cart -d "product_id=OLJCESPC7Z&quantity=2"
curl -s -i -b c.txt -c c.txt -X POST http://localhost:8080/cart/checkout \
  -d "email=priam-seed@example.com&street_address=42+Rue+de+la+Paix&zip_code=75002&city=Paris&state=IDF&country=France&credit_card_number=4432801561520454&credit_card_expiration_month=1&credit_card_expiration_year=2030&credit_card_cvv=672"
```
→ `HTTP/1.1 200 OK`, log line `"message":"order placed","order":"7d2ba0d6-8705-11f1-a78d-2a682942c216"`.

Real DB proof:
```
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT id, email FROM users; SELECT order_id, user_id, email, street_address, city, state, zip_code, country FROM orders; SELECT order_id, product_id, quantity FROM order_items;"
```
```
245060b7-c7a8-42e9-b2da-c35dc80ecaac|priam-seed@example.com
7d2ba0d6-8705-11f1-a78d-2a682942c216|245060b7-c7a8-42e9-b2da-c35dc80ecaac|priam-seed@example.com|42 Rue de la Paix|Paris|IDF|75002|France
7d2ba0d6-8705-11f1-a78d-2a682942c216|OLJCESPC7Z|2
```
(At this point PRIAM wasn't up yet — `reportProcessedData` logged a
connection failure to `actor:8082`, expected/fail-safe, and correctly
harmless: `priam: reportProcessedData(245060b7...) id lookup failed: dial
tcp: lookup actor on 127.0.0.11:53: no such host`.)

### 1.6 Finalize the SQL annotation with the real idRef, bring up PRIAM

`Databases/db_insertion_script.sql`'s seed `data_subject` row was set to
the real captured UUID. Then, sequentially (playbook §8.9 — one service at
a time):
```
docker compose build mysqldb
docker compose up -d mysqldb eureka          # wait for healthy
docker compose up -d actor                   # wait
docker compose up -d consent data provider   # wait
docker compose up -d right gateway           # wait
docker compose up -d keycloak
docker compose up -d frontuser frontprovider
```
Two stale-state issues hit along the way (both environment, not PRIAM
bugs — see INTEGRATION-REPORT.md #3/#4): fixed container names left over
from the earlier TeaStore session (`docker rm` on the *stopped* containers,
after explicit user confirmation since it's a destructive action); a host
port collision on 8080/8081 (OnlineBoutique's `frontend` republished on
9090 instead). Final state: all 10 PRIAM containers `healthy`/`Up`, all 10
OnlineBoutique containers `Up`.

Real DB proof the annotation loaded correctly:
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e \
  "SELECT * FROM \`priam-actor\`.data_subject; SELECT data_id,data_name,data_type_id,is_primary_key FROM \`priam-data\`.data; SELECT processing_id,processing_name,processing_type FROM \`priam-data\`.processing; SELECT * FROM \`priam-data\`.processed_data;"
```
```
data_subject_id  age  id_ref                                data_subject_category_id
1                16   245060b7-c7a8-42e9-b2da-c35dc80ecaac  1

data_id  data_name       data_type_id  is_primary_key
1        email           1             0
2        order_id        2             1
3        email           2             0
4        street_address  2             0
5        city            2             0
6        state           2             0
7        zip_code        2             0
8        country         2             0

processing_id  processing_name           processing_type
1              Account Management        NECESSARY
2              Order Fulfillment          NECESSARY
3              Product Recommendations    OPTIONAL

data_id  data_subject_id  nb_occurrences
1..8     1                1   (one row per data_id, from the seed script's processed_data inserts)
```

### 1.7 Provision a Keycloak test account bound to the seed idRef

Automatic Keycloak provisioning was deliberately not implemented this
session (see INTEGRATION-REPORT.md §5) — this account was created manually,
purely to obtain a real JWT for testing the authenticated `/right`/`/cdp`
Gateway routes.

```
ADMIN_TOKEN=$(curl -s -X POST "http://localhost:8080/realms/master/protocol/openid-connect/token" \
  -d "client_id=admin-cli" -d "username=admin" -d "password=admin" -d "grant_type=password" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -s -H "Authorization: Bearer $ADMIN_TOKEN" "http://localhost:8080/admin/realms/priam-realm/users/profile" \
  | python3 -c "import sys,json; d=json.load(sys.stdin); print([a['name'] for a in d['attributes']])"
# -> ['username', 'email', 'firstName', 'lastName', 'idReference']  (already declared by an earlier case study)

curl -s -o /dev/null -w "create user -> HTTP %{http_code}\n" \
  -X POST "http://localhost:8080/admin/realms/priam-realm/users" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" \
  -d '{
    "username": "priam-seed@example.com",
    "email": "priam-seed@example.com",
    "firstName": "priam-seed@example.com",
    "lastName": "priam-seed@example.com",
    "enabled": true, "emailVerified": true,
    "credentials": [{"type": "password", "value": "SuperSecret123", "temporary": false}],
    "attributes": {"idReference": ["245060b7-c7a8-42e9-b2da-c35dc80ecaac"]}
  }'
# -> create user -> HTTP 201
```

Obtain and decode a real token:
```
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/priam-realm/protocol/openid-connect/token" \
  -d "client_id=Data-client" -d "username=priam-seed@example.com" -d "password=SuperSecret123" -d "grant_type=password" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")
```
Decoded payload (base64url of the middle JWT segment):
```
idReference: 245060b7-c7a8-42e9-b2da-c35dc80ecaac
preferred_username: priam-seed@example.com
iss: http://localhost:8080/realms/priam-realm
```
`Data-client` is `publicClient: true, directAccessGrantsEnabled: true`
(`Keycloak/priam-realm-realm.json`) — Direct Grant works without a client
secret.

## 2. Consent workflow (grant / withdraw / re-grant), real proof

Baseline sanity check of the CEP/CIP endpoints, called the same way
`priam.go` calls them (directly against `PRIAM-Consent-Service`, bypassing
the Gateway — these are the machine-to-machine calls, not the
human-facing `/cdp` Gateway route):
```
curl -s "http://localhost:8089/api/decision/Product%20Recommendations?idRefList=245060b7-c7a8-42e9-b2da-c35dc80ecaac"
# -> {}   (empty = no decision yet = getConsent() returns false, fail-closed)
curl -s "http://localhost:8089/api/contract/list/consents/245060b7-c7a8-42e9-b2da-c35dc80ecaac/Product%20Recommendations"
# -> []   (empty = hasPendingConsentDecision() returns true)
```

### 2.1 Baseline: no consent decision yet → recommendations must not show

```
curl -s -c c.txt http://localhost:9090/
curl -s -b c.txt -c c.txt -X POST http://localhost:9090/accounts/login -d "email=priam-seed@example.com&password=SuperSecret123"
curl -s -b c.txt -c c.txt -X POST http://localhost:9090/cart -d "product_id=66VCHSJNUP&quantity=1"
curl -s -b c.txt http://localhost:9090/cart | grep -o "You May Also Like"
# -> (no output: absent, as expected)
```

### 2.2 Grant (real API, real JWT, as the human-facing Gateway route)

```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/245060b7-c7a8-42e9-b2da-c35dc80ecaac" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"processingId":"Product Recommendations"}'
```
Response: `{"consentId":1,"startDate":"2026-07-24T02:47:30.981+00:00","endDate":null,"processing":null,"contractId":1}`

Real DB proof:
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e \
  "SELECT * FROM \`priam-consent\`.contract; SELECT * FROM \`priam-consent\`.consent;"
```
```
contract_id  signature_date  expiration_date  data_subject_id
1            2026-07-24      NULL             1

consent_id  start_date           end_date  processing_id  contract_id
1           2026-07-24 02:47:31  NULL      3              1
```

Observable side effect, not just the API response:
```
curl -s "http://localhost:8089/api/decision/Product%20Recommendations?idRefList=245060b7-c7a8-42e9-b2da-c35dc80ecaac"
# -> {"245060b7-c7a8-42e9-b2da-c35dc80ecaac":true}
curl -s -b c.txt http://localhost:9090/cart | grep -o "You May Also Like"
# -> You May Also Like   (PRESENT)
```

### 2.3 Withdraw

Same endpoint — `ConsentServiceImpl.create` toggles the existing
open-ended consent (`endDate == null`) to closed:
```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/245060b7-c7a8-42e9-b2da-c35dc80ecaac" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":1,"startDate":"...","endDate":"2026-07-24T02:47:51.534+00:00","processing":null,"contractId":1}
```
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT * FROM \`priam-consent\`.consent;"
# -> consent_id=1, end_date=2026-07-24 02:47:52 (was NULL)
curl -s "http://localhost:8089/api/decision/Product%20Recommendations?idRefList=245060b7-c7a8-42e9-b2da-c35dc80ecaac"
# -> {"245060b7-c7a8-42e9-b2da-c35dc80ecaac":false}
curl -s -b c.txt http://localhost:9090/cart | grep -o "You May Also Like"
# -> (no output: absent, correct)
```

### 2.4 Re-grant

```
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/245060b7-c7a8-42e9-b2da-c35dc80ecaac" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":2,"startDate":"...","endDate":null,"processing":null,"contractId":1}
```
A **new** `consent_id=2` row (matches `ConsentServiceImpl.create`'s
documented case-1b path: prior consent already had a non-null `endDate`, so
a fresh row is created rather than reusing the old one).
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e "SELECT * FROM \`priam-consent\`.consent;"
```
```
consent_id  start_date           end_date              processing_id  contract_id
1           2026-07-24 02:47:31  2026-07-24 02:47:52   3              1
2           2026-07-24 02:48:01  NULL                  3              1
```
```
curl -s -b c.txt http://localhost:9090/cart | grep -o "You May Also Like"
# -> You May Also Like   (PRESENT, correct)
```

## 3. Rights workflow, real proof (via PRIAM-Right-service, never the Provider bridge directly)

`dataSubjectId` (internal numeric id) resolved once:
```
curl -s "http://localhost:8082/api/DataSubjectId/245060b7-c7a8-42e9-b2da-c35dc80ecaac"
# -> 1
```

### 3.1 Rectification — `answer=false`, then `answer=true`

```
# BEFORE
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT order_id, street_address FROM orders;"
# -> 7d2ba0d6-8705-11f1-a78d-2a682942c216|42 Rue de la Paix

# 1) create request
curl -s -X POST "http://localhost:8090/right/api/right/rectificationRequest" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":4},"newValue":"99 Avenue du Test","claim":"please fix my street address","primaryKeys":[{"primaryKeyId":2,"primaryKeyValue":"7d2ba0d6-8705-11f1-a78d-2a682942c216"}]}'
# -> {"dataRequestId":1, ...}

# 2) answer=false
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":1,"answer":false,"providerClaim":"refused for test","data":[]}'
# -> {"dataRequestAnswerId":1,"answer":"REFUSED","dataRequestClaim":"refused for test"}

# AFTER answer=false: unchanged
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT order_id, street_address FROM orders;"
# -> 7d2ba0d6-8705-11f1-a78d-2a682942c216|42 Rue de la Paix   (unchanged, correct)

# 3) new request + answer=true
curl -s -X POST "http://localhost:8090/right/api/right/rectificationRequest" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":4},"newValue":"99 Avenue du Test","claim":"please fix my street address","primaryKeys":[{"primaryKeyId":2,"primaryKeyValue":"7d2ba0d6-8705-11f1-a78d-2a682942c216"}]}'
# -> {"dataRequestId":2, ...}
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":2,"answer":true,"providerClaim":"approved for test","data":[{"dataId":4}]}'
# -> {"dataRequestAnswerId":2,"answer":"FULL","dataRequestClaim":"approved for test"}

# AFTER answer=true: real change
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT order_id, street_address FROM orders;"
# -> 7d2ba0d6-8705-11f1-a78d-2a682942c216|99 Avenue du Test   (CHANGED — real, automatic Provider bridge call)
```

### 3.2 Access request

```
curl -s -X POST "http://localhost:8090/right/api/right/accessRequest" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"please show me my data","data":[{"dataId":1}]}'
# -> {"dataRequestId":3, ...}

curl -s "http://localhost:8090/right/api/isAccepted?dataSubjectId=1&dataId=1"
# -> false   (before answer)

curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":3,"answer":true,"providerClaim":"approved","data":[{"dataId":1}]}'
# -> {"dataRequestAnswerId":3,"answer":"FULL","dataRequestClaim":"approved"}

curl -s "http://localhost:8090/right/api/isAccepted?dataSubjectId=1&dataId=1"
# -> true   (after answer=true)

curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  -H "Authorization: Bearer $TOKEN" \
  --data-urlencode "dataSubjectId=1" --data-urlencode "dataTypeName=User" --data-urlencode "attributes=email"
# -> [{"email":"priam-seed@example.com"}]   (the real, always-open read endpoint, per playbook §3 point 3)
```

### 3.3 Erasure — the bug (INTEGRATION-REPORT.md #2) and its fix

A second, disposable order was created first so this test didn't consume
the main seed order used above:
```
curl -s -c c2.txt http://localhost:9090/
curl -s -b c2.txt -c c2.txt -X POST http://localhost:9090/accounts/login -d "email=priam-seed@example.com&password=SuperSecret123"
curl -s -b c2.txt -c c2.txt -X POST http://localhost:9090/cart -d "product_id=1YMWWN1N4O&quantity=1"
curl -s -b c2.txt -c c2.txt -X POST http://localhost:9090/cart/checkout \
  -d "email=priam-seed@example.com&street_address=1+Throwaway+St&zip_code=10001&city=Testville&state=NY&country=USA&credit_card_number=4432801561520454&credit_card_expiration_month=1&credit_card_expiration_year=2030&credit_card_cvv=672"
```
→ new order `5aec5516-870a-11f1-a78d-2a682942c216`. Confirmed `reportProcessedData`
fired for real this time (PRIAM was up): `nb_occurrences` for `data_id`
2–8 went from 1 → 2 in `priam-data.processed_data`.

`answer=false` cycle (correct — no deletion):
```
curl -s -X POST "http://localhost:8090/right/api/right/erasureRequest" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":8},"claim":"please delete this order","primaryKeys":[{"primaryKeyId":2,"primaryKeyValue":"5aec5516-870a-11f1-a78d-2a682942c216"}]}'
# -> {"dataRequestId":4, ...}
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":4,"answer":false,"providerClaim":"refused for test","data":[]}'
# -> {"dataRequestAnswerId":4,"answer":"REFUSED",...}
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT order_id FROM orders WHERE order_id='5aec5516-870a-11f1-a78d-2a682942c216';"
# -> 5aec5516-870a-11f1-a78d-2a682942c216   (still exists, correct)
```

`answer=true` cycle — **this is where the bug surfaced**:
```
curl -s -X POST "http://localhost:8090/right/api/right/erasureRequest" ... (same body)
# -> {"dataRequestId":5, ...}
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":5,"answer":true,"providerClaim":"approved","data":[]}'
# -> {"timestamp":"2026-07-24T02:50:42.758+00:00","status":500,"error":"Internal Server Error","path":"/api/right/answer"}
docker run --rm -v "<repo>/onlineboutique-db-volume:/data:ro" keinos/sqlite3 sqlite3 /data/onlineboutique.db \
  "SELECT COUNT(*) FROM orders WHERE order_id='5aec5516-870a-11f1-a78d-2a682942c216'; SELECT COUNT(*) FROM order_items WHERE order_id='5aec5516-870a-11f1-a78d-2a682942c216';"
# -> 1 / 1   (NOT deleted — the 500 was real, not a red herring)
```
Direct Provider-bridge call to isolate the failure (the one intentional
direct call in this session, used only to diagnose):
```
curl -s -X POST "http://localhost:9090/api/erasure" -H "Content-Type: application/json" \
  -d '{"idRef":"245060b7-c7a8-42e9-b2da-c35dc80ecaac","dataTypeName":"Order","dataName":"country","primaryKeys":{"order_id":"5aec5516-870a-11f1-a78d-2a682942c216"}}'
# -> {"error":"constraint failed: FOREIGN KEY constraint failed (787)"}
```
`docker logs priam-right-ms` showed a Java stack trace from the failed
downstream call; `docker logs ob-frontend` showed the `POST /api/erasure`
request completing with a `500`. Root cause and fix: INTEGRATION-REPORT.md
bug #2 (`priamEraseOrder` deleted `orders` before `order_items`). After
fixing, rebuilding (`docker compose build frontend && docker compose up -d
frontend`), the direct call succeeded:
```
curl -s -X POST "http://localhost:9090/api/erasure" ... (same body)
# -> {"success":true}
docker run ... "SELECT COUNT(*) FROM orders WHERE order_id='5aec5516-...'; SELECT COUNT(*) FROM order_items WHERE order_id='5aec5516-...';"
# -> 0 / 0
```

**Full re-verification through the real workflow** (since the fix above
was confirmed via a direct call, not the real cycle) — a third disposable
order (`e212ffe3-870a-11f1-a78d-2a682942c216`) was created and put through
the complete `answer=false`→`answer=true` cycle again. The JWT had expired
by this point (~5 minutes, matching the playbook's documented session
lifetime — `WWW-Authenticate: Bearer error="invalid_token",
error_description="Jwt expired at 2026-07-24T02:50:53Z"`), so a fresh token
was obtained first:
```
TOKEN=$(curl -s -X POST "http://localhost:8080/realms/priam-realm/protocol/openid-connect/token" \
  -d "client_id=Data-client" -d "username=priam-seed@example.com" -d "password=SuperSecret123" -d "grant_type=password" \
  | python3 -c "import sys,json;print(json.load(sys.stdin)['access_token'])")

curl -s -X POST "http://localhost:8090/right/api/right/erasureRequest" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":8},"claim":"please delete this order","primaryKeys":[{"primaryKeyId":2,"primaryKeyValue":"e212ffe3-870a-11f1-a78d-2a682942c216"}]}'
# -> {"dataRequestId":6, ...}
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":6,"answer":false,"providerClaim":"refused","data":[]}'
# -> {"dataRequestAnswerId":5,"answer":"REFUSED",...}
docker run ... "SELECT COUNT(*) FROM orders WHERE order_id='e212ffe3-...';"
# -> 1   (still exists, correct)

curl -s -X POST "http://localhost:8090/right/api/right/erasureRequest" ... (same body)
# -> {"dataRequestId":7, ...}
curl -s -X POST "http://localhost:8090/right/api/right/answer" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataRequestId":7,"answer":true,"providerClaim":"approved","data":[]}'
# -> {"dataRequestAnswerId":6,"answer":"FULL","dataRequestClaim":"approved"}
docker run ... "SELECT COUNT(*) FROM orders WHERE order_id='e212ffe3-...'; SELECT COUNT(*) FROM order_items WHERE order_id='e212ffe3-...';"
# -> 0 / 0   (real erasure, through the real workflow, post-fix)
```

## 4. Registration + forced-redirect, real-time (not backfill)

```
curl -s -c cn.txt http://localhost:9090/
curl -s -i -b cn.txt -c cn.txt -X POST http://localhost:9090/accounts/signup \
  -d "email=priam-redirect-test@example.com&password=SuperSecret123&confirm_password=SuperSecret123"
```
```
HTTP/1.1 302 Found
Location: http://localhost:4200/consent
Set-Cookie: shop_user-id=82fb7d5e-b965-4192-bad8-5be23b03dfd1; Path=/; Max-Age=172800
```
Real DB proof:
```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e \
  "SELECT * FROM \`priam-actor\`.data_subject WHERE id_ref='82fb7d5e-b965-4192-bad8-5be23b03dfd1';"
# -> data_subject_id=2, id_ref=82fb7d5e-b965-4192-bad8-5be23b03dfd1, category 1

docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e \
  "SELECT ds.id_ref, pd.data_id, pd.nb_occurrences FROM \`priam-data\`.processed_data pd JOIN \`priam-actor\`.data_subject ds ON ds.data_subject_id=pd.data_subject_id WHERE ds.id_ref='82fb7d5e-...';"
# -> 82fb7d5e-...  |  1  |  1     (User.email reported, no ordering race — §8.6)
```

## 5. Round-trip navigation (§4ter)

```
curl -s -b <logged-in cookie jar> http://localhost:9090/ | grep -o 'href="[^"]*" class="cart-link" title="Manage on PRIAM"'
# -> href="http://localhost:4200" class="cart-link" title="Manage on PRIAM"
```
PRIAM-Frontend's own bundle (**note**: this required a rebuild — see
INTEGRATION-REPORT.md bug #3, the stale-image pitfall found here):
```
curl -s http://localhost:4200/main.js | grep -o "localhost:9090[a-zA-Z0-9/:.]*"
# -> localhost:9090/
```

## 6. Backfill script

```
MSYS_NO_PATHCONV=1 DB_VOLUME="<repo>/onlineboutique-db-volume" \
  sh case-studies/OnlineBoutique/priam-integration/backfill-data-subjects.sh
```
```
Reading users from <repo>/onlineboutique-db-volume/onlineboutique.db ...
--- backfilling idRef=245060b7-c7a8-42e9-b2da-c35dc80ecaac ---
  registerDataSubject -> HTTP 200
  dataSubjectId=1
  reportProcessedData(User) -> HTTP 200
  1 pre-existing order(s)
  reportProcessedData(Order) -> HTTP 200
--- backfilling idRef=82fb7d5e-b965-4192-bad8-5be23b03dfd1 ---
  registerDataSubject -> HTTP 200
  dataSubjectId=2
  reportProcessedData(User) -> HTTP 200
  0 pre-existing order(s)
Backfill complete.
```
Exit code 0. See INTEGRATION-REPORT.md bug #5 for the `set -e` issue hit
(and fixed) while developing this. Since both accounts were already
registered in real time by the sign-up hook before this ran, the real
effect is idempotent bookkeeping (`nb_occurrences` incremented again — the
script's own doc comment already discloses it is not perfectly idempotent
for that counter), not new subjects appearing — there were no genuinely
pre-hook accounts in this integration to catch up on.

## 7. Final real state snapshot (end of session)

```
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -e \
  "SELECT * FROM \`priam-actor\`.data_subject;"
```
```
data_subject_id  age   id_ref                                 data_subject_category_id
1                16    245060b7-c7a8-42e9-b2da-c35dc80ecaac   1
2                NULL  82fb7d5e-b965-4192-bad8-5be23b03dfd1   1
3                NULL  0ed3bbe9-9630-4f9a-9f44-5a1fffaae58d   1
```
`data_subject_id=3` was **not** created by any command in this session —
its `idRef` resolves to a real account `lam@gmail.com` in
`onlineboutique.db`. This is consistent with independent real browser
traffic against the running stack (e.g. the user testing manually) during
this session, though it was not something this agent drove or can take
credit for — see INTEGRATION-REPORT.md §4's "Real browser interaction"
note.
