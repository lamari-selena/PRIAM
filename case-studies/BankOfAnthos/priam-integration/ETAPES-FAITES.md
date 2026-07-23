# ÉTAPES FAITES — Bank of Anthos × PRIAM

Raw detail of every test actually run **this session** (2026-07-22), with real
state proof (not just HTTP 200s). Reproduce identically with the commands
below.

**Context**: this integration was previously built and committed to this
repository (`git log` shows a single commit already containing a full working
integration). At the start of this session the working tree had that
integration reverted out of `case-studies/BankOfAnthos/` and the root PRIAM
stack switched to a different case study (Ghostfolio, running). This session:
restored the BankOfAnthos-specific files from that commit, re-verified every
piece of code against the real current schema/routes, switched the running
Docker stack back to Bank of Anthos, and then ran every test below fresh
against real, live containers — not a replay of the old document. Two real
gaps were found and fixed in the process (§0 below); everything else was
independently re-verified with new timestamps/screenshots.

## 0. Gaps found and fixed this session (see INTEGRATION-REPORT.md §3 for full detail)

1. **Missing "Manage on PRIAM" link** — the restored code had never actually
   wired a link into any Bank of Anthos template. Added to
   `src/frontend/templates/shared/navigation.html` (account dropdown) +
   `src/frontend/frontend.py` (`priam_frontend_url` passed to `home()`'s
   `render_template`).
2. **Generic PRIAM bug**: `PRIAM-Frontend`'s consent page never rendered
   `MANDATORY` processings (only `NECESSARY`), because
   `consent.component.ts`'s `necessaryList` filter checked only
   `ProcessingType.NECESSARY`. Fixed on PRIAM's side (also documented in
   `Docs/PRIAM-INTEGRATION-PLAYBOOK.md` §8.1.d).
3. **Missing `local-jwt/` RSA key pair** — gitignored (`*.key`), never
   present on disk; regenerated with the exact command from Bank of Anthos's
   own `docs/development.md`.

## Reference — URLs/ports actually used

| Component | URL | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | `/right`, `/cdp`, `/actor`, `/data`, `/provider` prefixes |
| PRIAM-Actor-service (direct) | `http://actor:8082` (container); `http://localhost:8082` also host-published | Reached directly by Bank of Anthos services on `common_network`, not through the Gateway |
| PRIAM-Consent-Service (direct) | `http://consent:8089` (container); `http://localhost:8089` host-published | CDP/CIP |
| PRIAM-Data-service (direct) | `http://data:8081` (container); `http://localhost:8081` host-published | |
| PRIAM-Frontend | `http://localhost:4200` | Data-subject UI (Consent, Access Request, My Rights) |
| PRIAM-Frontend-Provider | `http://localhost:4000` | Data-controller dashboard |
| Keycloak | `http://localhost:8080`, realm `priam-realm` | `Data-client` (public, direct-grant enabled), `Provider-client` |
| Bank of Anthos frontend | `http://localhost:9000` | Browser entry point (login/signup/home/payment/deposit) |
| Bank of Anthos `userservice` (Provider bridge) | `http://userservice:8080` (container) = `CUSTOM_PROVIDER_URL`; `http://localhost:9001` host-published for ad hoc smoke curls only | bare `/api/*`, no auth |
| Bank of Anthos `accounts-db` | not host-published; `docker exec boa-accounts-db psql -U accounts-admin -d accounts-db` | real-state proof |
| PRIAM MySQL | not host-published under this compose project; `docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D <schema>` | real-state proof |

Test subject used for the full rights/consent cycle this session:
**`priamqa7`** (`data_subject_id=12` in PRIAM, non-numeric `idRef`,
dynamically registered through a real Bank of Anthos sign-up in this
session). Demo subjects `testuser`(1)/`alice`(2)/`bob`(3)/`eve`(4) were seeded
directly in `Databases/db_insertion_script.sql`; subjects 5-11
(`priamqa5`, `standalonetest`, `priamqa2-4`, `ww`, `alice789`) are real
leftover subjects from the original integration session, still present in
the reused MySQL/Postgres volumes.

## 1. Docker stack brought up this session

```bash
# Root PRIAM stack switched from the Ghostfolio project (priam-ghostfolio,
# running) back to Bank of Anthos:
docker stop <priam-ghostfolio containers> && docker rm <fixed-name ones>
# docker-compose.yml (root) restored to name: priam-bankofanthos,
# ./db-volume (already containing this exact case study's annotated data
# from the original session, dated the same day)
docker compose build mysqldb frontuser   # frontuser rebuilt twice: once for
                                          # the new TARGET_APP_URL, once more
                                          # after the consent.component.ts fix
docker compose up -d mysqldb eureka actor consent data right provider gateway keycloak frontuser frontprovider

cd case-studies/BankOfAnthos
docker compose build frontend            # rebuilt for the navigation.html/frontend.py change
docker compose up -d accounts-db ledger-db
docker compose up -d userservice contacts ledgerwriter balancereader transactionhistory
docker compose up -d frontend
```

All services reached `healthy`/`ok` on `/ready`. No rebuild was needed for
`userservice`, `contacts`, `ledgerwriter`, `balancereader`, `transactionhistory`,
or the fixed-tag PRIAM microservices — their images (and the MySQL/Postgres
data volumes) were already built from this exact codebase in an earlier
session on this machine.

## 2. Sign-up + registration (real browser, Playwright/Chromium)

```python
page.goto("http://localhost:9000/signup")
page.fill("#signup-username", "priamqa7")
page.fill("#signup-password", "PriamTest123!")
page.fill("#signup-password-repeat", "PriamTest123!")
page.fill("#signup-firstname", "Priam")
page.fill("#signup-lastname", "QaSeven")
page.fill("#signup-birthday", "1990-01-01")
page.click("#signup-form button[type=submit]")
```

Result: `http://localhost:9000/home`. Real state after sign-up:

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-actor" \
  -e "SELECT * FROM data_subject WHERE id_ref='priamqa7';"
# data_subject_id=12, id_ref=priamqa7, data_subject_category_id=1

docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=12 AND data_id<=10 ORDER BY data_id;"
# data_id 1-10, nb_occurrences=1 each (the 10 annotated User columns, reported
# by report_processed_data() in userservice.py's background thread)
```

Keycloak account + token claim:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d grant_type=password -d client_id=admin-cli -d username=admin -d password=admin \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/priam-realm/users?username=priamqa7"
# username=priamqa7, attributes.idReference=["priamqa7"], requiredActions=[]

curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d grant_type=password -d client_id=Data-client -d username=priamqa7 -d 'password=PriamTest123!'
# decoded access_token: {"idReference":"priamqa7","preferred_username":"priamqa7",...}
```

## 3. Forced consent redirect (real browser)

Navigating to `/home` immediately after sign-up (`has_pending_consent_decision`
finds an empty CIP list) redirected through:
`localhost:9000/home` → `localhost:8080/realms/priam-realm/.../auth?...redirect_uri=localhost:4200/consent`
→ (after Keycloak login) → `http://localhost:4200/consent?iss=...`.

Screenshot (`4_final_consent_page.png`, before the §0.2 fix): "Contact
Management" (Optional, unchecked) and only "Account Management" under
"Necessary processing" — **"Identity Verification" (`MANDATORY`) missing**.
After the fix + `frontuser` rebuild, re-tested the identical flow
(`5_consent_after_fix.png`): both "Account Management" and "Identity
Verification" now render, correctly pre-checked/disabled.

## 4. Consent grant (real browser, non-numeric idRef)

```python
page.goto("http://localhost:9000/login")
page.fill("#login-username", "priamqa7")
page.fill("#login-password", "PriamTest123!")
page.click("#login-form button[type=submit]")
# -> redirected to Keycloak -> consent page
page.locator(".title:has-text('Optional processing') + table mat-slide-toggle").first.click()
```

Network capture: `GET /cdp/api/contract/list/consents/priamqa7/{1,2,3,4}` (200
each) then `POST http://localhost:8090/cdp/api/consent/create/priamqa7` → `200`.

Real state:

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-consent" \
  -e "SELECT c.contract_id, co.consent_id, co.processing_id, co.start_date, co.end_date FROM contract c JOIN consent co ON co.contract_id=c.contract_id WHERE c.data_subject_id=12;"
# contract_id=7, consent_id=7, processing_id=4 (Contact Management),
# start_date=2026-07-22 21:29:42, end_date=NULL
```

## 5. Bidirectional navigation (real browser)

- Clicked "Back to the app" (PRIAM-Frontend navbar, visible on the consent
  page) → landed on `http://localhost:9000/home` (a real, working page — not
  the bare root — confirming `TARGET_APP_URL=http://localhost:9000/home`).
- On that page, opened the account dropdown (`#accountDropdown`) → found
  **"Manage on PRIAM"** (`href="http://localhost:4200"`, `target="_blank"`),
  above "Sign out" (screenshot `8_account_dropdown.png`).
  `page.locator("text=Manage on PRIAM").count()` → `1`.

## 6. Optional side effect gated by consent (deposit with a labeled contact)

With consent granted, submitted a deposit via the real UI: Deposit Funds →
"New External Account" → account `9099791111` / routing `808889111`, label
`PriamTestBank`, amount `$50.00`.

```bash
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa7';"
# priamqa7 | PriamTestBank | 9099791111 | 808889111 | t

docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=12 AND data_id IN (11,12,13,14);"
# data_id 11-14, nb_occurrences=2 each
```

**Note on `nb_occurrences=2`** (not 1): investigated and fully explained, not
a bug. Two independent, legitimate mechanisms both report the same
`Contact Management` data_ids for the same subject: (a) `contacts.py`'s own
`report_processed_data()` at contact-creation time (playbook §4bis), and (b)
`PRIAM-Consent-Service`'s own `ConsentServiceImpl.create()`, which
**independently** calls `addProcessedData`/`removeProcessedData` for every
data_id tied to the processing being granted/withdrawn (`processingRestClient
.getDataIds(processingId)`) — confirmed by reading
`ConsentServiceImpl.java:121,136,155`. Both fired once each for this subject's
one grant + one contact creation, giving 2. See §7 below for direct proof
this decrements correctly.

## 7. Consent withdrawal → re-grant → withdrawal (real browser, full cycle)

```python
page.goto("http://localhost:4200/consent")
# (Keycloak session already active)
page.locator(".title:has-text('Optional processing') + table mat-slide-toggle").first.click()
```

**Withdrawal 1:**
```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-consent" \
  -e "SELECT * FROM consent WHERE contract_id=7 ORDER BY consent_id;"
# consent_id=7, end_date=2026-07-22 21:38:18 (real timestamp of the click)

docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=12 AND data_id IN (11,12,13,14);"
# nb_occurrences DECREMENTED 2 -> 1 for all 4 rows (removeProcessedData worked)
```

**Re-grant** (same toggle click again):
```bash
# consent_id=8, start_date=2026-07-22 21:38:56, end_date=NULL (new row created,
# Case 1b in ConsentServiceImpl.create())
```

**Withdrawal 2:**
```bash
# consent_id=8, end_date=2026-07-22 21:39:26
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-data" \
  -e "SELECT * FROM processed_data WHERE data_subject_id=12 AND data_id IN (11,12,13,14);"
# nb_occurrences DECREMENTED 2 -> 1 again (consistent, correct behavior both times)
```

This directly contradicts the previously-documented "bug #7" (Feign
`DELETE`+body silently dropped) from the original session's report — that bug
does **not** reproduce in this checkout; `removeProcessedData` decremented
correctly on both withdrawals tested. Either it was already fixed generically
on PRIAM's side after that report was written, or the original diagnosis was
incomplete. Not re-added to the playbook as a live pitfall, since it does not
currently reproduce.

## 8. Rights workflow — get a bearer token first

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d grant_type=password -d client_id=Data-client -d username=priamqa7 -d 'password=PriamTest123!' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['access_token'])")
```

### 8a. Rectification — `answer=false` then `answer=true` (`User.firstname`)

```bash
# BEFORE
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa7';"
# priamqa7 | Priam

curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":12,"dataTypeName":"User","data":{"dataId":3},"newValue":"RectifiedFirstName","claim":"test rectification","primaryKeys":[]}'
# {"dataRequestId":8, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused for test","dataRequestId":8,"data":[]}'
# {"dataRequestAnswerId":7,"answer":"REFUSED",...}

# verify unchanged
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa7';"
# priamqa7 | Priam   <- unchanged, confirmed

curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":12,"dataTypeName":"User","data":{"dataId":3},"newValue":"RectifiedFirstName","claim":"test rectification 2","primaryKeys":[]}'
# {"dataRequestId":9, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved for test","dataRequestId":9,"data":[]}'
# {"dataRequestAnswerId":8,"answer":"FULL",...}

# verify REAL change
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT username, firstname FROM users WHERE username='priamqa7';"
# priamqa7 | RectifiedFirstName   <- changed, confirmed
```

### 8b. Erasure — `answer=false` then `answer=true` (`User.address`)

```bash
# BEFORE: priamqa7 | 123 Nth Avenue, New York City

curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":12,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure","primaryKeys":[]}'
# {"dataRequestId":10, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused","dataRequestId":10,"data":[]}'
# {"answer":"REFUSED",...}
# verify unchanged: priamqa7 | 123 Nth Avenue, New York City   <- confirmed

curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":12,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure 2","primaryKeys":[]}'
# {"dataRequestId":11, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":11,"data":[]}'
# {"answer":"FULL",...}
# verify: priamqa7 | (empty string)   <- blanked, confirmed
```

### 8c. Rectification on `Contact` (composite key `label`)

```bash
# BEFORE
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa7';"
# priamqa7 | PriamTestBank | 9099791111 | 808889111 | t

curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":12,"dataTypeName":"Contact","data":{"dataId":13},"newValue":"999999999","claim":"test contact rectification","primaryKeys":[{"primaryKeyId":11,"primaryKeyValue":"PriamTestBank"}]}'
# {"dataRequestId":12, "primaryKeys":{"11":"PriamTestBank"}, ...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":12,"data":[]}'
# {"answer":"FULL",...}

# verify ONLY this row changed
docker exec boa-accounts-db psql -U accounts-admin -d accounts-db \
  -c "SELECT * FROM contacts WHERE username='priamqa7';"
# priamqa7 | PriamTestBank | 9099791111 | 999999999 | t   <- routing_num changed, confirmed
```

### 8d. Access request (read)

```bash
curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=12&dataTypeName=User&attributes=username&attributes=firstname&attributes=address&attributes=ssn" \
  -H "Authorization: Bearer $TOKEN"
# [{"address":"","firstname":"RectifiedFirstName","ssn":"111-22-3333","username":"priamqa7"}]

curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=12&dataTypeName=Contact&attributes=label&attributes=account_num&attributes=routing_num" \
  -H "Authorization: Bearer $TOKEN"
# [{"account_num":"9099791111","label":"PriamTestBank","routing_num":"999999999"}]
```

Both reflect every rectification/erasure performed above.

### 8e. `dataValue` (Provider bridge's 4th endpoint, §8.2.f) — direct smoke test

```bash
curl -s -X POST http://localhost:9001/api/dataValue -H "Content-Type: application/json" \
  -d '{"idRef":"priamqa7","dataName":"lastname","primaryKeys":{}}'
# {"value":"QaSeven"}   <- User field, type inferred from dataName (no dataTypeName in body)

curl -s -X POST http://localhost:9001/api/dataValue -H "Content-Type: application/json" \
  -d '{"idRef":"priamqa7","dataName":"account_num","primaryKeys":{"label":"PriamTestBank"}}'
# {"value":"9099791111"}   <- Contact field, type inferred from primaryKeys presence
```

### 8f. Provider dashboard (data controller side, real browser)

```bash
# Logged into http://localhost:4000 as app.owner / OwnerPass123! (Keycloak)
```

Dashboard correctly listed a real pending request: `7 RECTIFICATION BoA
Account Holder 22/07/2026 02:36:01` — a genuine leftover unanswered request
from the original integration session (same text as documented in the
now-superseded report), persisted in the reused MySQL volume. Clicking the
notification did not navigate anywhere (no JS error thrown this time, unlike
the original session's report of a `TypeError`) — the element does not appear
to be a clickable row in this build. Not chased further: the approval
mechanism itself is independently and thoroughly verified via 8a-8c above.

## 9. Backfill for pre-existing users

```bash
docker cp priam-integration/backfill-data-subjects.py boa-userservice:/app/backfill-data-subjects.py
docker exec boa-userservice python /app/backfill-data-subjects.py
# INFO:backfill:Backfilling testuser / alice / bob / eve / standalonetest /
#   priamqa2 / priamqa3 / priamqa4 / priamqa5 / ww / alice789 / priamqa7
# (no warnings = all succeeded; script backfills every row currently in
# `users`, not just the 4 originally-seeded demo accounts, since this
# database also holds real accounts from the original session)
```

Verified no duplicate `data_subject` rows (idempotent upsert):

```bash
docker exec priam-databases mysql -u priamu -p'MaiRP_pWd-UsEr' -D "priam-actor" \
  -e "SELECT data_subject_id, id_ref FROM data_subject ORDER BY data_subject_id;"
# 1..13, one row each, testuser=1 unchanged despite being backfilled twice
# (once in the original session's own backfill run, once again here)
```

Verified Keycloak provisioning for a demo user that predates the sign-up hook:

```bash
curl -s -H "Authorization: Bearer $TOKEN" \
  "http://localhost:8080/admin/realms/priam-realm/users?username=testuser"
# username=testuser, attributes.idReference=["testuser"], firstName=Test,
# lastName=User, requiredActions=[]
```
