# PRIAM Integration — TeaStore

## Application profile

| Property | Value |
|----------|-------|
| Application | TeaStore — micro-service reference and test application (e-commerce store) |
| Repository | https://github.com/DescartesResearch/TeaStore |
| Stack | Java 11 · Jakarta EE · JAX-RS (Jersey 2) · JPA (EclipseLink) · PostgreSQL |
| Architecture | Microservices (6 services: persistence, auth, image, recommender, webui, registry) |
| Auth mechanism | Session cookie (SHA-256 HMAC tokens, managed by the Auth service) |
| User identifier | Long `User.id` |
| Total backend LOC (pre-integration) | 23 113 (Java source files, excl. tests) |

## Personal data annotated

See `annotations.json` for the full PRIAM metadata payload.

| Model | Field | Category | Portable |
|-------|-------|----------|---------|
| User | userName | identification | Yes |
| User | realName | identification | Yes |
| User | email | contact | Yes |
| Order | addressName | contact | Yes |
| Order | address1 | contact | Yes |
| Order | address2 | contact | Yes |
| Order | creditCardCompany | financial | Yes |
| Order | creditCardNumber | financial | No (sensitive) |

## Processing activities declared

| Processing | Type | Legal basis | Consent required |
|------------|------|-------------|-----------------|
| `account-management` | NECESSARY | CONTRACT | No — core feature |
| `order-processing` | NECESSARY | CONTRACT | No — core feature |
| `purchase-recommendations` | OPTIONAL | CONSENT | **Yes** |

## Integration steps performed

### Step 1 — Provider endpoints (1 new class in persistence service, ~275 LOC)

**New class**: `tools.descartes.teastore.persistence.rest.PriamEndpoint`

| File | Role |
|------|------|
| `persistence/rest/PriamEndpoint.java` | JAX-RS resource with 3 Provider endpoints |

Auto-discovered by the Jersey package scan configured in `web.xml`
(`jersey.config.server.provider.packages = tools.descartes.teastore.persistence.rest`).
**No `web.xml` modification required.**

**Endpoints exposed** at `/rest/priam/`:

| Endpoint | Method | GDPR right |
|----------|--------|-----------|
| `/rest/priam/dataAccessRight` | GET | Art. 15 — Access |
| `/rest/priam/rectification` | POST | Art. 16 — Rectification |
| `/rest/priam/erasure` | POST | Art. 17 — Erasure |

Supported models: `User` (userName, realName, email), `Order` (addressName, address1,
address2, creditCardCompany, creditCardNumber).

### Step 2 — Consent check on purchase recommendations

**New class**: `tools.descartes.teastore.recommender.priam.ConsentClient`

A lightweight HTTP utility (~85 LOC) that queries PRIAM's CDP before executing
the recommender. Placed in a dedicated package (`recommender.priam`) — no
changes to the existing recommender algorithm.

**Modified class**: `tools.descartes.teastore.recommender.rest.RecommendSingleEndpoint`

The `POST /rest/recommendsingle` endpoint drives the "customers also bought"
feature by analysing a user's order history. Because it processes personal data
for a secondary purpose, it requires explicit consent.

When the user has not consented (or has withdrawn consent), the endpoint returns
an **empty recommendations list** — the shopping cart is displayed without
personalised suggestions, and no order-history analysis is performed.

```java
// POST /rest/recommendsingle?uid=<userId>
if (uid != null && !ConsentClient.getConsent(String.valueOf(uid), "purchase-recommendations")) {
    return Response.ok().entity(new LinkedList<Long>()).build();
}
```

**Backward compatibility**: when `PRIAM_CDP_URL` is not set in the environment,
`ConsentClient.getConsent()` returns `true`, preserving existing recommendation
behaviour in environments without PRIAM.

### Step 3 — Authentication bridge

TeaStore uses session cookies managed by its own Auth service (SHA-256 HMAC tokens).
PRIAM's API Gateway validates requests via Keycloak using an `X-Username` header.
The bridge strategy:

1. Configure Keycloak as an OIDC identity provider in the TeaStore Auth service
   (supported via standard OIDC configuration — zero code changes).
2. The PRIAM frontend passes `X-Username: <userId>` when calling PRIAM endpoints
   through the Gateway.
3. The Gateway's `KeycloakLoginCheckFilter` verifies the user in the Keycloak realm.

**Net code change for auth bridge: 0 lines** — pure configuration.

## Integration metrics

| Metric | Value |
|--------|-------|
| **New files created** | 2 |
| **Existing files modified** | 1 (`RecommendSingleEndpoint.java`) |
| **LOC added (new files)** | 360 |
| **LOC added (modifications)** | 5 |
| **Total LOC added** | 365 |
| **Modification ratio** | 365 / 23 113 = **1.58%** of backend |
| **Existing tests broken** | 0 (consent guard returns `true` when `PRIAM_CDP_URL` is unset, preserving pre-integration recommendation behaviour) |
| **Optional processings guarded** | 1 (`purchase-recommendations` on 1 endpoint) |
| **Auth bridge code changes** | 0 (configuration only) |

## Environment variable added

```bash
PRIAM_CDP_URL=http://consent:8089   # PRIAM Consent Decision Point URL
                                    # If unset, consent checks are bypassed
```

## Acceptance tests

### Test 1 — Right of access (User)

```
GET /rest/priam/dataAccessRight?idRef=1&dataTypeName=User&attributes=userName,realName,email
→ 200 [{"userName": "user1", "realName": "Alice Smith", "email": "alice@example.com"}]
```

### Test 2 — Right of access (Order)

```
GET /rest/priam/dataAccessRight?idRef=1&dataTypeName=Order&attributes=addressName,address1,creditCardNumber
→ 200 [
    {"orderId": 42, "addressName": "Alice Smith", "address1": "123 Main St", "creditCardNumber": "4111111111111111"},
    ...
  ]
```

### Test 3 — Right to rectification

```
POST /rest/priam/rectification
{ "idRef": "1", "dataTypeName": "User", "dataName": "email",
  "newValue": "alice.new@example.com", "primaryKeys": {} }
→ 200 OK
```

### Test 4 — Right to erasure

```
POST /rest/priam/erasure
{ "idRef": "1", "dataTypeName": "Order", "dataName": "creditCardNumber",
  "primaryKeys": {"id": "42"} }
→ 200 OK  (creditCardNumber set to null in the database)
```

### Test 5 — Consent enforcement on purchase recommendations

```
# PRIAM_CDP_URL set, user has no active consent for purchase-recommendations
POST /rest/recommendsingle?uid=1  { "productId": 7, ... }
→ 200 []  (empty list — no recommendation computed, no order history analysed)

# After granting consent via PRIAM frontend
POST /rest/recommendsingle?uid=1  { "productId": 7, ... }
→ 200 [12, 34, 56]  (personalised recommendations returned)
```

## AI assistance

This integration was performed with **Claude Sonnet 4.6** (`claude-sonnet-4-6`, Anthropic) via Claude Code (interactive CLI). FastAPI-Healthcare and TeaStore were integrated **in the same session**; token and cost figures below reflect this case study's proportional share (~56% of session output).

### Tasks performed by the model

- Read and analysed the TeaStore codebase (JPA entities, JAX-RS REST endpoints, Jersey/web.xml configuration)
- Generated `PriamEndpoint.java` for the persistence service from scratch (auto-discovered by Jersey package scan — no `web.xml` changes required)
- Generated `ConsentClient.java` for the recommender service from scratch
- Applied a targeted modification to `RecommendSingleEndpoint.java` (consent guard — 4 lines)
- Drafted `annotations.json` with GDPR metadata covering all identified personal data fields (User, Order)
- Wrote this `INTEGRATION.md` integration report

### Generated artefacts

| Artefact | Type | LOC |
|----------|------|-----|
| `persistence/rest/PriamEndpoint.java` | new file | 275 |
| `recommender/priam/ConsentClient.java` | new file | 85 |
| `annotations.json` | new file | 139 |
| `INTEGRATION.md` | new file | 173 |
| `recommender/rest/RecommendSingleEndpoint.java` (consent guard) | modified | +4 |
| **Total** | | **676** |

### Token consumption (estimated)

Token counts are estimated: output tokens from artifact character counts (1 token ≈ 4 characters) plus in-conversation text; input tokens from the number and size of source files read, the accumulated conversation history, and the Claude Code system prompt. Figures are this case study's share (~56%) of the combined FastAPI-Healthcare + TeaStore session.

| Metric | Estimate |
|--------|----------|
| Input tokens | ~31 400 |
| Output tokens | ~8 500 |
| **Total tokens** | **~39 900** |
| Session duration | not recorded (shared with FastAPI-Healthcare) |

> Estimates carry ±25% uncertainty. No API usage log was retained for this session.

### Cost estimate (Claude Sonnet 4.6, standard rates)

| Tier | Tokens | Rate | Cost |
|------|--------|------|------|
| Input | ~31 400 | $3.00 / MTok | ~$0.09 |
| Output | ~8 500 | $15.00 / MTok | ~$0.13 |
| **Case-study share** | **~39 900** | | **~$0.22** |

### Human supervision

The human operator directed the work by specifying both case studies and the PRIAM integration pattern. All code and documentation were generated by the model. The human reviewed outputs and committed the result.