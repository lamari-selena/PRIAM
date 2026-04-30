USE `priam-actor`;

INSERT INTO `address` (`address_id`, `street_number`, `street_name`, `postal_code`, `city`, `complement`) VALUES
(1, '10', 'Rue de la Paix ', ' 75002', ' Paris', 'Bâtiment A, Appartement 3 '),
(2, '15', 'street1 ', '65735', 'city1', ''),
(3, '34', 'rue 14', '75000', 'Paris', '');

INSERT INTO `data_subject_category` (`data_subject_category_id`, `data_subject_category_name`, `location_id`) VALUES
                                                                                                                  (1, 'PERSISTENCEUSER', 'pu_ID'),
                                                                                                                  (2, 'none', '');
INSERT INTO `data_subject` (`data_subject_id`, `age`, `id_ref`, `data_subject_category_id`) VALUES
(1, 16, '507', 1),
(2, 16, '508', 1),
(3, NULL, '509', 1),
(4, NULL, '510', 1),
(5, NULL, '511', 1),
(6, 16, '512', 1),
(7, NULL, '513', 1),
(8, NULL, '514', 1),
(9, NULL, '515', 1),
(10, NULL, '516', 1),
(11, NULL, '517', 1),
(12, NULL, '518', 1),
(13, NULL, '519', 1),
(14, NULL, '520', 1),
(15, NULL, '521', 1),
(16, NULL, '522', 1),
(17, NULL, '523', 1),
(18, NULL, '524', 1),
(19, NULL, '525', 1),
(20, NULL, '526', 1),
(21, NULL, '527', 1),
(22, NULL, '528', 1),
(23, NULL, '529', 1),
(24, NULL, '530', 1),
(25, NULL, '531', 1),
(26, NULL, '532', 1),
(27, NULL, '533', 1),
(28, NULL, '534', 1),
(29, NULL, '535', 1),
(30, NULL, '536', 1),
(31, NULL, '537', 1),
(32, NULL, '538', 1),
(33, NULL, '539', 1),
(34, NULL, '540', 1),
(35, NULL, '541', 1),
(36, NULL, '542', 1),
(37, NULL, '543', 1),
(38, NULL, '544', 1),
(39, NULL, '545', 1),
(40, NULL, '546', 1),
(41, NULL, '547', 1),
(42, NULL, '548', 1),
(43, NULL, '549', 1),
(44, NULL, '550', 1),
(45, NULL, '551', 1),
(46, NULL, '552', 1),
(47, NULL, '553', 1),
(48, NULL, '554', 1),
(49, NULL, '555', 1),
(50, NULL, '556', 1),
(51, NULL, '557', 1),
(52, NULL, '558', 1),
(53, NULL, '559', 1),
(54, NULL, '560', 1),
(55, NULL, '561', 1),
(56, NULL, '562', 1),
(57, NULL, '563', 1),
(58, NULL, '564', 1),
(59, NULL, '565', 1),
(60, NULL, '566', 1),
(61, NULL, '567', 1),
(62, NULL, '568', 1),
(63, NULL, '569', 1),
(64, NULL, '570', 1),
(65, NULL, '571', 1),
(66, NULL, '572', 1),
(67, NULL, '573', 1),
(68, NULL, '574', 1),
(69, NULL, '575', 1),
(70, NULL, '576', 1),
(71, NULL, '577', 1),
(72, NULL, '578', 1),
(73, NULL, '579', 1),
(74, NULL, '580', 1),
(75, NULL, '581', 1),
(76, NULL, '582', 1),
(77, NULL, '583', 1),
(78, NULL, '584', 1),
(79, NULL, '585', 1),
(80, NULL, '586', 1),
(81, NULL, '587', 1),
(82, NULL, '588', 1),
(83, NULL, '589', 1),
(84, NULL, '590', 1),
(85, 16, '596', 1),
(86, NULL, '592', 1),
(87, NULL, '593', 1),
(88, NULL, '594', 1),
(89, NULL, '595', 1),
(90, NULL, '591', 1),
(91, NULL, '597', 1),
(92, NULL, '598', 1),
(93, NULL, '599', 1),
(94, NULL, '600', 1),
(95, NULL, '601', 1),
(96, NULL, '602', 1),
(97, NULL, '603', 1),
(98, NULL, '604', 1),
(99, NULL, '605', 1),
(100, NULL, '606', 1);


INSERT INTO `provider` (`provider_id`, `provider_name`, `address_id`, `provider_phone`, `provider_email`, `country_id`) VALUES
(1, 'Jean Dupont', 1, '+33123456789', 'jean.dupont@example.com ', 23);

INSERT INTO `secondary_actor_category` (`secondary_actor_category_id`, `secondary_actor_category_name`) VALUES
(1, 'partners'),
(2, 'supervisory authority');

INSERT INTO secondary_actor (
    secondary_actor_id,
    secondary_actor_type,
    secondary_actor_name,
    address_id,
    secondary_actor_phone,
    secondary_actor_email,
    safeguard,
    safeguard_type,
    secondary_actor_category_id,
    country_id
) VALUES (
             3,
             'RECEPIENT',
             'Acme Corp',
             1,
             '+33123456789',
             'contact@acme.com',
             'Standard contractual clauses applied',
             'CONTRACTUAL_CLAUSE',
             1,
             23
         );

USE `priam-data`;

ALTER TABLE `processed_data`
ADD COLUMN `nb_occurrences` INT DEFAULT 0;

INSERT INTO `data_type` (`data_type_id`, `data_type_name`) VALUES
(1, 'PERSISTENCEUSER'),
(2, 'PERSISTENCEORDER'),
(7, 'PERSISTENCEPRODUCT');

INSERT INTO `data` (`data_id`, `data_name`, `source`, `source_details`, `data_conservation_duration`, `is_personal`, `is_portable`, `is_primary_key`, `data_type_id`, `personal_data_category_id`, `data_subject_category_id`) VALUES
(1, 'pu_ID', 'DIRECT', '', 12, 1, 1, 1, 1, 4, 1),
(2, 'po_ADDRESS1', 'INDIRECT', '', 12, 1, 1, 0, 2, 4, 1),
(3, 'pu_EMAIL', 'PRODUCED', '', 12, 1, 1, 0, 1, 4, 1),
(4, 'pu_PASSWORD', 'DIRECT', '', 12, 1, 1, 0, 1, 4, 1),
(5, 'pu_REALNAME', 'DIRECT', '', 12, 1, 1, 0, 1, 4, 1),
(6, 'pu_USERNAME', 'DIRECT', '', 12, 1, 1, 0, 1, 4, 1),
(7, 'pp_ID', 'DIRECT', '', 12, 0, 1, 1, 7, 4, 2),
(8, 'pp_Description', 'DIRECT', '', -1, 0, 1, 0, 7, 10, 2),
(9, 'pp_LISTPRICEINCENTS', 'DIRECT', '', -1, 0, 1, 0, 7, 10, 2),
(10, 'pp_NAME', 'DIRECT', '', -1, 0, 1, 0, 7, 10, 2),
(11, 'po_CREDITCARDCOMPANY', 'DIRECT', '', 12, 1, 1, 0, 2, 4, 1);

INSERT INTO `processing` (`processing_id`, `processing_name`, `processing_type`, `processing_category`, `created_at`, `modified_at`, `ended_at`) VALUES
(1, 'recomended', 'OPTIONAL', 'CONSENT_CONTRACT', '2024-10-14', NULL, NULL),
(3, 'second', 'OPTIONAL', 'CONSENT_CONTRACT', '2024-10-14', NULL, NULL);

INSERT INTO `data_usage` (`data_usage_id`, `personal_status`, `c`, `r`, `u`, `d`, `data_id`, `processing_id`) VALUES
(5, 1, 0, 1, 0, 0, 3, 1),
(6, 1, 0, 1, 0, 0, 5, 1),
(7, 0, 0, 1, 0, 0, 8, 1),
(8, 1, 0, 1, 0, 0, 2, 3),
(9, 0, 0, 1, 0, 0, 8, 3);

INSERT INTO `processing_measure` (`measure_id`, `processing_id`) VALUES
                                                                     (1, 1),
                                                                     (6, 3);

INSERT INTO `purpose` (`purpose_id`, `purpose_description`, `purpose_type`, `processing_id`) VALUES
                                                                                                 (2, 'statistics', 'MAIN', 1),
                                                                                                (3, 'pub', 'MAIN', 3);




INSERT INTO `processed_data` (`data_id`, `data_subject_id`, `nb_occurrences`) VALUES
                                                                                  (1, 1, 0),
                                                                                  (2, 1, 1),
                                                                                  (2, 2, 1),
                                                                                  (3, 1, 3),
                                                                                  (3, 2, 2),
                                                                                  (5, 1, 1),
                                                                                  (5, 2, 3),
                                                                                  (8, 1, 4),
                                                                                  (8, 2, 3);


INSERT INTO `personal_data_transfer` (`Personal_data_transfer_id`, `processing_id`) VALUES
(1, 1),
(0, 3);

INSERT INTO `personal_data_transfer_data` (`personal_data_transfer_id`, `data_id`) VALUES
(0, 2),
(0, 5);

INSERT INTO `personal_data_transfer_secondary_actor` (`Personal_data_transfer_id`, `secondary_actor_id`) VALUES
                                                                                                             (0, 3),
                                                                                                             (1, 3);



USE `priam-consent`;

INSERT INTO `contract` (`contract_id`, `signature_date`, `expiration_date`, `data_subject_id`) VALUES
(1, '2024-10-14', NULL, 1),
(3, '2024-11-18', NULL, 2),
(4, '2024-12-27', NULL, 6);

INSERT INTO `consent` (`consent_id`, `start_date`, `end_date`, `processing_id`, `contract_id`) VALUES
(1, '2025-03-09', NULL, 3, 3),
(2, '2025-03-13', '2025-05-23', 1, 3),
(3, '2025-05-02', NULL, 1, 1),
(4, '2025-05-02', NULL, 3, 1);

