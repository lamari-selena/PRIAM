-- phpMyAdmin SQL Dump
-- version 3.5.1
-- http://www.phpmyadmin.net
-- 
-- Client: localhost
-- Généré le: Sam 12 Mars 2022 à 22:45
-- Version du serveur: 5.5.24-log
-- Version de PHP: 5.4.3

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Base de données: `nutritioncoachingdb`
--

DELIMITER $$
--
-- Procédures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `datum_listing`(IN `DB` CHAR(200))
    MODIFIES SQL DATA
BEGIN
DECLARE className Varchar(200);
DECLARE AttributeName Varchar(200);
DECLARE i Integer;
DECLARE Curseur1 Cursor FOR SELECT COLUMN_NAME, TABLE_NAME FROM INFORMATION_SCHEMA.COLUMNS where TABLE_SCHEMA=DB AND TABLE_NAME NOT LIKE 'gdpr%';

OPEN Curseur1;


SELECT Count(*) into i FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DB AND TABLE_NAME NOT LIKE 'gdpr%';
select i;

boucle: LOOP
    FETCH Curseur1 INTO AttributeName, ClassName;
    insert into gdprdatum (attribute_name, class_name) values(AttributeName, className);
    set i = i-1;
    IF i<1 THEN
      LEAVE boucle;
    END IF;
   END LOOP;

  Close Curseur1;

   END$$

DELIMITER ;

-- --------------------------------------------------------

--
-- Structure de la table `hibernate_sequence`
--

CREATE TABLE IF NOT EXISTS `hibernate_sequence` (
  `next_val` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

--
-- Contenu de la table `hibernate_sequence`
--

INSERT INTO `hibernate_sequence` (`next_val`) VALUES
(2);

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_account`
--

CREATE TABLE IF NOT EXISTS `nutrition_account` (
  `username` varchar(30) NOT NULL,
  `password` varchar(30) NOT NULL,
  `stat_account` varchar(255) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`username`),
  KEY `FKqeqbft17ojmddumyd99fkyehg` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_mealContent`
--

CREATE TABLE IF NOT EXISTS `nutrition_mealContent` (
  `id` int(11) NOT NULL,
  `recommended_qty_eat` int(11) NOT NULL,
  `food_id` int(11) DEFAULT NULL,
  `healthy_meal_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhecprtixsx54yvh4t9m2qlaj9` (`food_id`),
  KEY `FK681k5t8c3mslrnag2wnt4x1n3` (`healthy_meal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_daily_meal`
--

CREATE TABLE IF NOT EXISTS `nutrition_daily_meal` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `meal_type` varchar(255) NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKj5u5ctcc3bdcg6jnm4fi9tv2d` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_diet`
--

CREATE TABLE IF NOT EXISTS `nutrition_diet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `diet_type` varchar(255) NOT NULL,
  `duration` int(11) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_eat`
--

CREATE TABLE IF NOT EXISTS `nutrition_eat` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` date NOT NULL,
  `quantity_eaten` int(11) NOT NULL,
  `daily_meal_id` int(11) DEFAULT NULL,
  `food_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKhja7hwdtj26w40ql9aqpyrrbd` (`daily_meal_id`),
  KEY `FKet61bbovq7x1xo4owx94p0x55` (`food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_exercise`
--

CREATE TABLE IF NOT EXISTS `nutrition_exercise` (
  `id` int(11) NOT NULL,
  `category` varchar(25) COLLATE utf8_bin NOT NULL,
  `code` varchar(5) COLLATE utf8_bin NOT NULL,
  `description` varchar(250) COLLATE utf8_bin NOT NULL,
  `metabolic_equivalent` double NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

--
-- Contenu de la table `nutrition_exercise`
--

INSERT INTO `nutrition_exercise` (`id`, `category`, `code`, `description`, `metabolic_equivalent`) VALUES
(1, 'lawn and garden', '08202', 'shoveling snow, by hand, vigorous effort', 7.5),
(2, 'bicycling', '01011', 'bicycling, to/from work, self selected pace', 6.8),
(3, 'music playing', '10120', 'guitar, classical, folk, sitting', 2),
(4, 'sports', '15542', 'rodeo sports, general, light effort', 4),
(5, 'walking', '17090', 'marching rapidly, military, no pack', 8),
(6, 'winter activities', '19160', 'skiing, downhill, alpine or snowboarding, moderate effort, general, \r\n\r\nactive time only', 5.3),
(7, 'sports', '15592', 'rollerblading, in-line skating, \r\n\r\n17.7 km/h (11.0 mph), moderate pace, exercise training', 9.8),
(8, 'conditioning \r\n\r\nexercise', '02022', 'calisthenics (e.g., push ups, sit ups, pull-ups, lunges), \r\n\r\nmoderate effort', 3.8),
(9, 'water activities', '18180', 'skindiving, fast', 15.8),
(10, 'occupation', '11500', 'operating heavy duty equipment, automated, not \r\n\r\ndriving', 2.5),
(11, 'fishing and hunting', '04050', 'fishing in stream, in waders \r\n\r\n(Taylor Code 670)', 6),
(12, 'winter activities', '19050', 'skating, speed, \r\n\r\ncompetitive', 13.3),
(13, 'sports', '15400', 'horseback riding,walking', 3.8),
(14, 'occupation', '11525', 'police, directing traffic, standing', 2.5),
(15, 'water \r\n\r\nactivities', '18265', 'swimming, breaststroke, recreational ', 5.3),
(16, 'home \r\n\r\nactivities', '05011', 'cleaning, sweeping, slow, light effort ', 2.3),
(17, 'sports', '15380', 'saddling, cleaning, grooming, harnessing and unharnessing \r\n\r\nhorse', 4.5),
(18, 'sports', '15430', 'martial arts, different types, moderate pace \r\n\r\n(e.g., judo, jujitsu, karate, kick boxing, tae kwan do, tai-bo, Muay Thai boxing)', 10.3),
(19, 'home activities', '05024', 'polishing floors, standing, walking slowly, \r\n\r\nusing electric polishing machine ', 4.5),
(20, 'occupation', '11020', 'bookbinding', 2.3),
(21, 'inactivity quiet/light', '07040', 'standing quietly, standing in a \r\n\r\nline', 1.3),
(22, 'occupation', '11410', 'horse racing, walking', 3.8),
(23, 'water \r\n\r\nactivities', '18365', 'water volleyball', 3),
(24, 'lawn and garden', '08010', 'carrying, loading or stacking wood, loading/unloading or carrying lumber', 5.5),
(25, 'home repair', '06074', 'carpentry, home remodeling tasks, light effort  ', 2.3),
(26, 'occupation', '11060', 'carrying moderate loads up stairs, moving boxes \r\n\r\n25-49 lbs', 8),
(27, 'lawn and garden', '08255', 'wheelbarrow, pushing garden cart \r\n\r\nor wheelbarrow ', 5.5),
(28, 'sports', '15060', 'basketball, officiating (Taylor \r\n\r\nCode 500)', 7),
(29, 'conditioning exercise', '02070', 'rowing, stationary \r\n\r\nergometer, general, vigorous effort', 6),
(30, 'occupation', '11514', 'painting,house, furniture, moderate effort', 3.3),
(31, 'home activities', '05032', 'dusting or polishing furniture, general', 2.3),
(32, 'running', '12080', 'running, \r\n\r\n7.5 mph (8 min/mile)', 11.5),
(33, 'occupation', '11090', 'coal mining, erecting \r\n\r\nsupports', 5),
(34, 'water activities', '18240', 'swimming laps, freestyle, front \r\n\r\ncrawl, slow, light or moderate effort', 5.8),
(35, 'conditioning exercise', '02008', 'army type obstacle course exercise, boot camp training program ', 5),
(36, 'music \r\n\r\nplaying', '10045', 'drumming (e.g., bongo, conga, benbe), moderate, sitting', 3),
(37, 'sports', '15650', 'squash (Taylor Code 530)', 12),
(38, 'bicycling', '01030', 'bicycling, 12-13.9 mph, leisure, moderate effort', 8),
(39, 'home activities', '05042', 'wash dishes, clearing dishes from table, walking, light effort', 2.5),
(40, 'sexual activity', '14020', 'general, moderate effort', 1.8),
(41, 'sports', '15160', 'croquet', 3.3),
(42, 'religious activities', '20015', 'standing quietly in \r\n\r\nchurch, attending a ceremony', 1.3),
(43, 'occupation', '11115', 'cook, chef', 2.5),
(44, 'occupation', '11418', 'laundry worker', 3.3),
(45, 'religious \r\n\r\nactivities', '20039', 'walk/stand combination for religious purposes, usher', 2),
(46, 'lawn and garden', '08065', 'gardening, using containers, older adults > 60 \r\n\r\nyears ', 2.3),
(47, 'sports', '15138', 'cheerleading, gymnastic moves, competitive \r\n\r\n', 6),
(48, 'walking', '17088', 'marching, moderate speed, military, no pack', 4.5),
(49, 'dancing', '03014', 'tap ', 4.8),
(50, 'lawn and garden', '08025', 'clearing light brush, thinning garden, moderate effort ', 3.5),
(51, 'home \r\n\r\nactivities', '05193', 'walk/run, playing with animals, moderate effort, only active \r\n\r\nperiods', 4),
(52, 'conditioning exercise', '02019', 'bicycling, stationary, \r\n\r\nRPM/Spin bike class ', 8.5),
(53, 'religious activities', '20037', 'walking, 3.0 \r\n\r\nmph, moderate speed, not carrying anything', 3.5),
(54, 'water activities', '18255', 'swimming, backstroke, recreational', 4.8),
(55, 'transportation', '16015', 'riding \r\n\r\nin a car or truck', 1.3),
(56, 'conditioning exercise', '02200', 'native New \r\n\r\nZealander physical activities (e.g., Haka Powhiri, Moteatea, Waita Tira, \r\n\r\nWhakawatea, etc.), general, moderate effort', 5.3),
(57, 'occupation', '11450', 'Machine tooling, operating punch press, moderate effort', 5),
(58, 'sports', '15652', 'squash, general ', 7.3),
(59, 'lawn and garden', '08090', 'laying sod', 5),
(60, 'running', '12050', 'running, 6 mph (10 min/mile)', 9.8),
(61, 'fishing and \r\n\r\nhunting', '04090', 'hunting, duck, wading', 2.5),
(62, 'winter activities', '19202', 'snowmobiling, passenger', 2),
(63, 'home repair', '06128', 'home repair, general, \r\n\r\nvigorous effort ', 6),
(64, 'occupation', '11791', 'walking on job, less than 2.0 \r\n\r\nmph, very slow speed, in office or lab area', 2),
(65, 'religious activities', '20100', 'typing, electric, manual, or computer', 1.3),
(66, 'music playing', '10060', 'horn, standing', 1.8),
(67, 'occupation', '11765', 'tailoring, weaving, \r\n\r\nmoderate effort (e.g., spinning and weaving operations, delivering boxes of yam to \r\n\r\nspinners, loading of warp bean, pinwinding, conewinding, warping, cloth cutting)', 4),
(68, 'volunteer activities', '21016', 'sitting, child care, only active \r\n\r\nperiods', 2),
(69, 'sports', '15465', 'lawn bowling, bocce ball, outdoor ', 3.3),
(70, 'home repair', '06167', 'plumbing, general ', 3),
(71, 'home repair', '06120', 'hanging storm windows', 5),
(72, 'home repair', '06050', 'carpentry, outside house, \r\n\r\ninstalling rain gutters (Taylor Code 640),carpentry, outside house, building a \r\n\r\nfence', 6),
(73, 'fishing and hunting', '04040', 'fishing from river bank, standing \r\n\r\n(Taylor Code 660)', 3.5),
(74, 'walking', '17028', 'carrying 25 to 49 lb load, \r\n\r\nupstairs', 8),
(75, 'conditioning exercise', '02117', 'upper body exercise, \r\n\r\nstationary bicycle - Airdyne (arms only) 40 rpm, moderate ', 4.3),
(76, 'water \r\n\r\nactivities', '18200', 'skindiving, scuba diving, general (Taylor Code 310)', 7),
(77, 'religious activities', '20005', 'sitting in church, talking or singing, \r\n\r\nattending a ceremony, sitting, active participation', 1.8),
(78, 'home repair', '06144', 'repairing appliances ', 3),
(79, 'conditioning exercise', '02013', 'bicycling, stationary, 101-160 watts, vigorous effort', 8.8),
(80, 'sports', '15450', 'kickball', 7),
(81, 'miscellaneous', '09100', 'retreat/family reunion \r\n\r\nactivities involving sitting, relaxing, talking, eating', 1.8),
(82, 'bicycling', '01013', 'bicycling, on dirt or farm road, moderate pace', 5.8),
(83, 'volunteer \r\n\r\nactivities', '21070', 'walk/stand combination, for volunteer purposes', 3),
(84, 'lawn and garden', '08030', 'clearing brush/land, undergrowth, or ground, hauling \r\n\r\nbranches, wheelbarrow chores, vigorous effort', 6.3),
(85, 'running', '12040', 'running, 5.2 mph (11.5 min/mile)', 9),
(86, 'home repair', '06170', 'put on and \r\n\r\nremoval of tarp - sailboat', 3),
(87, 'miscellaneous', '09000', 'board game playing, \r\n\r\nsitting', 1.5),
(88, 'home activities', '05192', 'walk/run, playing with animals, \r\n\r\ngeneral, light effort, only active periods', 3),
(89, 'lawn and garden', '08070', 'irrigation channels, opening and closing ports ', 4),
(90, 'winter activities', '19090', 'skiing, cross country, 4.0-4.9 mph, moderate speed and effort, general', 9),
(91, 'sports', '15130', 'broomball', 7),
(92, 'occupation', '11476', 'manual or \r\n\r\nunskilled labor, general, moderate effort', 4.5),
(93, 'lawn and garden', '08262', 'yard work, general, vigorous effort', 6),
(94, 'occupation', '11780', 'using heavy \r\n\r\npower tools such as pneumatic tools (e.g., jackhammers, drills)', 6.3),
(95, 'conditioning exercise', '02045', 'CurvesTM exercise routines in women ', 3.5),
(96, 'sports', '15730', 'wrestling (one match = 5 minutes)', 6),
(97, 'occupation', '11805', 'walking, pushing a wheelchair', 3.5),
(98, 'occupation', '11720', 'tailoring, cutting fabric', 2.3),
(99, 'conditioning exercise', '02180', 'yoga, \r\n\r\nSurya Namaskar', 3.3),
(100, 'winter activities', '19020', 'skating, ice, 9 mph or \r\n\r\nless', 5.5),
(101, 'walking', '17211', 'walking, 2.9 to 3.5 mph, uphill, 6% to 15% \r\n\r\ngrade', 8),
(102, 'bicycling', '01060', 'bicycling, > 20 mph, racing, not drafting', 15.8),
(103, 'home repair', '06140', 'laying tile or linoleum,repairing appliances', 3.8),
(104, 'water activities', '18355', 'water aerobics, water calisthenics', 5.5),
(105, 'running', '12090', 'running, 8 mph (7.5 min/mile)', 11.8),
(106, 'water \r\n\r\nactivities', '18366', 'water jogging', 9.8),
(107, 'occupation', '11070', 'chambermaid, hotel housekeeper, making bed, cleaning bathroom, pushing cart', 4),
(108, 'home activities', '05012', 'cleaning, sweeping, slow, moderate effort', 3.8),
(109, 'fishing and hunting', '04123', 'hunting, pigs, wild ', 3.3),
(110, 'self \r\n\r\ncare', '13035', 'talking and eating or eating only, standing', 2),
(111, 'water \r\n\r\nactivities', '18030', 'canoeing, portaging', 7),
(112, 'occupation', '11260', 'forestry, ax chopping, slow, 1.25 kg axe, 19 blows/min, moderate effort', 5),
(113, 'walking', '17320', 'walking, backwards, 3.5 mph, level ', 6),
(114, 'sports', '15265', 'golf, walking, carrying clubs', 4.3),
(115, 'sexual activity', '14010', 'active, vigorous effort', 2.8),
(116, 'running', '12030', 'running, 5 mph (12 \r\n\r\nmin/mile)', 8.3),
(117, 'water activities', '18090', 'diving, springboard or \r\n\r\nplatform', 3),
(118, 'religious activities', '20025', 'kneeling in church or at \r\n\r\nhome, praying', 1.3),
(119, 'home activities', '05092', 'laundry, hanging wash, \r\n\r\nwashing clothes by hand, moderate effort ', 4),
(120, 'transportation', '16020', 'flying airplane or helicopter', 1.8),
(121, 'fishing and hunting', '04020', 'fishing from river bank and walking', 4),
(122, 'dancing', '03030', 'ballroom, fast \r\n\r\n(Taylor Code 125)', 5.5),
(123, 'occupation', '11800', 'walking, 3.0 mph, moderately \r\n\r\nand carrying light objects less than 25 lbs', 4.5),
(124, 'bicycling', '01015', 'bicycling, general', 7.5),
(125, 'sports', '15630', 'softball, officiating', 4),
(126, 'home activities', '05051', 'serving food, setting table, implied walking or \r\n\r\nstanding', 2.5),
(127, 'occupation', '11790', 'using heavy tools (not power) such as \r\n\r\nshovel, pick, tunnel bar, spade', 8),
(128, 'home activities', '05023', 'mopping, \r\n\r\nstanding, light effort', 2.5),
(129, 'home repair', '06200', 'scraping and painting \r\n\r\nsailboat or powerboat', 4.5),
(130, 'music playing', '10100', 'violin, sitting', 2.5),
(131, 'winter activities', '19192', 'snow shoeing, vigorous effort', 10),
(132, 'home repair', '06150', 'painting, outside home (Taylor Code 650)', 5),
(133, 'home \r\n\r\nactivities', '05021', 'cleaning, mopping, standing, moderate effort', 3.5),
(134, 'conditioning exercise', '02101', 'stretching, mild', 2.3),
(135, 'water \r\n\r\nactivities', '18060', 'canoeing, rowing, kayaking, competition, >6 mph, vigorous \r\n\r\neffort', 12.5),
(136, 'occupation', '11490', 'moving, carrying or pushing heavy \r\n\r\nobjects, 75 lbs or more, only active time (e.g., desks, moving van work)', 7.5),
(137, 'conditioning exercise', '02062', 'health club exercise, conditioning \r\n\r\nclasses', 7.8),
(138, 'home activities', '05131', 'scrubbing floors, on hands and \r\n\r\nknees, scrubbing bathroom, bathtub, light effort', 2),
(139, 'miscellaneous', '09030', 'sitting, reading, book, newspaper, etc.', 1.3),
(140, 'dancing', '03021', 'aerobic, high impact', 7.3),
(141, 'miscellaneous', '09095', 'standing, arts and \r\n\r\ncrafts, sand painting, carving, weaving, vigorous effort', 3.5),
(142, 'walking', '17020', 'carrying 15 pound load (e.g. suitcase), level ground or downstairs', 5),
(143, 'sports', '15350', 'hockey, field', 7.8),
(144, 'sports', '15720', 'volleyball, non-competitive, 6 - 9 member team, general', 3),
(145, 'occupation', '11485', 'massage therapist, standing', 4),
(146, 'water activities', '18025', 'canoeing, harvesting wild rice, knocking rice off the stalks', 3.3),
(147, 'water \r\n\r\nactivities', '18190', 'skindiving, moderate', 11.8),
(148, 'winter activities', '19006', 'dog sledding, passenger ', 2.5),
(149, 'walking', '17082', 'hiking or \r\n\r\nwalking at a normal pace through fields and hillsides ', 5.3),
(150, 'home repair', '06020', 'automobile body work', 4),
(151, 'water activities', '18222', 'surfing, \r\n\r\nbody or board, competitive', 5),
(152, 'inactivity quiet/light', '07024', 'sitting, \r\n\r\nsmoking', 1.3),
(153, 'sports', '15390', 'horseback riding, trotting', 5.8),
(154, 'walking', '17040', 'climbing hills with 10 to 20 lb load', 7.3),
(155, 'home \r\n\r\nrepair', '06165', 'painting, (Taylor Code 630)', 4.5),
(156, 'walking', '17133', 'stair climbing, slow pace', 4),
(157, 'walking', '17031', 'loading /unloading a \r\n\r\ncar, implied walking', 3.5),
(158, 'bicycling', '01070', 'unicycling', 5),
(159, 'miscellaneous', '09110', 'camping involving standing, walking, sitting, light-to-\r\n\r\nmoderate effort', 2.5),
(160, 'walking', '17030', 'carrying > 74 lb load, upstairs', 12),
(161, 'walking', '17152', 'walking, 2.0 mph, level, slow pace, firm surface', 2.8),
(162, 'occupation', '11035', 'building road, directing traffic, standing', 2),
(163, 'home activities', '05041', 'wash dishes, standing or in general (not \r\n\r\nbroken into stand/walk components)', 1.8),
(164, 'sports', '15680', 'tennis, doubles \r\n\r\n(Taylor Code 430)', 6),
(165, 'running', '12060', 'running, 6.7 mph (9 min/mile)', 10.5),
(166, 'miscellaneous', '09065', 'sitting, in class, general, including note-\r\n\r\ntaking or class discussion', 1.8),
(167, 'lawn and garden', '08246', 'picking fruit \r\n\r\noff trees, picking fruits/vegetables, moderate effort', 3.5),
(168, 'occupation', '11430', 'machine tooling (e.g., machining, working sheet metal, machine fitter, \r\n\r\noperating lathe, welding) light-to-moderate effort', 3),
(169, 'conditioning \r\n\r\nexercise', '02170', 'yoga, Nadisodhana ', 2),
(170, 'water activities', '18350', 'swimming, treading water, moderate effort, general', 3.5),
(171, 'volunteer \r\n\r\nactivities', '21025', 'standing, moderate (lifting 50 lbs., assembling at fast \r\n\r\nrate)', 3.5),
(172, 'fishing and hunting', '04140', 'rifle exercises, shooting, \r\n\r\nlying down ', 2.3),
(173, 'walking', '17280', 'walking, to and from an outhouse', 2.5),
(174, 'sports', '15120', 'boxing, sparring', 7.8),
(175, 'walking', '17305', 'walking, for exercise, 5.0 mph, with ski poles, Nordic walking, level, fast pace \r\n\r\n', 9.5),
(176, 'transportation', '16035', 'pulling rickshaw ', 6.3),
(177, 'winter \r\n\r\nactivities', '19075', 'skiing, general', 7),
(178, 'winter activities', '19150', 'skiing, downhill, alpine or snowboarding, light effort, active time only', 4.3),
(179, 'occupation', '11192', 'farming, taking care of animals (e.g., grooming, \r\n\r\nbrushing, shearing sheep, assisting with birthing, medical care, branding), \r\n\r\ngeneral', 4.5),
(180, 'conditioning exercise', '02080', 'ski machine, general', 6.8),
(181, 'music playing', '10050', 'flute, sitting', 2),
(182, 'sports', '15552', 'rope jumping, slow pace, < 100 skips/min, 2 foot skip, rhythm bounce', 8.8),
(183, 'home activities', '05100', 'making bed, changing linens', 3.3),
(184, 'bicycling', '01019', 'bicycling, leisure, 9.4 mph', 5.8),
(185, 'walking', '17210', 'walking, \r\n\r\n2.9 to 3.5 mph, uphill, 1 to 5% grade', 5.3),
(186, 'conditioning exercise', '02110', 'teaching exercise class (e.g., aerobic, water)', 6.8),
(187, 'sports', '15733', 'track and field (e.g., high jump, long jump, triple jump, javelin, pole \r\n\r\nvault)', 6),
(188, 'sports', '15170', 'curling', 4),
(189, 'dancing', '03040', 'ballroom, slow (e.g., waltz, foxtrot, slow dancing, samba, tango, 19th century \r\n\r\ndance, mambo, cha cha)', 3),
(190, 'lawn and garden', '08055', 'driving tractor ', 2.8),
(191, 'occupation', '11195', 'farming, rice, planting, grain milling \r\n\r\nactivities', 3.8),
(192, 'lawn and garden', '08150', 'planting trees', 4.5),
(193, 'lawn and garden', '08250', 'implied walking/standing - picking up yard, light, \r\n\r\npicking flowers or vegetables', 3.3),
(194, 'occupation', '11244', 'fire fighter, \r\n\r\nrescue victim, automobile accident, using pike pole', 6.8),
(195, 'lawn and garden', '08120', 'mowing lawn, walk, power mower, moderate or vigorous effort', 5),
(196, 'occupation', '11378', 'hairstylist (e.g., plaiting hair, manicure, make-up \r\n\r\nartist)', 1.8),
(197, 'walking', '17012', 'backpacking, hiking or organized walking \r\n\r\nwith a daypack', 7.8),
(198, 'lawn and garden', '08020', 'chopping wood, splitting \r\n\r\nlogs, vigorous effort', 6.3),
(199, 'occupation', '11264', 'forestry, moderate \r\n\r\neffort (e.g., sawing wood with power saw, weeding, hoeing)', 4.5),
(200, 'occupation', '11590', 'sitting tasks, moderate effort (e.g., pushing heavy levers, \r\n\r\nriding mower/forklift, crane operation)', 2.5),
(201, 'dancing', '03017', 'aerobic, \r\n\r\nstep, with 10 - 12 inch step', 9.5),
(202, 'bicycling', '01065', 'bicycling, 12 mph, \r\n\r\nseated, hands on brake hoods or bar drops, 80 rpm', 8.5),
(203, 'water activities', '18368', 'water walking, moderate effort, moderate pace', 4.5),
(204, 'sports', '15020', 'badminton, competitive (Taylor Code 450)', 7),
(205, 'occupation', '11792', 'walking on job, 3.0 mph, in office, moderate speed, not carrying \r\n\r\nanything', 3.5),
(206, 'religious activities', '20055', 'eating/talking at church or \r\n\r\nstanding eating, American Indian Feast days', 2),
(207, 'sports', '15594', 'rollerblading, in-line skating, 24.0 km/h (15.0 mph), maximal effort', 14),
(208, 'conditioning exercise', '02014', 'bicycling, stationary, 161-200 watts, vigorous \r\n\r\neffort', 11),
(209, 'occupation', '11850', 'walking or walk downstairs or standing, \r\n\r\ncarrying objects about 100 lbs or more', 8.5),
(210, 'sports', '15285', 'golf, \r\n\r\nwalking, pulling clubs ', 5.3),
(211, 'occupation', '11015', 'bakery, light effort', 2),
(212, 'sports', '15480', 'orienteering', 9),
(213, 'occupation', '11380', 'horse \r\n\r\ngrooming, including feeding, cleaning stalls, bathing, brushing, clipping, longeing \r\n\r\nand exercising horses', 7.3),
(214, 'winter activities', '19011', 'ice fishing, \r\n\r\nsitting ', 2),
(215, 'lawn and garden', '08160', 'raking lawn or leaves, moderate \r\n\r\neffort', 3.8),
(216, 'occupation', '11560', 'shoveling, less than 10 lbs/minute, \r\n\r\nmoderate effort', 5),
(217, 'conditioning exercise', '02012', 'bicycling, \r\n\r\nstationary, 90-100 watts, moderate to vigorous effort', 6.8),
(218, 'religious \r\n\r\nactivities', '20050', 'eating at church', 1.5),
(219, 'sports', '15100', 'boxing, in \r\n\r\nring, general', 12.8),
(220, 'occupation', '11530', 'shoe repair, general', 2),
(221, 'conditioning exercise', '02074', 'rowing, stationary, 200 watts, very vigorous \r\n\r\neffort', 12),
(222, 'dancing', '03022', 'aerobic dance wearing 10-15 lb weights ', 10),
(223, 'occupation', '11191', 'farming, hauling water for animals, general \r\n\r\nhauling water,farming, general hauling water', 4.3),
(224, 'inactivity quiet/light', '07021', 'sitting quietly, general', 1.3),
(225, 'conditioning exercise', '02112', 'therapeutic exercise ball, Fitball exercise ', 2.8),
(226, 'religious activities', '20047', 'washing dishes, cleaning kitchen at church', 3.3),
(227, 'religious \r\n\r\nactivities', '20065', 'standing, moderate effort (e.g., lifting heavy objects, \r\n\r\nassembling at fast rate)', 3.5),
(228, 'home activities', '05055', 'putting away \r\n\r\ngroceries (e.g. carrying groceries, shopping without a grocery cart), carrying \r\n\r\npackages', 2.5),
(229, 'running', '12170', 'running, stairs, up', 15),
(230, 'home \r\n\r\nactivities', '05180', 'walking/running, playing with child(ren), vigorous effort, \r\n\r\nonly active periods', 5.8),
(231, 'miscellaneous', '09075', 'sitting, arts and \r\n\r\ncrafts,  carving wood, weaving, spinning wool, light effort', 1.8),
(232, 'occupation', '11420', 'locksmith', 3),
(233, 'sports', '15500', 'paddleball, \r\n\r\ncasual, general (Taylor Code 460)', 6),
(234, 'music playing', '10035', 'double \r\n\r\nbass, standing ', 2.5),
(235, 'fishing and hunting', '04120', 'hunting, rabbit, \r\n\r\nsquirrel, prairie chick, raccoon, small game (Taylor Code 690)', 5),
(236, 'water \r\n\r\nactivities', '18290', 'swimming, crawl, medium speed, ~50 yards/minute, vigorous \r\n\r\neffort', 8.3),
(237, 'water activities', '18370', 'whitewater rafting, kayaking, or \r\n\r\ncanoeing', 5),
(238, 'sports', '15010', 'archery, non-hunting', 4.3),
(239, 'music \r\n\r\nplaying', '10131', 'marching band, playing an instrument, walking, brisk pace, \r\n\r\ngeneral ', 5.5),
(240, 'home activities', '05140', 'sweeping garage, sidewalk or \r\n\r\noutside of house', 4),
(241, 'occupation', '11767', 'Truch, driving delivery truck, \r\n\r\ntaxi, shuttlebus, school bus', 2),
(242, 'running', '12027', 'jogging, on a mini-\r\n\r\ntramp', 4.5),
(243, 'walking', '17060', 'climbing hills with 42+ lb load', 9),
(244, 'water activities', '18340', 'swimming, treading water, fast, vigorous effort', 9.8),
(245, 'music playing', '10077', 'organ, sitting ', 2),
(246, 'lawn and garden', '08261', 'yard work, general, moderate effort', 4),
(247, 'water activities', '18140', 'sailing, Sunfish/Laser/Hobby Cat, Keel boats, ocean sailing, yachting, \r\n\r\nleisure', 3.3),
(248, 'winter activities', '19175', 'skiing, roller, elite racers', 12.5),
(249, 'volunteer activities', '21019', 'walk/run play with children, \r\n\r\nvigorous, only active periods', 5.8),
(250, 'sports', '15544', 'rodeo sports, \r\n\r\ngeneral, moderate effort', 5.5),
(251, 'conditioning exercise', '02071', 'rowing, \r\n\r\nstationary, general, moderate effort', 4.8),
(252, 'bicycling', '01020', 'bicycling, \r\n\r\n10-11.9 mph, leisure, slow, light effort', 6.8),
(253, 'conditioning exercise', '02072', 'rowing, stationary, 100 watts, moderate effort', 7),
(254, 'walking', '17026', 'carrying 1 to 15 lb load, upstairs', 5),
(255, 'conditioning exercise', '02120', 'water aerobics, water calisthenics, water exercise', 5.3),
(256, 'sports', '15370', 'horseback riding, general', 5.5),
(257, 'conditioning exercise', '02135', 'whirlpool, sitting', 1.3),
(258, 'lawn and garden', '08200', 'shovelling snow, by \r\n\r\nhand (Taylor Code 610)', 6),
(259, 'home activities', '05170', 'sitting, playing \r\n\r\nwith child(ren), light effort, only active periods', 2.2),
(260, 'occupation', '11477', 'manual or unskilled labor, general, vigorous effort', 6.5),
(261, 'volunteer activities', '21018', 'walk/run play with children, moderate, only \r\n\r\nactive periods', 3.5),
(262, 'home activities', '05183', 'standing, holding child', 2),
(263, 'self care', '13045', 'hairstyling, standing', 2.5),
(264, 'lawn and \r\n\r\ngarden', '08215', 'trimming shrubs or trees, power cutter, using leaf blower, edge, \r\n\r\nmoderate effort', 3.5),
(265, 'bicycling', '01018', 'bicycling, leisure, 5.5 mph', 3.5),
(266, 'bicycling', '01040', 'bicycling, 14-15.9 mph, racing or leisure, fast, \r\n\r\nvigorous effort', 10),
(267, 'home activities', '05184', 'child care, infant, \r\n\r\ngeneral ', 2.5),
(268, 'occupation', '11585', 'sitting meetings, light effort, \r\n\r\ngeneral, and/or with talking involved (e.g., eating at a business meeting)', 1.5),
(269, 'inactivity quiet/light', '07041', 'standing, fidgeting', 1.8),
(270, 'home \r\n\r\nrepair', '06070', 'carpentry, sawing hardwood', 6),
(271, 'home activities', '05065', 'non-food shopping, with or without a cart, standing or walking', 2.3),
(272, 'walking', '17150', 'walking, household', 2),
(273, 'fishing and hunting', '04130', 'pistol shooting or trap shooting, standing', 2.5),
(274, 'home \r\n\r\nactivities', '05121', 'moving, lifting light loads', 5),
(275, 'winter activities', '19254', 'snow shoveling, by hand, vigorous effort', 7.5),
(276, 'sports', '15702', 'trampoline, competitive', 4.5),
(277, 'sports', '15562', 'rugby, touch, non-\r\n\r\ncompetitive', 6.3),
(278, 'sports', '15290', 'golf, using power cart (Taylor Code \r\n\r\n070)', 3.5),
(279, 'religious activities', '20030', 'standing, talking in church', 1.8),
(280, 'conditioning exercise', '02054', 'resistance (weight) training, \r\n\r\nmultiple exercises, 8-15 repetitions at varied resistance ', 3.5),
(281, 'music \r\n\r\nplaying', '10130', 'marching band, baton twirling, walking, moderate pace, \r\n\r\ngeneral', 4),
(282, 'home repair', '06220', 'washing and waxing hull of sailboat or \r\n\r\nairplane', 4.5),
(283, 'sports', '15535', 'rock climbing, ascending rock, high \r\n\r\ndifficulty', 7.5),
(284, 'sports', '15640', 'softball,pitching', 6),
(285, 'self \r\n\r\ncare', '13036', 'taking medication, sitting or standing', 1.5),
(286, 'sports', '15672', 'tai chi, qi gong, sitting, light effort', 1.5),
(287, 'walking', '17190', 'walking, 2.8 to 3.2 mph, level, moderate pace, firm surface', 3.5),
(288, 'walking', '17021', 'carrying 15 lb child, slow walking', 2.3),
(289, 'lawn and \r\n\r\ngarden', '08140', 'planting seedlings, shrub, stooping, moderate effort', 4.3),
(290, 'conditioning exercise', '02146', 'video exercise workouts, TV conditioning \r\n\r\nprograms (e.g., cardio-resistance), vigorous effort', 6),
(291, 'home repair', '06080', 'caulking, chinking log cabin', 5),
(292, 'walking', '17162', 'walking to \r\n\r\nneighbor’s house or family’s house for social reasons', 2.5),
(293, 'religious \r\n\r\nactivities', '20040', 'praise with dance or run, spiritual dancing in church', 5),
(294, 'religious activities', '20038', 'walking, 3.5 mph, brisk speed, not carrying \r\n\r\nanything', 4.3),
(295, 'inactivity quiet/light', '07075', 'meditating', 1),
(296, 'occupation', '11630', 'standing, moderate/heavy tasks (e.g., lifting more than 50 \r\n\r\nlbs, masonry, painting, paper hanging)', 4.5),
(297, 'sports', '15250', 'frisbee, \r\n\r\nultimate', 8),
(298, 'fishing and hunting', '04060', 'fishing, ice, sitting', 2),
(299, 'sports', '15072', 'basketball, drills, practice ', 9.3),
(300, 'dancing', '03015', 'aerobic, general', 7.3),
(301, 'home activities', '05175', 'walking/running, playing with child(ren), moderate effort, only active periods', 3.5),
(302, 'walking', '17302', 'walking, for exercise, 3.5 to 4 mph, with ski \r\n\r\npoles, Nordic walking, level, moderate pace ', 4.8),
(303, 'occupation', '11110', 'coal mining, shoveling coal', 6.3),
(304, 'walking', '17200', 'walking, 3.5 mph, \r\n\r\nlevel, brisk, firm surface, walking for exercise', 4.3),
(305, 'running', '12100', 'running, 8.6 mph (7 min/mile)', 12.3),
(306, 'walking', '17130', 'stair climbing, \r\n\r\nusing or climbing up ladder (Taylor Code 030)', 8),
(307, 'fishing and hunting', '04115', 'hunting, birds ', 3.3),
(308, 'water activities', '18150', 'skiing, water \r\n\r\nor wakeboarding (Taylor Code 220)', 6),
(309, 'miscellaneous', '09060', 'sitting, \r\n\r\nstudying, general, including reading and/or writing, light effort', 1.3),
(310, 'occupation', '11526', 'police, driving a squad car, sitting', 2.5),
(311, 'winter \r\n\r\nactivities', '19140', 'skiing, cross-country, biathlon, skating technique ', 13.5),
(312, 'sports', '15600', 'skydiving, base jumping, bungee jumping ', 3.5),
(313, 'self care', '13009', 'sitting on toilet, eliminating while standing or \r\n\r\nsquating', 1.8),
(314, 'home activities', '05053', 'feeding household animals', 2.5),
(315, 'sports', '15200', 'fencing', 6),
(316, 'bicycling', '01003', 'bicycling, \r\n\r\nmountain, uphill, vigorous', 14),
(317, 'miscellaneous', '09071', 'standing, \r\n\r\nmiscellaneous', 2.5),
(318, 'inactivity quiet/light', '07050', 'reclining, writing', 1.3),
(319, 'home activities', '05197', 'animal care, household animals, general ', 2.3),
(320, 'sports', '15731', 'wallyball, general', 7),
(321, 'miscellaneous', '09020', 'drawing, writing, painting, standing', 1.8),
(322, 'water activities', '18390', 'windsurfing, competition, pumping for speed', 13.5),
(323, 'lawn and \r\n\r\ngarden', '08180', 'riding snow blower', 3),
(324, 'running', '12190', 'running, \r\n\r\ntraining, pushing a wheelchair or baby carrier', 8),
(325, 'fishing and hunting', '04086', 'hunting large game from a car, plane, or boat', 2),
(326, 'occupation', '11527', 'police, riding in a squad car, sitting', 1.3),
(327, 'music playing', '10020', 'cello, sitting', 2.3),
(328, 'sports', '15050', 'basketball, non-game, \r\n\r\ngeneral (Taylor Code 480)', 6),
(329, 'occupation', '11766', 'truck driving, loading \r\n\r\nand unloading truck, tying down load, standing, walking and carrying heavy loads', 6.5),
(330, 'bicycling', '01010', 'bicycling, <10 mph, leisure, to work or for \r\n\r\npleasure (Taylor Code 115)', 4),
(331, 'conditioning exercise', '02052', 'resistance \r\n\r\n(weight) training, squats , slow or explosive effort', 5),
(332, 'home repair', '06090', 'caulking, except log cabin', 4.5),
(333, 'sports', '15395', 'horseback \r\n\r\nriding, canter or gallop ', 7.3),
(334, 'home activities', '05027', 'multiple \r\n\r\nhousehold tasks all at once, vigorous effort', 4.3),
(335, 'winter activities', '19200', 'snowmobiling, driving, moderate', 3.5),
(336, 'inactivity quiet/light', '07020', 'sitting quietly and watching television', 1.3),
(337, 'lawn and garden', '08009', 'carrying, loading or stacking wood, loading/unloading or carrying lumber, \r\n\r\nlight-to-moderate effort ', 3.3),
(338, 'sports', '15420', 'jai alai', 12),
(339, 'winter activities', '19010', 'moving ice house, set up/drill holes', 6),
(340, 'home repair', '06127', 'home repair, general, moderate effort ', 4.5),
(341, 'occupation', '11740', 'tailoring, hand sewing', 1.8),
(342, 'fishing and hunting', '04070', 'hunting, bow and arrow, or crossbow', 2.5),
(343, 'occupation', '11750', 'tailoring, machine sewing', 2.5),
(344, 'sports', '15092', 'bowling, indoor, \r\n\r\nbowling alley ', 3.8),
(345, 'occupation', '11262', 'forestry, ax chopping, fast, \r\n\r\n1.25 kg axe, 35 blows/min, vigorous effort', 8),
(346, 'water activities', '18130', 'sailing, in competition', 4.5),
(347, 'walking', '17085', 'bird watching, slow \r\n\r\nwalk', 2.5),
(348, 'home activities', '05195', 'standing, bathing dog', 3.5),
(349, 'sports', '15725', 'volleyball, beach, in sand', 8),
(350, 'home activities', '05165', 'walking, moderate effort tasks, non-cleaning (readying to leave, \r\n\r\nshut/lock doors, close windows, etc.)', 3.5),
(351, 'miscellaneous', '09045', 'sitting, playing traditional video game, computer game ', 1),
(352, 'sports', '15408', 'horse cart, driving, standing or sitting', 1.8),
(353, 'volunteer \r\n\r\nactivities', '21050', 'walking, 3.5 mph, brisk speed, not carrying anything', 4.3),
(354, 'sports', '15425', 'martial arts, different types, slower pace, novice \r\n\r\nperformers, practice', 5.3),
(355, 'conditioning exercise', '02048', 'Elliptical \r\n\r\ntrainer, moderate effort ', 5),
(356, 'music playing', '10110', 'woodwind, sitting', 1.8),
(357, 'sports', '15360', 'hockey, ice, general', 8),
(358, 'miscellaneous', '09013', 'chess game, sitting', 1.5),
(359, 'occupation', '11210', 'farming, milking \r\n\r\nby hand, cleaning pails, moderate effort', 3.5),
(360, 'sports', '15540', 'rock \r\n\r\nclimbing, rappelling', 5),
(361, 'home activities', '05130', 'scrubbing floors, on \r\n\r\nhands and knees, scrubbing bathroom, bathtub, moderate effort', 3.5),
(362, 'occupation', '11050', 'carrying heavy loads (e.g., bricks, tools)', 8),
(363, 'water activities', '18310', 'swimming, leisurely, not lap swimming, general', 6),
(364, 'bicycling', '01066', 'bicycling, 12 mph, standing, hands on brake hoods, 60 \r\n\r\nrpm', 9),
(365, 'volunteer activities', '21030', 'standing, moderate/heavy work', 4.5),
(366, 'miscellaneous', '09085', 'standing, arts and crafts, sand painting, \r\n\r\ncarving, weaving, light effort', 2.5),
(367, 'volunteer activities', '21017', 'standing, child care, only active periods', 3),
(368, 'occupation', '11540', 'shoveling, digging ditches', 7.8),
(369, 'occupation', '11570', 'shoveling, 10 to \r\n\r\n15 lbs/minute, vigorous effort', 6.5),
(370, 'music playing', '10040', 'drums, \r\n\r\nsitting', 3.8),
(371, 'occupation', '11520', 'printing, paper industry worker, \r\n\r\nstanding', 2),
(372, 'walking', '17010', 'backpacking (Taylor Code 050)', 7),
(373, 'inactivity quiet/light', '07060', 'reclining, talking or talking on phone', 1.3),
(374, 'walking', '17231', 'walking, 5.0 mph, level, firm surface', 8.3),
(375, 'occupation', '11760', 'tailoring, pressing', 3.5),
(376, 'transportation', '16040', 'pushing plane in and out of hangar', 6),
(377, 'miscellaneous', '09105', 'touring/traveling/vacation involving riding in a vehicle', 2),
(378, 'running', '12134', 'running, 13 mph (4.6 min/mile)', 19.8),
(379, 'sports', '15270', 'golf, \r\n\r\nminiature, driving range', 3),
(380, 'water activities', '18250', 'swimming, \r\n\r\nbackstroke, general, training or competition', 9.5),
(381, 'running', '12020', 'jogging, general', 7),
(382, 'running', '12070', 'running, 7 mph (8.5 min/mile)', 11),
(383, 'religious activities', '20020', 'standing, singing in church, attending \r\n\r\na ceremony, standing, active participation', 2),
(384, 'conditioning exercise', '02024', 'calisthenics (e.g., situps, abdominal crunches), light effort', 2.8),
(385, 'home activities', '05181', 'walking and carrying small child, child weighing \r\n\r\n15 lbs or more', 3),
(386, 'conditioning exercise', '02205', 'native New Zealander \r\n\r\nphysical activities (e.g., Haka, Taiahab), general, vigorous effort', 6.8),
(387, 'occupation', '11516', 'plumbing activities ', 3),
(388, 'sports', '15310', 'hacky \r\n\r\nsack', 4),
(389, 'running', '12130', 'running, 11 mph (5.5 min/mile)', 16),
(390, 'lawn and garden', '08145', 'planting crops or garden, stooping, moderate effort ', 4.3),
(391, 'sports', '15140', 'coaching, football, soccer, basketball, baseball, \r\n\r\nswimming, etc.', 4),
(392, 'conditioning exercise', '02090', 'slimnastics, \r\n\r\njazzercise', 6),
(393, 'home activities', '05022', 'cleaning windows, washing \r\n\r\nwindows, general', 3.2),
(394, 'dancing', '03025', 'ethnic or cultural dancing \r\n\r\n(e.g., Greek, Middle Eastern, hula, salsa, merengue, bamba y plena, flamenco, \r\n\r\nbelly, and swing)', 4.5),
(395, 'occupation', '11390', 'horse racing, galloping', 7.3),
(396, 'inactivity quiet/light', '07011', 'lying quietly, doing nothing, lying \r\n\r\nin bed awake, listening to music (not talking or reading)', 1.3),
(397, 'sports', '15230', 'football, touch, flag, general (Taylor Code 510)', 8),
(398, 'lawn and \r\n\r\ngarden', '08045', 'digging, spading, filling garden, composting, light-to-moderate \r\n\r\neffort', 3.5),
(399, 'bicycling', '01008', 'bicycling, BMX', 8.5),
(400, 'home \r\n\r\nactivities', '05188', 'reclining with baby', 1.5),
(401, 'home activities', '05082', 'sewing with a machine ', 2.8),
(402, 'home activities', '05186', 'child care, \r\n\r\nstanding (e.g., dressing, bathing, grooming, feeding, occasional lifting of child), \r\n\r\nmoderate effort', 3),
(403, 'home repair', '06052', 'carpentry, outside house, \r\n\r\nbuilding a fence ', 3.8),
(404, 'walking', '17080', 'hiking, cross country (Taylor \r\n\r\nCode 040)', 6),
(405, 'transportation', '16050', 'truck, semi, tractor, > 1 ton, or \r\n\r\nbus, driving', 2.5),
(406, 'walking', '17260', 'walking, grass track', 4.8),
(407, 'miscellaneous', '09055', 'sitting, talking in person, on the phone, computer, or \r\n\r\ntext messaging, light effort', 1.5),
(408, 'religious activities', '20095', 'Standing, moderate-to-heavy effort, manual labor, lifting ? 50 lbs, heavy \r\n\r\nmaintenance', 4.5),
(409, 'sports', '15546', 'rodeo sports, general, vigorous \r\n\r\neffort', 7),
(410, 'sports', '15711', 'volleyball, competitive, in gymnasium', 6),
(411, 'dancing', '03019', 'bench step class, general ', 8.5),
(412, 'sports', '15625', 'softball, practice ', 4),
(413, 'self care', '13020', 'dressing, \r\n\r\nundressing, standing or sitting', 2.5),
(414, 'occupation', '11400', 'horse racing, \r\n\r\ntrotting', 5.8),
(415, 'lawn and garden', '08052', 'digging, spading, filling \r\n\r\ngarden, composting, vigorous effort ', 7.8),
(416, 'winter activities', '19252', 'snow shoveling, by hand, moderate effort', 5.3),
(417, 'walking', '17165', 'walking \r\n\r\nthe dog', 3),
(418, 'home repair', '06230', 'washing fence, painting fence, moderate \r\n\r\neffort', 4.5),
(419, 'lawn and garden', '08080', 'laying crushed rock', 6.3),
(420, 'lawn and garden', '08245', 'gardening, general, moderate effort', 3.8),
(421, 'self \r\n\r\ncare', '13030', 'eating, sitting', 1.5),
(422, 'volunteer activities', '21020', 'standing, light/moderate work (e.g., pack boxes, assemble/repair, set up \r\n\r\nchairs/furniture)', 3),
(423, 'self care', '13046', 'having hair or nails done by \r\n\r\nsomeone else, sitting', 1.3),
(424, 'conditioning exercise', '02105', 'pilates, \r\n\r\ngeneral ', 3),
(425, 'sports', '15030', 'badminton, social singles and doubles, \r\n\r\ngeneral', 5.5),
(426, 'transportation', '16010', 'automobile or light truck (not a \r\n\r\nsemi) driving', 2.5),
(427, 'winter activities', '19170', 'skiing, downhill, \r\n\r\nvigorous effort, racing', 8),
(428, 'lawn and garden', '08248', 'picking fruit off \r\n\r\ntrees, gleaning fruits, picking fruits/vegetables, climbing ladder to pick fruit, \r\n\r\nvigorous effort ', 4.5),
(429, 'sports', '15090', 'bowling (Taylor Code 390)', 3),
(430, 'home repair', '06122', 'hanging sheet rock inside house ', 5),
(431, 'water \r\n\r\nactivities', '18080', 'canoeing, rowing, in competition, or crew or sculling \r\n\r\n(Taylor Code 260)', 12),
(432, 'inactivity quiet/light', '07026', 'sitting at a \r\n\r\ndesk, resting head in hands', 1.3),
(433, 'home activities', '05150', 'moving \r\n\r\nhousehold items upstairs, carrying boxes or furniture', 9),
(434, 'walking', '17160', 'walking for pleasure (Taylor Code 010)', 3.5),
(435, 'lawn and garden', '08057', 'felling trees, large size', 8.3),
(436, 'religious activities', '20000', 'sitting in church, in service, attending a ceremony, sitting quietly', 1.3),
(437, 'sports', '15537', 'rock climbing, ascending or traversing rock, low-to-moderate \r\n\r\ndifficulty ', 5.8),
(438, 'sports', '15530', 'racquetball, general (Taylor Code \r\n\r\n470)', 7),
(439, 'sports', '15330', 'handball, team', 8),
(440, 'walking', '17140', 'using crutches', 5),
(441, 'occupation', '11793', 'walking on job, 3.5 mph, in \r\n\r\noffice, brisk speed, not carrying anything', 4.3),
(442, 'occupation', '11240', 'fire fighter, general', 8),
(443, 'occupation', '11010', 'bakery, general, moderate \r\n\r\neffort', 4),
(444, 'sports', '15070', 'basketball, shooting baskets', 4.5),
(445, 'running', '12180', 'running, on a track, team practice', 10),
(446, 'home repair', '06240', 'wiring, tapping-splicing', 3.3),
(447, 'occupation', '11580', 'sitting \r\n\r\ntasks, light effort (e.g., office work, chemistry lab work, computer work, light \r\n\r\nassembly repair, watch repair, reading, desk work)', 1.5),
(448, 'occupation', '11247', 'fishing, commercial, light effort', 3.5),
(449, 'sports', '15440', 'juggling', 4),
(450, 'lawn and garden', '08190', 'sacking grass, leaves', 4),
(451, 'winter activities', '19030', 'skating, ice, general (Taylor Code 360)', 7),
(452, 'conditioning exercise', '02085', 'slide board exercise, general ', 11),
(453, 'occupation', '11130', 'electrical work (e.g., hook up wire, tapping-splicing)', 3.3),
(454, 'home activities', '05110', 'maple syruping/sugar bushing (including \r\n\r\ncarrying buckets, carrying wood)', 5),
(455, 'home repair', '06130', 'laying or \r\n\r\nremoving carpet', 4.5),
(456, 'home activities', '05060', 'food shopping with or \r\n\r\nwithout a grocery cart, standing or walking', 2.3),
(457, 'fishing and hunting', '04065', 'fishing with a spear, standing ', 2.3),
(458, 'sports', '15190', 'drag \r\n\r\nracing, pushing or driving a car', 6),
(459, 'home activities', '05149', 'building a \r\n\r\nfire inside', 2.5),
(460, 'home activities', '05205', 'elder care, disabled adult, \r\n\r\nfeeding, combing hair, light effort, only active periods', 2.3),
(461, 'walking', '17220', 'walking, 4.0 mph, level, firm surface, very brisk pace', 5),
(462, 'dancing', '03016', 'aerobic, step, with 6 - 8 inch step', 7.5),
(463, 'sports', '15340', 'hang gliding', 3.5),
(464, 'miscellaneous', '09050', 'standing, talking in \r\n\r\nperson, on the phone, computer, or text messaging, light effort', 1.8),
(465, 'walking', '17035', 'climbing hills with 0 to 9 lb load', 6.5),
(466, 'home \r\n\r\nactivities', '05040', 'cleaning, general (straightening up, changing linen, \r\n\r\ncarrying out trash, light effort', 2.5),
(467, 'religious activities', '20001', 'sitting, playing an instrument at church', 2),
(468, 'home repair', '06040', 'carpentry, general, workshop (Taylor Code 620)', 3),
(469, 'conditioning exercise', '02020', 'calisthenics (e.g., push ups, sit ups, pull-ups, jumping jacks), vigorous \r\n\r\neffort', 8),
(470, 'lawn and garden', '08100', 'mowing lawn, riding mower (Taylor \r\n\r\nCode 550)', 2.5),
(471, 'dancing', '03010', 'ballet, modern, or jazz, general, \r\n\r\nrehearsal or class', 5),
(472, 'occupation', '11620', 'standing, moderate effort, \r\n\r\nintermittent lifting 50 lbs, hitch/twisting ropes', 3.5),
(473, 'home activities', '05132', 'scrubbing floors, on hands and knees, scrubbing bathroom, bathtub, \r\n\r\nvigorous effort', 6.5),
(474, 'walking', '17105', 'pushing a wheelchair, non-\r\n\r\noccupational ', 3.8),
(475, 'occupation', '11710', 'steel mill, vigorous effort \r\n\r\n(e.g., hand rolling, merchant mill rolling, removing slag, tending furnace)', 8.3),
(476, 'water activities', '18070', 'canoeing, rowing, for pleasure, general \r\n\r\n(Taylor Code 250)', 3.5),
(477, 'lawn and garden', '08260', 'yard work, general, \r\n\r\nlight effort', 3),
(478, 'home activities', '05043', 'vacuuming, general, moderate \r\n\r\neffort', 3.3),
(479, 'sports', '15470', 'moto-cross, off-road motor sports, all-\r\n\r\nterrain vehicle, general', 4),
(480, 'occupation', '11615', 'standing, moderate \r\n\r\neffort, lifting items continuously, 10 – 20 lbs, with limited walking or resting', 4.5),
(481, 'music playing', '10074', 'playing musical instruments, general ', 2),
(482, 'conditioning exercise', '02064', 'home exercise, general ', 3.8),
(483, 'sports', '15593', 'rollerblading, in-line skating, 21.0 to 21.7 km/h (13.0 to 13.6 \r\n\r\nmph), fast pace, exercise training', 12.3),
(484, 'occupation', '11593', 'sitting, \r\n\r\nteaching stretching or yoga, or light effort exercise class', 2.8),
(485, 'sports', '15660', 'table tennis, ping pong (Taylor Code 410)', 4),
(486, 'home activities', '05049', 'cooking or food preparation, moderate effort', 3.5),
(487, 'water \r\n\r\nactivities', '18330', 'swimming, synchronized', 8),
(488, 'occupation', '11796', 'walking, gathering things at work, ready to leave', 3),
(489, 'running', '12010', 'jog/walk combination (jogging component of less than 10 minutes) (Taylor Code \r\n\r\n180)', 6),
(490, 'sports', '15135', 'children’s games, adults playing (e.g., \r\n\r\nhopscotch, 4-square, dodgeball, playground apparatus, t-ball, tetherball, marbles, \r\n\r\narcade games), moderate effort', 5.8),
(491, 'lawn and garden', '08241', 'weeding, \r\n\r\ncultivating garden, using a hoe, moderate-to-vigorous effort', 5),
(492, 'sports', '15510', 'polo, on horseback', 8),
(493, 'sports', '15732', 'track and field (e.g., \r\n\r\nshot, discus, hammer throw)', 4),
(494, 'dancing', '03060', 'Caribbean dance \r\n\r\n(Abakua, Beguine, Bellair, Bongo, Brukin''s, Caribbean Quadrills, Dinki Mini, Gere, \r\n\r\nGumbay, Ibo, Jonkonnu, Kumina, Oreisha, Jambu)', 3.5),
(495, 'sports', '15533', 'rock or mountain climbing (Taylor Code 470) (Formerly code = 17120) ', 8),
(496, 'occupation', '11220', 'farming, milking by machine, light effort', 1.3),
(497, 'conditioning exercise', '02073', 'rowing, stationary, 150 watts, vigorous effort', 8.5),
(498, 'miscellaneous', '09101', 'retreat/family reunion activities involving \r\n\r\nplaying games with children', 3),
(499, 'water activities', '18270', 'swimming, \r\n\r\nbutterfly, general', 13.8),
(500, 'sports', '15591', 'rollerblading, in-line \r\n\r\nskating, 14.4 km/h (9.0 mph), recreational pace', 7.5),
(501, 'sports', '15075', 'basketball, wheelchair', 7.8),
(502, 'sports', '15150', 'cricket, batting, bowling, \r\n\r\nfielding', 4.8),
(503, 'fishing and hunting', '04110', 'hunting, pheasants or grouse \r\n\r\n(Taylor Code 680)', 6),
(504, 'running', '12120', 'running, 10 mph (6 min/mile)', 14.5),
(505, 'water activities', '18050', 'canoeing, rowing, 4.0-5.9 mph, moderate \r\n\r\neffort', 5.8),
(506, 'running', '12025', 'jogging, in place', 8),
(507, 'home \r\n\r\nrepair', '06126', 'home repair, general, light effort ', 2.5),
(508, 'volunteer \r\n\r\nactivities', '21040', 'walking, less than 2.0 mph, very slow', 2),
(509, 'sports', '15080', 'billiards', 2.5),
(510, 'miscellaneous', '09040', 'sitting, writing, desk \r\n\r\nwork, typing', 1.3),
(511, 'sports', '15142', 'coaching, actively playing sport with \r\n\r\nplayers ', 8),
(512, 'conditioning exercise', '02160', 'yoga, Power ', 4),
(513, 'occupation', '11550', 'shoveling, more than 16 lbs/minute, deep digging, vigorous \r\n\r\neffort', 8.8),
(514, 'home repair', '06124', 'hammering nails ', 3),
(515, 'fishing \r\n\r\nand hunting', '04125', 'hunting, hiking with hunting gear ', 9.5),
(516, 'fishing \r\n\r\nand hunting', '04100', 'hunting, general', 5),
(517, 'home activities', '05048', 'tanning hides, general', 4),
(518, 'bicycling', '01004', 'bicycling, mountain, \r\n\r\ncompetitive, racing', 16),
(519, 'occupation', '11266', 'forestry, vigorous effort \r\n\r\n(e.g., barking, felling, or trimming trees, carrying or stacking logs, planting \r\n\r\nseeds, sawing lumber by hand)', 8),
(520, 'sports', '15335', 'high ropes course, \r\n\r\nmultiple elements', 4),
(521, 'occupation', '11146', 'farming, moderate effort \r\n\r\n(e.g., feeding animals, chasing cattle by walking and/or horseback, spreading \r\n\r\nmanure, harvesting crops)', 4.8),
(522, 'water activities', '18120', 'sailing, boat \r\n\r\nand board sailing, windsurfing, ice sailing, general (Taylor Code 235)', 3),
(523, 'home activities', '05020', 'cleaning, heavy or major (e.g. wash car, wash windows, \r\n\r\nclean garage), moderate effort', 3.5),
(524, 'music playing', '10125', 'guitar, rock \r\n\r\nand roll band, standing', 3),
(525, 'home activities', '05080', 'knitting, sewing, \r\n\r\nlight effort, wrapping presents, sitting', 1.3),
(526, 'running', '12140', 'running, \r\n\r\ncross country', 9),
(527, 'walking', '17027', 'carrying 16 to 24 lb load, upstairs', 6),
(528, 'water activities', '18020', 'canoeing, on camping trip (Taylor Code \r\n\r\n270)', 4),
(529, 'water activities', '18385', 'windsurfing or kitesurfing, crossing \r\n\r\ntrial', 11),
(530, 'home activities', '05191', 'stand, playing with animals, light \r\n\r\neffort, only active periods', 2.8),
(531, 'occupation', '11246', 'fire fighter, \r\n\r\nhauling hoses on ground, carrying/hoisting equipment, breaking down walls etc., \r\n\r\nwearing full gear', 9),
(532, 'occupation', '11820', 'walking or walk downstairs or \r\n\r\nstanding, carrying objects about 25 to 49 lbs', 5),
(533, 'home repair', '06160', 'painting inside house,wallpapering, scraping paint', 3.3),
(534, 'fishing and \r\n\r\nhunting', '04080', 'hunting, deer, elk, large game (Taylor Code 170)', 6),
(535, 'running', '12150', 'running, (Taylor code 200)', 8),
(536, 'lawn and garden', '08050', 'digging, spading, filling garden, compositing, (Taylor Code 590)', 5),
(537, 'conditioning exercise', '02005', 'activity promoting video/arcade game \r\n\r\n(e.g., Exergaming, Dance Dance Revolution), vigorous effort', 7.2),
(538, 'water \r\n\r\nactivities', '18220', 'surfing, body or board, general', 3),
(539, 'walking', '17170', 'walking, 2.5 mph, level, firm surface', 3),
(540, 'volunteer activities', '21010', 'sitting, moderate work', 2.5),
(541, 'walking', '17025', 'carrying load \r\n\r\nupstairs, general', 8.3),
(542, 'walking', '17180', 'walking, 2.5 mph, downhill', 3.3),
(543, 'occupation', '11528', 'police, making an arrest, standing', 4),
(544, 'occupation', '11375', 'garbage collector, walking, dumping bins into truck', 4),
(545, 'walking', '17325', 'walking, backwards, 3.5 mph, uphill, 5% grade', 8),
(546, 'home activities', '05185', 'child care, sitting/kneeling (e.g., dressing, bathing, \r\n\r\ngrooming, feeding, occasional lifting of child), light effort, general', 2),
(547, 'conditioning exercise', '02060', 'health club exercise, general (Taylor Code \r\n\r\n160)', 5.5),
(548, 'sports', '15560', 'rugby, union, team, competitive', 8.3),
(549, 'home activities', '05125', 'organizing room', 4.8),
(550, 'home activities', '05057', 'cooking Indian bread on an outside stove', 3),
(551, 'lawn and garden', '08165', 'raking lawn (Taylor Code 600)', 4),
(552, 'occupation', '11125', 'custodial work, light effort (e.g., cleaning sink and toilet, dusting, vacuuming, \r\n\r\nlight cleaning)', 2.3),
(553, 'conditioning exercise', '02001', 'activity promoting \r\n\r\nvideo game (e.g., Wii Fit), light effort (e.g., balance, yoga)', 2.3),
(554, 'sports', '15040', 'basketball, game (Taylor Code 490)', 8),
(555, 'walking', '17050', 'climbing hills with 21 to 42 lb load', 8.3),
(556, 'music playing', '10010', 'accordion, sitting', 1.8),
(557, 'occupation', '11475', 'manual or \r\n\r\nunskilled labor, general, light effort', 2.8),
(558, 'bicycling', '01050', 'bicycling, 16-19 mph, racing/not drafting or > 19 mph drafting, very fast, racing \r\n\r\ngeneral', 12),
(559, 'religious activities', '20060', 'cleaning church', 3.3),
(560, 'walking', '17161', 'walking from house to car or bus, from car or bus to go \r\n\r\nplaces, from car or bus to and from the worksite', 2.5);
INSERT INTO `nutrition_exercise` (`id`, `category`, `code`, `description`, `metabolic_equivalent`) VALUES
(561, 'miscellaneous', '09015', 'copying documents, standing ', 1.5),
(562, 'lawn and garden', '08125', 'mowing lawn, power mower, light or moderate effort (Taylor Code 590)', 4.5),
(563, 'self care', '13050', 'showering, toweling off, standing', 2),
(564, 'sports', '15320', 'handball, general (Taylor Code 520)', 12),
(565, 'winter activities', '19005', 'dog sledding, mushing ', 7.5),
(566, 'home activities', '05200', 'elder \r\n\r\ncare, disabled adult, bathing, dressing, moving into and out of bed, only active \r\n\r\nperiods', 4),
(567, 'water activities', '18260', 'swimming, breaststroke, general, \r\n\r\ntraining or competition', 10.3),
(568, 'sports', '15232', 'football, touch, flag, \r\n\r\nlight effort', 4),
(569, 'occupation', '11795', 'walking on job, 2.5 mph, slow speed \r\n\r\nand carrying light objects less than 25 lbs', 3.5),
(570, 'dancing', '03018', 'aerobic, step, with 4-inch step ', 5.5),
(571, 'fishing and hunting', '04083', 'hunting large marine animals ', 4),
(572, 'religious activities', '20035', 'walking \r\n\r\nin church', 2),
(573, 'sports', '15362', 'hockey, ice, competitive', 10),
(574, 'home \r\n\r\nactivities', '05147', 'implied walking, putting away household items, moderate \r\n\r\neffort', 3),
(575, 'sexual activity', '14030', 'passive, light effort, kissing, \r\n\r\nhugging', 1.3),
(576, 'water activities', '18360', 'water polo', 10),
(577, 'sports', '15375', 'horse chores, feeding, watering, cleaning stalls, implied walking and \r\n\r\nlifting loads ', 4.3),
(578, 'walking', '17033', 'climbing hills, no load', 6.3),
(579, 'sports', '15460', 'lacrosse', 8),
(580, 'volunteer activities', '21015', 'standing, light work (filing, talking, assembling)', 2.3),
(581, 'walking', '17110', 'race walking', 6.5),
(582, 'lawn and garden', '08195', 'shoveling snow, by \r\n\r\nhand, moderate effort ', 5.3),
(583, 'fishing and hunting', '04085', 'hunting large \r\n\r\ngame, from a hunting stand, limited walking ', 2.5),
(584, 'religious activities', '20036', 'walking, less than 2.0 mph, very slow', 2),
(585, 'walking', '17270', 'walking, to work or class (Taylor Code 015)', 4),
(586, 'transportation', '16060', 'walking for transportation, 2.8-3.2 mph, level, moderate pace, firm surface ', 3.5),
(587, 'winter activities', '19135', 'skiing, cross-country, skating ', 13.3),
(588, 'volunteer activities', '21035', 'typing, electric, manual, or computer', 1.3),
(589, 'home activities', '05160', 'standing, light effort tasks (pump gas, \r\n\r\nchange light bulb, etc.)', 2),
(590, 'occupation', '11810', 'walking, 3.5 mph, \r\n\r\nbriskly and carrying objects less than 25 lbs', 4.8),
(591, 'conditioning exercise', '02065', 'stair-treadmill ergometer, general', 9),
(592, 'occupation', '11600', 'standing tasks, light effort (e.g., bartending, store \r\n\r\nclerk, assembling, filing, duplicating, librarian, putting up a Christmas tree, \r\n\r\nstanding and talking at work, changing clothes when teaching physical education, \r\n\r\nstanding)', 3),
(593, 'home activities', '05194', 'walk/run, playing with animals, \r\n\r\nvigorous effort, only active periods', 5),
(594, 'walking', '17250', 'walking, for \r\n\r\npleasure, work break', 3.5),
(595, 'home activities', '05056', 'carrying groceries \r\n\r\nupstairs', 7.5),
(596, 'sports', '15410', 'horseshoe pitching, quoits', 3),
(597, 'occupation', '11006', 'airline flight attendant ', 3),
(598, 'sports', '15180', 'darts, wall or lawn', 2.5),
(599, 'volunteer activities', '21055', 'walking, 2.5 \r\n\r\nmph slowly and carrying objects less than 25 lbs', 3.5),
(600, 'inactivity \r\n\r\nquiet/ligh', '07070', 'reclining, reading', 1.3),
(601, 'occupation', '11730', 'tailoring, general', 2.5),
(602, 'self care', '13040', 'grooming, washing hands, \r\n\r\nshaving, brushing teeth, putting on make-up, sitting or standing', 2),
(603, 'lawn \r\n\r\nand garden', '08040', 'digging sandbox, shoveling sand', 5),
(604, 'sports', '15192', 'auto racing, open wheel', 8.5),
(605, 'occupation', '11763', 'tailoring, \r\n\r\nweaving, light effort (e.g., finishing operations, washing, dyeing, inspecting \r\n\r\ncloth, counting yards, paperwork)', 2),
(606, 'water activities', '18225', 'paddle \r\n\r\nboarding, standing', 6),
(607, 'winter activities', '19110', 'skiing, cross country, \r\n\r\n>8.0 mph, elite skier, racing', 15),
(608, 'fishing and hunting', '04081', 'hunting \r\n\r\nlarge game, dragging carcass ', 11.3),
(609, 'home activities', '05045', 'butchering \r\n\r\nanimal, large, vigorous effort', 6),
(610, 'home repair', '06030', 'automobile \r\n\r\nrepair, light or moderate effort', 3.3),
(611, 'sports', '15695', 'tennis, hitting \r\n\r\nballs, non-game play, moderate effort ', 5),
(612, 'miscellaneous', '09070', 'standing, reading', 1.8),
(613, 'home activities', '05120', 'moving furniture, \r\n\r\nhousehold items, carrying boxes', 5.8),
(614, 'water activities', '18320', 'swimming, sidestroke, general', 7),
(615, 'water activities', '18369', 'water \r\n\r\nwalking, vigorous effort, brisk pace', 6.8),
(616, 'occupation', '11135', 'engineer \r\n\r\n(e.g., mechanical or electrical)', 1.8),
(617, 'home repair', '06060', 'carpentry, \r\n\r\nfinishing or refinishing cabinets or furniture', 3.3),
(618, 'occupation', '11030', 'building road, driving heavy machinery', 6),
(619, 'fishing and hunting', '04062', 'fishing, dip net, setting net and retrieving fish, general', 3.5),
(620, 'winter \r\n\r\nactivities', '19260', 'snow blower, walking and pushing', 2.5),
(621, 'occupation', '11100', 'coal mining, general', 5.5),
(622, 'fishing and hunting', '04064', 'fishing, fishing wheel, setting net and retrieving fish, general', 3),
(623, 'inactivity quiet/light', '07022', 'sitting quietly, fidgeting, general, fidgeting \r\n\r\nhands', 1.5),
(624, 'fishing and hunting', '04095', 'hunting, flying fox, squirrel \r\n\r\n', 3),
(625, 'walking', '17134', 'stair climbing, fast pace', 8.8),
(626, 'music \r\n\r\nplaying', '10030', 'conducting orchestra, standing', 2.3),
(627, 'home activities', '05044', 'butchering animals, small ', 3),
(628, 'self care', '13000', 'getting \r\n\r\nready for bed, general, standing', 2.3),
(629, 'lawn and garden', '08058', 'felling \r\n\r\ntrees, small-medium size', 5.3),
(630, 'fishing and hunting', '04061', 'fishing, jog \r\n\r\nor line, standing, general ', 1.8),
(631, 'occupation', '11126', 'custodial work, \r\n\r\nmoderate effort (e.g., electric buffer, feathering arena floors, mopping, taking \r\n\r\nout trash, vacuuming)', 3.8),
(632, 'dancing', '03050', 'Anishinaabe Jingle \r\n\r\nDancing', 5.5),
(633, 'winter activities', '19060', 'ski jumping, climb up carrying \r\n\r\nskis', 7),
(634, 'home repair', '06100', 'cleaning gutters', 5),
(635, 'home \r\n\r\nactivities', '05189', 'breastfeeding, sitting or reclining ', 2),
(636, 'home \r\n\r\nactivities', '05190', 'sit, playing with animals, light effort, only active \r\n\r\nperiods', 2.5),
(637, 'sports', '15402', 'horseback riding, jumping ', 9),
(638, 'walking', '17235', 'walking, 5.0 mph, uphill, 3% grade', 9.8),
(639, 'home \r\n\r\nactivities', '05046', 'cutting and smoking fish, drying fish or meat ', 2.3),
(640, 'water activities', '18380', 'windsurfing, not pumping for speed', 5),
(641, 'walking', '17262', 'walking, normal pace, plowed field or sand ', 4.5),
(642, 'occupation', '11472', 'manager, property', 1.8),
(643, 'sports', '15590', 'skating, \r\n\r\nroller (Taylor Code 360)', 7),
(644, 'lawn and garden', '08210', 'trimming shrubs or \r\n\r\ntrees, manual cutter', 4),
(645, 'lawn and garden', '08230', 'watering lawn or \r\n\r\ngarden, standing or walking', 1.5),
(646, 'volunteer activities', '21060', 'walking, \r\n\r\n3.0 mph moderately and carrying objects less than 25 lbs, pushing something', 4.5),
(647, 'fishing and hunting', '04001', 'fishing, general', 3.5),
(648, 'conditioning exercise', '02003', 'activity promoting video game (e.g., Wii Fit), \r\n\r\nmoderate effort (e.g., aerobic, resistance)', 3.8),
(649, 'volunteer activities', '21000', 'sitting, meeting, general, and/or with talking involved', 1.5),
(650, 'bicycling', '01009', 'bicycling, mountain, general', 8.5),
(651, 'walking', '17029', 'carrying 50 to 74 lb load, upstairs', 10),
(652, 'home activities', '05171', 'standing, playing with child(ren) light effort, only active periods', 2.8),
(653, 'home repair', '06110', 'excavating garage', 5),
(654, 'religious \r\n\r\nactivities', '20061', 'general yard work at church', 4),
(655, 'conditioning \r\n\r\nexercise', '02035', 'circuit training, moderate effort', 4.3),
(656, 'walking', '17230', 'walking, 4.5 mph, level, firm surface, very, very brisk', 7),
(657, 'sports', '15550', 'rope jumping, fast pace, 120-160 skips/min', 12.3),
(658, 'lawn \r\n\r\nand garden', '08240', 'weeding, cultivating garden (Taylor Code 580)', 4.5),
(659, 'conditioning exercise', '02150', 'yoga, Hatha ', 2.5),
(660, 'occupation', '11370', 'furriery', 4.5),
(661, 'occupation', '11797', 'walking, 2.5 mph, slow speed, \r\n\r\ncarrying heavy objects more than 25 lbs', 3.8),
(662, 'water activities', '18210', 'snorkeling (Taylor Code 310)', 5),
(663, 'occupation', '11080', 'coal mining, \r\n\r\ndrilling coal, rock', 5.3),
(664, 'conditioning exercise', '02040', 'circuit \r\n\r\ntraining, including kettlebells, some aerobic movement with minimal rest, general, \r\n\r\nvigorous intensity', 8),
(665, 'inactivity quiet/light', '07023', 'sitting, \r\n\r\nfidgeting feet', 1.8),
(666, 'occupation', '11830', 'walking or walk downstairs or \r\n\r\nstanding, carrying objects about 50 to 74 lbs', 6.5),
(667, 'winter activities', '19180', 'sledding, tobogganing, bobsledding, luge (Taylor Code 370)', 7),
(668, 'conditioning exercise', '02030', 'calisthenics, light or moderate effort, general \r\n\r\n(e.g., back exercises), going up & down from floor (Taylor Code 150)', 3.5),
(669, 'occupation', '11250', 'forestry, ax chopping, very fast, 1.25 kg axe, 51 \r\n\r\nblows/min, extremely vigorous effort', 17.5),
(670, 'occupation', '11038', 'carpentry, general, light effort ', 2.5),
(671, 'water activities', '18230', 'swimming laps, freestyle, fast, vigorous effort', 9.8),
(672, 'music playing', '10070', 'piano, sitting', 2.3),
(673, 'sports', '15255', 'golf, general', 4.8),
(674, 'sports', '15645', 'sports spectator, very excited, emotional, physically \r\n\r\nmoving  ', 3.3),
(675, 'occupation', '11245', 'fire fighter, raising and climbing \r\n\r\nladder with full gear, simulated fire supression', 8),
(676, 'occupation', '11190', 'farming, feeding cattle, horses', 4.3),
(677, 'occupation', '11415', 'lawn keeper, \r\n\r\nyard work, general', 4),
(678, 'sports', '15685', 'tennis, doubles', 4.5),
(679, 'home activities', '05050', 'cooking or food preparation - standing or sitting or \r\n\r\nin general (not broken into stand/walk components), manual appliances, light \r\n\r\neffort', 2),
(680, 'walking', '17070', 'descending stairs', 3.5),
(681, 'sports', '15610', 'soccer, casual, general (Taylor Code 540)', 7),
(682, 'occupation', '11708', 'steel mill, moderate effort (e.g., fettling, forging, tipping molds)', 5.3),
(683, 'conditioning exercise', '02017', 'bicycling, stationary, 51-89 watts, \r\n\r\nlight-to-moderate effort ', 4.8),
(684, 'home repair', '06010', 'airplane repair', 3),
(685, 'lawn and garden', '08135', 'planting, potting, transplanting seedlings or \r\n\r\nplants, light effort ', 2),
(686, 'volunteer activities', '21005', 'sitting, light \r\n\r\noffice work, in general', 1.5),
(687, 'inactivity quiet/light', '07030', 'sleeping', 0.95),
(688, 'miscellaneous', '09090', 'standing, arts and crafts, sand painting, \r\n\r\ncarving, weaving, moderate effort', 3.3),
(689, 'home activities', '05146', 'standing, packing/unpacking boxes, occasional lifting of lightweight household \r\n\r\nitems, loading or unloading items in car, moderate effort', 3.5),
(690, 'water \r\n\r\nactivities', '18160', 'jet skiing, driving, in water', 7),
(691, 'miscellaneous', '09025', 'laughing, sitting ', 1),
(692, 'fishing and hunting', '04010', 'fishing \r\n\r\nrelated, digging worms, with shovel', 4.3),
(693, 'conditioning exercise', '02061', 'health club exercise classes, general, gym/weight training combined in one visit', 5),
(694, 'occupation', '11147', 'farming, light effort (e.g., cleaning animal \r\n\r\nsheds, preparing animal feed) ', 2),
(695, 'lawn and garden', '08130', 'operating \r\n\r\nsnow blower, walking', 2.5),
(696, 'dancing', '03031', 'general dancing (e.g., \r\n\r\ndisco, folk, Irish step dancing, line dancing, polka, contra, country)', 7.8),
(697, 'home activities', '05052', 'cooking or food preparation, walking', 2.5),
(698, 'volunteer activities', '21045', 'walking, 3.0 mph, moderate speed, not carrying \r\n\r\nanything', 3.5),
(699, 'sports', '15670', 'tai chi, qi gong, general', 3),
(700, 'conditioning exercise', '02015', 'bicycling, stationary, 201-270 watts, very \r\n\r\nvigorous effort', 14),
(701, 'conditioning exercise', '02115', 'upper body exercise, \r\n\r\narm ergometer ', 2.8),
(702, 'conditioning exercise', '02010', 'bicycling, \r\n\r\nstationary, general', 7),
(703, 'walking', '17151', 'walking, less than 2.0 mph, \r\n\r\nlevel, strolling, very slow', 2),
(704, 'running', '12029', 'Running, 4 mph (13 \r\n\r\nmin/mile) ', 6),
(705, 'fishing and hunting', '04007', 'fishing, catching fish with \r\n\r\nhands ', 4),
(706, 'fishing and hunting', '04063', 'fishing, set net, setting net \r\n\r\nand retrieving fish, general', 3.8),
(707, 'home activities', '05148', 'watering \r\n\r\nplants', 2.5),
(708, 'lawn and garden', '08019', 'chopping wood, splitting logs, \r\n\r\nmoderate effort', 4.5),
(709, 'music playing', '10090', 'trumpet, standing', 1.8),
(710, 'occupation', '11249', 'fishing, commercial, vigorous effort', 7),
(711, 'lawn \r\n\r\nand garden', '08220', 'walking, applying fertilizer or seeding a lawn, push \r\n\r\napplicator', 3),
(712, 'occupation', '11482', 'masonry, concrete, light effort ', 2.5),
(713, 'home activities', '05035', 'kitchen activity, general, (e.g., cooking, \r\n\r\nwashing dishes, cleaning up), moderate effort', 3.3),
(714, 'dancing', '03020', 'aerobic, low impact', 5),
(715, 'lawn and garden', '08095', 'mowing lawn, general', 5.5),
(716, 'inactivity quiet/light', '07010', 'lying quietly and watching \r\n\r\ntelevision', 1),
(717, 'sports', '15490', 'paddleball, competitive', 10),
(718, 'occupation', '11042', 'carpentry, general, heavy or vigorous effort ', 7),
(719, 'conditioning exercise', '02050', 'resistance training (weight lifting, free \r\n\r\nweight, nautilus or universal), power lifting or body building, vigorous effort \r\n\r\n(Taylor Code 210)', 6),
(720, 'sports', '15300', 'gymnastics, general', 3.8),
(721, 'conditioning exercise', '02143', 'video exercise workouts, TV conditioning \r\n\r\nprograms (e.g., cardio-resistance), moderate effort', 4),
(722, 'water activities', '18352', 'tubing, floating on a river, general', 2.3),
(723, 'religious activities', '20046', 'preparing food at church', 2),
(724, 'occupation', '11170', 'farming, \r\n\r\ndriving tasks (e.g., driving tractor or harvester) ', 2.8),
(725, 'inactivity \r\n\r\nquiet/ligh', '07025', 'sitting, listening to music (not talking or reading) or \r\n\r\nwatching a movie in a theater', 1.5),
(726, 'occupation', '11770', 'typing, \r\n\r\nelectric, manual or computer', 1.3),
(727, 'lawn and garden', '08192', 'shoveling \r\n\r\ndirt or mud ', 5.5),
(728, 'winter activities', '19130', 'skiing, cross country, \r\n\r\nhard snow, uphill, maximum, snow mountaineering', 15.5),
(729, 'sports', '15675', 'tennis, general', 7.3),
(730, 'lawn and garden', '08239', 'weeding, cultivating \r\n\r\ngarden, light-to-moderate effort ', 3.5),
(731, 'sports', '15210', 'football, \r\n\r\ncompetitive', 8),
(732, 'occupation', '11529', 'postal carrier, walking to deliver \r\n\r\nmail', 2.3),
(733, 'occupation', '11870', 'working in scene shop, theater actor, \r\n\r\nbackstage employee', 3),
(734, 'water activities', '18010', 'boating, power, \r\n\r\ndriving', 2.5),
(735, 'music playing', '10080', 'trombone, standing', 3.5),
(736, 'occupation', '11495', 'skindiving or SCUBA diving as a frogman, Navy Seal', 12),
(737, 'occupation', '11480', 'masonry, concrete, moderate effort', 4.3),
(738, 'running', '12135', 'running, 14 mph (4.3 min/mile) ', 23),
(739, 'miscellaneous', '09010', 'card playing, sitting', 1.5),
(740, 'sports', '15110', 'boxing, punching \r\n\r\nbag', 5.5),
(741, 'home activities', '05025', 'multiple household tasks all at once, \r\n\r\nlight effort', 2.8),
(742, 'water activities', '18100', 'kayaking, moderate effort', 5),
(743, 'sports', '15240', 'frisbee playing, general', 3),
(744, 'home activities', '05026', 'multiple household tasks all at once, moderate effort', 3.5),
(745, 'water \r\n\r\nactivities', '18110', 'paddle boat', 4),
(746, 'fishing and hunting', '04005', 'fishing, crab fishing ', 4.5),
(747, 'water activities', '18040', 'canoeing, \r\n\r\nrowing, 2.0-3.9 mph, light effort', 2.8),
(748, 'occupation', '11840', 'walking or \r\n\r\nwalk downstairs or standing, carrying objects about 75 to 99 lbs', 7.5),
(749, 'sports', '15000', 'Alaska Native Games, Eskimo Olympics, general ', 5.5),
(750, 'winter activities', '19100', 'skiing, cross country, 5.0-7.9 mph, brisk speed, \r\n\r\nvigorous effort', 12.5),
(751, 'walking', '17100', 'pushing or pulling stroller with \r\n\r\nchild or walking with children, 2.5 to 3.1 mph', 4),
(752, 'transportation', '16030', 'motor scooter, motorcycle', 3.5),
(753, 'sports', '15582', 'skateboarding, \r\n\r\ncompetitive, vigorous effort ', 6),
(754, 'fishing and hunting', '04124', 'trapping \r\n\r\ngame, general ', 2),
(755, 'conditioning exercise', '02140', 'video exercise \r\n\r\nworkouts, TV conditioning programs (e.g., yoga, stretching), light effort', 2.3),
(756, 'winter activities', '19040', 'skating, ice, rapidly, more than 9 mph, not \r\n\r\ncompetitive', 9),
(757, 'water activities', '18367', 'water walking, light effort, \r\n\r\nslow pace', 2.5),
(758, 'home activities', '05095', 'laundry, putting away clothes, \r\n\r\ngathering clothes to pack, putting away laundry, implied walking', 2.3),
(759, 'lawn \r\n\r\nand garden', '08060', 'gardening with heavy power tools, tilling a garden, chain \r\n\r\nsaw', 5.8),
(760, 'occupation', '11381', 'horse, feeding, watering, cleaning stalls, \r\n\r\nimplied walking and lifting loads', 4.3),
(761, 'winter activities', '19018', 'skating, ice dancing', 14),
(762, 'music playing', '10135', 'marching band, drum \r\n\r\nmajor, walking', 3.5),
(763, 'sports', '15580', 'skateboarding, general, moderate \r\n\r\neffort', 5),
(764, 'sports', '15570', 'shuffleboard', 3),
(765, 'water activities', '18280', 'swimming, crawl, fast speed, ~75 yards/minute, vigorous effort', 10),
(766, 'home repair', '06210', 'spreading dirt with a shovel', 5),
(767, 'lawn and \r\n\r\ngarden', '08110', 'mowing lawn, walk, hand mower (Taylor Code 570)', 6),
(768, 'water activities', '18012', 'boating, power, passenger, light ', 1.3),
(769, 'occupation', '11413', 'kitchen maid ', 3),
(770, 'home repair', '06225', 'washing \r\n\r\nand waxing car ', 2),
(771, 'religious activities', '20045', 'serving food at \r\n\r\nchurch', 2.5),
(772, 'miscellaneous', '09115', 'sitting at a sporting event, \r\n\r\nspectator', 1.5),
(773, 'dancing', '03038', 'ballroom dancing, competitive, general \r\n\r\n', 11.3),
(774, 'religious activities', '20010', 'sitting, reading religious \r\n\r\nmaterials at home', 1.3),
(775, 'home activities', '05010', 'cleaning, sweeping \r\n\r\ncarpet or floors, general', 3.3),
(776, 'self care', '13010', 'bathing, sitting', 1.5),
(777, 'conditioning exercise', '02068', 'rope skipping, general', 12.3),
(778, 'occupation', '11040', 'carpentry, general, moderate effort', 4.3),
(779, 'home \r\n\r\nactivities', '05030', 'cleaning, house or cabin, general, moderate effort', 3.3),
(780, 'sports', '15734', 'track and field (e.g., steeplechase, hurdles)', 10),
(781, 'sports', '15690', 'tennis, singles (Taylor Code 420)', 8),
(782, 'dancing', '03012', 'ballet, modern, or jazz, performance, vigorous effort ', 6.8),
(783, 'sports', '15605', 'soccer, competitive', 10),
(784, 'volunteer activities', '21065', 'walking, 3.5 mph, briskly and carrying objects less than 25 lbs', 4.8),
(785, 'fishing and hunting', '04145', 'rifle exercises, shooting, kneeling or \r\n\r\nstanding ', 2.5),
(786, 'sports', '15055', 'basketball, general ', 6.5),
(787, 'home \r\n\r\nactivities', '05090', 'laundry, fold or hang clothes, put clothes in washer or \r\n\r\ndryer, packing suitcase, washing clothes by hand, implied standing, light effort', 2),
(788, 'home activities', '05182', 'walking and carrying small child, child \r\n\r\nweighing less than 15 lbs', 2.3),
(789, 'sports', '15700', 'trampoline, \r\n\r\nrecreational', 3.5),
(790, 'walking', '17310', 'walking, for exercise, with ski \r\n\r\npoles, Nordic walking, uphill', 6.8),
(791, 'occupation', '11180', 'farming, feeding \r\n\r\nsmall animals', 3.5),
(792, 'occupation', '11145', 'farming, vigorous effort (e.g., \r\n\r\nbaling hay, cleaning barn) ', 7.8),
(793, 'winter activities', '19080', 'skiing, \r\n\r\ncross country, 2.5 mph, slow or light effort, ski walking', 6.8),
(794, 'home \r\n\r\nactivities', '05070', 'ironing', 1.8),
(795, 'occupation', '11510', 'orange grove \r\n\r\nwork, picking fruit', 4.5),
(796, 'running', '12110', 'running, 9 mph (6.5 \r\n\r\nmin/mile)', 12.8),
(797, 'occupation', '11610', 'standing, light/moderate effort \r\n\r\n(e.g., assemble/repair heavy parts, welding,stocking parts,auto repair,standing, \r\n\r\npacking boxes, nursing patient care)', 3),
(798, 'lawn and garden', '08170', 'raking \r\n\r\nroof with snow rake', 4),
(799, 'sports', '15551', 'rope jumping, moderate pace, \r\n\r\n100-120 skips/min, general,  2 foot skip, plain bounce', 11.8),
(800, 'miscellaneous', '09005', 'casino gambling, standing', 2.5),
(801, 'winter \r\n\r\nactivities', '19190', 'snow shoeing, moderate effort', 5.3),
(802, 'water \r\n\r\nactivities', '18300', 'swimming, lake, ocean, river (Taylor Codes 280, 295)', 6),
(803, 'running', '12200', 'running, marathon', 13.3),
(804, 'home repair', '06205', 'sharpening tools ', 2),
(805, 'occupation', '11248', 'fishing, commercial, moderate \r\n\r\neffort', 5),
(806, 'home repair', '06072', 'carpentry, home remodeling tasks, \r\n\r\nmoderate effort ', 4),
(807, 'sports', '15710', 'volleyball (Taylor Code 400)', 4),
(808, 'occupation', '11003', 'active workstation, treadmill desk, walking ', 2.3),
(809, 'lawn and garden', '08251', 'walking, gathering gardening tools', 3),
(810, 'home repair', '06190', 'sanding floors with a power sander', 4.5),
(811, 'conditioning exercise', '02011', 'bicycling, stationary, 30-50 watts, very light \r\n\r\nto light effort', 3.5),
(812, 'miscellaneous', '09106', 'touring/traveling/vacation \r\n\r\ninvolving walking', 3.5),
(813, 'sports', '15620', 'softball or baseball, fast or \r\n\r\nslow pitch, general (Taylor Code 440)', 5),
(814, 'fishing and hunting', '04030', 'fishing from boat or canoe, sitting', 2),
(815, 'sports', '15235', 'football or \r\n\r\nbaseball, playing catch', 2.5),
(816, 'sports', '15520', 'racquetball, competitive', 10),
(817, 'miscellaneous', '09080', 'sitting, arts and crafts,  carving wood, \r\n\r\nweaving, spinning wool, moderate effort', 3),
(818, 'occupation', '11120', 'construction, outside, remodeling, new structures (e.g., roof repair, \r\n\r\nmiscellaneous)', 4),
(819, 'home repair', '06180', 'roofing', 6),
(820, 'transportation', '16016', 'riding in a bus or train', 1.3),
(821, 'running', '12132', 'running, 12 mph (5 min/mile) ', 19);

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_stickToDiet`
--

CREATE TABLE IF NOT EXISTS `nutrition_stickToDiet` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `end_date` date NOT NULL,
  `start_date` date NOT NULL,
  `diets_id` int(11) DEFAULT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK6l5sm5jx3i3w29gsyrmf4hado` (`diets_id`),
  KEY `FKdu54qn44hggnog7itd6d88hkv` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_food`
--

CREATE TABLE IF NOT EXISTS `nutrition_food` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `calories` int(11) NOT NULL,
  `carbs` double NOT NULL,
  `created_time` datetime NOT NULL,
  `default_serving_type` varchar(10) COLLATE utf8_bin NOT NULL,
  `fat` double NOT NULL,
  `fiber` double NOT NULL,
  `last_updated_time` datetime NOT NULL,
  `name` varchar(50) COLLATE utf8_bin NOT NULL,
  `protein` double NOT NULL,
  `saturated_fat` double NOT NULL,
  `serving_type_qty` double NOT NULL,
  `sodium` double NOT NULL,
  `sugar` double NOT NULL,
  `owner_id` binary(16) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_bin AUTO_INCREMENT=429 ;

--
-- Contenu de la table `nutrition_food`
--

INSERT INTO `nutrition_food` (`id`, `calories`, `carbs`, `created_time`, `default_serving_type`, `fat`, `fiber`, `last_updated_time`, `name`, `protein`, `saturated_fat`, `serving_type_qty`, `sodium`, `sugar`, `owner_id`) VALUES
(1, 90, 4, '2008-04-11 00:00:00', 'CUP', 7, 0, '2008-04-11 00:00:00', 'Classico Sun-Dried Tomato Alfredo \r\n\r\nSauce', 2, 4, 0.25, 430, 2, NULL),
(2, 3820, 345, '2008-02-17 00:00:00', 'OUNCE', 188, 21, '2008-02-17 00:00:00', 'Amandas Chicken Casserole (entire dish)', 183.5, 94.25, 76, 5175, 18.5, NULL),
(3, 15, 4, '2012-06-22 00:00:00', 'TABLESPOON', 0, 0, '2012-06-22 00:00:00', 'Kroger Bourbon Peppercorn marinade', 0, 0, 1, 400, 3, NULL),
(4, 600, 66, '2008-06-06 00:00:00', 'CUSTOM', 21, 8, '2008-06-06 00:00:00', 'Cheese Ravioli', 35, 12, 6, 1744, 14, NULL),
(5, 275, 0, '2008-01-23 00:00:00', 'OUNCE', 6.25, 0, '2008-01-23 00:00:00', 'Chicken Breast, boneless and skinless', 57.5, 1.25, 10, 0, 0, NULL),
(6, 140, 23, '2008-03-05 00:00:00', 'CUP', 1.5, 4, '2008-03-05 00:00:00', 'Campbells Chunky Fajita Chicken Soup', 9, 0.5, 1, 850, 7, NULL),
(7, 130, 19, '2008-06-02 00:00:00', 'CUP', 4.5, 1, '2008-06-02 00:00:00', 'Combos, nacho pretzel', 3, 3, 0.33000001311302185, 320, 5, NULL),
(8, 146, 26, '2008-02-04 00:00:00', 'CUSTOM', 2, 1, '2008-02-04 00:00:00', 'Dinner roll', 5, 0, 1, 272, 0, NULL),
(9, 540, 59, '2008-06-13 00:00:00', 'CUSTOM', 28, 6, '2008-06-13 00:00:00', 'Krystal Chili Cheese Fries', 13, 13, 1, 800, 1, NULL),
(10, 300, 28, '2008-09-24 00:00:00', 'CUSTOM', 14, 1, '2008-09-24 00:00:00', 'Red Baron Thin \r\n\r\nCheese Pizza Single', 14, 8, 1, 600, 3, NULL),
(11, 35, 9, '2008-09-27 00:00:00', 'CUP', 0, 2, '2008-09-27 00:00:00', 'Carrots (1 cup = 12 baby carrots)', 1, 0, 1, 88, 6, NULL),
(12, 80, 11, '2008-12-27 00:00:00', 'CUP', 2.5, 0, '2008-12-27 00:00:00', 'Cream of Mushroom Soup (condensed, low-fat)', 2, 1, 0.5, 800, 2, NULL),
(13, 45, 0.5, '2008-03-05 00:00:00', 'CUSTOM', 2.4000000953674316, 0.20000000298023224, '2008-03-05 00:00:00', 'Chicken Wing (non-breaded)', 5.5, 0.6000000238418579, 1, 350, 0.20000000298023224, NULL),
(14, 220, 42, '2008-04-27 00:00:00', 'CUSTOM', 2, 12, '2008-04-27 00:00:00', 'French Toast slices', 4, 0, 2, 235, 4, NULL),
(15, 90, 18, '2008-04-17 00:00:00', 'CUSTOM', 3, 3, '2008-04-17 00:00:00', 'Weight Watchers Caramel Cake', 1, 0.5, 1, 100, 10, NULL),
(16, 120, 23, '2008-09-28 00:00:00', 'CUP', 2.5, 0, '2008-09-28 00:00:00', 'Rice Dream drink', 1, 0, 1, 80, 10, NULL),
(17, 170, 0, '2013-12-18 00:00:00', 'OUNCE', 8, 0, '2013-12-18 00:00:00', 'Ground Beef, 93/7', 23, 3.5, 4, 75, 0, NULL),
(18, 240, 49, '2008-03-22 00:00:00', 'OUNCE', 1.5, 2, '2008-03-22 00:00:00', 'Rice a Roni Fried Rice', 7, 0, 2.5, 1390, 4, NULL),
(19, 17, 3.5999999046325684, '2008-09-24 00:00:00', 'CUP', 0.20000000298023224, 1.899999976158142, '2008-09-24 00:00:00', 'Celery stalks (2 \r\n\r\nstalks = about 1 cup)', 0.8299999833106995, 0, 1, 96, 2.200000047683716, NULL),
(20, 80, 23, '2008-02-08 00:00:00', 'CUP', 1, 10, '2008-02-08 00:00:00', 'All-Bran \r\n\r\ncereal', 4, 0, 0.5, 80, 6, NULL),
(21, 95, 11, '2008-10-29 00:00:00', 'CUP', 3.4000000953674316, 2.200000047683716, '2008-10-29 00:00:00', 'Squash Casserole', 6.199999809265137, 0, 0.75, 277, 0, NULL),
(22, 231, 0, '2012-03-14 00:00:00', 'OUNCE', 14.800000190734863, 0, '2012-03-14 00:00:00', 'Ground Beef, 80/20', 23, 5.599999904632568, 3, 77, 0, NULL),
(23, 160, 25, '2008-10-29 00:00:00', 'CUSTOM', 5, 0, '2008-10-29 00:00:00', 'Ice Cream Sandwich', 2, 3, 1, 85, 13, NULL),
(24, 350, 38, '2008-10-07 00:00:00', 'CUSTOM', 11, 2, '2008-10-07 00:00:00', 'Quesadilla, \r\n\r\ncheese', 2, 4.5, 1, 38, 1, NULL),
(25, 201, 31, '2008-12-20 00:00:00', 'CUSTOM', 6, 0, '2008-12-20 00:00:00', 'Salad (Olive Garden)', 1, 0, 1, 784, 0, NULL),
(26, 200, 16, '2013-12-25 00:00:00', 'GRAM', 6, 1, '2013-12-25 00:00:00', 'Pure Protein \r\n\r\nChocolate Peanut Butter Bar', 20, 3, 50, 190, 2, NULL),
(27, 80, 3, '2008-12-08 00:00:00', 'CUSTOM', 3, 2, '2008-12-08 00:00:00', 'MorningStar Veggie Sausage \r\n\r\nLinks', 9, 0.5, 2, 300, 0, NULL),
(28, 160, 34, '2013-06-28 00:00:00', 'CUP', 0.5, 1, '2013-06-28 00:00:00', 'Zatarains Jambalaya Mix', 4, 0, 1, 440, 0, NULL),
(29, 170, 14, '2010-09-30 00:00:00', 'OUNCE', 12, 1, '2010-09-30 00:00:00', 'Sesame \r\n\r\nstickToDiets', 3, 1.5, 1, 350, 0, NULL),
(30, 240, 34, '2008-06-12 00:00:00', 'OUNCE', 10, 1, '2008-06-12 00:00:00', 'M&Ms Plain', 2, 6, 1.690000057220459, 30, 31, NULL),
(31, 90, 15, '2011-06-07 00:00:00', 'TABLESPOON', 2, 0, '2011-06-07 00:00:00', 'Malted \r\n\r\nMilk mix', 2, 1, 3, 100, 10, NULL),
(32, 160, 26, '2008-06-15 00:00:00', 'CUSTOM', 5, 1, '2008-06-15 00:00:00', 'Betty Crocker brownies (20 per batch)', 1, 1.5, 1, 105, 18, NULL),
(33, 120, 0, '2008-02-14 00:00:00', 'TABLESPOON', 14, 0, '2008-02-14 00:00:00', 'Olive Oil', 0, 2, 1, 0, 0, NULL),
(34, 44, 10, '2008-09-14 00:00:00', 'CUSTOM', 0, 2, '2008-09-14 00:00:00', 'Onion (medium)', 1, 0, 1, 4, 5, NULL),
(35, 350, 7, '2008-05-25 00:00:00', 'CUSTOM', 7, 0, '2008-05-25 00:00:00', 'Bagel', 0, 0, 1, 0, 0, NULL),
(36, 140, 16, '2008-05-08 00:00:00', 'CUSTOM', 8, 1, '2008-05-08 00:00:00', 'Ritz Bits, peanut butter', 3, 1.5, 12, 240, 3, NULL),
(37, 793, 78, '2008-12-20 00:00:00', 'CUSTOM', 39, 8, '2008-12-20 00:00:00', 'Eggplant Parmigiana \r\n\r\n(Olive Garden)', 32, 0, 1, 1583, 0, NULL),
(38, 130, 2, '2012-05-23 00:00:00', 'OUNCE', 5, 0, '2012-05-23 00:00:00', 'Hormel Peppercorn Beef Shoulder Filet', 18, 2, 4, 500, 2, NULL),
(39, 150, 26, '2009-04-28 00:00:00', 'CUP', 1.5, 4, '2009-04-28 00:00:00', 'Progresso Lentil Soup', 8, 0, 1, 500, 1, NULL),
(40, 710, 33, '0000-00-00 00:00:00', 'OUNCE', 49, 4, '2008-06-27 00:00:00', 'Penne with Sausage and \r\n\r\nPeppers ', 35, 0, 14, 1690, 0, NULL),
(41, 74, 20, '2008-12-01 00:00:00', 'CUP', 0.20000000298023224, 2, '2008-12-01 00:00:00', 'Pineapple', 1, 0, 1, 2, 14, NULL),
(42, 4240, 308, '2008-06-22 00:00:00', 'CUSTOM', 251, 18, '2008-06-22 00:00:00', '14-in pan crust Meat Lover pizza (8 slices total)', 188, 89, 1, 11170, 26, NULL),
(43, 227, 40, '2008-01-30 00:00:00', 'CUP', 1, 15, '2008-01-30 00:00:00', 'Black \r\n\r\nbeans (cooked)', 15, 0, 1, 407, 0, NULL),
(44, 100, 4, '2008-02-27 00:00:00', 'OUNCE', 6, 0, '2008-02-27 00:00:00', 'Turkey Sausage', 8, 2, 2, 610, 1, NULL),
(45, 138, 0, '2012-06-05 00:00:00', 'OUNCE', 12, 0, '2012-06-05 00:00:00', 'Pepperoni (1 \r\n\r\noz = 14 pizza-sized slices)', 6, 4, 1, 463, 0, NULL),
(46, 140, 30, '2010-10-21 00:00:00', 'CUP', 1, 10, '2010-10-21 00:00:00', 'Kashi GOLEAN cereal', 13, 0, 1, 85, 6, NULL),
(47, 200, 21, '2008-07-22 00:00:00', 'CUSTOM', 9, 1, '2008-07-22 00:00:00', 'Lance Peanut Butter Crackers (6 per pack)', 4, 1.5, 6, 250, 4, NULL),
(48, 170, 26, '2008-02-07 00:00:00', 'CUP', 2.5, 4, '2008-02-07 00:00:00', 'Campbells Chunky Beef Barley Soup', 10, 1, 1, 890, 5, NULL),
(49, 220, 35, '0000-00-00 00:00:00', 'CUSTOM', 6, 4, '2008-05-02 00:00:00', 'Burrito', 8, 1.5, 1, 490, 1, NULL),
(50, 160, 7, '2008-05-01 00:00:00', 'OUNCE', 13, 2, '2008-05-01 00:00:00', 'Honey Roasted Peanuts', 6, 2, 1, 110, 4, NULL),
(51, 45, 6, '2008-04-16 00:00:00', 'CUP', 1.5, 1, '2008-04-16 00:00:00', 'Campbells Condensed French Onion Soup', 2, 1, 0.5, 900, 4, NULL),
(52, 70, 9, '2008-04-22 00:00:00', 'CUP', 1.5, 0, '2008-04-22 00:00:00', 'Lloyds Barbeque Pork', 6, 0.5, 0.25, 360, 8, NULL),
(53, 200, 26, '0000-00-00 00:00:00', 'CUSTOM', 8, 1, '2008-09-08 00:00:00', 'Cheese pizza rolls', 7, 2, 7, 480, 3, NULL),
(54, 30, 2, '2008-02-06 00:00:00', 'OUNCE', 2, 0, '2008-02-06 00:00:00', 'Soy Coffee Creamer', 0, 0, 1, 20, 0, NULL),
(55, 125, 0.36000001430511475, '2008-03-05 00:00:00', 'OUNCE', 0, 0, '2008-03-05 00:00:00', 'Red Wine, dry', 0.2800000011920929, 0, 6, 8.5, 0.36000001430511475, NULL),
(56, 147, 11.300000190734863, '2012-08-11 00:00:00', 'OUNCE', 5.900000095367432, 0, '2012-08-11 00:00:00', 'Irish Cream', 1.399999976158142, 3.5999999046325684, 1.5, 36, 9, NULL),
(57, 370, 37, '2008-04-29 00:00:00', 'CUSTOM', 22, 3, '2008-04-29 00:00:00', 'Reeses Large Crispy Crunchy Bar', 7, 8, 1, 160, 28, NULL),
(58, 300, 47, '2008-09-04 00:00:00', 'CUSTOM', 9, 2, '2008-09-04 00:00:00', 'Empanada', 8, 3, 1, 1000, 19, NULL),
(59, 15, 4, '2012-06-27 00:00:00', 'TEASPOON', 0, 3, '2012-06-27 00:00:00', 'Benefiber', 0, 0, 2, 0, 0, NULL),
(60, 147, 30, '2008-04-20 00:00:00', 'OUNCE', 1, 1, '2008-04-20 00:00:00', 'Beignet (2 oz each)', 4, 0, 2, 80, 29, NULL),
(61, 961, 114, '2008-03-08 00:00:00', 'CUSTOM', 31, 0, '2008-03-08 00:00:00', 'Pizza, 10 inch, whole', 56, 13.5, 1, 1828, 0, NULL),
(62, 140, 18, '2008-02-10 00:00:00', 'OUNCE', 6, 2, '2008-02-10 00:00:00', 'Corn chips', 0, 0.5, 1, 80, 0, NULL),
(63, 410, 49, '2008-10-04 00:00:00', 'CUSTOM', 12, 9, '2008-10-04 00:00:00', 'Chick-Fil-A wrap', 33, 4, 1, 1510, 7, NULL),
(64, 160, 17, '2008-06-13 00:00:00', 'CUSTOM', 7, 1, '2008-06-13 00:00:00', 'Krystal', 7, 3, 1, 260, 1, NULL),
(65, 611, 24, '2008-07-22 00:00:00', 'CUSTOM', 42, 0, '2008-07-22 00:00:00', 'Quiznos Classic \r\n\r\nItalian on Wheat (large)', 35, 14, 1, 2487, 6.5, NULL),
(66, 359, 37, '2008-06-20 00:00:00', 'CUP', 16, 0, '2008-06-20 00:00:00', 'Mac and Cheese', 17, 0, 1, 721, 0, NULL),
(67, 140, 18, '2008-05-27 00:00:00', 'CUSTOM', 7, 1, '2008-05-27 00:00:00', 'Keebler Fudge Grahams', 1, 4.5, 3, 70, 10, NULL),
(68, 260, 36, '2009-01-09 00:00:00', 'OUNCE', 10, 2, '2009-01-09 00:00:00', 'Yakisoba Teriyaki Noodles', 5, 5, 2, 630, 5, NULL),
(69, 115, 29, '2008-01-29 00:00:00', 'OUNCE', 1.7000000476837158, 0, '2008-01-29 00:00:00', 'Nan (2 oz. per piece)', 5, 0, 2, 345, 0, NULL),
(70, 75, 17, '2010-09-18 00:00:00', 'OUNCE', 0, 1, '2010-09-18 00:00:00', 'Lays Light Original Potato Chips', 2, 0, 1, 200, 0, NULL),
(71, 110, 12, '2009-01-15 00:00:00', 'CUP', 2.5, 0, '2009-01-15 00:00:00', 'Milk, 1-percent', 8, 1.5, 1, 120, 12, NULL),
(72, 200, 24, '2010-11-17 00:00:00', 'CUP', 10, 0, '0000-00-00 00:00:00', 'Vegetable Lo Mein', 6, 0, 1, 700, 0, NULL),
(73, 230, 24, '0000-00-00 00:00:00', 'CUSTOM', 11, 1, '2008-07-20 00:00:00', 'Pizza Rolls', 8, 3, 18, 540, 2, NULL),
(74, 60, 0, '2008-03-14 00:00:00', 'OUNCE', 2, 0, '2008-03-14 00:00:00', 'Croutons', 0, 0, 0.5, 150, 0, NULL),
(75, 260, 19, '2008-06-13 00:00:00', 'CUSTOM', 19, 1, '2008-06-13 00:00:00', 'Krystal Corn Pup', 5, 8, 1, 480, 5, NULL),
(76, 260, 46, '2008-03-03 00:00:00', 'OUNCE', 6, 2, '2008-03-03 00:00:00', 'Rice a Roni \r\n\r\nBroccoli Au Gratin', 7, 3, 2.5, 870, 4, NULL),
(77, 50, 12, '2012-04-26 00:00:00', 'TABLESPOON', 0, 0, '2012-04-26 00:00:00', 'Bulls-Eye Kansas City Style BBQ Sauce', 0, 0, 2, 280, 11, NULL),
(78, 320, 26, '2011-01-06 00:00:00', 'CUSTOM', 16, 5, '2011-01-06 00:00:00', 'Egg & Cheese Croissant (Burger King)', 11, 7, 1, 690, 0, NULL),
(79, 430, 36, '2013-06-07 00:00:00', 'OUNCE', 26, 4, '2013-06-07 00:00:00', 'Chicken Pot Pie', 14, 10, 8, 770, 3, NULL),
(80, 39, 1.2999999523162842, '0000-00-00 00:00:00', 'OUNCE', 3.4700000286102295, 0, '2008-01-23 00:00:00', 'Half and \r\n\r\nHalf', 0.8939999938011169, 2.1600000858306885, 1, 12.289999961853027, 0, NULL),
(81, 180, 9, '2008-01-25 00:00:00', 'OUNCE', 9, 1, '2008-01-25 00:00:00', 'Kroger Baby-\r\n\r\nBack Ribs (5 servings per rack)', 16, 3.5, 3, 260, 7, NULL),
(82, 240, 13, '0000-00-00 00:00:00', 'CUSTOM', 10, 4, '2008-01-31 00:00:00', 'South Beach frozen dinner \r\n\r\n(pork with green bea', 23, 2.5, 1, 700, 5, NULL),
(83, 546, 41, '2008-05-14 00:00:00', 'OUNCE', 33.70000076293945, 0, '2008-05-14 00:00:00', 'Quesadilla, \r\n\r\nChicken', 15.699999809265137, 0, 10, 1100, 0, NULL),
(84, 190, 24, '2008-06-04 00:00:00', 'CUSTOM', 9, 2, '2008-06-04 00:00:00', 'Lance Cheese Crackers pack', 4, 1.5, 1, 290, 4, NULL),
(85, 70, 3, '2013-12-30 00:00:00', 'OUNCE', 4, 0, '2013-12-30 00:00:00', 'Velveeta', 4, 2.5, 1, 410, 2, NULL),
(86, 160, 18, '2008-05-18 00:00:00', 'CUP', 10, 3, '2008-05-18 00:00:00', 'Act II microwave popcorn, butter', 3, 5, 5, 360, 0, NULL),
(87, 56, 2, '2008-02-12 00:00:00', 'CUP', 4, 0, '2008-02-12 00:00:00', 'Hot and Sour Soup', 3, 1, 1, 0, 0, NULL),
(88, 400, 15, '2008-06-09 00:00:00', 'CUSTOM', 19, 3, '2008-06-09 00:00:00', 'Chick-Fil-A nuggets', 40, 4, 12, 1250, 4, NULL),
(89, 80, 0, '2008-01-25 00:00:00', 'TABLESPOON', 8, 0, '0000-00-00 00:00:00', 'Promise spread', 0, 1.5, 1, 85, 0, NULL),
(90, 60, 10, '2008-03-14 00:00:00', 'OUNCE', 2, 0, '2008-03-14 00:00:00', 'Oyster Crackers', 1, 0, 0.5, 140, 0, NULL),
(91, 120, 0, '2008-12-22 00:00:00', 'TABLESPOON', 14, 0, '2008-12-22 00:00:00', 'Canola Oil', 0, 1, 1, 0, 0, NULL),
(92, 170, 21, '2008-03-19 00:00:00', 'OUNCE', 9, 2, '2008-03-19 00:00:00', 'Tater Rounds, Kroger (3 oz = 11 pieces)', 2, 1.5, 3, 320, 0, NULL),
(93, 330, 35, '2008-05-31 00:00:00', 'CUSTOM', 15, 2, '0000-00-00 00:00:00', 'Red Baron Supreme Pizza (5 slices each)', 14, 7, 0.20000000298023224, 640, 4, NULL),
(94, 280, 31, '2009-01-05 00:00:00', 'OUNCE', 14, 2, '2009-01-05 00:00:00', 'Yakisoba Cheddar Cheese Noodles', 6, 7, 2, 480, 1, NULL),
(95, 330, 25, '2008-06-08 00:00:00', 'CUSTOM', 17, 1, '2008-06-08 00:00:00', 'Freschetta Ultra Thin Pizza', 17, 7, 0.33000001311302185, 730, 2, NULL),
(96, 230, 6, '2011-03-27 00:00:00', 'CUP', 21, 0, '2011-03-27 00:00:00', 'Pesto', 3, 0, 0.25, 0, 2, NULL),
(97, 25, 6, '2008-02-23 00:00:00', 'CUP', 0, 2, '2008-02-23 00:00:00', 'Tomatoes, diced', 1, 0, 0.5, 250, 4, NULL),
(98, 47, 2.200000047683716, '2008-06-13 00:00:00', 'OUNCE', 0, 0, '2008-06-13 00:00:00', 'Margarita', 0, 0, 1, 179, 1.2999999523162842, NULL),
(99, 180, 29, '2008-02-27 00:00:00', 'CUSTOM', 6, 2, '2008-02-27 00:00:00', 'Nature Valley Granola Bar (Oats and Honey)', 4, 0.5, 1, 160, 11, NULL),
(100, 30, 1, '2009-05-19 00:00:00', 'CUP', 0, 0, '2009-05-19 00:00:00', 'Egg Beaters', 6, 0, 0.25, 0, 0, NULL),
(101, 300, 33, '2008-07-28 00:00:00', 'CUSTOM', 14, 3, '2008-07-28 00:00:00', 'El Monterey Burrito', 9, 5, 1, 440, 1, NULL),
(102, 219, 49, '2008-04-26 00:00:00', 'CUP', 0.5, 3.5, '2008-04-26 00:00:00', 'Mashed Potatoes', 8, 0, 1, 85, 2, NULL),
(103, 380, 28, '2008-05-04 00:00:00', 'CUSTOM', 25, 0, '2008-05-04 00:00:00', 'Chicken Biscuit (QuikTrip)', 14, 9, 1, 720, 4, NULL),
(104, 800, 5, '2008-08-10 00:00:00', 'CUSTOM', 68, 0, '2008-08-10 00:00:00', 'IHOP Colorado Omelette', 40, 19, 1, 978, 0, NULL),
(105, 160, 18, '2013-12-17 00:00:00', 'OUNCE', 13, 5, '2013-12-17 00:00:00', 'Atkins \r\n\r\nPeanut Butter Cups', 2, 7, 1.2000000476837158, 105, 0, NULL),
(106, 280, 35, '0000-00-00 00:00:00', 'OUNCE', 14, 1, '2011-03-08 00:00:00', 'Snickers', 4, 5, 2, 140, 30, NULL),
(107, 140, 18, '2008-04-10 00:00:00', 'OUNCE', 6, 2, '2008-04-10 00:00:00', 'Sun Chips, original', 2, 1, 1, 120, 2, NULL),
(108, 258, 17, '2008-06-13 00:00:00', 'CUSTOM', 1.25, 0, '2008-06-13 00:00:00', 'White Russian Cocktail', 0.33000001311302185, 0.75, 1, 7, 0, NULL),
(109, 911, 119, '2008-04-08 00:00:00', 'CUSTOM', 29, 7, '2008-04-08 00:00:00', 'Moes Joey Bag of Donuts with Chicken', 50, 10, 1, 1777, 6, NULL),
(110, 60, 12, '2008-03-01 00:00:00', 'TABLESPOON', 1, 0, '2008-03-01 00:00:00', 'Chili Seasonings (Carroll Shelby), 12 tbls per bag', 2, 0, 2, 1320, 0, NULL),
(111, 240, 40, '2008-02-12 00:00:00', 'CUP', 2.5, 1, '2008-02-12 00:00:00', 'General Tso Chicken', 16, 1, 1, 550, 11, NULL),
(112, 150, 2, '0000-00-00 00:00:00', 'GRAM', 13, 0, '2008-07-14 00:00:00', 'Slim Jim Giant Slim', 6, 5, 28, 420, 0, NULL),
(113, 101, 1.340000033378601, '2008-01-23 00:00:00', 'CUSTOM', 7.449999809265137, 0, '2008-01-23 00:00:00', 'Egg (large)', 6.760000228881836, 2.240000009536743, 1, 171, 0, NULL),
(114, 310, 33, '2012-06-16 00:00:00', 'OUNCE', 10, 2, '2012-06-16 00:00:00', 'MET-Rx Chocolate Chunk Protein Bar', 32, 8, 3, 220, 1, NULL),
(115, 180, 31, '2008-04-26 00:00:00', 'OUNCE', 5, 0, '2008-04-26 00:00:00', 'Cornbread (Boston Market)', 2, 1.5, 2, 320, 12, NULL),
(116, 240, 26, '2008-05-17 00:00:00', 'CUSTOM', 14, 2, '2008-05-17 00:00:00', 'M&Ms Peanut \r\n\r\nButter', 5, 9, 1, 100, 22, NULL),
(117, 103, 4, '2008-01-29 00:00:00', 'CUSTOM', 7, 0, '2008-01-29 00:00:00', 'Chicken Wing (breaded)', 8.399999618530273, 2, 1, 25, 0, NULL),
(118, 140, 16, '2008-01-30 00:00:00', 'CUP', 8, 0, '2008-01-30 00:00:00', 'Ice Cream (vanilla)', 2, 5, 0.5, 45, 14, NULL),
(119, 240, 23, '2008-10-28 00:00:00', 'OUNCE', 14, 4, '2008-10-28 00:00:00', 'Eggplant Parmesan', 6, 2.5, 5, 390, 8, NULL),
(120, 303, 31, '2008-05-16 00:00:00', 'CUP', 8, 0, '2008-05-16 00:00:00', 'Beef Stew', 26, 0, 1.5, 0, 0, NULL),
(121, 120, 2, '2008-03-19 00:00:00', 'CUP', 8, 1, '2008-03-19 00:00:00', 'Barbecue Beef (Castleberrys)', 10, 3, 0.25, 230, 2, NULL),
(122, 110, 27, '2008-08-09 00:00:00', 'CUP', 0, 0, '0000-00-00 00:00:00', 'Sherbert', 1, 0, 0.5, 25, 27, NULL),
(123, 250, 40, '2008-01-24 00:00:00', 'CUSTOM', 6, 5, '2008-01-24 00:00:00', 'Clif Bar (Peanut Butter)', 12, 1.5, 1, 250, 18, NULL),
(124, 145, 17, '2008-04-29 00:00:00', 'CUP', 7.900000095367432, 0.5, '2008-04-29 00:00:00', 'Ice Cream', 2.5199999809265137, 4.900000095367432, 0.5, 58, 15.300000190734863, NULL),
(125, 170, 15, '2008-07-03 00:00:00', 'CUSTOM', 7, 0, '2008-07-03 00:00:00', 'Fried Chicken Leg', 13, 2, 1, 570, 0, NULL),
(126, 210, 22, '2010-05-26 00:00:00', 'CUSTOM', 12, 2, '2010-05-26 00:00:00', 'Mushroom Turnover', 5, 7, 5, 350, 4, NULL),
(127, 60, 13, '2008-05-08 00:00:00', 'CUP', 0, 2, '2008-05-08 00:00:00', 'Ore Ida Potatoes OBrien', 1, 0, 0.75, 20, 1, NULL),
(128, 90, 10, '2008-10-29 00:00:00', 'CUSTOM', 5, 1, '2008-10-29 00:00:00', 'Reese Peanut Butter Cup (per piece)', 2, 2, 1, 60, 8, NULL),
(129, 360, 43, '2009-04-21 00:00:00', 'CUSTOM', 13, 2, '2009-04-21 00:00:00', 'Freschetta \r\n\r\nRising Crust 4-cheese Pizza', 17, 7, 0.20000000298023224, 850, 7, NULL),
(130, 220, 26, '2008-07-04 00:00:00', 'OUNCE', 11, 1, '2008-07-04 00:00:00', 'Reeses Pieces', 5, 7, 1.5, 80, 23, NULL),
(131, 400, 30, '2008-07-26 00:00:00', 'CUSTOM', 27, 3, '2008-07-26 00:00:00', 'Waffle House hash browns', 10, 5, 1, 700, 2, NULL),
(132, 350, 34, '2008-04-19 00:00:00', 'CUSTOM', 16, 2, '2008-04-19 00:00:00', 'Arbys \r\n\r\nRoast Beef Sandwich', 21, 6, 1, 950, 0, NULL),
(133, 372, 51, '2009-07-19 00:00:00', 'OUNCE', 9, 2, '2009-07-19 00:00:00', ' Fettucine Alfredo (2 oz. dry pasta)', 19, 6, 2, 341, 6, NULL),
(134, 110, 1, '2008-02-24 00:00:00', 'CUP', 9, 0, '2008-02-24 00:00:00', 'Cheese (cheddar)', 6, 6, 0.25, 180, 0, NULL),
(135, 130, 19, '2008-02-11 00:00:00', 'CUP', 2, 4, '2008-02-11 00:00:00', 'Campbells Chunky Sirloin Steak \r\n\r\nSoup', 8, 1, 1, 890, 4, NULL),
(136, 60, 14, '2010-05-01 00:00:00', 'CUP', 0, 4, '2010-05-01 00:00:00', 'Progresso Light Italian Style Vegetable Soup', 2, 0, 1, 690, 4, NULL),
(137, 420, 36, '2008-05-17 00:00:00', 'OUNCE', 26, 1, '2008-05-17 00:00:00', 'Panda Express Beijing Beef', 14, 5, 5, 730, 15, NULL),
(138, 110, 21, '2013-07-13 00:00:00', 'OUNCE', 2.5, 1, '2013-07-13 00:00:00', 'Peanut Butter Capn \r\n\r\nCrunch', 2, 1, 0.75, 200, 9, NULL),
(139, 90, 2, '2008-02-08 00:00:00', 'TABLESPOON', 9, 0, '2008-02-08 00:00:00', 'Cream cheese', 2, 0, 2, 130, 2, NULL),
(140, 270, 33, '2008-05-03 00:00:00', 'CUSTOM', 3.5, 3, '2008-05-03 00:00:00', 'Chick-Fil-A grilled sandwich', 28, 1, 1, 940, 7, NULL),
(141, 20, 3, '2008-04-04 00:00:00', 'GRAM', 0, 2, '2008-04-04 00:00:00', 'Baby Spinach (3 cups = 85 grams)', 2, 0, 85, 65, 0, NULL),
(142, 180, 40, '2008-09-14 00:00:00', 'OUNCE', 0.5, 2, '2008-09-14 00:00:00', 'Rice a Roni Spanish Rice', 5, 0, 2, 1030, 3, NULL),
(143, 35, 8.100000381469727, '2008-04-16 00:00:00', 'OUNCE', 0.10000000149011612, 4, '2008-04-16 00:00:00', 'Green Beans', 2.0999999046325684, 0, 4, 7, 1.600000023841858, NULL),
(144, 300, 46, '2008-05-23 00:00:00', 'GRAM', 12, 1, '2008-05-23 00:00:00', 'Apple Pie', 2, 2.5, 100, 410, 21, NULL),
(145, 150, 16, '2008-04-08 00:00:00', 'CUSTOM', 9, 1, '2008-04-08 00:00:00', 'Moes Side of Chips', 2, 1.5, 1, 180, 0, NULL),
(146, 160, 20, '2008-03-16 00:00:00', 'CUP', 8, 2, '0000-00-00 00:00:00', 'Breakfast Potatoes', 1, 1.5, 0.75, 290, 1, NULL),
(147, 130, 20, '2008-03-13 00:00:00', 'CUP', 2.5, 3, '2008-03-13 00:00:00', 'Campbells Chunky \r\n\r\nMexican Chicken Tortilla Soup', 8, 1, 1, 480, 2, NULL),
(148, 230, 46, '2008-02-09 00:00:00', 'CUP', 3, 4, '2008-02-09 00:00:00', 'Kroger Low-Fat Granola Cereal w/o \r\n\r\nraisins', 5, 0.5, 0.6600000262260437, 150, 17, NULL),
(149, 100, 18, '2010-05-04 00:00:00', 'CUSTOM', 3.5, 0, '2010-05-04 00:00:00', 'Abuelita chocolate (quarter \r\n\r\ntablet)', 0, 2, 1, 0, 17, NULL),
(150, 180, 4, '2008-07-30 00:00:00', 'CUSTOM', 14, 0, '2008-07-30 00:00:00', 'Link Sausage', 9, 5, 3, 440, 4, NULL),
(151, 90, 7, '2008-02-06 00:00:00', 'CUP', 4, 1, '2008-02-06 00:00:00', 'Soymilk (plain)', 7, 0.5, 1, 115, 6, NULL),
(152, 240, 35, '2008-09-04 00:00:00', 'CUP', 6, 1, '0000-00-00 00:00:00', 'Tortellini shells', 11, 3, 1, 260, 2, NULL),
(153, 190, 22, '0000-00-00 00:00:00', 'CUSTOM', 9, 1, '2008-05-15 00:00:00', 'Grands Biscuits', 4, 2.5, 1, 670, 3, NULL),
(154, 250, 38, '2008-09-16 00:00:00', 'CUSTOM', 7, 4, '2008-09-16 00:00:00', 'Lean Pockets Whole Grain Cheese and Broccoli', 10, 3, 1, 480, 10, NULL),
(155, 242, 17.40999984741211, '2008-02-12 00:00:00', 'OUNCE', 17.399999618530273, 0, '2008-02-12 00:00:00', 'Eggroll (QuikTrip)', 4.5, 0, 4.25, 485, 0, NULL),
(156, 270, 30, '2008-02-07 00:00:00', 'CUSTOM', 5, 4, '2008-02-07 00:00:00', 'Clif Builders Protein Bar (Peanut Butter)', 20, 1.5, 1, 310, 20, NULL),
(157, 519, 58.5, '2008-05-30 00:00:00', 'CUSTOM', 15.5, 2.5, '2008-05-30 00:00:00', 'Philly Chicken Sandwich', 41, 6.5, 1, 999, 0, NULL),
(158, 360, 50, '2011-02-13 00:00:00', 'CUSTOM', 13, 4, '2011-02-13 00:00:00', 'Vegetable Pot Pie', 10, 1.5, 1, 640, 3, NULL),
(159, 150, 33, '2014-05-19 00:00:00', 'CUP', 1, 2, '0000-00-00 00:00:00', 'Rice (brown, parboiled)', 4, 0, 1, 0, 0, NULL),
(160, 35, 8.899999618530273, '2011-11-15 00:00:00', 'CUSTOM', 0.10000000149011612, 1.2999999523162842, '2011-11-15 00:00:00', 'Clementine', 0.6000000238418579, 0, 1, 0.699999988079071, 6.800000190734863, NULL),
(161, 300, 30, '2008-06-27 00:00:00', 'CUSTOM', 18, 2, '2008-06-27 00:00:00', 'DrumstickToDiet Vanilla Cone', 6, 10, 1, 90, 17, NULL),
(162, 340, 40, '2008-06-11 00:00:00', 'OUNCE', 19, 1, '2008-06-11 00:00:00', 'Milk chocolate', 4, 12, 2.25, 55, 36, NULL),
(163, 60, 0, '2010-04-24 00:00:00', 'OUNCE', 4.5, 0, '2010-04-24 00:00:00', 'Cheese (fresh mozzarella)', 6, 2.5, 1, 25, 0, NULL),
(164, 50, 11, '2008-05-02 00:00:00', 'CUSTOM', 0, 0, '2008-05-02 00:00:00', 'Sugar Cone, Brusters', 0, 0, 1, 20, 3, NULL),
(165, 160, 24, '2008-10-31 00:00:00', 'CUSTOM', 6, 1, '2008-10-31 00:00:00', 'Kroger Peanut Butter Sandwich \r\n\r\nCookies', 2, 1.5, 2, 115, 11, NULL),
(166, 770, 87, '2008-04-12 00:00:00', 'OUNCE', 18.600000381469727, 3, '2008-04-12 00:00:00', 'Calzone', 39, 15, 12, 1410, 0, NULL),
(167, 126, 9, '2008-03-14 00:00:00', 'CUSTOM', 6, 2, '2008-03-14 00:00:00', 'House Salad (Doc Greens)', 7, 4.199999809265137, 1, 290, 3, NULL),
(168, 140, 26, '2008-12-20 00:00:00', 'CUSTOM', 1.5, 0, '2008-12-20 00:00:00', 'BreadstickToDiet (Olive \r\n\r\nGarden)', 5, 0, 1, 270, 0, NULL),
(169, 280, 43, '2008-05-09 00:00:00', 'CUSTOM', 7, 3, '2008-05-09 00:00:00', 'Lean Pockets Pepperoni Pizza', 13, 3.5, 1, 720, 7, NULL),
(170, 30, 2, '2008-09-27 00:00:00', 'TABLESPOON', 1.5, 0, '2008-09-27 00:00:00', 'Bacos', 3, 0, 1, 115, 0, NULL),
(171, 210, 36, '2008-02-08 00:00:00', 'CUP', 4, 2, '2008-02-08 00:00:00', 'Cheese-stuffed tortellini (DaVinci)', 10, 3, 0.5, 570, 1, NULL),
(172, 80, 12, '2008-10-03 00:00:00', 'CUP', 0, 0, '2008-10-03 00:00:00', 'Milk, skim', 8, 0, 1, 120, 12, NULL),
(173, 630, 85, '2008-09-13 00:00:00', 'CUSTOM', 20, 16, '2008-09-13 00:00:00', 'Moes Vandalay Burrito', 23, 8, 1, 2148, 4, NULL),
(174, 650, 82, '2008-02-17 00:00:00', 'CUSTOM', 24, 5, '0000-00-00 00:00:00', 'Panera Bread Portabello And Mozzarella Panini', 27, 10, 1, 1270, 8, NULL),
(175, 420, 12, '2008-04-30 00:00:00', 'CUSTOM', 1, 0, '2008-04-30 00:00:00', '1/2 Rotisserie Chicken, no skin (Boston Market)', 84, 0, 1, 1280, 0, NULL),
(176, 170, 17, '2008-06-07 00:00:00', 'CUSTOM', 11, 1, '2008-06-07 00:00:00', 'Reese \r\n\r\nEaster Egg (full size)', 4, 3.5, 1, 150, 15, NULL),
(177, 140, 13, '2008-05-18 00:00:00', 'CUSTOM', 3, 2, '2008-05-18 00:00:00', 'Smart Ones Sweet & Sour \r\n\r\nChicken', 16, 0.5, 1, 660, 7, NULL),
(178, 110, 2, '2008-09-28 00:00:00', 'GRAM', 1.5, 0, '2008-09-28 00:00:00', 'Protein Powder, Body Fortress (30g per scoop)', 23, 0.5, 30, 55, 1, NULL),
(179, 96, 3, '2008-02-09 00:00:00', 'OUNCE', 0, 0, '0000-00-00 00:00:00', 'Miller Lite beer', 1, 0, 12, 5, 0, NULL),
(180, 140, 21, '2013-11-05 00:00:00', 'OUNCE', 5, 1, '2013-11-05 00:00:00', 'Popped Rice Snacks (1 oz. = 18 \r\n\r\ncakes)', 2, 0.5, 1, 410, 1, NULL),
(181, 220, 7, '2008-01-30 00:00:00', 'CUSTOM', 17, 1, '2008-01-30 00:00:00', 'Chili Rellano', 12, 8, 1, 51, 3, NULL),
(182, 250, 23, '2014-01-01 00:00:00', 'CUSTOM', 14, 9, '2014-01-01 00:00:00', 'Atkins \r\n\r\nChocolate Peanut Butter Bar', 17, 8, 1, 190, 1, NULL),
(183, 130, 13, '2008-02-01 00:00:00', 'CUP', 8, 1, '2008-02-01 00:00:00', 'Pasta sauce (Kroger organic tomato \r\n\r\nbasil)', 2, 0.5, 0.5, 520, 12, NULL),
(184, 170, 5, '2008-04-20 00:00:00', 'CUP', 15, 2, '2008-04-20 00:00:00', 'Peanuts, in shell', 8, 2, 0.6600000262260437, 125, 1, NULL),
(185, 110, 1, '2012-03-12 00:00:00', 'OUNCE', 70, 0, '2012-03-12 00:00:00', 'Spam Lite', 9, 3, 2, 580, 0, NULL),
(186, 602, 118, '2008-04-26 00:00:00', 'CUSTOM', 10, 4, '2008-04-26 00:00:00', 'Pad Thai (Nothing But \r\n\r\nNoodles)', 14, 2, 1, 2817, 39, NULL),
(187, 140, 33, '2014-08-30 23:22:52', 'CUSTOM', 1, 14, '2014-08-30 23:22:52', 'Eggplant, medium whole', 6, 0, 1, 15, 19, NULL),
(188, 70, 7, '2008-04-28 00:00:00', 'OUNCE', 0.5, 0, '2008-04-28 00:00:00', 'Beef Jerky', 11, 0, 1, 290, 3, NULL),
(189, 460, 52, '2008-05-17 00:00:00', 'CUSTOM', 20, 4, '2008-05-17 00:00:00', 'Banquet Chicken Fingers Meal', 18, 4.5, 1, 700, 21, NULL),
(190, 140, 35, '2008-02-16 00:00:00', 'CUP', 0, 7, '2008-02-16 00:00:00', 'Barley (dry)', 4, 0, 0.25, 0, 0, NULL),
(191, 272, 32, '2008-02-10 00:00:00', 'CUSTOM', 8, 2, '2008-02-10 00:00:00', 'Enchilada, chicken', 18, 3, 1, 924, 0, NULL),
(192, 160, 11, '2008-12-08 00:00:00', 'OUNCE', 11, 2, '2008-12-08 00:00:00', 'Planters Trail Mix', 5, 1.5, 1, 270, 1, NULL),
(193, 280, 0, '2008-05-16 00:00:00', 'POUND', 20, 0, '2008-05-16 00:00:00', 'Hamburger patty', 26, 8, 0.33000001311302185, 80, 0, NULL),
(194, 499, 43, '2008-08-31 00:00:00', 'OUNCE', 41, 0, '2008-08-31 00:00:00', 'Chocolate Pie (4 oz slice)', 5, 0, 4, 332, 0, NULL),
(195, 220, 42, '2008-04-09 00:00:00', 'CUP', 2, 3, '2008-04-09 00:00:00', 'Couscous, tomato lentil', 8, 0, 1, 670, 3, NULL),
(196, 180, 16, '2008-04-10 00:00:00', 'CUSTOM', 6, 2, '2008-04-10 00:00:00', 'Detour Caramel Peanut Bar', 15, 3, 1, 250, 6, NULL),
(197, 100, 8, '2008-03-14 00:00:00', 'CUP', 6, 2, '2008-03-14 00:00:00', 'Cream of Chicken Soup', 3, 1, 0.5, 880, 1, NULL),
(198, 280, 16, '0000-00-00 00:00:00', 'CUSTOM', 17, 0, '2008-07-23 00:00:00', 'McDonalds Chicken \r\n\r\nMcNuggets', 14, 3, 6, 600, 0, NULL),
(199, 333, 52, '2008-01-29 00:00:00', 'OUNCE', 11, 6, '2008-01-29 00:00:00', 'Fried Rice', 7, 0, 10, 650, 0, NULL),
(200, 320, 48, '2008-08-06 00:00:00', 'CUSTOM', 14, 0, '2008-08-06 00:00:00', 'Zebra Cakes', 1, 8, 2, 150, 32, NULL),
(201, 190, 0, '2012-03-08 00:00:00', 'OUNCE', 16, 0, '2012-03-08 00:00:00', 'Beef Summer Sausage', 10, 7, 2, 700, 0, NULL),
(202, 130, 20, '0000-00-00 00:00:00', 'CUP', 5, 0, '2012-03-07 00:00:00', 'Kroger Sugar Free Peanut Fudge \r\n\r\nIce Cream', 4, 3, 0.5, 10, 5, NULL),
(203, 400, 36, '2011-12-19 00:00:00', 'CUSTOM', 26, 3, '2011-12-19 00:00:00', 'Hush puppy', 5, 11, 4, 650, 4, NULL),
(204, 200, 43, '2008-09-02 00:00:00', 'CUP', 2.5, 8, '2008-09-02 00:00:00', 'Hearty Morning \r\n\r\ncereal', 5, 0.5, 0.75, 360, 11, NULL),
(205, 210, 19, '2008-10-04 00:00:00', 'CUSTOM', 14, 1, '2008-10-04 00:00:00', 'Ritter chocolate bar (18 pieces per bar)', 3, 7, 6, 25, 17, NULL),
(206, 262, 1, '2008-02-20 00:00:00', 'POUND', 4.5, 0, '0000-00-00 00:00:00', 'Snow Crab Cluster', 33, 1, 1, 1630, 0, NULL),
(207, 110, 27, '2008-05-23 00:00:00', 'OUNCE', 0, 0, '2008-05-23 00:00:00', 'Orange Juice', 2, 0, 8, 15, 22, NULL),
(208, 160, 15, '2008-07-02 00:00:00', 'OUNCE', 10, 0, '2008-07-02 00:00:00', 'Cheetos', 2, 1.5, 1, 290, 1, NULL),
(209, 170, 38, '2008-02-06 00:00:00', 'CUP', 1, 5, '2008-02-06 00:00:00', 'Grape Nuts cereal', 6, 0, 0.5, 210, 3, NULL),
(210, 40, 6, '2009-07-26 00:00:00', 'CUP', 1, 1, '2009-07-26 00:00:00', 'Green Bean Casserole', 2, 0, 0.5, 270, 2, NULL),
(211, 230, 43, '2008-03-23 00:00:00', 'OUNCE', 3.5, 1, '2008-03-23 00:00:00', 'Mahatma Broccoli and Cheese \r\n\r\nRice', 6, 1, 2.25, 930, 3, NULL),
(212, 210, 23, '2008-05-02 00:00:00', 'CUSTOM', 12, 1, '2008-05-02 00:00:00', 'Ice Cream, Brusters (1 scoop)', 3, 7, 1, 60, 19, NULL),
(213, 310, 37, '2012-09-27 00:00:00', 'CUSTOM', 11, 2, '2012-09-27 00:00:00', 'Beef tacquito (QuikTrip)', 18, 4.5, 1, 980, 0, NULL),
(214, 50, 11, '2009-09-11 00:00:00', 'OUNCE', 0, 0, '2009-09-11 00:00:00', 'Kikkoman Teriyaki Glaze', 1, 0, 1, 810, 9, NULL),
(215, 160, 40, '2010-04-21 00:00:00', 'CUP', 0, 0, '2010-04-21 00:00:00', 'Grape juice', 0, 0, 1, 35, 40, NULL),
(216, 80, 21, '2012-11-23 00:00:00', 'OUNCE', 0, 0, '2012-11-23 00:00:00', 'Gatorade', 0, 0, 12, 160, 21, NULL),
(217, 330, 39, '2010-07-26 00:00:00', 'CUSTOM', 13, 2, '2010-07-26 00:00:00', 'Hot Pockets Four Cheese Pizza', 13, 6, 1, 750, 9, NULL),
(218, 140, 39, '2008-11-26 00:00:00', 'OUNCE', 0, 0, '2008-11-26 00:00:00', 'Coke', 0, 0, 12, 50, 39, NULL),
(219, 3880, 296, '2008-06-14 00:00:00', 'CUSTOM', 214, 17, '2008-06-14 00:00:00', '14-in reg. crust Meat Lover pizza (8 slices total)', 193, 86, 1, 12040, 28, NULL),
(220, 160, 37, '2013-10-05 00:00:00', 'CUP', 0, 1, '2013-10-05 00:00:00', 'Grits (cooked, 1 cup = 1/4 cup uncooked)', 3, 0, 1, 0, 0, NULL),
(221, 150, 30, '2008-03-12 00:00:00', 'CUP', 1.5, 1, '2008-03-12 00:00:00', 'Tuna Helper Cheesy \r\n\r\nPasta', 5, 0.5, 0.75, 750, 2, NULL),
(222, 160, 14, '2008-03-12 00:00:00', 'CUP', 10, 2, '2008-03-12 00:00:00', 'Smartfood White Cheddar Popcorn', 3, 2, 1.75, 290, 2, NULL),
(223, 20, 3, '2008-04-12 00:00:00', 'OUNCE', 0, 0, '2008-04-12 00:00:00', 'Fat Free Half and Half', 1, 0, 1, 30, 2, NULL),
(224, 420, 50, '2008-09-08 00:00:00', 'CUSTOM', 17, 3, '2008-09-08 00:00:00', 'Tombstone Individual Deep Dish \r\n\r\nCheese Pizza', 17, 10, 1, 830, 7, NULL),
(225, 170, 32, '2008-03-18 00:00:00', 'CUP', 2.5, 1, '2008-03-18 00:00:00', 'Hamburger Helper Cheesy Nacho', 5, 0.5, 0.5, 730, 4, NULL),
(226, 234, 53, '2010-01-26 00:00:00', 'CUSTOM', 3, 11, '2010-01-26 00:00:00', 'Pomegranate, 4-inch diameter', 5, 0, 1, 8, 39, NULL),
(227, 140, 2, '2012-03-08 00:00:00', 'OUNCE', 7, 0, '2012-03-08 00:00:00', 'Chicken Bratwurst (1 \r\n\r\nlink = 3 oz)', 15, 2, 3, 480, 2, NULL),
(228, 270, 47, '2008-05-17 00:00:00', 'CUP', 4.5, 1, '2008-05-17 00:00:00', 'Lipton Creamy Garlic Shells', 8, 2.5, 1, 750, 3, NULL),
(229, 250, 0, '2008-03-16 00:00:00', 'OUNCE', 25, 0, '2008-03-16 00:00:00', 'Pork Sausage', 7, 9, 2, 360, 0, NULL),
(230, 160, 20, '2014-05-21 00:00:00', 'OUNCE', 8, 1, '2014-05-21 00:00:00', 'Crunch n Munch', 2, 2.5, 1, 100, 11, NULL),
(231, 140, 18, '2008-10-22 00:00:00', 'OUNCE', 7, 1, '2008-10-22 00:00:00', 'Hot \r\n\r\nFries', 2, 1.5, 1, 220, 1, NULL),
(232, 100, 1, '2008-02-06 00:00:00', 'CUSTOM', 3, 0, '2008-02-06 00:00:00', 'Gortons Salmon fillets (3.15 oz each)', 17, 0.5, 1, 300, 0, NULL),
(233, 20, 3, '2011-05-08 00:00:00', 'TEASPOON', 0, 3, '2011-05-08 00:00:00', 'Metamucil Smooth Texture Sugar Free Orange Flavor', 0, 0, 1, 5, 0, NULL),
(234, 200, 42, '2008-02-01 00:00:00', 'OUNCE', 1, 2, '2008-02-01 00:00:00', 'Spaghetti, Barilla (2 oz dry = 1 cup cooked)', 7, 0, 2, 0, 1, NULL),
(235, 130, 27, '2009-02-17 00:00:00', 'CUP', 1, 1, '2009-02-17 00:00:00', 'Honey Bunches of Oats \r\n\r\ncereal (honey roasted)', 2, 0, 0.75, 150, 6, NULL),
(236, 242, 53, '2008-01-30 00:00:00', 'CUP', 0, 0, '2008-01-30 00:00:00', 'Rice (white, cooked)', 4, 0, 1, 0, 0, NULL),
(237, 230, 19, '2009-01-12 00:00:00', 'CUP', 17, 2, '2009-01-12 00:00:00', 'Chocolate Covered Peanuts', 5, 8, 0.25, 25, 15, NULL),
(238, 80, 9, '2011-07-09 00:00:00', 'CUP', 2, 0, '2011-07-09 00:00:00', 'Lloyds Barbeque Chicken', 6, 1, 0.25, 360, 9, NULL),
(239, 497, 36.29999923706055, '2008-02-15 00:00:00', 'CUSTOM', 28.799999237060547, 0.6000000238418579, '2008-02-15 00:00:00', 'Chicken Tequila \r\n\r\nFettuccine (CPK)', 23.600000381469727, 15.199999809265137, 1, 504, 1, NULL),
(240, 40, 0, '2012-06-27 00:00:00', 'TEASPOON', 4.5, 0, '2012-06-27 00:00:00', 'Cod liver \r\n\r\noil', 0, 1, 1, 0, 0, NULL),
(241, 220, 34, '2008-05-12 00:00:00', 'OUNCE', 8, 2, '2008-05-12 00:00:00', 'Corn Nuts, ranch', 4, 1, 1.7000000476837158, 410, 1, NULL),
(242, 190, 40, '2008-09-03 00:00:00', 'CUP', 1, 4, '2008-09-03 00:00:00', 'Vigo Red Beans and Rice', 7, 0, 1, 730, 2, NULL),
(243, 30, 2, '2008-05-07 00:00:00', 'TABLESPOON', 1, 1, '2008-05-07 00:00:00', 'Imitation Bacon Bits', 3, 0, 1, 125, 0, NULL),
(244, 230, 14, '2012-07-15 00:00:00', 'OUNCE', 11, 1, '2012-07-15 00:00:00', 'Broccoli and Cheese Stuffed Chicken Breast', 19, 3.5, 5, 400, 1, NULL),
(245, 480, 50, '2008-05-17 00:00:00', 'OUNCE', 21, 2, '2008-05-17 00:00:00', 'Panda Express Orange Chicken', 21, 4.5, 5.5, 820, 5, NULL),
(246, 170, 4, '0000-00-00 00:00:00', 'CUSTOM', 9, 2, '2008-01-26 00:00:00', 'Veggie Burger patty \r\n\r\n(Morningstar)', 17, 1, 1, 360, 0, NULL),
(247, 170, 1, '2013-12-20 00:00:00', 'TABLESPOON', 18, 0, '2013-12-20 00:00:00', 'Newmans Own Creamy Caesar dressing', 0, 3, 2, 340, 0, NULL),
(248, 120, 22, '2008-01-25 00:00:00', 'CUSTOM', 1.5, 3, '2008-01-25 00:00:00', 'Whole Wheat Bread (45g slice)', 6, 0.5, 1, 220, 4, NULL),
(249, 426, 43, '2010-01-22 00:00:00', 'CUSTOM', 19, 4, '2010-01-22 00:00:00', 'Homemade Vegetable Lasagna', 22, 9, 1, 776, 0, NULL),
(250, 200, 21, '2008-04-03 00:00:00', 'CUSTOM', 7, 2, '2008-04-03 00:00:00', 'Slim-Fast High Protein Peanut \r\n\r\nGranola Bar', 15, 2.5, 1, 200, 8, NULL),
(251, 350, 25, '2008-07-31 00:00:00', 'OUNCE', 15, 0, '2008-07-31 00:00:00', 'Chicken Cordon Bleu', 38, 0, 7, 575, 0, NULL),
(252, 120, 16, '2008-02-22 00:00:00', 'CUP', 3.5, 3, '2008-02-22 00:00:00', 'Progresso Minestrone with Chicken Soup', 6, 0.5, 1, 870, 2, NULL),
(253, 160, 18, '2008-10-14 00:00:00', 'GRAM', 8, 1, '2008-10-14 00:00:00', 'Cheez-It crackers', 4, 2, 30, 250, 1, NULL),
(254, 280, 28, '2008-05-17 00:00:00', 'CUSTOM', 17, 2, '0000-00-00 00:00:00', 'Twix PB', 5, 8, 1, 140, 19, NULL),
(255, 240, 38, '2008-05-29 00:00:00', 'GRAM', 9, 0, '2008-05-29 00:00:00', 'Triscuits', 5, 1.5, 55, 350, 0, NULL),
(256, 70, 0, '2009-04-06 00:00:00', 'OUNCE', 6, 0, '2009-04-06 00:00:00', 'Cheese (Muenster slice)', 5, 3.5, 0.75, 160, 0, NULL),
(257, 110, 14, '2008-02-28 00:00:00', 'CUP', 2.5, 1, '2008-02-28 00:00:00', 'Progresso Chicken and Homestyle \r\n\r\nNoodles Soup', 8, 0.5, 1, 920, 2, NULL),
(258, 166, 9.800000190734863, '2008-03-15 00:00:00', 'OUNCE', 0, 0, '2008-03-15 00:00:00', 'Beer', 0, 0, 12, 18, 0, NULL),
(259, 70, 2, '2011-08-31 00:00:00', 'OUNCE', 1.5, 0, '2011-08-31 00:00:00', 'Roast \r\n\r\nBeef', 11, 0.5, 2.4000000953674316, 600, 1, NULL),
(260, 229, 25, '2008-02-24 00:00:00', 'OUNCE', 13, 0, '2008-02-24 00:00:00', 'Donut, glazed (1 donut = 2 oz)', 3, 0, 2, 220, 0, NULL),
(261, 328, 38, '2008-02-13 00:00:00', 'CUSTOM', 9, 4, '0000-00-00 00:00:00', 'Manicotti (2 pieces)', 24, 5, 1, 891, 0, NULL),
(262, 250, 32, '2012-01-17 00:00:00', 'CUSTOM', 9, 1, '2012-01-17 00:00:00', 'Little Caesars \r\n\r\ncheese pizza (1 slice of 8)', 12, 4, 1, 440, 3, NULL),
(263, 150, 21, '2008-03-12 00:00:00', 'CUSTOM', 6, 1, '2008-03-12 00:00:00', 'Wheat Thins (16 crackers)', 2, 1, 1, 260, 4, NULL),
(264, 100, 3, '2008-02-20 00:00:00', 'CUP', 9, 0, '2008-02-20 00:00:00', 'Ragu Double Cheddar Cheese Sauce', 2, 3, 0.25, 510, 2, NULL),
(265, 20, 3, '2008-04-04 00:00:00', 'OUNCE', 0, 1, '2008-04-04 00:00:00', 'Portabella \r\n\r\nMushrooms', 3, 0, 3, 15, 0, NULL),
(266, 320, 34, '2008-10-22 00:00:00', 'CUSTOM', 16, 1, '2008-10-22 00:00:00', 'Totinos Cheese pizza', 12, 5, 0.5, 3, 4, NULL),
(267, 230, 44, '2008-09-07 00:00:00', 'CUSTOM', 3, 5, '2008-09-07 00:00:00', 'Subway \r\n\r\nVeggie Delite (6 inch)', 9, 1, 1, 500, 7, NULL),
(268, 230, 20, '2013-05-08 00:00:00', 'CUP', 13, 3, '2013-05-08 00:00:00', 'Campbells Chunky Clam Chowder \r\n\r\nSoup', 7, 2, 1, 890, 1, NULL),
(269, 310, 37, '2008-01-30 00:00:00', 'CUSTOM', 11, 2, '2008-01-30 00:00:00', 'Chicken tacquito (QuikTrip)', 18, 4.5, 1, 980, 0, NULL),
(270, 289, 30, '2008-01-28 00:00:00', 'OUNCE', 8, 0, '2008-01-28 00:00:00', 'French Dip Sandwich', 23, 0, 6.599999904632568, 458, 0, NULL),
(271, 80, 18, '0000-00-00 00:00:00', 'CUP', 0, 2, '2008-05-07 00:00:00', 'Idahoan Instant Potatoes \r\n\r\nmix, Original', 2, 0, 0.33000001311302185, 15, 0, NULL),
(272, 1430, 99, '2011-10-08 00:00:00', 'CUSTOM', 92, 10, '2011-10-08 00:00:00', 'Fish and Chips (Joes Crab \r\n\r\nShack)', 52, 17, 1, 3160, 12, NULL),
(273, 8, 1, '2008-09-27 00:00:00', 'CUP', 0, 1, '2008-09-27 00:00:00', 'Romaine Lettuce', 0.5, 0, 4, 2, 0, NULL),
(274, 120, 0, '2008-01-23 00:00:00', 'TABLESPOON', 13.600000381469727, 0, '2008-01-23 00:00:00', 'Flaxseed Oil', 0, 1.2799999713897705, 1, 0, 0, NULL),
(275, 400, 32, '2008-08-12 00:00:00', 'CUSTOM', 27, 1, '2008-08-12 00:00:00', 'McDonalds Sausage Biscuit', 11, 8, 1, 5, 2, NULL),
(276, 670, 55, '2008-05-04 00:00:00', 'OUNCE', 27, 3, '2008-05-04 00:00:00', 'Asiago Roast Beef Sandwich (Panera Bread)', 50, 15, 13, 1200, 3, NULL),
(277, 380, 28, '2008-05-04 00:00:00', 'CUSTOM', 25, 0, '2008-05-04 00:00:00', 'Sausage, Egg, Cheese Croissant (QuikTrip)', 14, 9, 1, 720, 4, NULL),
(278, 80, 13, '2012-08-20 00:00:00', 'CUP', 4, 4, '2012-08-20 00:00:00', 'Breyers No Sugar Added \r\n\r\nVanilla Ice Cream', 2, 2.5, 0.5, 45, 4, NULL),
(279, 143, 0, '2008-04-16 00:00:00', 'OUNCE', 3, 0, '2008-04-16 00:00:00', 'Pork Tenderloin', 13, 3, 3, 0, 0, NULL),
(280, 250, 28, '2008-09-12 00:00:00', 'CUSTOM', 11, 2, '2008-09-12 00:00:00', '14-\r\n\r\nin pan crust Veggie Lover pizza (8 slice to', 16, 4, 1, 530, 3, NULL),
(281, 170, 5, '2008-09-08 00:00:00', 'CUP', 16, 3, '2008-09-08 00:00:00', 'Almonds', 6, 1.5, 0.25, 250, 1, NULL),
(282, 290, 26, '2008-08-04 00:00:00', 'CUP', 10, 4, '0000-00-00 00:00:00', 'Michael Angelo Meat Lasagna', 24, 8, 1, 490, 5, NULL),
(283, 170, 25, '2009-01-25 00:00:00', 'CUP', 7, 0, '2009-01-25 00:00:00', 'Broccoli Cheese \r\n\r\nSoup', 3, 2, 1, 970, 8, NULL),
(284, 5, 1, '2008-02-16 00:00:00', 'CUP', 0, 0, '2008-02-16 00:00:00', 'Broth (from cube or powder)', 0, 0, 1, 870, 0, NULL),
(285, 230, 36, '2008-05-17 00:00:00', 'CUSTOM', 9, 0, '2008-05-17 00:00:00', 'Reeses \r\n\r\nWhipps', 4, 7, 1, 130, 32, NULL),
(286, 150, 27, '2010-10-16 00:00:00', 'CUP', 2.5, 4, '2010-10-16 00:00:00', 'Oatmeal, uncooked', 6, 0, 0.5, 0, 1, NULL),
(287, 150, 32, '2008-02-16 00:00:00', 'CUP', 1, 1, '2008-02-16 00:00:00', 'Rice (brown, \r\n\r\ncooked)', 2, 0, 0.75, 0, 0, NULL),
(288, 220, 18, '2012-04-10 00:00:00', 'CUP', 9, 3, '2012-04-10 00:00:00', 'Hormel Chili No Beans', 16, 4, 1, 970, 3, NULL),
(289, 160, 23, '2008-04-09 00:00:00', 'CUP', 6, 2, '2008-04-09 00:00:00', 'Campbells \r\n\r\nChunky Baked Potato Cheddar Bacon So', 4, 1, 1, 870, 3, NULL),
(290, 120, 1, '2008-03-06 00:00:00', 'OUNCE', 4, 0, '2008-03-06 00:00:00', 'Turkey Breast, \r\n\r\nButterball, roasted', 21, 1.5, 3, 430, 0, NULL),
(291, 180, 34, '2008-11-14 00:00:00', 'OUNCE', 4, 0, '2008-11-14 00:00:00', 'Thai Iced Coffee', 3, 2, 10, 200, 29, NULL),
(292, 45, 2, '2008-09-26 00:00:00', 'TABLESPOON', 4, 0, '2008-09-26 00:00:00', 'Newmans Own Light Balsamic Vinaigrette dressing', 0, 0.5, 2, 470, 1, NULL),
(293, 150, 13, '2008-02-15 00:00:00', 'CUP', 8, 1, '2008-02-15 00:00:00', 'Progresso Creamy Chicken Wild Rice Soup', 6, 2, 1, 900, 5, NULL),
(294, 150, 37, '2008-09-21 00:00:00', 'CUP', 0, 0, '2008-09-21 00:00:00', 'Risotto rice \r\n\r\n(uncooked)', 3, 0, 0.25, 0, 0, NULL),
(295, 188, 29, '2008-01-28 00:00:00', 'OUNCE', 8, 0, '2008-01-28 00:00:00', 'Carrots, glazed', 2, 3, 6.199999809265137, 172, 20, NULL),
(296, 260, 35, '2008-04-19 00:00:00', 'CUSTOM', 10, 2, '2008-04-19 00:00:00', 'Sushi Rolls (4 rolls)', 6, 2, 4, 590, 8, NULL),
(297, 13, 2.5, '2012-05-29 00:00:00', 'OUNCE', 0, 1.2999999523162842, '2012-05-29 00:00:00', 'Asparagus, raw \r\n\r\n(4 spears)', 1.399999976158142, 0, 2.299999952316284, 1, 1.2000000476837158, NULL),
(298, 200, 14, '2008-02-10 00:00:00', 'CUSTOM', 11, 2, '2008-02-10 00:00:00', 'Taco, chicken (hard shell)', 11, 3.5, 1, 340, 1, NULL),
(299, 140, 20, '2008-05-09 00:00:00', 'OUNCE', 5, 0, '2008-05-09 00:00:00', 'Snyders Jalapeno Pretzel Pieces', 2, 3, 1, 370, 1, NULL),
(300, 160, 15, '2008-08-27 00:00:00', 'OUNCE', 10, 1, '0000-00-00 00:00:00', 'Fritos', 2, 1.5, 1, 260, 1, NULL),
(301, 200, 23, '2008-07-02 00:00:00', 'CUSTOM', 8, 0, '2008-07-02 00:00:00', 'Fried Chicken Wing', 10, 2, 1, 740, 0, NULL),
(302, 130, 19, '2008-06-14 00:00:00', 'OUNCE', 5, 0, '2008-06-14 00:00:00', 'Baked Cheetos', 2, 1, 1, 240, 1, NULL),
(303, 25, 2.5, '2008-04-10 00:00:00', 'CUSTOM', 1.5, 0, '2008-04-10 00:00:00', 'Hersheys Kiss', 0.33000001311302185, 1, 1, 4, 2.5, NULL),
(304, 340, 31, '2008-05-10 00:00:00', 'CUSTOM', 14, 2, '2008-05-10 00:00:00', 'Jersey Mikes Italian Sub (regular size, 2 \r\n\r\nserv', 22, 6, 1, 1320, 5, NULL),
(305, 560, 37, '2008-05-30 00:00:00', 'CUSTOM', 38, 0, '2008-05-30 00:00:00', 'Bacon, Egg & Cheese Biscuit', 16, 11, 1, 1360, 4, NULL),
(306, 400, 38, '2008-06-06 00:00:00', 'CUSTOM', 23, 2, '2008-06-06 00:00:00', 'Swanson Chicken Pot Pie', 10, 7, 1, 760, 2, NULL),
(307, 120, 19, '2008-06-12 00:00:00', 'CUSTOM', 4.5, 0, '2008-06-12 00:00:00', 'Peanut Butter Cream Cookies', 1, 1, 3, 100, 9, NULL),
(308, 190, 43, '2008-12-01 00:00:00', 'CUP', 0, 1, '0000-00-00 00:00:00', 'Long Grain and Wild Rice (2 oz dry - 1 cup cooked)', 4, 0, 1, 700, 1, NULL),
(309, 130, 24, '2008-04-03 00:00:00', 'CUSTOM', 4.5, 4, '2008-04-03 00:00:00', 'Chocolate Cookies and Cream Ice Cream Bar (WW)', 3, 1.5, 1, 100, 16, NULL),
(310, 105, 5, '2008-04-19 00:00:00', 'OUNCE', 0, 0, '2008-04-19 00:00:00', 'Sake', 0.5, 0, 3.5, 0, 5, NULL),
(311, 153, 0, '2008-04-09 00:00:00', 'OUNCE', 8.600000381469727, 0, '2008-04-09 00:00:00', 'Catfish', 17.700000762939453, 2, 4, 60, 0, NULL),
(312, 937, 18, '2011-07-31 00:00:00', 'CUSTOM', 16, 6, '2011-07-31 00:00:00', 'CPK Original Chopped Salad, Full', 42, 16, 1, 1865, 0, NULL),
(313, 10, 2, '2008-04-18 00:00:00', 'OUNCE', 0, 0, '2008-04-18 00:00:00', 'Coffeemate, fat-\r\n\r\nfree', 0, 0, 0.5, 0, 2, NULL),
(314, 160, 16, '2008-03-19 00:00:00', 'CUP', 10, 1, '2008-03-19 00:00:00', 'Gardettos Rye Chips', 2, 2, 0.5, 340, 1, NULL),
(315, 310, 48, '2008-01-26 00:00:00', 'CUSTOM', 5, 5, '2008-01-26 00:00:00', 'Subway Oven \r\n\r\nRoasted Chicken Breast (6 inch)', 24, 1.5, 1, 830, 9, NULL),
(316, 170, 20, '0000-00-00 00:00:00', 'CUP', 2.5, 3, '2008-02-14 00:00:00', 'Campbells Chunky Fully \r\n\r\nLoaded Beef Stew', 10, 2, 1, 810, 5, NULL),
(317, 180, 20, '2008-03-15 00:00:00', 'CUSTOM', 8, 1, '2008-03-15 00:00:00', 'Cheese stickToDiets', 8, 3, 2, 380, 0, NULL),
(318, 100, 22, '2008-03-14 00:00:00', 'OUNCE', 1, 1, '2008-03-14 00:00:00', 'Pretzels', 2.5999999046325684, 0, 1, 485, 0, NULL),
(319, 300, 0, '2008-01-27 00:00:00', 'OUNCE', 18, 0, '2008-01-27 00:00:00', 'Steak (flat-iron cut)', 33, 6.75, 6, 127.5, 0, NULL),
(320, 180, 18, '2008-03-07 00:00:00', 'CUP', 7, 2, '0000-00-00 00:00:00', 'Campbells Chunky Chicken Alfredo Soup', 10, 1, 1, 930, 1, NULL),
(321, 70, 11, '2010-04-24 00:00:00', 'CUSTOM', 2.5, 1, '2010-04-24 00:00:00', 'Carr \r\n\r\nCrackers', 2, 0, 4, 140, 0, NULL),
(322, 230, 36, '2008-05-21 00:00:00', 'CUSTOM', 8, 1, '2008-05-21 00:00:00', 'Milky Way Dark', 1, 6, 1, 75, 30, NULL),
(323, 404, 80, '2008-02-09 00:00:00', 'CUSTOM', 25, 0, '2008-02-09 00:00:00', 'Chicken Panini \r\n\r\n(13 oz)', 30, 0, 1, 983, 0, NULL),
(324, 290, 9, '2008-05-12 00:00:00', 'OUNCE', 25, 4, '2008-05-12 00:00:00', 'Hot Peanuts', 12, 3.5, 1.75, 350, 2, NULL),
(325, 52, 11, '2008-09-14 00:00:00', 'CUSTOM', 1, 4, '2008-09-14 00:00:00', 'Zucchini (large)', 4, 0, 1, 32, 6, NULL),
(326, 130, 20, '2008-05-26 00:00:00', 'CUSTOM', 5, 1.5, '2008-05-26 00:00:00', 'Muffin', 2.4000000953674316, 1, 1, 0, 6, NULL),
(327, 460, 43, '2008-06-25 00:00:00', 'CUSTOM', 24, 4, '2008-06-25 00:00:00', 'Stouffers \r\n\r\nFrench Bread Pizza', 17, 8, 1, 880, 5, NULL),
(328, 410, 38, '2008-04-29 00:00:00', 'CUSTOM', 16, 1, '2008-04-29 00:00:00', 'Chick-Fil-A sandwich', 28, 3.5, 1, 1300, 5, NULL),
(329, 35, 6, '2008-04-16 00:00:00', 'OUNCE', 1.5, 0, '2008-04-16 00:00:00', 'Coffeemate', 0, 0, 0.5, 0, 6, NULL),
(330, 130, 18, '2008-01-24 00:00:00', 'CUP', 2, 2, '2008-01-24 00:00:00', 'Campbells Chunky Steak and Potato \r\n\r\nSoup', 10, 0.5, 1, 920, 2, NULL),
(331, 100, 15, '2012-07-15 00:00:00', 'CUSTOM', 3, 1, '2012-07-15 00:00:00', 'Little Caesars crazy bread (1 breadstickToDiet)', 3, 0.5, 1, 150, 1, NULL),
(332, 220, 27, '2008-05-01 00:00:00', 'CUSTOM', 11, 1, '2008-05-01 00:00:00', 'Kit Kat bar', 3, 7, 1, 30, 22, NULL),
(333, 200, 6, '2008-01-30 00:00:00', 'TABLESPOON', 16, 2, '2008-01-30 00:00:00', 'Peanut Butter', 8, 4, 2, 85, 2, NULL),
(334, 310, 37, '2008-05-05 00:00:00', 'CUSTOM', 11, 2, '2008-05-05 00:00:00', 'Jalapeno Cheese tacquito (QuikTrip)', 18, 4.5, 1, 980, 0, NULL),
(335, 200, 15, '2008-02-01 00:00:00', 'CUP', 13, 4, '2008-02-01 00:00:00', 'Kroger Chunky \r\n\r\nChicken Broccoli Cheese Potato S', 6, 3.5, 1, 1280, 0, NULL),
(336, 323, 31, '2008-05-02 00:00:00', 'CUSTOM', 18, 0, '2008-05-02 00:00:00', 'Enchilada', 12, 9, 1, 1319, 0, NULL),
(337, 120, 18, '2008-02-29 00:00:00', 'CUP', 1.5, 2, '2008-02-29 00:00:00', 'Kroger Chunky Beef Pot Roast Soup', 9, 0.5, 1, 1020, 4, NULL),
(338, 1, 1, '2008-05-25 00:00:00', 'OUNCE', 1, 1, '2008-05-25 00:00:00', 'Pie', 1, 1, 1, 1, 1, NULL),
(339, 120, 22, '2008-07-01 00:00:00', 'CUP', 2.5, 1, '2008-07-01 00:00:00', 'Betty Crocker Three Cheese Baked Potatoes', 2, 2, 0.6600000262260437, 600, 2, NULL),
(340, 25, 6, '2008-03-21 00:00:00', 'CUSTOM', 0, 0, '2008-03-21 00:00:00', 'Jello Sugar-Free Pudding Mix (4 servings per box)', 0, 0, 1, 300, 0, NULL),
(341, 350, 37, '2008-10-15 00:00:00', 'CUSTOM', 15, 4, '2008-10-15 00:00:00', 'Tombstone Original Cheese Pizza (4 slices ea)', 18, 8, 0.25, 660, 5, NULL),
(342, 30, 11, '2008-02-20 00:00:00', 'CUP', 0, 0, '2008-02-20 00:00:00', 'Sugar-Free \r\n\r\nMaple Syrup (Maple Grove Farms)', 0, 0, 0.25, 110, 0, NULL),
(343, 130, 19, '0000-00-00 00:00:00', 'CUP', 3.5, 3, '2010-10-20 00:00:00', 'Bear Naked Granola \r\n\r\n(chocolate)', 3, 1, 0.25, 10, 6, NULL),
(344, 310, 49, '2008-05-23 00:00:00', 'CUSTOM', 11, 1, '2008-05-23 00:00:00', 'Cinnamon Roll', 5, 4, 1, 410, 24, NULL),
(345, 189, 18.700000762939453, '2008-05-06 00:00:00', 'CUP', 8.899999618530273, 3, '2008-05-06 00:00:00', 'Spicy Tofu And Vegetable Stir Fry', 12, 1.2999999523162842, 1, 440, 10, NULL),
(346, 80, 41, '2008-10-28 00:00:00', 'CUP', 1.2000000476837158, 0, '2008-10-28 00:00:00', 'Corn', 5.300000190734863, 0.20000000298023224, 0.5, 78, 0, NULL),
(347, 510, 69, '2008-06-11 00:00:00', 'CUSTOM', 17, 3, '2008-06-11 00:00:00', 'CPK Personal Cheese Pizza', 22, 7, 1, 730, 10, NULL),
(348, 180, 28, '2008-06-20 00:00:00', 'CUSTOM', 4, 3, '2008-06-20 00:00:00', 'Smart Ones Chocolate \r\n\r\nMousse', 7, 3.5, 1, 100, 13, NULL),
(349, 100, 0, '2008-09-21 00:00:00', 'CUP', 7, 0, '2008-09-21 00:00:00', 'Cheese (parmesan)', 10, 4, 0.25, 350, 0, NULL),
(350, 210, 19, '2013-06-09 00:00:00', 'OUNCE', 9, 3, '2013-06-09 00:00:00', 'Parmesan \r\n\r\nEncrusted Tilapia fish fillets', 12, 1, 3.5, 430, 1, NULL),
(351, 140, 24, '0000-00-00 00:00:00', 'CUP', 1, 8, '2008-01-27 00:00:00', 'Campbells Condensed Lentil \r\n\r\nSoup', 9, 0.5, 0.5, 800, 2, NULL),
(352, 290, 4, '2008-04-26 00:00:00', 'CUSTOM', 11, 0, '2008-04-26 00:00:00', '1/4 White Rotisserie Chicken (Boston Market)', 45, 3.5, 1, 780, 1, NULL),
(353, 350, 46, '2008-05-03 00:00:00', 'OUNCE', 17, 5, '0000-00-00 00:00:00', 'French Fries', 4, 3.5, 4, 150, 1, NULL),
(354, 140, 24, '0000-00-00 00:00:00', 'OUNCE', 3.5, 2, '2009-05-19 00:00:00', 'Baked Potato Chips', 2, 0.5, 1.1749999523162842, 240, 3, NULL),
(355, 564, 64.19999694824219, '2008-03-16 00:00:00', 'OUNCE', 31.799999237060547, 0, '2008-03-16 00:00:00', 'Onion Rings', 7.599999904632568, 14.199999809265137, 6, 881, 0, NULL),
(356, 25, 6, '2008-02-21 00:00:00', 'TABLESPOON', 0, 5, '2008-02-21 00:00:00', 'Fibersure', 0, 0, 1, 0, 0, NULL),
(357, 190, 0, '2011-11-22 00:00:00', 'OUNCE', 14, 0, '2011-11-22 00:00:00', 'Beef brisket', 15, 6, 4, 990, 0, NULL),
(358, 134, 0, '2012-06-15 00:00:00', 'OUNCE', 4.400000095367432, 0, '2012-06-15 00:00:00', 'Chicken Thigh, boneless and \r\n\r\nskinless', 22.200000762939453, 1.100000023841858, 4, 97, 0, NULL),
(359, 90, 0, '2008-01-23 00:00:00', 'CUP', 7, 0, '2008-01-23 00:00:00', 'Cheese (Italian \r\n\r\nblend)', 7, 4, 0.25, 190, 0, NULL),
(360, 190, 41, '2012-11-18 00:00:00', 'OUNCE', 1, 5, '2012-11-18 00:00:00', 'Dreamfields Linguine', 7, 0, 2, 10, 1, NULL),
(361, 120, 22, '2009-07-10 00:00:00', 'CUSTOM', 3.5, 0, '2009-07-10 00:00:00', 'Graham \r\n\r\ncrackers (29 grams, 2 full sheets)', 2, 1, 1, 160, 7, NULL),
(362, 140, 28, '0000-00-00 00:00:00', 'CUP', 1, 3, '2008-02-14 00:00:00', 'Buckwheat Pancake Mix \r\n\r\n(Hodgson Mill)', 5, 0, 0.33000001311302185, 290, 1, NULL),
(363, 300, 45, '0000-00-00 00:00:00', 'CUSTOM', 7, 3, '2008-10-18 00:00:00', 'Lean Pockets Four Cheese \r\n\r\nPizza', 15, 4, 1, 680, 11, NULL),
(364, 140, 20, '2008-02-26 00:00:00', 'CUP', 6, 1, '2008-02-26 00:00:00', 'Chex Mix', 3, 1, 0.5, 390, 0, NULL),
(365, 29, 6, '0000-00-00 00:00:00', 'CUSTOM', 1, 0, '2008-01-30 00:00:00', 'Tortilla, corn (5 inch \r\n\r\ndiameter)', 1, 0, 1, 20, 0, NULL),
(366, 140, 13, '2011-02-27 00:00:00', 'GRAM', 9, 0, '2011-02-27 00:00:00', 'Girl Scout Tagalong Cookies (25g = 2 cookies)', 2, 5, 25, 95, 8, NULL),
(367, 220, 25, '2008-10-19 00:00:00', 'CUSTOM', 12, 3, '2008-10-19 00:00:00', 'Hersheys Special Dark', 2, 7, 1, 0, 21, NULL),
(368, 370, 45, '0000-00-00 00:00:00', 'CUSTOM', 19, 3, '2008-10-04 00:00:00', 'Chick-Fil-A brownie', 4, 6, 1, 180, 28, NULL),
(369, 352, 49.099998474121094, '2014-04-23 00:00:00', 'CUP', 10.199999809265137, 10.300000190734863, '2014-04-23 00:00:00', 'Mushroom and Bacon \r\n\r\nCasserole', 16, 4.300000190734863, 1, 435, 0, NULL),
(370, 250, 17, '2008-03-13 00:00:00', 'CUSTOM', 8, 0, '2008-03-13 00:00:00', 'Cupcake', 0, 5, 1, 3, 17, NULL),
(371, 62, 16, '2009-01-07 00:00:00', 'CUP', 0.30000001192092896, 1, '0000-00-00 00:00:00', 'Grapes', 0.6000000238418579, 0.10000000149011612, 1, 2, 15, NULL);
INSERT INTO `nutrition_food` (`id`, `calories`, `carbs`, `created_time`, `default_serving_type`, `fat`, `fiber`, `last_updated_time`, `name`, `protein`, `saturated_fat`, `serving_type_qty`, `sodium`, `sugar`, `owner_id`) VALUES
(372, 130, 9, '2008-03-17 00:00:00', 'CUP', 10, 1, '2008-03-17 00:00:00', 'Progresso Creamy Mushroom Soup', 2, 3, 1, 820, 2, NULL),
(373, 190, 26, '2008-11-22 00:00:00', 'OUNCE', 7, 1, '2008-11-22 00:00:00', 'Ramen noodles (3 oz. block)', 5, 3.5, 1.5, 880, 0, NULL),
(374, 73, 13.899999618530273, '2008-02-15 00:00:00', 'OUNCE', 0.800000011920929, 0.4000000059604645, '2008-02-15 00:00:00', 'Focaccia \r\n\r\nBread', 2.4000000953674316, 0.20000000298023224, 1, 133, 0, NULL),
(375, 180, 31, '2008-09-04 00:00:00', 'CUSTOM', 4.5, 0, '2008-09-04 00:00:00', 'Garlic Bread \r\n\r\n(loaf)', 0, 0, 0.20000000298023224, 0, 0, NULL),
(376, 200, 38, '2008-01-26 00:00:00', 'CUSTOM', 2.5, 2, '2008-01-26 00:00:00', 'Hamburger Bun (whole wheat)', 7, 0.5, 1, 280, 9, NULL),
(377, 1338, 257.5, '2008-01-26 00:00:00', 'OUNCE', 6.75, 8.75, '0000-00-00 00:00:00', 'Pad See-Ew (with tofu)', 54.5, 2, 24, 4692.5, 11.75, NULL),
(378, 100, 0, '2008-02-04 00:00:00', 'TABLESPOON', 11.5, 0, '2008-02-04 00:00:00', 'Butter', 0, 7, 1, 120, 0, NULL),
(379, 210, 21, '2008-01-25 00:00:00', 'CUP', 10, 3, '2008-01-25 00:00:00', 'Campbells Chunky Cheese Steak and Potato Soup', 9, 2.5, 1, 940, 3, NULL),
(380, 360, 7, '2008-07-02 00:00:00', 'CUSTOM', 21, 0, '2008-07-02 00:00:00', 'Fried Chicken Breast', 37, 5, 1, 1020, 0, NULL),
(381, 370, 51, '0000-00-00 00:00:00', 'CUSTOM', 14, 7, '2008-01-23 00:00:00', 'Peanut Butter Sandwich on \r\n\r\nWheat', 16, 2.5, 1, 630, 10, NULL),
(382, 210, 37, '2008-09-12 00:00:00', 'OUNCE', 5, 1, '2008-09-12 00:00:00', 'Rice a Roni Creamy Four Cheese', 6, 2.5, 2, 740, 3, NULL),
(383, 97, 0, '2008-06-08 00:00:00', 'OUNCE', 0, 0, '2008-06-08 00:00:00', 'Bourbon', 0, 0, 1.5, 0, 0, NULL),
(384, 250, 32, '2008-06-21 00:00:00', 'CUSTOM', 8, 0, '2008-06-21 00:00:00', 'French Toast', 13, 2, 1, 355, 20, NULL),
(385, 210, 25, '2008-05-17 00:00:00', 'CUSTOM', 11, 1, '2008-05-17 00:00:00', 'Hersheys Take \r\n\r\n5', 4, 5, 1, 180, 18, NULL),
(386, 190, 0, '2012-05-29 00:00:00', 'OUNCE', 9.699999809265137, 0, '2012-05-29 00:00:00', 'Fresh salmon', 24.100000381469727, 1.7000000476837158, 4, 53, 0, NULL),
(387, 190, 14, '2008-05-04 00:00:00', 'OUNCE', 10, 1, '2008-05-04 00:00:00', 'Potato Chips', 2, 1, 1, 230, 0, NULL),
(388, 85, 2, '2010-12-24 00:00:00', 'OUNCE', 0, 0, '2010-12-24 00:00:00', 'Brandy', 0, 0, 1, 0, 2, NULL),
(389, 520, 52, '2011-06-16 00:00:00', 'CUSTOM', 18, 6, '2011-06-16 00:00:00', 'Subway Big Philly Cheesesteak (6 inch)', 39, 9, 1, 1370, 8, NULL),
(390, 100, 23, '2008-05-20 00:00:00', 'CUSTOM', 9.600000381469727, 1, '2008-05-20 00:00:00', 'Pancake', 2, 0, 1, 320, 7, NULL),
(391, 180, 16, '2008-02-03 00:00:00', 'CUSTOM', 10, 1, '2008-02-03 00:00:00', 'MorningStar Broccoli Cheddar Veggie \r\n\r\nBites', 8, 2.5, 3, 550, 4, NULL),
(392, 130, 16, '2008-08-03 00:00:00', 'CUSTOM', 5, 0, '2008-08-03 00:00:00', 'Eggo Waffle', 3, 1, 1, 250, 0, NULL),
(393, 244, 12, '2012-03-06 00:00:00', 'CUP', 9.399999618530273, 2, '2012-03-06 00:00:00', 'Spicy \r\n\r\nBasil Chicken', 28, 0, 2, 656, 0, NULL),
(394, 220, 18, '2008-01-30 00:00:00', 'CUSTOM', 8, 2, '2008-01-30 00:00:00', 'Smart Ones frozen dinner (turkey breast, \r\n\r\npotat', 18, 2.5, 1, 720, 1, NULL),
(395, 130, 19, '2008-02-28 00:00:00', 'CUP', 1.5, 1, '2008-02-28 00:00:00', 'Progresso Beef Pot Roast Soup', 10, 0.5, 1, 950, 4, NULL),
(396, 250, 2, '2012-03-09 00:00:00', 'OUNCE', 22, 0, '2012-03-09 00:00:00', 'Spam', 11, 8, 3, 990, 1, NULL),
(397, 300, 34.099998474121094, '2008-11-06 00:00:00', 'CUP', 15.600000381469727, 0, '2008-11-06 00:00:00', 'Fried okra', 6.800000190734863, 0, 0.5, 445, 0, NULL),
(398, 220, 0, '2008-03-01 00:00:00', 'OUNCE', 17, 0, '2008-03-01 00:00:00', 'Turkey, ground', 19, 5, 4, 130, 0, NULL),
(399, 109, 0, '2008-05-16 00:00:00', 'CUSTOM', 9, 0, '2008-05-16 00:00:00', 'Bacon', 6, 3, 3, 303, 0, NULL),
(400, 110, 19, '2008-03-27 00:00:00', 'CUP', 2.5, 2, '2008-03-27 00:00:00', 'Idahoan Instant Potatoes mix, Loaded Baked', 3, 0.5, 0.25, 450, 2, NULL),
(401, 150, 12, '2008-03-15 00:00:00', 'CUP', 8, 0, '2008-03-15 00:00:00', 'Milk, whole', 8, 5, 1, 130, 12, NULL),
(402, 51, 10, '2008-01-25 00:00:00', 'OUNCE', 0.6000000238418579, 3.9000000953674316, '2008-01-25 00:00:00', 'Broccoli (5.3 oz stalk)', 4.300000190734863, 0.10000000149011612, 5.300000190734863, 50, 2.5999999046325684, NULL),
(403, 762, 79, '2008-04-06 00:00:00', 'CUSTOM', 33, 6, '2008-04-06 00:00:00', 'Olive Garden Five Cheese Ziti \r\n\r\nAl Forno', 38, 16, 1, 1703, 10, NULL),
(404, 96, 3, '2008-04-19 00:00:00', 'OUNCE', 0, 0, '2008-04-19 00:00:00', 'Beer, lite', 0, 0, 12, 0, 0, NULL),
(405, 200, 31, '2011-08-02 00:00:00', 'CUP', 2.5, 2, '2011-08-02 00:00:00', 'Spicy Chicken And \r\n\r\nVegetable Stir Fry', 11, 0.5, 1, 1040, 11, NULL),
(406, 182, 8, '2011-12-19 00:00:00', 'CUSTOM', 12, 1, '2011-12-19 00:00:00', 'Captain D Battered Fish \r\n\r\nFillet', 9, 6, 1, 454, 0, NULL),
(407, 80, 20, '2008-02-16 00:00:00', 'CUP', 0, 11, '2008-02-16 00:00:00', 'Lentils (dry)', 10, 0, 0.25, 0, 2, NULL),
(408, 70, 9, '2008-02-06 00:00:00', 'CUP', 2, 1, '2008-02-06 00:00:00', 'Campbells Condensed \r\n\r\nChicken Noodle Soup', 3, 0.5, 0.5, 900, 0, NULL),
(409, 260, 27, '2008-09-08 00:00:00', 'CUP', 10, 3, '2008-09-08 00:00:00', 'Michael Angelo Vegetable Lasagna', 15, 5, 1, 600, 8, NULL),
(410, 37, 8, '2008-04-25 00:00:00', 'OUNCE', 0, 3, '0000-00-00 00:00:00', 'Steamed vegetables', 2, 0, 4, 35, 3, NULL),
(411, 192, 0, '0000-00-00 00:00:00', 'OUNCE', 10.210000038146973, 0, '2008-01-23 00:00:00', 'Ham (lean \r\n\r\nonly)', 23.600000381469727, 2.359999895095825, 3.5, 1250, 0, NULL),
(412, 160, 31, '2008-01-23 00:00:00', 'CUSTOM', 2, 0, '2008-01-23 00:00:00', 'Pillsbury Pizza \r\n\r\nCrust (1/6 package)', 5, 0.5, 1, 470, 4, NULL),
(413, 330, 48, '2011-02-25 00:00:00', 'OUNCE', 8, 3, '2011-02-25 00:00:00', 'Veggie Burrito (Cederlane)', 14, 0, 6, 590, 8, NULL),
(414, 270, 34, '2008-05-21 00:00:00', 'CUSTOM', 13, 2, '0000-00-00 00:00:00', 'Reeses Fast Break', 5, 4.5, 1, 210, 29, NULL),
(415, 214, 34, '2008-05-15 00:00:00', 'OUNCE', 7, 1, '2008-05-15 00:00:00', 'Chocolate Chip \r\n\r\nCookie', 2.5, 4, 2, 112, 12, NULL),
(416, 367, 19, '2008-03-28 00:00:00', 'CUSTOM', 17, 0, '2008-03-28 00:00:00', 'Chicken Parmigiana (1 breast)', 35, 8, 1, 913, 0, NULL),
(417, 30, 7, '2008-04-04 00:00:00', 'CUSTOM', 0, 2, '2008-04-04 00:00:00', 'Green Bell Pepper, medium size', 1, 0, 1, 0, 4, NULL),
(418, 490, 34, '2008-05-03 00:00:00', 'CUSTOM', 31, 1, '2008-05-03 00:00:00', 'Jalapeno Cheese Sausage \r\n\r\n(QuikTrip)', 20, 11, 1, 1220, 3, NULL),
(419, 170, 8, '2008-04-25 00:00:00', 'CUP', 14, 1, '2008-04-25 00:00:00', 'Caesar Salad', 3, 2.5, 1.5, 380, 2, NULL),
(420, 330, 30, '2008-07-02 00:00:00', 'CUSTOM', 15, 0, '2008-07-02 00:00:00', 'Fried Chicken \r\n\r\nThigh', 19, 4, 1, 1000, 0, NULL),
(421, 17, 0.5, '2008-04-26 00:00:00', 'OUNCE', 1, 0, '2008-04-26 00:00:00', 'Tofu', 1.8600000143051147, 0, 1, 2.2699999809265137, 0, NULL),
(422, 30, 7, '2010-10-20 00:00:00', 'OUNCE', 0, 0, '2010-10-20 00:00:00', 'Coffeemate, fat free', 0, 0, 0.5, 5, 6, NULL),
(423, 679, 14, '2008-10-14 00:00:00', 'CUSTOM', 7.400000095367432, 3, '2008-10-14 00:00:00', 'IHOP Garden \r\n\r\nOmelette', 45, 2.640000104904175, 1, 1097, 1, NULL),
(424, 210, 53, '2009-12-27 00:00:00', 'CUP', 0, 0, '2009-12-27 00:00:00', 'Maple Syrup', 0, 0, 0.25, 5, 50, NULL),
(425, 710, 81, '2008-05-20 00:00:00', 'OUNCE', 23, 4, '2008-05-20 00:00:00', 'Cuban Sandwich', 43, 11, 10, 1600, 9, NULL),
(426, 730, 92, '2008-05-03 00:00:00', 'OUNCE', 38, 2, '2008-05-03 00:00:00', 'Peanut Buster Parfait', 16, 0, 10.699999809265137, 400, 74, NULL),
(427, 170, 40, '2008-03-06 00:00:00', 'CUP', 1, 6, '2008-03-06 00:00:00', 'Shredded Wheat', 6, 0, 1.25, 0, 0, NULL),
(428, 100, 26, '2008-02-09 00:00:00', 'CUSTOM', 0, 3, '2008-02-09 00:00:00', 'Baked Potato (5.2 \r\n\r\noz)', 4, 0, 1, 0, 3, NULL);

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_food_diets`
--

CREATE TABLE IF NOT EXISTS `nutrition_food_diets` (
  `foods_id` int(11) NOT NULL,
  `diets_id` int(11) NOT NULL,
  KEY `FK4rietiwuanikkqvd7fdwsalc9` (`diets_id`),
  KEY `FKd9h4curq64qnw7bb636c1ybnn` (`foods_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_food_habit`
--

CREATE TABLE IF NOT EXISTS `nutrition_food_habit` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FK21cnok6qfqusue49iodyaded2` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_food_habit_food`
--

CREATE TABLE IF NOT EXISTS `nutrition_food_habit_food` (
  `food_habits_id` int(11) NOT NULL,
  `food_id` int(11) NOT NULL,
  KEY `FK7tawrs4gv9htjf4l6d97ng82i` (`food_id`),
  KEY `FKg5xnybys3c25l6l19uxekulkx` (`food_habits_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_healthy_meal`
--

CREATE TABLE IF NOT EXISTS `nutrition_healthy_meal` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `health_meal_type` varchar(255) NOT NULL,
  `diets_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKbii6fdfyv6c3mnyi2faqfb7fu` (`diets_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_healthy_meal_mealContents`
--

CREATE TABLE IF NOT EXISTS `nutrition_healthy_meal_mealContents` (
  `healthy_meal_id` int(11) NOT NULL,
  `mealContents_id` int(11) NOT NULL,
  UNIQUE KEY `UK_pllt6kn1gawl8cvh9w1gh47kl` (`mealContents_id`),
  KEY `FKtg7wyvqjcwl7q2hl01snyxfhl` (`healthy_meal_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_mensuration`
--

CREATE TABLE IF NOT EXISTS `nutrition_mensuration` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` date NOT NULL,
  `fat_mass` float NOT NULL,
  `waist_size` float NOT NULL,
  `weight` float NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkpbvmjrju50r3j8xywd3rfxqq` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_patient_food_habits`
--

CREATE TABLE IF NOT EXISTS `nutrition_patient_food_habits` (
  `user_id` int(11) NOT NULL,
  `food_habits_id` int(11) NOT NULL,
  UNIQUE KEY `UK_cvsc1uc0okp3mm7tu3jhpckg1` (`food_habits_id`),
  KEY `FKsmir3jc05i2r6cx6fm1a1hbj8` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_sport_activity`
--

CREATE TABLE IF NOT EXISTS `nutrition_sport_activity` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `calorie` int(11) NOT NULL,
  `duration` float NOT NULL,
  `objectid` int(11) DEFAULT NULL,
  `sportdate` datetime NOT NULL,
  `user_id` int(11) DEFAULT NULL,
  `exercise_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `FKja0nqgdr9t14utpdm0t95p83e` (`user_id`),
  KEY `FKja10qgdr9t15utp289295p84o` (`exercise_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Structure de la table `nutrition_user`
--

CREATE TABLE IF NOT EXISTS `nutrition_user` (
  `user_id` int(11) NOT NULL AUTO_INCREMENT,
  `date_of_birth` date NOT NULL,
  `fat_mass` float DEFAULT NULL,
  `first_name` varchar(25) NOT NULL,
  `morpholigical_profil` varchar(255) DEFAULT NULL,
  `name` varchar(25) NOT NULL,
  `profession` varchar(255) NOT NULL,
  `psycological_profil` varchar(255) DEFAULT NULL,
  `registration_date` date NOT NULL,
  `waist_size` int(11) DEFAULT NULL,
  `weight` int(11) DEFAULT NULL,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8mb4 AUTO_INCREMENT=589216 ;

--
-- Contenu de la table `nutrition_user`
--

INSERT INTO `nutrition_user` (`user_id`, `date_of_birth`, `fat_mass`, `first_name`, `morpholigical_profil`, `name`, `profession`, `psycological_profil`, `registration_date`, `waist_size`, `weight`) VALUES
(193258, '2002-02-14', NULL, 'Jenny', NULL, 'Tayla', 'teacher', NULL, '2020-02-05', NULL, 90),
(497598, '0000-00-00', NULL, 'Benjamin', NULL, 'Dover', 'retired', NULL, '2017-01-23', NULL, 74),
(589215, '1970-09-18', NULL, 'Mike', NULL, 'Lock', 'photographer', NULL, '0000-00-00', NULL, 89);

--
-- Contraintes pour les tables exportées
--

--
-- Contraintes pour la table `nutrition_account`
--
ALTER TABLE `nutrition_account`
  ADD CONSTRAINT `FKqeqbft17ojmddumyd99fkyehg` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_mealContent`
--
ALTER TABLE `nutrition_mealContent`
  ADD CONSTRAINT `FK681k5t8c3mslrnag2wnt4x1n3` FOREIGN KEY (`healthy_meal_id`) REFERENCES `nutrition_healthy_meal` (`id`),
  ADD CONSTRAINT `FKhecprtixsx54yvh4t9m2qlaj9` FOREIGN KEY (`food_id`) REFERENCES `nutrition_food` (`id`);

--
-- Contraintes pour la table `nutrition_daily_meal`
--
ALTER TABLE `nutrition_daily_meal`
  ADD CONSTRAINT `FKj5u5ctcc3bdcg6jnm4fi9tv2d` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_eat`
--
ALTER TABLE `nutrition_eat`
  ADD CONSTRAINT `FKet61bbovq7x1xo4owx94p0x55` FOREIGN KEY (`food_id`) REFERENCES `nutrition_food` (`id`),
  ADD CONSTRAINT `FKhja7hwdtj26w40ql9aqpyrrbd` FOREIGN KEY (`daily_meal_id`) REFERENCES `nutrition_daily_meal` (`id`);

--
-- Contraintes pour la table `nutrition_stickToDiet`
--
ALTER TABLE `nutrition_stickToDiet`
  ADD CONSTRAINT `FK6l5sm5jx3i3w29gsyrmf4hado` FOREIGN KEY (`diets_id`) REFERENCES `nutrition_diet` (`id`),
  ADD CONSTRAINT `FKdu54qn44hggnog7itd6d88hkv` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_food_diets`
--
ALTER TABLE `nutrition_food_diets`
  ADD CONSTRAINT `FK4rietiwuanikkqvd7fdwsalc9` FOREIGN KEY (`diets_id`) REFERENCES `nutrition_diet` (`id`),
  ADD CONSTRAINT `FKd9h4curq64qnw7bb636c1ybnn` FOREIGN KEY (`foods_id`) REFERENCES `nutrition_food` (`id`);

--
-- Contraintes pour la table `nutrition_food_habit`
--
ALTER TABLE `nutrition_food_habit`
  ADD CONSTRAINT `FK21cnok6qfqusue49iodyaded2` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_food_habit_food`
--
ALTER TABLE `nutrition_food_habit_food`
  ADD CONSTRAINT `FK7tawrs4gv9htjf4l6d97ng82i` FOREIGN KEY (`food_id`) REFERENCES `nutrition_food` (`id`),
  ADD CONSTRAINT `FKg5xnybys3c25l6l19uxekulkx` FOREIGN KEY (`food_habits_id`) REFERENCES `nutrition_food_habit` (`id`);

--
-- Contraintes pour la table `nutrition_healthy_meal`
--
ALTER TABLE `nutrition_healthy_meal`
  ADD CONSTRAINT `FKbii6fdfyv6c3mnyi2faqfb7fu` FOREIGN KEY (`diets_id`) REFERENCES `nutrition_diet` (`id`);

--
-- Contraintes pour la table `nutrition_healthy_meal_mealContents`
--
ALTER TABLE `nutrition_healthy_meal_mealContents`
  ADD CONSTRAINT `FKsxbdwpb7ka7okqrkyypy15ihv` FOREIGN KEY (`mealContents_id`) REFERENCES `nutrition_mealContent` (`id`),
  ADD CONSTRAINT `FKtg7wyvqjcwl7q2hl01snyxfhl` FOREIGN KEY (`healthy_meal_id`) REFERENCES `nutrition_healthy_meal` (`id`);

--
-- Contraintes pour la table `nutrition_mensuration`
--
ALTER TABLE `nutrition_mensuration`
  ADD CONSTRAINT `FKkpbvmjrju50r3j8xywd3rfxqq` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_patient_food_habits`
--
ALTER TABLE `nutrition_patient_food_habits`
  ADD CONSTRAINT `FKk7c0bgk9lviqfeagq3vepuxp6` FOREIGN KEY (`food_habits_id`) REFERENCES `nutrition_food_habit` (`id`),
  ADD CONSTRAINT `FKsmir3jc05i2r6cx6fm1a1hbj8` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`);

--
-- Contraintes pour la table `nutrition_sport_activity`
--
ALTER TABLE `nutrition_sport_activity`
  ADD CONSTRAINT `FKja0nqgdr9t14utpdm0t95p83e` FOREIGN KEY (`user_id`) REFERENCES `nutrition_user` (`user_id`),
  ADD CONSTRAINT `FKja10qgdr9t15utp289295p84o` FOREIGN KEY (`exercise_id`) REFERENCES `nutrition_exercise` (`id`);

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;