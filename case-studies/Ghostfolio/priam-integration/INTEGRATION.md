# PRIAM Integration — Ghostfolio

## Application profile

| Property | Value |
|----------|-------|
| Application | Ghostfolio — open-source personal wealth management |
| Repository | https://github.com/ghostfolio/ghostfolio |
| Stack | TypeScript · NestJS 11 · Prisma 7 · PostgreSQL · Redis |
| Architecture | Modular monolith (Nx monorepo: API + Angular client) |
| Auth mechanism | JWT Bearer tokens (+ Google OAuth, WebAuthn, API keys) |
| Total backend LOC (pre-integration) | 30 336 |
| Total project LOC (pre-integration) | 48 223 |

## Personal data annotated

See `annotations.json` for the full PRIAM metadata payload.

| Model | Field | Category | Portable |
|-------|-------|----------|---------|
| User | accessToken | identification | No |
| User | thirdPartyId | identification | No |
| Account | name | profile | Yes |
| Account | balance | financial | Yes |
| Order | unitPrice | financial | Yes |
| Order | quantity | financial | Yes |
| Order | fee | financial | Yes |
| Order | comment | profile | Yes |

## Processing activities declared

| Processing | Type | Legal basis | Consent required |
|------------|------|-------------|-----------------|
| `portfolio-management` | NECESSARY | CONTRACT | No — core feature |
| `portfolio-ai-analysis` | OPTIONAL | CONSENT | **Yes** |
| `activity-analytics` | OPTIONAL | CONSENT | **Yes** |

## Integration steps performed

### Step 1 — Provider endpoints (GDPR Art. 15, 16, 17)

**New module**: `apps/api/src/app/priam/` (6 new files)

| File | Role |
|------|------|
| `priam.module.ts` | NestJS module wiring |
| `priam.controller.ts` | Exposes the 3 Provider endpoints |
| `priam.service.ts` | Prisma-based data access / rectification / erasure |
| `priam-consent.service.ts` | CDP client — `getConsent(userId, processingId)` |
| `dto/rectification.dto.ts` | Request body schema |
| `dto/erasure.dto.ts` | Request body schema |

**Endpoints exposed:**

| Endpoint | Method | GDPR right |
|----------|--------|-----------|
| `/api/priam/dataAccessRight` | GET | Art. 15 — Access |
| `/api/priam/rectification` | POST | Art. 16 — Rectification |
| `/api/priam/erasure` | POST | Art. 17 — Erasure |

### Step 2 — Consent check on AI feature

**Modified file**: `apps/api/src/app/endpoints/ai/ai.controller.ts`

The `GET /api/ai/prompt/:mode` endpoint sends portfolio holdings data to an
external LLM (OpenRouter). Before executing, it now queries PRIAM's Consent
Decision Point:

```typescript
const canUse = await this.priamConsentService.getConsent(userId, 'portfolio-ai-analysis');
if (!canUse) throw new ForbiddenException('Consent not granted.');
```

If the user has not consented (or has withdrawn consent), the endpoint
returns **403 Forbidden** and the portfolio data is never assembled or sent
to the external API.

**Also modified**: `apps/api/src/app/endpoints/ai/ai.module.ts` — `PriamModule`
added to imports so `PriamConsentService` is available via DI.

### Step 3 — Authentication bridge

Ghostfolio uses JWT Bearer tokens signed with its own `JWT_SECRET_KEY`.
PRIAM's API Gateway validates requests via Keycloak using an `X-Username`
header. The bridge strategy for Ghostfolio is:

1. Configure Keycloak as an OIDC provider in Ghostfolio (already supported
   via `passport-openidconnect` — configuration only, zero code changes).
2. On login, Ghostfolio issues its JWT; the PRIAM frontend passes
   `X-Username: <ghostfolioUserId>` when calling PRIAM endpoints through the
   Gateway.
3. The Gateway's `KeycloakLoginCheckFilter` verifies the user exists in the
   `teastore` Keycloak realm.

**Net code change for auth bridge: 0 lines** — pure configuration.

## Integration metrics

| Metric | Value |
|--------|-------|
| **New files created** | 6 |
| **Existing files modified** | 3 (`app.module.ts`, `ai.controller.ts`, `ai.module.ts`) |
| **LOC added (new files)** | 370 |
| **LOC added (modifications)** | 31 |
| **Total LOC added** | 401 |
| **Modification ratio** | 401 / 30 336 = **1.32%** of backend |
| **Existing tests broken** | 0 (3 suites already failing pre-integration due to `redis-cache.service.ts` type errors unrelated to PRIAM; 28/28 previously-passing tests still pass) |
| **Optional processings guarded** | 1 (`portfolio-ai-analysis`) |
| **Auth bridge code changes** | 0 (configuration only) |

## Environment variable added

```bash
PRIAM_CDP_URL=http://consent:8089   # PRIAM Consent Decision Point URL
```

## Acceptance tests

### Test 1 — Right of access

```
GET /api/priam/dataAccessRight?idRef=<userId>&dataTypeName=Order&attributes=unitPrice,quantity
→ 200 [ { "orderId": "...", "unitPrice": 42.5 }, ... ]
```

### Test 2 — Right to rectification

```
POST /api/priam/rectification
{ "idRef": "<userId>", "dataTypeName": "Account", "dataName": "name",
  "newValue": "Savings account", "primaryKeys": { "id": "<accountId>" } }
→ 200 OK
```

### Test 3 — Right to erasure

```
POST /api/priam/erasure
{ "idRef": "<userId>", "dataTypeName": "Order", "dataName": "comment",
  "primaryKeys": { "id": "<orderId>" } }
→ 200 OK  (comment set to null in PostgreSQL)
```

### Test 4 — Consent enforcement on AI feature

```
# Without consent
GET /api/ai/prompt/HOLDINGS  (userId has no active consent for portfolio-ai-analysis)
→ 403 Forbidden

# After granting consent via PRIAM frontend
GET /api/ai/prompt/HOLDINGS
→ 200 { "prompt": "..." }
```

## AI assistance

This integration was performed with **Claude Sonnet 4.6** (`claude-sonnet-4-6`, Anthropic) via Claude Code (interactive CLI). The model operated autonomously under human direction within a single dedicated session.

### Tasks performed by the model

- Read and analysed the Ghostfolio codebase (NestJS modules, Prisma schema, existing controllers and services)
- Generated all 6 new TypeScript files of the `priam/` module from scratch
- Applied targeted modifications to 3 existing files (`app.module.ts`, `ai.controller.ts`, `ai.module.ts`)
- Drafted `annotations.json` with GDPR metadata covering all identified personal data fields
- Wrote this `INTEGRATION.md` integration report

### Generated artefacts

| Artefact | Type | LOC |
|----------|------|-----|
| `priam.module.ts` | new file | 14 |
| `priam.controller.ts` | new file | 68 |
| `priam.service.ts` | new file | 189 |
| `priam-consent.service.ts` | new file | 45 |
| `dto/rectification.dto.ts` | new file | 31 |
| `dto/erasure.dto.ts` | new file | 22 |
| `annotations.json` | new file | 140 |
| `INTEGRATION.md` | new file | 154 |
| `ai.controller.ts`, `ai.module.ts`, `app.module.ts` | modified | +31 |
| **Total** | | **694** |

### Token consumption (estimated)

Token counts are estimated: output tokens from artifact character counts (1 token ≈ 4 characters) plus in-conversation text; input tokens from the number and size of source files read, the accumulated conversation history, and the Claude Code system prompt.

| Metric | Estimate |
|--------|----------|
| Input tokens | ~45 000 |
| Output tokens | ~9 400 |
| **Total tokens** | **~54 400** |
| Session duration | not recorded |

> Estimates carry ±25% uncertainty. No API usage log was retained for this session.

### Cost estimate (Claude Sonnet 4.6, standard rates)

| Tier | Tokens | Rate | Cost |
|------|--------|------|------|
| Input | ~45 000 | $3.00 / MTok | ~$0.14 |
| Output | ~9 400 | $15.00 / MTok | ~$0.14 |
| **Session total** | **~54 400** | | **~$0.28** |

### Human supervision

The human operator directed the work by specifying the case study and the PRIAM integration pattern. All code and documentation were generated by the model. The human reviewed outputs and committed the result.
