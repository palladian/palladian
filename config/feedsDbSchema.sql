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
  `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'The feeds internal identifier.',
  `feedUrl` VARCHAR(255) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The URL the feed can be found at. Set when feed is added.',
  `checks` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Number of times the feed has been retrieved and successfully parsed. Updated at every poll.',
  `unreachableCount` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Number of times the feed was checked but could not be found. Updated at every poll.',
  `unparsableCount` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Number of times the feed was checked but could not be parsed. Updated at every poll.',
  `misses` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'Number of times we found a MISS. Updated at every poll.',
  `totalItems` INT(11) UNSIGNED NOT NULL DEFAULT '0' COMMENT 'The total number or unique items received so far. Updated at every poll.',
  `windowSize` INT(11) DEFAULT NULL COMMENT 'The number of items at one request. Updated at every poll. Do not change to unsigned!',
  `hasVariableWindowSize` TINYINT(1) DEFAULT NULL COMMENT 'If true, the window size varies. Never set back to false. Updated at every poll. Do not change to unsigned!',
  `checkInterval` INT(11) DEFAULT NULL COMMENT 'Time in minutes to wait before checking the feed again. Updated at every poll. Do not change to unsigned!',
  `lastPollTime` DATETIME DEFAULT NULL COMMENT 'Timestamp of last poll of the feed. Updated at every poll.',
  `lastSuccessfulCheck` DATETIME DEFAULT NULL COMMENT 'Timestamp of last successful check (feed has been retrieved and successfully parsed). Updated at every poll.',
  `lastMissTimestamp` TIMESTAMP NULL DEFAULT NULL COMMENT 'The timestamp we detected the last MISS. Updated at every poll.',
  `lastFeedEntry` DATETIME DEFAULT NULL COMMENT 'Timestamp of the last feed entry found in this feed. Updated at every poll.',
  `isAccessibleFeed` TINYINT(1) DEFAULT NULL COMMENT 'Is the feed accessible? Updated when metadata is updated. Do not change to unsigned!',
  `blocked` TINYINT(1) NOT NULL DEFAULT '0' COMMENT 'If true, do not use feed to create a data set. Never set back to false. Updated at every poll.',
  `totalProcessingTime` DOUBLE DEFAULT NULL COMMENT 'Total time in milliseconds that was spend on processing this feed. Updated at every poll.',
  `lastETag` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The last ETag we received in the http header. Updated at every poll. Do not change to unsigned!',
  `lastModified` DATETIME DEFAULT NULL COMMENT 'The most recent value of the last-modidied element in the http header. Updated at every poll. Do not change to unsigned!',
  `lastResult` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The result of the most recent FeedTask. Updated at every poll.',
  `activityPattern` INT(11) DEFAULT NULL COMMENT 'Update class of the feed. Updated when metadata is updated. Do not change to unsigned!',
  `feedFormat` CHAR(20) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'Contains the version of the feed such as RSS/0.9 or ATOM/1.0. Updated when metadata is updated. Do not change to unsigned!',
  `feedSize` DOUBLE DEFAULT '0' COMMENT 'The size of the raw feed. Updated when metadata is updated. Do not change to unsigned!',
  `siteUrl` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The URL of the website this feed has been found at. Set when feed is added.',
  `title` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The title of this feed. ',
  `added` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'The timestamp this feed has been added to the table. (Set when feed is added.)',
  `language` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The language this feed is in. Updated when metadata is updated.',
  `hasItemIds` TINYINT(1) DEFAULT NULL COMMENT 'Do items have an id? Updated when metadata is updated. Do not change to unsigned!',
  `hasPubDate` TINYINT(1) DEFAULT NULL COMMENT 'Does the entries have a pubDate (RSS2 specific). Updated when metadata is updated. Do not change to unsigned!',
  `hasCloud` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the cloud element (RSS2 specific). Updated when metadata is updated. Do not change to unsigned!',
  `ttl` INT(11) DEFAULT NULL COMMENT 'The time to live, (RSS2 specific). Updated when metadata is updated. Do not change to unsigned!',
  `hasSkipHours` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the skipHours element (RSS2 specific). Updated when metadata is updated. Do not change to unsigned!',
  `hasSkipDays` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the skipDays element (RSS2 specific). Updated when metadata is updated. Do not change to unsigned!',
  `hasUpdated` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the updated element (Atom specific). Updated when metadata is updated. Do not change to unsigned!',
  `hasPublished` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the published element (Atom specific). Updated when metadata is updated. Do not change to unsigned!',
  `supportsPubSubHubBub` TINYINT(1) DEFAULT NULL COMMENT 'Does the feed support the PubSubHubBub element. Updated when metadata is updated. Do not change to unsigned!',
  `httpHeaderSize` INT(11) DEFAULT NULL COMMENT 'The size of a conditional GET response. Updated when metadata is updated. Do not change to unsigned!',
  PRIMARY KEY (`id`),
  UNIQUE KEY `feedUrl` (`feedUrl`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

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
  `pollTimestamp` bigint(20) unsigned NOT NULL COMMENT 'the feed has been polled at this timestamp',
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
  `itemHash` char(40) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The sha1-hash of the item.',
  PRIMARY KEY (`id`),
  CONSTRAINT `feed_items_ibfk_1` FOREIGN KEY (`feedId`) REFERENCES `feeds` (`id`) ON DELETE CASCADE,
  UNIQUE KEY `itemHash` (`itemHash`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Daten für Tabelle `feed_items`
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_polls`
--

CREATE TABLE IF NOT EXISTS `feed_polls` (
  `id` INT(10) NOT NULL,
  `pollTimestamp` DATETIME NOT NULL,
  `httpETag` VARCHAR(255) COLLATE utf8_unicode_ci DEFAULT NULL COMMENT 'The ETag element from the HTTP header. ',
  `httpDate` DATETIME DEFAULT NULL COMMENT 'The date element from the HTTP header. ',
  `httpLastModified` DATETIME DEFAULT NULL COMMENT 'The lastModified element from the HTTP header. ',
  `httpExpires` DATETIME DEFAULT NULL COMMENT 'The expires element from the HTTP header.',
  `newestItemTimestamp` DATETIME DEFAULT NULL COMMENT 'The newest timestamp of the newest item (one item might have two timestamps).',
  `numberNewItems` INT(10) DEFAULT NULL COMMENT 'The number of new items.',
  `windowSize` INT(10) DEFAULT NULL COMMENT 'The current window size.',
  `httpStatusCode` INT(10) NOT NULL COMMENT 'The http status code returned.',
  `responseSize` INT(10) DEFAULT NULL COMMENT 'The size in bytes of the received HTTP response. Value has been restored from dataset. If httpStatusCode = 304, we used the HTTP header size from one poll, otherwise the size of the content of the current or previous gz file.'  
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

--
-- Daten für Tabelle `feed_polls
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_item_cache`
--
CREATE TABLE IF NOT EXISTS `feed_item_cache` (
  `id` int(10) unsigned NOT NULL COMMENT 'The feeds internal identifier.',
  `itemHash` CHAR(40) COLLATE utf8_unicode_ci NOT NULL COMMENT 'The sha1-hash of a item (in xml) of the last poll.',
  `correctedPollTime` DATETIME NOT NULL COMMENT 'Corrected publish date of this item.',
  PRIMARY KEY (`id`,`itemHash`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci COMMENT='table to serialize the item cache';

--
-- Daten für Tabelle `feed_item_cache
--


-- --------------------------------------------------------

--
-- Tabellenstruktur für Tabelle `feed_evaluation_items`
--

CREATE TABLE IF NOT EXISTS `feed_evaluation_items` (
  `feedId` INT(10) UNSIGNED NOT NULL COMMENT 'The feeds internal identifier.',
  `sequenceNumber` INT(10) NOT NULL COMMENT 'A sequence number of the item to imitate line numbers.',
  `pollTimestamp` DATETIME NOT NULL COMMENT 'At this timestamp, the item has been received.',
  `extendedItemHash` CHAR(60) COLLATE utf8_unicode_ci NOT NULL COMMENT 'An extended item hash made of pollTimestamp_itemHash where itemHash is a sha-1(id,title,link).',
  `publishTime` DATETIME DEFAULT NULL COMMENT 'Original publish date of this item.',
  `correctedPublishTime` DATETIME NOT NULL COMMENT 'Corrected publish date of this item.',
  PRIMARY KEY (`feedId`,`sequenceNumber`),
  KEY `pollTimestamp_idx` (`pollTimestamp`),
  KEY `correctedPublishTime_idx` (`correctedPublishTime`)
) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci

--
-- Daten für Tabelle `feed_items`
--

