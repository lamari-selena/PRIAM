# ÉTAPES FAITES — Bank of Anthos × PRIAM

Raw detail of every test actually run this session, with real state proof
(not just HTTP 200s). Reproduce identically with the commands below.

## Reference — URLs/ports actually used

| Component | URL | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | `/right`, `/cdp`, `/actor`, `/data`, `/provider` prefixes |
| PRIAM-Actor-service (direct) | `http://actor:8082` (container), not host-published | Reached directly by Bank of Anthos services on `common_network`, not through the Gateway |
| PRIAM-Consent-Service (direct) | `http://consent:8089` (container), also `http://localhost:8089` host-published by PRIAM's own compose | CDP/CIP |
| PRIAM-Data-service (direct) | `http://data:8081` (container), `http://localhost:8081` host-published | |
| PRIAM-Frontend | `http://localhost:4200` | Data-subject UI (Consent, Access Request, My Rights) |
| PRIAM-Frontend-Provider | `http://localhost:4000` | Data-controller dashboard |
| Keycloak | `http://localhost:8080`, realm `priam-realm` | `Data-client` (public, direct-grant enabled), `Provider-client` |
| Bank of Anthos frontend | `http://localhost:9000` | Browser entry point (login/signup/home/payment/deposit) |
| Bank of Anthos `userservice` (Provider bridge) | `http://userservice:8080` (container) = `CUSTOM_PROVIDER_URL`; `http://localhost:9001` host-published for ad hoc smoke curls only | bare `/api/*`, no auth |
| Bank of Anthos `accounts-db` | not host-published; `docker exec boa-accounts-db psql -U accounts-admin -d accounts-db` | real-state proof |
| PRIAM MySQL | not host-published under this compose name; `docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D <schema>` | real-state proof |

Test subject used for the full rights/consent cycle: **`priamqa5`**
(`data_subject_id=5` in PRIAM, non-numeric `idRef`, dynamically registered
through a real Bank of Anthos sign-up). Demo subjects `testuser`(1)/
`alice`(2)/`bob`(3)/`eve`(4) were seeded directly in
`Databases/db_insertion_script.sql` and later backfilled through the real
runtime path (see last section).

## 1. Standalone Bank of Anthos (PRIAM code present, PRIAM containers not running)

```bash
cd case-studies/BankOfAnthos
docker compose up -d
```

Logged in as `testuser`/`bankofanthos` via a real headless-Chromium session
(Playwright), navigated to `/home`: real balance ($6,641.46), real
transaction history, real account number rendered. Signed up a throwaway
user (`standalonetest`) — succeeded, landed on `/home` with $0.00 balance.
`docker logs boa-userservice` showed the 3 PRIAM hooks failing with
`NameResolutionError` (PRIAM containers not started yet), caught, logged as
`WARNING`, no impact on the HTTP response — confirms the fail-open design.

## 2. Sign-up + registration (real, with PRIAM running)

Browser sign-up as `priamqa5` (`http://localhost:9000/signup`, fields:
username, password, password-repeat, firstname, lastname, birthday — the
rest are pre-filled/readonly on this form).

Real state after sign-up:

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-actor" \
  -e "SELECT * FROM data_subject WHERE id_ref='priamqa5';"
# data_subject_id=5, id_ref=priamqa5, data_subject_category_id=1

docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=5 ORDER BY data_id;"
# data_id 1-10, nb_occurrences=1 each (the 10 annotated User columns)
```

Keycloak account:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/priam-realm/users?username=priamqa5"
# username=priamqa5, attributes.idReference=["priamqa5"], requiredActions=[]
```

Login token carries the claim:

```bash
curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d grant_type=password -d client_id=Data-client -d username=priamqa5 -d 'password=PriamTest123!'
# decode the access_token: {"idReference":"priamqa5", "preferred_username":"priamqa5", ...}
```

## 3. Consent redirect + grant (real browser)

Login → `home()` calls `has_pending_consent_decision("priamqa5","Contact Management")`
→ empty CIP list → redirect chain (all followed by the browser):
`localhost:9000/login` → `localhost:9000/home` → `localhost:4200/consent`
→ (PRIAM-Frontend's `APP_INITIALIZER` finds no Keycloak session) →
`localhost:8080/realms/priam-realm/.../auth?...redirect_uri=localhost:4200/consent`.

Logged into Keycloak (username/password form, same credentials) → landed on
`http://localhost:4200/consent?iss=...` — real PRIAM consent page rendered:
"Contact Management" (Optional, unchecked), "Account Management" (Necessary,
checked+disabled), "Back to the app" link visible (confirms `TARGET_APP_URL`
wiring).

Clicked the Contact Management toggle → network capture showed
`POST http://localhost:8090/cdp/api/consent/create/priamqa5` → `200`.

Real state:

```bash
curl -s "http://localhost:8089/api/contract/list/consents/priamqa5/Contact%20Management"
# [{"consentId":5,"startDate":"2026-07-22T01:21:26...","endDate":null,"contractId":5}]
```

## 4. Optional side effect gated by consent (deposit with a labeled contact)

With consent granted, submitted a deposit via the real UI
(`http://localhost:9000/home` → Deposit Funds → External account
`9099791699`/`808889588`, label `MyExternalBank`, $100.00).

```bash
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa5';"
# priamqa5 | MyExternalBank | 9099791699 | 808889588 | t

docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=5 AND data_id IN (11,12,13,14);"
# data_id 11-14, nb_occurrences=1 each — report_processed_data() fired correctly
```

Balance confirmed $100.00 on the home page screenshot.

## 5. Consent withdrawal (real browser)

Logged into `http://localhost:4200/consent` again (same session pattern),
clicked the Contact Management toggle off.

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-consent" \
  -e "SELECT * FROM consent WHERE contract_id=5;"
# consent_id=5, start_date=2026-07-22 01:21:26, end_date=2026-07-22 01:29:12 (real timestamp of the click)
```

`processed_data` rows 11-14 were checked again and found **unchanged**
(`nb_occurrences=1`, rows still present) — this is bug #7 in
`INTEGRATION-REPORT.md` (Feign `DELETE`+body silently dropped), isolated by
calling the endpoint directly:

```bash
curl -s -X DELETE "http://localhost:8081/api/processed-data/remove?subjectId=5" \
  -H "Content-Type: application/json" -d '[11,12,13,14]'
# "Processed data removed successfully." - and the rows ARE gone afterward,
# proving ProcessedDataService itself works; the bug is in how
# ConsentServiceImpl's Feign client calls it.
```

## 6. Rights workflow — get a bearer token first

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d grant_type=password -d client_id=Data-client -d username=priamqa5 -d 'password=PriamTest123!' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### 6a. Rectification — `answer=false` then `answer=true` (`User.firstname`)

```bash
# BEFORE
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa5';"
# priamqa5 | Priam

# 1. Create request
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"User","data":{"dataId":3},"newValue":"RectifiedFirstName","claim":"test rectification","primaryKeys":[]}'
# {"dataRequestId":1, ...}

# 2. Answer false
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused for test","dataRequestId":1,"data":[]}'
# {"dataRequestAnswerId":1,"answer":"REFUSED",...}

# 3. Verify unchanged
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa5';"
# priamqa5 | Priam   <- unchanged, confirmed

# 4. Create a second request, same field
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"User","data":{"dataId":3},"newValue":"RectifiedFirstName","claim":"test rectification 2","primaryKeys":[]}'
# {"dataRequestId":2, ...}

# 5. Answer true
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved for test","dataRequestId":2,"data":[]}'
# {"dataRequestAnswerId":2,"answer":"FULL",...}

# 6. Verify REAL change
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa5';"
# priamqa5 | RectifiedFirstName   <- changed, confirmed
```

### 6b. Erasure — `answer=false` then `answer=true` (`User.address`)

```bash
# BEFORE: priamqa5 | 123 Nth Avenue, New York City

curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure","primaryKeys":[]}'
# {"dataRequestId":3, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused","dataRequestId":3,"data":[]}'
# {"answer":"REFUSED",...}
# verify unchanged: priamqa5 | 123 Nth Avenue, New York City   <- confirmed

curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure 2","primaryKeys":[]}'
# {"dataRequestId":4, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":4,"data":[]}'
# {"answer":"FULL",...}
# verify: priamqa5 | (empty string)   <- blanked, confirmed
```

### 6c. Rectification on a multi-row type with composite primary key (`Contact.routing_num`)

```bash
# BEFORE
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa5';"
# priamqa5 | MyExternalBank | 9099791699 | 808889588 | t

curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"Contact","data":{"dataId":13},"newValue":"999999999","claim":"test contact rectification","primaryKeys":[{"primaryKeyId":11,"primaryKeyValue":"MyExternalBank"}]}'
# {"dataRequestId":5, "primaryKeys":{"11":"MyExternalBank"}, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":5,"data":[]}'
# {"answer":"FULL",...}

# verify ONLY this row changed
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa5';"
# priamqa5 | MyExternalBank | 9099791699 | 999999999 | t   <- routing_num changed, confirmed
```

### 6d. Access request (read) + access request bookkeeping

```bash
# Always-open read endpoint (not through the answer=true auto-execution mechanism)
curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=5&dataTypeName=User&attributes=username&attributes=firstname&attributes=lastname&attributes=ssn&attributes=address" \
  -H "Authorization: Bearer $TOKEN"
# [{"address":"","firstname":"RectifiedFirstName","lastname":"QaFive","ssn":"111-22-3333","username":"priamqa5"}]

curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=5&dataTypeName=Contact&attributes=label&attributes=account_num&attributes=routing_num" \
  -H "Authorization: Bearer $TOKEN"
# [{"account_num":"9099791699","label":"MyExternalBank","routing_num":"999999999"}]

# Bookkeeping (request + answer)
curl -s -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataRequestClaim":"test access request","data":[]}'
# {"dataRequestId":6, "requestType":"ACCESS", ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":6,"data":[]}'
# {"answer":"FULL",...}
```

Also verified in a real browser: logged in as `priamqa5`, navigated to
`http://localhost:4200/access-request` (through the same Keycloak session
flow as above) — the "User" Data List table rendered `priamqa5`,
`RectifiedFirstName`, `QaFive`, birthday, `NY`, `10004`, `-5`,
`111-22-3333`, address blank — matching every change made above, in a real
browser, for a non-numeric idRef.

### 6e. Provider dashboard (data controller side, real browser)

```bash
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":5,"dataTypeName":"User","data":{"dataId":4},"newValue":"ApprovedViaBrowser","claim":"browser test","primaryKeys":[]}'
# {"dataRequestId":7, ...}
```

Logged into `http://localhost:4000` as `app.owner`/`OwnerPass123!` (Keycloak)
→ Dashboard correctly listed `7 RECTIFICATION BoA Account Holder
22/07/2026 02:36:01`. Clicking into the row's detail page hit a client-side
`TypeError` in the automated (Playwright) test — not root-caused within this
session (see Known limitations in `INTEGRATION-REPORT.md`); the approval
mechanism itself is independently verified via 6a-6d above.

## 7. Backfill for pre-existing (SQL-seeded) demo users

```bash
docker cp priam-integration/backfill-data-subjects.py boa-userservice:/app/backfill-data-subjects.py
docker exec boa-userservice python /app/backfill-data-subjects.py
# INFO:backfill:Backfilling testuser / alice / bob / eve / ... (no warnings = all succeeded)
```

Verified no duplicate `data_subject` rows were created for the 4 seeded
users (idempotent upsert, `DataSubjectServiceImpl.saveDataSubject`):

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-actor" \
  -e "SELECT * FROM data_subject ORDER BY data_subject_id;"
# 1|16|testuser|1  2|16|alice|1  3|16|bob|1  4|16|eve|1  5..9|NULL|priamqa*/standalonetest|1
```

Verified Keycloak provisioning for a demo user:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/priam-realm/users?username=testuser"
# username=testuser, attributes.idReference=["testuser"], firstName=Test, lastName=User
```
