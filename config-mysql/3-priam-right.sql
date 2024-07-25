-- phpMyAdmin SQL Dump
-- version 5.2.0
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jun 10, 2024 at 06:56 PM
-- Server version: 10.4.24-MariaDB
-- PHP Version: 8.1.6

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

CREATE DATABASE IF NOT EXISTS `priam-right`;
USE `priam-right`;

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `priam-right1`
--

-- --------------------------------------------------------

--
-- Table structure for table `data_request`
--

CREATE TABLE `data_request` (
  `data_request_id` int(11) NOT NULL,
  `data_request_claim` varchar(255) DEFAULT NULL,
  `data_request_issued_at` datetime DEFAULT NULL,
  `data_request_type` int(11) DEFAULT NULL,
  `data_subject_id` int(11) NOT NULL,
  `is_isolated` bit(1) NOT NULL,
  `new_value` varchar(255) DEFAULT NULL,
  `response` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_request`
--

INSERT INTO `data_request` (`data_request_id`, `data_request_claim`, `data_request_issued_at`, `data_request_type`, `data_subject_id`, `is_isolated`, `new_value`, `response`) VALUES
(1, 'error address', '2024-05-09 16:38:34', 0, 1, b'1', 'Address 1', b'1'),
(2, 'rectif error', '2024-05-27 00:05:05', 0, 1, b'0', 'sss', b'1'),
(3, 'yes', '2024-05-09 02:30:44', 0, 1, b'1', 'user0@petsupplystore.fr', b'0');

-- --------------------------------------------------------

--
-- Table structure for table `data_request_answer`
--

CREATE TABLE `data_request_answer` (
  `data_request_answer_id` int(11) NOT NULL,
  `answer` int(11) DEFAULT NULL,
  `data_request_claim` varchar(255) DEFAULT NULL,
  `data_request_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_request_answer`
--

INSERT INTO `data_request_answer` (`data_request_answer_id`, `answer`, `data_request_claim`, `data_request_id`) VALUES
(1, 0, 's<br>', 1),
(2, 1, 'no', 2),
(3, 0, 'yes', 3);

-- --------------------------------------------------------

--
-- Table structure for table `data_request_data`
--

CREATE TABLE `data_request_data` (
  `data_id` int(11) NOT NULL,
  `data_request_id` int(11) NOT NULL,
  `answer_by_data` bit(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_request_data`
--

INSERT INTO `data_request_data` (`data_id`, `data_request_id`, `answer_by_data`) VALUES
(2, 2, b'0'),
(8, 3, b'1');

-- --------------------------------------------------------

--
-- Table structure for table `data_request_primary_key`
--

CREATE TABLE `data_request_primary_key` (
  `data_request_id` int(11) NOT NULL,
  `primary_key_id` int(11) NOT NULL,
  `primary_key_value` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Dumping data for table `data_request_primary_key`
--

INSERT INTO `data_request_primary_key` (`data_request_id`, `primary_key_id`, `primary_key_value`) VALUES
(2, 3, '607');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `data_request`
--
ALTER TABLE `data_request`
  ADD PRIMARY KEY (`data_request_id`);

--
-- Indexes for table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  ADD PRIMARY KEY (`data_request_answer_id`),
  ADD KEY `FK8jpx5fwb3tal6v2log7agiyfe` (`data_request_id`);

--
-- Indexes for table `data_request_data`
--
ALTER TABLE `data_request_data`
  ADD PRIMARY KEY (`data_id`,`data_request_id`);

--
-- Indexes for table `data_request_primary_key`
--
ALTER TABLE `data_request_primary_key`
  ADD PRIMARY KEY (`data_request_id`,`primary_key_id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `data_request`
--
ALTER TABLE `data_request`
  MODIFY `data_request_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  MODIFY `data_request_answer_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  ADD CONSTRAINT `FK8jpx5fwb3tal6v2log7agiyfe` FOREIGN KEY (`data_request_id`) REFERENCES `data_request` (`data_request_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
