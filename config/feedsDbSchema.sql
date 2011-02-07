-- phpMyAdmin SQL Dump
-- version 3.2.4
-- http://www.phpmyadmin.net
--
-- Host: localhost
-- Erstellungszeit: 31. Januar 2011 um 11:38
-- Server Version: 5.1.41
-- PHP-Version: 5.3.1

SET SQL_MODE="NO_AUTO_VALUE_ON_ZERO";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- Datenbank: `feeds`
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
  `supportsPubSubHubBub` tinyint(1) DEFAULT NULL COMMENT 'true if feed supports the hub relation used by PubSubHubBub',
  PRIMARY KEY (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=2 ;

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
  PRIMARY KEY (`feedID`,`minuteOfDay`),
  CONSTRAINT `feeds_post_distribution_ibfk_1` FOREIGN KEY (`feedID`) REFERENCES `feeds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

--
-- Daten für Tabelle `feeds_post_distribution`
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
  `timelinessLate` double DEFAULT NULL COMMENT 'averaged over all new and missed items in the poll, NULL if no new item has been discovered (only for evaluation mode MIN interesting)',
  PRIMARY KEY (`feedID`,`numberOfPoll`),
  CONSTRAINT `feed_evaluation_polls_ibfk_1` FOREIGN KEY (`feedID`) REFERENCES `feeds` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Daten für Tabelle `feed_evaluation_polls`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_items`
--

CREATE TABLE IF NOT EXISTS `feed_items` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `feedId` int(10) unsigned NOT NULL,
  `title` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `link` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `rawId` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `published` datetime DEFAULT NULL,
  `authors` varchar(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `added` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `text` text COLLATE utf8_unicode_ci,
  `pageText` text COLLATE utf8_unicode_ci COMMENT 'text which we scraped from the corresponding page',
  PRIMARY KEY (`id`),
  CONSTRAINT `feed_items_ibfk_1` FOREIGN KEY (`feedId`) REFERENCES `feeds` (`id`) ON DELETE CASCADE,
  UNIQUE KEY `feedId_rawId_unique` (`feedId`,`rawId`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=101 ;

--
-- Daten für Tabelle `feed_items`
--

