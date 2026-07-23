-- ============================================================================
-- PRIAM annotation for OnlineBoutique (Google's microservices-demo -
-- polyglot e-commerce demo: Go frontend/checkout/shipping/productcatalog,
-- C# cartservice, Node currency/payment, Python email/recommendation/
-- shoppingassistant). See Docs/PRIAM-INTEGRATION-PLAYBOOK.md §1.
--
-- ----------------------------------------------------------------------------
-- Real schema referenced (not invented) - verified directly in the target
-- app's source, case-studies/OnlineBoutique/src/:
--
--   - This application has NO sign-up/login/account of any kind (grep for
--     "signup|login|register|account|auth" across src/frontend returns zero
--     hits in application code; no "users"/"customers" table exists
--     anywhere in the whole src/ tree). The only durable-ish identity
--     concept is `session_id`: an anonymous UUID cookie minted by
--     frontend/middleware.go:85-110 (`ensureSessionID`, cookie name
--     `shop_session-id`), never tied to any name/email. It is used
--     everywhere as the "user id" (frontend/handlers.go:362
--     `UserId: sessionID(r)` in the checkout gRPC call, cartservice/src/
--     protos/Cart.proto `Cart.user_id`).
--   - Checkout PII (email, street address, credit card - collected in
--     frontend/handlers.go:324-370 `placeOrderHandler`) is purely
--     TRANSIENT: checkoutservice/main.go's PlaceOrder (lines 230-280) never
--     persists it anywhere - paymentservice/charge.js only logs the last 4
--     card digits, shippingservice hashes the address into a tracking id
--     without storing it, emailservice's DummyEmailService just logs the
--     address to stdout (email_server.py:108-111, dummy_mode=true is what's
--     actually wired, the real SMTP class is dead code raising
--     NotImplemented). There is no order-history table anywhere. This PII is
--     therefore NOT annotated below (nothing to genuinely access/rectify/
--     erase after the fact) - see priam-integration/INTEGRATION-REPORT.md
--     "Scope decisions" for the documented reasoning instead of fabricating
--     a fake backing store for it.
--   - The only durably-stored personal data in the whole application is the
--     shopping cart: cartservice/src/cartstore/RedisCartStore.cs
--     (AddItemAsync/GetCartAsync/EmptyCartAsync), backed by
--     Microsoft.Extensions.Caching.StackExchangeRedis, keyed by `userId`
--     (= session_id), holding a serialized `Hipstershop.Cart` protobuf
--     (cartservice/src/protos/Cart.proto:27-48: `CartItem{product_id,
--     quantity}`, `Cart{user_id, items}`) in the Redis hash field `data`
--     (the standard field name this library stores its payload under -
--     confirmed empirically against the real redis-cart container during
--     this session, see priam-integration/ETAPES-FAITES.md). Several rows
--     per subject (one per product in the cart) - `is_primary_key=1` on
--     `product_id` below (playbook §1 point 5 / §8.1.c).
--   - The one genuinely OPTIONAL processing found in this application's own
--     code: frontend/rpc.go:99-117 `getRecommendations()` (called from
--     home/product/cart pages, frontend/handlers.go) reads the cart's
--     product_ids and calls recommendationservice.ListRecommendations to
--     personalize which other products are shown - the app's core shopping/
--     checkout flow works perfectly without it (confirmed by reading every
--     call site: recommendations are rendered into a template block,
--     ignored on error, never required for add-to-cart/checkout to
--     succeed). Gated by get_consent() (playbook §4) - see
--     frontend/rpc.go's modified getRecommendations.
--
-- idRef: `session_id` (the `shop_session-id` cookie value) - always a
-- non-numeric UUID string by construction (frontend/middleware.go:94
-- `uuid.NewRandom()`), satisfying the playbook §7 "non-numeric idRef" test
-- requirement for every real test run against this integration, not just a
-- specially-crafted one.
--
-- No MANDATORY processing annotated: unlike Bank of Anthos (SSN/KYC,
-- Art. 6.1.c), nothing in OnlineBoutique's own code is processed under a
-- distinct legal obligation rather than contract necessity or consent - not
-- invented here for the sake of covering all 4 processing_type values
-- (playbook §1 point 6 lists them as available, not mandatory to use all).
-- No personal_data_transfer/secondary_actor either (playbook §1 point 12):
-- Cart Management and Product Recommendations both stay entirely internal
-- to this application's own microservices (cartservice/recommendationservice),
-- no external third party ever receives this data.
--
-- Seed subject: no default/demo session ships with a fresh OnlineBoutique
-- checkout (session_id is minted fresh per browser on first visit). A real
-- session was obtained by curling the running frontend once
-- (`curl -i http://localhost:8080/` and reading the `Set-Cookie:
-- shop_session-id=...` response header) before writing this script - see
-- priam-integration/ETAPES-FAITES.md for the exact command and captured
-- value - then a real product was added to that same session's cart via the
-- application's own UI/API so the seeded processed_data/consent rows below
-- reflect a real, observable Redis state, not a placeholder.
-- ============================================================================

USE `priam-actor`;

INSERT INTO data_subject_category (data_subject_category_id, data_subject_category_name, location_id)
VALUES (1, 'Shopper', NULL);

USE `priam-data`;

INSERT INTO data_type (data_type_id, data_type_name) VALUES
  (1, 'Cart');

-- data: the only 2 columns cartservice's Redis-backed store genuinely
-- holds (Cart.proto CartItem{product_id, quantity}). is_primary_key=1 on
-- product_id (playbook §1 point 5 / §8.1.c): Cart has several rows per
-- subject (one per product), and product_id is the only column that
-- distinguishes one row from another for a given idRef - the Provider
-- bridge resolves a specific cart row via (idRef=session_id,
-- primaryKeys['product_id']). personal_data_category 7 ('Profil data',
-- the closest of the 10 default categories - see db_creation_script.sql -
-- to "behavioral/preference data revealing what a subject shops for").
INSERT INTO data (data_id, data_name, `source`, source_details, is_personal, is_portable, is_primary_key, data_type_id, personal_data_category_id, data_subject_category_id) VALUES
  (1, 'product_id', 'DIRECT', 'cartservice Redis-backed store (RedisCartStore.cs AddItemAsync/GetCartAsync), key=session_id, StackExchangeRedis hash field "data", protobuf Cart.items[].product_id (Cart.proto:28)', 1, 1, 1, 1, 7, 1),
  (2, 'quantity',   'DIRECT', 'same Redis-backed Cart record, protobuf Cart.items[].quantity (Cart.proto:29)',                                                                                                  1, 1, 0, 1, 7, 1);

-- processing: playbook §1 point 6 - only the 2 types genuinely justified by
-- this application's own code (see header comment above for why no
-- MANDATORY/DEFAULT is invented). processing_type/processing_category in
-- exact UPPERCASE (§8.1.a) - the CHECK constraint's TitleCase literals
-- ('Necessary','Optional',...) are collation-insensitive in MySQL but
-- crash Hibernate with IllegalArgumentException at read time.
INSERT INTO processing (processing_id, processing_name, processing_type, processing_category, created_at, modified_at, ended_at) VALUES
  (1, 'Cart Management',          'NECESSARY', 'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL),
  (2, 'Product Recommendations',  'OPTIONAL',  'CONSENT_CONTRACT', CURDATE(), CURDATE(), NULL);

INSERT INTO purpose (purpose_description, purpose_type, processing_id) VALUES
  ('Store and retrieve the items a shopper has added to their cart so checkout can proceed (frontend/rpc.go insertCart/getCart, cartservice RedisCartStore)', 'MAIN', 1),
  ('Personalize which other products are suggested to the shopper based on their current cart contents (frontend/rpc.go getRecommendations, recommendationservice.ListRecommendations)', 'MAIN', 2);

-- data_usage: link data to the processing(s) that actually touch it.
-- Cart Management can create/read/update(rectify a quantity)/delete
-- (erase a cart row) both columns. Product Recommendations only READS
-- product_id (it never creates/modifies/deletes cart rows itself - it
-- merely consumes them to compute a recommendation).
INSERT INTO data_usage (personal_status, c, r, u, d, data_id, processing_id) VALUES
  (1, 1, 1, 1, 1, 1, 1), -- product_id (Cart Management)
  (1, 1, 1, 1, 1, 2, 1), -- quantity   (Cart Management)
  (1, 0, 1, 0, 0, 1, 2); -- product_id (Product Recommendations, read-only)

-- No personal_data_transfer/secondary_actor (§1 point 12): neither
-- processing above sends any of this data to an external third party -
-- both cartservice and recommendationservice are internal microservices of
-- this same application. Conditional annotation, correctly left empty here.

-- ----------------------------------------------------------------------------
-- Seed data subject: a real session_id captured by curling the running
-- frontend once (see priam-integration/ETAPES-FAITES.md, "Seed session
-- capture"), then used to add one real product to cart through the
-- application's own /cart endpoint so the processed_data/consent rows below
-- mirror exactly what register_data_subject()/report_processed_data() do at
-- runtime for any real visitor (playbook §1 point 8-9-11, §8.1.b).
-- ----------------------------------------------------------------------------
-- Real session_id captured via `curl -i http://localhost:8080/` against the
-- running frontend container (see priam-integration/ETAPES-FAITES.md,
-- "Seed session capture" for the exact command/response), then used to add
-- one real product to cart (OLJCESPC7Z x2) through the application's own
-- POST /cart endpoint before this script was finalized.
USE `priam-actor`;
INSERT INTO data_subject (data_subject_id, id_ref, data_subject_category_id) VALUES
  (1, '207acaaf-a999-4ede-9ca6-7e1eeaaedda5', 1);

USE `priam-consent`;
INSERT INTO contract (contract_id, signature_date, expiration_date, data_subject_id) VALUES
  (1, CURDATE(), NULL, 1);

-- Pre-granted OPTIONAL consent (end_date NULL) so the withdraw/re-grant
-- cycle (§3) can be tested immediately without first exercising the runtime
-- consent-grant path.
INSERT INTO consent (start_date, end_date, processing_id, contract_id) VALUES
  (NOW(), NULL, 2, 1); -- Product Recommendations granted for the seed subject

USE `priam-data`;
-- processed_data bookkeeping (§8.1.b): required for the first consent
-- withdrawal against this pre-seeded row to succeed, and for the seed
-- subject's Cart fields to show up in the Access Request list at all.
INSERT INTO processed_data (data_id, data_subject_id) VALUES
  (1, 1), -- product_id
  (2, 1); -- quantity

-- HOW TO ACTIVATE: this file is baked into the `mysqldb` image at build time
-- (see Databases/Dockerfile) and only runs on a virgin MySQL volume
-- (docker-entrypoint-initdb.d convention) - rebuild the image
-- (`docker compose build mysqldb`) and clear the volume (db-volume/) after
-- editing this file for changes to apply.
--
-- Dynamically registered subjects (any real visitor whose session_id is
-- minted after this integration's hooks are wired) are NOT seeded here -
-- register_data_subject()/report_processed_data() (playbook §4bis,
-- case-studies/OnlineBoutique/src/frontend/priam.go) create their
-- data_subject/processed_data rows at runtime instead.
