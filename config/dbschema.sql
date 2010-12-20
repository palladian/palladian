-- phpMyAdmin SQL Dump
-- version 3.2.4
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 30. November 2010 um 13:18
-- Server Version: 5.1.41
-- PHP-Version: 5.3.1

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Datenbank: `tudiirdb`
--

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `answers`
--

CREATE TABLE IF NOT EXISTS `answers` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `answer` text NOT NULL,
  `questionID` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `questionID` (`questionID`),
  FULLTEXT KEY `answer` (`answer`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT AUTO_INCREMENT=24232 ;

--
-- Daten für Tabelle `answers`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `api_log`
--

CREATE TABLE IF NOT EXISTS `api_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `methodID` int(10) unsigned DEFAULT NULL,
  `appID` varchar(50) DEFAULT NULL,
  `appKey` varchar(100) DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `api_log`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `api_methods`
--

CREATE TABLE IF NOT EXISTS `api_methods` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `methodCode` varchar(50) DEFAULT NULL,
  `methodName` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `api_methods`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `attributes`
--

CREATE TABLE IF NOT EXISTS `attributes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `trust` double(15,3) unsigned NOT NULL,
  `lastSearched` datetime DEFAULT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `attributes`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `attributes_concepts`
--

CREATE TABLE IF NOT EXISTS `attributes_concepts` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID` bigint(20) unsigned NOT NULL,
  `conceptID` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `attributeID` (`attributeID`,`conceptID`),
  KEY `conceptID` (`conceptID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `attributes_concepts`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `attributes_range`
--

CREATE TABLE IF NOT EXISTS `attributes_range` (
  `attributeID` int(11) NOT NULL,
  `type` enum('min','max','possible') NOT NULL,
  `conceptID` int(11) NOT NULL,
  `value` varchar(100) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Daten für Tabelle `attributes_range`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `attributes_sources`
--

CREATE TABLE IF NOT EXISTS `attributes_sources` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID` bigint(20) unsigned NOT NULL,
  `sourceID` bigint(20) unsigned NOT NULL,
  `extractionType` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `attributeID` (`attributeID`),
  KEY `sourceID` (`sourceID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `attributes_sources`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `attribute_synonyms`
--

CREATE TABLE IF NOT EXISTS `attribute_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID1` bigint(20) unsigned DEFAULT NULL,
  `attributeID2` bigint(20) unsigned DEFAULT NULL,
  `trust` double unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `attributeID1` (`attributeID1`),
  KEY `attributeID2` (`attributeID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `attribute_synonyms`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `bugs`
--

CREATE TABLE IF NOT EXISTS `bugs` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `description` text NOT NULL,
  `userID` bigint(12) NOT NULL,
  `reportedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `bugs`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `concepts`
--

CREATE TABLE IF NOT EXISTS `concepts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `domainID` int(11) unsigned DEFAULT NULL,
  `ontologyID` int(11) unsigned DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `lastSearched` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `domainID` (`domainID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `concepts`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `concept_synonyms`
--

CREATE TABLE IF NOT EXISTS `concept_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `conceptID1` int(11) unsigned DEFAULT NULL,
  `conceptID2` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `conceptID1` (`conceptID1`),
  KEY `conceptID2` (`conceptID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `concept_synonyms`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `domains`
--

CREATE TABLE IF NOT EXISTS `domains` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `domains`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `domain_synonyms`
--

CREATE TABLE IF NOT EXISTS `domain_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `domainID1` int(11) unsigned DEFAULT NULL,
  `domainID2` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `domainID1` (`domainID1`),
  KEY `domainID2` (`domainID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `domain_synonyms`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `entities`
--

CREATE TABLE IF NOT EXISTS `entities` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `conceptID` int(11) unsigned NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `trust` double(15,3) unsigned NOT NULL,
  `lastSearched` datetime DEFAULT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `voting` tinyint(1) NOT NULL DEFAULT '0',
  `class` tinyint(1) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `conceptID` (`conceptID`),
  KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `entities`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `entities_sources`
--

CREATE TABLE IF NOT EXISTS `entities_sources` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `entityID` bigint(20) unsigned DEFAULT NULL,
  `sourceID` bigint(20) unsigned DEFAULT NULL,
  `extractionType` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `entityID` (`entityID`),
  KEY `sourceID` (`sourceID`),
  KEY `extractionType` (`extractionType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `entities_sources`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `entity_trust_view`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`entity_trust_view` AS select `tudiirdb`.`entities`.`conceptID` AS `conceptID`,`tudiirdb`.`entities`.`name` AS `name`,floor(`tudiirdb`.`entities`.`trust`) AS `trust`,count(`tudiirdb`.`entities`.`trust`) AS `numberOfEntities` from `tudiirdb`.`entities` group by `tudiirdb`.`entities`.`conceptID`,floor(`tudiirdb`.`entities`.`trust`) having (`trust` > 0) order by floor(`tudiirdb`.`entities`.`trust`) desc;

--
-- Daten für Tabelle `entity_trust_view`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `entity_voting_view`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`entity_voting_view` AS select `tudiirdb`.`sources`.`id` AS `id`,`tudiirdb`.`entities_sources`.`entityID` AS `entityID`,`tudiirdb`.`sources`.`entityTrust` AS `entityTrust` from (`tudiirdb`.`sources` join `tudiirdb`.`entities_sources`) where (`tudiirdb`.`sources`.`id` = `tudiirdb`.`entities_sources`.`sourceID`);

--
-- Daten für Tabelle `entity_voting_view`
--


-- --------------------------------------------------------


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `facts`
--

CREATE TABLE IF NOT EXISTS `facts` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `entityID` bigint(20) unsigned DEFAULT NULL,
  `attributeID` bigint(20) unsigned DEFAULT NULL,
  `value` varchar(255) DEFAULT NULL,
  `trust` double(15,3) unsigned DEFAULT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `entityID` (`entityID`),
  KEY `attributeID` (`attributeID`),
  KEY `value` (`value`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `facts`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `facts_sources`
--

CREATE TABLE IF NOT EXISTS `facts_sources` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `factID` bigint(20) unsigned DEFAULT NULL,
  `sourceID` bigint(20) unsigned DEFAULT NULL,
  `extractionType` int(11) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `factID` (`factID`),
  KEY `sourceID` (`sourceID`),
  KEY `extractionType` (`extractionType`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `facts_sources`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feeds`
--

CREATE TABLE IF NOT EXISTS `feeds` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `feedUrl` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `siteUrl` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `format` tinyint(4) NOT NULL,
  `textType` tinyint(4) NOT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `language` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `checks` int(11) unsigned NOT NULL DEFAULT '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` int(11) unsigned NOT NULL DEFAULT '30' COMMENT 'time in minutes until it is expected to find at least one new entry in the feed',
  `maxCheckInterval` int(11) unsigned NOT NULL DEFAULT '60' COMMENT 'time in minutes until it is expected to find only new but one new entries in the feed',
  `lastHeadlines` longtext COLLATE utf8_unicode_ci NOT NULL COMMENT 'a list of headlines that were found at the last check',
  `unreachableCount` int(11) unsigned NOT NULL DEFAULT '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  `lastFeedEntry` timestamp NULL DEFAULT NULL COMMENT 'timestamp of the last feed entry found in this feed',
  `activityPattern` int(11) NOT NULL DEFAULT '-1' COMMENT 'update class of the feed',
  `supportsLMS` tinyint(1) DEFAULT NULL,
  `supportsETag` tinyint(1) DEFAULT NULL,
  `conditionalGetResponseSize` int(11) DEFAULT NULL,
  `lastPollTime` timestamp NULL DEFAULT NULL,
  `lastETag` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `feeds`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feeds_post_distribution`
--

CREATE TABLE IF NOT EXISTS `feeds_post_distribution` (
  `feedID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `minuteOfDay` int(10) unsigned NOT NULL DEFAULT '0',
  `posts` int(10) unsigned DEFAULT NULL,
  `chances` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`feedID`,`minuteOfDay`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `feeds_post_distribution`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_entries`
--

CREATE TABLE IF NOT EXISTS `feed_entries` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `feedId` int(10) unsigned NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `link` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `rawId` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `published` datetime DEFAULT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `text` text COLLATE utf8_unicode_ci,
  `pageText` text COLLATE utf8_unicode_ci COMMENT 'text which we scraped from the corresponding page',
  PRIMARY KEY (`id`),
  UNIQUE KEY `feedId_rawId_unique` (`feedId`,`rawId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `feed_entries`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_entries_tags`
--

CREATE TABLE IF NOT EXISTS `feed_entries_tags` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name_unique` (`name`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `feed_entries_tags`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_entry_tag`
--

CREATE TABLE IF NOT EXISTS `feed_entry_tag` (
  `entryId` int(10) unsigned NOT NULL,
  `tagId` int(10) NOT NULL,
  `weight` float unsigned NOT NULL,
  PRIMARY KEY (`entryId`,`tagId`),
  KEY `tagId_index` (`tagId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Daten für Tabelle `feed_entry_tag`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_evaluation_polls`
--

CREATE TABLE IF NOT EXISTS `feed_evaluation_polls` (
  `feedID` int(10) unsigned NOT NULL,
  `numberOfPoll` int(10) unsigned NOT NULL COMMENT 'how often has this feed been polled (retrieved and read)',
  `activityPattern` int(11) NOT NULL COMMENT 'activity pattern of the feed',
  `conditionalGetResponseSize` int(11) DEFAULT NULL COMMENT 'the size of the HTTP 304 response in Bytes, (if there is no new entry)',
  `sizeOfPoll` float NOT NULL COMMENT 'the amount of bytes that have been downloadad',
  `pollTimestamp` bigint(20) unsigned NOT NULL COMMENT 'the feed has been pooled at this timestamp',
  `checkInterval` float unsigned DEFAULT NULL COMMENT 'time in minutes we waited betwen last and this check',
  `newWindowItems` float NOT NULL COMMENT 'number of new items in the window',
  `missedItems` int(10) NOT NULL COMMENT 'the number of new items we missed because there more new items since the last poll than fit into the window',
  `windowSize` int(10) unsigned NOT NULL COMMENT 'the current size of the feed''s window (number of items found)',
  `cumulatedDelay` double DEFAULT NULL COMMENT 'cumulated delay in seconds, adds absolute delay of polls that were too early and too late',
  `cumulatedLateDelay` double DEFAULT NULL COMMENT 'cumulated delay in seconds, adds absolute delay of polls that were too late',
  `timeliness` double DEFAULT NULL COMMENT 'averaged over all new and missed items in the poll including early polls, NULL if no new item has been discovered (only for evaluation mode MIN interesting)',
  `timelinessLate` double DEFAULT NULL COMMENT 'averaged over all new and missed items in the poll, NULL if no new item has been discovered (only for evaluation mode MIN interesting)'
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Daten für Tabelle `feed_evaluation_polls`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_evaluation_update_intervals`
--

CREATE TABLE IF NOT EXISTS `feed_evaluation_update_intervals` (
  `id` int(10) unsigned NOT NULL COMMENT 'feedID',
  `updateClass` int(11) NOT NULL COMMENT 'the feeds update class (activity pattern)',
  `averageEntriesPerDay` double NOT NULL COMMENT 'the average number of new entries per day',
  `medianItemInterval` bigint(20) NOT NULL COMMENT 'the feed''s median item interval in milliseconds (several items may be updated at the same time)',
  `averageUpdateInterval` varchar(25) NOT NULL COMMENT 'the feeds average update interval in milliseconds (one update may contain several items)',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Daten für Tabelle `feed_evaluation_update_intervals`
--

CREATE TABLE `events` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(1024) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `title` varchar(1024) CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `text` text CHARACTER SET utf8 COLLATE utf8_unicode_ci NOT NULL,
  `who` varchar(512) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `what` varchar(1024) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `where` varchar(512) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `when` varchar(512) CHARACTER SET utf8 COLLATE utf8_unicode_ci DEFAULT NULL,
  `why` text CHARACTER SET utf8 COLLATE utf8_unicode_ci,
  `how` text CHARACTER SET utf8 COLLATE utf8_unicode_ci,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ip2location`
--

CREATE TABLE IF NOT EXISTS `ip2location` (
  `ipStart` bigint(20) unsigned NOT NULL,
  `countryCode` char(2) DEFAULT NULL,
  `countryName` varchar(50) DEFAULT NULL,
  `regionCode` varchar(20) DEFAULT NULL,
  `regionName` varchar(50) DEFAULT NULL,
  `city` varchar(50) DEFAULT NULL,
  `zipCode` varchar(20) DEFAULT NULL,
  `latitude` double DEFAULT NULL,
  `longitude` double DEFAULT NULL,
  `metrocode` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`ipStart`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1

--
-- Daten für Tabelle `ip2location`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `live_status`
--

CREATE TABLE IF NOT EXISTS `live_status` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT COMMENT 'the id of the status',
  `percent` double unsigned NOT NULL DEFAULT '0' COMMENT 'the percentage that the current phase has progressed',
  `timeLeft` varchar(50) DEFAULT NULL COMMENT 'the time left until the next phase starts',
  `currentPhase` varchar(50) DEFAULT NULL COMMENT 'the name of the current phase',
  `currentAction` varchar(1000) DEFAULT NULL COMMENT 'detailed information about the current action in the current phase',
  `logExcerpt` text COMMENT 'a log excerpt',
  `moreText1` text COMMENT 'free text',
  `moreText2` text COMMENT 'free text',
  `downloadedBytes` bigint(20) unsigned DEFAULT '0' COMMENT 'the total number of downloaded bytes',
  `updatedAt` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'the timestamp of the entry',
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `live_status`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `ontology_listeners`
--

CREATE TABLE IF NOT EXISTS `ontology_listeners` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(100) NOT NULL,
  `concept` varchar(30) NOT NULL,
  `attribute` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `ontology_listeners`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `query_log`
--

CREATE TABLE IF NOT EXISTS `query_log` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `input` varchar(200) DEFAULT NULL,
  `class` enum('entity','question','api','other') DEFAULT NULL,
  `timestamp` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `query_log`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `questions`
--

CREATE TABLE IF NOT EXISTS `questions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sourceID` bigint(20) unsigned NOT NULL,
  `question` varchar(255) NOT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `question_3` (`question`),
  FULLTEXT KEY `question_2` (`question`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=4022 ;

--
-- Daten für Tabelle `questions`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `quotes`
--

CREATE TABLE IF NOT EXISTS `quotes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `quote` varchar(400) NOT NULL,
  `author` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `quote` (`quote`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `quotes`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rolepages`
--

CREATE TABLE IF NOT EXISTS `rolepages` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(500) DEFAULT NULL,
  `count` bigint(20) DEFAULT NULL,
  `conceptID` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `rolepages`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `rolepage_usages`
--

CREATE TABLE IF NOT EXISTS `rolepage_usages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `rolepageID` bigint(20) NOT NULL,
  `entityID` bigint(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `rolepage_usages`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `snippets`
--

CREATE TABLE IF NOT EXISTS `snippets` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `entityID` bigint(20) unsigned NOT NULL,
  `sourceID` bigint(20) unsigned NOT NULL,
  `text` text NOT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `entityID` (`entityID`),
  KEY `sourceID` (`sourceID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `snippets`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `sources`
--

CREATE TABLE IF NOT EXISTS `sources` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(500) DEFAULT NULL,
  `entityTrust` double unsigned NOT NULL DEFAULT '0.5',
  `voting` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `url` (`url`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `sources`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `sources_entities_view`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`sources_entities_view` AS select `tudiirdb`.`entities_sources`.`entityID` AS `entityID`,`tudiirdb`.`entities`.`voting` AS `entityVoting`,`tudiirdb`.`entities`.`trust` AS `entityTrust`,`tudiirdb`.`entities_sources`.`sourceID` AS `sourceID`,`tudiirdb`.`sources`.`voting` AS `sourceVoting`,`tudiirdb`.`sources`.`entityTrust` AS `sourceEntityTrust` from ((`tudiirdb`.`entities` join `tudiirdb`.`entities_sources`) join `tudiirdb`.`sources`) where ((`tudiirdb`.`entities`.`id` = `tudiirdb`.`entities_sources`.`entityID`) and (`tudiirdb`.`entities_sources`.`sourceID` = `tudiirdb`.`sources`.`id`));

--
-- Daten für Tabelle `sources_entities_view`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `source_ranking_features`
--

CREATE TABLE IF NOT EXISTS `source_ranking_features` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sourceId` bigint(20) unsigned NOT NULL,
  `service` tinyint(4) NOT NULL,
  `ranking` float NOT NULL,
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sourceId_service_unique` (`sourceId`,`service`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `source_ranking_features`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `source_voting_view`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`source_voting_view` AS select `tudiirdb`.`entities`.`id` AS `id`,`tudiirdb`.`entities_sources`.`sourceID` AS `sourceID`,`tudiirdb`.`entities`.`trust` AS `trust`,`tudiirdb`.`entities`.`voting` AS `voting` from (`tudiirdb`.`entities` join `tudiirdb`.`entities_sources`) where (`tudiirdb`.`entities`.`id` = `tudiirdb`.`entities_sources`.`entityID`);

--
-- Daten für Tabelle `source_voting_view`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `training_samples`
--

CREATE TABLE IF NOT EXISTS `training_samples` (
  `conceptID` int(11) unsigned NOT NULL,
  `entityID` bigint(20) unsigned NOT NULL,
  `class` tinyint(1) DEFAULT NULL,
  `test` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 if sample is used in testing set',
  PRIMARY KEY (`entityID`),
  KEY `conceptID` (`conceptID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Daten für Tabelle `training_samples`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `users_in`
--

CREATE TABLE IF NOT EXISTS `users_in` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `userID` bigint(12) unsigned NOT NULL,
  `inID` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `inID` (`inID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `users_in`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `webknox_development`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`webknox_development` AS select `tudiirdb`.`entities`.`conceptID` AS `conceptID`,`tudiirdb`.`entities`.`name` AS `name`,floor(`tudiirdb`.`entities`.`trust`) AS `trust`,count(`tudiirdb`.`entities`.`trust`) AS `numberOfEntities` from `tudiirdb`.`entities` group by `tudiirdb`.`entities`.`conceptID`,floor(`tudiirdb`.`entities`.`trust`) having (`trust` > 0) order by floor(`tudiirdb`.`entities`.`trust`) desc;

--
-- Daten für Tabelle `webknox_development`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `webknox_entity_assessment`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`webknox_entity_assessment` AS select `tudiirdb`.`entities`.`conceptID` AS `conceptID`,`tudiirdb`.`entities`.`name` AS `name`,floor(`tudiirdb`.`entities`.`trust`) AS `trust`,count(`tudiirdb`.`entities`.`trust`) AS `numberOfEntities` from `tudiirdb`.`entities` group by `tudiirdb`.`entities`.`conceptID`,floor(`tudiirdb`.`entities`.`trust`) having (`trust` > 0) order by floor(`tudiirdb`.`entities`.`trust`) desc;

--
-- Daten für Tabelle `webknox_entity_assessment`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `wishes`
--

CREATE TABLE IF NOT EXISTS `wishes` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `description` text NOT NULL,
  `userID` bigint(12) NOT NULL,
  `reportedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `wishes`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `wx_entity_assessment`
--

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `tudiirdb`.`wx_entity_assessment` AS select `tudiirdb`.`entities`.`conceptID` AS `conceptID`,`tudiirdb`.`entities`.`name` AS `name`,floor(`tudiirdb`.`entities`.`trust`) AS `trust`,count(`tudiirdb`.`entities`.`trust`) AS `numberOfEntities` from `tudiirdb`.`entities` group by `tudiirdb`.`entities`.`conceptID`,floor(`tudiirdb`.`entities`.`trust`) having (`trust` > 0) order by floor(`tudiirdb`.`entities`.`trust`) desc;

--
-- Daten für Tabelle `wx_entity_assessment`
--


/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
