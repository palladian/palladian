# ************************************************************
# Sequel Pro SQL dump
# Version 4096
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.5.25)
# Datenbank: locations
# Erstellungsdauer: 2013-06-04 19:45:40 +0000
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
  `language` char(2) DEFAULT NULL COMMENT 'The language for this alternative name, in ISO 639-1 format. NULL means no specified language.',
  UNIQUE KEY `idNameLangUnique` (`locationId`,`alternativeName`,`language`),
  KEY `locationId` (`locationId`),
  KEY `alternativeName` (`alternativeName`),
  KEY `language` (`language`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;



# Export von Tabelle locations
# ------------------------------------------------------------

DROP TABLE IF EXISTS `locations`;

CREATE TABLE `locations` (
  `id` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'The id of the location.',
  `type` varchar(20) NOT NULL DEFAULT '' COMMENT 'The type of the location.',
  `name` varchar(255) NOT NULL COMMENT 'The primary name of the location.',
  `latitude` double(8,5) DEFAULT NULL COMMENT 'The latitude of the location.',
  `longitude` double(8,5) DEFAULT NULL COMMENT 'The longitude of the location.',
  `population` bigint(15) unsigned DEFAULT NULL COMMENT 'If applicable, the population of the location.',
  `ancestorIds` varchar(255) DEFAULT NULL COMMENT 'All ancestor IDs in the hierarchical relation, separated by slashes, starting with the root ancestor. String must start and end with slash character.',
  PRIMARY KEY (`id`),
  KEY `name` (`name`),
  KEY `type` (`type`),
  KEY `ancestorIds` (`ancestorIds`),
  KEY `latitudeLongitude` (`latitude`,`longitude`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;




--
-- Dumping routines (PROCEDURE) for database 'locations'
--
DELIMITER ;;

# Dump of PROCEDURE search_locations
# ------------------------------------------------------------

/*!50003 DROP PROCEDURE IF EXISTS `search_locations` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 PROCEDURE `search_locations`(in searchNames varchar(1048576), in searchLanguages varchar(512))
begin
  declare currentName varchar(1024);
  -- two tables with same content; necessary because MySQL does not allow to re-use one table
  -- within a stored procedure
  CREATE temporary table tmp1 (query varchar(1024)) engine memory;
  CREATE temporary table tmp2 (query varchar(1024)) engine memory;
  -- split up the comma-separated query locations and store them in temporary table
  while(char_length(searchNames) > 0) do
    if (locate(',', searchNames)) then
      -- head: take first element
      set currentName = (SELECT substring_index(searchNames, ',', 1)); 
      -- tail: remaining elements
      set searchNames = (SELECT substring(searchNames, char_length(currentName) + 2));
    else
      set currentName = searchNames;
      set searchNames = '';
    end if;
    insert into tmp1 values(currentName);
    insert into tmp2 values(currentName);
  end while;
  -- the query; important is to include the actual search string as first column, so that we
  -- can associate this later
  SELECT ids.query, l.* , lan.*, group_concat(alternativeName, '', '#', IFNULL(`language`,'')) as alternatives
  from locations l join
  (SELECT id, `query` FROM tmp1, locations WHERE `query` = `name`
  union
  SELECT locationId AS id, `query` 
    FROM tmp2, location_alternative_names 
    WHERE `query` = alternativeName AND (`language` IS NULL or find_in_set(`language`, searchLanguages) > 0)
  ) as ids
  on l.id = ids.id left join location_alternative_names lan on l.id = lan.locationId
  group by id, `query`;
  DROP temporary table IF EXISTS tmp1;
  DROP temporary table IF EXISTS tmp2;
end */;;

/*!50003 SET SESSION SQL_MODE=@OLD_SQL_MODE */;;
DELIMITER ;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
