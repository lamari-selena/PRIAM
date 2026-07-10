-- ============================================================================
-- PRIAM data/processing annotation for FastAPI-Healthcare-PRIAM ONLY.
--
-- Self-contained: written as if this were the only case study ever loaded
-- into PRIAM, to demonstrate PRIAM works generically without needing to
-- change its own code for a new target application. Simple sequential ids
-- (1, 2, 3...) are used since this script is not meant to coexist with any
-- other case study's data in the same PRIAM database (see
-- Databases/db_insertion_script_4cases.sql for the older combined version
-- covering all 4 case studies at once, kept as a reference).
--
-- HOW TO USE: copy this file to Databases/ and reference it instead of
-- db_insertion_script_4cases.sql in Databases/Dockerfile's COPY line, then
-- rebuild the PRIAM databases image (or run it manually against the running
-- MySQL container). Not wired in automatically — left to a deliberate step
-- so switching case studies stays an explicit action.
--
-- Real schema verified directly in app/db/models.py (this clone):
--   patients(id, first_name, last_name, date_of_birth, email, phone,
--            address, insurance_provider, insurance_id, ...)
--   medical_records(id, patient_id, appointment_id, diagnosis, treatment,
--            prescription, notes, ...)
--   appointments(id, patient_id, doctor_id, start_time, end_time, status,
--            notes, ...)
--
-- data_subject.id_ref = '1' — REAL as of this session: app/db/seed.py
-- (called from app/main.py, gated by SEED_DEMO_DATA, default on) seeds one
-- demo patient "Jane Doe" on a fresh database, which gets id=1.
--
-- NOTE on processing ids: PRIAM's Consent Decision Point now resolves a
-- processingId given as a human-readable processingName generically (see
-- PRIAM-Consent-Service ContractServiceImpl.getConsentByDataSubject, fixed
-- this session to call Data-service's new GET /api/processing/byName/{name}
-- instead of a hardcoded name-to-id translation). So the consent-check code
-- we add to FastAPI-Healthcare-PRIAM can call PRIAM with the processingName
-- directly, e.g. get_consent(patient_id, "appointment-notifications") — no
-- need to hardcode or pass the numeric id.
--
-- NOTE on ordering: data_subject_category (priam-actor) must be inserted
-- BEFORE data (priam-data), since data.data_subject_category_id is a
-- foreign key into priam-actor.data_subject_category (cross-schema FK).
-- ============================================================================

-- ── Actor: data_subject_category must exist before priam-data.data can
--    reference it (cross-schema foreign key) ───────────────────────────────

USE `priam-actor`;

INSERT INTO `data_subject_category` (`data_subject_category_id`, `data_subject_category_name`, `location_id`) VALUES
(1, 'patients', 'id');

-- ── Data & Processing annotation ───────────────────────────────────────────

USE `priam-data`;

-- personal_data_category ships with only 10 generic rows (see
-- Databases/db_creation_script.sql); "contact" and "financial" are missing
-- and needed here, so add them (safe: plain data rows, no CHECK constraint).
INSERT INTO `personal_data_category` (`personal_data_category_id`, `personal_data_category_name`) VALUES
(11, 'contact data'),
(12, 'financial data');

INSERT INTO `data_type` (`data_type_id`, `data_type_name`) VALUES
(1, 'Patient'),
(2, 'MedicalRecord'),
(3, 'Appointment');

INSERT INTO `data` (`data_id`, `data_name`, `source`, `source_details`, `data_conservation_duration`, `is_personal`, `is_portable`, `is_primary_key`, `data_type_id`, `personal_data_category_id`, `data_subject_category_id`) VALUES
(1,  'first_name',         'DIRECT', 'patients table, column first_name',         3650, 1, 1, 0, 1, 4,  1),
(2,  'last_name',          'DIRECT', 'patients table, column last_name',          3650, 1, 1, 0, 1, 4,  1),
(3,  'email',              'DIRECT', 'patients table, column email',              3650, 1, 1, 0, 1, 11, 1),
(4,  'phone',              'DIRECT', 'patients table, column phone',              3650, 1, 1, 0, 1, 11, 1),
(5,  'date_of_birth',      'DIRECT', 'patients table, column date_of_birth',      3650, 1, 1, 0, 1, 4,  1),
(6,  'address',            'DIRECT', 'patients table, column address',            3650, 1, 1, 0, 1, 11, 1),
(7,  'insurance_provider', 'DIRECT', 'patients table, column insurance_provider', 3650, 1, 1, 0, 1, 12, 1),
(8,  'insurance_id',       'DIRECT', 'patients table, column insurance_id',       3650, 1, 0, 0, 1, 12, 1),
(9,  'diagnosis',          'DIRECT', 'medical_records table, column diagnosis',   3650, 1, 0, 0, 2, 8,  1),
(10, 'treatment',          'DIRECT', 'medical_records table, column treatment',   3650, 1, 0, 0, 2, 8,  1),
(11, 'prescription',       'DIRECT', 'medical_records table, column prescription', 3650, 1, 0, 0, 2, 8, 1),
(12, 'notes',              'DIRECT', 'medical_records table, column notes',       3650, 1, 0, 0, 2, 8,  1),
(13, 'notes',              'DIRECT', 'appointments table, column notes',          1825, 1, 1, 0, 3, 7,  1);

INSERT INTO `processing` (`processing_id`, `processing_name`, `processing_type`, `processing_category`, `created_at`, `modified_at`, `ended_at`) VALUES
(1, 'appointment-management',      'Necessary', 'CONSENT_CONTRACT', '2026-01-01', NULL, NULL),
(2, 'medical-records-mgmt',        'Necessary', 'CONSENT_CONTRACT', '2026-01-01', NULL, NULL),
(3, 'appointment-notifications',   'Optional',  'CONSENT_CONTRACT', '2026-01-01', NULL, NULL);

INSERT INTO `data_usage` (`data_usage_id`, `personal_status`, `c`, `r`, `u`, `d`, `data_id`, `processing_id`) VALUES
(1,  1, 1, 1, 1, 1, 1,  1),
(2,  1, 1, 1, 1, 1, 2,  1),
(3,  1, 1, 1, 1, 1, 3,  1),
(4,  1, 1, 1, 1, 1, 4,  1),
(5,  1, 1, 1, 1, 1, 5,  1),
(6,  1, 1, 1, 1, 1, 9,  2),
(7,  1, 1, 1, 1, 1, 10, 2),
(8,  1, 1, 1, 1, 1, 11, 2),
(9,  1, 1, 1, 1, 1, 12, 2),
(10, 1, 0, 1, 0, 0, 3,  3),
(11, 1, 0, 1, 0, 0, 1,  3),
(12, 1, 0, 1, 0, 0, 2,  3);

INSERT INTO `purpose` (`purpose_id`, `purpose_description`, `purpose_type`, `processing_id`) VALUES
(1, 'Record, store and manage healthcare appointments between patients and doctors.', 'Main', 1),
(2, 'Record and store medical diagnoses, treatments and prescriptions for the purpose of providing healthcare (GDPR Art. 9(2)(h)).', 'Main', 2),
(3, 'Send appointment creation, update and cancellation reminders to patients via email using the RabbitMQ notification queue.', 'Secondary', 3);

-- ── Actor: the one demo patient seeded by app/db/seed.py ───────────────────
-- (data_subject references data_subject_category, already inserted above)

USE `priam-actor`;

INSERT INTO `data_subject` (`data_subject_id`, `age`, `id_ref`, `data_subject_category_id`) VALUES
(1, NULL, '1', 1);

-- ── Consent: pre-granted for the one optional processing ──────────────────
-- Starts GRANTED (end_date NULL) so rights tests (access/rectification/
-- erasure) work from a clean state; withdraw/re-grant through the real
-- Consent service API during the consent-gating test, not by editing this
-- file.

USE `priam-consent`;

INSERT INTO `contract` (`contract_id`, `signature_date`, `expiration_date`, `data_subject_id`) VALUES
(1, '2026-01-01', NULL, 1);

INSERT INTO `consent` (`consent_id`, `start_date`, `end_date`, `processing_id`, `contract_id`) VALUES
(1, '2026-01-01', NULL, 3, 1);
