# PRIAM ↔ Habitica — Raw test log (ÉTAPES FAITES)

Every test actually run this session, in order, with the real request, the
real HTTP response, and the real database state checked afterward (§7 of
`Docs/PRIAM-INTEGRATION-PLAYBOOK.md` — a `200`/`FULL` proves nothing by
itself). See `INTEGRATION-REPORT.md` for the summary/mechanism/LOC breakdown.

## 0. Reference — URLs/ports actually used in this integration

| Component | URL | Notes |
|---|---|---|
| PRIAM Gateway | `http://localhost:8090` | Single entry point for every PRIAM API below |
| Gateway → Rights | `http://localhost:8090/right/api/...` | `PRIAM-Right-service` (§3) |
| Gateway → Consent | `http://localhost:8090/cdp/api/...` | `PRIAM-Consent-Service` (CEP/CIP, §4) |
| Gateway → Actor | `http://localhost:8090/actor/api/...` | `PRIAM-Actor-service` (data_subject) |
| Gateway → Data | `http://localhost:8090/data/api/...` | `PRIAM-Data-service` (processed_data, access lists) |
| Gateway → Provider | `http://localhost:8090/provider/api/...` | Strips `/provider`, forwards to `CUSTOM_PROVIDER_URL` |
| Habitica Provider bridge (direct) | `http://localhost:3000/api/...` | `case-studies/Habitica/website/server/controllers/top-level/priamProvider.js` — reachable directly on the host because `server`'s `3000:3000` is published; PRIAM itself reaches it over `common_network` at `http://server:3000` |
| Habitica app (API) | `http://localhost:3000` | `docker-compose.yml` service `server` |
| Habitica app (client/browser) | `http://localhost:5173` | `docker-compose.yml` service `client` (Vite dev server) |
| Habitica Mongo | `mongodb://localhost:27017/habitrpg` | `docker-compose.yml` service `mongo`, container `habitica-mongodb` |
| PRIAM MySQL | `localhost:3308` (mapped from container port 3306) | schemas `priam-actor`/`priam-data`/`priam-consent`/`priam-right` |
| PRIAM-Frontend (data subject UI) | `http://localhost:4200` | `/consent`, `/access-request`, home (with "Back to the app" link) |
| PRIAM-Frontend-Provider (data controller UI) | `http://localhost:4000` | Pending rights requests dashboard |
| Keycloak | `http://localhost:8080` | realm `priam-realm`, admin `admin`/`admin` |

Seed test subject: `idRef = b411d9e2-81cb-4f2d-825b-ce8502be2ae7` (Habitica
`User._id`, a real UUID — non-numeric by construction, §7), username
`priam-seed-subject`, email `priam-seed-subject@example.com`, password
`PriamSeed123!`, created via the real `POST /api/v4/user/auth/local/register`
(not inserted directly). `data_subject_id = 1` in PRIAM.

## 1. Environment setup / prerequisites hit before any PRIAM test

- **Docker Hub DNS (§8.9)**: `docker pull mongo:7.0`/`node:20` intermittently
  failed with `dial tcp: lookup registry-1.docker.io: no such host` even
  though `%USERPROFILE%\.wslconfig` already had `networkingMode=mirrored`/
  `dnsTunneling=true` from a prior session. Fix applied: `wsl --shutdown`,
  wait ~20s for Docker Desktop to reattach, then retry the pull in a loop
  (succeeded on the 1st-3rd retry every time, never required more).
- **No `config.json` existed** in a fresh checkout (only `config.json.example`,
  gitignored via `config*.json`) — `website/client/vite.config.mjs:48`
  (`nconf.get('BASE_URL').indexOf(...)`) crashes the Docker build
  (`TypeError: Cannot read properties of undefined (reading 'indexOf')`)
  without it. Fix: `cp config.json.example config.json` before the first
  build — a target-application prerequisite, not a PRIAM bug (§7).
- **Stale containers/images from a prior BankOfAnthos session** were present
  under the OLD project name `priam-bankofanthos`. Since the root
  `docker-compose.yml`'s `name:` was changed to `priam-habitica` for this
  session, `docker compose down` (scoped by project name) found nothing to
  remove — the old containers had to be force-removed by their fixed
  `container_name` values instead (§5 pitfall: project-identity changes
  orphan previously-created containers, not just the reverse case the
  playbook already documents).
- MySQL data volume (`db-volume/`) cleared before rebuilding `mysqldb`, since
  `db_insertion_script.sql` only runs on a virgin volume (§5).

## 2. SQL annotation — two real bugs caught by MySQL itself on first load

| # | Error | Fix |
|---|---|---|
| 1 | `ERROR 1406 (22001) at line 76: Data too long for column 'secondary_actor_name'` — `'Apple APNs / Google Firebase Cloud Messaging'` (44 chars) into a `varchar(40)` column. | Shortened to `'Apple APNs / Google FCM'` (23 chars). |
| 2 | (caught proactively before load, not a runtime error) `purpose_description` for the Push Notifications purpose was 199/200 chars — trimmed for safety margin. | Shortened the sentence, dropped the trailing file citation. |

After the fix, `docker logs priam-databases` showed the full `db_creation_script.sql` then `db_insertion_script.sql` run to completion with no further errors, and the healthcheck (`mysqladmin ping`) went `starting` → `healthy`.

## 3. Seeding the real test subject (before finalizing the SQL script)

```bash
# 1. Register the seed subject through the REAL endpoint (not inserted directly)
curl -X POST http://localhost:3000/api/v4/user/auth/local/register \
  -H "Content-Type: application/json" -H "x-client: habitica-web" \
  -d '{"username":"priam-seed-subject","email":"priam-seed-subject@example.com","password":"PriamSeed123!","confirmPassword":"PriamSeed123!"}'
# -> real _id captured via direct Mongo query (registration response only echoes it in
#    the full body, but here confirmed via mongosh): b411d9e2-81cb-4f2d-825b-ce8502be2ae7
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({'auth.local.username':'priam-seed-subject'},{_id:1,tasksOrder:1,apiToken:1}))"
# {"_id":"b411d9e2-81cb-4f2d-825b-ce8502be2ae7","tasksOrder":{"habits":[],"dailys":[],"todos":[],"rewards":[]},"apiToken":"f2390ab9-148d-490f-aaa1-0a260b0acefc"}
```

**Note on `tasksOrder` being empty**: `server/models/user/hooks.js:87`
(`if (user.registeredThrough === 'habitica-web') return Promise.all(tasksToCreate);`)
— the real `habitica-web` client (the `x-client` header used above) creates
**no** default tasks at sign-up time ("`@TODO: default tasks are handled
differently now, and not during registration`", a comment already in the
app's own code) — only the mobile clients get seeded defaults inline. This
turned out to be convenient: it meant `report_processed_data()` for Task
fields is exercised exclusively through the normal task-creation path
(`libs/tasks/index.js`), with no special-casing needed for a signup-time
default that this app doesn't actually create.

```bash
# 2. Create 2 real tasks (to get real Task._id values for the SQL script + §8.1.c test)
USERID="b411d9e2-81cb-4f2d-825b-ce8502be2ae7"; TOKEN="f2390ab9-148d-490f-aaa1-0a260b0acefc"
curl -X POST http://localhost:3000/api/v4/tasks/user -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN" \
  -d '{"text":"Read GDPR playbook","type":"todo","notes":"Initial seed task"}'
# -> _id: 18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0
curl -X POST http://localhost:3000/api/v4/tasks/user -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN" \
  -d '{"text":"Drink water","type":"habit","notes":"Stay hydrated","up":true,"down":false}'
# -> _id: 7492a4d5-dff9-4e9d-aeaf-0eb2f83546e4

# 3. Attempt to register a push device BEFORE PRIAM is running
curl -X POST http://localhost:3000/api/v3/user/push-devices -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN" \
  -d '{"regId":"priam-seed-device-token-0001","type":"android"}'
# -> {"success":true,"data":[],...}  <- fail-CLOSED: getConsent() could not reach
#    consent:8089 (PRIAM stack not started yet), caught the network error, returned
#    false. Real proof: Mongo pushDevices stays [] (checked directly, see next line).
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({_id:'$USERID'},{pushDevices:1}))"
# {"_id":"...","pushDevices":[]}   <- confirms nothing was written
```

This first push-device attempt is itself a real, useful proof: **PRIAM
configured but unreachable → fail-closed (deny)**, exactly per §4's
documented contract, before PRIAM was even started.

One push device was then seeded **directly in MongoDB** (not through the
API, which would have failed-closed again) to give the pre-granted OPTIONAL
consent in the SQL script a real record to describe:
```js
db.users.updateOne({_id:'b411d9e2-81cb-4f2d-825b-ce8502be2ae7'},
  {$push:{pushDevices:{regId:'priam-seed-device-token-0001', type:'android'}}})
```

## 4. A real bug found and fixed in my own code — `logger.warn is not a function`

First real registration attempt after wiring `server/libs/priam.js` crashed
the background PRIAM chain:
```
TypeError: _logger.default.warn is not a function
    at registerDataSubject (/usr/src/habitica/website/server/libs/priam.js:54:12)
```
Root cause: `server/libs/logger.js`'s exported interface
(`loggerInterface`, line 156-237) only implements `.info()` and `.error()` —
**no `.warn()`** (its internal winston instance has one, but it is not part
of the public interface the rest of the app is meant to use). `priam.js`
called `logger.warn(...)` in 4 places, copying a convention from other
projects that doesn't exist here. Fixed: all 4 call sites changed to
`logger.error(err, 'message')` (or `logger.info(msg, {data})` for the one
non-Error case), matching every other file in this codebase. Verified: the
exact same registration request, re-run after the fix, produced a clean
`fetch failed`/`ENOTFOUND keycloak` **`error`**-level log entry (Keycloak
wasn't up yet at that point) with no crash and no unhandled rejection.

**Second real bug**, found while smoke-testing the Provider bridge directly
(before wiring PRIAM's real calls to it):
```
TypeError: Cannot read properties of undefined (reading 'find')
    at loadRecords (priamProvider.js:44:30)
```
Root cause: `server/models/task.js` exports the model as `export const Task
= mongoose.model('Task', TaskSchema);` (a **named** export `Task`), not
`export const model = ...` like `server/models/user/index.js` does. My
`import { model as Task } from '../../models/task';` silently bound
`Task` to `undefined` (no such export) instead of erroring at import time.
Fixed: `import { Task } from '../../models/task';`. Verified below (§5).

Both bugs required a `docker compose restart server` to take effect — this
app's `node --watch` did **not** pick up the file change from the Windows
bind mount on its own (the same class of pitfall §8.9 already documents for
Vite/`webpack-dev-server`, now also observed on plain `node --watch`).

## 5. Provider bridge — direct smoke tests (before going through PRIAM-Right-service)

```bash
IDREF=b411d9e2-81cb-4f2d-825b-ce8502be2ae7
curl "http://localhost:3000/api/dataAccessRight?idRef=$IDREF&dataTypeName=User&attributes=username,email,displayName"
# [{"username":"priam-seed-subject","email":"priam-seed-subject@example.com","displayName":"priam-seed-subject"}]
curl "http://localhost:3000/api/dataAccessRight?idRef=$IDREF&dataTypeName=Task&attributes=id,text,notes"
# (after the Task-import fix) [{"id":"18fb55c9-...","text":"Read GDPR playbook","notes":"Initial seed task"},{"id":"7492a4d5-...","text":"Drink water","notes":"Stay hydrated"}]
curl "http://localhost:3000/api/dataAccessRight?idRef=$IDREF&dataTypeName=PushDevice&attributes=regId,type"
# [{"regId":"priam-seed-device-token-0001","type":"android"}]
curl -X POST http://localhost:3000/api/dataValue -H "Content-Type: application/json" \
  -d "{\"idRef\":\"$IDREF\",\"dataName\":\"username\",\"primaryKeys\":{}}"
# {"value":"priam-seed-subject"}
curl -X POST http://localhost:3000/api/dataValue -H "Content-Type: application/json" \
  -d "{\"idRef\":\"$IDREF\",\"dataName\":\"text\",\"primaryKeys\":{\"id\":\"18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0\"}}"
# {"value":"Read GDPR playbook"}
```
All 4 Provider endpoints (`dataAccessRight`, `dataValue` here; `rectification`/
`erasure` exercised indirectly in §6-7 through the real workflow) confirmed
working directly against real Mongo data, always returning a JSON array for
`dataAccessRight` (§2) as required.

## 6. Authentication through the Gateway

The seed subject had no Keycloak account yet (its automatic provisioning
attempt in §4's first registration had failed with `ENOTFOUND keycloak`,
since Keycloak wasn't started at that point). Rather than leaving a
mismatched state, a Keycloak account was provisioned **manually via the
Admin API, reproducing byte-for-byte what `provision_keycloak_user()` does**
(same username=email pattern, same firstName/lastName=email, same
`idReference` attribute) — used only for this one already-registered
subject; every subsequent fresh registration (§8-9) exercises the real
automatic path with no manual step.

```bash
ADMIN_TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" | jq -r .access_token)
curl -X POST http://localhost:8080/admin/realms/priam-realm/users \
  -H "Content-Type: application/json" -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{"username":"priam-seed-subject@example.com","email":"priam-seed-subject@example.com",
       "enabled":true,"emailVerified":true,
       "firstName":"priam-seed-subject@example.com","lastName":"priam-seed-subject@example.com",
       "credentials":[{"type":"password","value":"PriamSeed123!","temporary":false}],
       "attributes":{"idReference":["b411d9e2-81cb-4f2d-825b-ce8502be2ae7"]}}'
# HTTP 201

USER_TOKEN=$(curl -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=Data-client&username=priam-seed-subject@example.com&password=PriamSeed123!" \
  | jq -r .access_token)
# Decoded payload includes: "idReference": "b411d9e2-81cb-4f2d-825b-ce8502be2ae7"

# No token -> 401
curl -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" -d '{}'
# 401
```

## 7. Rights workflow through PRIAM-Right-service (§3) — real DB proof at every step

### 7.1 Access request

```bash
TOKEN=$USER_TOKEN
# Request 1
curl -X POST http://localhost:8090/right/api/right/accessRequest \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"give me my data","data":[{"dataId":1},{"dataId":2},{"dataId":3}]}'
# -> {"dataRequestId":1, ...}
# answer=false
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataRequestId":1,"answer":false,"providerClaim":"refused for test","data":[]}'
# -> {"dataRequestAnswerId":1,"answer":"REFUSED","dataRequestClaim":"refused for test"}
# Real DB proof:
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-right \
  -e "SELECT * FROM data_request_answer WHERE data_request_id=1;"
# data_request_answer_id=1, answer=REFUSED, data_request_id=1  <- confirmed recorded

# Request 2, answer=true
curl -X POST http://localhost:8090/right/api/right/accessRequest -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataRequestClaim":"give me my data take 2","data":[{"dataId":1},{"dataId":2},{"dataId":3}]}'
# -> {"dataRequestId":2, ...}
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataRequestId":2,"answer":true,"providerClaim":"approved","data":[{"dataId":1},{"dataId":2},{"dataId":3}]}'
# -> {"dataRequestAnswerId":2,"answer":"FULL","dataRequestClaim":"approved"}

# Real read via the always-open endpoint (Provider bridge, live data)
curl "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=1&dataTypeName=User&attributes=username&attributes=email&attributes=displayName" \
  -H "Authorization: Bearer $TOKEN"
# -> [{"username":"priam-seed-subject","email":"priam-seed-subject@example.com","displayName":"priam-seed-subject"}]
```
**Result**: `answer=false` → `REFUSED` recorded, no further action.
`answer=true` → `FULL` recorded, and the always-open read endpoint confirms
live data flowing correctly from Habitica's real MongoDB through the
Provider bridge.

### 7.2 Rectification — `Task.text`, composite primary key (§8.1.c scenario)

```bash
# BEFORE
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.tasks.findOne({_id:'18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0'},{text:1}))"
# {"_id":"18fb55c9-...","text":"Read GDPR playbook"}

# Request 1 (dataId=5=Task.text, primaryKeyId=4=Task.id)
curl -X POST http://localhost:8090/right/api/right/rectificationRequest -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataTypeName":"Task","data":{"dataId":5},"newValue":"Read GDPR playbook (rectified v1)","claim":"typo fix","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0"}]}'
# -> {"dataRequestId":3, ...}
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataRequestId":3,"answer":false,"providerClaim":"refused","data":[]}'
# -> REFUSED

# AFTER answer=false — real Mongo state UNCHANGED
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.tasks.findOne({_id:'18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0'},{text:1}))"
# {"_id":"18fb55c9-...","text":"Read GDPR playbook"}   <- confirmed unchanged

# Request 2, answer=true
curl -X POST http://localhost:8090/right/api/right/rectificationRequest -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataTypeName":"Task","data":{"dataId":5},"newValue":"Read GDPR playbook (rectified)","claim":"typo fix v2","primaryKeys":[{"primaryKeyId":4,"primaryKeyValue":"18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0"}]}'
# -> {"dataRequestId":4, ...}
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataRequestId":4,"answer":true,"providerClaim":"approved","data":[]}'
# -> FULL (this triggers PRIAM's real auto-execution of POST /api/rectification on the Provider bridge)

# AFTER answer=true — real Mongo state CHANGED
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.tasks.findOne({_id:'18fb55c9-b2b6-4eb6-a6b5-0b84ebbd0dc0'},{text:1}))"
# {"_id":"18fb55c9-...","text":"Read GDPR playbook (rectified)"}   <- confirmed changed
```
Confirms the `primaryKeys` disambiguation actually selects the **right**
Task row among the subject's several tasks (the 2nd task, `text: "Drink
water"`, was never touched — separately verified untouched throughout).

### 7.3 Erasure — `User.displayName`, single-row type (no primaryKeys)

```bash
# BEFORE
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({_id:'b411d9e2-81cb-4f2d-825b-ce8502be2ae7'},{'profile.name':1}))"
# {"_id":"...","profile":{"name":"priam-seed-subject"}}

# Request 1, answer=false
curl -X POST http://localhost:8090/right/api/right/erasureRequest -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":3},"claim":"erase my display name","primaryKeys":[]}'
# -> {"dataRequestId":5, ...}
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d '{"dataRequestId":5,"answer":false,"providerClaim":"refused","data":[]}'
# -> REFUSED
# AFTER: unchanged (confirmed by direct query, same as before)

# Request 2, answer=true
curl -X POST http://localhost:8090/right/api/right/erasureRequest -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":3},"claim":"erase my display name v2","primaryKeys":[]}'
# -> {"dataRequestId":6, ...}
curl -X POST http://localhost:8090/right/api/right/answer -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" -d '{"dataRequestId":6,"answer":true,"providerClaim":"approved","data":[]}'
# -> FULL

# AFTER answer=true
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({_id:'b411d9e2-81cb-4f2d-825b-ce8502be2ae7'},{'profile.name':1}))"
# {"_id":"...","profile":{"name":""}}   <- confirmed blanked
```

## 8. Consent workflow (§4) — grant (pre-seeded) / withdraw / re-grant, real proof

The seed subject's OPTIONAL "Push Notifications" consent started
pre-granted directly in `db_insertion_script.sql` (§1 point 9). This section
tests the full withdraw → re-grant cycle through the **real** PRIAM endpoint,
plus verifies the CEP correctly gates the actual optional side effect.

```bash
# Current state via the CIP
curl "http://localhost:8090/cdp/api/contract/list/consents/b411d9e2-81cb-4f2d-825b-ce8502be2ae7/Push%20Notifications" \
  -H "Authorization: Bearer $TOKEN"
# [{"consentId":1,"startDate":"2026-07-22T23:51:21...","endDate":null,...}]  <- granted (from SQL seed)

# Real optional side effect while granted: add a 2nd push device through the ACTUAL app API
curl -X POST http://localhost:3000/api/v3/user/push-devices -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN_API" -d '{"regId":"priam-seed-device-token-0002","type":"ios"}'
# -> succeeds, 2 devices now in Mongo (confirmed via direct query)
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-data \
  -e "SELECT * FROM processed_data WHERE data_id IN (7,8);"
# nb_occurrences=2 for both (was 1 before this device add)   <- report_processed_data() confirmed working

# WITHDRAW (2nd call to the toggle closes the open consent)
curl -X POST "http://localhost:8090/cdp/api/consent/create/b411d9e2-81cb-4f2d-825b-ce8502be2ae7" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"processingId":"Push Notifications"}'
# -> {"consentId":1,"startDate":"...","endDate":"2026-07-23T00:07:59...",...}
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-consent \
  -e "SELECT * FROM consent WHERE processing_id=4;"
# consent_id=1, end_date=2026-07-23 00:08:00   <- confirmed closed
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-data \
  -e "SELECT * FROM processed_data WHERE data_id IN (7,8);"
# nb_occurrences back to 1  <- PRIAM's own withdrawal bookkeeping (ConsentServiceImpl.removeProcessedData)

# Attempt to add a 3rd device WHILE WITHDRAWN
curl -X POST http://localhost:3000/api/v3/user/push-devices -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN_API" -d '{"regId":"priam-seed-device-token-0003","type":"android"}'
# -> {"success":true,"data":[...only the existing 2 devices...],"message":"Push device added successfully"}
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({_id:'$USERID'},{pushDevices:1}).pushDevices.length)"
# 2   <- CONFIRMED BLOCKED: no 3rd device actually written, despite the "success" message
#        (the app's own pre-existing silent-no-op convention for duplicates was reused, see §4)

# RE-GRANT (3rd call creates a new, open consent row)
curl -X POST "http://localhost:8090/cdp/api/consent/create/b411d9e2-81cb-4f2d-825b-ce8502be2ae7" \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" -d '{"processingId":"Push Notifications"}'
# -> {"consentId":2,"startDate":"...","endDate":null,...}

# Retry adding the 3rd device
curl -X POST http://localhost:3000/api/v3/user/push-devices -H "Content-Type: application/json" \
  -H "x-api-user: $USERID" -H "x-api-key: $TOKEN_API" -d '{"regId":"priam-seed-device-token-0003","type":"android"}'
# -> succeeds this time
docker exec habitica-mongodb mongosh habitrpg --quiet --eval \
  "JSON.stringify(db.users.findOne({_id:'$USERID'},{pushDevices:1}).pushDevices.length)"
# 3   <- confirmed written
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-data \
  -e "SELECT * FROM processed_data WHERE data_id IN (7,8);"
# nb_occurrences=3 for both   <- confirmed bookkeeping restored and incremented
```

**Full cycle verified with real state at every transition**: granted (proof:
side effect happens + bookkeeping increments) → withdrawn (proof: side
effect blocked, real Mongo state unchanged, bookkeeping decrements) →
re-granted (proof: side effect works again, bookkeeping increments again).

## 9. Fresh registration — full automatic chain, no manual steps

A brand-new user (`priam-fresh-test`/`priam-fresh-test@example.com`),
registered **after** the whole PRIAM stack (including Keycloak) was already
running, to prove the entire `register_data_subject` →
`report_processed_data` → `provision_keycloak_user` chain works
automatically end-to-end, in the correct order (§4bis/§8.6 race).

```bash
curl -X POST http://localhost:3000/api/v4/user/auth/local/register -H "Content-Type: application/json" \
  -H "x-client: habitica-web" \
  -d '{"username":"priam-fresh-test","email":"priam-fresh-test@example.com","password":"FreshTest123!","confirmPassword":"FreshTest123!"}'
# -> 201, real _id: 51a569bb-5d95-47d1-b4e0-1fbca5b45eec

docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-actor \
  -e "SELECT * FROM data_subject WHERE id_ref='51a569bb-5d95-47d1-b4e0-1fbca5b45eec';"
# data_subject_id=2, id_ref=51a569bb-...   <- confirmed created automatically

docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-data \
  -e "SELECT * FROM processed_data WHERE data_subject_id=2;"
# data_id 1,2,3 each nb_occurrences=1   <- confirmed reported automatically (User fields)

ADMIN_TOKEN=$(curl -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" | jq -r .access_token)
curl "http://localhost:8080/admin/realms/priam-realm/users?username=priam-fresh-test@example.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# -> real Keycloak account, attributes.idReference = ["51a569bb-5d95-47d1-b4e0-1fbca5b45eec"]
#    <- confirmed automatic Keycloak provisioning, correct claim, no manual step

# Real login with the synced password
curl -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=Data-client&username=priam-fresh-test@example.com&password=FreshTest123!"
# -> HTTP 200, real access token   <- confirmed NOT "Account is not fully set up" (§4bis pitfall avoided:
#    firstName/lastName supplied, username=email long enough)

# priamConsentRequired flag: true before any decision
curl http://localhost:3000/api/v4/user -H "x-api-user: 51a569bb-5d95-47d1-b4e0-1fbca5b45eec" -H "x-api-key: <apiToken>"
# data.priamConsentRequired: true

# Grant the OPTIONAL processing through the real endpoint, then re-check
curl -X POST "http://localhost:8090/cdp/api/consent/create/51a569bb-5d95-47d1-b4e0-1fbca5b45eec" \
  -H "Content-Type: application/json" -H "Authorization: Bearer <fresh-user-token>" \
  -d '{"processingId":"Push Notifications"}'
curl http://localhost:3000/api/v4/user -H "x-api-user: 51a569bb-5d95-47d1-b4e0-1fbca5b45eec" -H "x-api-key: <apiToken>"
# data.priamConsentRequired: false   <- confirmed flag flips, no redirect loop possible afterward
```

No PRIAM-related error appeared in `docker logs habitica-server-1` around
this registration (checked explicitly, `grep -i priam`) — the whole
sequential chain (`await registerDataSubject` before any `reportProcessedData`
call, per the §8.6 ordering fix already applied in `priam.js`) completed
without a race, on the very first try.

## 10. Backfill script — genuinely pre-existing user (not created via the app's own hooks)

To realistically exercise `priam-integration/backfill-data-subjects.mjs`, a
user was inserted **directly into MongoDB**, bypassing the application
entirely (simulating an account that existed before this integration was
wired up):
```js
db.users.insertOne({_id:'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee', auth:{local:{username:'priam-legacy-user', ...}}, profile:{name:'priam-legacy-user'}, tasksOrder:{habits:[],dailys:[],todos:[],rewards:[]}, pushDevices:[]})
db.tasks.insertOne({_id:'ffffffff-1111-4222-8333-444444444444', userId:'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee', type:'todo', text:'Legacy task', notes:''})
```
```bash
docker exec -e NODE_DB_URI=mongodb://mongo/habitrpg -e PRIAM_ACTOR_URL=http://actor:8082 \
  -e PRIAM_DATA_URL=http://data:8081 habitica-server-1 \
  node /usr/src/habitica/priam-integration/backfill-data-subjects.mjs
# Backfilled b411d9e2-81cb-4f2d-825b-ce8502be2ae7 (2 tasks, 3 push devices)
# Backfilled 9b286f3f-321a-4b88-beb2-5814068a5cdb (0 tasks, 0 push devices)
# Backfilled 51a569bb-5d95-47d1-b4e0-1fbca5b45eec (0 tasks, 0 push devices)
# Backfilled aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee (1 tasks, 0 push devices)
# Done: 4 user(s) backfilled.

docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-actor \
  -e "SELECT * FROM data_subject WHERE id_ref='aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee';"
# data_subject_id=4   <- confirmed created

docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-data \
  -e "SELECT * FROM processed_data WHERE data_subject_id=4;"
# data_id 1,2,3,4,5,6 each nb_occurrences=1   <- confirmed User + Task fields reported

# Idempotency: re-run the exact same script a second time
docker exec ... node /usr/src/habitica/priam-integration/backfill-data-subjects.mjs
docker exec priam-databases mysql -upriamu -pMaiRP_pWd-UsEr -D priam-actor \
  -e "SELECT COUNT(*) FROM data_subject WHERE id_ref='aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee';"
# 1   <- confirmed NOT duplicated (Actor-service's upsert-by-idRef, per playbook §4bis)
```

## 11. Real browser test — handed off to the user

A containerized Playwright test (`priam-integration/browser-test.mjs`,
written this session) was attempted via `docker pull
mcr.microsoft.com/playwright:v1.48.0-jammy`, since this sandbox has no
browser or Playwright installed locally. The pull repeatedly stalled on the
first layer (same class of Docker Hub DNS instability documented in
playbook §8.9), well past the point of reasonable retries. The user chose
to run this step manually instead of continuing to fight the image pull.

**Left ready for a manual pass**, all services still running:
- Habitica: `http://localhost:5173` — a fresh, consent-undecided account
  `priam-browser-test` / `priam-browser-test@example.com` /
  `BrowserTest123!` (registered via `POST /api/v4/user/auth/local/register`
  this session, never given a consent decision — `priamConsentRequired`
  should read `true` and the forced redirect should fire on the next route
  change per `client/src/router/index.js`'s `afterEach` hook).
- Same credentials work on Keycloak (`http://localhost:8080`, realm
  `priam-realm`) — auto-provisioned by `provisionKeycloakUser()` at
  registration time, confirmed via the Admin API in §9.
- PRIAM-Frontend: `http://localhost:4200` (`/consent`, access-request pages).
- PRIAM-Frontend-Provider: `http://localhost:4000` (pending-request
  dashboard — the rectification/erasure/access requests created in §7 above
  should be visible here as a real data controller would see them).
- Habitica Settings → Site Data should show the new "Manage on PRIAM" row
  pointing at `http://localhost:4200`.


