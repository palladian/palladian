package ws.palladian.retrieval.feeds.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;

/**
 * Load data from table feed_evaluation_newest_items
 * 
 * @author Sandro Reichert
 */
public class FeedEvalNewestItemHashRowConverter implements RowConverter<EvaluationFeedItem> {

    @Override
    public EvaluationFeedItem convert(ResultSet resultSet) throws SQLException {

        EvaluationFeedItem item = new EvaluationFeedItem();

        item.setFeedId(resultSet.getInt("feedId"));
        item.setHash(resultSet.getString("extendedItemHash"), true);

        return item;

    }

}