-- phpMyAdmin SQL Dump
-- version 2.11.7.1
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 01. Juni 2010 um 22:06
-- Server Version: 5.0.41
-- PHP-Version: 5.2.6
-- 
-- Database: `tudiirdb`
-- 

-- --------------------------------------------------------

CREATE DATABASE IF NOT EXISTS tudiirdb;
USE tudiirdb;

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";

-- --------------------------------------------------------

--
-- Table structure for table `answers`
--

DROP TABLE IF EXISTS `answers`;
CREATE TABLE IF NOT EXISTS `answers` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `answer` text NOT NULL,
  `questionID` bigint(20) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `questionID` (`questionID`),
  FULLTEXT KEY `answer` (`answer`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT AUTO_INCREMENT=24232 ;

-- --------------------------------------------------------

--
-- Table structure for table `attributes`
--

DROP TABLE IF EXISTS `attributes`;
CREATE TABLE IF NOT EXISTS `attributes` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  `trust` double(15,3) unsigned NOT NULL,
  `lastSearched` datetime DEFAULT NULL,
  `extractedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `attributes_range`
--

CREATE TABLE IF NOT EXISTS `attributes_range` (
  `attributeID` int(11) NOT NULL,
  `type` enum('min','max','possible') NOT NULL,
  `conceptID` int(11) NOT NULL,
  `value` varchar(100) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `attributes_concepts`
--

DROP TABLE IF EXISTS `attributes_concepts`;
CREATE TABLE IF NOT EXISTS `attributes_concepts` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID` bigint(20) unsigned NOT NULL,
  `conceptID` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `attributeID` (`attributeID`,`conceptID`),
  KEY `conceptID` (`conceptID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `attributes_sources`
--

DROP TABLE IF EXISTS `attributes_sources`;
CREATE TABLE IF NOT EXISTS `attributes_sources` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID` bigint(20) unsigned NOT NULL,
  `sourceID` bigint(20) unsigned NOT NULL,
  `extractionType` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `attributeID` (`attributeID`),
  KEY `sourceID` (`sourceID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `attribute_synonyms`
--

DROP TABLE IF EXISTS `attribute_synonyms`;
CREATE TABLE IF NOT EXISTS `attribute_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `attributeID1` bigint(20) unsigned DEFAULT NULL,
  `attributeID2` bigint(20) unsigned DEFAULT NULL,
  `trust` double unsigned NOT NULL DEFAULT '1',
  PRIMARY KEY (`id`),
  KEY `attributeID1` (`attributeID1`),
  KEY `attributeID2` (`attributeID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `bugs`
--

DROP TABLE IF EXISTS `bugs`;
CREATE TABLE IF NOT EXISTS `bugs` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `description` text NOT NULL,
  `userID` bigint(12) NOT NULL,
  `reportedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `concepts`
--

DROP TABLE IF EXISTS `concepts`;
CREATE TABLE IF NOT EXISTS `concepts` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `domainID` int(11) unsigned DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `lastSearched` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `domainID` (`domainID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `concept_synonyms`
--

DROP TABLE IF EXISTS `concept_synonyms`;
CREATE TABLE IF NOT EXISTS `concept_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `conceptID1` int(11) unsigned DEFAULT NULL,
  `conceptID2` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `conceptID1` (`conceptID1`),
  KEY `conceptID2` (`conceptID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `domains`
--

DROP TABLE IF EXISTS `domains`;
CREATE TABLE IF NOT EXISTS `domains` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `domain_synonyms`
--

DROP TABLE IF EXISTS `domain_synonyms`;
CREATE TABLE IF NOT EXISTS `domain_synonyms` (
  `id` int(11) unsigned NOT NULL AUTO_INCREMENT,
  `domainID1` int(11) unsigned DEFAULT NULL,
  `domainID2` int(11) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `domainID1` (`domainID1`),
  KEY `domainID2` (`domainID2`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `entities`
--

DROP TABLE IF EXISTS `entities`;
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

-- --------------------------------------------------------

--
-- Table structure for table `entities_sources`
--

DROP TABLE IF EXISTS `entities_sources`;
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

-- --------------------------------------------------------

--
-- Stand-in structure for view `entity_trust_view`
--
DROP VIEW IF EXISTS `entity_trust_view`;
CREATE TABLE IF NOT EXISTS `entity_trust_view` (
`conceptID` int(11) unsigned
,`name` varchar(255)
,`trust` double(17,0)
,`numberOfEntities` bigint(21)
);
-- --------------------------------------------------------

--
-- Stand-in structure for view `entity_voting_view`
--
DROP VIEW IF EXISTS `entity_voting_view`;
CREATE TABLE IF NOT EXISTS `entity_voting_view` (
`id` bigint(20) unsigned
,`entityID` bigint(20) unsigned
,`entityTrust` double unsigned
);
-- --------------------------------------------------------

--
-- Table structure for table `evaluations`
--

DROP VIEW IF EXISTS `evaluations`;
CREATE TABLE IF NOT EXISTS `evaluations` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `snippetID` bigint(20) NOT NULL DEFAULT '0',
  `evaluator` varchar(255) NOT NULL DEFAULT '',
  `relevancy` int(11) NOT NULL DEFAULT '0',
  `interestingness` int(11) NOT NULL DEFAULT '0',
  `teaser` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `entityID` (`snippetID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `extraction_status`
--

DROP TABLE IF EXISTS `extraction_status`;
CREATE TABLE IF NOT EXISTS `extraction_status` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `phase` int(11) NOT NULL DEFAULT '-1',
  `progress` smallint(5) unsigned NOT NULL DEFAULT '0',
  `logExcerpt` text NOT NULL,
  `downloadedBytes` bigint(20) unsigned NOT NULL DEFAULT '0',
  `updatedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 ROW_FORMAT=COMPACT AUTO_INCREMENT=1 ;

INSERT INTO `tudiirdb`.`extraction_status` SET phase = 1;

-- --------------------------------------------------------

--
-- Table structure for table `facts`
--

DROP TABLE IF EXISTS `facts`;
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

-- --------------------------------------------------------

--
-- Table structure for table `facts_sources`
--

DROP TABLE IF EXISTS `facts_sources`;
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

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_entries`
--

DROP TABLE IF EXISTS `feed_entries`;
CREATE TABLE IF NOT EXISTS `feed_entries` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `feedId` int(10) unsigned NOT NULL,
  `title` varchar(255) collate utf8_unicode_ci default NULL,
  `link` varchar(255) collate utf8_unicode_ci default NULL,
  `rawId` varchar(255) collate utf8_unicode_ci NOT NULL,
  `published` datetime default NULL,
  `added` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `text` text collate utf8_unicode_ci,
  `pageText` text collate utf8_unicode_ci COMMENT 'text which we scraped from the corresponding page',
  `tags` text collate utf8_unicode_ci COMMENT 'tags as comma separated list',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `feedId_rawId_unique` (`feedId`,`rawId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feeds`
--

DROP TABLE IF EXISTS `feeds`;
CREATE TABLE `feeds` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `feedUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `siteUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `title` varchar(255) collate utf8_unicode_ci default NULL,
  `format` tinyint(4) NOT NULL,
  `textType` tinyint(4) NOT NULL,
  `added` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `language` varchar(255) collate utf8_unicode_ci default NULL,
  `checks` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `maxCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `lastHeadlines` text collate utf8_unicode_ci NOT NULL COMMENT 'a list of headlines that were found at the last check',
  `unreachableCount` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=MyISAM AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- --------------------------------------------------------

DROP TABLE IF EXISTS `feeds_fixed_learned`;
CREATE TABLE `feeds_fixed_learned` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `feedUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `siteUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `title` varchar(255) collate utf8_unicode_ci default NULL,
  `format` tinyint(4) NOT NULL,
  `textType` tinyint(4) NOT NULL,
  `added` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `language` varchar(255) collate utf8_unicode_ci default NULL,
  `checks` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `maxCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `lastHeadlines` text collate utf8_unicode_ci NOT NULL COMMENT 'a list of headlines that were found at the last check',
  `unreachableCount` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=MyISAM AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- --------------------------------------------------------

DROP TABLE IF EXISTS `feeds_adaptive`;
CREATE TABLE `feeds_adaptive` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `feedUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `siteUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `title` varchar(255) collate utf8_unicode_ci default NULL,
  `format` tinyint(4) NOT NULL,
  `textType` tinyint(4) NOT NULL,
  `added` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `language` varchar(255) collate utf8_unicode_ci default NULL,
  `checks` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `maxCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `lastHeadlines` text collate utf8_unicode_ci NOT NULL COMMENT 'a list of headlines that were found at the last check',
  `unreachableCount` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=MyISAM AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- --------------------------------------------------------

DROP TABLE IF EXISTS `feeds_probabilistic`;
CREATE TABLE `feeds_probabilistic` (
  `id` int(10) unsigned NOT NULL auto_increment,
  `feedUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `siteUrl` varchar(255) collate utf8_unicode_ci NOT NULL,
  `title` varchar(255) collate utf8_unicode_ci default NULL,
  `format` tinyint(4) NOT NULL,
  `textType` tinyint(4) NOT NULL,
  `added` timestamp NOT NULL default CURRENT_TIMESTAMP,
  `language` varchar(255) collate utf8_unicode_ci default NULL,
  `checks` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `maxCheckInterval` int(11) unsigned NOT NULL default '60' COMMENT 'time in minutes between two consecutive checks',
  `lastHeadlines` text collate utf8_unicode_ci NOT NULL COMMENT 'a list of headlines that were found at the last check',
  `unreachableCount` int(11) unsigned NOT NULL default '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  PRIMARY KEY  (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=MyISAM AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- --------------------------------------------------------

DROP TABLE IF EXISTS `feeds_post_distribution`;
CREATE TABLE `feeds_post_distribution` (
  `feedID` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `minuteOfDay` int(10) unsigned,
  `posts` int(10) unsigned,
  `chances` int(10) unsigned,
   PRIMARY KEY  (`feedID`)
) ENGINE=MyISAM AUTO_INCREMENT=1763 DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
-- --------------------------------------------------------

--
-- Table structure for table `ontology_listeners`
--

DROP TABLE IF EXISTS `ontology_listeners`;
CREATE TABLE IF NOT EXISTS `ontology_listeners` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(100) NOT NULL,
  `concept` varchar(30) NOT NULL,
  `attribute` varchar(30) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Table structure for table `questions`
--

DROP TABLE IF EXISTS `questions`;
CREATE TABLE IF NOT EXISTS `questions` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `sourceID` bigint(20) unsigned NOT NULL,
  `question` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `question_3` (`question`),
  FULLTEXT KEY `question_2` (`question`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=4022 ;

-- --------------------------------------------------------

--
-- Table structure for table `quotes`
--

DROP TABLE IF EXISTS `quotes`;
CREATE TABLE IF NOT EXISTS `quotes` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `quote` varchar(400) NOT NULL,
  `author` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `quote` (`quote`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;


-- 
-- Table structure for table `searches`
-- 

DROP TABLE IF EXISTS `searches`;
CREATE TABLE IF NOT EXISTS `searches` (
  `id` bigint(20) unsigned NOT NULL auto_increment,
  `query` varchar(255) NOT NULL,
  `time` timestamp NOT NULL default CURRENT_TIMESTAMP,
  PRIMARY KEY  (`id`)
) ENGINE=MyISAM  DEFAULT CHARSET=latin1 AUTO_INCREMENT=60 ;
-- --------------------------------------------------------

--
-- Table structure for table `snippets`
--

DROP TABLE IF EXISTS `snippets`;
CREATE TABLE IF NOT EXISTS `snippets` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `entityID` bigint(20) unsigned NOT NULL,
  `sourceID` bigint(20) unsigned NOT NULL,
  `text` text NOT NULL,
  `extractedAt` datetime NOT NULL,
  `regressionRank` float DEFAULT NULL,
  `f_AggregatedRank` float NOT NULL,
  `f_SearchEngine1` float NOT NULL,
  `f_SearchEngine2` float NOT NULL,
  `f_SearchEngine3` float NOT NULL,
  `f_SearchEngine4` float NOT NULL,
  `f_SearchEngine5` float NOT NULL,
  `f_SearchEngine6` float NOT NULL,
  `f_SearchEngine7` float NOT NULL,
  `f_SearchEngine8` float NOT NULL,
  `f_SearchEngine9` float NOT NULL,
  `f_PageRank` float NOT NULL,
  `f_TopLevelDomain` float NOT NULL,
  `f_MainContentCharCount` float NOT NULL,
  `f_CharacterCount` float NOT NULL,
  `f_LetterNumberPercentage` float NOT NULL,
  `f_SyllablesPerWordCount` float NOT NULL,
  `f_WordCount` float NOT NULL,
  `f_UniqueWordCount` float NOT NULL,
  `f_ComplexWordPercentage` float NOT NULL,
  `f_SentenceCount` float NOT NULL,
  `f_WordsPerSentenceCount` float NOT NULL,
  `f_FleschKincaidReadingEase` float NOT NULL,
  `f_GunningFogScore` float NOT NULL,
  `f_FleschKincaidGradeLevel` float NOT NULL,
  `f_AutomatedReadabilityIndex` float NOT NULL,
  `f_ColemanLiauIndex` float NOT NULL,
  `f_SmogIndex` float NOT NULL,
  `f_ContainsProperNoun` float NOT NULL,
  `f_CapitalizedWordCount` float NOT NULL,
  `f_StartsWithEntity` float NOT NULL,
  `f_RelatedEntityCount` float NOT NULL,
  `source` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `entityID` (`entityID`),
  KEY `sourceID` (`sourceID`)
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

--
-- Table structure for table `sources`
--

DROP TABLE IF EXISTS `sources`;
CREATE TABLE IF NOT EXISTS `sources` (
  `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(500) DEFAULT NULL,
  `entityTrust` double unsigned NOT NULL DEFAULT '0.5',
  `voting` tinyint(1) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `url` (`url`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `sources_entities_view`
--
DROP VIEW IF EXISTS `sources_entities_view`;
CREATE TABLE IF NOT EXISTS `sources_entities_view` (
`entityID` bigint(20) unsigned
,`entityVoting` tinyint(1)
,`entityTrust` double(15,3) unsigned
,`sourceID` bigint(20) unsigned
,`sourceVoting` tinyint(1)
,`sourceEntityTrust` double unsigned
);
-- --------------------------------------------------------

--
-- Stand-in structure for view `source_voting_view`
--
DROP VIEW IF EXISTS `source_voting_view`;
CREATE TABLE IF NOT EXISTS `source_voting_view` (
`id` bigint(20) unsigned
,`sourceID` bigint(20) unsigned
,`trust` double(15,3) unsigned
,`voting` tinyint(1)
);
-- --------------------------------------------------------

--
-- Table structure for table `training_samples`
--

DROP TABLE IF EXISTS `training_samples`;
CREATE TABLE IF NOT EXISTS `training_samples` (
  `conceptID` int(11) unsigned NOT NULL,
  `entityID` bigint(20) unsigned NOT NULL,
  `class` tinyint(1) DEFAULT NULL,
  `test` tinyint(1) NOT NULL DEFAULT '0' COMMENT '1 if sample is used in testing set',
  PRIMARY KEY (`entityID`),
  KEY `conceptID` (`conceptID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Table structure for table `users_in`
--

DROP TABLE IF EXISTS `users_in`;
CREATE TABLE IF NOT EXISTS `users_in` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `userID` bigint(12) unsigned NOT NULL,
  `inID` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `inID` (`inID`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

CREATE TABLE `source_ranking_features` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `sourceId` bigint(20) unsigned NOT NULL,
  `service` tinyint(4) NOT NULL,
  `ranking` float NOT NULL,
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `sourceId_service_unique` (`sourceId`,`service`)
) ENGINE=MyISAM  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `webknox_development`
--
DROP VIEW IF EXISTS `webknox_development`;
CREATE TABLE IF NOT EXISTS `webknox_development` (
`conceptID` int(11) unsigned
,`name` varchar(255)
,`trust` double(17,0)
,`numberOfEntities` bigint(21)
);
-- --------------------------------------------------------

--
-- Stand-in structure for view `webknox_entity_assessment`
--
DROP VIEW IF EXISTS `webknox_entity_assessment`;
CREATE TABLE IF NOT EXISTS `webknox_entity_assessment` (
`conceptID` int(11) unsigned
,`name` varchar(255)
,`trust` double(17,0)
,`numberOfEntities` bigint(21)
);
-- --------------------------------------------------------

--
-- Table structure for table `wishes`
--

DROP TABLE IF EXISTS `wishes`;
CREATE TABLE IF NOT EXISTS `wishes` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `description` text NOT NULL,
  `userID` bigint(12) NOT NULL,
  `reportedAt` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Stand-in structure for view `wx_entity_assessment`
--
DROP VIEW IF EXISTS `wx_entity_assessment`;
CREATE TABLE IF NOT EXISTS `wx_entity_assessment` (
`conceptID` int(11) unsigned
,`name` varchar(255)
,`trust` double(17,0)
,`numberOfEntities` bigint(21)
);
-- --------------------------------------------------------

--
-- Structure for view `entity_trust_view`
--
DROP TABLE IF EXISTS `entity_trust_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `entity_trust_view` AS select `entities`.`conceptID` AS `conceptID`,`entities`.`name` AS `name`,floor(`entities`.`trust`) AS `trust`,count(`entities`.`trust`) AS `numberOfEntities` from `entities` group by `entities`.`conceptID`,floor(`entities`.`trust`) having (`trust` > 0) order by floor(`entities`.`trust`) desc;

-- --------------------------------------------------------

--
-- Structure for view `entity_voting_view`
--
DROP TABLE IF EXISTS `entity_voting_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `entity_voting_view` AS select `sources`.`id` AS `id`,`entities_sources`.`entityID` AS `entityID`,`sources`.`entityTrust` AS `entityTrust` from (`sources` join `entities_sources`) where (`sources`.`id` = `entities_sources`.`sourceID`);

-- --------------------------------------------------------

--
-- Structure for view `sources_entities_view`
--
DROP TABLE IF EXISTS `sources_entities_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `sources_entities_view` AS select `entities_sources`.`entityID` AS `entityID`,`entities`.`voting` AS `entityVoting`,`entities`.`trust` AS `entityTrust`,`entities_sources`.`sourceID` AS `sourceID`,`sources`.`voting` AS `sourceVoting`,`sources`.`entityTrust` AS `sourceEntityTrust` from ((`entities` join `entities_sources`) join `sources`) where ((`entities`.`id` = `entities_sources`.`entityID`) and (`entities_sources`.`sourceID` = `sources`.`id`));

-- --------------------------------------------------------

--
-- Structure for view `source_voting_view`
--
DROP TABLE IF EXISTS `source_voting_view`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `source_voting_view` AS select `entities`.`id` AS `id`,`entities_sources`.`sourceID` AS `sourceID`,`entities`.`trust` AS `trust`,`entities`.`voting` AS `voting` from (`entities` join `entities_sources`) where (`entities`.`id` = `entities_sources`.`entityID`);

-- --------------------------------------------------------

--
-- Structure for view `webknox_development`
--
DROP TABLE IF EXISTS `webknox_development`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `webknox_development` AS select `entities`.`conceptID` AS `conceptID`,`entities`.`name` AS `name`,floor(`entities`.`trust`) AS `trust`,count(`entities`.`trust`) AS `numberOfEntities` from `entities` group by `entities`.`conceptID`,floor(`entities`.`trust`) having (`trust` > 0) order by floor(`entities`.`trust`) desc;

-- --------------------------------------------------------

--
-- Structure for view `webknox_entity_assessment`
--
DROP TABLE IF EXISTS `webknox_entity_assessment`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `webknox_entity_assessment` AS select `entities`.`conceptID` AS `conceptID`,`entities`.`name` AS `name`,floor(`entities`.`trust`) AS `trust`,count(`entities`.`trust`) AS `numberOfEntities` from `entities` group by `entities`.`conceptID`,floor(`entities`.`trust`) having (`trust` > 0) order by floor(`entities`.`trust`) desc;

-- --------------------------------------------------------

--
-- Structure for view `wx_entity_assessment`
--
DROP TABLE IF EXISTS `wx_entity_assessment`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `wx_entity_assessment` AS select `entities`.`conceptID` AS `conceptID`,`entities`.`name` AS `name`,floor(`entities`.`trust`) AS `trust`,count(`entities`.`trust`) AS `numberOfEntities` from `entities` group by `entities`.`conceptID`,floor(`entities`.`trust`) having (`trust` > 0) order by floor(`entities`.`trust`) desc;

DELIMITER $$
--
-- Procedures
--
DROP PROCEDURE IF EXISTS `entity_trust_assignment_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `entity_trust_assignment_procedure`()
    DETERMINISTIC
BEGIN

UPDATE `entities` es0 SET trust = (SELECT (SUM(entityTrust) * POW(2,(SELECT COUNT(DISTINCT extractionType) 
    FROM `entities_sources` es1
    WHERE es1.entityID = es0.`id`))) as trust 
  FROM `entity_voting_view` evv
  WHERE entityID = es0.`id`
  GROUP BY entityID);
  
END$$

DROP PROCEDURE IF EXISTS `entity_voting_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `entity_voting_procedure`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE a INT;
  DECLARE b DOUBLE;
  DECLARE cur1 CURSOR FOR SELECT id,trust FROM `entities` WHERE voting = 0 AND trust > 0;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;
  
  OPEN cur1;
  START TRANSACTION;	
  REPEAT
    FETCH cur1 INTO a,b;
    IF NOT done THEN
       UPDATE `entity_voting_view` SET entityTrust = entityTrust + b WHERE entityID = a;
       UPDATE `entities` SET `entities`.voting = 1 WHERE `entities`.id = a;
    END IF;
  UNTIL done END REPEAT;
  COMMIT;

  CLOSE cur1;
END$$

DROP PROCEDURE IF EXISTS `final_source_voting_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `final_source_voting_procedure`()
BEGIN
DECLARE done INT DEFAULT 0;
DECLARE a INT;
DECLARE b DOUBLE;
DECLARE cur1 CURSOR FOR SELECT entities.id,SUM(entityTrust) FROM entities,entities_sources,sources WHERE entities.id = entities_sources.entityID AND entities_sources.sourceID = sources.id GROUP BY entities.id;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;
  START TRANSACTION;
  
  REPEAT
    FETCH cur1 INTO a,b;
    IF NOT done THEN
       UPDATE entities SET trust = b WHERE id = a;
    END IF;
  UNTIL done END REPEAT;
  
  COMMIT;

  CLOSE cur1;
END$$

DROP PROCEDURE IF EXISTS `source_trust_assignment_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `source_trust_assignment_procedure`()
BEGIN
DECLARE done INT DEFAULT 0;
DECLARE a INT;
DECLARE cur1 CURSOR FOR SELECT sources.id FROM sources WHERE 1;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO a;
    IF NOT done THEN
     UPDATE `sources` SET `sources`.`entityTrust` = 
     (SELECT COALESCE((totalSources/total),0.00) AS trust
      FROM (
        SELECT COUNT(id) as total FROM `entities_sources` es1 WHERE es1.sourceID = a	
      ) as totalTable,
      (
        SELECT COALESCE(SUM(sourceCounts.sourceCount),0.00) AS totalSources FROM 
        (SELECT COUNT(id) AS sourceCount FROM `entities_sources` es1, (SELECT es2.entityID FROM entities_sources es2 WHERE es2.sourceID = a) AS tab2
        WHERE es1.`entityID` = tab2.entityID
        GROUP BY es1.`entityID` HAVING COUNT(id) > 1) AS sourceCounts
      ) as totalSharedSourcesTable
      WHERE 1) WHERE id = a;
    END IF;
  UNTIL done END REPEAT;

  CLOSE cur1;
  
END$$

DROP PROCEDURE IF EXISTS `source_trust_assignment_procedure2`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `source_trust_assignment_procedure2`()
    DETERMINISTIC
BEGIN
DECLARE done INT DEFAULT 0;
DECLARE a INT;
DECLARE cur1 CURSOR FOR SELECT sources.id FROM sources WHERE 1;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO a;
    IF NOT done THEN
     UPDATE `sources` SET `sources`.`entityTrust` = 
     (SELECT COALESCE(SUM(sourceCounts.sourceCount),0.00) AS totalSources FROM 
        (SELECT COUNT(id) AS sourceCount FROM `entities_sources` es1, (SELECT es2.entityID FROM entities_sources es2 WHERE es2.sourceID = a) AS tab2
        WHERE es1.`entityID` = tab2.entityID
        GROUP BY es1.`entityID` HAVING COUNT(id) > 1) AS sourceCounts
      ) WHERE id = a;
    END IF;
  UNTIL done END REPEAT;

  CLOSE cur1;
  
END$$

DROP PROCEDURE IF EXISTS `source_voting_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `source_voting_procedure`()
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE a INT;
  DECLARE b DOUBLE;
  DECLARE cur1 CURSOR FOR SELECT id,entityTrust FROM sources WHERE voting = 0 AND entityTrust > 0;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;
  START TRANSACTION; 
  REPEAT
    FETCH cur1 INTO a,b;
    IF NOT done THEN
       UPDATE `source_voting_view` SET trust = trust + b WHERE sourceID = a;
       UPDATE `sources` SET `sources`.voting = 1 WHERE `sources`.id = a;
    END IF;
  UNTIL done END REPEAT;
  COMMIT;
  
  CLOSE cur1;  
END$$

DROP PROCEDURE IF EXISTS `temp_procedure`$$
CREATE DEFINER=`root`@`localhost` PROCEDURE `temp_procedure`()
BEGIN
DECLARE done INT DEFAULT 0;
DECLARE a INT;
DECLARE cur1 CURSOR FOR SELECT sources.id FROM sources WHERE 1;
DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO a;
    IF NOT done THEN
        SELECT COALESCE(SUM(sourceCounts.sourceCount),0.00) AS totalSources FROM 
        (SELECT COUNT(id) AS sourceCount FROM `entities_sources` es1, (SELECT es2.entityID FROM entities_sources es2 WHERE es2.sourceID = a) AS tab2
        WHERE es1.`entityID` = tab2.entityID
        GROUP BY es1.`entityID` HAVING COUNT(id) > 1) AS sourceCounts;
    END IF;
  UNTIL done END REPEAT;

  CLOSE cur1;

END$$

--
-- Functions
--
DROP FUNCTION IF EXISTS `avgWordLength`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `avgWordLength`(
        text1 TEXT
    ) RETURNS double
    DETERMINISTIC
BEGIN
  DECLARE avgWordLength DOUBLE;
  SET avgWordLength = LENGTH(REPLACE(text1," ","")) / wordCount(text1);
  RETURN avgWordLength;
END$$

DROP FUNCTION IF EXISTS `countNumbers`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `countNumbers`(
        text1 TEXT
    ) RETURNS smallint(6)
    DETERMINISTIC
BEGIN
  DECLARE numericChars INT;
  DECLARE text2 TEXT;
  SET text2 = text1;
  SET text2 = REPLACE(text2,"0","");
  SET text2 = REPLACE(text2,"1","");
  SET text2 = REPLACE(text2,"2","");
  SET text2 = REPLACE(text2,"3","");
  SET text2 = REPLACE(text2,"4","");
  SET text2 = REPLACE(text2,"5","");
  SET text2 = REPLACE(text2,"6","");
  SET text2 = REPLACE(text2,"7","");
  SET text2 = REPLACE(text2,"8","");
  SET text2 = REPLACE(text2,"9","");
  
  RETURN LENGTH(text1) - LENGTH(text2);
END$$

DROP FUNCTION IF EXISTS `endsWithNumber`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `endsWithNumber`(
        text1 TEXT
    ) RETURNS tinyint(4)
    DETERMINISTIC
BEGIN
    DECLARE numericChar INT;
    SET numericChar = ASCII(REVERSE(text1)) BETWEEN 48 AND 57;

  RETURN numericChar;
END$$

DROP FUNCTION IF EXISTS `source_voting_function`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `source_voting_function`() RETURNS bigint(20)
BEGIN
  DECLARE done INT DEFAULT 0;
  DECLARE counter INT DEFAULT 0;
  DECLARE a INT;
  DECLARE b DOUBLE;
  DECLARE cur1 CURSOR FOR SELECT id,entityTrust FROM sources WHERE voting = 0 AND entityTrust > 0;
  DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

  OPEN cur1;

  REPEAT
    FETCH cur1 INTO a,b;
    IF NOT done THEN
       UPDATE `source_voting_view` SET trust = trust + (0.8 * b) WHERE sourceID = a AND voting = 0;
       UPDATE `sources` SET `sources`.voting = 1 WHERE `sources`.id = a;
       SET counter = counter + 1;
    END IF;
  UNTIL done END REPEAT;

  CLOSE cur1;
  
  RETURN counter;
END$$

DROP FUNCTION IF EXISTS `startsWithNumber`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `startsWithNumber`(
        text1 TEXT
    ) RETURNS tinyint(4)
    DETERMINISTIC
BEGIN
    DECLARE numericChar INT;
    SET numericChar = ASCII(text1) BETWEEN 48 AND 57;

  RETURN numericChar;
END$$

DROP FUNCTION IF EXISTS `wordCount`$$
CREATE DEFINER=`root`@`localhost` FUNCTION `wordCount`(
        text1 TEXT
    ) RETURNS int(11)
    DETERMINISTIC
BEGIN
  DECLARE words INT;
  SET words = LENGTH(text1) - LENGTH(REPLACE(text1,' ','')) + 1;

  RETURN words;
END$$

DELIMITER ;
