-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Hôte : localhost
-- Généré le : dim. 16 mars 2025 à 23:46
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
-- Base de données : `priam-actor`
--

DELIMITER $$
--
-- Procédures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `CreateDataSubject` ()   BEGIN
    DECLARE referenceId VARCHAR(200);
    DECLARE tab_ref VARCHAR(200);
    DECLARE query VARCHAR(1000);

    -- Sélectionner les valeurs depuis la table `data_subject_category`
    SELECT data_subject_category_id INTO tab_ref FROM `data_subject_category` LIMIT 1;
    SELECT location_id INTO referenceId FROM `data_subject_category` LIMIT 1;

    -- Vérifier que les variables contiennent des valeurs valides avant de continuer
    IF tab_ref IS NOT NULL AND referenceId IS NOT NULL THEN
        -- Créer la table avec un SELECT dynamique (éviter d'utiliser EXECUTE IMMEDIATE)
        SET query = CONCAT('CREATE TABLE data_ssubject AS SELECT * FROM ', tab_ref, ' WHERE 1=0;');
        
        -- Préparer la requête
        PREPARE stmt FROM query;
        
        -- Exécuter la requête pour créer la table
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

        -- Ajouter la colonne `data_subject_id` comme clé primaire
        SET query = 'ALTER TABLE data_ssubject ADD COLUMN data_subject_id INT AUTO_INCREMENT PRIMARY KEY FIRST;';
        
        -- Préparer et exécuter la requête ALTER TABLE
        PREPARE stmt FROM query;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;

    ELSE
        -- Si l'une des variables est NULL, générer un message d'erreur
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Les valeurs sélectionnées sont invalides (tab_ref ou referenceId est NULL)';
    END IF;

END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `address`
--

CREATE TABLE `address` (
  `address_id` int(11) NOT NULL,
  `street_number` varchar(10) DEFAULT NULL,
  `street_name` varchar(255) DEFAULT NULL,
  `postal_code` varchar(10) DEFAULT NULL,
  `city` varchar(255) DEFAULT NULL,
  `complement` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `address`
--

INSERT INTO `address` (`address_id`, `street_number`, `street_name`, `postal_code`, `city`, `complement`) VALUES
(1, '10', 'Rue de la Paix ', ' 75002', ' Paris', 'Bâtiment A, Appartement 3 '),
(2, '15', 'street1 ', '65735', 'city1', ''),
(3, '34', 'rue 14', '75000', 'Paris', '');

-- --------------------------------------------------------

--
-- Structure de la table `country`
--

CREATE TABLE `country` (
  `country_id` int(11) NOT NULL,
  `country_name` varchar(100) DEFAULT NULL,
  `minor_age` int(11) DEFAULT NULL,
  `adequate` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `country`
--

INSERT INTO `country` (`country_id`, `country_name`, `minor_age`, `adequate`) VALUES
(12, 'Italy', 14, 0),
(22, 'Greece', 15, 0),
(23, 'France', 13, 1),
(29, 'Poland', 16, 0),
(45, 'Spain', 16, 0),
(59, 'Belgium', 16, 0),
(66, 'Sweden', 16, 1),
(71, 'Netherlands', 15, 1),
(87, 'Germany', 16, 1),
(104, 'Portugal', 16, 0);

-- --------------------------------------------------------

--
-- Structure de la table `data_subject`
--

CREATE TABLE `data_subject` (
  `data_subject_id` int(11) NOT NULL,
  `age` int(11) DEFAULT NULL,
  `id_ref` varchar(25) NOT NULL,
  `data_subject_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_subject`
--

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
(85, NULL, '591', 1),
(86, NULL, '592', 1),
(87, NULL, '593', 1),
(88, NULL, '594', 1),
(89, NULL, '595', 1),
(90, NULL, '596', 1),
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

-- --------------------------------------------------------

--
-- Structure de la table `data_subject_category`
--

CREATE TABLE `data_subject_category` (
  `data_subject_category_id` int(11) NOT NULL,
  `data_subject_category_name` varchar(25) DEFAULT NULL,
  `location_id` varchar(40) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_subject_category`
--

INSERT INTO `data_subject_category` (`data_subject_category_id`, `data_subject_category_name`, `location_id`) VALUES
(1, 'PERSISTENCEUSER', 'pu_ID'),
(2, 'none', '');

-- --------------------------------------------------------

--
-- Structure de la table `dpo`
--

CREATE TABLE `dpo` (
  `dpo_id` int(11) NOT NULL,
  `dpo_name` varchar(40) NOT NULL,
  `address_id` int(11) NOT NULL,
  `dpo_phone` varchar(40) DEFAULT NULL,
  `dpo_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `provider`
--

CREATE TABLE `provider` (
  `provider_id` int(11) NOT NULL,
  `provider_name` varchar(40) NOT NULL,
  `address_id` int(11) NOT NULL,
  `provider_phone` varchar(40) DEFAULT NULL,
  `provider_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `provider`
--

INSERT INTO `provider` (`provider_id`, `provider_name`, `address_id`, `provider_phone`, `provider_email`, `country_id`) VALUES
(1, 'Jean Dupont', 1, '+33123456789', 'jean.dupont@example.com ', 23);

-- --------------------------------------------------------

--
-- Structure de la table `representative`
--

CREATE TABLE `representative` (
  `representative_id` int(11) NOT NULL,
  `representative_name` varchar(40) NOT NULL,
  `address_id` int(11) NOT NULL,
  `representative_phone` varchar(40) DEFAULT NULL,
  `representative_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor`
--

CREATE TABLE `secondary_actor` (
  `secondary_actor_id` int(11) NOT NULL,
  `secondary_actor_type` varchar(40) DEFAULT NULL CHECK (`secondary_actor_type` in ('RECEPIENT','DATA_PROCESSOR','THIRD_PARTY')),
  `secondary_actor_name` varchar(40) NOT NULL,
  `address_id` int(11) NOT NULL,
  `secondary_actor_phone` varchar(40) DEFAULT NULL,
  `secondary_actor_email` varchar(40) DEFAULT NULL,
  `safeguard` varchar(255) DEFAULT NULL,
  `safeguard_type` varchar(20) DEFAULT NULL CHECK (`safeguard_type` in ('ADEQUACY_DECISION','CONTRACTUAL_CLAUSE','DEROGATION','BCR','NO')),
  `secondary_actor_category_id` int(11) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `secondary_actor_address` varchar(255) DEFAULT NULL,
  `secondary_actor_category_secondary_actor_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `secondary_actor`
--

INSERT INTO `secondary_actor` (`secondary_actor_id`, `secondary_actor_type`, `secondary_actor_name`, `address_id`, `secondary_actor_phone`, `secondary_actor_email`, `safeguard`, `safeguard_type`, `secondary_actor_category_id`, `country_id`, `secondary_actor_address`, `secondary_actor_category_secondary_actor_category_id`) VALUES
(2, 'RECEPIENT', 'compagny TTT', 2, '05 555 555 55', 'TTT@aaa.org', NULL, 'DEROGATION', 1, 12, '1', 1),
(3, 'RECEPIENT', 'CNIL', 3, '07 66 88 98 66', 'CNIL@cnil.fr', NULL, 'DEROGATION', 2, 23, '1', 2);

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor_category`
--

CREATE TABLE `secondary_actor_category` (
  `secondary_actor_category_id` int(11) NOT NULL,
  `secondary_actor_category_name` varchar(40) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `secondary_actor_category`
--

INSERT INTO `secondary_actor_category` (`secondary_actor_category_id`, `secondary_actor_category_name`) VALUES
(1, 'partners'),
(2, 'supervisory authority');

-- --------------------------------------------------------

--
-- Structure de la table `tutor`
--

CREATE TABLE `tutor` (
  `tutor_id` int(11) NOT NULL,
  `tutor_name` varchar(40) NOT NULL,
  `country_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `address`
--
ALTER TABLE `address`
  ADD PRIMARY KEY (`address_id`);

--
-- Index pour la table `country`
--
ALTER TABLE `country`
  ADD PRIMARY KEY (`country_id`);

--
-- Index pour la table `data_subject`
--
ALTER TABLE `data_subject`
  ADD PRIMARY KEY (`data_subject_id`),
  ADD KEY `data_subject_category_id` (`data_subject_category_id`);

--
-- Index pour la table `data_subject_category`
--
ALTER TABLE `data_subject_category`
  ADD PRIMARY KEY (`data_subject_category_id`);

--
-- Index pour la table `dpo`
--
ALTER TABLE `dpo`
  ADD PRIMARY KEY (`dpo_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `address_id` (`address_id`);

--
-- Index pour la table `provider`
--
ALTER TABLE `provider`
  ADD PRIMARY KEY (`provider_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `address_id` (`address_id`);

--
-- Index pour la table `representative`
--
ALTER TABLE `representative`
  ADD PRIMARY KEY (`representative_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `address_id` (`address_id`);

--
-- Index pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD PRIMARY KEY (`secondary_actor_id`),
  ADD KEY `secondary_actor_category_id` (`secondary_actor_category_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `address_id` (`address_id`),
  ADD KEY `FKs43ufd65t572wc18r098aiugk` (`secondary_actor_category_secondary_actor_category_id`);

--
-- Index pour la table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  ADD PRIMARY KEY (`secondary_actor_category_id`);

--
-- Index pour la table `tutor`
--
ALTER TABLE `tutor`
  ADD PRIMARY KEY (`tutor_id`),
  ADD KEY `country_id` (`country_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `data_subject`
--
ALTER TABLE `data_subject`
  MODIFY `data_subject_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=101;

--
-- AUTO_INCREMENT pour la table `data_subject_category`
--
ALTER TABLE `data_subject_category`
  MODIFY `data_subject_category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `dpo`
--
ALTER TABLE `dpo`
  MODIFY `dpo_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `provider`
--
ALTER TABLE `provider`
  MODIFY `provider_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `representative`
--
ALTER TABLE `representative`
  MODIFY `representative_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  MODIFY `secondary_actor_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  MODIFY `secondary_actor_category_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3;

--
-- AUTO_INCREMENT pour la table `tutor`
--
ALTER TABLE `tutor`
  MODIFY `tutor_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `data_subject`
--
ALTER TABLE `data_subject`
  ADD CONSTRAINT `data_subject_ibfk_1` FOREIGN KEY (`data_subject_category_id`) REFERENCES `data_subject_category` (`data_subject_category_id`);

--
-- Contraintes pour la table `dpo`
--
ALTER TABLE `dpo`
  ADD CONSTRAINT `dpo_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `dpo_ibfk_2` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `provider`
--
ALTER TABLE `provider`
  ADD CONSTRAINT `provider_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `provider_ibfk_2` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `representative`
--
ALTER TABLE `representative`
  ADD CONSTRAINT `representative_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `representative_ibfk_2` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD CONSTRAINT `FKs43ufd65t572wc18r098aiugk` FOREIGN KEY (`secondary_actor_category_secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_1` FOREIGN KEY (`secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_2` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_3` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `tutor`
--
ALTER TABLE `tutor`
  ADD CONSTRAINT `tutor_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
