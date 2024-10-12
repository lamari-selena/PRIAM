-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : sam. 12 oct. 2024 à 23:50
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
-- Base de données : `priam-data`
--

-- --------------------------------------------------------

--
-- Structure de la table `consent`
--

CREATE TABLE `consent` (
  `data_subject_id` int(11) NOT NULL,
  `processing_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `country`
--

CREATE TABLE `country` (
  `country_id` int(11) NOT NULL,
  `adequate` bit(1) NOT NULL,
  `country_name` varchar(255) DEFAULT NULL,
  `minor_age` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `country`
--

INSERT INTO `country` (`country_id`, `adequate`, `country_name`, `minor_age`) VALUES
(2, b'1', 'France', 14);

-- --------------------------------------------------------

--
-- Structure de la table `data`
--

CREATE TABLE `data` (
  `data_id` int(11) NOT NULL,
  `data_name` varchar(25) DEFAULT NULL,
  `source` varchar(25) DEFAULT NULL,
  `source_details` varchar(255) DEFAULT NULL,
  `data_conservation_duration` int(11) DEFAULT NULL,
  `is_personal` tinyint(1) DEFAULT NULL,
  `is_portable` tinyint(1) DEFAULT NULL,
  `is_primary_key` tinyint(1) DEFAULT NULL,
  `data_type_id` int(11) DEFAULT NULL,
  `personal_data_category_id` int(11) DEFAULT NULL,
  `data_subject_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data`
--

INSERT INTO `data` (`data_id`, `data_name`, `source`, `source_details`, `data_conservation_duration`, `is_personal`, `is_portable`, `is_primary_key`, `data_type_id`, `personal_data_category_id`, `data_subject_category_id`) VALUES
(1, 'po_ADDRESS1', '0', NULL, 12, 1, 1, 0, 3, 2, 1),
(2, 'po_ADDRESS2', '0', NULL, 12, 1, 1, 0, 3, 2, 1),
(3, 'po_ID', '1', NULL, 12, 1, 1, 1, 3, 1, 1),
(4, 'pu_ID', '2', NULL, 12, 1, 1, 1, 6, 2, 1),
(8, 'pu_EMAIL', '0', NULL, 12, 1, 1, 0, 6, 2, 1),
(10, 'sdsd', '0', 'ed', 12, 1, 1, 0, 1, 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `data_type`
--

CREATE TABLE `data_type` (
  `data_type_id` int(11) NOT NULL,
  `data_type_name` varchar(40) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_type`
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
-- Structure de la table `data_usage`
--

CREATE TABLE `data_usage` (
  `data_usage_id` int(11) NOT NULL,
  `personal_status` tinyint(1) DEFAULT 0,
  `c` tinyint(1) DEFAULT 0,
  `r` tinyint(1) DEFAULT 0,
  `u` tinyint(1) DEFAULT 0,
  `d` tinyint(1) DEFAULT 0,
  `data_id` int(11) DEFAULT NULL,
  `processing_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_usage`
--

INSERT INTO `data_usage` (`data_usage_id`, `personal_status`, `c`, `r`, `u`, `d`, `data_id`, `processing_id`) VALUES
(1, 1, 1, 1, 1, 1, 1, 1),
(2, 1, 1, 1, 0, 0, 2, 1);

-- --------------------------------------------------------

--
-- Structure de la table `measure`
--

CREATE TABLE `measure` (
  `measure_id` int(11) NOT NULL,
  `measure_description` varchar(255) DEFAULT NULL,
  `measure_type` varchar(15) DEFAULT NULL CHECK (`measure_type` in ('Organisational','Technical')),
  `measure_category` varchar(20) DEFAULT NULL CHECK (`measure_category` in ('Cryption','Anonymisation','Physical_Security','Training','Access_Control','Data_Disposal','Policy_Management'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `measure`
--

INSERT INTO `measure` (`measure_id`, `measure_description`, `measure_type`, `measure_category`) VALUES
(1, 'Use of a firewall ', 'Technical', 'Physical_Security'),
(2, 'Encryption of data carriers and data transfers to ensure data confidentiality during transmission and storage.', 'Technical', 'Cryption'),
(3, 'Pseudonymisation and encryption of personal data to minimize the risk of identification.', 'Technical', 'Anonymisation'),
(4, ' Installation of an alarm system to enhance physical security of premises.', 'Technical', 'Physical_Security'),
(5, 'Structural protection of buildings/premises to prevent unauthorized access.', 'Technical', 'Physical_Security'),
(6, 'Defaults for the password complexity of users (e.g., FIDO2) to improve account security.', 'Technical', 'Access_Control'),
(7, 'Employee training on data protection to ensure compliance and awareness.', 'Organisational', 'Training'),
(8, 'Visitor registration to monitor access to sensitive areas.', 'Organisational', 'Access_Control'),
(9, 'Data protection-compliant disposal of documents containing personal data', 'Organisational', 'Data_Disposal'),
(10, 'Establishment of clear data access policies to define who can access what data.', 'Organisational', 'Policy_Management');

-- --------------------------------------------------------

--
-- Structure de la table `personal_data_category`
--

CREATE TABLE `personal_data_category` (
  `personal_data_category_id` int(11) NOT NULL,
  `personal_data_category_name` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `personal_data_category`
--

INSERT INTO `personal_data_category` (`personal_data_category_id`, `personal_data_category_name`) VALUES
(1, 'identity'),
(2, 'Contract Information'),
(3, 'Financial');

-- --------------------------------------------------------

--
-- Structure de la table `personal_data_transfer`
--

CREATE TABLE `personal_data_transfer` (
  `Personal_data_transfer_id` int(11) NOT NULL,
  `processing_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `personal_data_transfer`
--

INSERT INTO `personal_data_transfer` (`Personal_data_transfer_id`, `processing_id`) VALUES
(1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `personal_data_transfer_data`
--

CREATE TABLE `personal_data_transfer_data` (
  `personal_data_transfer_id` int(11) NOT NULL,
  `data_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `personal_data_transfer_data`
--

INSERT INTO `personal_data_transfer_data` (`personal_data_transfer_id`, `data_id`) VALUES
(1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `personal_data_transfer_secondary_actor`
--

CREATE TABLE `personal_data_transfer_secondary_actor` (
  `Personal_data_transfer_id` int(11) NOT NULL,
  `secondary_actor_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `personal_data_transfer_secondary_actor`
--

INSERT INTO `personal_data_transfer_secondary_actor` (`Personal_data_transfer_id`, `secondary_actor_id`) VALUES
(1, 10);

-- --------------------------------------------------------

--
-- Structure de la table `processed_data`
--

CREATE TABLE `processed_data` (
  `data_id` int(11) NOT NULL,
  `data_subject_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `processed_data`
--

INSERT INTO `processed_data` (`data_id`, `data_subject_id`) VALUES
(1, 1),
(2, 2),
(8, 1);

-- --------------------------------------------------------

--
-- Structure de la table `processing`
--

CREATE TABLE `processing` (
  `processing_id` int(11) NOT NULL,
  `processing_name` varchar(25) DEFAULT NULL,
  `processing_type` varchar(25) DEFAULT NULL CHECK (`processing_type` in ('Default','Mandatory','Optional','Necessary')),
  `processing_category` varchar(25) DEFAULT NULL CHECK (`processing_category` in ('Consent_Contract','LegitimateInterest','LegalObligation','publicInterest','VitalInterests')),
  `created_at` date DEFAULT NULL,
  `modified_at` date DEFAULT NULL,
  `ended_at` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `processing`
--

INSERT INTO `processing` (`processing_id`, `processing_name`, `processing_type`, `processing_category`, `created_at`, `modified_at`, `ended_at`) VALUES
(1, 'recomended', 'OPTIONAL', 'Consent_Contract', '2024-05-10', '2024-05-23', NULL);

-- --------------------------------------------------------

--
-- Structure de la table `processing_link`
--

CREATE TABLE `processing_link` (
  `processing1` int(11) NOT NULL,
  `processing2` int(11) NOT NULL,
  `type_of_link` varchar(20) DEFAULT NULL
) ;

-- --------------------------------------------------------

--
-- Structure de la table `processing_measure`
--

CREATE TABLE `processing_measure` (
  `measure_id` int(11) NOT NULL,
  `processing_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `processing_purposes`
--

CREATE TABLE `processing_purposes` (
  `processing_processing_id` int(11) NOT NULL,
  `purposes_purpose_id` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `processing_purposes`
--

INSERT INTO `processing_purposes` (`processing_processing_id`, `purposes_purpose_id`) VALUES
(1, 1),
(1, 2);

-- --------------------------------------------------------

--
-- Structure de la table `purpose`
--

CREATE TABLE `purpose` (
  `purpose_id` int(11) NOT NULL,
  `purpose_description` varchar(200) NOT NULL,
  `purpose_type` varchar(10) DEFAULT NULL CHECK (`purpose_type` in ('Main','Secondary')),
  `processing_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `purpose`
--

INSERT INTO `purpose` (`purpose_id`, `purpose_description`, `purpose_type`, `processing_id`) VALUES
(1, 'faire des statistiques et chiffres', 'MAIN', 1),
(2, 'publicités', 'MAIN', 1);

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor`
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
  `country_id` int(11) DEFAULT NULL,
  `secondary_actor_category_secondary_actor_category_id` int(11) DEFAULT NULL,
  `secondary_actor_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `secondary_actor`
--

INSERT INTO `secondary_actor` (`secondary_actor_id`, `safeguard`, `safeguard_type`, `secondary_actor_address`, `secondary_actor_email`, `secondary_actor_name`, `secondary_actor_phone`, `secondary_actor_type`, `country_id`, `secondary_actor_category_secondary_actor_category_id`, `secondary_actor_category_id`) VALUES
(10, NULL, 1, NULL, 'auth@xx.com', 'authority', '043987283', 1, 2, 1, 1);

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor_category`
--

CREATE TABLE `secondary_actor_category` (
  `secondary_actor_category_id` int(11) NOT NULL,
  `secondary_actor_category_name` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `secondary_actor_category`
--

INSERT INTO `secondary_actor_category` (`secondary_actor_category_id`, `secondary_actor_category_name`) VALUES
(1, 'Commercant');

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `consent`
--
ALTER TABLE `consent`
  ADD PRIMARY KEY (`data_subject_id`,`processing_id`);

--
-- Index pour la table `country`
--
ALTER TABLE `country`
  ADD PRIMARY KEY (`country_id`);

--
-- Index pour la table `data`
--
ALTER TABLE `data`
  ADD PRIMARY KEY (`data_id`),
  ADD KEY `data_subject_category_id` (`data_subject_category_id`),
  ADD KEY `data_type_id` (`data_type_id`),
  ADD KEY `personal_data_category_id` (`personal_data_category_id`);

--
-- Index pour la table `data_type`
--
ALTER TABLE `data_type`
  ADD PRIMARY KEY (`data_type_id`);

--
-- Index pour la table `data_usage`
--
ALTER TABLE `data_usage`
  ADD PRIMARY KEY (`data_usage_id`),
  ADD KEY `data_id` (`data_id`),
  ADD KEY `processing_id` (`processing_id`);

--
-- Index pour la table `measure`
--
ALTER TABLE `measure`
  ADD PRIMARY KEY (`measure_id`);

--
-- Index pour la table `personal_data_category`
--
ALTER TABLE `personal_data_category`
  ADD PRIMARY KEY (`personal_data_category_id`);

--
-- Index pour la table `personal_data_transfer`
--
ALTER TABLE `personal_data_transfer`
  ADD PRIMARY KEY (`Personal_data_transfer_id`),
  ADD KEY `processing_id` (`processing_id`);

--
-- Index pour la table `personal_data_transfer_data`
--
ALTER TABLE `personal_data_transfer_data`
  ADD PRIMARY KEY (`personal_data_transfer_id`,`data_id`),
  ADD KEY `data_id` (`data_id`);

--
-- Index pour la table `personal_data_transfer_secondary_actor`
--
ALTER TABLE `personal_data_transfer_secondary_actor`
  ADD PRIMARY KEY (`Personal_data_transfer_id`,`secondary_actor_id`),
  ADD KEY `secondary_actor_id` (`secondary_actor_id`);

--
-- Index pour la table `processed_data`
--
ALTER TABLE `processed_data`
  ADD PRIMARY KEY (`data_id`,`data_subject_id`),
  ADD KEY `data_subject_id` (`data_subject_id`);

--
-- Index pour la table `processing`
--
ALTER TABLE `processing`
  ADD PRIMARY KEY (`processing_id`);

--
-- Index pour la table `processing_link`
--
ALTER TABLE `processing_link`
  ADD PRIMARY KEY (`processing1`,`processing2`),
  ADD KEY `processing2` (`processing2`);

--
-- Index pour la table `processing_measure`
--
ALTER TABLE `processing_measure`
  ADD PRIMARY KEY (`measure_id`,`processing_id`),
  ADD KEY `processing_id` (`processing_id`);

--
-- Index pour la table `processing_purposes`
--
ALTER TABLE `processing_purposes`
  ADD UNIQUE KEY `UK_ri36woyv3agt73grbbyj0vuyk` (`purposes_purpose_id`),
  ADD KEY `FKegb7uieypuui8mx8kyun02xse` (`processing_processing_id`);

--
-- Index pour la table `purpose`
--
ALTER TABLE `purpose`
  ADD PRIMARY KEY (`purpose_id`),
  ADD KEY `processing_id` (`processing_id`);

--
-- Index pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD PRIMARY KEY (`secondary_actor_id`),
  ADD KEY `FKijrjuuce5ik1ym56il6o3mxyu` (`country_id`),
  ADD KEY `FKs43ufd65t572wc18r098aiugk` (`secondary_actor_category_secondary_actor_category_id`),
  ADD KEY `FKeahjwfv1ibxl8ktyffa4nfuwx` (`secondary_actor_category_id`);

--
-- Index pour la table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  ADD PRIMARY KEY (`secondary_actor_category_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `country`
--
ALTER TABLE `country`
  MODIFY `country_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `data_usage`
--
ALTER TABLE `data_usage`
  MODIFY `data_usage_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `purpose`
--
ALTER TABLE `purpose`
  MODIFY `purpose_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  MODIFY `secondary_actor_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  MODIFY `secondary_actor_category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `data`
--
ALTER TABLE `data`
  ADD CONSTRAINT `data_ibfk_1` FOREIGN KEY (`data_subject_category_id`) REFERENCES `priam-actor`.`data_subject_category` (`data_subject_category_id`),
  ADD CONSTRAINT `data_ibfk_2` FOREIGN KEY (`data_type_id`) REFERENCES `data_type` (`data_type_id`),
  ADD CONSTRAINT `data_ibfk_3` FOREIGN KEY (`personal_data_category_id`) REFERENCES `personal_data_category` (`personal_data_category_id`);

--
-- Contraintes pour la table `data_usage`
--
ALTER TABLE `data_usage`
  ADD CONSTRAINT `data_usage_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `data` (`data_id`),
  ADD CONSTRAINT `data_usage_ibfk_2` FOREIGN KEY (`processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `personal_data_transfer`
--
ALTER TABLE `personal_data_transfer`
  ADD CONSTRAINT `personal_data_transfer_ibfk_1` FOREIGN KEY (`processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `personal_data_transfer_data`
--
ALTER TABLE `personal_data_transfer_data`
  ADD CONSTRAINT `FK6s1br404fgwn06y9lq87mared` FOREIGN KEY (`personal_data_transfer_id`) REFERENCES `personal_data_transfer` (`Personal_data_transfer_id`),
  ADD CONSTRAINT `personal_data_transfer_data_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `data` (`data_id`);

--
-- Contraintes pour la table `personal_data_transfer_secondary_actor`
--
ALTER TABLE `personal_data_transfer_secondary_actor`
  ADD CONSTRAINT `personal_data_transfer_secondary_actor_ibfk_1` FOREIGN KEY (`Personal_data_transfer_id`) REFERENCES `personal_data_transfer` (`Personal_data_transfer_id`),
  ADD CONSTRAINT `personal_data_transfer_secondary_actor_ibfk_2` FOREIGN KEY (`secondary_actor_id`) REFERENCES `priam-actor`.`secondary_actor` (`secondary_actor_id`);

--
-- Contraintes pour la table `processed_data`
--
ALTER TABLE `processed_data`
  ADD CONSTRAINT `processed_data_ibfk_1` FOREIGN KEY (`data_id`) REFERENCES `data` (`data_id`),
  ADD CONSTRAINT `processed_data_ibfk_2` FOREIGN KEY (`data_subject_id`) REFERENCES `priam-actor`.`data_subject` (`data_subject_id`);

--
-- Contraintes pour la table `processing_link`
--
ALTER TABLE `processing_link`
  ADD CONSTRAINT `processing_link_ibfk_1` FOREIGN KEY (`processing1`) REFERENCES `processing` (`processing_id`),
  ADD CONSTRAINT `processing_link_ibfk_2` FOREIGN KEY (`processing2`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `processing_measure`
--
ALTER TABLE `processing_measure`
  ADD CONSTRAINT `processing_measure_ibfk_1` FOREIGN KEY (`measure_id`) REFERENCES `measure` (`measure_id`),
  ADD CONSTRAINT `processing_measure_ibfk_2` FOREIGN KEY (`processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `processing_purposes`
--
ALTER TABLE `processing_purposes`
  ADD CONSTRAINT `FK18wkhdud45orr3i2oswt3081b` FOREIGN KEY (`purposes_purpose_id`) REFERENCES `purpose` (`purpose_id`),
  ADD CONSTRAINT `FKegb7uieypuui8mx8kyun02xse` FOREIGN KEY (`processing_processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `purpose`
--
ALTER TABLE `purpose`
  ADD CONSTRAINT `purpose_ibfk_1` FOREIGN KEY (`processing_id`) REFERENCES `processing` (`processing_id`);

--
-- Contraintes pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD CONSTRAINT `FKeahjwfv1ibxl8ktyffa4nfuwx` FOREIGN KEY (`secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`),
  ADD CONSTRAINT `FKijrjuuce5ik1ym56il6o3mxyu` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `FKs43ufd65t572wc18r098aiugk` FOREIGN KEY (`secondary_actor_category_secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
