import json
import logging
from typing import Optional
from datetime import datetime
import aio_pika
import asyncio

from app.core.config import settings
from app.db.session import SessionLocal
from app.crud.crud_appointment import appointment
from app.crud.crud_patient import patient
from app.crud.crud_doctor import doctor

logger = logging.getLogger(__name__)

async def send_to_queue(message: dict):
    """Send a message to RabbitMQ queue"""
    try:
        connection = await aio_pika.connect_robust(settings.RABBITMQ_URL)

        async with connection:
            channel = await connection.channel()

            queue = await channel.declare_queue("notifications", durable=True)

            await channel.default_exchange.publish(
                aio_pika.Message(
                    body=json.dumps(message).encode(),
                    delivery_mode=aio_pika.DeliveryMode.PERSISTENT
                ),
                routing_key=queue.name,
            )

            logger.info(f"Sent notification message: {message}")

    except Exception as e:
        logger.error(f"Failed to send message to queue: {e}")

def send_appointment_notification(
    appointment_id: int,
    notification_type: str,
    patient_id: Optional[int] = None,
    doctor_id: Optional[int] = None,
    appointment_time: Optional[datetime] = None,
    status: Optional[str] = None
):
    """Send notification about appointment changes"""
    try:
        db = SessionLocal()

        if notification_type == "cancelled" and patient_id and doctor_id and appointment_time:
            patient_obj = patient.get(db, id=patient_id)
            doctor_obj = doctor.get(db, id=doctor_id)

            message = {
                "type": notification_type,
                "appointment_id": appointment_id,
                "patient_email": patient_obj.email,
                "patient_name": f"{patient_obj.first_name} {patient_obj.last_name}",
                "doctor_name": f"{doctor_obj.first_name} {doctor_obj.last_name}",
                "appointment_time": appointment_time.isoformat(),
            }
        else:
            appointment_obj = appointment.get_with_details(db, id=appointment_id)
            if not appointment_obj:
                logger.error(f"Appointment {appointment_id} not found for notification")
                return

            message = {
                "type": notification_type,
                "appointment_id": appointment_id,
                "patient_email": appointment_obj["patient"].email,
                "patient_name": appointment_obj["patient_name"],
                "doctor_name": appointment_obj["doctor_name"],
                "appointment_time": appointment_obj["start_time"].isoformat(),
            }

            if status:
                message["status"] = status

        asyncio.run(send_to_queue(message))

    except Exception as e:
        logger.error(f"Error sending notification: {e}")
    finally:
        db.close()
