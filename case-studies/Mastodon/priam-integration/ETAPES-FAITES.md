# PRIAM ↔ Mastodon — Detailed test log (ÉTAPES FAITES)

Every command below was actually run against a real local Docker stack
during this session (not reconstructed from memory) — real command, real
response, real database state. Reproduce identically by running the
commands as-is after bringing up the stack (§0).

## 0. Reference — URLs/ports actually used

| Component | URL | Notes |
|---|---|---|
| PRIAM-Gateway | `http://localhost:8090` | `/right`, `/cdp`, `/actor`, `/data`, `/provider` prefixes |
| — Rights | `http://localhost:8090/right/api/right/...` | requires Bearer token (Keycloak) |
| — Consent (CDP/CIP) | `http://localhost:8090/cdp/api/...` | requires Bearer token |
| — Actor (machine-to-machine) | `http://localhost:8090/actor/api/...` | always open, no auth |
| — Data (machine-to-machine) | `http://localhost:8090/data/api/...` | always open, no auth |
| — Provider (machine-to-machine) | `http://localhost:8090/provider/api/...` | always open, no auth, strips `/provider` then forwards to `CUSTOM_PROVIDER_URL` |
| Mastodon Provider bridge (this case study) | `http://localhost:3000/api/{dataAccessRight,rectification,erasure,dataValue}` | bare `/api`, no auth — reached directly here for smoke tests, reached via the Gateway's `/provider/**` route for the real §3 workflow |
| Mastodon web (Puma) | `http://localhost:3000` | `X-Forwarded-Proto: https` header required on any non-GET or force_ssl-covered request from curl (no TLS terminator in this local stack) |
| Mastodon streaming | `http://localhost:4001` | remapped from the upstream default 4000 — collided with PRIAM-Frontend-Provider's host port 4000 |
| Keycloak | `http://localhost:8080` | realm `priam-realm`; admin `admin`/`admin` |
| PRIAM-Frontend (data subject UI) | `http://localhost:4200` | `/consent`, `/accessRequest`, etc. |
| PRIAM-Frontend-Provider (data controller UI) | `http://localhost:4000` | |
| PRIAM MySQL | `docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb` | root password from `Databases/Dockerfile` |
| Mastodon Postgres | `docker compose exec db psql -U postgres -d mastodon_production` | trust auth, no password |

Seed subject: `priam_seed` / `priam-seed-test@gmail.com` / `PriamSeed!2026`
(`data_subject_id=1`, pre-seeded via `Databases/db_insertion_script.sql`
for the `User` fields only — see that file's header comment). Second test
subject used only for the backfill test: `priam_backfill_test` /
`priam-backfill-test@gmail.com` / `BackfillTest!2026`.

## 1. Bringing up the stack (this session's exact sequence)

```bash
# Wiped case-study-switch scratch volume (was Habitica's data before this session)
rm -rf db-volume && mkdir -p db-volume
docker rm priam-actor-ms priam-consent-ms priam-data-ms priam-eureka priam-right-ms \
  priam-provider-ms priam-api-gateway priam-frontend priam-frontend-provider priam-keycloak priam-databases

cd case-studies/Mastodon
docker compose up -d db redis

cd ../..
COMPOSE_BAKE=false docker compose build mysqldb
docker compose up -d mysqldb   # loads Databases/db_insertion_script.sql on the virgin volume

COMPOSE_BAKE=false docker compose build eureka
docker compose up -d --build actor consent data right provider gateway keycloak frontuser frontprovider

cd case-studies/Mastodon
cp .env.production.sample .env.production   # then filled in manually, see below
docker compose run --rm web bin/rails db:encryption:init   # generate ACTIVE_RECORD_ENCRYPTION_* secrets, add to .env.production
docker compose run --rm web bin/rails db:setup             # creates + migrates mastodon_production
docker compose up -d web sidekiq streaming
```

`.env.production` filled manually with: `LOCAL_DOMAIN=localhost:3000`,
`DB_HOST=db`, `DB_USER=postgres`, `DB_NAME=mastodon_production`,
`REDIS_HOST=redis`, a random `SECRET_KEY_BASE` (`openssl rand -hex 64`),
the 3 `ACTIVE_RECORD_ENCRYPTION_*` secrets from the command above,
`ES_ENABLED=false`, `S3_ENABLED=false`, no SMTP configured (see §2 for how
sign-up email confirmation was handled without a real mailer).

**Environment note**: two Docker build failures were hit and retried
during this session, both matching the already-documented playbook §8.9
pitfall ("Unstable Docker Desktop DNS behind a VPN... dial tcp: lookup
<host>: no such host") — `codeload.github.com` (FFmpeg source download)
failed intermittently mid-build; a plain retry of the same `docker compose
build` succeeded both times, no code or config change involved.

## 2. Seed registration (real API sign-up, not manually inserted)

Registrations were closed by default on a fresh instance
(`Setting.registrations_mode == "none"`) — opened for this test session:

```bash
docker compose exec web bin/rails runner "Setting.registrations_mode = 'open'"
```

```bash
# 1. Create an OAuth app
curl -s -X POST http://localhost:3000/api/v1/apps -H "X-Forwarded-Proto: https" \
  -d client_name=priam-seed-script -d redirect_uris=urn:ietf:wg:oauth:2.0:oob \
  -d scopes="read write" -d website=http://localhost:3000
# -> {"client_id":"lyOgOuIrtl8MZH4l7UomSROPPfO2G8KE3QoVqfPSmKg","client_secret":"JC36cwpe01DYVQA8Var-QL2xRFW8kkF3BBA2RQ_dkWk",...}

# 2. Get an app-level token (client_credentials)
curl -s -X POST http://localhost:3000/oauth/token -H "X-Forwarded-Proto: https" \
  -d client_id=lyOgOuIrtl8MZH4l7UomSROPPfO2G8KE3QoVqfPSmKg \
  -d client_secret=JC36cwpe01DYVQA8Var-QL2xRFW8kkF3BBA2RQ_dkWk \
  -d grant_type=client_credentials -d scope="read write"
# -> {"access_token":"fmwyTFFJnc4VadNyh7JUstzxizVGC6ajCGgF1Rr1Vu8",...}

# 3. Register the account — username chosen in advance ("priam_seed", non-numeric,
#    matches Account::USERNAME_RE), a domain with real MX records used for
#    the email (MX validation is enforced in production env; example.com has
#    none and fails with ERR_UNREACHABLE, confirmed by a failed first attempt)
curl -s -X POST http://localhost:3000/api/v1/accounts -H "X-Forwarded-Proto: https" \
  -H "Authorization: Bearer fmwyTFFJnc4VadNyh7JUstzxizVGC6ajCGgF1Rr1Vu8" \
  -d username=priam_seed -d email=priam-seed-test@gmail.com -d password='PriamSeed!2026' \
  -d agreement=true -d locale=en -d reason="PRIAM integration seed account"
# -> {"access_token":"c3T7iEFRXuU76_HvJ-fuT6QzIVQ5ApJg96QU_V9mkiY",...}
```

```bash
# No SMTP configured -> confirm/approve directly (legitimate substitute for
# clicking a confirmation email link, not part of the PRIAM mechanism itself)
docker compose exec web bin/rails runner \
  "u = User.find_by(email: 'priam-seed-test@gmail.com'); u.confirm; u.approve!"
```

**Real proof — `PriamRegisterSubjectWorker` ran automatically** (`docker
logs mastodon-sidekiq-1`):
```
INFO ... class=PriamRegisterSubjectWorker: start
WARN ... [Priam] report_processed_data(priam_seed) failed: Net::ReadTimeout with #<TCPSocket:(closed)>
INFO ... class=PriamRegisterSubjectWorker elapsed=5.597: done
```
The `Net::ReadTimeout` (3s timeout at the time) was a cold-start false
negative, not a real failure — confirmed by direct MySQL query
immediately after:
```bash
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-actor\`.data_subject; SELECT * FROM \`priam-data\`.processed_data WHERE data_subject_id=1;"
# data_subject_id=1, id_ref='priam_seed' (unchanged - idempotent upsert onto the pre-seeded row)
# processed_data: data_id 1-6, nb_occurrences=2 each (was 1 from the SQL seed, incremented
#   by the live report_processed_data call that DID complete server-side despite the
#   client-side timeout - the write is asynchronous from the client's perspective)
```
`TIMEOUT_SECONDS` was widened 3s → 8s in `app/lib/priam.rb` after this
observation (a real, environment-driven latency under Docker Desktop's
Windows virtualization, not a logic bug) — confirmed by re-running
`Priam.provision_keycloak_user` afterward with no timeout warning (§3).

## 3. Keycloak provisioning (bug #3 in the report — found, then fixed)

```bash
# First attempt (BEFORE the AppSignUpService fix) - silently did nothing,
# since priam_seed was registered via POST /api/v1/accounts (AppSignUpService),
# which did not call provision_keycloak_user at the time.
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/realms/master/protocol/openid-connect/token \
  -d "grant_type=password&client_id=admin-cli&username=admin&password=admin" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['access_token'])")
curl -s "http://localhost:8080/admin/realms/priam-realm/users?username=priam-seed-test@gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# -> []   <- confirmed missing, root-caused to AppSignUpService not calling provision_keycloak_user

# Fixed AppSignUpService#create_user! (report §3 bug #3), rebuilt, then manually
# replayed provisioning for the already-existing seed account:
docker compose exec web bin/rails runner \
  "Priam.provision_keycloak_user('priam_seed', 'priam-seed-test@gmail.com', 'PriamSeed!2026')"

curl -s "http://localhost:8080/admin/realms/priam-realm/users?username=priam-seed-test@gmail.com" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
# -> [{"id":"65fed2e4-...","username":"priam-seed-test@gmail.com","firstName":"priam-seed-test@gmail.com",
#      "lastName":"priam-seed-test@gmail.com","email":"priam-seed-test@gmail.com","emailVerified":true,
#      "attributes":{"idReference":["priam_seed"]},"enabled":true,...}]
#   <- correct idReference, confirmed the realm's declarative User Profile
#      (already baked into Keycloak/priam-realm-realm.json) does NOT silently
#      drop the custom attribute (the playbook §4bis pitfall this project
#      already fixed generically) - no PRIAM-side change needed.

# Real Direct Grant login with the synced password:
curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d "grant_type=password&client_id=Data-client&username=priam-seed-test@gmail.com&password=PriamSeed!2026"
# -> 200, real access_token; decoded JWT payload includes "idReference":"priam_seed"
```

## 4. Gateway auth (401 / 200)

```bash
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8090/right/api/right/accessRequest \
  -X POST -H "Content-Type: application/json" -d '{}'
# -> HTTP 401   (no token)

TOKEN=$(curl -s -X POST http://localhost:8080/realms/priam-realm/protocol/openid-connect/token \
  -d "grant_type=password&client_id=Data-client&username=priam-seed-test@gmail.com&password=PriamSeed!2026" \
  | python3 -c "import json,sys;print(json.load(sys.stdin)['access_token'])")
curl -s -o /dev/null -w "HTTP %{http_code}\n" http://localhost:8090/actor/api/DataSubjectId/priam_seed \
  -H "Authorization: Bearer $TOKEN"
# -> HTTP 200   (real token; /actor is machine-to-machine/always-open anyway, but confirms the token itself is valid)
```

## 5. Rights workflow — rectification (`User.locale`, `data_id=3`)

`answer=false` first (§3's non-negotiable "not a shortcut"):

```bash
# BEFORE
docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT locale FROM users WHERE email='priam-seed-test@gmail.com';"
# -> en

curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":3},"newValue":"fr","claim":"test rectification refused","primaryKeys":[]}'
# -> {"dataRequestId":1,...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused for test","dataRequestId":1,"data":[]}'
# -> {"dataRequestAnswerId":1,"answer":"REFUSED",...}

# verify unchanged
docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT locale FROM users WHERE email='priam-seed-test@gmail.com';"
# -> en   <- unchanged, confirmed
```

`answer=true` (first attempt hit bugs #1 and #2 from the report — 500s,
fixed, then retried):

```bash
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":3},"newValue":"fr","claim":"test rectification 3","primaryKeys":[]}'
# -> {"dataRequestId":3,...}

curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved for test","dataRequestId":3,"data":[]}'
# -> {"dataRequestAnswerId":2,"answer":"FULL","dataRequestClaim":"approved for test"}

docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT locale FROM users WHERE email='priam-seed-test@gmail.com';"
# -> fr   <- REAL change, confirmed
```

## 6. Rights workflow — erasure (`User.note`, `data_id=6`)

A real value was set first directly via `update_column` (Doorkeeper's
`password` grant type is disabled in this Mastodon version, so there was
no scriptable way to call `update_credentials` as the resource owner
without a full browser OAuth dance — this is unrelated to the PRIAM
mechanism itself, purely test-data setup):

```bash
docker compose exec web bin/rails runner \
  "Account.find_by(username: 'priam_seed').update_column(:note, 'Hello from priam_seed, this is my real bio.')"
```

```bash
# answer=false
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure refused","primaryKeys":[]}'
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":false,"providerClaim":"refused","dataRequestId":4,"data":[]}'
docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT note FROM accounts WHERE username='priam_seed';"
# -> "Hello from priam_seed, this is my real bio."   <- unchanged, confirmed

# answer=true
curl -s -X POST http://localhost:8090/right/api/right/erasureRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"User","data":{"dataId":6},"claim":"test erasure approved","primaryKeys":[]}'
# -> {"dataRequestId":5,...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":5,"data":[]}'
# -> {"dataRequestAnswerId":4,"answer":"FULL",...}
docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT note FROM accounts WHERE username='priam_seed';"
# -> "" (empty)   <- REAL erasure, confirmed
```

## 7. Status creation → `report_processed_data` → rectification with `primaryKeys`

```bash
# Mint a real Doorkeeper user token via rails console (Doorkeeper's password
# grant is disabled in this Mastodon version - this is the standard
# equivalent used by this app's own test suites)
docker compose exec web bin/rails runner "
u = User.find_by(email: 'priam-seed-test@gmail.com')
app = Doorkeeper::Application.find_by(name: 'priam-seed-script')
t = Doorkeeper::AccessToken.create!(application: app, resource_owner_id: u.id, scopes: 'read write', expires_in: 7200, use_refresh_token: false)
puts t.token"
# -> yXqUgvgnK3AuTPzNnFNLcfLI5sUxoLnH62n_J39UHXg

curl -s -X POST http://localhost:3000/api/v1/statuses -H "X-Forwarded-Proto: https" \
  -H "Authorization: Bearer yXqUgvgnK3AuTPzNnFNLcfLI5sUxoLnH62n_J39UHXg" \
  -d status="Hello from priam_seed, my first real toot!" -d visibility=public
# -> {"id":"116968805474706761","language":"fr",...}   <- note: language inherited "fr" from
#    the rectification in §5, incidental confirmation that rectification really took effect app-wide

docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-data\`.processed_data WHERE data_subject_id=1 AND data_id IN (7,8,9,10);"
# -> all 4 rows, nb_occurrences=1   <- report_processed_data fired correctly at Status creation,
#    with NO manual intervention (app/services/post_status_service.rb hook)
```

Rectification with `primaryKeys` (§8.1.c scenario):

```bash
curl -s -X POST http://localhost:8090/right/api/right/rectificationRequest \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"dataSubjectId":1,"dataTypeName":"Status","data":{"dataId":8},"newValue":"Edited via PRIAM rectification test","claim":"test status rectification","primaryKeys":[{"primaryKeyId":7,"primaryKeyValue":"116968805474706761"}]}'
# -> {"dataRequestId":6,"primaryKeys":{"7":"116968805474706761"},...}
curl -s -X POST http://localhost:8090/right/api/right/answer \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"answer":true,"providerClaim":"approved","dataRequestId":6,"data":[]}'
# -> {"dataRequestAnswerId":5,"answer":"FULL",...}
docker compose exec db psql -U postgres -d mastodon_production \
  -c "SELECT id, text FROM statuses WHERE account_id=(SELECT id FROM accounts WHERE username='priam_seed');"
# -> 116968805474706761 | Edited via PRIAM rectification test   <- only this row changed, confirmed
```

## 8. Access request (read)

```bash
curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=1&dataTypeName=User&attributes=email&attributes=locale&attributes=display_name&attributes=note&attributes=sign_up_ip&attributes=time_zone" \
  -H "Authorization: Bearer $TOKEN"
# -> [{"email":"priam-seed-test@gmail.com","locale":"fr","display_name":"","note":"","sign_up_ip":"192.168.128.1","time_zone":null}]
#    <- reflects both prior real changes (locale=fr, note erased)

curl -s "http://localhost:8090/right/api/personalDataValues/accessRight?dataSubjectId=1&dataTypeName=Status&attributes=id&attributes=text&attributes=spoiler_text&attributes=language" \
  -H "Authorization: Bearer $TOKEN"
# -> [{"id":"116968805474706761","text":"Edited via PRIAM rectification test","spoiler_text":"","language":"fr"}]
#    <- reflects the Status rectification from §7
```

## 9. Consent workflow — Push Notifications (fail-closed → grant → withdraw → re-grant)

A real browser session was simulated with a cookie jar + CSRF token
(Mastodon's `Api::Web::BaseController` enforces `protect_from_forgery
with: :exception`, unlike the machine-to-machine Provider bridge):

```bash
COOKIES=/tmp/mastodon_cookies.txt
SIGNIN_HTML=$(curl -s -c "$COOKIES" -H "X-Forwarded-Proto: https" http://localhost:3000/auth/sign_in)
CSRF=$(echo "$SIGNIN_HTML" | grep -o 'name="csrf-token" content="[^"]*"' | sed 's/.*content="//;s/"$//')
curl -s -c "$COOKIES" -b "$COOKIES" -H "X-Forwarded-Proto: https" -X POST http://localhost:3000/auth/sign_in \
  --data-urlencode "user[email]=priam-seed-test@gmail.com" \
  --data-urlencode "user[password]=PriamSeed!2026" \
  --data-urlencode "authenticity_token=$CSRF" -D - -o /dev/null | grep -E "^(HTTP|location)"
# -> HTTP/1.1 302 Found; location: https://localhost:3000/
```

**Forced-consent redirect** — confirmed firing correctly on the real home
route for this still-undecided subject:

```bash
curl -s -b "$COOKIES" -c "$COOKIES" -H "X-Forwarded-Proto: https" -D - -o /dev/null http://localhost:3000/
# -> HTTP/1.1 302 Found; location: http://localhost:4200/consent
```

Fetch a fresh CSRF token from a non-redirecting page, then attempt a
subscription **before** any consent decision exists (fail-closed proof):

```bash
CSRF=$(curl -s -b "$COOKIES" -c "$COOKIES" -H "X-Forwarded-Proto: https" http://localhost:3000/auth/edit \
  | grep -o 'name="csrf-token" content="[^"]*"' | sed 's/.*content="//;s/"$//')

curl -s -b "$COOKIES" -H "X-Forwarded-Proto: https" -H "X-CSRF-Token: $CSRF" -H "Content-Type: application/json" \
  -X POST http://localhost:3000/api/web/push_subscriptions \
  -d '{"subscription":{"endpoint":"https://fcm.googleapis.com/fcm/send/priam-test-endpoint-1","keys":{"p256dh":"BNbxGYNMhEAxNVfoZo8YCX9CDdymSKQeSRMpN1nOTVsWjeYWa6cB2hVv3JMFdF6cJRLnA8ZzWpz1EDe6EXAMPLE1","auth":"tBHItJI5svbpez7KI4CCXg"}},"data":{"policy":"all"}}'
# -> {"error":"Consent required for Push Notifications"}
docker compose exec db psql -U postgres -d mastodon_production -c "SELECT count(*) FROM web_push_subscriptions;"
# -> 0   <- fail-closed confirmed
```

Check the Consent Information Point (should be empty — no decision yet),
then grant via the real PRIAM consent endpoint:

```bash
curl -s "http://localhost:8090/cdp/api/contract/list/consents/priam_seed/Push%20Notifications" \
  -H "Authorization: Bearer $TOKEN"
# -> []

curl -s -X POST "http://localhost:8090/cdp/api/consent/create/priam_seed" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"processingId":"Push Notifications"}'
# -> {"consentId":1,"startDate":"2026-07-23T10:28:09.291+00:00","endDate":null,"contractId":1}
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-consent\`.consent WHERE processing_id=4;"
# -> consent_id=1, start_date=2026-07-23 10:28:09, end_date=NULL, processing_id=4, contract_id=1
```

Create the subscription now that consent is granted — needs a real
P-256/Curve25519 public key, generated for this test only:

```bash
openssl ecparam -name prime256v1 -genkey -noout -out webpush_key.pem
openssl ec -in webpush_key.pem -text -noout   # extract the raw 65-byte uncompressed public point,
                                               # base64url-encode it (p256dh) and 16 random bytes (auth)
# P256DH=BIWyVRPgRWjw-sUuwFRxji1mq59faQ36SgoR3tUR-tDOfaNHdGc7F67fjGDrgNCZWmGesYLX9UTJHo2ejM9oIzY
# AUTH=wv2z_c-SIcw3eYqYpjv8TQ

curl -s -b "$COOKIES" -H "X-Forwarded-Proto: https" -H "X-CSRF-Token: $CSRF" -H "Content-Type: application/json" \
  -X POST http://localhost:3000/api/web/push_subscriptions \
  -d '{"subscription":{"endpoint":"https://fcm.googleapis.com/fcm/send/priam-test-endpoint-1","keys":{"p256dh":"BIWyVRPgRWjw-sUuwFRxji1mq59faQ36SgoR3tUR-tDOfaNHdGc7F67fjGDrgNCZWmGesYLX9UTJHo2ejM9oIzY","auth":"wv2z_c-SIcw3eYqYpjv8TQ"}},"data":{"policy":"all"}}'
# -> {"id":1,"endpoint":"https://fcm.googleapis.com/fcm/send/priam-test-endpoint-1",...}

docker compose exec db psql -U postgres -d mastodon_production -c "SELECT id, user_id, endpoint FROM web_push_subscriptions;"
# -> 1 row: id=1, user_id=1, real endpoint
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-data\`.processed_data WHERE data_subject_id=1 AND data_id IN (11,12,13,14);"
# -> nb_occurrences=2 for each (1 from PRIAM's own consent-grant bookkeeping + 1 from Mastodon's
#    explicit report_processed_data call - confirmed harmless double-bookkeeping, see report §3)
```

Withdraw consent, confirm real decrement + `get_consent()` deny, confirm
the CEP blocks a further resubscribe attempt:

```bash
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/priam_seed" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"processingId":"Push Notifications"}'
# -> {"consentId":1,"startDate":"2026-07-23T10:28:09.000+00:00","endDate":"2026-07-23T10:29:38.673+00:00",...}
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-data\`.processed_data WHERE data_subject_id=1 AND data_id IN (11,12,13,14);"
# -> nb_occurrences DECREMENTED 2 -> 1 for all 4 rows
curl -s "http://localhost:8090/cdp/api/decision/Push%20Notifications?idRefList=priam_seed" -H "Authorization: Bearer $TOKEN"
# -> {"priam_seed":false}

# Attempt another subscription while withdrawn (endpoint #2) - this is the
# exact sequence that originally exposed bug #4 (consent-loss) before the fix:
curl -s -b "$COOKIES" -H "X-Forwarded-Proto: https" -H "X-CSRF-Token: $CSRF" -H "Content-Type: application/json" \
  -X POST http://localhost:3000/api/web/push_subscriptions \
  -d '{"subscription":{"endpoint":"https://fcm.googleapis.com/fcm/send/priam-test-endpoint-2",...}}'
# -> {"error":"Consent required for Push Notifications"}
docker compose exec db psql -U postgres -d mastodon_production -c "SELECT count(*) FROM web_push_subscriptions;"
# -> 0   <- BEFORE the bug #4 fix, this was 0 even though row #1 should have survived a mere
#    denial (it did not - destroy_previous_subscriptions had already deleted it). AFTER the
#    fix (rebuild), re-running the exact same sequence with a fresh subscription first:
#    BEFORE denied attempt: 1 row (id=2, endpoint-3). AFTER denied attempt: still 1 row,
#    same id, same endpoint - confirmed fixed, see report §3 bug #4 for the full before/after.
```

Re-grant (final state left granted, `consentId=3`):

```bash
curl -s -X POST "http://localhost:8090/cdp/api/consent/create/priam_seed" \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" -d '{"processingId":"Push Notifications"}'
# -> {"consentId":3,"startDate":"2026-07-23T10:36:14.215+00:00","endDate":null,"contractId":1}
```

## 10. `dataValue` (4th Provider endpoint, §8.2.f) — direct smoke test

```bash
curl -s -X POST http://localhost:3000/api/dataValue -H "X-Forwarded-Proto: https" -H "Content-Type: application/json" \
  -d '{"idRef":"priam_seed","dataName":"locale","primaryKeys":{}}'
# -> {"value":"fr"}   (User, no dataTypeName, inferred from dataName)

curl -s -X POST http://localhost:3000/api/dataValue -H "X-Forwarded-Proto: https" -H "Content-Type: application/json" \
  -d '{"idRef":"priam_seed","dataName":"text","primaryKeys":{"id":"116968805474706761"}}'
# -> {"value":"Edited via PRIAM rectification test"}   (Status, inferred + primaryKeys.id)

curl -s -X POST http://localhost:3000/api/dataValue -H "X-Forwarded-Proto: https" -H "Content-Type: application/json" \
  -d '{"idRef":"priam_seed","dataName":"endpoint","primaryKeys":{"subscriptionId":"2"}}'
# -> {"value":"https://fcm.googleapis.com/fcm/send/priam-test-endpoint-3"}   (PushSubscription, inferred + primaryKeys.subscriptionId)
```

## 11. Backfill script

```bash
# Register a 2nd account, confirm the live hook already created its data_subject
curl -s -X POST http://localhost:3000/api/v1/accounts -H "X-Forwarded-Proto: https" \
  -H "Authorization: Bearer <app token>" \
  -d username=priam_backfill_test -d email=priam-backfill-test@gmail.com -d password='BackfillTest!2026' \
  -d agreement=true -d locale=en -d reason="backfill test"

docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb \
  -e "SELECT * FROM \`priam-actor\`.data_subject WHERE id_ref='priam_backfill_test';"
# -> data_subject_id=2, id_ref='priam_backfill_test'   <- the live hook already worked

# Simulate "existed before the hook / lost its registration": delete the row directly
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb -D "priam-data" \
  -e "DELETE FROM processed_data WHERE data_subject_id=2;"
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb -D "priam-actor" \
  -e "DELETE FROM data_subject WHERE id_ref='priam_backfill_test';"

# Run the real backfill script inside the container
docker compose exec web bin/rails runner priam-integration/backfill-data-subjects.rb
# -> Backfilled priam_seed (1 statuses, 1 push subscriptions)
#    Backfilled priam_backfill_test (0 statuses, 0 push subscriptions)

docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb -D "priam-actor" \
  -e "SELECT * FROM data_subject WHERE id_ref='priam_backfill_test';"
# -> data_subject_id=3, id_ref='priam_backfill_test'   <- row recreated

# Idempotency: re-run, confirm no duplicate
docker compose exec web bin/rails runner priam-integration/backfill-data-subjects.rb
docker exec priam-databases mysql -uroot -p'MaiRP_pWd-ToOr' -h mysqldb -D "priam-actor" \
  -e "SELECT COUNT(*) FROM data_subject WHERE id_ref='priam_backfill_test';"
# -> 1   <- confirmed idempotent
```

## 12. Not performed — real browser test

No Playwright/browser-automation tool was available in this environment
(checked via the tool-search mechanism; only a non-interactive
`WebFetch`-style tool exists, which cannot render a JS SPA, click a
toggle, or hold a real cookie/OIDC session). Per playbook §7 point 14, this
is stated explicitly rather than claimed: **frontend visual/interactive
validation of the PRIAM-Frontend consent page and Access Request page, and
of the "Manage on PRIAM" link in Mastodon's own UI, was not performed**.
§9 above substitutes the closest achievable equivalent (a real Devise
session + real CSRF token + the exact same HTTP calls a browser would
make), which is sufficient to prove the server-side mechanism but not the
rendered UI. The stack was left running and a ready-to-use account
(`priam_seed` / `priam-seed-test@gmail.com` / `PriamSeed!2026`) is
available for a manual pass at `http://localhost:3000` /
`http://localhost:4200`.
