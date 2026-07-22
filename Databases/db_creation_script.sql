-- actors creation ---
create database IF NOT EXISTS `priam-actor`;
USE `priam-actor` ;

-- Creation of static table Country --
create Table country( 
country_id int primary key,
country_name varchar(100),
minor_age int,
adequate boolean);
insert into country(country_id, country_name, minor_age, adequate) values (23, 'France' , '13' , true);
insert into country(country_id, country_name, minor_age, adequate) values (45, 'Spain' , '16' , false);
insert into country(country_id, country_name, minor_age, adequate) values (87, 'Germany' , '16' , true);
insert into country(country_id, country_name, minor_age, adequate) values (12, 'Italy' , '14' , false);
insert into country(country_id, country_name, minor_age, adequate) values (59, 'Belgium' , '16' , false);
insert into country(country_id, country_name, minor_age, adequate) values (22, 'Greece' , '15' , false);
insert into country(country_id, country_name, minor_age, adequate) values (104, 'Portugal' , '16' , false);
insert into country(country_id, country_name, minor_age, adequate) values (66, 'Sweden' , '16' , true);
insert into country(country_id, country_name, minor_age, adequate) values (29, 'Poland' , '16' , false);
insert into country(country_id, country_name, minor_age, adequate) values (71, 'Netherlands' , '15' , true);

-- Creation table Address --
CREATE TABLE address (address_id INT PRIMARY KEY,street_number VARCHAR(10),street_name VARCHAR(255),postal_code VARCHAR(10),city VARCHAR(255),complement VARCHAR(255));
create table provider(
provider_id int primary key auto_increment,
provider_name varchar(40) not null,
address_id int not null,
provider_phone varchar(40),
provider_email varchar(40), 
country_id int, 
foreign key(country_id) references country(country_id),
foreign key(address_id) references address(address_id));

create table dpo(
dpo_id int primary key auto_increment,
dpo_name varchar(40) not null,
address_id int not null,
dpo_phone varchar(40),
dpo_email varchar(40), 
country_id int, 
foreign key(country_id) references country(country_id),
foreign key(address_id) references address(address_id));

create table representative(
representative_id int primary key auto_increment,
representative_name varchar(40) not null,
address_id int not null,
representative_phone varchar(40),
representative_email varchar(40), 
country_id int, 
foreign key(country_id) references country(country_id),
foreign key(address_id) references address(address_id));

create table tutor(
tutor_id int primary key auto_increment,
tutor_name varchar(40) not null,
country_id int, 
foreign key(country_id) references country(country_id));

create table secondary_actor_category(
secondary_actor_category_id int primary key auto_increment,
secondary_actor_category_name varchar(40));

create table secondary_actor(
secondary_actor_id int primary key auto_increment,
secondary_actor_type varchar(40) check(secondary_actor_type in('RECEPIENT', 'DATA_PROCESSOR', 'THIRD_PARTY')),
secondary_actor_name varchar(40) not null,
address_id int not null,
secondary_actor_phone varchar(40),
secondary_actor_email varchar(40), 
safeguard varchar(255), 
safeguard_type varchar (20) check(safeguard_type in('ADEQUACY_DECISION','CONTRACTUAL_CLAUSE','DEROGATION','BCR','NO')), 
secondary_actor_category_id int,
country_id int,
foreign key(secondary_actor_category_id) references secondary_actor_category(secondary_actor_category_id),
foreign key(country_id) references country(country_id),
foreign key(address_id) references address(address_id));

create table data_subject_category (
data_subject_category_id int primary key auto_increment,data_subject_category_name varchar(25), 
location_id varchar(40));

-- DataSubject table gdpr_Creation --
-- id_ref widened from varchar(25) to varchar(64): a standard UUID (the
-- default primary-key format in most modern frameworks, e.g. Prisma's
-- @default(uuid())) is 36 characters and overflowed the original column,
-- 500ing every insert with "Data too long for column 'id_ref'" for any
-- target app using UUID-shaped ids (see Ghostfolio-PRIAM-test1's
-- INTEGRATION-REPORT.md §2.3). 64 leaves headroom for other common id
-- formats (ULID, prefixed ids, etc.) without being unbounded.
create table data_subject(
data_subject_id int primary key auto_increment,
age int default 16,
id_ref varchar(64) not null,
data_subject_category_id int(11) DEFAULT NULL,
foreign key (data_subject_category_id) references `priam-actor`.data_subject_category(data_subject_category_id));
create database  IF NOT EXISTS  `priam-data`;
USE `priam-data`;

-- Personal Data Category table creation
create table personal_data_category(
personal_data_category_id int primary key,
personal_data_category_name varchar(150));
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (1, 'biometric data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (2, 'genetic data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (3, 'ethnic data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (4, 'identification data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (5, 'political data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (6, 'physic data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (7, 'Profil data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (8, 'health data');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (9, 'criminal convictions');
insert into personal_data_category(personal_data_category_id, personal_data_category_name) values (10, 'none personal data');

-- Data Annotation table Creation --

-- DataType table creation
create table data_type (
data_type_id int primary key,
data_type_name varchar(40));

-- Data table creation
create table data( 
data_id int primary key,
data_name varchar(25), `source` varchar(25),
source_details varchar(255),
data_conservation_duration int DEFAULT -1,
is_personal boolean,
is_portable boolean,
is_primary_key boolean,
data_type_id int,
personal_data_category_id int,
data_subject_category_id int, 
foreign key (data_subject_category_id) references `priam-actor`.data_subject_category(data_subject_category_id),
foreign key (data_type_id) references data_type(data_type_id),
constraint check_source check (`source` in('DIRECT','INDIRECT','PRODUCED')),
foreign key(personal_data_category_id) references personal_data_category(personal_data_category_id));


-- Processing Annotation table Creation --

create table processing (
processing_id int primary key, 
processing_name varchar(25), 
processing_type varchar(25) check (processing_type in('Default','Mandatory','Optional', 'Necessary')), 
processing_category varchar(25) check (processing_category in('CONSENT_CONTRACT','LEGITIMATE_INTEREST','LEGAL_OBLIGATION','PUBLIC_INTEREST','VITAL_INTERESTS')),
created_at date, 
modified_at date,
ended_at date);
create table measure (
measure_id int primary key , 
measure_description varchar(255), 
measure_type varchar(15) check (measure_type in( 'ORGANISATIONAL','TECHNICAL')),
measure_category varchar(20) check (measure_category in( 'Cryption','Anonymisation','Physical_Security','Training','Access_Control','Data_Disposal','Policy_Management')));
insert into measure (measure_id, measure_description, measure_type, measure_category) values (1, 'Use of a firewall ', 'TECHNICAL', 'Physical_Security');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (2, 'Encryption of data carriers and data transfers to ensure data confidentiality during transmission and storage.', 'TECHNICAL', 'Cryption');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (3, 'Pseudonymisation and encryption of personal data to minimize the risk of identification.', 'TECHNICAL', 'Anonymisation');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (4, ' Installation of an alarm system to enhance physical security of premises.', 'TECHNICAL', 'Physical_Security');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (5, 'Structural protection of buildings/premises to prevent unauthorized access.', 'TECHNICAL', 'Physical_Security');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (6, 'Defaults for the password complexity of users (e.g., FIDO2) to improve account security.', 'TECHNICAL', 'Access_Control');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (7, 'Employee training on data protection to ensure compliance and awareness.', 'ORGANISATIONAL', 'Training');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (8, 'Visitor registration to monitor access to sensitive areas.', 'ORGANISATIONAL', 'Access_Control');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (9, 'Data protection-compliant disposal of documents containing personal data', 'ORGANISATIONAL', 'Data_Disposal');

insert into measure (measure_id, measure_description, measure_type, measure_category) values (10, 'Establishment of clear data access policies to define who can access what data.', 'ORGANISATIONAL', 'Policy_Management');


create table purpose (
purpose_id int primary key auto_increment, 
purpose_description varchar(200) not null,
purpose_type varchar(10) check(purpose_type in('Main', 'Secondary')) ,
processing_id int,
foreign key (processing_id) references processing(processing_id));


create table processing_link(
processing1 int, 
processing2 int, 
type_of_link varchar(20),
primary key(processing1, processing2),
foreign key (processing1) references  processing(processing_id),
foreign key (processing2) references  processing(processing_id),
constraint const1 check (type_of_link in('SimilarityLink','InclusionLink')));

-- DataUsage Annotation --
create table data_usage(
data_usage_id int primary key auto_increment,
personal_status boolean default 0,
c boolean default 0,
r boolean default 0,
u boolean default 0,
d boolean default 0,
data_id int,
processing_id int,
foreign key(data_id) references `priam-data`.data(data_id),
foreign key(processing_id) references processing(processing_id));

create table personal_data_transfer(
Personal_data_transfer_id int primary key, 
processing_id int,
foreign key(processing_id) references processing(processing_id));

create table personal_data_transfer_secondary_actor(
Personal_data_transfer_id int, 
secondary_actor_id int,
primary key (Personal_data_transfer_id, secondary_actor_id),
foreign key(Personal_data_transfer_id) references personal_data_transfer(Personal_data_transfer_id),
foreign key(secondary_actor_id) references `priam-actor`.secondary_actor(secondary_actor_id));

create table personal_data_transfer_data(
personal_data_transfer_id int, 
data_id int,
primary key (personal_data_transfer_id, data_id),
foreign key(data_id) references `priam-data`.data(data_id));

create table processing_measure(
measure_id int, 
processing_id int,
primary key (measure_id, processing_id),
foreign key(measure_id) references measure(measure_id),
foreign key(processing_id) references processing(processing_id));

-- nb_occurrences: PRIAM-Data-service's ProcessedData.java JPA entity
-- (priam.data.priamdataservice.entities.ProcessedData) declares this field
-- (Hibernate default naming: nbOccurrences -> nb_occurrences) and both
-- ProcessedDataService.addProcessedData/removeProcessedData read/write it
-- as a reference count (incremented on a repeat report, decremented on
-- withdrawal, row only deleted once it reaches 0) - necessary as soon as
-- more than one record of a data_type can report the same data_id (e.g.
-- several rows of a one-to-many type). This column was missing here,
-- causing every /api/processed-data/add or /remove call to fail with
-- "Unknown column 'processedd0_.nb_occurrences' in 'field list'" - a
-- schema/entity mismatch entirely internal to PRIAM, not specific to any
-- target application. Found and fixed during the Bank of Anthos
-- integration - see Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8 index and
-- PRIAM-Services/PRIAM-INTERNAL-FIXES.md for the full writeup.
create table processed_data(
data_id int,
data_subject_id int,
nb_occurrences int not null default 1,
primary key (data_id, data_subject_id),
foreign key(data_id) references `priam-data`.data(data_id),
foreign key(data_subject_id) references `priam-actor`.data_subject(data_subject_id));

create database  IF NOT EXISTS  `priam-right`;
USE `priam-right`;

-- DataSubject Rights Creation--
create table data_request ( 
data_request_id int primary key auto_increment, 
data_request_claim varchar(250), 
data_request_issued_at datetime, 
new_value varchar(250), 
is_isolated boolean default false,
data_request_type varchar(25) check (data_request_type in('RECTIFICATION','ERASURE','ACCESS')),
data_subject_id int,
foreign key(data_subject_id) references `priam-actor`.data_subject(data_subject_id),
response boolean);

create table data_request_data (
data_request_id int, 
data_id int,
primary key (data_request_id, data_id),
answer_by_data boolean,
foreign key(data_request_id) references data_request(data_request_id),
foreign key(data_id) references `priam-data`.data(data_id));

create table data_request_primary_key (
data_request_id int, 
primary_key_id int,
primary_key_value varchar(50), 
primary key (data_request_id, primary_key_id),
foreign key(data_request_id) references data_request(data_request_id),
foreign key(primary_key_id) references `priam-data`.data(data_id));

create table data_request_answer(
data_request_answer_id int primary key auto_increment,
answer varchar(7) check (answer in('Full', 'Partial', 'Refused')) ,
data_request_claim varchar(250),
data_request_id int,
foreign key (data_request_id) references data_request(data_request_id));

create table processing_request(
processing_request_id int primary key, 
processing_request_claim varchar(250), 
processing_request_issued_at datetime, 
processing_request_type varchar(25) check (processing_request_type in('OBJECTION','RESTRICTION')),
data_subject_id int, 
processing_id int, 
foreign key (data_subject_id) references `priam-actor`.data_subject(data_subject_id),
foreign key (processing_id) references `priam-data`.processing(processing_id),
response boolean);

create table processing_request_answer(
processing_request_answer_id int primary key ,
answer varchar(7) check (answer in('Full', 'Partial', 'Refused')),
processing_request_answer_claim varchar(250),
processing_request_id int,
foreign key (processing_request_id) references processing_request(processing_request_id));

-- Contract and consent management --
create database  IF NOT EXISTS  `priam-consent`;
USE `priam-consent`;

create table contract(
contract_id int primary key auto_increment,
signature_date date, 
expiration_date date, 
data_subject_id int, 
foreign key (data_subject_id) references `priam-actor`.data_subject(data_subject_id));

-- start_date/end_date widened from date to datetime: ConsentServiceImpl
-- .create() finds "the current consent to toggle" via
-- `ORDER BY start_date DESC LIMIT 1` (ConsentRepository) - with day-only
-- precision, every consent created the same calendar day ties on start_date,
-- so that ORDER BY can't tell them apart and silently falls back to an
-- arbitrary row. Symptom: the first grant/revoke toggle of the day works,
-- every later toggle that same day keeps finding the same stale first row
-- (already closed) and only ever adds new granted rows, never closing
-- anything again - a data subject unchecking an OPTIONAL processing a second
-- or third time appears to do nothing. See
-- Docs/PRIAM-INTEGRATION-PLAYBOOK.md §8.5 (found during the Ghostfolio
-- integration).
create table consent (
consent_id int primary key auto_increment,
start_date datetime,
end_date datetime,
processing_id int,
contract_id int,
foreign key (processing_id) references `priam-data`.processing(processing_id),
foreign key (contract_id) references contract(contract_id));

create database  IF NOT EXISTS  `priam-breach`;

USE `priam-breach`;

create table breach (
breach_id int primary key , 
nature varchar(40), 
risk_level varchar(7) check (risk_level in('NoRisk','Average','High')), 
creation_date date, 
sprv_auth_non_notif_reason varchar(255), 
ds_non_notif_reason varchar(255)); 

-- Consequence table gdpr_Creation --
create table consequence (
consequence_id int primary key , 
consequence_description varchar(255));

create table breach_measure(
measure_id int, 
breach_id int,
primary key (measure_id, breach_id),
foreign key(measure_id) references `priam-data`.measure(measure_id),
foreign key(breach_id) references breach(breach_id));

create table Breach_Consequence(
consequence_id int, 
breach_id int,
primary key (consequence_id, breach_id),
foreign key(consequence_id) references consequence(consequence_id),
foreign key(breach_id) references breach(breach_id));

create table breach_data_subject(
data_subject_id int, 
breach_id int,
primary key (data_subject_id, breach_id),
foreign key(data_subject_id) references `priam-actor`.data_subject(data_subject_id),
foreign key(breach_id) references breach(breach_id));

create table breach_data(
data_id int, 
breach_id int,
nb_records int,
primary key (data_id, breach_id),
foreign key(data_id) references `priam-data`.data(data_id),
foreign key(breach_id) references breach(breach_id));
