# ÉTAPES FAITES — TeaStore × PRIAM

Raw detail of every test actually run this session, with real state proof
(not just HTTP 200s). Reproduce identically with the commands below.

## 0. Reference — URLs/ports actually used in this integration

| Component | URL |
|---|---|
| PRIAM Gateway | `http://localhost:8090` |
| — Rights (`PRIAM-Right-service`) | `http://localhost:8090/right/api/...` |
| — Consent (`PRIAM-Consent-service`) | `http://localhost:8090/cdp/api/...` |
| — Actor (`PRIAM-Actor-service`) | `http://localhost:8090/actor/api/...` |
| — Data (`PRIAM-Data-service`) | `http://localhost:8090/data/api/...` |
| — Provider bridge (routed to TeaStore) | `http://localhost:8090/provider/api/...` |
| TeaStore webui (Provider bridge lives here too, at `/api/*` under its own context) | `http://localhost:9100/tools.descartes.teastore.webui/` |
| TeaStore persistence REST (internal only — no host port published; use `docker exec teastore-persistence-1 curl ...`) | `http://persistence:8080/tools.descartes.teastore.persistence/rest/...` |
| PRIAM-Frontend (data subject) | `http://localhost:4200` |
| PRIAM-Frontend-Provider (data controller) | `http://localhost:4000` |
| Keycloak | `http://localhost:8080` (realm `priam-realm`) |
| MySQL (PRIAM's own DB, direct proof reads) | `docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' ...` |

Test subjects used:
- `user1` (idRef = TeaStore `userName`, a DataGenerator-seeded account,
  `data_subject_id=1`, pre-seeded in `Databases/db_insertion_script.sql`).
  Internal TeaStore persistence id `592`, 3 real orders (`1020`, `1046`,
  `1102`).
- `selena.test.subject` / `selena.test.subject2` — brand-new accounts
  created live through `POST /register` during this session, to satisfy the
  "non-numeric idRef, not a specially crafted one" requirement with a
  genuinely fresh subject (not just the already-non-numeric `user1`).

## 1. Backfill — pre-existing seeded users

```bash
# Throwaway container on both networks (common_network for PRIAM, teastore's
# own internal network for its persistence service)
docker run -d --name priam-backfill-teastore --network common_network \
  -v "/c/Users/lamar/Documents/priam-experimentation:/w" -w //w python:3.11-slim sh -c "sleep 3600"
docker network connect teastore_teastore_internal priam-backfill-teastore
docker exec priam-backfill-teastore pip install -q requests
docker exec priam-backfill-teastore python //w/case-studies/TeaStore/priam-integration/backfill-data-subjects.py
```

Output: 100 users backfilled, 0 failures (e.g. `user1: registered, 3
order(s) reported`).

**Real state proof**:

```bash
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-actor\`; SELECT COUNT(*) FROM data_subject;"
# -> 100

docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-data\`; SELECT * FROM processed_data WHERE data_subject_id=1 ORDER BY data_id;"
# data_id 1,2,3  (User fields)   -> nb_occurrences=1  (reported once at backfill)
# data_id 4..10 (Order fields)   -> nb_occurrences=3  (user1 has 3 real orders)
```

## 2. Rights workflow — access request (User fields, single-row type)

```bash
curl -s -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"Access request test - User fields","data":[{"dataId":1},{"dataId":2},{"dataId":3}]}'
# -> {"dataRequestId":2, ..., "response":false, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":2,"answer":true,"providerClaim":"approved for testing","data":[{"dataId":1},{"dataId":2},{"dataId":3}]}'
# -> {"dataRequestAnswerId":1,"answer":"FULL", ...}

curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  --data-urlencode "dataSubjectId=1" --data-urlencode "dataTypeName=User" \
  --data-urlencode "attributes=userName" --data-urlencode "attributes=email" --data-urlencode "attributes=realName"
```

**Real state proof**: `[{"userName":"user1","email":"user1@teastore.com","realName":"Dorothy Brown"}]`
— matches TeaStore's persistence database directly (verified separately via
`docker exec teastore-persistence-1 curl -s
http://localhost:8080/tools.descartes.teastore.persistence/rest/users/name/user1`).

## 3. Rights workflow — access request (Order fields, multi-row type)

```bash
curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  --data-urlencode "dataSubjectId=1" --data-urlencode "dataTypeName=Order" \
  --data-urlencode "attributes=id" --data-urlencode "attributes=addressName" --data-urlencode "attributes=creditCardNumber"
```

**Real state proof**: all 3 real orders returned
(`[{"id":"1020",...},{"id":"1046",...},{"id":"1102",...}]`), each with the
real `addressName`/`creditCardNumber` from TeaStore's DB.

## 4. Rights workflow — rectification (refuse then approve)

Before:

```bash
docker exec teastore-persistence-1 curl -s "http://localhost:8080/tools.descartes.teastore.persistence/rest/orders/1020"
# addressName: "Dorothy Brown"
```

Refuse:

```bash
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":5},"newValue":"REFUSED_TEST_VALUE","claim":"rectify addressName - refusal test","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"1020"}]}'
# -> {"dataRequestId":3, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":3,"answer":false,"providerClaim":"refused for testing","data":[]}'
# -> {"dataRequestAnswerId":2,"answer":"REFUSED", ...}
```

**Proof of absence**: `GET .../orders/1020` again → `addressName` still
`"Dorothy Brown"` — unchanged.

Approve (new request — a `DataRequest` can only be answered once):

```bash
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":5},"newValue":"Jane Rectified TestName","claim":"rectify addressName - approval test","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"1020"}]}'
# -> {"dataRequestId":4, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":4,"answer":true,"providerClaim":"approved for testing","data":[{"dataId":5}]}'
# -> {"dataRequestAnswerId":3,"answer":"FULL", ...}
```

**Real state proof**: `GET .../orders/1020` → `"addressName":"Jane
Rectified TestName"` — real change, auto-executed by PRIAM (not by a direct
call to the Provider bridge).

## 5. Rights workflow — erasure (refuse then approve)

Before: `GET .../orders/1046` → `creditCardCompany: "Visa"`.

Refuse:

```bash
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":8},"claim":"erase creditCardCompany - refusal test","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"1046"}]}'
# -> {"dataRequestId":5, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":5,"answer":false,"providerClaim":"refused for testing","data":[]}'
```

**Proof of absence**: `GET .../orders/1046` → `creditCardCompany` still
`"Visa"`.

Approve:

```bash
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Order","data":{"dataId":8},"claim":"erase creditCardCompany - approval test","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"1046"}]}'
# -> {"dataRequestId":6, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Content-Type: application/json" \
  -d '{"dataRequestId":6,"answer":true,"providerClaim":"approved for testing","data":[{"dataId":8}]}'
```

**Real state proof**: `GET .../orders/1046` → `"creditCardCompany":""` —
real erasure.

## 6. Consent workflow — full grant → withdraw → re-grant cycle

Pre-state (never decided):

```bash
curl -s -G "http://localhost:8090/cdp/api/decision/Product%20Recommendations" --data-urlencode "idRefList=user1"
# -> {}
curl -s "http://localhost:8090/cdp/api/contract/list/consents/user1/Product%20Recommendations"
# -> []
```

Grant:

```bash
curl -s -X POST http://localhost:8090/cdp/api/consent/create/user1 \
  -H "Content-Type: application/json" -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":1,"startDate":"...","endDate":null,"contractId":1}
```

**Real state proof**:
```bash
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-consent\`; SELECT * FROM consent WHERE processing_id=3;"
# consent_id=1, end_date=NULL

docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-data\`; SELECT * FROM processed_data WHERE data_subject_id=1 AND data_id=4;"
# nb_occurrences: 3 -> 4 (auto-incremented by ConsentServiceImpl.create)

curl -s -G "http://localhost:8090/cdp/api/decision/Product%20Recommendations" --data-urlencode "idRefList=user1"
# -> {"user1":true}
```

Withdraw (same endpoint toggles):

```bash
curl -s -X POST http://localhost:8090/cdp/api/consent/create/user1 \
  -H "Content-Type: application/json" -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":1,"startDate":"...","endDate":"2026-07-24T00:43:31...","contractId":1}
```

**Real state proof**: `end_date` now set; `processed_data(4,1).nb_occurrences`
back to 3; `GET decision` → `{"user1":false}`.

Re-grant:

```bash
curl -s -X POST http://localhost:8090/cdp/api/consent/create/user1 \
  -H "Content-Type: application/json" -d '{"processingId":"Product Recommendations"}'
# -> {"consentId":2,"startDate":"...","endDate":null,"contractId":1}
```

**Real state proof**: new `consent` row (`end_date NULL`);
`processed_data(4,1).nb_occurrences` back to 4; `GET decision` →
`{"user1":true}`.

### Observation-mechanism check (§7) — recommender independently broken

Before drawing any conclusion from the Cart page's HTML, the observation
mechanism itself was checked first, per the playbook:

```bash
# Real login, real cart page (see §8 for full login command)
curl -s -b cookies.txt "http://localhost:9100/tools.descartes.teastore.webui/cart" | grep -c "interested in"
# -> 0, both before AND after granting consent

# Direct call to TeaStore's own Recommender, bypassing PRIAM entirely
docker exec teastore-recommender-1 curl -s "http://localhost:8080/tools.descartes.teastore.recommender/rest/train"
# -> "The (re)train was succesfully done. It took 424ms and 881 of Orderitems were retrieved..."
docker exec teastore-recommender-1 curl -s -X POST \
  "http://localhost:8080/tools.descartes.teastore.recommender/rest/recommend?uid=592" \
  -H "Content-Type: application/json" -d "[]"
# -> [] (tried uid 1,2,3,4,5,100,300,500,592,599,600,601 - all empty)
```

Conclusion: TeaStore's own Recommender returns nothing for any user,
independent of consent — a pre-existing limitation documented in
INTEGRATION-REPORT.md §3, not a PRIAM or integration bug. The CEP mechanism
itself (decision flip + bookkeeping above) is unaffected and fully verified.

## 7. Fresh sign-up, non-numeric idRef — full chain (register → order → access request)

```bash
curl -s -c cookies_new.txt -X POST "http://localhost:9100/tools.descartes.teastore.webui/register" \
  --data-urlencode "username=selena.test.subject" --data-urlencode "password=TestPass123!" \
  --data-urlencode "email=selena.test@example.com" --data-urlencode "realName=Selena Test" \
  -D headers.txt -o /dev/null -w "HTTP:%{http_code}\n"
# HTTP:302
# Set-Cookie: sessionBlob=...%22userName%22%3A%22selena.test.subject%22%2C%22priamConsentRequired%22%3Atrue%7D
# Location: http://localhost:4200/consent
```

**Real state proof**:

```bash
docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-actor\`; SELECT * FROM data_subject WHERE id_ref='selena.test.subject';"
# data_subject_id=101, id_ref='selena.test.subject'

docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e "
USE \`priam-data\`;
SELECT pd.* FROM processed_data pd
JOIN \`priam-actor\`.data_subject ds ON ds.data_subject_id = pd.data_subject_id
WHERE ds.id_ref='selena.test.subject';"
# data_id 1,2,3, nb_occurrences=1 each (User fields reported at sign-up)
```

Access request for this brand-new subject (the exact §8.1.b scenario):

```bash
curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  --data-urlencode "dataSubjectId=101" --data-urlencode "dataTypeName=User" \
  --data-urlencode "attributes=userName" --data-urlencode "attributes=email" --data-urlencode "attributes=realName"
# -> [{"userName":"selena.test.subject","email":"selena.test@example.com","realName":"Selena Test"}]
```

Now place a real order for this same subject (proves `report_processed_data`
fires at order creation too, not just sign-up):

```bash
curl -s -b cookies_new.txt -c cookies_new.txt \
  "http://localhost:9100/tools.descartes.teastore.webui/cartAction?addToCart&productid=7"
curl -s -b cookies_new.txt -c cookies_new.txt -X POST \
  "http://localhost:9100/tools.descartes.teastore.webui/cartAction" \
  --data-urlencode "confirm=Order" \
  --data-urlencode "firstname=Selena" --data-urlencode "lastname=Test" \
  --data-urlencode "address1=1 Test Street" --data-urlencode "address2=Test City" \
  --data-urlencode "cardtype=Visa" --data-urlencode "cardnumber=4111111111111111" \
  --data-urlencode "expirydate=12/2030"
```

**Real state proof**:
```bash
docker exec teastore-persistence-1 curl -s "http://localhost:8080/tools.descartes.teastore.persistence/rest/orders/user/1501?start=-1&max=-1"
# -> [{"id":1502, "addressName":"Selena Test", ...}]  (real new order)

docker exec priam-databases mysql -upriamu -p'MaiRP_pWd-UsEr' -e \
  "USE \`priam-data\`; SELECT * FROM processed_data WHERE data_subject_id=101 ORDER BY data_id;"
# data_id 1..10, all nb_occurrences=1 (Order fields 4-10 appeared only after this order,
# not at sign-up - proves the placeOrder hook, not just the register hook)

curl -s -G "http://localhost:8090/right/api/personalDataValues/accessRight" \
  --data-urlencode "dataSubjectId=101" --data-urlencode "dataTypeName=Order" \
  --data-urlencode "attributes=id" --data-urlencode "attributes=addressName" --data-urlencode "attributes=creditCardNumber"
# -> [{"id":"1502","addressName":"Selena Test","creditCardNumber":"4111111111111111"}]
```

## 8. Bidirectional navigation — "Manage on PRIAM"

```bash
curl -s -X POST "http://localhost:9100/tools.descartes.teastore.webui/loginAction" \
  -c cookies.txt --data-urlencode "username=user1" --data-urlencode "password=password"
curl -s -b cookies.txt "http://localhost:9100/tools.descartes.teastore.webui/profile" | grep -o 'href="[^"]*consent[^"]*"\|Manage on PRIAM'
# -> href="http://localhost:4200/consent"
# -> Manage on PRIAM
```

TARGET_APP_URL (root `.env`) is set to
`http://localhost:9100/tools.descartes.teastore.webui/` — the real
storefront home page, confirmed reachable (`HTTP 200`).

## 9. Keycloak provisioning + credential sync

Second fresh sign-up (the first hit a transient 3s timeout under heavy
system load — see INTEGRATION-REPORT.md §3 — this one succeeded cleanly):

```bash
curl -s -X POST "http://localhost:9100/tools.descartes.teastore.webui/register" \
  --data-urlencode "username=selena.test.subject2" --data-urlencode "password=TestPass456!" \
  --data-urlencode "email=selena.test2@example.com" --data-urlencode "realName=Selena Test Two"
```

**Real state proof** (Keycloak Admin API):

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" | jq -r .access_token)
curl -s "http://localhost:8080/admin/realms/priam-realm/users?username=selena.test2@example.com" \
  -H "Authorization: Bearer $TOKEN"
```

```json
[{"username":"selena.test2@example.com","firstName":"Selena Test Two",
  "lastName":"Selena Test Two","email":"selena.test2@example.com",
  "emailVerified":true,"attributes":{"idReference":["selena.test.subject2"]},
  "enabled":true}]
```

Credential sync (Direct Grant with the password just chosen at sign-up):

```bash
curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d "grant_type=password&client_id=Data-client&username=selena.test2@example.com&password=TestPass456!"
```

`200`, real access token issued. Decoded payload contains
`"idReference": "selena.test.subject2"`, `"preferred_username":
"selena.test2@example.com"` — the claim mapping (`Keycloak/priam-realm-realm.json`,
generic, unmodified) correctly exposes the real TeaStore idRef.

## 9bis. Keycloak provisioning for pre-existing seeded users (bug #3, INTEGRATION-REPORT.md §2)

Found live: `user2`/`password` logs into TeaStore itself but not into
PRIAM-Frontend, because the backfill script never provisioned Keycloak
accounts for pre-existing seeded users (only the new `/register` endpoint
did). Fixed by adding `provision_keycloak_user()` to the backfill script
(well-known seed password `"password"`), then re-ran it:

```bash
docker run -d --name priam-backfill-teastore2 --network common_network \
  -v "/c/Users/lamar/Documents/priam-experimentation:/w" -w //w python:3.11-slim sh -c "sleep 3600"
docker network connect teastore_teastore_internal priam-backfill-teastore2
docker exec priam-backfill-teastore2 pip install -q requests
docker exec priam-backfill-teastore2 python //w/case-studies/TeaStore/priam-integration/backfill-data-subjects.py
```

Output: `user2: registered, 1 order(s) reported, Keycloak provisioned` — and
103/103 total, 0 failures.

**Real state proof**:

```bash
docker exec teastore-persistence-1 curl -s "http://localhost:8080/tools.descartes.teastore.persistence/rest/users/name/user2"
# -> {"id":601,"userName":"user2","realName":"Eric Moorse","email":"user2@teastore.com", ...}

curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d "grant_type=password&client_id=Data-client&username=user2@teastore.com&password=password"
# -> 200, real access token (decoded: idReference="user2")
```

## 10. Gateway OIDC — appendix of auth tests (as `Docs/PRIAM-AUTH-OIDC.md` requests)

No token:

```bash
curl -s -w "\nHTTP_CODE:%{http_code}\n" -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" -d '{"dataSubjectId":1,"dataRequestClaim":"should be 401","data":[{"dataId":1}]}'
# -> HTTP_CODE:401
```

Valid token:

```bash
TOK=$(curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d "grant_type=password&client_id=Data-client&username=selena.test2@example.com&password=TestPass456!" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
curl -s -w "\nHTTP_CODE:%{http_code}\n" "http://localhost:8090/right/api/right/dataRequest/2" \
  -H "Authorization: Bearer $TOK"
# -> HTTP_CODE:200, real DataRequest #2 JSON body
```

Invalid token:

```bash
curl -s -w "\nHTTP_CODE:%{http_code}\n" "http://localhost:8090/right/api/right/dataRequest/2" \
  -H "Authorization: Bearer invalid.token.value"
# -> HTTP_CODE:401
```

Machine-to-machine route, no token required, unaffected:

```bash
curl -s -w "\nHTTP_CODE:%{http_code}\n" \
  "http://localhost:8090/provider/api/dataAccessRight?idRef=user1&dataTypeName=User&attributes=userName"
# -> HTTP_CODE:200, [{"userName":"user1"}]
```

## 11. Real browser — handed off

PRIAM's frontends (`:4200`/`:4000`) were started and confirmed reachable
(`HTTP 200`) at the end of this session so the user could perform the final
visual pass themselves — no browser-automation tool was available in this
environment to drive one directly. Suggested check: log into
`http://localhost:4200` with `selena.test2@example.com` / `TestPass456!`,
confirm the `/consent` page shows "Product Recommendations" as the only
optional toggle, and that the Access Request page shows the real
`selena.test.subject2` data reported in §7/§9 above.
