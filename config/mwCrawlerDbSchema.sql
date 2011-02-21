/*
SQLyog Community v8.63 
MySQL - 5.1.36-community : Database - wiki_crawler
*********************************************************************
*/

/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`wiki_crawler` /*!40100 DEFAULT CHARACTER SET latin1 */;

USE `wiki_crawler`;

/*Table structure for table `links` */

CREATE TABLE `links` (
  `wikiID` int(10) unsigned NOT NULL COMMENT 'Unique ID of a Wiki',
  `pageIDSource` int(10) unsigned NOT NULL COMMENT 'pageID of the source page, defined by each wiki. ',
  `pageIDDest` int(10) unsigned NOT NULL COMMENT 'pageID of the destination page, defined by each wiki. ',
  KEY `links_ibfk_1` (`wikiID`,`pageIDSource`),
  KEY `links_ibfk_2` (`wikiID`,`pageIDDest`),
  CONSTRAINT `links_ibfk_1` FOREIGN KEY (`wikiID`, `pageIDSource`) REFERENCES `pages` (`wikiID`, `pageID`) ON DELETE CASCADE,
  CONSTRAINT `links_ibfk_2` FOREIGN KEY (`wikiID`, `pageIDDest`) REFERENCES `pages` (`wikiID`, `pageID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Table structure for table `namespaces` */

CREATE TABLE `namespaces` (
  `wikiID` int(10) unsigned NOT NULL COMMENT 'Unique ID of a Wiki',
  `namespaceID` smallint(6) NOT NULL COMMENT 'A namespace ID in a Wiki',
  `namespaceName` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT 'Name or short description of the namespace',
  `useForCrawling` tinyint(1) NOT NULL DEFAULT '1' COMMENT 'If true, include namespace into crawling',
  PRIMARY KEY (`wikiID`,`namespaceID`),
  CONSTRAINT `namespaces_ibfk_1` FOREIGN KEY (`wikiID`) REFERENCES `wikis` (`wikiID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Table structure for table `pages` */

CREATE TABLE `pages` (
  `wikiID` int(10) unsigned NOT NULL COMMENT 'Unique ID of a Wiki',
  `pageID` int(10) unsigned NOT NULL COMMENT 'pageID, defined by each wiki. ',
  `pageTitle` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'The name of the page, unique per wiki',
  `namespaceID` smallint(6) DEFAULT NULL COMMENT 'The namespace ID of this page',
  `sourceDynamics` float DEFAULT NULL COMMENT 'calculated from all timestamps of all revisions',
  `pageContent` mediumtext COLLATE utf8_bin COMMENT 'The page''s rendered content as shown in a browser, Wiki markup is not contained ',
  `revisionID` bigint(20) DEFAULT NULL COMMENT 'The Wiki''s revision id the pageContent has.',
  `nextCheck` datetime DEFAULT NULL COMMENT 'The timestamp to check this page for new revisions. Timestamp written by predictor.',
  `fullURL` mediumtext COLLATE utf8_bin COMMENT 'The absolute path to the page as returned by API fullurl.',
  PRIMARY KEY (`wikiID`,`pageID`),
  KEY `wikiID` (`wikiID`,`namespaceID`),
  CONSTRAINT `pages_ibfk_1` FOREIGN KEY (`wikiID`, `namespaceID`) REFERENCES `namespaces` (`wikiID`, `namespaceID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Table structure for table `revisions` */

CREATE TABLE `revisions` (
  `wikiID` int(10) unsigned NOT NULL COMMENT 'UNIQUE ID of a Wiki',
  `pageID` int(10) unsigned NOT NULL COMMENT 'The Wiki''s pageID',
  `revisionID` bigint(20) unsigned NOT NULL COMMENT 'The unique revision id of this revision',
  `timestamp` datetime NOT NULL COMMENT 'timestemp of a revision',
  `author` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'The name of the author (user) that created the revisions',
  PRIMARY KEY (`wikiID`,`pageID`,`revisionID`),
  CONSTRAINT `revisions_ibfk_1` FOREIGN KEY (`wikiID`, `pageID`) REFERENCES `pages` (`wikiID`, `pageID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*Table structure for table `wikis` */

CREATE TABLE `wikis` (
  `wikiID` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'Unique ID of a Wiki',
  `wikiName` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'Unique name of the Wiki to distinguish between them by name',
  `wikiURL` varchar(255) COLLATE utf8_bin NOT NULL COMMENT 'Path to the Wiki, like http://en.wikipedia.org/',
  `pathToApi` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT 'Path to API, relative from wiki_url, without page name of the api, like /w/ in wikipedia (resulting path is http://de.wikipedia.org/w/)',
  `pathToContent` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT 'Path to wiki pages, relative from wiki_url, like /wiki/ as used in wikipedia (resulting path is http://de.wikipedia.org/wiki/)',
  `lastCheckNewPages` datetime DEFAULT NULL COMMENT 'The timestamp the wiki has been checked for new pages the last time',
  `crawler_username` varchar(30) COLLATE utf8_bin DEFAULT NULL COMMENT 'The username to log in the wiki, empty if no login is required for reading ',
  `crawler_password` varchar(30) COLLATE utf8_bin DEFAULT NULL COMMENT 'The password to log in the wiki, empty if no login is required for reading. Caution! Password in plain text! ',
  PRIMARY KEY (`wikiID`),
  UNIQUE KEY `wikiName_idx` (`wikiName`),
  UNIQUE KEY `wikiURL_idx` (`wikiURL`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
