package ws.palladian.retrieval.feeds.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.ConnectionManager;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedEvaluationItemRowConverter;

/**
 * TUDCS6 specific evaluation code.
 * 
 * @author Sandro Reichert
 */
public class EvaluationFeedDatabase extends FeedDatabase {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EvaluationFeedDatabase.class);

    private static final String TABLE_REPLACEMENT = "###TABLE_NAME###";
    private static final String ADD_EVALUATION_ITEMS = "INSERT IGNORE INTO feed_evaluation_items SET feedId = ?, sequenceNumber = ?, pollTimestamp = ?, extendedItemHash = ?, publishTime = ?, correctedPublishTime = ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID = "SELECT * FROM feed_evaluation_items WHERE feedId = ? ORDER BY feedId ASC, sequenceNumber ASC LIMIT ?, ?;";
    private static final String GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_LIMIT = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND correctedPublishTime <= ? ORDER BY sequenceNumber DESC LIMIT 0, ?";
    private static final String ADD_EVALUATION_POLL = "INSERT IGNORE INTO `###TABLE_NAME###` SET feedId = ?, numberOfPoll = ?, activityPattern = ?, sizeOfPoll = ?, pollTimestamp = ?, checkInterval = ?, newWindowItems = ?, missedItems = ?, windowSize = ?, cumulatedDelay = ?, pendingItems = ?, droppedItems = ?";
    /** reset table feeds except activityPattern and blocked. */
    private static final String RESET_TABLE_FEEDS = "UPDATE feeds SET checks = DEFAULT, unreachableCount = DEFAULT, unparsableCount = DEFAULT, misses = DEFAULT, totalItems = DEFAULT, windowSize = DEFAULT, hasVariableWindowSize = DEFAULT, lastPollTime = DEFAULT, lastSuccessfulCheck = DEFAULT, lastMissTimestamp = DEFAULT, lastFeedEntry = DEFAULT, isAccessibleFeed = DEFAULT, totalProcessingTime = DEFAULT, newestItemHash = DEFAULT, lastETag = DEFAULT, lastModified = DEFAULT, lastResult = DEFAULT, feedFormat = DEFAULT, feedSize = DEFAULT, title = DEFAULT, LANGUAGE = DEFAULT, hasItemIds = DEFAULT, hasPubDate = DEFAULT, hasCloud = DEFAULT, ttl = DEFAULT, hasSkipHours = DEFAULT, hasSkipDays = DEFAULT, hasUpdated = DEFAULT, hasPublished = DEFAULT, supportsPubSubHubBub = DEFAULT, httpHeaderSize = DEFAULT";
    private static final String GET_MISSED_ITEMS_BETWEEN_POLLS = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND pollTimestamp < ? AND correctedPublishTime > ? AND correctedPublishTime < ?";

    private static final String GET_NUMBER_MISSED_ITEMS_BY_ID_SEQUENCE_NUMBERS = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND sequenceNumber > ? AND sequenceNumber < ?";

    private static final String GET_PENDING_ITEMS_BY_ID = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ? ORDER BY pollTimestamp ASC, correctedPublishTime ASC";

    private static final String GET_NUMBER_PENDING_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ? AND correctedPublishTime <= ?";
    private static final String GET_NUMBER_PRE_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp < ? AND correctedPublishTime < ?";
    private static final String GET_NUMBER_POST_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ?";

    protected EvaluationFeedDatabase(ConnectionManager connectionManager) {
        super(connectionManager);
        // TODO Auto-generated constructor stub
    }

    /**
     * Add the provided items to table feed_evaluation_items. This may be used in TUDCS6 dataset to put all items from
     * csv files to database. Caution! A modified version of the item hash is written. It has the pollTimestamp as
     * prefix and has a length of 60 chars (19 chars timestamp + separator "_" + itemHash), e.g.
     * 2011-07-25_16-14-02_3a61deec78cda535ede2e2c862a8c80d01ae7e83
     * 
     * @param allItems The items to add.
     * @return true if all items have been added.
     */
    public boolean addEvaluationItems(List<EvaluationFeedItem> allItems) {

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        for (EvaluationFeedItem item : allItems) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(item.getFeedId());
            parameters.add(item.getSequenceNumber());
            parameters.add(item.getPollSQLTimestamp());

            // generate extended item hash
            String pollTime = DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", item.getPollTimestamp().getTime());
            String extendetItemHash = pollTime + "_" + item.getHash();
            parameters.add(extendetItemHash);
            parameters.add(item.getPublishedSQLTimestamp());
            parameters.add(item.getCorrectedPublishedSQLTimestamp());
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
    public List<EvaluationFeedItem> getEvaluationItemsByID(int feedID, int from, int to) {
        return runQuery(new FeedEvaluationItemRowConverter(), GET_EVALUATION_ITEMS_BY_ID, feedID, from, to);
    }

    /**
     * Get a simulated window from table feed_evaluation_items by feedID. Items are ordered by pollTimestamp DESC and
     * correctedPublishTime DESC, we start with the provided correctedPublishTime and load the last #window items (that
     * are older).
     * 
     * @param feedID The feed to get items for
     * @param correctedPublishTime The pollTimestamp
     * @param window Use db's LIMIT command to limit number of results. LIMIT 0, window
     * @return
     */
    public List<EvaluationFeedItem> getEvaluationItemsByIDCorrectedPublishTimeLimit(int feedID,
            Timestamp correctedPublishTime,
            int window) {
        return runQuery(new FeedEvaluationItemRowConverter(), GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_LIMIT,
                feedID, correctedPublishTime, window);
    }

    /**
     * Creates a table to write evaluation data into. Uses {@link #getEvaluationDbTableName()} to get the
     * name. In case creation of table is impossible, evaluation is aborted.
     * 
     * @param The name of the table to create.
     * @return <code>true</code> table has been successfully created, <code>false</code> otherwise.
     */
    public boolean createEvaluationDbTable(String evaluationTableName) {
        final String sql = "CREATE TABLE `"
                + evaluationTableName
                + "` ("
                + "`feedId` INT(10) UNSIGNED NOT NULL,"
                + "`numberOfPoll` INT(10) UNSIGNED NOT NULL COMMENT 'how often has this feed been polled (retrieved AND READ)',"
                + "`activityPattern` INT(11) NOT NULL COMMENT 'activity pattern of the feed',"
                + "`sizeOfPoll` INT(11) NOT NULL COMMENT 'the estimated amount of bytes to transfer: HTTP header + XML document',"
                + "`pollTimestamp` DATETIME NOT NULL COMMENT 'the feed has been pooled AT this TIMESTAMP',"
                + "`checkInterval` INT(11) UNSIGNED DEFAULT NULL COMMENT 'TIME IN minutes we waited betwen LAST AND this CHECK',"
                + "`newWindowItems` INT(10) UNSIGNED NOT NULL COMMENT 'number of NEW items IN the window',"
                + "`missedItems` INT(10) NOT NULL COMMENT 'the number of NEW items we missed because there more NEW items since the LAST poll THAN fit INTO the window',"
                + "`windowSize` INT(10) UNSIGNED NOT NULL COMMENT 'the current size of the feed''s window (number of items FOUND)',"
                + "`cumulatedDelay` DOUBLE DEFAULT NULL COMMENT 'cumulated delay IN seconds, adds absolute delay of polls that were too late',"
                + "`pendingItems` INT(10) UNSIGNED DEFAULT NULL COMMENT 'The number of items whos publishTime is newer than the last simulated poll that is within the benchmark interval.',"
                + "`droppedItems` INT(10) UNSIGNED DEFAULT NULL COMMENT 'At the first poll: The number of items prior to the first simulated poll. At the last poll: The number of items outside (newer) to benchmark stop time.'"
                + ") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Inject a custom table name into sql prepared statement string.
     * 
     * @param sql The sql statement to insert the table name into.
     * @param tableName The name to insert.
     * @return The modified sql string.
     */
    private String replaceTableName(String sql, String tableName) {
        return sql.replace(TABLE_REPLACEMENT, tableName);
    }

    /**
     * Add the provided PolLData to the provided table.
     * 
     * @param pollData The data of one simulated poll to add.
     * @param feedId The id of the feed the pollData belongs to.
     * @param activityPattern The feed's activityPattern.
     * @param tableName The name of the table to add the information to.
     * @return true if all pollData has been added.
     */
    public boolean addPollData(PollData pollData, int feedId, int activityPattern, String tableName) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feedId);
        parameters.add(pollData.getNumberOfPoll());
        parameters.add(activityPattern);
        parameters.add(pollData.getDownloadSize());
        parameters.add(pollData.getPollSQLTimestamp());
        parameters.add(pollData.getCheckInterval());
        parameters.add(pollData.getNewWindowItems());
        parameters.add(pollData.getMisses());
        parameters.add(pollData.getWindowSize());
        parameters.add(pollData.getCumulatedDelay());
        parameters.add(pollData.getPendingItems());
        parameters.add(pollData.getPreBenchmarkItems());

        String replacedSqlStatement = replaceTableName(ADD_EVALUATION_POLL, tableName);
        int result = runInsertReturnId(replacedSqlStatement, parameters);

        return (result != -1);
    }

    /**
     * Reset all rows of table feeds to default values except activityPattern.
     * 
     * @return <code>true</code> if successful, <code>false</code> otherwise.
     */
    public boolean resetTableFeeds() {
        return runUpdate(RESET_TABLE_FEEDS) != -1 ? true : false;
    }

    // /**
    // *
    // * FIXME: update description: Items on border are not included anymore.
    // * Get all items that are between the two items specified by publishDate and pollTimestamp, including the items on
    // * those borders. See example post history:
    // * <p>
    // *
    // * <pre>
    // * pollTimestamp ; publishTime ; itemHash
    // * ...
    // * 2011-07-12 17:22:32 ; 2011-07-12 12:56:13 ; be9881cc41862c98cacba3211c940eecf53728a4
    // * 2011-07-11 19:38:11 ; 2011-07-11 11:22:49 ; fb1987e2e0534f0d2decc37c5e14c2666f7b4d7c
    // * 2011-07-10 20:09:11 ; 2011-07-10 13:55:55 ; d0243d5f548205f5d23faf107f6b211609e8969b
    // * 2011-07-09 21:30:10 ; 2011-07-09 13:55:08 ; dc84301cfb00b14e7c997227617278b3f974dcf0
    // * 2011-07-09 03:09:10 ; 2011-07-08 19:02:06 ; 3b8c62a72be27000aa6dfa642b8e8f0193dcbff7
    // * ...
    // * </pre>
    // *
    // * </p>
    // *
    // * Assume the current <i>simulated</i> poll was at 2011-07-23 00:00:00 and the oldest item in the window has been
    // * published at 2011-07-12 12:56:13. The previous <i>simulated</i> poll was at 2011-07-09 00:00:00 and the newest
    // * item in this poll has been published at 2011-07-08 19:02:06. We now search for all items within these borders.
    // * In the example, the three items in between were missed. <br />
    // * TODO : Do we need to include the items at the borders? Some feeds did not provide publishTimes so there may be
    // * multiple items at the borders.
    // *
    // * @param feedId The feed to get missed items for.
    // * @param lastPoll The timestamp of the last poll.
    // * @param currentPoll The timestamp of the current poll.
    // * @param newestCorrectedPublishLastPoll The corrected publish date of the newest item from the last poll.
    // * @param oldestCorrectedPublishCurrentPoll The corrected publish date of the oldest item from the current poll.
    // * @return All items that have been missed between to simulated polls.
    // */
    // public List<EvaluationFeedItem> getMissedItemsBetweenPolls(int feedId, Timestamp lastPoll, Timestamp currentPoll,
    // Timestamp newestCorrectedPublishLastPoll, Timestamp oldestCorrectedPublishCurrentPoll) {
    // return runQuery(new FeedEvaluationItemRowConverter(), GET_MISSED_ITEMS_BETWEEN_POLLS, feedId, lastPoll,
    // currentPoll, newestCorrectedPublishLastPoll, oldestCorrectedPublishCurrentPoll);
    // }

    /**
     * Get all pending items that will not be downloaded in simulation. The number of items whos publishTime is newer
     * than the last simulated poll that is within the benchmark interval. Usually, this is relevant to the very last
     * poll only.<br />
     * Detail: Get the number of items that have a corrected publish date that is a) newer than
     * lastFeedEntrySQLTimestamp and b) older or equal to {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}
     * and c) a pollTimestamp newer than lastPollTimeSQLTimestamp.
     * 
     * @param feedId The feed to get the information for.
     * @param newestPollTime The timestamp of the newest simulated poll.
     * @param lastFeedEntry The timestamp of the newest entry in the newest simulated poll.
     * @return The number of pending items.
     */
    public int getNumberOfPendingItems(int feedId, Timestamp newestPollTime, Timestamp lastFeedEntry) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_PENDING_ITEMS_BY_ID, feedId, newestPollTime,
                lastFeedEntry, new Timestamp(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND));

        return numItems == null ? 0 : numItems;
    }

    /**
     * Get the number of items that have been missed. We missed all items with sequence number between (not including)
     * the numbers of the previous and the current poll.
     * 
     * @param feedId The feed to get the information for.
     * @param highestNumPreviousPoll The highest sequence number of the previous poll.
     * @param lowestNumCurrentPoll The lowest sequence number of the current poll.
     * @return The number of pending items.
     */
    public int getNumberOfMissedItems(int feedId, int highestNumPreviousPoll, int lowestNumCurrentPoll) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_MISSED_ITEMS_BY_ID_SEQUENCE_NUMBERS, feedId,
                highestNumPreviousPoll, lowestNumCurrentPoll);

        return numItems == null ? 0 : numItems;
    }

    /**
     * The number of items that have not been seen in evaluation mode. This is relevant to the very first poll only.
     * Usually, {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} is set a couple of hours later than the
     * creation of the dataset was started. Therefore, for some feeds we have more than one window at the first
     * simulated poll.<br />
     * Detail: Get the number of items that have a publish date older than lastFeedEntrySQLTimestamp and a pollTimestamp
     * older than lastPollTimeSQLTimestamp.
     * 
     * @param feedId The feed to get the information for.
     * @param firstPollTime The timestamp of the first simulated poll.
     * @param oldestFeedEntry The timestamp of the oldest entry in the first simulated poll.
     * @return The number of items prior to the first simulated poll.
     */
    public int getNumberOfPreBenchmarkItems(int feedId, Timestamp firstPollTime, Timestamp oldestFeedEntry) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_PRE_BENCHMARK_ITEMS_BY_ID, feedId,
                firstPollTime, oldestFeedEntry);

        return numItems == null ? 0 : numItems;
    }

    /**
     * The number of items in the dataset that are excluded from evaluation since their publish date is newer
     * that {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}. This is relevant to the last poll only.<br />
     * Detail: Get the number of items that have a publish date older than
     * {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND} and a pollTimestamp
     * older than {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}.
     * 
     * @param feedId The feed to get the information for.
     * @return The number of items in the dataset that are excluded from evaluation since their publish date is newer
     *         that {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}.
     */
    public int getNumberOfPostBenchmarkItems(int feedId) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_POST_BENCHMARK_ITEMS_BY_ID, feedId, new Timestamp(
                FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND), new Timestamp(
                FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND));

        return numItems == null ? 0 : numItems;
    }

}
