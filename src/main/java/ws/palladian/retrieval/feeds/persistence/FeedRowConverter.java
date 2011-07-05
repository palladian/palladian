package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;

public class FeedRowConverter implements RowConverter<Feed> {

    @Override
    public Feed convert(ResultSet resultSet) throws SQLException {

        /*
         * Caution! Using (Integer) resultSet.getObject() instead of getInt() since getInt() returns 0 if database
         * contains NULL, but we need to differentiate between 0 and NULL, e.g. empty window or not a feed.
         * This is a bit dangerous since the db field MUST NOT be UNSIGNED! --Sandro 01.07.2011
         */

        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"), true);
        feed.setChecks(resultSet.getInt("checks"));
        feed.setUnreachableCount(resultSet.getInt("unreachableCount"));
        feed.setUnparsableCount(resultSet.getInt("unparsableCount"));
        feed.setMisses(resultSet.getInt("misses"));
        feed.setNumberOfItemsReceived(resultSet.getInt("totalItems"));
        feed.setWindowSize((Integer) resultSet.getObject("windowSize"));
        feed.setVariableWindowSize((Boolean) resultSet.getObject("hasVariableWindowSize"));
        feed.setUpdateInterval((Integer) resultSet.getObject("checkInterval"));
        feed.setLastPollTime(resultSet.getTimestamp("lastPollTime"));
        feed.setLastSuccessfulCheckTime(resultSet.getTimestamp("lastSuccessfulCheck"));
        feed.setLastMissTime(resultSet.getTimestamp("lastMissTimestamp"));
        feed.setLastFeedEntry(resultSet.getTimestamp("lastFeedEntry"));
        feed.getMetaInformation().setAccessible((Boolean) resultSet.getObject("isAccessibleFeed"));
        feed.setBlocked(resultSet.getBoolean("blocked"));
        feed.setTotalProcessingTime(resultSet.getLong("totalProcessingTime"));
        feed.setNewestItemHash(resultSet.getString("newestItemHash"));
        feed.setLastETag(resultSet.getString("lastEtag"));
        feed.setHttpLastModified(resultSet.getTimestamp("lastModified"));
        feed.setLastFeedTaskResult(resultSet.getString("lastResult"));
        feed.setActivityPattern((Integer) resultSet.getObject("activityPattern"));
        feed.getMetaInformation().setFeedFormat(resultSet.getString("feedFormat"));
        feed.getMetaInformation().setByteSize(resultSet.getLong("feedSize"));
        feed.getMetaInformation().setSiteUrl(resultSet.getString("siteUrl"));
        feed.getMetaInformation().setTitle(resultSet.getString("title"));
        feed.getMetaInformation().setAdded(resultSet.getTimestamp("added"));
        feed.getMetaInformation().setLanguage(resultSet.getString("language"));
        feed.getMetaInformation().setHasItemIds((Boolean) resultSet.getObject("hasItemIds"));
        feed.getMetaInformation().setHasPubDate((Boolean) resultSet.getObject("hasPubDate"));
        feed.getMetaInformation().setHasCloud((Boolean) resultSet.getObject("hasCloud"));
        feed.getMetaInformation().setTtl((Integer) resultSet.getObject("ttl"));
        feed.getMetaInformation().setHasSkipHours((Boolean) resultSet.getObject("hasSkipHours"));
        feed.getMetaInformation().setHasSkipDays((Boolean) resultSet.getObject("hasSkipDays"));
        feed.getMetaInformation().setHasUpdated((Boolean) resultSet.getObject("hasUpdated"));
        feed.getMetaInformation().setHasPublished((Boolean) resultSet.getObject("hasPublished"));
        feed.getMetaInformation().setSupportsPubSubHubBub((Boolean) resultSet.getObject("supportsPubSubHubBub"));
        feed.getMetaInformation().setCgHeaderSize((Integer) resultSet.getObject("httpHeaderSize"));
        
        return feed;

    }

}
