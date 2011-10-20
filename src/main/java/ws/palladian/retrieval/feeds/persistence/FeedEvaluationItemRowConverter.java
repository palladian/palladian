package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.helper.SqlHelper;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;

/**
 * Load data from table feed_evaluation_items
 * 
 * @author Sandro Reichert
 */
public class FeedEvaluationItemRowConverter implements RowConverter<EvaluationFeedItem> {

    @Override
    public EvaluationFeedItem convert(ResultSet resultSet) throws SQLException {

        EvaluationFeedItem item = new EvaluationFeedItem();

        item.setFeedId(resultSet.getInt("feedId"));
        item.setSequenceNumber(SqlHelper.getInteger(resultSet, "sequenceNumber"));
        item.setPollTimestamp(resultSet.getTimestamp("pollTimestamp"));
        item.setPublished(resultSet.getTimestamp("publishTime"));
        item.setCorrectedPublishedDate(resultSet.getTimestamp("correctedPublishTime"));
        item.setHash(resultSet.getString("extendedItemHash"), true);

        return item;

    }

}