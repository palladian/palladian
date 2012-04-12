package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;

import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;

/**
 * Load data from table feed_evaluation_items. This is to be used for benchmark purpose only.
 * In benchmark mode, we inject the correctedPublishTime (correction done when creating the dataset) as publish date so
 * the {@link FeedItem} has no idea that its "original" publish date has already been corrected. This is required to do
 * an additional, optional correction of this timestamp when adding the item to the {@link Feed}. This correction is
 * required in the seldom case of simulating polls with variable window size: there it may happen that we see items in
 * the past of the last but one simulated poll so we need to 'correct' this item's timestamp. This anomaly can't be
 * avoided.
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

        // caution - we inject a corrected timestamp here!
        item.setPublished(resultSet.getTimestamp("correctedPublishTime"));
        // item.setPublished(resultSet.getTimestamp("publishTime"));
        // item.setCorrectedPublishedDate(resultSet.getTimestamp("correctedPublishTime"));
        item.setHash(resultSet.getString("extendedItemHash"), true);

        return item;

    }

}