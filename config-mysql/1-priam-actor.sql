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

CREATE DATABASE IF NOT EXISTS `priam-actor`;
USE `priam-actor`;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `priam-actor1`
--

-- --------------------------------------------------------

--
-- Table structure for table `address`
--

CREATE TABLE `address` (
  `address_id` int(11) NOT NULL,
  `city` varchar(255) DEFAULT NULL,
  `complement` varchar(255) DEFAULT NULL,
  `postal_code` varchar(255) DEFAULT NULL,
  `street_name` varchar(255) DEFAULT NULL,
  `street_number` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
-- Table structure for table `data_subject`
--

CREATE TABLE `data_subject` (
  `data_subject_id` int(11) NOT NULL,
  `age` int(11) NOT NULL,
  `id_ref` varchar(255) DEFAULT NULL,
  `data_subject_category_id` int(11) DEFAULT NULL,
  `data_subject_category_data_subject_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_subject`
--

INSERT INTO `data_subject` (`data_subject_id`, `age`, `id_ref`, `data_subject_category_id`, `data_subject_category_data_subject_category_id`) VALUES
(1, 20, '706', 1, 1),
(2, 20, '508', 1, 1);

-- --------------------------------------------------------

--
-- Table structure for table `data_subject_category`
--

CREATE TABLE `data_subject_category` (
  `data_subject_category_id` int(11) NOT NULL,
  `data_subject_category_name` varchar(255) DEFAULT NULL,
  `location_id` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_subject_category`
--

INSERT INTO `data_subject_category` (`data_subject_category_id`, `data_subject_category_name`, `location_id`) VALUES
(1, 'persistenceuser', 'pu_ID');

-- --------------------------------------------------------

--
-- Table structure for table `dpo`
--

CREATE TABLE `dpo` (
  `dpo_id` int(11) NOT NULL,
  `dpo_email` varchar(255) DEFAULT NULL,
  `dpo_name` varchar(255) DEFAULT NULL,
  `dpo_phone` varchar(255) DEFAULT NULL,
  `country_country_id` int(11) DEFAULT NULL,
  `dpo_address_address_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `provider`
--

CREATE TABLE `provider` (
  `provider_id` int(11) NOT NULL,
  `provider_email` varchar(255) DEFAULT NULL,
  `provider_name` varchar(255) DEFAULT NULL,
  `provider_phone` varchar(255) DEFAULT NULL,
  `country_country_id` int(11) DEFAULT NULL,
  `provider_address_address_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Table structure for table `representative`
--

CREATE TABLE `representative` (
  `representative_id` int(11) NOT NULL,
  `representative_email` varchar(255) DEFAULT NULL,
  `representative_name` varchar(255) DEFAULT NULL,
  `representative_phone` varchar(255) DEFAULT NULL,
  `country_country_id` int(11) DEFAULT NULL,
  `representative_address_address_id` int(11) DEFAULT NULL
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

-- --------------------------------------------------------

--
-- Table structure for table `tutor`
--

CREATE TABLE `tutor` (
  `tutor_id` int(11) NOT NULL,
  `tutor_name` varchar(255) DEFAULT NULL,
  `country_country_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `address`
--
ALTER TABLE `address`
  ADD PRIMARY KEY (`address_id`);

--
-- Indexes for table `country`
--
ALTER TABLE `country`
  ADD PRIMARY KEY (`country_id`);

--
-- Indexes for table `data_subject`
--
ALTER TABLE `data_subject`
  ADD PRIMARY KEY (`data_subject_id`),
  ADD KEY `FK7gkh80jsld0t7nnfeqiryw8oi` (`data_subject_category_id`);

--
-- Indexes for table `data_subject_category`
--
ALTER TABLE `data_subject_category`
  ADD PRIMARY KEY (`data_subject_category_id`);

--
-- Indexes for table `dpo`
--
ALTER TABLE `dpo`
  ADD PRIMARY KEY (`dpo_id`),
  ADD KEY `FKin65oalt1y6sj1k5iy6b4tq0w` (`country_country_id`),
  ADD KEY `FKpytcbmj42upevq1hehg26rij7` (`dpo_address_address_id`);

--
-- Indexes for table `provider`
--
ALTER TABLE `provider`
  ADD PRIMARY KEY (`provider_id`),
  ADD KEY `FKba33tgbqilq5v2xm6i9vdqjvm` (`country_country_id`),
  ADD KEY `FKe3iqhxo7vgbbqmqlt6igsgqg7` (`provider_address_address_id`);

--
-- Indexes for table `representative`
--
ALTER TABLE `representative`
  ADD PRIMARY KEY (`representative_id`),
  ADD KEY `FKqhko8ugjgipq3u31h1pg9axmi` (`country_country_id`),
  ADD KEY `FKjkle3705ruou9jildnxao3v2l` (`representative_address_address_id`);

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
-- Indexes for table `tutor`
--
ALTER TABLE `tutor`
  ADD PRIMARY KEY (`tutor_id`),
  ADD KEY `FKlt9d8oivxy4nmjwtbuarxcorp` (`country_country_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `address`
--
ALTER TABLE `address`
  MODIFY `address_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `country`
--
ALTER TABLE `country`
  MODIFY `country_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `data_subject`
--
ALTER TABLE `data_subject`
  MODIFY `data_subject_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT for table `data_subject_category`
--
ALTER TABLE `data_subject_category`
  MODIFY `data_subject_category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `dpo`
--
ALTER TABLE `dpo`
  MODIFY `dpo_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `provider`
--
ALTER TABLE `provider`
  MODIFY `provider_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `representative`
--
ALTER TABLE `representative`
  MODIFY `representative_id` int(11) NOT NULL AUTO_INCREMENT;

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
-- AUTO_INCREMENT for table `tutor`
--
ALTER TABLE `tutor`
  MODIFY `tutor_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `data_subject`
--
ALTER TABLE `data_subject`
  ADD CONSTRAINT `FK7gkh80jsld0t7nnfeqiryw8oi` FOREIGN KEY (`data_subject_category_id`) REFERENCES `data_subject_category` (`data_subject_category_id`);

--
-- Constraints for table `dpo`
--
ALTER TABLE `dpo`
  ADD CONSTRAINT `FKin65oalt1y6sj1k5iy6b4tq0w` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `FKpytcbmj42upevq1hehg26rij7` FOREIGN KEY (`dpo_address_address_id`) REFERENCES `address` (`address_id`);

--
-- Constraints for table `provider`
--
ALTER TABLE `provider`
  ADD CONSTRAINT `FKba33tgbqilq5v2xm6i9vdqjvm` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `FKe3iqhxo7vgbbqmqlt6igsgqg7` FOREIGN KEY (`provider_address_address_id`) REFERENCES `address` (`address_id`);

--
-- Constraints for table `representative`
--
ALTER TABLE `representative`
  ADD CONSTRAINT `FKjkle3705ruou9jildnxao3v2l` FOREIGN KEY (`representative_address_address_id`) REFERENCES `address` (`address_id`),
  ADD CONSTRAINT `FKqhko8ugjgipq3u31h1pg9axmi` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`);

--
-- Constraints for table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD CONSTRAINT `FK8du7bowbsefxt341l7u29vgt4` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `FKs43ufd65t572wc18r098aiugk` FOREIGN KEY (`secondary_actor_category_secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`);

--
-- Constraints for table `tutor`
--
ALTER TABLE `tutor`
  ADD CONSTRAINT `FKlt9d8oivxy4nmjwtbuarxcorp` FOREIGN KEY (`country_country_id`) REFERENCES `country` (`country_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
