-- ============================================================================
-- PRIAM annotation for Mastodon (Ruby on Rails + PostgreSQL, ActivityPub
-- federated social network). See Docs/PRIAM-INTEGRATION-PLAYBOOK.md §1.
--
-- Real schema referenced (not invented) - verified directly in the target
-- app's source, case-studies/Mastodon/:
--   - db/schema.rb "users" (Devise auth: email, encrypted_password,
--     sign_up_ip, locale, time_zone) and "accounts" (ActivityPub actor /
--     profile: username, display_name, note) - joined 1:1 via
--     users.account_id -> accounts.id (app/models/user.rb:72
--     `belongs_to :account`).
--   - db/schema.rb "statuses" (posts/toots, several rows per subject,
--     account_id FK) - created in app/services/post_status_service.rb.
--   - db/schema.rb "web_push_subscriptions" (Web Push registrations,
--     several rows per subject, user_id FK, endpoint/key_p256dh/key_auth) -
--     created in app/controllers/api/web/push_subscriptions_controller.rb,
--     genuinely optional (the app is fully usable without ever enabling
--     push notifications) and a real transfer of personal data (the push
--     endpoint URL + encryption keys) to the browser vendor's push
--     relay (Google FCM / Mozilla autopush / Apple Push, depending on the
--     browser) - hence the personal_data_transfer/secondary_actor
--     annotation below (playbook §1 point 12).
--
-- idRef: accounts.username (local accounts only, domain IS NULL) - a
-- stable, human-chosen string, non-numeric by construction (playbook §7),
-- NOT users.id/accounts.id (accounts.id is a Snowflake-style bigint -
-- timestamp_id() - and users.id a plain autoincrement bigint, both numeric
-- and neither stable as a public-facing handle the way username is:
-- Mastodon does not allow renaming a local account's username after
-- creation, app/models/account.rb `validates_with UniqueUsernameValidator,
-- if: -> { will_save_change_to_username? }`).
--
-- No MANDATORY processing annotated: unlike Bank of Anthos (SSN/KYC,
-- Art. 6.1.c), nothing in Mastodon's own code is processed under a
-- distinct legal obligation rather than contract necessity or consent -
-- not invented here for the sake of covering all 4 processing_type values
-- (playbook §1 point 6 lists them as available, not mandatory to use all).
--
-- Seed subject: no default/demo local account ships with a fresh Mastodon
-- checkout. "priam_seed" was chosen as the username *before* running the
-- application (valid per app/models/account.rb USERNAME_RE, non-numeric),
-- then actually registered through the application's own sign-up
-- (POST /auth or the real registration form) to make it a real, stable
-- row - see priam-integration/ETAPES-FAITES.md for exactly how it was
-- created. Only the username (chosen, not discovered) is needed here:
-- unlike Habitica/Bank of Anthos, no Mastodon-internal numeric id
-- (accounts.id/users.id/statuses.id) needs to be hardcoded in this script,
-- since data_subject.id_ref stores the username directly and
-- processed_data only needs to reference PRIAM's own auto-increment
-- data_subject_id (1, the first row inserted below).
--
-- Deliberately NOT pre-seeded here (left to be exercised live during
-- testing instead, per playbook §7's real-workflow requirement):
--   - Any `statuses` row (posting is a live, user-initiated action - the
--     first real toot is posted during testing itself, see
--     ETAPES-FAITES.md, and report_processed_data() is what makes it show
--     up on the Access Request page - the exact mechanism under test).
--   - The OPTIONAL "Push Notifications" consent/processed_data - granted
--     live during testing (§3/§7's grant/withdraw/re-grant cycle), not
--     pre-seeded, since there is genuinely no push subscription yet for a
--     freshly registered subject.
-- ============================================================================

USE `priam-actor`;

INSERT INTO data_subject_category (data_subject_category_id, data_subject_category_name, location_id)
VALUES (1, 'Mastodon User', NULL);

-- personal_data_transfer/secondary_actor (§1 point 12): a Web Push
-- subscription's endpoint/keys are genuinely relayed through the browser
-- vendor's push service as soon as push notifications are enabled
-- (Web Push protocol, RFC 8030 - the endpoint URL itself points at
-- Google/Mozilla/Apple's infrastructure, not Mastodon's).
-- No `country` row for the USA exists in the 10 EU defaults shipped by
-- db_creation_script.sql - added here (id 200, matching the id already
-- used for this exact purpose by the Habitica integration, no collision
-- since only one case study's script is ever loaded into a virgin volume
-- at a time - playbook §5), adequate=false.
INSERT INTO country (country_id, country_name, minor_age, adequate) VALUES
  (200, 'United States', 16, false);

INSERT INTO address (address_id, street_number, street_name, postal_code, city, complement) VALUES
  (1, '1600', 'Amphitheatre Pkwy', '94043', 'Mountain View', 'Google LLC / Mozilla Corp. / Apple Inc. (Web Push relay services)');

INSERT INTO secondary_actor_category (secondary_actor_category_id, secondary_actor_category_name) VALUES
  (1, 'Web Push Relay Service');

INSERT INTO secondary_actor (secondary_actor_id, secondary_actor_type, secondary_actor_name, address_id, secondary_actor_phone, secondary_actor_email, safeguard, safeguard_type, secondary_actor_category_id, country_id) VALUES
  (1, 'DATA_PROCESSOR', 'Google FCM / Mozilla / Apple', 1, NULL, NULL,
     'Standard Contractual Clauses (per-vendor Web Push service terms)',
     'CONTRACTUAL_CLAUSE', 1, 200);

USE `priam-data`;

-- personal_data_category: the 10 default rows (db_creation_script.sql) have
-- no "contact" category (playbook §1 point 3) - add the one this
-- application genuinely needs (email). display_name/note/status text reuse
-- the existing default "Profil data" (id 7); id/ip/push-endpoint columns
-- reuse "identification data" (id 4).
INSERT INTO personal_data_category (personal_data_category_id, personal_data_category_name) VALUES
  (11, 'contact data');

INSERT INTO data_type (data_type_id, data_type_name) VALUES
  (1, 'User'),
  (2, 'Status'),
  (3, 'PushSubscription');

-- processing: 3 of the 4 types actually exercised by real Mastodon code
-- paths (playbook §1 point 6) - no MANDATORY (see header note above).
--   - DEFAULT   'Authentication'      - Devise (app/models/user.rb) login
--     via email + encrypted_password, no consent toggle.
--   - NECESSARY 'Account Management'  - app/controllers/auth/
--     registrations_controller.rb sign-up + app/models/user.rb: the
--     profile/auth fields required to use the app (contract necessity,
--     Art. 6.1.b).
--   - NECESSARY 'Posting'             - app/services/post_status_service.rb:
--     the core feature (publishing toots), required to use the app.
--   - OPTIONAL  'Push Notifications'  - app/controllers/api/web/
--     push_subscriptions_controller.rb#create: gated by get_consent()
--     (playbook §4), the only optional side effect found in the app's own
--     code that also transfers data to an external third party.
-- processing_type/purpose_type in exact UPPERCASE (§8.1.a).
INSERT INTO processing (processing_id, processing_name, processing_type, processing_category, created_at, modified_at, ended_at) VALUES
  (1, 'Authentication',     'DEFAULT',   'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (2, 'Account Management', 'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (3, 'Posting',            'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (4, 'Push Notifications', 'OPTIONAL',  'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL);

INSERT INTO purpose (purpose_description, purpose_type, processing_id) VALUES
  ('Authenticate the user via email + password and start a session (Devise, app/models/user.rb)', 'MAIN', 1),
  ('Maintain the account profile (email, locale, time zone, display name, bio) required to use Mastodon (app/controllers/auth/registrations_controller.rb, app/models/account.rb)', 'MAIN', 2),
  ('Store and publish the posts (toots) the user authors (app/services/post_status_service.rb)', 'MAIN', 3),
  ('Register a Web Push subscription (endpoint + encryption keys) to deliver notifications through the browser vendor push relay (app/controllers/api/web/push_subscriptions_controller.rb)', 'MAIN', 4);

-- data: one row per field actually exposed by the Provider bridge
-- (app/controllers/api/priam_provider_controller.rb WHITELISTS).
-- is_primary_key=1 on the id column of every multi-row-per-subject type
-- (§8.1.c): Status.id and PushSubscription.subscriptionId (renamed from
-- the physical `web_push_subscriptions.id` to avoid a dataName collision
-- with Status.id on the 4th Provider endpoint, dataValue, which receives
-- no dataTypeName and must infer the type from dataName alone - see the
-- Provider bridge controller comment). Both marked is_personal=1
-- (otherwise never included in value lists, §8.1.c) and source=DIRECT (not
-- PRODUCED/INDIRECT, which would gate them behind an acceptance guard).
--
-- Scope decisions (documented, not silent - see
-- priam-integration/INTEGRATION-REPORT.md for the full rationale):
--   - `email` is rectifiable but NOT erasable (d=0 in data_usage below) -
--     Devise requires a present, valid-format email for login
--     (app/models/user.rb `validates :email, presence: true,
--     email_address: true`); blanking it would break the account's own
--     ability to authenticate, not a realistic "erase while keeping the
--     account usable" scenario.
--   - `sign_up_ip` is read-only (u=0, d=0) - a registration-time security/
--     abuse-prevention record (IP-based sign-up throttling,
--     app/models/user.rb `sign_up_from_ip_requires_approval?`), not a
--     field a data subject rectifies/erases in place.
INSERT INTO data (data_id, data_name, `source`, source_details, is_personal, is_portable, is_primary_key, data_type_id, personal_data_category_id, data_subject_category_id) VALUES
  (1,  'email',          'DIRECT', 'db/schema.rb users.email',                                            1, 1, 0, 1, 11, 1),
  (2,  'sign_up_ip',     'DIRECT', 'db/schema.rb users.sign_up_ip',                                       1, 0, 0, 1, 4,  1),
  (3,  'locale',         'DIRECT', 'db/schema.rb users.locale',                                           1, 1, 0, 1, 7,  1),
  (4,  'time_zone',      'DIRECT', 'db/schema.rb users.time_zone',                                        1, 1, 0, 1, 7,  1),
  (5,  'display_name',   'DIRECT', 'db/schema.rb accounts.display_name',                                  1, 1, 0, 1, 7,  1),
  (6,  'note',           'DIRECT', 'db/schema.rb accounts.note',                                          1, 1, 0, 1, 7,  1),
  (7,  'id',              'DIRECT', 'db/schema.rb statuses.id (bigint, primary key for rectification/erasure - §8.1.c)', 1, 0, 1, 2, 4, 1),
  (8,  'text',            'DIRECT', 'db/schema.rb statuses.text',                                          1, 1, 0, 2, 7,  1),
  (9,  'spoiler_text',    'DIRECT', 'db/schema.rb statuses.spoiler_text',                                  1, 1, 0, 2, 7,  1),
  (10, 'language',        'DIRECT', 'db/schema.rb statuses.language',                                      1, 1, 0, 2, 7,  1),
  (11, 'subscriptionId',  'DIRECT', 'db/schema.rb web_push_subscriptions.id (bigint, primary key for rectification/erasure - §8.1.c; renamed from `id` on the Provider bridge to avoid a dataName collision with Status.id on the dataValue endpoint)', 1, 0, 1, 3, 4, 1),
  (12, 'endpoint',        'DIRECT', 'db/schema.rb web_push_subscriptions.endpoint',                        1, 0, 0, 3, 4,  1),
  (13, 'key_p256dh',      'DIRECT', 'db/schema.rb web_push_subscriptions.key_p256dh',                      1, 0, 0, 3, 4,  1),
  (14, 'key_auth',        'DIRECT', 'db/schema.rb web_push_subscriptions.key_auth',                        1, 0, 0, 3, 4,  1);

-- data_usage: link data to the processing(s) that actually touch it.
-- email also usable read-only at Authentication (login by email+password).
INSERT INTO data_usage (personal_status, c, r, u, d, data_id, processing_id) VALUES
  (1, 0, 1, 0, 0, 1,  1), -- email (Authentication, read-only usage)
  (1, 1, 1, 1, 0, 1,  2), -- email (Account Management - not erasable, see scope note above)
  (1, 1, 1, 0, 0, 2,  2), -- sign_up_ip (read-only, see scope note above)
  (1, 1, 1, 1, 1, 3,  2), -- locale
  (1, 1, 1, 1, 1, 4,  2), -- time_zone
  (1, 1, 1, 1, 1, 5,  2), -- display_name
  (1, 1, 1, 1, 1, 6,  2), -- note
  (1, 1, 1, 0, 0, 7,  3), -- Status.id (primary key, not itself rectifiable/erasable)
  (1, 1, 1, 1, 1, 8,  3), -- Status.text
  (1, 1, 1, 1, 1, 9,  3), -- Status.spoiler_text
  (1, 1, 1, 1, 1, 10, 3), -- Status.language
  (1, 1, 1, 0, 0, 11, 4), -- PushSubscription.subscriptionId (primary key)
  (1, 1, 1, 0, 1, 12, 4), -- PushSubscription.endpoint (not rectifiable, see Provider bridge - only create/read/erase)
  (1, 1, 1, 0, 1, 13, 4), -- PushSubscription.key_p256dh
  (1, 1, 1, 0, 1, 14, 4); -- PushSubscription.key_auth

INSERT INTO personal_data_transfer (Personal_data_transfer_id, processing_id) VALUES (1, 4);
INSERT INTO personal_data_transfer_secondary_actor (Personal_data_transfer_id, secondary_actor_id) VALUES (1, 1);
INSERT INTO personal_data_transfer_data (personal_data_transfer_id, data_id) VALUES (1, 11), (1, 12), (1, 13), (1, 14);

-- data_subject / contract / consent / processed_data for the one real seed
-- subject (playbook §1 point 8-9-11, §8.1.b). Only the "User" type
-- (email/sign_up_ip/locale/time_zone/display_name/note) is pre-seeded as
-- processed_data, since the account+profile rows genuinely exist the
-- instant sign-up completes. "Status" and "PushSubscription" are
-- deliberately left unseeded (see header note) - report_processed_data()
-- and the OPTIONAL consent grant/withdraw cycle for them are exercised
-- live during testing instead (priam-integration/ETAPES-FAITES.md).
USE `priam-actor`;
INSERT INTO data_subject (data_subject_id, id_ref, data_subject_category_id) VALUES
  (1, 'priam_seed', 1);

USE `priam-consent`;
INSERT INTO contract (contract_id, signature_date, expiration_date, data_subject_id) VALUES
  (1, CURDATE(), NULL, 1);

USE `priam-data`;
INSERT INTO processed_data (data_id, data_subject_id, nb_occurrences) VALUES
  (1, 1, 1), (2, 1, 1), (3, 1, 1), (4, 1, 1), (5, 1, 1), (6, 1, 1);
