"""PRIAM CEP + bookkeeping client (playbook §4, §4bis).

Talks directly to PRIAM-Consent-Service/PRIAM-Actor-service/
PRIAM-Data-service (no Gateway, no auth), contract verified against their
real controllers (ContractRestController.java, ProcessedDataController.java).
"""
import os

import requests

PRIAM_CDP_URL = os.environ.get('PRIAM_CDP_URL')
PRIAM_ACTOR_URL = os.environ.get('PRIAM_ACTOR_URL')
PRIAM_DATA_URL = os.environ.get('PRIAM_DATA_URL')
TIMEOUT_SECONDS = 3


def get_consent(id_ref, processing_name, logger):
    """Fail-open if PRIAM absent, fail-closed on error (playbook §4)."""
    if not PRIAM_CDP_URL:
        return True
    try:
        resp = requests.get(f'{PRIAM_CDP_URL}/api/decision/{processing_name}',
                            params={'idRefList': id_ref}, timeout=TIMEOUT_SECONDS)
        resp.raise_for_status()
        # Missing idRef in the response map = no consent at all, not False
        # explicitly (ContractServiceImpl only adds entries with a match).
        return resp.json().get(id_ref, False) is True
    except requests.exceptions.RequestException as err:
        logger.warning('PRIAM get_consent(%s) failed: %s', id_ref, err)
        return False


def report_processed_data(id_ref, data_ids, logger):
    """Bookkeeping so the Access Request page shows these columns for id_ref."""
    if not PRIAM_ACTOR_URL or not PRIAM_DATA_URL:
        return
    try:
        resp = requests.get(f'{PRIAM_ACTOR_URL}/api/DataSubjectId/{id_ref}',
                            timeout=TIMEOUT_SECONDS)
        resp.raise_for_status()
        subject_id = resp.json()
        requests.post(f'{PRIAM_DATA_URL}/api/processed-data/add',
                      params={'subjectId': subject_id},
                      json=data_ids,
                      timeout=TIMEOUT_SECONDS)
    except requests.exceptions.RequestException as err:
        logger.warning('PRIAM report_processed_data(%s) failed: %s', id_ref, err)
