"""
PRIAM integration for SportTracker.

Exposes the three Provider endpoints required by PRIAM's Right Management service:
  GET  /api/priam/dataAccessRight  — Right of Access (GDPR Art. 15)
  POST /api/priam/rectification    — Right to Rectification (GDPR Art. 16)
  POST /api/priam/erasure          — Right to Erasure (GDPR Art. 17)

Also provides get_consent() for use as a Consent Enforcement Point (CEP).
"""

import logging
import os
import urllib.error
import urllib.parse
import urllib.request
import json
from http import HTTPStatus
from typing import Any

from flask import Blueprint, jsonify, request, abort

from sporttracker import Constants
from sporttracker.db import db
from sporttracker.user.UserEntity import User
from sporttracker.user.ParticipantEntity import Participant
from sporttracker.workout.WorkoutEntity import Workout

LOGGER = logging.getLogger(Constants.APP_NAME)

# ── Allowed fields per model ───────────────────────────────────────────────────

_ALLOWED_FIELDS: dict[str, list[str]] = {
    'User': ['username'],
    'Workout': ['name', 'average_heart_rate'],
    'Participant': ['name'],
}

# ── Consent Decision Point (CDP) client ───────────────────────────────────────

_CDP_URL: str | None = os.environ.get('PRIAM_CDP_URL')


def get_consent(user_id: int | str, processing_id: str) -> bool:
    """
    Query PRIAM's Consent Decision Point before executing an optional processing.

    - If PRIAM_CDP_URL is not set (PRIAM not configured), returns True so that
      existing behaviour is preserved in environments without PRIAM.
    - If PRIAM_CDP_URL is set but unreachable, returns False (deny-by-default).

    Maps to the ABAC Consent Enforcement Point (CEP) role.
    """
    if not _CDP_URL:
        return True  # PRIAM not configured — allow processing

    id_ref = str(user_id)
    url = (
        f'{_CDP_URL}/api/decision/{urllib.parse.quote(processing_id)}'
        f'?idRefList={urllib.parse.quote(id_ref)}'
    )
    try:
        req = urllib.request.Request(url, headers={'Accept': 'application/json'})
        with urllib.request.urlopen(req, timeout=3) as resp:
            decision: dict[str, bool] = json.loads(resp.read())
            return decision.get(id_ref, False) is True
    except Exception as exc:
        LOGGER.warning('PRIAM CDP unreachable (%s). Denying by default.', exc)
        return False


# ── Helper ────────────────────────────────────────────────────────────────────

def _validate(model_name: str, fields: list[str]) -> None:
    allowed = _ALLOWED_FIELDS.get(model_name)
    if allowed is None:
        abort(HTTPStatus.BAD_REQUEST, description=f'Unknown dataTypeName: {model_name}')
    forbidden = [f for f in fields if f not in allowed]
    if forbidden:
        abort(HTTPStatus.BAD_REQUEST,
              description=f'Fields not allowed for {model_name}: {forbidden}')


def _get_record(model_name: str, id_ref: str, primary_keys: dict[str, str]) -> Any:
    user_id = int(id_ref)
    if model_name == 'User':
        return db.session.get(User, user_id)
    if model_name == 'Workout':
        workout_id = int(primary_keys.get('id', 0))
        return Workout.query.filter_by(id=workout_id, user_id=user_id).first()
    if model_name == 'Participant':
        participant_id = int(primary_keys.get('id', 0))
        return Participant.query.filter_by(id=participant_id, user_id=user_id).first()
    return None


# ── Blueprint ─────────────────────────────────────────────────────────────────

def construct_blueprint() -> Blueprint:
    priam = Blueprint('priam', __name__, url_prefix='/api/priam')

    @priam.get('/dataAccessRight')
    def data_access_right():
        """Right of Access — GDPR Art. 15."""
        id_ref = request.args.get('idRef', '')
        model_name = request.args.get('dataTypeName', '')
        attributes_raw = request.args.getlist('attributes')
        # support both repeated params and comma-separated
        attributes = [
            a.strip()
            for raw in attributes_raw
            for a in raw.split(',')
            if a.strip()
        ]

        if not id_ref or not model_name or not attributes:
            abort(HTTPStatus.BAD_REQUEST, description='idRef, dataTypeName and attributes are required')

        _validate(model_name, attributes)

        user_id = int(id_ref)

        if model_name == 'User':
            user = db.session.get(User, user_id)
            if user is None:
                abort(HTTPStatus.NOT_FOUND)
            return jsonify([{attr: getattr(user, attr, None)} for attr in attributes])

        if model_name == 'Workout':
            workouts = Workout.query.filter_by(user_id=user_id).all()
            return jsonify([
                {'workoutId': w.id, **{attr: getattr(w, attr, None) for attr in attributes}}
                for w in workouts
            ])

        if model_name == 'Participant':
            participants = Participant.query.filter_by(user_id=user_id).all()
            return jsonify([
                {'participantId': p.id, **{attr: getattr(p, attr, None) for attr in attributes}}
                for p in participants
            ])

        abort(HTTPStatus.BAD_REQUEST)

    @priam.post('/rectification')
    def rectification():
        """Right to Rectification — GDPR Art. 16."""
        body = request.get_json(force=True) or {}
        id_ref = body.get('idRef', '')
        model_name = body.get('dataTypeName', '')
        data_name = body.get('dataName', '')
        new_value = body.get('newValue', '')
        primary_keys: dict[str, str] = body.get('primaryKeys') or {}

        if not all([id_ref, model_name, data_name, new_value]):
            abort(HTTPStatus.BAD_REQUEST, description='idRef, dataTypeName, dataName and newValue are required')

        _validate(model_name, [data_name])

        record = _get_record(model_name, id_ref, primary_keys)
        if record is None:
            abort(HTTPStatus.NOT_FOUND)

        # Cast numeric fields
        if data_name == 'average_heart_rate':
            setattr(record, data_name, int(new_value))
        else:
            setattr(record, data_name, new_value)

        db.session.commit()
        LOGGER.info('Rectification applied: user=%s model=%s field=%s', id_ref, model_name, data_name)
        return '', HTTPStatus.OK

    @priam.post('/erasure')
    def erasure():
        """Right to Erasure — GDPR Art. 17."""
        body = request.get_json(force=True) or {}
        id_ref = body.get('idRef', '')
        model_name = body.get('dataTypeName', '')
        data_name = body.get('dataName', '')
        primary_keys: dict[str, str] = body.get('primaryKeys') or {}

        if not all([id_ref, model_name, data_name]):
            abort(HTTPStatus.BAD_REQUEST, description='idRef, dataTypeName and dataName are required')

        _validate(model_name, [data_name])

        record = _get_record(model_name, id_ref, primary_keys)
        if record is None:
            abort(HTTPStatus.NOT_FOUND)

        setattr(record, data_name, None)
        db.session.commit()
        LOGGER.info('Erasure applied: user=%s model=%s field=%s', id_ref, model_name, data_name)
        return '', HTTPStatus.OK

    return priam