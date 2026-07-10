"""
PRIAM Consent Enforcement Point (CEP) for FastAPI-Healthcare-PRIAM.

Queries PRIAM's Consent Decision Point (CDP) before executing an optional processing.
If PRIAM_CDP_URL is not set, returns True to preserve existing behaviour.
If PRIAM is reachable but consent is denied, or if the CDP is unreachable, returns False.
"""

import json
import logging
import os
import urllib.parse
import urllib.request

logger = logging.getLogger(__name__)

_CDP_URL: str | None = os.environ.get("PRIAM_CDP_URL")


def get_consent(patient_id: int | str, processing_id: str) -> bool:
    if not _CDP_URL:
        return True

    id_ref = str(patient_id)
    url = f"{_CDP_URL}/api/decision/{urllib.parse.quote(processing_id)}?idRefList={urllib.parse.quote(id_ref)}"
    try:
        req = urllib.request.Request(url, headers={"Accept": "application/json"})
        with urllib.request.urlopen(req, timeout=3) as resp:
            decision: dict = json.loads(resp.read())
            return decision.get(id_ref, False) is True
    except Exception as exc:
        logger.warning("PRIAM CDP unreachable (%s). Denying by default.", exc)
        return False
