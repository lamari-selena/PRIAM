-- ============================================================================
-- PRIAM annotation for Ghostfolio (open-source personal wealth/portfolio
-- tracker - NestJS/Prisma/PostgreSQL API + Angular client, served from the
-- same process on :3333). See Docs/PRIAM-INTEGRATION-PLAYBOOK.md §1.
--
-- ----------------------------------------------------------------------------
-- Real schema referenced (not invented) - verified directly in
-- case-studies/Ghostfolio/prisma/schema.prisma:
--
--   - idRef = User.id (Prisma `String @id @default(uuid())`, Postgres table
--     "User") - always a non-numeric UUID by construction (playbook §7 "non-
--     numeric idRef" requirement satisfied for every real subject, not just a
--     specially crafted one). Ghostfolio's User model carries NO email and NO
--     password at all (see UserService.createUser,
--     apps/api/src/app/user/user.service.ts) - identity is either an opaque,
--     hashed `accessToken` (anonymous sign-up) or `provider`/`thirdPartyId`
--     (Google/OIDC sign-in). This shapes the whole annotation below: there is
--     no "contact data" category to add, and rectification/erasure of
--     enum-typed or system-managed fields (User.role, User.provider,
--     Order.type, every *.id primary key, every required timestamp) is
--     deliberately left access-only - see the Provider bridge's own MUTABLE
--     whitelist (apps/api/src/app/provider-bridge/provider-bridge.controller.ts),
--     which this script's data_usage c/r/u/d flags mirror exactly so PRIAM's
--     UI never offers a rectify/erase action that the bridge would 400 on.
--   - `User` data_type (Postgres table "User", exactly one row per subject):
--     id, provider, thirdPartyId, createdAt.
--   - `Account` data_type (table "Account", several rows per subject -
--     composite Prisma PK `[id, userId]`, but the Provider bridge only needs
--     `id` to disambiguate since `idRef` already scopes to the subject's own
--     userId): id, name, currency, balance. UserService.createUser() always
--     creates one default Account transactionally alongside the User row
--     itself - the "processed data reported right at sign-up" case the
--     playbook's §4bis race-condition warning (§8.6) is about.
--   - `Order` data_type (table "Order", activities/transactions, several rows
--     per subject): id, type, currency, quantity, unitPrice, fee, date,
--     comment. Created by ActivitiesService.createActivity
--     (apps/api/src/app/activities/activities.service.ts).
--   - `Analytics` data_type (table "Analytics", exactly one row per subject,
--     1:1 PK = userId): country, activityCount, lastRequestAt. This is the one
--     genuinely OPTIONAL processing found in Ghostfolio's own code:
--     JwtStrategy.validate/ApiKeyStrategy.validate
--     (apps/api/src/app/auth/{jwt,api-key}.strategy.ts) only upsert this row
--     when `ENABLE_FEATURE_SUBSCRIPTION` is on, and core authentication works
--     identically whether or not it runs - confirmed by reading every call
--     site (the upsert result is never used to gate the rest of `validate()`).
--     Gated by get_consent() (playbook §4) in both strategy files.
--   - No "financial data" category exists among the 10 defaults
--     (Databases/db_creation_script.sql) - added below (id 11) per playbook
--     §1 point 3, used for Account.currency/balance and every Order money
--     field. "identification data" (4) and "Profil data" (7, existing
--     defaults) cover the rest.
--
-- idRef non-numeric proof: a real account was created through the running
-- application's own POST /api/v1/user (anonymous sign-up) before finalizing
-- this script - see priam-integration/ETAPES-FAITES.md, "Seed account
-- capture" for the exact request/response. The id_ref seeded below is that
-- real, observed UUID, not a placeholder.
--
-- No MANDATORY processing annotated: nothing in Ghostfolio's own code is
-- processed under a distinct legal obligation rather than contract necessity
-- or consent - not invented here for the sake of covering all 4
-- processing_type values (playbook §1 point 6 lists them as available, not
-- mandatory to use all).
--
-- No personal_data_transfer/secondary_actor (§1 point 12): within the scope
-- annotated here, Ghostfolio never sends User/Account/Order/Analytics data to
-- an external third party - market-data providers (Yahoo, Alpha Vantage, ...)
-- only ever receive security symbols/dates, never anything modeled as
-- personal data below. Subscription (Stripe billing) is deliberately NOT
-- annotated in this pass - out of scope for this integration session, to be
-- added (with its own personal_data_transfer row, since Stripe is a genuine
-- external processor) if/when Subscription-related rights are exercised.
--
-- No consent pre-granted for Usage Analytics (the OPTIONAL processing): unlike
-- a NECESSARY/DEFAULT processing, seeding a fake "already granted" state here
-- would require also faking an Analytics row and its processed_data
-- bookkeeping (§8.1.b) that does not actually exist yet for a subject who has
-- never made an authenticated request. This integration instead tests the
-- full, real cycle (grant -> verify Analytics upserts -> withdraw -> verify it
-- stops -> re-grant -> verify it resumes) - see priam-integration/
-- ETAPES-FAITES.md - a stronger proof than pre-seeding, and the same
-- deliberate choice already made in case-studies/OnlineBoutique's own
-- db_insertion_script.sql for its own OPTIONAL processing.
-- ============================================================================

USE `priam-actor`;

INSERT INTO data_subject_category (data_subject_category_id, data_subject_category_name, location_id)
VALUES (1, 'Ghostfolio Investor', NULL);

USE `priam-data`;

INSERT INTO personal_data_category (personal_data_category_id, personal_data_category_name) VALUES
  (11, 'financial data');

INSERT INTO data_type (data_type_id, data_type_name) VALUES
  (1, 'User'),
  (2, 'Account'),
  (3, 'Order'),
  (4, 'Analytics');

-- data: is_primary_key=1 on Account.id/Order.id (playbook §1 point 5 /
-- §8.1.c) - both have several rows per subject; User/Analytics have exactly
-- one row per subject (primaryKeys stays empty {} for those two types,
-- matching §2's "the subject table itself" case). is_primary_key columns are
-- access-only below (data_usage c/u/d=0) - not exposed for rectification/
-- erasure by the Provider bridge (mutating a row's own id would break FK
-- relations, e.g. Order.accountId -> Account.id), same scope decision as
-- Habitica's Task.id in this repository's own prior integration.
INSERT INTO data (data_id, data_name, `source`, source_details, is_personal, is_portable, is_primary_key, data_type_id, personal_data_category_id, data_subject_category_id) VALUES
  (1,  'id',            'DIRECT', 'Postgres "User".id (primary key, uuid, = idRef itself)',                         1, 0, 0, 1, 4,  1),
  (2,  'provider',      'DIRECT', 'Postgres "User".provider (enum ANONYMOUS/GOOGLE/OIDC/INTERNET_IDENTITY)',        1, 1, 0, 1, 4,  1),
  (3,  'thirdPartyId',  'DIRECT', 'Postgres "User".thirdPartyId (external OAuth subject id, nullable)',             1, 1, 0, 1, 4,  1),
  (4,  'createdAt',     'DIRECT', 'Postgres "User".createdAt',                                                      1, 1, 0, 1, 7,  1),
  (5,  'id',            'DIRECT', 'Postgres "Account".id (several rows per subject, composite PK with userId)',     1, 0, 1, 2, 4,  1),
  (6,  'name',          'DIRECT', 'Postgres "Account".name (user-chosen label, e.g. the default "My Account")',     1, 1, 0, 2, 7,  1),
  (7,  'currency',      'DIRECT', 'Postgres "Account".currency',                                                    1, 1, 0, 2, 11, 1),
  (8,  'balance',       'DIRECT', 'Postgres "Account".balance',                                                     1, 1, 0, 2, 11, 1),
  (9,  'id',            'DIRECT', 'Postgres "Order".id (several rows per subject, one per placed activity)',        1, 0, 1, 3, 4,  1),
  (10, 'type',          'DIRECT', 'Postgres "Order".type (enum BUY/SELL/FEE/INTEREST/LIABILITY/...)',               1, 1, 0, 3, 11, 1),
  (11, 'currency',      'DIRECT', 'Postgres "Order".currency',                                                      1, 1, 0, 3, 11, 1),
  (12, 'quantity',      'DIRECT', 'Postgres "Order".quantity',                                                      1, 1, 0, 3, 11, 1),
  (13, 'unitPrice',     'DIRECT', 'Postgres "Order".unitPrice',                                                     1, 1, 0, 3, 11, 1),
  (14, 'fee',           'DIRECT', 'Postgres "Order".fee',                                                           1, 1, 0, 3, 11, 1),
  (15, 'date',          'DIRECT', 'Postgres "Order".date',                                                          1, 1, 0, 3, 11, 1),
  (16, 'comment',       'DIRECT', 'Postgres "Order".comment (free-text note, nullable)',                            1, 1, 0, 3, 7,  1),
  (17, 'country',       'DIRECT', 'Postgres "Analytics".country (derived from the request timezone header)',       1, 1, 0, 4, 7,  1),
  (18, 'activityCount', 'DIRECT', 'Postgres "Analytics".activityCount',                                             1, 1, 0, 4, 7,  1),
  (19, 'lastRequestAt', 'DIRECT', 'Postgres "Analytics".lastRequestAt',                                             1, 1, 0, 4, 7,  1);

-- processing: playbook §1 point 6 - only the types genuinely justified by
-- this application's own code (see header comment above). UPPERCASE (§8.1.a).
INSERT INTO processing (processing_id, processing_name, processing_type, processing_category, created_at, modified_at, ended_at) VALUES
  (1, 'Authentication',        'DEFAULT',   'LEGITIMATE_INTEREST', CURDATE(), CURDATE(), NULL),
  (2, 'Portfolio Management',  'NECESSARY', 'CONSENT_CONTRACT',    CURDATE(), CURDATE(), NULL),
  (3, 'Usage Analytics',       'OPTIONAL',  'CONSENT_CONTRACT',    CURDATE(), CURDATE(), NULL);

INSERT INTO purpose (purpose_description, purpose_type, processing_id) VALUES
  ('Authenticate the data subject via JWT bearer tokens / OAuth so they can access their own portfolio (JwtStrategy.validate, AuthService.validateOAuthLogin)', 'MAIN', 1),
  ('Manage the subject''s investment accounts and record their buy/sell/fee activities (UserService.createUser default Account, AccountService.createAccount, ActivitiesService.createActivity)', 'MAIN', 2),
  ('Track feature usage (activity count, country, last request) to inform product decisions - only performed when ENABLE_FEATURE_SUBSCRIPTION is on (JwtStrategy.validate / ApiKeyStrategy.validate)', 'MAIN', 3);

-- data_usage: link data to the processing(s) that actually touch it.
-- c/r/u/d mirrors provider-bridge.controller.ts's MUTABLE whitelist exactly -
-- id/provider/type/date/createdAt/lastRequestAt stay read-only (r=1 only).
INSERT INTO data_usage (personal_status, c, r, u, d, data_id, processing_id) VALUES
  (1, 0, 1, 0, 0, 1,  1), -- User.id            (Authentication, read-only)
  (1, 0, 1, 0, 0, 2,  1), -- User.provider      (Authentication, read-only)
  (1, 1, 1, 1, 1, 3,  1), -- User.thirdPartyId  (Authentication)
  (1, 0, 1, 0, 0, 4,  1), -- User.createdAt     (Authentication, read-only)
  (1, 0, 1, 0, 0, 5,  2), -- Account.id         (Portfolio Management, read-only)
  (1, 1, 1, 1, 1, 6,  2), -- Account.name       (Portfolio Management)
  (1, 1, 1, 1, 1, 7,  2), -- Account.currency   (Portfolio Management)
  (1, 1, 1, 1, 1, 8,  2), -- Account.balance    (Portfolio Management)
  (1, 0, 1, 0, 0, 9,  2), -- Order.id           (Portfolio Management, read-only)
  (1, 0, 1, 0, 0, 10, 2), -- Order.type         (Portfolio Management, read-only)
  (1, 1, 1, 1, 1, 11, 2), -- Order.currency     (Portfolio Management)
  (1, 1, 1, 1, 1, 12, 2), -- Order.quantity     (Portfolio Management)
  (1, 1, 1, 1, 1, 13, 2), -- Order.unitPrice    (Portfolio Management)
  (1, 1, 1, 1, 1, 14, 2), -- Order.fee          (Portfolio Management)
  (1, 0, 1, 0, 0, 15, 2), -- Order.date         (Portfolio Management, read-only)
  (1, 1, 1, 1, 1, 16, 2), -- Order.comment      (Portfolio Management)
  (1, 1, 1, 1, 1, 17, 3), -- Analytics.country        (Usage Analytics)
  (1, 1, 1, 1, 1, 18, 3), -- Analytics.activityCount  (Usage Analytics)
  (1, 0, 1, 0, 0, 19, 3); -- Analytics.lastRequestAt  (Usage Analytics, read-only)

-- No personal_data_transfer/secondary_actor (§1 point 12): see header
-- comment - conditional annotation, correctly left empty here.

-- ----------------------------------------------------------------------------
-- Seed data subject: a real account created through the running
-- application's own POST /api/v1/user (anonymous sign-up), so the
-- processed_data rows below mirror exactly what register_data_subject()/
-- report_processed_data() do at runtime for any real visitor (playbook §1
-- point 8-9-11, §8.1.b) - see priam-integration/ETAPES-FAITES.md, "Seed
-- account capture" for the exact commands/responses.
-- ----------------------------------------------------------------------------
USE `priam-actor`;
INSERT INTO data_subject (data_subject_id, id_ref, data_subject_category_id) VALUES
  (1, 'b4f64a6c-8681-4444-be31-7a1ebc93bb97', 1);

USE `priam-data`;
-- processed_data bookkeeping (§8.1.b): required for the seed subject's
-- User/Account fields to show up in the Access Request list at all. Mirrors
-- exactly the data_ids PriamService.onUserRegistered() reports at runtime
-- for this same subject (one sign-up = one User row + one default Account).
-- No Order/Analytics rows here - see header comment (tested live instead).
INSERT INTO processed_data (data_id, data_subject_id) VALUES
  (1, 1), -- User.id
  (2, 1), -- User.provider
  (3, 1), -- User.thirdPartyId
  (4, 1), -- User.createdAt
  (5, 1), -- Account.id
  (6, 1), -- Account.name
  (7, 1), -- Account.currency
  (8, 1); -- Account.balance

-- HOW TO ACTIVATE: this file is baked into the `mysqldb` image at build time
-- (see Databases/Dockerfile) and only runs on a virgin MySQL volume
-- (docker-entrypoint-initdb.d convention) - rebuild the image
-- (`docker compose build mysqldb`) and clear the volume (db-volume/) after
-- editing this file for changes to apply.
--
-- Any other, pre-existing account created before this integration's hooks
-- were wired (there were none for this application - see priam-integration/
-- INTEGRATION-REPORT.md, "Backfill") or any dynamically registered subject
-- (any real visitor who signs up after this integration's hooks are wired) is
-- NOT seeded here - PriamService.onUserRegistered()/reportProcessedData()
-- (playbook §4bis, apps/api/src/services/priam/priam.service.ts) create their
-- data_subject/processed_data rows at runtime instead.
