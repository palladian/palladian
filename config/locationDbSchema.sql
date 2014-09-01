# ************************************************************
# Sequel Pro SQL dump
# Version 4096
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.5.34)
# Datenbank: locations
# Erstellungsdauer: 2014-09-01 21:41:01 +0000
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
  `locationId` int(11) unsigned NOT NULL COMMENT 'The id of the location.',
  `alternativeName` varchar(200) NOT NULL DEFAULT '' COMMENT 'An alternative name used for the location.',
  `language` char(2) DEFAULT NULL COMMENT 'The language for this alternative name, in ISO 639-1 format. NULL means no specified language.',
  UNIQUE KEY `idNameLangUnique` (`locationId`,`alternativeName`,`language`),
  KEY `alternativeName` (`alternativeName`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;



# Export von Tabelle locations
# ------------------------------------------------------------

DROP TABLE IF EXISTS `locations`;

CREATE TABLE `locations` (
  `id` int(11) unsigned NOT NULL DEFAULT '0' COMMENT 'The id of the location.',
  `type` enum('CITY','CONTINENT','COUNTRY','LANDMARK','POI','REGION','STREET','UNDETERMINED','UNIT') NOT NULL DEFAULT 'UNDETERMINED',
  `name` varchar(200) NOT NULL DEFAULT '' COMMENT 'The primary name of the location.',
  `latitude` float(8,5) DEFAULT NULL,
  `longitude` float(8,5) DEFAULT NULL,
  `population` bigint(15) unsigned DEFAULT NULL COMMENT 'If applicable, the population of the location.',
  `ancestorIds` varchar(100) DEFAULT NULL COMMENT 'All ancestor IDs in the hierarchical relation, separated by slashes, starting with the root ancestor. String must start and end with slash character.',
  PRIMARY KEY (`id`),
  KEY `name` (`name`),
  KEY `ancestorIds` (`ancestorIds`),
  KEY `latitudeLongitude` (`latitude`,`longitude`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8;




--
-- Dumping routines (PROCEDURE) for database 'locations'
--
DELIMITER ;;

# Dump of PROCEDURE delete_misleading_abbreviations
# ------------------------------------------------------------

/*!50003 DROP PROCEDURE IF EXISTS `delete_misleading_abbreviations` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 PROCEDURE `delete_misleading_abbreviations`()
BEGIN
-- This procedure removes abbreviations from the database, which have shown to be
-- misleading for location extraction. A prominent example is the abbrevation 'CNN',
-- which was present for the location 'Canonbury Railway Station', its usual
-- meaning however is obviously different. After some empiric analysis of the data, 
-- we found, misclassifications through abbrevations happen frequently for railway
-- stations and Maxican states (e.g. 'Estado de Sinaloa' with abbreviation 'SIN'),
-- that's why we remove abbreviations for those two types of locations.
	DELETE lan 
	FROM `location_alternative_names` lan 
	INNER JOIN `locations` l ON lan.`locationId` = l.`id` 
	WHERE (l.`name` LIKE "estado de %" OR l.`name` LIKE "% station")
		AND lan.`alternativeName` = BINARY UPPER(lan.`alternativeName`)
		AND CHAR_LENGTH(lan.`alternativeName`) < 4
		AND lan.`language` IS NULL;
END */;;

/*!50003 SET SESSION SQL_MODE=@OLD_SQL_MODE */;;
# Dump of PROCEDURE search_locations
# ------------------------------------------------------------

/*!50003 DROP PROCEDURE IF EXISTS `search_locations` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 PROCEDURE `search_locations`(
  IN searchNames varchar(1048576) CHARACTER SET utf8, 
  IN searchLanguages varchar(512),
  IN latitude double,
  IN longitude double,
  IN radius double
  )
BEGIN
-- Procedure for searching locations. Three different search modi are available:
-- a) search by name(s) and languages for alternative names;
--    example: search_locations('san francisco', 'en', null, null, null)
-- b) search by latitude and longitude coordinates and a given radius in kilometers;
--    example: search_locations(null, null, 37, -122, 5)
-- c) combination of a) and b), i.e. search for locations with a specified name in an area;
--    example: search_locations('san francisco', 'en', 37, -122, 250)
-- For parameters 'searchNames' and 'searchLanguages', multiple values can be specified
-- by separating them with colons, e.g. 'san francisco,los angeles' or 'en,de,fr'.
  DECLARE nameQuery bool;
  DECLARE geoQuery bool;
  DECLARE currentName varchar(1024);
  DECLARE north double DEFAULT 0;
  DECLARE east double DEFAULT 0;
  DECLARE south double DEFAULT 0;
  DECLARE west double DEFAULT 0;
  -- 
  SET nameQuery = searchNames IS NOT NULL;
  SET geoQuery = latitude IS NOT NULL AND longitude IS NOT NULL AND radius IS NOT NULL;
  SET north = latitude + radius / 111.04;
  SET south = latitude - radius / 111.04;
  SET east = longitude + radius / ABS(COS(RADIANS(latitude)) * 111.04);
  SET west = longitude - radius / ABS(COS(RADIANS(latitude)) * 111.04);
  IF (NOT nameQuery AND NOT geoQuery) THEN 
    CALL raise_error;
  END IF;
  -- two tables with same content; necessary because MySQL does not allow to re-use one table
  -- within a stored procedure
  CREATE TEMPORARY TABLE `tmp1` (`query` varchar(1024) CHARACTER SET utf8) ENGINE MEMORY;
  CREATE TEMPORARY TABLE `tmp2` (`query` varchar(1024) CHARACTER SET utf8) ENGINE MEMORY;
  -- split up the comma-separated query locations and store them in temporary table
  WHILE (CHAR_LENGTH(searchNames) > 0) DO
    IF (LOCATE(',', searchNames)) THEN
      -- head: take first element
      SET currentName = (SELECT SUBSTRING_INDEX(searchNames, ',', 1)); 
      -- tail: remaining elements
      SET searchNames = (SELECT SUBSTRING(searchNames, char_length(currentName) + 2));
    ELSE
      SET currentName = searchNames;
      SET searchNames = '';
    END IF;
    INSERT INTO `tmp1` VALUES(currentName);
    INSERT INTO `tmp2` VALUES(currentName);
  END WHILE;
  -- if no name was given, a dummy string is inserted in the temporary table, this is because we have 
  -- to use this for joining later (LEFT JOIN is not an option, because it performs terribly in this case)
  IF (NOT nameQuery) THEN
    INSERT INTO `tmp1` VALUES(CONCAT('radius(',latitude,', ',longitude,', ',radius,')'));
  END IF;
  -- the query; important is to include the actual search string as first column, so that we
  -- can associate this later
  SELECT 
    ids.query, 
    l.* , 
    lan.*, 
    -- concatenate all alternative name rows of a location into one column, use # and , as separators
    GROUP_CONCAT(alternativeName, '', '#', IFNULL(`language`,'')) AS `alternatives`,
    -- if we do a geo query, calculate distance, else just assume a distance of zero
    IF (geoQuery, distance(latitude, longitude, l.`latitude`, l.`longitude`), 0) as `distance`
  FROM `locations` l JOIN
  (SELECT `id`, `query` FROM `tmp1`, `locations` l2
    WHERE 
      (NOT nameQuery OR `query` = l2.`name`) AND 
      (NOT geoQuery OR (l2.`latitude` BETWEEN south AND north AND l2.`longitude` BETWEEN west AND east))
  UNION
  SELECT `locationId` AS id, `query` 
    FROM `tmp2`, `location_alternative_names` 
    WHERE 
      (NOT nameQuery OR (`query` = alternativeName AND (`language` IS NULL OR FIND_IN_SET(`language`, searchLanguages) > 0)))
  ) AS ids
  ON l.`id` = ids.id LEFT JOIN `location_alternative_names` lan on l.`id` = lan.`locationId`
  GROUP BY `id`, `query`
  HAVING IF (geoQuery, `distance` <= radius, true) 
  ORDER BY `distance`;
  DROP TEMPORARY TABLE IF EXISTS `tmp1`;
  DROP TEMPORARY TABLE IF EXISTS `tmp2`;
END */;;

/*!50003 SET SESSION SQL_MODE=@OLD_SQL_MODE */;;
DELIMITER ;

--
-- Dumping routines (FUNCTION) for database 'locations'
--
DELIMITER ;;

# Dump of FUNCTION distance
# ------------------------------------------------------------

/*!50003 DROP FUNCTION IF EXISTS `distance` */;;
/*!50003 SET SESSION SQL_MODE=""*/;;
/*!50003 CREATE*/ /*!50020 DEFINER=`root`@`localhost`*/ /*!50003 FUNCTION `distance`(lat1 double, lng1 double, lat2 double, lng2 double) RETURNS double
    DETERMINISTIC
BEGIN     
  RETURN 2 * 6375 * ASIN( 
    SQRT( 
      POWER(SIN(RADIANS(lat2 - lat1)/2), 2) + 
      COS(RADIANS(lat1)) * 
      COS(RADIANS(lat2)) * 
      POWER(SIN(RADIANS(lng2 - lng1)/2), 2)
    )
  );
END */;;

/*!50003 SET SESSION SQL_MODE=@OLD_SQL_MODE */;;
DELIMITER ;

/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
