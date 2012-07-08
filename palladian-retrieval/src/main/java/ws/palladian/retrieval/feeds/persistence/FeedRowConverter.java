package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedActivityPattern;
import ws.palladian.retrieval.feeds.FeedTaskResult;

public class FeedRowConverter implements RowConverter<Feed> {

    @Override
    public Feed convert(ResultSet resultSet) throws SQLException {

        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"));
        feed.setChecks(resultSet.getInt("checks"));
        feed.setUnreachableCount(resultSet.getInt("unreachableCount"));
        feed.setUnparsableCount(resultSet.getInt("unparsableCount"));
        feed.setMisses(resultSet.getInt("misses"));
        feed.setNumberOfItemsReceived(resultSet.getInt("totalItems"));
        feed.setWindowSize(SqlHelper.getInteger(resultSet, "windowSize"));
        feed.setVariableWindowSize(SqlHelper.getBoolean(resultSet, "hasVariableWindowSize"));
        feed.setUpdateInterval(SqlHelper.getInteger(resultSet, "checkInterval"));
        feed.setLastPollTime(resultSet.getTimestamp("lastPollTime"));
        feed.setLastSuccessfulCheckTime(resultSet.getTimestamp("lastSuccessfulCheck"));
        feed.setLastMissTime(resultSet.getTimestamp("lastMissTimestamp"));
        feed.setLastFeedEntry(resultSet.getTimestamp("lastFeedEntry"));
        feed.getMetaInformation().setAccessible(SqlHelper.getBoolean(resultSet, "isAccessibleFeed"));
        feed.setBlocked(resultSet.getBoolean("blocked"));
        feed.setTotalProcessingTime(resultSet.getLong("totalProcessingTime"));
        feed.setNewestItemHash(resultSet.getString("newestItemHash"));
        feed.setLastETag(resultSet.getString("lastEtag"));
        feed.setHttpLastModified(resultSet.getTimestamp("lastModified"));
        feed.setLastFeedTaskResult(FeedTaskResult.valueOf(resultSet.getString("lastResult")));
        feed.setActivityPattern(FeedActivityPattern.fromIdentifier(resultSet.getInt("activityPattern")));
        feed.getMetaInformation().setFeedFormat(resultSet.getString("feedFormat"));
        feed.getMetaInformation().setByteSize(resultSet.getLong("feedSize"));
        feed.getMetaInformation().setSiteUrl(resultSet.getString("siteUrl"));
        feed.getMetaInformation().setTitle(resultSet.getString("title"));
        feed.getMetaInformation().setAdded(resultSet.getTimestamp("added"));
        feed.getMetaInformation().setLanguage(resultSet.getString("language"));
        feed.getMetaInformation().setHasItemIds(SqlHelper.getBoolean(resultSet, "hasItemIds"));
        feed.getMetaInformation().setHasPubDate(SqlHelper.getBoolean(resultSet, "hasPubDate"));
        feed.getMetaInformation().setHasCloud(SqlHelper.getBoolean(resultSet, "hasCloud"));
        feed.getMetaInformation().setTtl(SqlHelper.getInteger(resultSet, "ttl"));
        feed.getMetaInformation().setHasSkipHours(SqlHelper.getBoolean(resultSet, "hasSkipHours"));
        feed.getMetaInformation().setHasSkipDays(SqlHelper.getBoolean(resultSet, "hasSkipDays"));
        feed.getMetaInformation().setHasUpdated(SqlHelper.getBoolean(resultSet, "hasUpdated"));
        feed.getMetaInformation().setHasPublished(SqlHelper.getBoolean(resultSet, "hasPublished"));
        feed.getMetaInformation().setSupportsPubSubHubBub(SqlHelper.getBoolean(resultSet, "supportsPubSubHubBub"));
        feed.getMetaInformation().setCgHeaderSize(SqlHelper.getInteger(resultSet, "httpHeaderSize"));

        return feed;

    }

}
