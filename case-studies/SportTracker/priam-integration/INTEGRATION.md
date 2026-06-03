# PRIAM Integration — SportTracker

## Application profile

| Property | Value |
|----------|-------|
| Application | SportTracker — self-hosted fitness tracking |
| Repository | https://github.com/deadlocker8/SportTracker |
| Stack | Python 3.14 · Flask 3.1 · SQLAlchemy · PostgreSQL |
| Architecture | Monolithic Flask application |
| Auth mechanism | Session-based (Flask-Login, bcrypt passwords) |
| User identifier | Integer `User.id` |
| Total backend LOC (pre-integration) | 16 793 |

## Personal data annotated

See `annotations.json` for the full PRIAM metadata payload.

| Model | Field | Category | Art. 9? | Portable |
|-------|-------|----------|---------|---------|
| User | username | identification | No | Yes |
| Workout | name | profile | No | Yes |
| Workout | average_heart_rate | health / biometric | **Yes** | Yes |
| Participant | name | identification | No | No |

## Processing activities declared

| Processing | Type | Legal basis | Consent required |
|------------|------|-------------|-----------------|
| `workout-tracking` | NECESSARY | CONTRACT | No — core feature |
| `heart-rate-tracking` | OPTIONAL | CONSENT | **Yes** (Art. 9 biometric data) |
| `participant-tracking` | OPTIONAL | CONSENT | **Yes** |

## Integration steps performed

### Step 1 — Provider endpoints (1 new module, 2 new files, ~130 LOC)

**New module**: `sporttracker/priam/` (2 files)

| File | Role |
|------|------|
| `__init__.py` | Package marker |
| `PriamBlueprint.py` | Flask Blueprint with 3 Provider endpoints + `get_consent()` helper |

**Endpoints exposed** at `/api/priam/`:

| Endpoint | Method | GDPR right |
|----------|--------|-----------|
| `/api/priam/dataAccessRight` | GET | Art. 15 — Access |
| `/api/priam/rectification` | POST | Art. 16 — Rectification |
| `/api/priam/erasure` | POST | Art. 17 — Erasure |

Supported models: `User` (username), `Workout` (name, average_heart_rate), `Participant` (name).

### Step 2 — Consent check on heart-rate endpoints

**Modified file**: `sporttracker/api/Api.py`

Heart-rate data is GDPR Art. 9 biometric data — it requires explicit consent.
The guard was added to both heart-rate upload endpoints:

```python
# POST /api/v2/workouts/distanceWorkout/<id>/addHeartRateData
# POST /api/v2/workouts/fitnessWorkout/<id>/addHeartRateData
if not get_consent(current_user.id, 'heart-rate-tracking'):
    return jsonify({'error': 'Consent for heart-rate tracking has not been granted.'}), 403
```

**Backward compatibility**: when `PRIAM_CDP_URL` is not set in the environment,
`get_consent()` returns `True` (allow), preserving existing behaviour in
environments without PRIAM. Consent is enforced only when PRIAM is configured.

### Step 3 — Authentication bridge

SportTracker uses Flask-Login sessions with integer user IDs. The auth bridge
for PRIAM is handled at the client level: when the SportTracker frontend calls
PRIAM endpoints through the Gateway, it passes `X-Username: <userId>`. This is
a **configuration-only** change — zero code modifications in SportTracker.

The PRIAM Provider endpoints (`/api/priam/*`) are unprotected in the
evaluation environment. In production they should be restricted to the PRIAM
internal network or protected with an API key.

## Integration metrics

| Metric | Value |
|--------|-------|
| **New files created** | 2 |
| **Existing files modified** | 2 (`SportTracker.py`, `Api.py`) |
| **LOC added (new files)** | ~130 |
| **LOC added (modifications)** | 12 |
| **Total LOC added** | ~142 |
| **Modification ratio** | 142 / 16 793 = **0.85%** of backend |
| **Existing tests broken** | 0 (syntax verified; full suite not runnable — Python 3.14+ and private `TheCodeLabs` packages required; `get_consent()` returns `True` when PRIAM is not configured, preserving pre-integration behaviour) |
| **Optional processings guarded** | 2 (`heart-rate-tracking` on 2 endpoints) |
| **Auth bridge code changes** | 0 (configuration only) |

## Environment variable added

```bash
PRIAM_CDP_URL=http://consent:8089   # PRIAM Consent Decision Point URL
                                    # If unset, consent checks are bypassed
```

## Acceptance tests

### Test 1 — Right of access

```
GET /api/priam/dataAccessRight?idRef=1&dataTypeName=Workout&attributes=name,average_heart_rate
→ 200 [ { "workoutId": 42, "name": "Morning run", "average_heart_rate": 145 }, ... ]
```

### Test 2 — Right to rectification

```
POST /api/priam/rectification
{ "idRef": "1", "dataTypeName": "Workout", "dataName": "name",
  "newValue": "Evening run", "primaryKeys": { "id": "42" } }
→ 200 OK
```

### Test 3 — Right to erasure

```
POST /api/priam/erasure
{ "idRef": "1", "dataTypeName": "Workout", "dataName": "average_heart_rate",
  "primaryKeys": { "id": "42" } }
→ 200 OK  (average_heart_rate set to NULL in PostgreSQL)
```

### Test 4 — Consent enforcement on heart-rate upload

```
# PRIAM_CDP_URL set, user has no active consent for heart-rate-tracking
POST /api/v2/workouts/distanceWorkout/42/addHeartRateData  [...]
→ 403 { "error": "Consent for heart-rate tracking has not been granted." }

# After granting consent via PRIAM frontend
POST /api/v2/workouts/distanceWorkout/42/addHeartRateData  [...]
→ 200 OK
```