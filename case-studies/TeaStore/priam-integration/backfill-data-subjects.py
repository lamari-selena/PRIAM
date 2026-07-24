#!/usr/bin/env python3
"""One-off backfill for TeaStore users that existed before the PRIAM sign-up
hooks were wired in (playbook §4bis, last point) - the DataGenerator-seeded
accounts (user0..user{N-1}, see DataGenerator.generateUsers/generateOrders),
none of which ever went through the new POST /useractions/register and
therefore never fired register_data_subject()/report_processed_data().

Re-running is safe: register_data_subject() upserts by idRef
(DataSubjectServiceImpl.saveDataSubject), report_processed_data() replays
are idempotent occurrence counting (ProcessedDataService.addProcessedData -
see Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8 index, nb_occurrences), and
provision_keycloak_user() treats a 409 (already provisioned) as a no-op
(playbook §4bis).

Also provisions a matching Keycloak account for every seeded user, using
the well-known demo password ("password", DataGenerator.PASSWORD) - the
only password these pre-existing accounts have, same pattern as
case-studies/BankOfAnthos/priam-integration/backfill-data-subjects.py's
DEMO_PASSWORD. Without this, a seeded user (e.g. "user2") can log into
TeaStore itself (a local BCrypt check, independent of Keycloak) but has no
way to log into PRIAM-Frontend, since provisionKeycloakUser() is otherwise
only wired at the new POST /useractions/register endpoint - a seeded
account never goes through it.

Reuses TeaStore's own already-exposed REST API directly (persistence
service's `users`/`orders` endpoints - tools.descartes.teastore.persistence.
rest.UserEndpoint/OrderEndpoint) - no separate database-to-database access,
consistent with the pattern in case-studies/BankOfAnthos/priam-integration/
backfill-data-subjects.py and case-studies/Ghostfolio-PRIAM-test1's .mts
equivalent (those reuse the target app's own ORM/module; TeaStore has no
importable Python/JS module, so its own REST API is the equivalent "already-
configured access").

Intended to run once, after both stacks (PRIAM's root docker-compose.yml and
case-studies/TeaStore/docker-compose.yml) are up, from a throwaway container
on the shared `common_network` (see priam-integration/ETAPES-FAITES.md for
the exact `docker run` command actually used this session):

    docker run --rm --network common_network -v "$PWD:/w" -w /w \\
        python:3.11-slim sh -c "pip install -q requests && \\
        python case-studies/TeaStore/priam-integration/backfill-data-subjects.py"
"""
import logging
import os

import requests

PERSISTENCE_URL = os.environ.get('PERSISTENCE_URL', 'http://persistence:8080/tools.descartes.teastore.persistence/rest')
PRIAM_ACTOR_URL = os.environ.get('PRIAM_ACTOR_URL', 'http://actor:8082')
PRIAM_DATA_URL = os.environ.get('PRIAM_DATA_URL', 'http://data:8081')
KEYCLOAK_ADMIN_URL = os.environ.get('KEYCLOAK_ADMIN_URL', 'http://keycloak:8080')
KEYCLOAK_REALM = os.environ.get('KEYCLOAK_REALM', 'priam-realm')
KEYCLOAK_ADMIN_USERNAME = os.environ.get('KEYCLOAK_ADMIN_USERNAME', 'admin')
KEYCLOAK_ADMIN_PASSWORD = os.environ.get('KEYCLOAK_ADMIN_PASSWORD', 'admin')
# DataGenerator.java: PASSWORD = "password" - the only password these
# pre-existing seeded accounts have (see module docstring above).
SEED_PASSWORD = 'password'
TIMEOUT_SECONDS = 5

# Databases/db_insertion_script.sql: priam-actor.data_subject_category(1) = 'TeaStore Customer'.
DATA_SUBJECT_CATEGORY_ID = 1
# Databases/db_insertion_script.sql: priam-data.data(data_id) for User fields (userName/email/realName).
USER_DATA_IDS = [1, 2, 3]
# Databases/db_insertion_script.sql: priam-data.data(data_id) for Order fields (id + address/credit-card).
ORDER_DATA_IDS = [4, 5, 6, 7, 8, 9, 10]

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('backfill')


def register_data_subject(id_ref):
    requests.post(f'{PRIAM_ACTOR_URL}/api/DataSubject',
                  json={'idRef': id_ref, 'dataSubjectCategoryId': DATA_SUBJECT_CATEGORY_ID},
                  timeout=TIMEOUT_SECONDS)


def report_processed_data(id_ref, data_ids):
    resp = requests.get(f'{PRIAM_ACTOR_URL}/api/DataSubjectId/{id_ref}', timeout=TIMEOUT_SECONDS)
    resp.raise_for_status()
    subject_id = resp.json()
    requests.post(f'{PRIAM_DATA_URL}/api/processed-data/add',
                  params={'subjectId': subject_id}, json=data_ids, timeout=TIMEOUT_SECONDS)


def get_keycloak_admin_token():
    resp = requests.post(f'{KEYCLOAK_ADMIN_URL}/realms/master/protocol/openid-connect/token',
                          data={'grant_type': 'password', 'client_id': 'admin-cli',
                                'username': KEYCLOAK_ADMIN_USERNAME, 'password': KEYCLOAK_ADMIN_PASSWORD},
                          timeout=TIMEOUT_SECONDS)
    resp.raise_for_status()
    return resp.json()['access_token']


def provision_keycloak_user(admin_token, id_ref, email, real_name):
    # Same shape as auth/priam/PriamClient.java's provisionKeycloakUser():
    # Keycloak username = email (seeded usernames like "user2" are too short
    # / not what a human would type at Keycloak's login form), idReference
    # attribute = the real TeaStore idRef, firstName/lastName reused from
    # realName (TeaStore has no separate first/last name fields).
    resp = requests.post(
        f'{KEYCLOAK_ADMIN_URL}/admin/realms/{KEYCLOAK_REALM}/users',
        headers={'Authorization': f'Bearer {admin_token}'},
        json={
            'username': email, 'email': email, 'enabled': True, 'emailVerified': True,
            'firstName': real_name, 'lastName': real_name,
            'credentials': [{'type': 'password', 'value': SEED_PASSWORD, 'temporary': False}],
            'attributes': {'idReference': [id_ref]},
        },
        timeout=TIMEOUT_SECONDS)
    if resp.status_code not in (201, 409):
        raise requests.exceptions.RequestException(
            f'Keycloak provisioning returned {resp.status_code}: {resp.text}')


def main():
    admin_token = get_keycloak_admin_token()
    users = requests.get(f'{PERSISTENCE_URL}/users', params={'start': -1, 'max': -1},
                         timeout=TIMEOUT_SECONDS).json()
    for user in users:
        id_ref = user['userName']
        logger.info('Backfilling %s (id=%s)', id_ref, user['id'])
        try:
            register_data_subject(id_ref)
            report_processed_data(id_ref, USER_DATA_IDS)
            orders = requests.get(f'{PERSISTENCE_URL}/orders/user/{user["id"]}',
                                  params={'start': -1, 'max': -1}, timeout=TIMEOUT_SECONDS).json()
            for _ in orders:
                report_processed_data(id_ref, ORDER_DATA_IDS)
            provision_keycloak_user(admin_token, id_ref, user['email'], user['realName'])
            logger.info('%s: registered, %d order(s) reported, Keycloak provisioned', id_ref, len(orders))
        except requests.exceptions.RequestException as err:
            logger.warning('Backfill failed for %s: %s', id_ref, err)


if __name__ == '__main__':
    main()
