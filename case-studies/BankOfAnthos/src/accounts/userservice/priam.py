"""PRIAM registration/bookkeeping client (playbook §4bis).

Fire-and-forget: never raises, never blocks user creation on PRIAM's
availability. Talks directly to PRIAM-Actor-service/PRIAM-Data-service (no
Gateway, no auth - machine-to-machine, playbook §6), contract verified
against their real controllers (DataSubjectRestAPI.java,
ProcessedDataController.java).
"""
import os

import requests

PRIAM_ACTOR_URL = os.environ.get('PRIAM_ACTOR_URL')
PRIAM_DATA_URL = os.environ.get('PRIAM_DATA_URL')
DATA_SUBJECT_CATEGORY_ID = 1
TIMEOUT_SECONDS = 3

KEYCLOAK_ADMIN_URL = os.environ.get('KEYCLOAK_ADMIN_URL')
KEYCLOAK_REALM = os.environ.get('KEYCLOAK_REALM', 'priam-realm')
KEYCLOAK_ADMIN_USERNAME = os.environ.get('KEYCLOAK_ADMIN_USERNAME', 'admin')
KEYCLOAK_ADMIN_PASSWORD = os.environ.get('KEYCLOAK_ADMIN_PASSWORD', 'admin')


def register_data_subject(id_ref, logger):
    """Idempotent upsert by idRef (DataSubjectServiceImpl.saveDataSubject)."""
    if not PRIAM_ACTOR_URL:
        return
    try:
        requests.post(f'{PRIAM_ACTOR_URL}/api/DataSubject',
                      json={'idRef': id_ref, 'dataSubjectCategoryId': DATA_SUBJECT_CATEGORY_ID},
                      timeout=TIMEOUT_SECONDS)
    except requests.exceptions.RequestException as err:
        logger.warning('PRIAM register_data_subject(%s) failed: %s', id_ref, err)


def report_processed_data(id_ref, data_ids, logger):
    """Bookkeeping so the Access Request page shows these columns for id_ref.

    Must be called after register_data_subject() has committed - see §8.6
    (idRef->dataSubjectId resolution races with the DataSubject insert).
    """
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


def provision_keycloak_user(id_ref, firstname, lastname, password, logger):
    """Automatic Keycloak identity provisioning at sign-up (playbook §4bis).

    Bank of Anthos has its own local sign-up with no email field, so an
    email is synthesized (Keycloak's User Profile requires one) and the
    Keycloak login `username` is padded to Keycloak's 3-character minimum
    if needed - the `idReference` attribute always stays the real,
    unpadded username (`id_ref`), matching the PRIAM data_subject.id_ref
    used everywhere else in this integration. Fire-and-forget: never
    raises, never blocks sign-up. 409 (already provisioned) is not an
    error (idempotent by construction, per the playbook).
    """
    if not KEYCLOAK_ADMIN_URL:
        return
    try:
        token_resp = requests.post(
            f'{KEYCLOAK_ADMIN_URL}/realms/master/protocol/openid-connect/token',
            data={'grant_type': 'password', 'client_id': 'admin-cli',
                  'username': KEYCLOAK_ADMIN_USERNAME, 'password': KEYCLOAK_ADMIN_PASSWORD},
            timeout=TIMEOUT_SECONDS)
        token_resp.raise_for_status()
        admin_token = token_resp.json()['access_token']

        kc_username = id_ref if len(id_ref) >= 3 else id_ref.ljust(3, '_')
        resp = requests.post(
            f'{KEYCLOAK_ADMIN_URL}/admin/realms/{KEYCLOAK_REALM}/users',
            headers={'Authorization': f'Bearer {admin_token}'},
            json={
                'username': kc_username,
                'email': f'{kc_username}@bankofanthos.local',
                'enabled': True,
                'emailVerified': True,
                'firstName': firstname,
                'lastName': lastname,
                'credentials': [{'type': 'password', 'value': password, 'temporary': False}],
                'attributes': {'idReference': [id_ref]},
            },
            timeout=TIMEOUT_SECONDS)
        if resp.status_code not in (201, 409):
            logger.warning('PRIAM provision_keycloak_user(%s) unexpected status %s: %s',
                           id_ref, resp.status_code, resp.text)
    except requests.exceptions.RequestException as err:
        logger.warning('PRIAM provision_keycloak_user(%s) failed: %s', id_ref, err)
