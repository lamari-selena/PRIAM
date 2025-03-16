-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : dim. 16 mars 2025 à 23:47
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
-- Base de données : `priam-consent`
--

-- --------------------------------------------------------

--
-- Structure de la table `consent`
--

CREATE TABLE `consent` (
  `consent_id` int(11) NOT NULL,
  `start_date` date DEFAULT NULL,
  `end_date` date DEFAULT NULL,
  `processing_id` int(11) DEFAULT NULL,
  `contract_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `consent`
--

INSERT INTO `consent` (`consent_id`, `start_date`, `end_date`, `processing_id`, `contract_id`) VALUES
(19, '2024-11-18', '2025-03-13', 3, 3),
(20, '2024-11-18', '2025-03-13', 1, 3),
(21, '2024-12-27', '2024-12-27', 1, 1),
(22, '2024-12-27', '2024-12-27', 1, 1),
(23, '2024-12-27', '2024-12-27', 1, 1),
(24, '2024-12-27', '2024-12-27', 1, 1),
(25, '2024-12-27', NULL, 1, 1),
(26, '2025-03-09', '2025-03-09', 3, 1),
(27, '2025-03-09', NULL, 3, 1),
(28, '2025-03-13', NULL, 1, 3);

-- --------------------------------------------------------

--
-- Structure de la table `contract`
--

CREATE TABLE `contract` (
  `contract_id` int(11) NOT NULL,
  `signature_date` date DEFAULT NULL,
  `expiration_date` date DEFAULT NULL,
  `data_subject_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `contract`
--

INSERT INTO `contract` (`contract_id`, `signature_date`, `expiration_date`, `data_subject_id`) VALUES
(1, '2024-10-14', NULL, 1),
(3, '2024-11-18', NULL, 2),
(4, '2024-12-27', NULL, 6);

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `consent`
--
ALTER TABLE `consent`
  ADD PRIMARY KEY (`consent_id`),
  ADD KEY `processing_id` (`processing_id`),
  ADD KEY `contract_id` (`contract_id`);

--
-- Index pour la table `contract`
--
ALTER TABLE `contract`
  ADD PRIMARY KEY (`contract_id`),
  ADD KEY `data_subject_id` (`data_subject_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `consent`
--
ALTER TABLE `consent`
  MODIFY `consent_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=29;

--
-- AUTO_INCREMENT pour la table `contract`
--
ALTER TABLE `contract`
  MODIFY `contract_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `consent`
--
ALTER TABLE `consent`
  ADD CONSTRAINT `consent_ibfk_1` FOREIGN KEY (`processing_id`) REFERENCES `priam-data`.`processing` (`processing_id`),
  ADD CONSTRAINT `consent_ibfk_2` FOREIGN KEY (`contract_id`) REFERENCES `contract` (`contract_id`);

--
-- Contraintes pour la table `contract`
--
ALTER TABLE `contract`
  ADD CONSTRAINT `contract_ibfk_1` FOREIGN KEY (`data_subject_id`) REFERENCES `priam-actor`.`data_subject` (`data_subject_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
