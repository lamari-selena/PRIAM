-- ============================================================================
-- PRIAM annotation for TeaStore (Descartes Research's TeaStore - a Java/
-- Spring microservices e-commerce benchmark app: registry, persistence, auth,
-- image, recommender, webui). See Docs/PRIAM-INTEGRATION-PLAYBOOK.md §1.
--
-- ----------------------------------------------------------------------------
-- Real schema referenced (not invented) - verified directly in the target
-- app's source, case-studies/TeaStore/services/:
--
--   - Persistence uses EclipseLink (not Hibernate) with
--     eclipselink.ddl-generation=create-tables and no @Table annotations, so
--     the physical table name is the entity class's simple name:
--     `PersistenceUser` and `PersistenceOrder`
--     (tools.descartes.teastore.persistence.domain), database `teadb`
--     (persistence.xml). `PersistenceOrderItem` (product_id/quantity) is NOT
--     annotated below: it carries no personal data of its own (a quantity and
--     a product reference), only Order does.
--   - TeaStore ships NO public sign-up UI/endpoint (verified: no "register"
--     hit anywhere in webui/auth source) - only DataGenerator-seeded accounts
--     (`user0..user{N-1}`, password "password" for all,
--     DataGenerator.generateUsers/generateOrders). A minimal sign-up flow
--     (RegisterServlet/register.jsp + AuthUserActionsRest "register") was
--     added on the TeaStore side as part of this integration (documented in
--     priam-integration/INTEGRATION-REPORT.md) - required to have a real
--     user-creation point to wire register_data_subject()/forced consent
--     into, per the playbook §4bis.
--   - idRef = PersistenceUser.userName (a free-form string chosen at sign-up,
--     e.g. "user1" for the DataGenerator-seeded accounts) - NOT the numeric
--     internal `id` - satisfying the playbook §7 "non-numeric idRef" test
--     requirement by construction, and resolved to the numeric internal id
--     only where TeaStore's own code needs it (SessionBlob.uid), via the
--     already-existing `GET persistence/rest/users/name/{name}` endpoint
--     (UserEndpoint.findById).
--   - The one genuinely OPTIONAL processing found/added in this application:
--     CartServlet.java's personalized "Advertisment" block (calls
--     LoadBalancedRecommenderOperations.getRecommendations against the
--     shopper's own order history) - the cart/checkout flow works perfectly
--     without it (confirmed by reading the call site: on no consent, the ad
--     list is simply left empty, checkout is entirely unaffected). Gated by
--     get_consent() (playbook §4).
--   - `Account Management` (NECESSARY): userName/email/realName, used to
--     identify the customer across sessions and correlate them with their
--     own orders (ProfileServlet, AuthUserActionsRest.login) - core account
--     functionality, Art. 6.1.b, not revocable without losing the ability to
--     log in.
--   - `Order Fulfillment` (NECESSARY): the Order's address/credit-card
--     fields, collected at checkout (AuthUserActionsRest.placeOrder) to ship
--     goods and process payment - Art. 6.1.b, same non-revocable treatment.
--   - `Product Recommendations` (OPTIONAL, purpose_type SECONDARY): reuses
--     only the Order primary key (i.e. "how many/which orders this shopper
--     has placed", the profiling signal behind personalized recommendations)
--     for a second, non-essential purpose distinct from the MAIN purpose
--     (shipping/payment) the same Order rows were originally collected for -
--     the textbook justification for purpose_type=SECONDARY (playbook §1
--     point 6/7). Address/credit-card fields stay tied to Order Fulfillment
--     only (data_usage below), never linked to this OPTIONAL processing.
--
-- No MANDATORY processing annotated: nothing in TeaStore's own code is
-- processed under a distinct legal obligation rather than contract necessity
-- or consent - not invented here for the sake of covering all 4
-- processing_type values (playbook §1 point 6 lists them as available, not
-- mandatory to use all). No personal_data_transfer/secondary_actor either
-- (playbook §1 point 12): every processing above stays entirely internal to
-- TeaStore's own microservices (persistence/auth/recommender), no external
-- third party ever receives this data.
--
-- personal_data_category: the 10 default rows (db_creation_script.sql) have
-- no "contact"/"financial" category - added below (ids 11/12) per playbook
-- §1 point 3.
--
-- Deliberate scope decision, documented rather than glossed over: this
-- script seeds ONLY the schema annotation (data_subject_category, data_type,
-- data, personal_data_category, processing, purpose, data_usage) plus ONE
-- `data_subject` row for a real, stable, pre-existing seeded account
-- (`user1`) - no `contract`/`consent`/`processed_data` rows are pre-seeded.
-- Reasons, verified by reading the real code rather than assumed:
--   1. `DataGenerator.generateOrders` seeds each user with a RANDOM number of
--      orders (`random.nextInt(maxOrdersPerUser + 1)`) drawn from a `Random`
--      instance shared, unsynchronized, across a `parallelStream()` - the
--      exact order ids/count for `user1` are not reliably predictable ahead
--      of a real container start, so hardcoding an Order-linked
--      `processed_data` row here would be guessing, not verifying (unlike
--      the User-type fields, which exist exactly once per subject with no
--      such ambiguity).
--   2. `ConsentServiceImpl.create` (the real grant/withdraw toggle, verified
--      in PRIAM-Consent-Service source) already calls
--      `addProcessedData`/`removeProcessedData` for the toggled processing's
--      own `data_usage`-linked data_ids AUTOMATICALLY, on every real
--      grant/withdraw call - pre-seeding a "granted" consent here would only
--      save one API call and would reintroduce exactly the §8.1.b pitfall
--      this same playbook warns about (a pre-seeded consent with no matching
--      processed_data row breaks the first withdrawal) for no real benefit.
--   3. This integration instead tests the full, real 3-phase cycle (grant ->
--      verify the Cart page's ad block appears -> withdraw -> verify it
--      disappears -> re-grant -> verify it reappears), a stronger proof than
--      starting from a pre-seeded "already granted" state - see
--      priam-integration/ETAPES-FAITES.md.
--   4. Pre-existing seeded users (`user0..user{N-1}`, including `user1`) are
--      instead caught up by the one-off backfill script
--      (priam-integration/backfill-data-subjects.py), run once against the
--      live stack, which queries the REAL persistence service for each
--      user's real orders and reports real data_ids - no guessing involved.
-- ============================================================================

USE `priam-actor`;

INSERT INTO data_subject_category (data_subject_category_id, data_subject_category_name, location_id)
VALUES (1, 'TeaStore Customer', NULL);

USE `priam-data`;

INSERT INTO personal_data_category (personal_data_category_id, personal_data_category_name) VALUES
  (11, 'contact data'),
  (12, 'financial data');

INSERT INTO data_type (data_type_id, data_type_name) VALUES
  (1, 'User'),
  (2, 'Order');

-- data: is_primary_key=1 on Order.id (playbook §1 point 5 / §8.1.c) - Order
-- has several rows per subject (one per placed order), User has exactly one
-- (the subject's own account row), so no primary key is needed for User
-- (primaryKeys stays empty {} for that type, matching §2's "the subject
-- table itself" case).
INSERT INTO data (data_id, data_name, `source`, source_details, is_personal, is_portable, is_primary_key, data_type_id, personal_data_category_id, data_subject_category_id) VALUES
  (1,  'userName',             'DIRECT', 'teadb.PersistenceUser.userName (persistence service, EclipseLink default table naming)',  1, 1, 0, 1, 4,  1),
  (2,  'email',                'DIRECT', 'teadb.PersistenceUser.email',                                                             1, 1, 0, 1, 11, 1),
  (3,  'realName',             'DIRECT', 'teadb.PersistenceUser.realName',                                                          1, 1, 0, 1, 4,  1),
  (4,  'id',                   'DIRECT', 'teadb.PersistenceOrder.id (primary key, one row per placed order, several per subject)',  1, 0, 1, 2, 4,  1),
  (5,  'addressName',          'DIRECT', 'teadb.PersistenceOrder.addressName',                                                      1, 1, 0, 2, 11, 1),
  (6,  'address1',             'DIRECT', 'teadb.PersistenceOrder.address1',                                                         1, 1, 0, 2, 11, 1),
  (7,  'address2',             'DIRECT', 'teadb.PersistenceOrder.address2',                                                         1, 1, 0, 2, 11, 1),
  (8,  'creditCardCompany',    'DIRECT', 'teadb.PersistenceOrder.creditCardCompany',                                                1, 1, 0, 2, 12, 1),
  (9,  'creditCardNumber',     'DIRECT', 'teadb.PersistenceOrder.creditCardNumber',                                                 1, 1, 0, 2, 12, 1),
  (10, 'creditCardExpiryDate', 'DIRECT', 'teadb.PersistenceOrder.creditCardExpiryLocalDate (exposed as ISO string via getCreditCardExpiryDate())', 1, 1, 0, 2, 12, 1);

-- processing: playbook §1 point 6 - only the types genuinely justified by
-- this application's own code (see header comment above). UPPERCASE (§8.1.a).
INSERT INTO processing (processing_id, processing_name, processing_type, processing_category, created_at, modified_at, ended_at) VALUES
  (1, 'Account Management',       'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (2, 'Order Fulfillment',        'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (3, 'Product Recommendations',  'OPTIONAL',  'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL);

INSERT INTO purpose (purpose_description, purpose_type, processing_id) VALUES
  ('Identify the customer and let a logged-in shopper open/place orders and view their own order history (AuthUserActionsRest.login, ProfileServlet)', 'MAIN', 1),
  ('Ship purchased goods and process payment for a placed order (AuthUserActionsRest.placeOrder, PersistenceOrder address/credit-card fields)', 'MAIN', 2),
  ('Personalize which additional products are suggested on the Cart page, based on the shopper''s own order history (CartServlet.java, LoadBalancedRecommenderOperations.getRecommendations)', 'SECONDARY', 3);

-- data_usage: link data to the processing(s) that actually touch it. Order.id
-- is linked to BOTH Order Fulfillment (full CRUD - it identifies which order
-- to rectify/erase) and Product Recommendations (read-only reuse - see
-- header comment, purpose_type SECONDARY); address/credit-card fields stay
-- tied to Order Fulfillment only.
INSERT INTO data_usage (personal_status, c, r, u, d, data_id, processing_id) VALUES
  (1, 1, 1, 1, 1, 1,  1), -- userName             (Account Management)
  (1, 1, 1, 1, 1, 2,  1), -- email                (Account Management)
  (1, 1, 1, 1, 1, 3,  1), -- realName             (Account Management)
  (1, 1, 1, 1, 1, 4,  2), -- Order.id             (Order Fulfillment)
  (1, 1, 1, 1, 1, 5,  2), -- addressName          (Order Fulfillment)
  (1, 1, 1, 1, 1, 6,  2), -- address1             (Order Fulfillment)
  (1, 1, 1, 1, 1, 7,  2), -- address2             (Order Fulfillment)
  (1, 1, 1, 1, 1, 8,  2), -- creditCardCompany    (Order Fulfillment)
  (1, 1, 1, 1, 1, 9,  2), -- creditCardNumber     (Order Fulfillment)
  (1, 1, 1, 1, 1, 10, 2), -- creditCardExpiryDate (Order Fulfillment)
  (1, 0, 1, 0, 0, 4,  3); -- Order.id             (Product Recommendations, read-only)

-- ----------------------------------------------------------------------------
-- Seed data subject: `user1`, one of the DataGenerator-seeded accounts
-- (userN/"password", generateUsers/generateOrders) - a real, stable id_ref
-- that exists on any fresh TeaStore database without needing a separate
-- seed script on the target-application side (playbook §1 point 8). No
-- contract/consent/processed_data seeded here - see header comment for why.
-- ----------------------------------------------------------------------------
USE `priam-actor`;
INSERT INTO data_subject (data_subject_id, id_ref, data_subject_category_id) VALUES
  (1, 'user1', 1);

-- HOW TO ACTIVATE: this file is baked into the `mysqldb` image at build time
-- (see Databases/Dockerfile) and only runs on a virgin MySQL volume
-- (docker-entrypoint-initdb.d convention) - rebuild the image
-- (`docker compose build mysqldb`) and clear the volume (db-volume/) after
-- editing this file for changes to apply.
--
-- Any other pre-existing seeded user (user0, user2, ...) or any dynamically
-- registered subject (through the new RegisterServlet/register.jsp) is NOT
-- seeded here - the former is caught up by the one-off backfill script
-- (priam-integration/backfill-data-subjects.py), the latter registers itself
-- at runtime via register_data_subject()/report_processed_data() (playbook
-- §4bis, case-studies/TeaStore/services/.../auth/priam/PriamClient.java).
