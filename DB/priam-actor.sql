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
-- Base de données : `priam-actor`
--

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
(1, '12', 'za', '1234', 'ZDZ', NULL);

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
(23, 'France', 13, 0);

-- --------------------------------------------------------

--
-- Structure de la table `data_subject`
--

CREATE TABLE `data_subject` (
  `data_subject_id` int(11) NOT NULL,
  `age` int(11) NOT NULL,
  `id_ref` varchar(25) NOT NULL,
  `data_subject_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `data_subject`
--

INSERT INTO `data_subject` (`data_subject_id`, `age`, `id_ref`, `data_subject_category_id`) VALUES
(1, 20, '507', 1),
(2, 20, '508', 1);

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
(1, 'persistenceuser', 'pu_ID');

-- --------------------------------------------------------

--
-- Structure de la table `dpo`
--

CREATE TABLE `dpo` (
  `dpo_id` int(11) NOT NULL,
  `dpo_name` varchar(40) NOT NULL,
  `dpo_address` int(11) NOT NULL,
  `dpo_phone` varchar(40) DEFAULT NULL,
  `dpo_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `address_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `provider`
--

CREATE TABLE `provider` (
  `provider_id` int(11) NOT NULL,
  `provider_name` varchar(40) NOT NULL,
  `provider_address` int(11) NOT NULL,
  `provider_phone` varchar(40) DEFAULT NULL,
  `provider_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `address_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `representative`
--

CREATE TABLE `representative` (
  `representative_id` int(11) NOT NULL,
  `representative_name` varchar(40) NOT NULL,
  `representative_address` int(11) NOT NULL,
  `representative_phone` varchar(40) DEFAULT NULL,
  `representative_email` varchar(40) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `address_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor`
--

CREATE TABLE `secondary_actor` (
  `secondary_actor_id` int(11) NOT NULL,
  `secondary_actor_type` varchar(40) DEFAULT NULL CHECK (`secondary_actor_type` in ('Recepient','DataProcessor','ThirdParty')),
  `secondary_actor_name` varchar(40) NOT NULL,
  `secondary_actor_address` int(11) NOT NULL,
  `secondary_actor_phone` varchar(40) DEFAULT NULL,
  `secondary_actor_email` varchar(40) DEFAULT NULL,
  `safeguard` varchar(255) DEFAULT NULL,
  `safeguard_type` varchar(20) DEFAULT NULL CHECK (`safeguard_type` in ('AdequacyDecision','ContractualClauses','Derogation','BCR','No')),
  `secondary_actor_category_id` int(11) DEFAULT NULL,
  `country_id` int(11) DEFAULT NULL,
  `secondary_actor_category_secondary_actor_category_id` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Déchargement des données de la table `secondary_actor`
--

INSERT INTO `secondary_actor` (`secondary_actor_id`, `secondary_actor_type`, `secondary_actor_name`, `secondary_actor_address`, `secondary_actor_phone`, `secondary_actor_email`, `safeguard`, `safeguard_type`, `secondary_actor_category_id`, `country_id`, `secondary_actor_category_secondary_actor_category_id`) VALUES
(10, 'DataProcessor', 'Diet', 1, NULL, NULL, NULL, NULL, NULL, NULL, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `secondary_actor_category`
--

CREATE TABLE `secondary_actor_category` (
  `secondary_actor_category_id` int(11) NOT NULL,
  `secondary_actor_category_name` varchar(40) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
  ADD KEY `dpo_address` (`dpo_address`),
  ADD KEY `FK36e8s4itwrq50dlt2jugadf2v` (`address_id`);

--
-- Index pour la table `provider`
--
ALTER TABLE `provider`
  ADD PRIMARY KEY (`provider_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `provider_address` (`provider_address`),
  ADD KEY `FKlo7xqsaujf6jgci95h7tijkqa` (`address_id`);

--
-- Index pour la table `representative`
--
ALTER TABLE `representative`
  ADD PRIMARY KEY (`representative_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `representative_address` (`representative_address`),
  ADD KEY `FKhone8o5btv2qn83bymn984i80` (`address_id`);

--
-- Index pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD PRIMARY KEY (`secondary_actor_id`),
  ADD KEY `secondary_actor_category_id` (`secondary_actor_category_id`),
  ADD KEY `country_id` (`country_id`),
  ADD KEY `secondary_actor_address` (`secondary_actor_address`),
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
-- AUTO_INCREMENT pour la table `dpo`
--
ALTER TABLE `dpo`
  MODIFY `dpo_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `provider`
--
ALTER TABLE `provider`
  MODIFY `provider_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `representative`
--
ALTER TABLE `representative`
  MODIFY `representative_id` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  MODIFY `secondary_actor_id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=11;

--
-- AUTO_INCREMENT pour la table `secondary_actor_category`
--
ALTER TABLE `secondary_actor_category`
  MODIFY `secondary_actor_category_id` int(11) NOT NULL AUTO_INCREMENT;

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
  ADD CONSTRAINT `FK36e8s4itwrq50dlt2jugadf2v` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  ADD CONSTRAINT `dpo_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `dpo_ibfk_2` FOREIGN KEY (`dpo_address`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `provider`
--
ALTER TABLE `provider`
  ADD CONSTRAINT `FKlo7xqsaujf6jgci95h7tijkqa` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  ADD CONSTRAINT `provider_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `provider_ibfk_2` FOREIGN KEY (`provider_address`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `representative`
--
ALTER TABLE `representative`
  ADD CONSTRAINT `FKhone8o5btv2qn83bymn984i80` FOREIGN KEY (`address_id`) REFERENCES `address` (`address_id`),
  ADD CONSTRAINT `representative_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `representative_ibfk_2` FOREIGN KEY (`representative_address`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `secondary_actor`
--
ALTER TABLE `secondary_actor`
  ADD CONSTRAINT `FKs43ufd65t572wc18r098aiugk` FOREIGN KEY (`secondary_actor_category_secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_1` FOREIGN KEY (`secondary_actor_category_id`) REFERENCES `secondary_actor_category` (`secondary_actor_category_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_2` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`),
  ADD CONSTRAINT `secondary_actor_ibfk_3` FOREIGN KEY (`secondary_actor_address`) REFERENCES `address` (`address_id`);

--
-- Contraintes pour la table `tutor`
--
ALTER TABLE `tutor`
  ADD CONSTRAINT `tutor_ibfk_1` FOREIGN KEY (`country_id`) REFERENCES `country` (`country_id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
