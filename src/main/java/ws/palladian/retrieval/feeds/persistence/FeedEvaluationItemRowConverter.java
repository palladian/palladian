package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.FeedItem;

/**
 * Load data from table feed_evaluation_items
 * 
 * @author Sandro Reichert
 */
public class FeedEvaluationItemRowConverter implements RowConverter<FeedItem> {

    @Override
    public FeedItem convert(ResultSet resultSet) throws SQLException {

        FeedItem entry = new FeedItem();

        entry.setFeedId(resultSet.getInt("feedId"));
        entry.setPollTimestamp(resultSet.getTimestamp("pollTimestamp"));
        entry.setPublished(resultSet.getTimestamp("publishTime"));
        entry.setHash(resultSet.getString("itemHash"), true);

        return entry;

    }

}