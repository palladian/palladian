package ws.palladian.retrieval.feeds.evaluation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.persistence.ConnectionManager;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedEvaluationItemRowConverter;

public class EvaluationFeedDatabase extends FeedDatabase {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EvaluationFeedDatabase.class);

    private static final String ADD_EVALUATION_ITEMS = "INSERT IGNORE INTO feed_evaluation_items SET feedId = ?, pollTimestamp = ?, itemHash = ?, publishTime = ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID = "SELECT * FROM feed_evaluation_items WHERE feedId = ? ORDER BY feedId ASC, pollTimestamp ASC, publishTime ASC LIMIT ?, ?;";
    private static final String GET_EVALUATION_ITEMS_BY_ID_PUBLISHTIME_LIMIT = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND publishTime <= ? ORDER BY pollTimestamp DESC, publishTime DESC LIMIT 0, ?";

    protected EvaluationFeedDatabase(ConnectionManager connectionManager) {
        super(connectionManager);
        // TODO Auto-generated constructor stub
    }

    /**
     * Add the provided items to table feed_evaluation_items. This may be used in TUDCS6 dataset to put all items from
     * csv files to database.
     * 
     * @param allItems The items to add.
     * @return true if all items have been added.
     */
    public boolean addEvaluationItems(List<FeedItem> allItems) {

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        for (FeedItem item : allItems) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(item.getFeedId());
            parameters.add(item.getPollSQLTimestamp());
            parameters.add(item.getHash());
            parameters.add(item.getPublishedSQLTimestamp());
            batchArgs.add(parameters);
        }

        int[] result = runBatchInsertReturnIds(ADD_EVALUATION_ITEMS, batchArgs);

        return (result.length == allItems.size());
    }

    /**
     * Get items from table feed_evaluation_items by feedID.
     * 
     * @param feedID The feed to get items for
     * @param from Use db's LIMIT command to limit number of results. LIMIT from, to
     * @param to Use db's LIMIT command to limit number of results. LIMIT from, to
     * @return
     */
    public List<FeedItem> getEvaluationItemsByID(int feedID, int from, int to) {
        return runQuery(new FeedEvaluationItemRowConverter(), GET_EVALUATION_ITEMS_BY_ID, feedID, from, to);
    }

    /**
     * Get a simulated window from table feed_evaluation_items by feedID. Items are ordered by pollTimestamp DESC and
     * publishTime DESC, we start with the provided publishTime and load the next #window items (that are older).
     * 
     * @param feedID The feed to get items for
     * @param publishTime The pollTimestamp
     * @param window Use db's LIMIT command to limit number of results. LIMIT 0, window
     * @return
     */
    public List<FeedItem> getEvaluationItemsByIDPollTimeLimit(int feedID, Timestamp publishTime, int window) {
        return runQuery(new FeedEvaluationItemRowConverter(), GET_EVALUATION_ITEMS_BY_ID_PUBLISHTIME_LIMIT, feedID,
                publishTime, window);
    }
}
