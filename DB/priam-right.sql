-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : sam. 12 oct. 2024 à 23:51
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
  `data_request_type` varchar(25) DEFAULT NULL CHECK (`data_request_type` in ('RECTIFICATION','ERASURE','ACCESS')),
  `data_subject_id` int(11) DEFAULT NULL,
  `response` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request`
--

INSERT INTO `data_request` (`data_request_id`, `data_request_claim`, `data_request_issued_at`, `new_value`, `is_isolated`, `data_request_type`, `data_subject_id`, `response`) VALUES
(1, 'er', '2024-10-10 01:01:06', '3600 East 1509 North ', 1, 'RECTIFICATION', 1, 1),
(2, 'der', '2024-10-11 16:39:44', 'user0@petsupplystore.fr ', 1, 'RECTIFICATION', 1, 0),
(3, 'erreur', '2024-10-11 16:53:28', '360 East 1509 North', 1, 'RECTIFICATION', 1, 0),
(4, 'err', '2024-10-12 22:47:07', '36000 East 1509 North ', 1, 'RECTIFICATION', 1, 0);

-- --------------------------------------------------------

--
-- Structure de la table `data_request_answer`
--

CREATE TABLE `data_request_answer` (
  `data_request_answer_id` int(11) NOT NULL,
  `answer` varchar(7) DEFAULT NULL CHECK (`answer` in ('FULL','PARTIAL','REFUSED')),
  `data_request_claim` varchar(250) DEFAULT NULL,
  `data_request_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_request_answer`
--

INSERT INTO `data_request_answer` (`data_request_answer_id`, `answer`, `data_request_claim`, `data_request_id`) VALUES
(1, 'FULL', 'YES', 3),
(2, 'FULL', 'yes', 4);

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
(3, 1, 1),
(4, 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `data_request_primary_key`
--

CREATE TABLE `data_request_primary_key` (
  `data_request_id` int(11) NOT NULL,
  `primary_key_id` int(11) NOT NULL,
  `primary_key_value` varchar(50) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `processing_request`
--

CREATE TABLE `processing_request` (
  `processing_request_id` int(11) NOT NULL,
  `processing_request_claim` varchar(250) DEFAULT NULL,
  `processing_request_issued_at` datetime DEFAULT NULL,
  `processing_request_type` varchar(25) DEFAULT NULL CHECK (`processing_request_type` in ('Objection','Restriction')),
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
  MODIFY `data_request_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `data_request_answer`
--
ALTER TABLE `data_request_answer`
  MODIFY `data_request_answer_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

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
