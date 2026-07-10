"""
Seeds a small set of demo Patient/Doctor/Appointment/MedicalRecord rows so
PRIAM's rights (access/rectification/erasure) and consent-gating tests have
real data to exercise (this app ships with no seed data otherwise).
No-op if a patient already exists.
"""
import logging
from datetime import date, datetime, timedelta

from sqlalchemy.orm import Session

from app.db.models import Appointment, AppointmentStatus, Doctor, MedicalRecord, Patient

logger = logging.getLogger(__name__)


def seed_demo_data(db: Session) -> None:
    if db.query(Patient).count() > 0:
        return

    logger.info("Seeding demo patient data for PRIAM testing")

    patient = Patient(
        first_name="Jane",
        last_name="Doe",
        date_of_birth=date(1990, 5, 12),
        email="jane.doe@example.com",
        phone="+33612345678",
        address="1 Rue de la Sante, 75001 Paris",
        insurance_provider="MutuelleSante",
        insurance_id="INS-000001",
    )
    db.add(patient)
    db.flush()  # assign patient.id (expected to be 1 on a fresh database)

    doctor = Doctor(
        first_name="Alice",
        last_name="Martin",
        email="alice.martin@example.com",
        phone="+33698765432",
        specialization="General Practice",
    )
    db.add(doctor)
    db.flush()

    appointment = Appointment(
        patient_id=patient.id,
        doctor_id=doctor.id,
        start_time=datetime.now() + timedelta(days=7),
        end_time=datetime.now() + timedelta(days=7, hours=1),
        status=AppointmentStatus.SCHEDULED.value,
        notes="Annual check-up",
    )
    db.add(appointment)
    db.flush()

    medical_record = MedicalRecord(
        patient_id=patient.id,
        appointment_id=appointment.id,
        diagnosis="Seasonal allergy",
        treatment="Antihistamine",
        prescription="Cetirizine 10mg once daily",
        notes="Follow up in 2 weeks if symptoms persist",
    )
    db.add(medical_record)

    db.commit()
    logger.info(f"Seeded demo patient id={patient.id}, doctor id={doctor.id}")
