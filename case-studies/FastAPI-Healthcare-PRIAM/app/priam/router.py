"""
PRIAM Provider endpoints for FastAPI-Healthcare-PRIAM.

Exposes the three Provider endpoints PRIAM's Right service calls (via the
Gateway's /provider/** route, which strips the /provider prefix):
  GET  /api/dataAccessRight  — Right of Access (GDPR Art. 15)
  POST /api/rectification    — Right to Rectification (GDPR Art. 16)
  POST /api/erasure          — Right to Erasure (GDPR Art. 17)

Mounted at bare "/api" (not "/api/priam") in main.py: PRIAM's
ProviderRestClient (PRIAM-Right-service/.../openfeign/ProviderRestClient.java)
requests exactly these paths — no extra "/priam" segment.

Supported data types: Patient, MedicalRecord, Appointment.
The idRef parameter maps to patient_id (integer primary key of the Patient table).
"""

import logging
from typing import Any

from fastapi import APIRouter, Depends, HTTPException, Query
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.db.session import get_db
from app.db.models import Patient, MedicalRecord, Appointment

logger = logging.getLogger(__name__)

router = APIRouter()

_ALLOWED_FIELDS: dict[str, list[str]] = {
    "Patient": [
        "first_name", "last_name", "email", "phone",
        "date_of_birth", "address", "insurance_provider", "insurance_id",
    ],
    "MedicalRecord": ["diagnosis", "treatment", "prescription", "notes"],
    "Appointment": ["notes"],
}


class RectificationRequest(BaseModel):
    idRef: str
    dataTypeName: str
    dataName: str
    newValue: str
    primaryKeys: dict = {}


class ErasureRequest(BaseModel):
    idRef: str
    dataTypeName: str
    dataName: str
    primaryKeys: dict = {}


def _validate(model_name: str, fields: list[str]) -> None:
    allowed = _ALLOWED_FIELDS.get(model_name)
    if allowed is None:
        raise HTTPException(status_code=400, detail=f"Unknown dataTypeName: {model_name}")
    forbidden = [f for f in fields if f not in allowed]
    if forbidden:
        raise HTTPException(
            status_code=400,
            detail=f"Fields not allowed for {model_name}: {forbidden}",
        )


def _get_record(db: Session, model_name: str, id_ref: str, primary_keys: dict) -> Any:
    patient_id = int(id_ref)
    if model_name == "Patient":
        return db.query(Patient).filter(Patient.id == patient_id).first()
    if model_name == "MedicalRecord":
        record_id = int(primary_keys.get("id", 0))
        return (
            db.query(MedicalRecord)
            .filter(MedicalRecord.id == record_id, MedicalRecord.patient_id == patient_id)
            .first()
        )
    if model_name == "Appointment":
        appt_id = int(primary_keys.get("id", 0))
        return (
            db.query(Appointment)
            .filter(Appointment.id == appt_id, Appointment.patient_id == patient_id)
            .first()
        )
    return None


@router.get("/dataAccessRight")
def data_access_right(
    idRef: str = Query(...),
    dataTypeName: str = Query(...),
    attributes: str = Query(...),
    db: Session = Depends(get_db),
):
    """Right of Access — GDPR Art. 15."""
    attrs = [a.strip() for a in attributes.split(",") if a.strip()]
    if not attrs:
        raise HTTPException(status_code=400, detail="attributes must not be empty")
    _validate(dataTypeName, attrs)

    patient_id = int(idRef)

    if dataTypeName == "Patient":
        record = db.query(Patient).filter(Patient.id == patient_id).first()
        if not record:
            raise HTTPException(status_code=404, detail="Patient not found")
        return [{attr: str(getattr(record, attr, None)) for attr in attrs}]

    if dataTypeName == "MedicalRecord":
        records = db.query(MedicalRecord).filter(MedicalRecord.patient_id == patient_id).all()
        return [{attr: str(getattr(r, attr, None)) for attr in attrs} for r in records]

    if dataTypeName == "Appointment":
        appts = db.query(Appointment).filter(Appointment.patient_id == patient_id).all()
        return [{attr: str(getattr(a, attr, None)) for attr in attrs} for a in appts]

    raise HTTPException(status_code=400, detail=f"Unknown dataTypeName: {dataTypeName}")


@router.post("/rectification")
def rectification(body: RectificationRequest, db: Session = Depends(get_db)):
    """Right to Rectification — GDPR Art. 16."""
    _validate(body.dataTypeName, [body.dataName])
    record = _get_record(db, body.dataTypeName, body.idRef, body.primaryKeys)
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    setattr(record, body.dataName, body.newValue)
    db.commit()
    logger.info("Rectification: patient=%s model=%s field=%s", body.idRef, body.dataTypeName, body.dataName)
    return {"status": "ok"}


@router.post("/erasure")
def erasure(body: ErasureRequest, db: Session = Depends(get_db)):
    """Right to Erasure — GDPR Art. 17."""
    _validate(body.dataTypeName, [body.dataName])
    record = _get_record(db, body.dataTypeName, body.idRef, body.primaryKeys)
    if not record:
        raise HTTPException(status_code=404, detail="Record not found")
    setattr(record, body.dataName, None)
    db.commit()
    logger.info("Erasure: patient=%s model=%s field=%s", body.idRef, body.dataTypeName, body.dataName)
    return {"status": "ok"}
