# ************************************************************
# Sequel Pro SQL dump
# Version 4004
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.5.25)
# Datenbank: locations
# Erstellungsdauer: 2013-02-06 14:04:12 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Export von Tabelle location_alternative_names
# ------------------------------------------------------------

DROP TABLE IF EXISTS `location_alternative_names`;

CREATE TABLE `location_alternative_names` (
  `locationId` bigint(20) unsigned NOT NULL COMMENT 'The id of the location.',
  `alternativeName` varchar(200) DEFAULT NULL COMMENT 'An alternative name used for the location.',
  KEY `locationId` (`locationId`),
  KEY `alternativeName` (`alternativeName`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;



# Export von Tabelle location_hierarchy
# ------------------------------------------------------------

DROP TABLE IF EXISTS `location_hierarchy`;

CREATE TABLE `location_hierarchy` (
  `parentId` bigint(20) unsigned NOT NULL COMMENT 'The parent in the hierarchical relation.',
  `childId` bigint(20) unsigned NOT NULL COMMENT 'The child in the hierarchical relation.',
  UNIQUE KEY `parentChildUnique` (`parentId`,`childId`),
  KEY `parentId` (`parentId`),
  KEY `childId` (`childId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;



# Export von Tabelle locations
# ------------------------------------------------------------

DROP TABLE IF EXISTS `locations`;

CREATE TABLE `locations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'The id of the location.',
  `type` varchar(20) NOT NULL DEFAULT '' COMMENT 'The type of the location.',
  `name` varchar(255) NOT NULL COMMENT 'The primary name of the location.',
  `longitude` double(8,5) NOT NULL COMMENT 'The longitude of the location.',
  `latitude` double(8,5) NOT NULL COMMENT 'The latitude of the location.',
  `population` int(11) DEFAULT NULL COMMENT 'If applicable, the population of the location.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique` (`type`,`name`,`longitude`,`latitude`),
  KEY `name` (`name`),
  KEY `type` (`type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
