-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 10, 2024 at 06:55 PM
-- Server version: 10.4.24-MariaDB
-- PHP Version: 8.1.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `priam-data`;
USE `priam-data`;
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `priam-data1`
--

-- --------------------------------------------------------

--
-- Table structure for table `consent`
--

CREATE TABLE `consent` (
  `data_subject_id` int(11) NOT NULL,
  `processing_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `consent`
--

-- INSERT INTO `consent` (`data_subject_id`, `processing_id`) VALUES
-- (1, 1);

-- --------------------------------------------------------

--
-- Table structure for table `country`
--

CREATE TABLE `country` (
  `country_id` int(11) NOT NULL,
  `adequate` bit(1) NOT NULL,
  `country_name` varchar(255) DEFAULT NULL,
  `minor_age` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `data`
--

CREATE TABLE `data` (
  `data_id` int(11) NOT NULL,
  `data_conservation_duration` int(11) DEFAULT NULL,
  `data_name` varchar(255) DEFAULT NULL,
  `data_subject_category_id` int(11) DEFAULT NULL,
  `is_personal` bit(1) DEFAULT NULL,
  `is_portable` bit(1) DEFAULT NULL,
  `is_primary_key` bit(1) DEFAULT NULL,
  `source` int(11) DEFAULT NULL,
  `source_details` varchar(255) DEFAULT NULL,
  `data_type_id` int(11) DEFAULT NULL,
  `personal_data_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data`
--

-- INSERT INTO `data` (`data_id`, `data_conservation_duration`, `data_name`, `data_subject_category_id`, `is_personal`, `is_portable`, `is_primary_key`, `source`, `source_details`, `data_type_id`, `personal_data_category_id`) VALUES
-- (1, 12, 'po_ADDRESS1', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
-- (2, 12, 'po_ADDRESS2', 1, b'1', b'1', b'0', 0, NULL, 3, 2),
-- (3, 12, 'po_ID', 1, b'1', b'1', b'1', 1, NULL, 3, 1),
-- (4, 12, 'pu_ID', 1, b'1', b'1', b'1', 2, NULL, 6, 2),
-- (8, 12, 'pu_EMAIL', 1, b'1', b'1', b'0', 0, NULL, 6, 2);

-- --------------------------------------------------------

--
-- Table structure for table `data_type`
--

CREATE TABLE `data_type` (
  `data_type_id` int(11) NOT NULL,
  `data_type_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_type`
--

INSERT INTO `data_type` (`data_type_id`, `data_type_name`) VALUES
(1, 'databasemanagemententity'),
(2, 'persistencecategory'),
(3, 'persistenceorder'),
(4, 'persistenceorderitem'),
(5, 'persistenceproduct'),
(6, 'persistenceuser'),
(7, 'databasemanagemententity');

-- --------------------------------------------------------

--
-- Table structure for table `data_usage`
--

CREATE TABLE `data_usage` (
  `data_usage_id` int(11) NOT NULL,
  `c` bit(1) NOT NULL,
  `d` bit(1) NOT NULL,
  `data_id` int(11) NOT NULL,
  `personal_status` bit(1) NOT NULL,
  `r` bit(1) NOT NULL,
  `u` bit(1) NOT NULL,
  `processing_processing_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_usage`
--

-- INSERT INTO `data_usage` (`data_usage_id`, `c`, `d`, `data_id`, `personal_status`, `r`, `u`, `processing_processing_id`) VALUES
-- (1, b'1', b'1', 1, b'1', b'1', b'1', 1),
-- (2, b'1', b'0', 2, b'1', b'1', b'0', 1);

-- --------------------------------------------------------

--
-- Table structure for table `measure`
--

CREATE TABLE `measure` (
  `measure_id` int(11) NOT NULL,
  `measure_category` int(11) DEFAULT NULL,
  `measure_description` varchar(255) DEFAULT NULL,
  `measure_type` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `personal_data_category`
--

CREATE TABLE `personal_data_category` (
  `personal_data_category_id` int(11) NOT NULL,
  `personal_data_category_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `personal_data_category`
--

INSERT INTO `personal_data_category` (`personal_data_category_id`, `personal_data_category_name`) VALUES
(1, 'identity'),
(2, 'Contract Information'),
(3, 'Financial');

-- --------------------------------------------------------

--
-- Table structure for table `personal_data_transfer`
--

CREATE TABLE `personal_data_transfer` (
  `personal_data_transfer_id` int(11) NOT NULL,
  `processing_processing_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `personal_data_transfer_data`
--

CREATE TABLE `personal_data_transfer_data` (
  `personal_data_transfer_id` int(11) NOT NULL,
  `data_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `personal_data_transfer_secondary_actor`
--

CREATE TABLE `personal_data_transfer_secondary_actor` (
  `personal_data_transfer_id` int(11) NOT NULL,
  `secondary_actor_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `processed_data`
--

CREATE TABLE `processed_data` (
  `data_id` int(11) NOT NULL,
  `data_subject_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `processed_data`
--

-- INSERT INTO `processed_data` (`data_id`, `data_subject_id`) VALUES
-- (1, 1),
-- (2, 1),
-- (8, 1);

-- --------------------------------------------------------

--
-- Table structure for table `processing`
--

CREATE TABLE `processing` (
  `processing_id` int(11) NOT NULL,
  `created_at` datetime DEFAULT NULL,
  `modified_at` datetime DEFAULT NULL,
  `processing_category` ENUM ('CONSENT', 'LEGITIMATE_INTEREST', 'LEGAL_OBLIGATION', 'PUBLIC_INTEREST', 'VITAL_INTERESTS') DEFAULT NULL,
  `processing_name` varchar(255) DEFAULT NULL,
  `processing_type` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `processing`
--

-- INSERT INTO `processing` (`processing_id`, `created_at`, `modified_at`, `processing_category`, `processing_name`, `processing_type`) VALUES
-- (1, '2024-05-10 00:28:30', '2024-05-23 00:28:30', NULL, 'stat', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `processing_measure`
--

CREATE TABLE `processing_measure` (
  `processing_id` int(11) NOT NULL,
  `measure_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `processing_purposes`
--

CREATE TABLE `processing_purposes` (
  `processing_processing_id` int(11) NOT NULL,
  `purposes_purpose_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `purpose`
--

CREATE TABLE `purpose` (
  `purpose_id` int(11) NOT NULL,
  `purpose_description` varchar(255) NOT NULL,
  `purpose_type` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `secondary_actor`
--

CREATE TABLE `secondary_actor` (
  `secondary_actor_id` int(11) NOT NULL,
  `safeguard` varchar(255) DEFAULT NULL,
  `safeguard_type` int(11) DEFAULT NULL,
  `secondary_actor_address` varchar(255) DEFAULT NULL,
  `secondary_actor_email` varchar(255) DEFAULT NULL,
  `secondary_actor_name` varchar(255) DEFAULT NULL,
  `secondary_actor_phone` varchar(255) DEFAULT NULL,
  `secondary_actor_type` int(11) DEFAULT NULL,
  `country_country_id` int(11) DEFAULT NULL,
  `secondary_actor_category_secondary_actor_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `secondary_actor_category`
--

CREATE TABLE `secondary_actor_category` (
  `secondary_actor_category_id` int(11) NOT NULL,
  `secondary_actor_category_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `consent`
--
ALTER TABLE `consent`
  ADD PRIMARY KEY (`data_subject_id`,`processing_id`);

--
-- Indexes for table `country`
--
ALTER TABLE `country`
  ADD PRIMARY KEY (`country_id`);

--
-- Indexes for table `data`
--
ALTER TABLE `data`
  ADD PRIMARY KEY (`data_id`),
  ADD KEY `FKjeqou18j4cw352pkd7p7xj8mc` (`data_type_id`),
  ADD KEY `FKsd2rj77grnc57ax53lic3noao` (`personal_data_category_id`);

--
-- Indexes for table `data_type`
--
ALTER TABLE `data_type`
  ADD PRIMARY KEY (`data_type_id`);

--
-- Indexes for table `data_usage`
--
ALTER TABLE `data_usage`
  ADD PRIMARY KEY (`data_usage_id`),
  ADD KEY `FK1fex4iifbbuomt7gbd48fb6mx` (`processing_processing_id`);

--
-- Indexes for table `measure`
--
ALTER TABLE `measure`
  ADD PRIMARY KEY (`measure_id`);

--
-- Indexes for table `personal_data_category`
--
ALTER TABLE `personal_data_category`
  ADD PRIMARY KEY (`personal_data_category_id`);

--
-- Indexes for table `personal_data_transfer`
--
ALTER TABLE `personal_data_transfer`
  ADD PRIMARY KEY (`personal_data_transfer_id`),
  ADD KEY `FKjewgw2yns4avxlj7t1j8awb4v` (`processing_processing_id`);

--
-- Indexes for table `personal_data_transfer_data`
--
ALTER TABLE `personal_data_transfer_data`
  ADD KEY `FKonrtdyv9q91bqhvnrvoqf3i57` (`data_id`),
  ADD KEY `FK6s1br404fgwn06y9lq87mared` (`personal_data_transfer_id`);

--
-- Indexes for table `personal_data_transfer_secondary_actor`
--
ALTER TABLE `personal_data_transfer_secondary_actor`
  ADD KEY `FK41nnt1pvq8p2o455391kcyedc` (`secondary_actor_id`),
  ADD KEY `FKent3j0tcyjlnvpd2xfod0soao` (`personal_data_transfer_id`);

--
-- Indexes for table `processed_data`
--
ALTER TABLE `processed_data`
  ADD PRIMARY KEY (`data_id`,`data_subject_id`);

--
-- Indexes for table `processing`
--
ALTER TABLE `processing`
  ADD PRIMARY KEY (`processing_id`);

--
-- Indexes for table `processing_measure`
--
ALTER TABLE `processing_measure`
  ADD KEY `FK640a5vjiekv93lgte9em1ata5` (`measure_id`),
  ADD KEY `FKoclrfe9praecrue7mbv6t8da` (`processing_id`);

--
-- Indexes for table `processing_purposes`
--
ALTER TABLE `processing_purposes`
  ADD UNIQUE KEY `UK_ri36woyv3agt73grbbyj0vuyk` (`purposes_purpose_id`),
  ADD KEY `FKegb7uieypuui8mx8kyun02xse` (`processing_processing_id`);

--
-- Indexes for table `purpose`
--
ALTER TABLE `purpose`
  ADD PRIMARY KEY (`purpose_id`);

--
-- Indexes for table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD PRIMARY KEY (`secondary_actor_id`),
  ADD KEY `FK8du7bowbsefxt341l7u29vgt4` (`country_country_id`),
  ADD KEY `FKs43ufd65t572wc18r098aiugk` (`secondary_actor_category_secondary_actor_category_id`);

--
-- Indexes for table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  ADD PRIMARY KEY (`secondary_actor_category_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `country`
--
ALTER TABLE `country`
  MODIFY `country_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `data`
--
ALTER TABLE `data`
  MODIFY `data_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `data_type`
--
ALTER TABLE `data_type`
  MODIFY `data_type_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=9;

--
-- AUTO_INCREMENT for table `data_usage`
--
ALTER TABLE `data_usage`
  MODIFY `data_usage_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `measure`
--
ALTER TABLE `measure`
  MODIFY `measure_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `personal_data_category`
--
ALTER TABLE `personal_data_category`
  MODIFY `personal_data_category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `personal_data_transfer`
--
ALTER TABLE `personal_data_transfer`
  MODIFY `personal_data_transfer_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `processing`
--
ALTER TABLE `processing`
  MODIFY `processing_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `purpose`
--
ALTER TABLE `purpose`
  MODIFY `purpose_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  MODIFY `secondary_actor_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  MODIFY `secondary_actor_category_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `data`
--
ALTER TABLE `data`
  ADD CONSTRAINT `FKjeqou18j4cw352pkd7p7xj8mc` FOREIGN KEY (`data_type_id`) REFERENCES `data_type` (`data_type_id`),
  ADD CONSTRAINT `FKsd2rj77grnc57ax53lic3noao` FOREIGN KEY (`personal_data_category_id`) REFERENCES `personal_data_category` (`personal_data_category_id`);

--
-- Constraints for table `data_usage`
--
ALTER TABLE `data_usage`
  ADD CONSTRAINT `FK1fex4iifbbuomt7gbd48fb6mx` FOREIGN KEY (`processing_processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Constraints for table `personal_data_transfer`
--
ALTER TABLE `personal_data_transfer`
  ADD CONSTRAINT `FKjewgw2yns4avxlj7t1j8awb4v` FOREIGN KEY (`processing_processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Constraints for table `personal_data_transfer_data`
--
ALTER TABLE `personal_data_transfer_data`
  ADD CONSTRAINT `FK6s1br404fgwn06y9lq87mared` FOREIGN KEY (`personal_data_transfer_id`) REFERENCES `personal_data_transfer` (`personal_data_transfer_id`),
  ADD CONSTRAINT `FKonrtdyv9q91bqhvnrvoqf3i57` FOREIGN KEY (`data_id`) REFERENCES `data` (`data_id`);

--
-- Constraints for table `personal_data_transfer_secondary_actor`
--
ALTER TABLE `personal_data_transfer_secondary_actor`
  ADD CONSTRAINT `FK41nnt1pvq8p2o455391kcyedc` FOREIGN KEY (`secondary_actor_id`) REFERENCES `secondary_actor` (`secondary_actor_id`),
  ADD CONSTRAINT `FKent3j0tcyjlnvpd2xfod0soao` FOREIGN KEY (`personal_data_transfer_id`) REFERENCES `personal_data_transfer` (`personal_data_transfer_id`);

--
-- Constraints for table `processing_measure`
--
ALTER TABLE `processing_measure`
  ADD CONSTRAINT `FK640a5vjiekv93lgte9em1ata5` FOREIGN KEY (`measure_id`) REFERENCES `measure` (`measure_id`),
  ADD CONSTRAINT `FKoclrfe9praecrue7mbv6t8da` FOREIGN KEY (`processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Constraints for table `processing_purposes`
--
ALTER TABLE `processing_purposes`
  ADD CONSTRAINT `FK18wkhdud45orr3i2oswt3081b` FOREIGN KEY (`purposes_purpose_id`) REFERENCES `purpose` (`purpose_id`),
  ADD CONSTRAINT `FKegb7uieypuui8mx8kyun02xse` FOREIGN KEY (`processing_processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Constraints for table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD CONSTRAINT `FK8du7bowbsefxt341l7u29vgt4` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `FKs43ufd65t572wc18r098aiugk` FOREIGN KEY (`secondary_actor_category_secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
