# PRIAM Integration — FastAPI-Healthcare

## Application profile

| Property | Value |
|----------|-------|
| Application | FastAPI-Healthcare — healthcare appointment management API |
| Stack | Python 3.11 · FastAPI · SQLAlchemy · PostgreSQL · RabbitMQ · Redis |
| Architecture | Monolithic FastAPI application |
| Auth mechanism | JWT Bearer tokens (python-jose) |
| User identifier | Integer `Patient.id` |
| Total backend LOC (pre-integration) | 2 135 (all Python files, incl. tests) |

## Personal data annotated

See `annotations.json` for the full PRIAM metadata payload.

| Model | Field | Category | Art. 9? | Portable |
|-------|-------|----------|---------|---------|
| Patient | first_name | identification | No | Yes |
| Patient | last_name | identification | No | Yes |
| Patient | email | contact | No | Yes |
| Patient | phone | contact | No | Yes |
| Patient | date_of_birth | identification | No | Yes |
| Patient | address | contact | No | Yes |
| Patient | insurance_provider | financial | No | Yes |
| Patient | insurance_id | financial | No | No |
| MedicalRecord | diagnosis | health | **Yes** | No |
| MedicalRecord | treatment | health | **Yes** | No |
| MedicalRecord | prescription | health | **Yes** | No |
| MedicalRecord | notes | health | **Yes** | No |
| Appointment | notes | profile | No | Yes |

## Processing activities declared

| Processing | Type | Legal basis | Consent required |
|------------|------|-------------|-----------------|
| `appointment-management` | NECESSARY | CONTRACT | No — core feature |
| `medical-records-management` | NECESSARY | CONTRACT (Art. 9(2)(h)) | No — healthcare purpose |
| `appointment-notifications` | OPTIONAL | CONSENT | **Yes** |

## Integration steps performed

### Step 1 — Provider endpoints (1 new module, 3 new files, ~203 LOC)

**New module**: `app/priam/` (3 files)

| File | Role |
|------|------|
| `__init__.py` | Package marker |
| `consent.py` | CDP client — `get_consent(patient_id, processing_id)` |
| `router.py` | FastAPI router with 3 Provider endpoints |

**Endpoints exposed** at `/api/priam/`:

| Endpoint | Method | GDPR right |
|----------|--------|-----------|
| `/api/priam/dataAccessRight` | GET | Art. 15 — Access |
| `/api/priam/rectification` | POST | Art. 16 — Rectification |
| `/api/priam/erasure` | POST | Art. 17 — Erasure |

Supported models: `Patient` (8 fields), `MedicalRecord` (diagnosis, treatment, prescription, notes), `Appointment` (notes).

### Step 2 — Consent check on appointment notifications

**Modified file**: `app/api/routes/appointment.py`

Appointment notifications send the patient's name and email address to an external
RabbitMQ queue for delivery by the notification worker. This is an optional
processing (`appointment-notifications`) that requires explicit consent.

The consent guard was added to the two notification-dispatching endpoints:

```python
# POST /api/appointments — create appointment
# PUT  /api/appointments/{id} — update appointment
if get_consent(patient_id, "appointment-notifications"):
    background_tasks.add_task(send_appointment_notification, ...)
```

**Backward compatibility**: when `PRIAM_CDP_URL` is not set in the environment,
`get_consent()` returns `True` (allow), preserving existing notification behaviour
in environments without PRIAM. Notifications are suppressed only when PRIAM is
configured and the patient has not consented.

### Step 3 — Router registration

**Modified file**: `app/main.py`

The PRIAM router is registered without authentication, as PRIAM's Right Management
service calls these endpoints directly using internal network access:

```python
from app.priam.router import router as priam_router
app.include_router(priam_router, prefix="/api/priam", tags=["PRIAM"])
```

In production these endpoints should be restricted to PRIAM's internal network
or protected with an API key.

### Step 4 — Authentication bridge

FastAPI-Healthcare uses JWT Bearer tokens. PRIAM's API Gateway validates requests
via Keycloak using an `X-Username` header. The bridge strategy:

1. Configure Keycloak as the OIDC provider for FastAPI-Healthcare's JWT issuance
   (configuration only — zero code changes in the application).
2. The PRIAM frontend passes `X-Username: <patientId>` when calling PRIAM endpoints
   through the Gateway.

**Net code change for auth bridge: 0 lines** — pure configuration.

## Integration metrics

| Metric | Value |
|--------|-------|
| **New files created** | 3 |
| **Existing files modified** | 2 (`main.py`, `appointment.py`) |
| **LOC added (new files)** | 203 |
| **LOC added (modifications)** | 5 |
| **Total LOC added** | 208 |
| **Modification ratio** | 208 / 2 135 = **9.74%** of total Python LOC |
| **Existing tests broken** | 0 (consent guard returns `True` when `PRIAM_CDP_URL` is unset, preserving pre-integration behaviour; all tests remain passing) |
| **Optional processings guarded** | 1 (`appointment-notifications` on 2 endpoints) |
| **Auth bridge code changes** | 0 (configuration only) |

> **Note on ratio**: the high percentage (9.74%) reflects the small size of the application (2 135 LOC), not the complexity of the integration. The absolute number of added lines (208) is comparable to the other case studies.

## Environment variable added

```bash
PRIAM_CDP_URL=http://consent:8089   # PRIAM Consent Decision Point URL
                                    # If unset, consent checks are bypassed (notifications always sent)
```

## Acceptance tests

### Test 1 — Right of access (Patient)

```
GET /api/priam/dataAccessRight?idRef=1&dataTypeName=Patient&attributes=first_name,last_name,email
→ 200 [{"first_name": "Alice", "last_name": "Martin", "email": "alice@example.com"}]
```

### Test 2 — Right of access (MedicalRecord)

```
GET /api/priam/dataAccessRight?idRef=1&dataTypeName=MedicalRecord&attributes=diagnosis,treatment
→ 200 [{"recordId": 5, "diagnosis": "Hypertension", "treatment": "ACE inhibitor"}, ...]
```

### Test 3 — Right to rectification

```
POST /api/priam/rectification
{ "idRef": "1", "dataTypeName": "Patient", "dataName": "email",
  "newValue": "alice.new@example.com", "primaryKeys": {"id": "1"} }
→ 200 {"status": "ok"}
```

### Test 4 — Right to erasure

```
POST /api/priam/erasure
{ "idRef": "1", "dataTypeName": "MedicalRecord", "dataName": "notes",
  "primaryKeys": {"id": "5"} }
→ 200 {"status": "ok"}  (notes set to NULL in PostgreSQL)
```

### Test 5 — Consent enforcement on appointment notifications

```
# PRIAM_CDP_URL set, patient has no active consent for appointment-notifications
POST /api/appointments  { "patient_id": 1, "doctor_id": 2, ... }
→ 201 OK  (appointment created, but notification NOT dispatched to RabbitMQ)

# After granting consent via PRIAM frontend
POST /api/appointments  { "patient_id": 1, "doctor_id": 2, ... }
→ 201 OK  (appointment created and notification dispatched)
```

## AI assistance

This integration was performed with **Claude Sonnet 4.6** (`claude-sonnet-4-6`, Anthropic) via Claude Code (interactive CLI). FastAPI-Healthcare and TeaStore were integrated **in the same session**; token and cost figures below reflect this case study's proportional share (~44% of session output).

### Tasks performed by the model

- Read and analysed the FastAPI-Healthcare codebase (SQLAlchemy models, FastAPI routes, dependency injection, notification system)
- Generated the `app/priam/` module from scratch (3 files: `__init__.py`, `consent.py`, `router.py`)
- Applied targeted modifications to 2 existing files (`main.py` for router registration, `appointment.py` for consent guards)
- Drafted `annotations.json` with GDPR metadata covering all identified personal data fields (Patient, MedicalRecord, Appointment)
- Wrote this `INTEGRATION.md` integration report

### Generated artefacts

| Artefact | Type | LOC |
|----------|------|-----|
| `app/priam/__init__.py` | new file | 0 |
| `app/priam/consent.py` | new file | 36 |
| `app/priam/router.py` | new file | 167 |
| `annotations.json` | new file | 192 |
| `INTEGRATION.md` | new file | 179 |
| `app/main.py` (router registration) | modified | +3 |
| `app/api/routes/appointment.py` (2 consent guards) | modified | +4 |
| **Total** | | **581** |

### Token consumption (estimated)

Token counts are estimated: output tokens from artifact character counts (1 token ≈ 4 characters) plus in-conversation text; input tokens from the number and size of source files read, the accumulated conversation history, and the Claude Code system prompt. Figures are this case study's share (~44%) of the combined FastAPI-Healthcare + TeaStore session.

| Metric | Estimate |
|--------|----------|
| Input tokens | ~24 600 |
| Output tokens | ~6 600 |
| **Total tokens** | **~31 200** |
| Session duration | not recorded (shared with TeaStore) |

> Estimates carry ±25% uncertainty. No API usage log was retained for this session.

### Cost estimate (Claude Sonnet 4.6, standard rates)

| Tier | Tokens | Rate | Cost |
|------|--------|------|------|
| Input | ~24 600 | $3.00 / MTok | ~$0.07 |
| Output | ~6 600 | $15.00 / MTok | ~$0.10 |
| **Case-study share** | **~31 200** | | **~$0.17** |

### Human supervision

The human operator directed the work by specifying both case studies and the PRIAM integration pattern. All code and documentation were generated by the model. The human reviewed outputs and committed the result.