package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.helper.SQLHelper;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;

public class FeedRowConverter implements RowConverter<Feed> {

    @Override
    public Feed convert(ResultSet resultSet) throws SQLException {


        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"), true);
        feed.setChecks(resultSet.getInt("checks"));
        feed.setUnreachableCount(resultSet.getInt("unreachableCount"));
        feed.setUnparsableCount(resultSet.getInt("unparsableCount"));
        feed.setMisses(resultSet.getInt("misses"));
        feed.setNumberOfItemsReceived(resultSet.getInt("totalItems"));
        feed.setWindowSize(SQLHelper.getInteger(resultSet, "windowSize"));
        feed.setVariableWindowSize(SQLHelper.getBoolean(resultSet, "hasVariableWindowSize"));
        feed.setUpdateInterval(SQLHelper.getInteger(resultSet, "checkInterval"));
        feed.setLastPollTime(resultSet.getTimestamp("lastPollTime"));
        feed.setLastSuccessfulCheckTime(resultSet.getTimestamp("lastSuccessfulCheck"));
        feed.setLastMissTime(resultSet.getTimestamp("lastMissTimestamp"));
        feed.setLastFeedEntry(resultSet.getTimestamp("lastFeedEntry"));
        feed.getMetaInformation().setAccessible(SQLHelper.getBoolean(resultSet, "isAccessibleFeed"));
        feed.setBlocked(resultSet.getBoolean("blocked"));
        feed.setTotalProcessingTime(resultSet.getLong("totalProcessingTime"));
        feed.setNewestItemHash(resultSet.getString("newestItemHash"));
        feed.setLastETag(resultSet.getString("lastEtag"));
        feed.setHttpLastModified(resultSet.getTimestamp("lastModified"));
        feed.setLastFeedTaskResult(resultSet.getString("lastResult"));
        feed.setActivityPattern(SQLHelper.getInteger(resultSet, "activityPattern"));
        feed.getMetaInformation().setFeedFormat(resultSet.getString("feedFormat"));
        feed.getMetaInformation().setByteSize(resultSet.getLong("feedSize"));
        feed.getMetaInformation().setSiteUrl(resultSet.getString("siteUrl"));
        feed.getMetaInformation().setTitle(resultSet.getString("title"));
        feed.getMetaInformation().setAdded(resultSet.getTimestamp("added"));
        feed.getMetaInformation().setLanguage(resultSet.getString("language"));
        feed.getMetaInformation().setHasItemIds(SQLHelper.getBoolean(resultSet, "hasItemIds"));
        feed.getMetaInformation().setHasPubDate(SQLHelper.getBoolean(resultSet, "hasPubDate"));
        feed.getMetaInformation().setHasCloud(SQLHelper.getBoolean(resultSet, "hasCloud"));
        feed.getMetaInformation().setTtl(SQLHelper.getInteger(resultSet, "ttl"));
        feed.getMetaInformation().setHasSkipHours(SQLHelper.getBoolean(resultSet, "hasSkipHours"));
        feed.getMetaInformation().setHasSkipDays(SQLHelper.getBoolean(resultSet, "hasSkipDays"));
        feed.getMetaInformation().setHasUpdated(SQLHelper.getBoolean(resultSet, "hasUpdated"));
        feed.getMetaInformation().setHasPublished(SQLHelper.getBoolean(resultSet, "hasPublished"));
        feed.getMetaInformation().setSupportsPubSubHubBub(SQLHelper.getBoolean(resultSet, "supportsPubSubHubBub"));
        feed.getMetaInformation().setCgHeaderSize(SQLHelper.getInteger(resultSet, "httpHeaderSize"));
        
        return feed;

    }

}
