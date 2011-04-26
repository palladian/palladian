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

CREATE TABLE IF NOT EXISTS `rankingCache` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `url` varchar(255) COLLATE utf8_unicode_ci NOT NULL,
  `service` tinyint(4) NOT NULL,
  `ranking` float NOT NULL,
  `updated` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `url_service_unique` (`url`,`service`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feeds`
--

CREATE TABLE IF NOT EXISTS `feeds` (
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT,
  `feedUrl` VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL,
  `siteUrl` VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL,
  `title` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `format` TINYINT(4) NOT NULL,
  `textType` TINYINT(4) NOT NULL,
  `added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `language` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `checks` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'number of times the feed has been retrieved and read',
  `minCheckInterval` INT(11) UNSIGNED NOT NULL DEFAULT '5' COMMENT 'time in minutes until it is expected to find at least one new entry in the feed',
  `maxCheckInterval` INT(11) UNSIGNED NOT NULL DEFAULT '360' COMMENT 'time in minutes until it is expected to find only new but one new entries in the feed',
  `newestItemHash` CHAR(40) COLLATE utf8_unicode_ci NOT NULL COMMENT 'the sha1-hash of the first item (in xml) of the last poll',
  `unreachableCount` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'number of times the feed was checked but could not be found or parsed',
  `lastFeedEntry` DATETIME DEFAULT NULL COMMENT 'timestamp of the last feed entry found in this feed',
  `activityPattern` INT(11) NOT NULL DEFAULT '-1' COMMENT 'update class of the feed',
  `supportsLMS` TINYINT(1) DEFAULT NULL,
  `supportsETag` TINYINT(1) DEFAULT NULL,
  `conditionalGetResponseSize` INT(11) DEFAULT NULL,
  `lastPollTime` DATETIME DEFAULT NULL,
  `lastETag` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL,
  `totalProcessingTime` DOUBLE DEFAULT '0' COMMENT 'Total time in milliseconds that was spend on processing this feed.',
  `misses` INT(11) DEFAULT '0' COMMENT 'Number of times we found a MISS',
  `lastMissTimestamp` TIMESTAMP NULL DEFAULT NULL COMMENT 'The timestamp we detected the last MISS.',
  `blocked` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'If true, do not use feed to create a data set',
  PRIMARY KEY (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ;

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ;

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
  `description` text COLLATE utf8_unicode_ci,
  `text` text COLLATE utf8_unicode_ci,
  `pageText` text COLLATE utf8_unicode_ci COMMENT 'text which we scraped from the corresponding page',
  PRIMARY KEY (`id`),
  CONSTRAINT `feed_items_ibfk_1` FOREIGN KEY (`feedId`) REFERENCES `feeds` (`id`) ON DELETE CASCADE,
  UNIQUE KEY `feedId_rawId_unique` (`feedId`,`rawId`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci ;

--
-- Daten für Tabelle `feed_items`
--

