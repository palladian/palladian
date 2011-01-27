package tud.iir.web.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import tud.iir.persistence.RowConverter;
import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedContentClassifier.FeedContentType;

public class FeedRowConverter implements RowConverter<Feed> {

    @Override
    public Feed convert(ResultSet resultSet) throws SQLException {
        
        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"));
        feed.setSiteUrl(resultSet.getString("siteUrl"));
        feed.setTitle(resultSet.getString("title"));
        feed.setContentType(FeedContentType.getByIdentifier(resultSet.getInt("textType")));
        feed.setLanguage(resultSet.getString("language"));
        feed.setAdded(resultSet.getTimestamp("added"));
        feed.setChecks(resultSet.getInt("checks"));
        feed.setUpdateInterval(resultSet.getInt("minCheckInterval"));
        feed.setUpdateInterval(resultSet.getInt("maxCheckInterval"));
        feed.setLastHeadlines(resultSet.getString("lastHeadlines"));
        feed.setUnreachableCount(resultSet.getInt("unreachableCount"));
        feed.setLastFeedEntry(resultSet.getTimestamp("lastFeedEntry"));
        feed.setActivityPattern(resultSet.getInt("activityPattern"));
        feed.setLastETag(resultSet.getString("lastEtag"));
        feed.setETagSupport(resultSet.getBoolean("supportsETag"));
        feed.setLMSSupport(resultSet.getBoolean("supportsLMS"));
        feed.setCgHeaderSize(resultSet.getInt("conditionalGetResponseSize"));
        
        return feed;

    }

}
