#!/usr/bin/env python3
"""One-off backfill for users that existed before the PRIAM sign-up hooks
were wired in (playbook §4bis, last point) - the 4 demo accounts seeded by
accounts-db/initdb/1-load-testdata.sql (testuser, alice, bob, eve), which
never went through userservice.py's POST /users and therefore never fired
register_data_subject()/report_processed_data()/provision_keycloak_user().

Re-running is safe: register_data_subject() upserts by idRef
(DataSubjectServiceImpl.saveDataSubject), report_processed_data() and
provision_keycloak_user() are idempotent replays (playbook §4bis).

Reuses userservice's own priam.py directly (not a separate database-to-
database access), so it must run with that module importable - i.e. inside
the userservice container:

    docker cp priam-integration/backfill-data-subjects.py \\
        boa-userservice:/app/backfill-data-subjects.py
    docker compose exec userservice python /app/backfill-data-subjects.py
"""
import logging
import os

from sqlalchemy import create_engine, text

import priam

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger('backfill')

USER_DATA_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
# Demo password for all 4 seeded users (accounts-db/initdb/1-load-testdata.sh
# - "All demo user accounts are hardcoded to use the login password
# 'bankofanthos'", public/documented). The plaintext password is normally
# only available at sign-up time (playbook §4bis) - these accounts predate
# the hook, so the well-known demo password is the only option here.
DEMO_PASSWORD = 'bankofanthos'


def main():
    engine = create_engine(os.environ['ACCOUNTS_DB_URI'])
    with engine.connect() as conn:
        rows = conn.execute(text('SELECT username, firstname, lastname FROM users')).all()
    for username, firstname, lastname in rows:
        logger.info('Backfilling %s', username)
        priam.register_data_subject(username, logger)
        priam.report_processed_data(username, USER_DATA_IDS, logger)
        priam.provision_keycloak_user(username, firstname, lastname, DEMO_PASSWORD, logger)


if __name__ == '__main__':
    main()
