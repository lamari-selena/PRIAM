"""PRIAM Consent Information Point client (playbook §4bis).

Talks directly to PRIAM-Consent-Service (no Gateway, no auth), contract
verified against its real controller (ContractRestController.java).
"""
import os

import requests

PRIAM_CDP_URL = os.environ.get('PRIAM_CDP_URL')
TIMEOUT_SECONDS = 3


def has_pending_consent_decision(id_ref, processing_name, logger):
    """True = never answered yet (redirect to PRIAM's consent page). False =
    already decided (granted or refused) or PRIAM unreachable - never force
    a redirect the user can't act on."""
    if not PRIAM_CDP_URL:
        return False
    try:
        resp = requests.get(
            f'{PRIAM_CDP_URL}/api/contract/list/consents/{id_ref}/{processing_name}',
            timeout=TIMEOUT_SECONDS)
        resp.raise_for_status()
        return len(resp.json()) == 0
    except requests.exceptions.RequestException as err:
        logger.warning('PRIAM has_pending_consent_decision(%s) failed: %s', id_ref, err)
        return False
