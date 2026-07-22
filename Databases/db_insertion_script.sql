-- ============================================================================
-- PRIAM annotation for Bank of Anthos (Google Cloud Platform's GKE banking
-- demo - 7 polyglot microservices, Python/Flask + Java/Spring Boot). See
-- Docs/PRIAM-INTEGRATION-PLAYBOOK.md §1.
--
-- Real schema referenced (not invented) - verified directly in the target
-- app's source, case-studies/BankOfAnthos/:
--   - src/accounts/accounts-db/initdb/0-accounts-schema.sql: `users` table
--     (accountid PK, username UNIQUE, passhash, firstname, lastname,
--     birthday, timezone, address, state, zip, ssn - ALL NOT NULL) and
--     `contacts` table (username FK, label, account_num, routing_num,
--     is_external - no surrogate id column at all).
--   - src/accounts/userservice/userservice.py (create_user, __validate_new_user):
--     the only user-creation endpoint, `POST /users`. accountid is a random
--     10-digit numeric string (db.py generate_accountid()) - NOT usable as a
--     non-numeric idRef test subject.
--   - src/accounts/contacts/contacts.py (add_contact, _check_contact_allowed):
--     `label` is enforced unique per username (duplicate labels rejected),
--     making it a reliable composite-key substitute for the missing
--     surrogate id (§1 point 5, §8.1.c).
--   - src/ledger/ledger-db/initdb/0_init_tables.sql: `TRANSACTIONS` table is
--     genuinely immutable at the database level (`CREATE RULE
--     PREVENT_UPDATE`/`PREVENT_DELETE` ... `DO INSTEAD NOTHING` - a real SQL
--     UPDATE/DELETE against it silently no-ops). Deliberately NOT annotated
--     here - see priam-integration/INTEGRATION-REPORT.md "Known limitations"
--     for the documented scope decision (financial ledger, legal retention
--     exception, Art. 17(3)(b)) instead of pretending rectification/erasure
--     would do anything against this table.
--
-- idRef: `users.username` (2-15 alphanumeric/underscore, e.g. "testuser",
-- "alice") - non-numeric by construction, unlike `accountid`. Chosen over
-- accountid specifically so both the pre-seeded demo subjects below AND any
-- dynamically registered subject satisfy the playbook §7 "non-numeric idRef"
-- test requirement without a special case. `contacts.username` is a direct
-- FK to this same value, so no join is needed to resolve a subject's
-- contacts; `users.accountid` is looked up once by the Provider bridge for
-- filters that need it, but is never itself the identity used by PRIAM.
--
-- Demo seed: 0-accounts-schema.sql ships 4 deterministic users (testuser,
-- alice, bob, eve) via 1-load-testdata.sql (USE_DEMO_DATA=True), each with
-- 3 internal contacts + 1 external "External Bank" contact - reliable,
-- stable idRefs available immediately, annotated below with a pre-granted
-- OPTIONAL consent (§1 point 9) and matching processed_data bookkeeping for
-- every field they actually hold (§1 point 11, §8.1.b), mirroring exactly
-- what register_data_subject()/report_processed_data() do at runtime for a
-- real new sign-up.
-- ============================================================================

USE `priam-actor`;

-- data_subject_category_name is varchar(25) - "Bank of Anthos Account
-- Holder" (29 chars) overflows it, hence the abbreviation.
INSERT INTO data_subject_category (data_subject_category_id, data_subject_category_name, location_id)
VALUES (1, 'BoA Account Holder', NULL);

USE `priam-data`;

-- personal_data_category: the 10 default rows (see db_creation_script.sql)
-- have no "financial" or "contact" category (playbook §1 point 3) - add the
-- two this application genuinely needs.
INSERT INTO personal_data_category (personal_data_category_id, personal_data_category_name) VALUES
  (11, 'financial data'),
  (12, 'contact data');

INSERT INTO data_type (data_type_id, data_type_name) VALUES
  (1, 'User'),
  (2, 'Contact');

-- processing: all 4 types (playbook §1 point 6), each tied to a real code
-- path, not invented for completeness:
--   - DEFAULT   'Authentication'                  - userservice.py login():
--     checks username/passhash and issues a JWT. Doesn't even appear in the
--     consent UI (no toggle for DEFAULT).
--   - NECESSARY 'Account Management'               - userservice.py
--     create_user(): the profile fields required to open/operate an
--     account (contract necessity, Art. 6.1.b) - shown pre-checked/disabled
--     in the consent UI, not revocable without losing use of the app.
--   - MANDATORY 'Identity Verification'   - the `ssn` column
--     specifically: distinct legal basis (Art. 6.1.c, legal obligation -
--     banks must retain a KYC identifier) from the contract-necessity basis
--     above, same non-revocable UI treatment. This is also *why* ssn is
--     marked non-erasable in data_usage below (Art. 17(3)(b) exception).
--   - OPTIONAL  'Contact Management'   - frontend.py
--     _add_contact() / contacts.py add_contact(): a user can submit a
--     payment or deposit WITHOUT ever giving a contact a label (the
--     "save as contact" checkbox is optional in the app's own UI/logic
--     already) - the only genuinely optional side effect found in this
--     application's code. Gated by get_consent() (playbook §4) inside
--     contacts.py, without blocking the (mandatory) transaction itself.
-- processing_type in exact UPPERCASE (§8.1.a) - the CHECK constraint's own
-- TitleCase literals ('Default','Mandatory',...) are collation-insensitive
-- and would silently pass MySQL while crashing Hibernate.
INSERT INTO processing (processing_id, processing_name, processing_type, processing_category, created_at, modified_at, ended_at) VALUES
  (1, 'Authentication',                    'DEFAULT',   'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (2, 'Account Management',                'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (3, 'Identity Verification',   'MANDATORY', 'LEGAL_OBLIGATION', CURDATE(), CURDATE(), NULL),
  (4, 'Contact Management',   'OPTIONAL',  'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL);

INSERT INTO purpose (purpose_description, purpose_type, processing_id) VALUES
  ('Authenticate the account holder via username/password and issue a signed JWT session token (userservice.py login())', 'MAIN', 1),
  ('Maintain the identity and profile information required to open and operate a bank account (userservice.py create_user())', 'MAIN', 2),
  ('Retain the account holder''s Social Security Number for regulatory identity verification and tax reporting obligations', 'MAIN', 3),
  ('Store labeled payee/contact accounts chosen by the user to simplify future payments and deposits (frontend.py _add_contact(), contacts.py add_contact())', 'MAIN', 4);

-- data: one row per column actually exposed by the Provider bridge
-- (case-studies/BankOfAnthos/src/accounts/userservice/priam_provider.py).
-- username/accountid are read-only identifiers (u=0,d=0 below), excluded
-- from the writable whitelist on the Provider-bridge side too (defense in
-- depth, same treatment as productId/userName in the OnlineBoutique/
-- TeaStore annotations). is_primary_key=1 on `label` (§8.1.c): `contacts`
-- has no surrogate id column, and label is the only column contacts.py
-- itself enforces as unique per subject (_check_contact_allowed) - the
-- Provider bridge resolves a specific contact row via
-- (username=idRef, label=primaryKeys['label']).
INSERT INTO data (data_id, data_name, `source`, source_details, is_personal, is_portable, is_primary_key, data_type_id, personal_data_category_id, data_subject_category_id) VALUES
  (1,  'username',     'DIRECT',   'accounts-db.users.username (login identifier, immutable)',           1, 0, 0, 1, 4,  1),
  (2,  'accountid',    'PRODUCED', 'accounts-db.users.accountid, generated by UserDb.generate_accountid()', 1, 0, 0, 1, 11, 1),
  (3,  'firstname',    'DIRECT',   'accounts-db.users.firstname',                                          1, 1, 0, 1, 4,  1),
  (4,  'lastname',     'DIRECT',   'accounts-db.users.lastname',                                           1, 1, 0, 1, 4,  1),
  (5,  'birthday',     'DIRECT',   'accounts-db.users.birthday (DATE NOT NULL - no sensible "blank" value, rectify only)', 1, 1, 0, 1, 4, 1),
  (6,  'address',      'DIRECT',   'accounts-db.users.address',                                            1, 1, 0, 1, 12, 1),
  (7,  'state',        'DIRECT',   'accounts-db.users.state',                                              1, 1, 0, 1, 12, 1),
  (8,  'zip',          'DIRECT',   'accounts-db.users.zip',                                                1, 1, 0, 1, 12, 1),
  (9,  'timezone',     'DIRECT',   'accounts-db.users.timezone',                                           1, 0, 0, 1, 12, 1),
  (10, 'ssn',          'DIRECT',   'accounts-db.users.ssn (rectify only - not erasable, Art. 17(3)(b) legal-obligation exception, see processing 3)', 1, 0, 0, 1, 4, 1),
  (11, 'label',        'DIRECT',   'accounts-db.contacts.label (unique per username, contacts.py _check_contact_allowed - used as the composite primary key)', 1, 1, 1, 2, 4, 1),
  (12, 'account_num',  'DIRECT',   'accounts-db.contacts.account_num',                                     1, 1, 0, 2, 11, 1),
  (13, 'routing_num',  'DIRECT',   'accounts-db.contacts.routing_num',                                     1, 1, 0, 2, 11, 1),
  (14, 'is_external',  'DIRECT',   'accounts-db.contacts.is_external',                                     1, 0, 0, 2, 10, 1);

-- data_usage: link data to the processing(s) that actually touch it.
-- username/accountid are read-only (c=0,u=0,d=0) identifiers everywhere
-- they appear. birthday/ssn are rectifiable but not erasable (see the
-- rationale in the `data` rows above). is_external is a boolean flag with
-- no sensible "blank" value either (rectify only).
INSERT INTO data_usage (personal_status, c, r, u, d, data_id, processing_id) VALUES
  (1, 0, 1, 0, 0, 1,  1), -- username (Authentication)
  (1, 0, 1, 0, 0, 2,  2), -- accountid (Account Management)
  (1, 1, 1, 1, 1, 3,  2), -- firstname
  (1, 1, 1, 1, 1, 4,  2), -- lastname
  (1, 1, 1, 1, 0, 5,  2), -- birthday
  (1, 1, 1, 1, 1, 6,  2), -- address
  (1, 1, 1, 1, 1, 7,  2), -- state
  (1, 1, 1, 1, 1, 8,  2), -- zip
  (1, 1, 1, 1, 0, 9,  2), -- timezone
  (1, 0, 1, 1, 0, 10, 3), -- ssn (Identity Verification / KYC)
  (1, 1, 1, 1, 1, 11, 4), -- label (Contact Management)
  (1, 1, 1, 1, 1, 12, 4), -- account_num
  (1, 1, 1, 1, 1, 13, 4), -- routing_num
  (1, 1, 1, 0, 0, 14, 4); -- is_external

-- No personal_data_transfer/secondary_actor (§1 point 12): none of the 7
-- services in this application call an external third party with any of
-- the personal data above (no email/notification provider, no
-- subcontractor - confirmed by reading every service's outbound calls).
-- Conditional annotation, correctly left empty here.

-- data_subject / contract / consent / processed_data for the 4 deterministic
-- demo users (testuser=1, alice=2, bob=3, eve=4 below) - mirrors exactly
-- what register_data_subject()/report_processed_data() do at runtime for a
-- real sign-up (playbook §1 point 8-9-11, §8.1.b). All 4 have every User
-- field (data_id 1-10) and, since 1-load-testdata.sql gives each of them 4
-- contacts, every Contact field too (data_id 11-14) - plus a pre-granted,
-- non-expired OPTIONAL consent (end_date NULL) for Contact Management so
-- the withdraw/re-grant cycle (§3) can be tested immediately without first
-- exercising the runtime consent-grant path.
USE `priam-actor`;
INSERT INTO data_subject (data_subject_id, id_ref, data_subject_category_id) VALUES
  (1, 'testuser', 1),
  (2, 'alice',    1),
  (3, 'bob',      1),
  (4, 'eve',      1);

USE `priam-consent`;
INSERT INTO contract (contract_id, signature_date, expiration_date, data_subject_id) VALUES
  (1, CURDATE(), NULL, 1),
  (2, CURDATE(), NULL, 2),
  (3, CURDATE(), NULL, 3),
  (4, CURDATE(), NULL, 4);

INSERT INTO consent (start_date, end_date, processing_id, contract_id) VALUES
  (NOW(), NULL, 4, 1), -- testuser: Contact Management granted
  (NOW(), NULL, 4, 2), -- alice
  (NOW(), NULL, 4, 3), -- bob
  (NOW(), NULL, 4, 4); -- eve

USE `priam-data`;
INSERT INTO processed_data (data_id, data_subject_id) VALUES
  -- testuser (data_subject_id=1)
  (1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(9,1),(10,1),(11,1),(12,1),(13,1),(14,1),
  -- alice (data_subject_id=2)
  (1,2),(2,2),(3,2),(4,2),(5,2),(6,2),(7,2),(8,2),(9,2),(10,2),(11,2),(12,2),(13,2),(14,2),
  -- bob (data_subject_id=3)
  (1,3),(2,3),(3,3),(4,3),(5,3),(6,3),(7,3),(8,3),(9,3),(10,3),(11,3),(12,3),(13,3),(14,3),
  -- eve (data_subject_id=4)
  (1,4),(2,4),(3,4),(4,4),(5,4),(6,4),(7,4),(8,4),(9,4),(10,4),(11,4),(12,4),(13,4),(14,4);

-- HOW TO ACTIVATE: this file is baked into the `mysqldb` image at build time
-- (see Databases/Dockerfile) and only runs on a virgin MySQL volume
-- (docker-entrypoint-initdb.d convention) - rebuild the image
-- (`docker compose build mysqldb`) and clear the volume (db-volume/) after
-- editing this file for changes to apply.
--
-- Dynamically registered subjects (any user who signs up through
-- POST /users after this integration's hooks are wired) are NOT seeded
-- here - register_data_subject()/report_processed_data() (playbook §4bis,
-- case-studies/BankOfAnthos/src/accounts/userservice/priam.py) create their
-- data_subject/processed_data rows at runtime instead. See
-- priam-integration/backfill-data-subjects.py for the one-off script that
-- retroactively covers the 4 demo users above through the real runtime
-- path too (idempotent - safe to run even though this script already seeded
-- them directly).
