-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : lun. 17 mars 2025 à 00:17
-- Version du serveur : 10.4.32-MariaDB
-- Version de PHP : 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `priam-right`
--

-- --------------------------------------------------------

--
-- Structure de la table `data_request`
--

CREATE TABLE `data_request` (
  `data_request_id` int(11) NOT NULL,
  `data_request_claim` varchar(250) DEFAULT NULL,
  `data_request_issued_at` datetime DEFAULT NULL,
  `new_value` varchar(250) DEFAULT NULL,
  `is_isolated` tinyint(1) DEFAULT 0,
  `data_request_type` varchar(25) DEFAULT NULL CHECK (`data_request_type` in ('Rectification','Erasure','Access')),
  `data_subject_id` int(11) DEFAULT NULL,
  `response` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request`
--

INSERT INTO `data_request` (`data_request_id`, `data_request_claim`, `data_request_issued_at`, `new_value`, `is_isolated`, `data_request_type`, `data_subject_id`, `response`) VALUES
(6, 'erreur', '2024-11-04 14:11:43', 'Richard Haris', 1, 'RECTIFICATION', 1, 0),
(7, 'supp', '2024-11-04 14:14:23', NULL, 1, 'ERASURE', 1, 0),
(8, 'err', '2024-11-04 14:30:08', 'Helene', 1, 'RECTIFICATION', 1, 0),
(9, 'rr', '2024-11-04 14:43:12', 'helene', 1, 'RECTIFICATION', 1, 0),
(10, 'hggg', '2024-11-04 14:45:35', 'helene', 1, 'RECTIFICATION', 1, 0),
(11, 'erreur', '2024-11-18 01:44:07', 'Helen Johnson', 1, 'RECTIFICATION', 1, 0),
(12, 'erreur de nom', '2024-11-18 10:28:05', 'Helen Johnson', 1, 'RECTIFICATION', 1, 0),
(13, 'errur2', '2024-11-20 23:20:18', 'Helen Johnson', 1, 'RECTIFICATION', 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `data_request_answer`
--

CREATE TABLE `data_request_answer` (
  `data_request_answer_id` int(11) NOT NULL,
  `answer` varchar(7) DEFAULT NULL CHECK (`answer` in ('Full','Partial','Refused')),
  `data_request_claim` varchar(250) DEFAULT NULL,
  `data_request_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request_answer`
--

INSERT INTO `data_request_answer` (`data_request_answer_id`, `answer`, `data_request_claim`, `data_request_id`) VALUES
(3, 'REFUSED', 'ee', 6),
(4, 'FULL', 'ui', 8),
(5, 'FULL', 'yes', 9),
(6, 'FULL', 'yes', 10),
(7, 'FULL', 'ytr', 7),
(8, 'FULL', 'yes', 12),
(9, 'FULL', 'yes', 13);

-- --------------------------------------------------------

--
-- Structure de la table `data_request_data`
--

CREATE TABLE `data_request_data` (
  `data_request_id` int(11) NOT NULL,
  `data_id` int(11) NOT NULL,
  `answer_by_data` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request_data`
--

INSERT INTO `data_request_data` (`data_request_id`, `data_id`, `answer_by_data`) VALUES
(6, 5, 0),
(7, 5, 1),
(8, 5, 1),
(9, 5, 1),
(10, 5, 1),
(11, 5, 0),
(12, 5, 1),
(13, 5, 1);

-- --------------------------------------------------------

--
-- Structure de la table `data_request_primary_key`
--

CREATE TABLE `data_request_primary_key` (
  `data_request_id` int(11) NOT NULL,
  `primary_key_id` int(11) NOT NULL,
  `primary_key_value` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request_primary_key`
--

INSERT INTO `data_request_primary_key` (`data_request_id`, `primary_key_id`, `primary_key_value`) VALUES
(6, 1, '507'),
(7, 1, '507'),
(8, 1, '507'),
(9, 1, '507'),
(10, 1, '507');

-- --------------------------------------------------------

--
-- Structure de la table `processing_request`
--

CREATE TABLE `processing_request` (
  `processing_request_id` int(11) NOT NULL,
  `processing_request_claim` varchar(250) DEFAULT NULL,
  `processing_request_issued_at` datetime DEFAULT NULL,
  `processing_request_type` varchar(25) DEFAULT NULL CHECK (`processing_request_type` in ('OBJECTION','RESTRICTION')),
  `data_subject_id` int(11) DEFAULT NULL,
  `processing_id` int(11) DEFAULT NULL,
  `response` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `processing_request_answer`
--

CREATE TABLE `processing_request_answer` (
  `processing_request_answer_id` int(11) NOT NULL,
  `answer` varchar(7) DEFAULT NULL CHECK (`answer` in ('Full','Partial','Refused')),
  `processing_request_answer_claim` varchar(250) DEFAULT NULL,
  `processing_request_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `data_request`
--
ALTER TABLE `data_request`
  ADD PRIMARY KEY (`data_request_id`),
  ADD KEY `data_subject_id` (`data_subject_id`);

--
-- Index pour la table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  ADD PRIMARY KEY (`data_request_answer_id`),
  ADD KEY `data_request_id` (`data_request_id`);

--
-- Index pour la table `data_request_data`
--
ALTER TABLE `data_request_data`
  ADD PRIMARY KEY (`data_request_id`,`data_id`),
  ADD KEY `data_id` (`data_id`);

--
-- Index pour la table `data_request_primary_key`
--
ALTER TABLE `data_request_primary_key`
  ADD PRIMARY KEY (`data_request_id`,`primary_key_id`),
  ADD KEY `primary_key_id` (`primary_key_id`);

--
-- Index pour la table `processing_request`
--
ALTER TABLE `processing_request`
  ADD PRIMARY KEY (`processing_request_id`),
  ADD KEY `data_subject_id` (`data_subject_id`),
  ADD KEY `processing_id` (`processing_id`);

--
-- Index pour la table `processing_request_answer`
--
ALTER TABLE `processing_request_answer`
  ADD PRIMARY KEY (`processing_request_answer_id`),
  ADD KEY `processing_request_id` (`processing_request_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `data_request`
--
ALTER TABLE `data_request`
  MODIFY `data_request_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT pour la table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  MODIFY `data_request_answer_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `data_request`
--
ALTER TABLE `data_request`
  ADD CONSTRAINT `data_request_ibfk_1` FOREIGN KEY (`data_subject_id`) REFERENCES `priam-actor`.`data_subject` (`data_subject_id`);

--
-- Contraintes pour la table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  ADD CONSTRAINT `data_request_answer_ibfk_1` FOREIGN KEY (`data_request_id`) REFERENCES `data_request` (`data_request_id`);

--
-- Contraintes pour la table `data_request_data`
--
ALTER TABLE `data_request_data`
  ADD CONSTRAINT `data_request_data_ibfk_1` FOREIGN KEY (`data_request_id`) REFERENCES `data_request` (`data_request_id`),
  ADD CONSTRAINT `data_request_data_ibfk_2` FOREIGN KEY (`data_id`) REFERENCES `priam-data`.`data` (`data_id`);

--
-- Contraintes pour la table `data_request_primary_key`
--
ALTER TABLE `data_request_primary_key`
  ADD CONSTRAINT `data_request_primary_key_ibfk_1` FOREIGN KEY (`data_request_id`) REFERENCES `data_request` (`data_request_id`),
  ADD CONSTRAINT `data_request_primary_key_ibfk_2` FOREIGN KEY (`primary_key_id`) REFERENCES `priam-data`.`data` (`data_id`);

--
-- Contraintes pour la table `processing_request`
--
ALTER TABLE `processing_request`
  ADD CONSTRAINT `processing_request_ibfk_1` FOREIGN KEY (`data_subject_id`) REFERENCES `priam-actor`.`data_subject` (`data_subject_id`),
  ADD CONSTRAINT `processing_request_ibfk_2` FOREIGN KEY (`processing_id`) REFERENCES `priam-data`.`processing` (`processing_id`);

--
-- Contraintes pour la table `processing_request_answer`
--
ALTER TABLE `processing_request_answer`
  ADD CONSTRAINT `processing_request_answer_ibfk_1` FOREIGN KEY (`processing_request_id`) REFERENCES `processing_request` (`processing_request_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
