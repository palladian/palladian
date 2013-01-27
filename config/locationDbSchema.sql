/*
SQLyog Community v10.51 
MySQL - 5.5.24 : Database - webknox
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*Table structure for table `location_alternative_names` */

DROP TABLE IF EXISTS `location_alternative_names`;

CREATE TABLE `location_alternative_names` (
  `locationId` bigint(20) unsigned NOT NULL COMMENT 'The id of the location.',
  `alternativeName` varchar(200) DEFAULT NULL COMMENT 'An alternative name used for the location.',
  KEY `locationId` (`locationId`),
  KEY `alternativeName` (`alternativeName`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*Table structure for table `locations` */

DROP TABLE IF EXISTS `locations`;

CREATE TABLE `locations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'The id of the location.',
  `type` varchar(50) NOT NULL COMMENT 'The type of the location.',
  `name` varchar(255) NOT NULL COMMENT 'The primary name of the location.',
  `longitude` double(15,3) NOT NULL COMMENT 'The longitude of the location.',
  `latitude` double(15,3) NOT NULL COMMENT 'The latitude of the location.',
  `population` int(11) DEFAULT NULL COMMENT 'If applicable, the population of the location.',
  PRIMARY KEY (`id`),
  UNIQUE KEY `unique` (`type`,`name`,`longitude`,`latitude`),
  KEY `name` (`name`),
  KEY `type` (`type`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
