package ws.palladian.retrieval.feeds.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.ConnectionManager;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedEvaluationItemRowConverter;
import ws.palladian.retrieval.feeds.persistence.FeedRowConverter;

/**
 * TUDCS6 specific evaluation code.
 * 
 * @author Sandro Reichert
 */
public class EvaluationFeedDatabase extends FeedDatabase {


    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EvaluationFeedDatabase.class);

    private static final String TABLE_REPLACEMENT = "###TABLE_NAME###";
    private static final String GET_FEEDS_WITH_TIMESTAMPS = "SELECT * FROM feeds WHERE hasPubDate = 1 OR hasUpdated = 1 OR hasPublished = 1 OR totalItems = 0";
    private static final String ADD_EVALUATION_ITEMS = "INSERT IGNORE INTO feed_evaluation_items SET feedId = ?, sequenceNumber = ?, pollTimestamp = ?, extendedItemHash = ?, publishTime = ?, correctedPublishTime = ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID = "SELECT * FROM feed_evaluation_items WHERE feedId = ? ORDER BY feedId ASC, sequenceNumber ASC LIMIT ?, ?;";
    private static final String GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_LIMIT = "SELECT * FROM feed_evaluation_items FORCE INDEX (PRIMARY, correctedPublishTime_idx) WHERE feedId = ? AND correctedPublishTime <= ? ORDER BY sequenceNumber DESC LIMIT 0, ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_RANGE_LIMIT = "SELECT * FROM feed_evaluation_items FORCE INDEX (PRIMARY, correctedPublishTime_idx) WHERE feedId = ? AND correctedPublishTime <= ? AND correctedPublishTime >= ? ORDER BY sequenceNumber DESC LIMIT 0, ?";
    private static final String ADD_EVALUATION_POLL = "INSERT IGNORE INTO `###TABLE_NAME###` SET feedId = ?, numberOfPoll = ?, numPollNewItem = ?, activityPattern = ?, sizeOfPoll = ?, pollTimestamp = ?, checkInterval = ?, newWindowItems = ?, missedItems = ?, windowSize = ?, cumulatedDelay = ?, pendingItems = ?, droppedItems = ?";
    /** reset table feeds except activityPattern and blocked. */
    private static final String RESET_TABLE_FEEDS = "UPDATE feeds SET checks = DEFAULT, unreachableCount = DEFAULT, unparsableCount = DEFAULT, misses = DEFAULT, windowSize = DEFAULT, lastPollTime = DEFAULT, lastSuccessfulCheck = DEFAULT, lastMissTimestamp = DEFAULT, lastFeedEntry = DEFAULT, totalProcessingTime = DEFAULT, newestItemHash = DEFAULT, lastETag = DEFAULT, lastModified = DEFAULT, lastResult = DEFAULT, feedFormat = DEFAULT, feedSize = DEFAULT, title = DEFAULT, LANGUAGE = DEFAULT, httpHeaderSize = DEFAULT";

    private static final String GET_NUMBER_MISSED_ITEMS_BY_ID_SEQUENCE_NUMBERS = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND sequenceNumber > ? AND sequenceNumber < ?";

    private static final String GET_NUMBER_PENDING_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ? AND correctedPublishTime <= ?";
    private static final String GET_NUMBER_PRE_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp < ? AND correctedPublishTime < ?";
    private static final String GET_NUMBER_POST_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ?";

    private static final String GET_EVALUATION_NEWEST_ITEM_HASHES = "SELECT f1.`feedId` , f1.`extendedItemHash` FROM `feed_evaluation_items` f1 JOIN (SELECT `feedId` , MAX( `sequenceNumber` ) AS maxsn FROM `feed_evaluation_items` GROUP BY `feedId`) AS f2 ON f1.`feedId` = f2.`feedId` AND f1.`sequenceNumber` = f2.maxsn";

    private static final String ADD_INDEX_TO_EVALUATION_TABLE = "ALTER TABLE `###TABLE_NAME###` ADD INDEX `feedId_idx` (`feedId`);";

    /**
     * Used as postfix for table names; table contains evaluation results per feed averaged over all items of this feed.
     */
    private static final String MODE_FEEDS = "feeds";

    /**
     * Identifier for mode items
     */
    private static final String MODE_ITEMS = "items";

    /**
     * Used as postfix for table names; table contains final evaluation results for mode items and feeds.
     */
    private static final String AVG_POSTFIX = "_avg";

    /**
     * All feeds from database that do have item timestamps or no items at all.
     */
    private Collection<Feed> feeds;

    /** Queue batch inserts into evaluation tables. Structure: Map<SQL statement,list of batchArgs> */
    private static final ConcurrentHashMap<String, List<List<Object>>> BATCH_INSERT_QUEUE = new ConcurrentHashMap<String, List<List<Object>>>();

    /**
     * Contains for each feedId the extendedItemHash from table feed_evaluation_items that has the highest
     * sequenceNumber within this feed. Used to simulate the last item in csv file.
     */
    private static final ConcurrentHashMap<Integer, String> NEWEST_ITEM_HASHES = new ConcurrentHashMap<Integer, String>();

    /**
     * This is an optimization cache to prevent unnecessary data base accesses when searching for a simulated window. If
     * we know that we found the item with the highest sequence number for a feed, we cache this window and return it on
     * every subsequent query for simulated windows since those query results can't contain a newer window than this
     * one. This can't be directly done by db (query) caching since queries differ.
     */
    private static final ConcurrentHashMap<Integer, List<EvaluationFeedItem>> CACHED_NEWEST_WINDOWS = new ConcurrentHashMap<Integer, List<EvaluationFeedItem>>();

    /**
     * Capacity of {@link #BATCH_INSERT_QUEUE}. Size of the queue is defined as sum of all insert operation, i.e. the
     * sum
     * over the sizes of all outer lists of all SQL statements.
     */
    private static final int QUEUE_CAPACITY = 1000;
    
    protected EvaluationFeedDatabase(ConnectionManager connectionManager) {
        super(connectionManager);
        feeds = runQuery(new FeedRowConverter(), GET_FEEDS_WITH_TIMESTAMPS);
        initializeNewestItemHashes();
    }

    /**
     * Initialize/load {@link #NEWEST_ITEM_HASHES} from db.
     */
    private void initializeNewestItemHashes() {
        List<EvaluationFeedItem> newestHashes = runQuery(new FeedEvalNewestItemHashRowConverter(),
                GET_EVALUATION_NEWEST_ITEM_HASHES);

        for (EvaluationFeedItem item : newestHashes) {
            NEWEST_ITEM_HASHES.put(item.getFeedId(), item.getHash());
        }

    }

    /**
     * @return All feeds from database that do have item timestamps or no items at all.
     */
    public Collection<Feed> getFeedsWithTimestamps() {
        return feeds;
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
     * @param feedId The feed to get items for
     * @param correctedPublishTime The pollTimestamp
     * @param window Use db's LIMIT command to limit number of results. LIMIT 0, window
     * @return a simulated window.
     */
    public List<EvaluationFeedItem> getEvaluationItemsByIDCorrectedPublishTimeLimit(int feedId,
            Timestamp correctedPublishTime, int window) {

        // try to get simulated window from local cache
        List<EvaluationFeedItem> simulatedWindow = getSimulatedWindowFromCache(feedId);

        // if we didn't found it, load it from db and cache the response.
        if (simulatedWindow == null) {

            simulatedWindow = runQuery(new FeedEvaluationItemRowConverter(),
                    GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_LIMIT, feedId, correctedPublishTime, window);

            putSimulatedWindowToCache(feedId, simulatedWindow);
        } else {
            // LOGGER.info("FeedId " + feedId + ": got simulated window from cache.");
        }
        return simulatedWindow;
    }

    /**
     * Get a simulated window from table feed_evaluation_items by feedID. Use if you know that the feeds has no variable
     * windowSize and you got at least one window before the current simulated poll. This is much faster than
     * {@link #getEvaluationItemsByIDCorrectedPublishTimeLimit(int, Timestamp, int)}. <br />
     * <br />
     * Items are ordered by pollTimestamp DESC and correctedPublishTime DESC, we start with the provided
     * correctedPublishTime and load the last #window items (that are older).
     * 
     * @param feedId The feed to get items for
     * @param correctedPublishTime The timestamp of the simulated poll
     * @param correctedPublishTimeLowerBound The timestamp of the oldest item from the previous poll
     * @param window Use db's LIMIT command to limit number of results. LIMIT 0, window
     * @return a simulated window.
     */
    public List<EvaluationFeedItem> getEvaluationItemsByIDCorrectedPublishTimeRangeLimit(int feedId,
            Timestamp correctedPublishTime, Timestamp correctedPublishTimeLowerBound, int window) {

        // try to get simulated window from local cache
        List<EvaluationFeedItem> simulatedWindow = getSimulatedWindowFromCache(feedId);

        // if we didn't found it, load it from db and cache the response.
        if (simulatedWindow == null) {

            simulatedWindow = runQuery(new FeedEvaluationItemRowConverter(),
                    GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_RANGE_LIMIT, feedId, correctedPublishTime, 
                    correctedPublishTimeLowerBound, window); 

            putSimulatedWindowToCache(feedId, simulatedWindow);
        } else {
            // LOGGER.info("FeedId " + feedId + ": got simulated window from cache.");
        }
        return simulatedWindow;
    }

    
    /**
     * Load the simulatedWindow from cache if it has been cached.
     * <p>
     * This is an optimization to prevent unnecessary data base accesses when searching for a simulated window. If we
     * know that we found the item with the highest sequence number for a feed, we cache this window and return it on
     * every subsequent query for simulated windows since those query results can't contain a newer window than this
     * one. This can't be directly done by db (query) caching since queries differ.
     * </p>
     * 
     * @param feedId The feedId the window belongs to.
     * @return The cached window or <code>null</code> if there is no cached window yet.
     */
    private List<EvaluationFeedItem> getSimulatedWindowFromCache(Integer feedId) {
        return CACHED_NEWEST_WINDOWS.get(feedId);
    }

    /**
     * Checks whether the window contains the newest item we have in dataset. If so, ad window to the internal cache.
     * <p>
     * This is an optimization to prevent unnecessary data base accesses when searching for a simulated window. If we
     * know that we found the item with the highest sequence number for a feed, we cache this window and return it on
     * every subsequent query for simulated windows since those query results can't contain a newer window than this
     * one. This can't be directly done by db (query) caching since queries differ.
     * </p>
     * 
     * @param feedId The feedId the window belongs to.
     * @param simulatedWindow The window to check and add to cache.
     * @return <code>true</code> if simulatedWindow has been cached or <code>false</code> if not.
     */
    private boolean putSimulatedWindowToCache(Integer feedId, List<EvaluationFeedItem> simulatedWindow) {
        boolean cached = false;

        String newestHash = null;
        newestHash = NEWEST_ITEM_HASHES.get(feedId);

        if (newestHash != null) {
            for (EvaluationFeedItem item : simulatedWindow) {

                // check whether window can be cached
                if (item.getHash().equals(newestHash)) {
                    LOGGER.debug("Found newestItemHash " + " in window, putting window to cache.");
                    CACHED_NEWEST_WINDOWS.put(feedId, simulatedWindow);
                    cached = true;
                }
            }
        }
        return cached;
    }

    /**
     * Creates a table to write evaluation data into. Uses {@link #simulatedPollsDbTableName()} to get the
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
                + "`numPollNewItem` int(10) DEFAULT NULL COMMENT 'number of poll WITH AT LEAST ONE NEW item, the initial poll has VALUE 1, polls without NEW items NULL.',"
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
     * Create two tables to write evaluation results such as average delay or polls per item of a single strategy to.
     * 
     * 
     * @param baseTableName The base name of the tables to create.
     * @return <code>true</code> if both tables have been successfully created, <code>false</code> otherwise.
     */
    private boolean createEvaluationResultTables(String baseTableName) {
    
        String tableName = baseTableName + "_" + MODE_FEEDS;
        StringBuilder sqlBuilder = new StringBuilder();

        // create table to average over all feeds
        sqlBuilder.append("CREATE TABLE `");
        sqlBuilder.append(tableName);
        sqlBuilder.append("` (");
        sqlBuilder.append("`feedId` INT(10) UNSIGNED NOT NULL ");
        sqlBuilder.append("COMMENT 'The feeds internal identifier.',");
        sqlBuilder.append("`PPI` DOUBLE DEFAULT NULL COMMENT 'Arithmetic average of polls per newly found item.',");
        sqlBuilder.append("`avgDelayMinutes` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'Arithmetic average delay to a newly found item in minutes.',");
        sqlBuilder.append("`totalMisses` INT DEFAULT NULL COMMENT 'Cumulated number of items that have been missed.',");
        sqlBuilder.append("`recall` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'tp/(tp+fn) where tp is sum of items found by the algorithm and fn is the sum ");
        sqlBuilder.append("of misses and pending items (that are in the time span between the last simulated poll ");
        sqlBuilder.append("and the end of the benchmark period.'");
        sqlBuilder.append(", PRIMARY KEY (`feedId`)");
        sqlBuilder.append(") ENGINE=MYISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

        final String sql = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        boolean resultFeeds = runUpdate(sql) != -1 ? true : false;

        // create table to write final results to
        sqlBuilder = new StringBuilder();
        tableName = baseTableName + "_avg";
        sqlBuilder.append("CREATE TABLE `");
        sqlBuilder.append(tableName);
        sqlBuilder.append("` (");
        sqlBuilder.append("`mode` CHAR(5) COLLATE utf8_unicode_ci DEFAULT NULL ");
            sqlBuilder.append("COMMENT 'The averaging mode, one of \"feeds\" or \"items\".',");
        sqlBuilder.append("`PPI` DOUBLE DEFAULT NULL COMMENT 'Arithmetic average of polls per newly found item.',");
        sqlBuilder.append("`avgDelayMinutes` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'Arithmetic average delay to a newly found item in minutes.',");
        sqlBuilder.append("`totalMisses` INT DEFAULT NULL COMMENT 'Cumulated number of items that have been missed.',");
        sqlBuilder.append("`recall` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'tp/(tp+fn) where tp is sum of items found by the algorithm and fn is the sum ");
        sqlBuilder.append("of misses and pending items (that are in the time span between the last simulated poll ");
        sqlBuilder.append("and the end of the benchmark period.'");
        sqlBuilder.append(") ENGINE=MYISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

        final String sqlAvg = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sqlAvg);
        }

        boolean resultAvg = runUpdate(sqlAvg) != -1 ? true : false;
        return resultFeeds && resultAvg;
    }




    /**
     * Creates evaluation results totalMisses and (average) recall from the given table.
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @param outputTableName The name of the table to write evaluation data to.
     * @param modeFeeds If <code>true</code>, average over all items per feed and subsequently over all feeds. If
     *            <code>false</code>, directly average over all items.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean generateBasicEvaluationResultsPerStrategyModeFeeds(String sourceTableName) {

        String outputTableName = sourceTableName + "_" + MODE_FEEDS;

        // estimate totalMisses and recall and insert into table
        StringBuilder sqlBuilder = new StringBuilder(); 
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` (feedId, totalMisses, recall)");
        sqlBuilder.append("SELECT ");
        sqlBuilder.append("feedId,");
        sqlBuilder.append("SUM(missedItems) AS 'totalMisses',");
        sqlBuilder.append("SUM(newWindowItems)/(SUM(missedItems)+SUM(newWindowItems)+SUM(pendingItems)) AS 'recall'");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("`");
        sqlBuilder.append("GROUP BY feedId;");

        final String sql = sqlBuilder.toString();
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }

        boolean updated = runUpdate(sql) != -1 ? true : false;
        if (!updated) {
            LOGGER.error("Could not write evaluation data to table " + outputTableName);
            return false;
        }
        return updated;
    }

    /**
     * Creates evaluation results totalMisses and recall from the given table, modus items
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean generateBasicEvaluationResultsPerStrategyModeItems(String sourceTableName) {

        String outputTableName = sourceTableName + AVG_POSTFIX;

        // estimate totalMisses and recall and insert into table
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` (mode, totalMisses, recall) ");
        sqlBuilder.append("SELECT '");
        sqlBuilder.append(MODE_ITEMS);
        sqlBuilder.append("', SUM(missedItems) AS 'totalMisses',");
        sqlBuilder.append("SUM(newWindowItems)/(SUM(missedItems)+SUM(newWindowItems)+SUM(pendingItems)) AS 'recall'");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("`;");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }

        boolean updated = runUpdate(sql) != -1 ? true : false;
        if (!updated) {
            LOGGER.error("Could not write evaluation data to table " + outputTableName);
            return false;
        }
        return updated;
    }


    /**
     * calculate average delay per feed (mode feeds) from the given table and write results to db.<br />
     * <br />
     * e.g.<br />
     * UPDATE `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_feeds_OK` u
     * SET avgDelayMinutes = (
     * SELECT SUM(s.cumulatedDelay)/(60*SUM(s.newWindowItems)) AS 'avgDelay'
     * FROM `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_OK` s
     * WHERE s.cumulatedDelay > 0 AND u.feedId = s.feedId
     * GROUP BY s.feedId
     * );
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setAvgDelayModeFeeds(String sourceTableName) {

        String outputTableName = sourceTableName + "_" + MODE_FEEDS;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` u ");
        sqlBuilder.append("SET avgDelayMinutes = (");
        sqlBuilder.append("SELECT SUM(s.cumulatedDelay)/(60*SUM(s.newWindowItems)) AS 'avgDelay' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` s ");
        sqlBuilder.append("WHERE s.numPollNewItem > 1 ");
        sqlBuilder.append("AND u.feedId = s.feedId ");
        sqlBuilder.append("GROUP BY s.feedId);");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Calculate "averageDelay" averaged over all items (mode items) from the given table and write results to db. <br />
     * <br />
     * e.g.<br />
     * SELECT SUM(cumulatedDelay)/(60*SUM(newWindowItems)) AS 'avgDelay'
     * FROM `eval_fixlearned_min_time_100_2011-11-03_18-40-41`
     * WHERE cumulatedDelay > 0;
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setAvgDelayModeItems(String sourceTableName) {

        String outputTableName = sourceTableName + AVG_POSTFIX;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SET avgDelayMinutes = (");
        sqlBuilder.append("SELECT SUM(cumulatedDelay)/(60*SUM(newWindowItems)) AS 'avgDelay' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` WHERE numPollNewItem > 1)");
        sqlBuilder.append("WHERE MODE LIKE '%");
        sqlBuilder.append(MODE_ITEMS);
        sqlBuilder.append("%';");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Calculate "polls per item" per feed (mode feeds) from the given table and write results to db. <br />
     * <br />
     * e.g.<br />
     * UPDATE `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_feeds_OK` u
     * SET PPI = (
     * SELECT COUNT(*)/SUM(newWindowItems) AS 'PPI'
     * FROM `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_OK` s
     * WHERE NOT (newWindowItems = windowSize AND cumulatedDelay = 0)
     * AND u.feedId = s.feedId
     * GROUP BY s.feedId
     * );
     * 
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result tables have been filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setPPIModeFeeds(String sourceTableName) {

        String outputTableName = sourceTableName + "_" + MODE_FEEDS;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` u ");
        sqlBuilder.append("SET PPI = (");
        sqlBuilder.append("SELECT COUNT(*)/SUM(newWindowItems) AS 'PPI_feeds' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` s ");
        sqlBuilder.append("WHERE NOT (newWindowItems = windowSize AND cumulatedDelay = 0) ");
        sqlBuilder.append("AND u.feedId = s.feedId ");
        sqlBuilder.append("GROUP BY s.feedId);");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Calculate "polls per item" averaged over all items (mode items) from the given table and write results to db. <br />
     * <br />
     * e.g.<br />
     * SELECT COUNT(*)/SUM(newWindowItems) AS 'PPI_polls'
     * FROM `eval_fixlearned_min_time_100_2011-11-03_18-40-41`
     * WHERE NOT (newWindowItems = windowSize AND cumulatedDelay = 0);
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result tables have been filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setPPIModeItems(String sourceTableName) {
        
        String outputTableName = sourceTableName + AVG_POSTFIX;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` u ");
        sqlBuilder.append("SET PPI = (");
        sqlBuilder.append("SELECT COUNT(*)/SUM(newWindowItems) AS 'PPI_feeds' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` s ");
        sqlBuilder.append("WHERE NOT (newWindowItems = windowSize AND cumulatedDelay = 0)) ");
        sqlBuilder.append("WHERE MODE LIKE '%");
        sqlBuilder.append(MODE_ITEMS);
        sqlBuilder.append("%';");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Wrapper method to generate all intermediate and final evaluation results for averaging mode feeds. <br />
     * <br />
     * First, generate results from simulated polls by averaging per feed. Second, read the intermediate results to get
     * an global average over all feeds and write the results to db.
     * 
     * @param baseTableName The name of the table that contains simulated poll data.
     * @return <code>true</code> if result tables have been filled with results, <code>false</code> on any
     *         error.
     */         
    private boolean createModeFeedsSummary(String baseTableName) {
        boolean result = true;

        // generate average values per feed
        result = result && generateBasicEvaluationResultsPerStrategyModeFeeds(baseTableName);
        result = result && setAvgDelayModeFeeds(baseTableName);
        result = result && setPPIModeFeeds(baseTableName);

        // generate global average over all feeds
        String sourceTableName = baseTableName + "_" + MODE_FEEDS;
        String outputTableName = baseTableName + AVG_POSTFIX;
        
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SELECT '");
        sqlBuilder.append(MODE_FEEDS);
        sqlBuilder.append("', AVG(PPI), AVG(avgDelayMinutes), SUM(totalMisses), AVG(recall) ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("WHERE PPI IS NOT NULL AND avgDelayMinutes IS NOT NULL AND recall IS NOT NULL;");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        result = result && runUpdate(sql) != -1 ? true : false;
        return result;
    }

    /**
     * Wrapper method to generate all (final) evaluation results for averaging mode items.
     * 
     * @param baseTableName The name of the table to do the evaluation for.
     * @return <code>true</code> if all results have been written, <code>false</code> on any
     *         error.
     */
    private boolean createModeItemSummary(String baseTableName) {
        boolean result = true;
        result = result && generateBasicEvaluationResultsPerStrategyModeItems(baseTableName);
        result = result && setAvgDelayModeItems(baseTableName);
        result = result && setPPIModeItems(baseTableName);
        return result;
    }

    /**
     * Add index feedId to given table. This may of course take very long in case the given table is large.
     * 
     * @param baseTableName Name of table that contains simulated poll data.
     * @return <code>true</code> if index has been added, <code>false</code> on any error.
     */
    private boolean addIndexToEvalTable(String baseTableName) {
    
        final String sql = replaceTableName(ADD_INDEX_TO_EVALUATION_TABLE, baseTableName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Generate evaluation results average delay, polls per item, (average) recall and total misses for the two
     * averaging modes "feeds" and "items".
     * 
     * @param baseTableName The name of the table to do the evaluation for.
     * @return <code>true</code> if all results have been written, <code>false</code> on any
     *         error.
     */
    public boolean generateEvaluationSummary(String baseTableName) {
        boolean success = true;
        success = success && addIndexToEvalTable(baseTableName);
        success = success && createEvaluationResultTables(baseTableName);
        success = success && createModeFeedsSummary(baseTableName);
        success = success && createModeItemSummary(baseTableName);
        return success;
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
     * Add the provided PolLData to the provided database table.
     * 
     * @param pollData The data of one simulated poll to add.
     * @param feedId The id of the feed the pollData belongs to.
     * @param activityPattern The feed's activityPattern.
     * @param tableName The name of the table to add the information to.
     * @param useQueue If set to <code>true</code>, do not add poll now but put to a queue for batch insert.
     */
    public void addPollData(PollData pollData, int feedId, int activityPattern, String tableName, boolean useQueue) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feedId);
        parameters.add(pollData.getNumberOfPoll());
        parameters.add(pollData.getNumberOfPollWithNewItem());
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
        if (useQueue) {
            addPollDataToQueue(replacedSqlStatement, parameters);
        } else {
            runInsertReturnId(replacedSqlStatement, parameters);
        }

    }

    /**
     * Determine whether {@link #BATCH_INSERT_QUEUE} is full or not. Size of the queue is defined as sum of all insert
     * operation, * i.e. the sum over the sizes of all outer lists of all SQL statements.
     * 
     * @return <code>true</code> if current size of {@link #BATCH_INSERT_QUEUE} equals (or exceeds)
     *         {@link EvaluationFeedDatabase#QUEUE_CAPACITY}
     */
    private boolean isBatchInsertQueueFull() {
        int size = 0;
        for (List<List<Object>> batchArgs : BATCH_INSERT_QUEUE.values()) {
            size += batchArgs.size();
        }
        return size >= QUEUE_CAPACITY;
    }

    /**
     * Add the list of parameters to {@link #BATCH_INSERT_QUEUE}. In case the queue's capacity is reached, all batch
     * inserts are executed. Therefore, the thread that adds the last element has to "pay".
     * 
     * @param sql The sql statement to be executed.
     * @param parameters The parameters to be filled in this statement.
     */
    private synchronized void addPollDataToQueue(String sql, List<Object> parameters) {
        // get current batchArgs for this sql statement
        // this method is required to be synchronized since reading batchArgs, adding own parameters and writing it back
        // to queue is a transaction!
        List<List<Object>> batchArgs = BATCH_INSERT_QUEUE.get(sql);

        // null if sql statement not in queue, create blanc list
        if (batchArgs == null) {
            batchArgs = new ArrayList<List<Object>>();
        }
        batchArgs.add(parameters);
        BATCH_INSERT_QUEUE.put(sql, batchArgs);
        if (isBatchInsertQueueFull()) {
            processBatchInsertQueue();
        }
    }

    /**
     * Process all queued batch inserts and drain queue.
     */
    public synchronized void processBatchInsertQueue() {
        for (String sqlStatement : BATCH_INSERT_QUEUE.keySet()) {
            List<List<Object>> batchArgs = BATCH_INSERT_QUEUE.get(sqlStatement);
            runBatchInsertReturnIds(sqlStatement, batchArgs);
            BATCH_INSERT_QUEUE.remove(sqlStatement);
        }
    }

    /**
     * Reset all rows of table feeds to default values except totalItems, hasVariableWindowSize, activityPattern,
     * isAccessibleFeed, hasItemIds, hasPubDate, hasCloud, ttl, hasSkipHours, hasSkipDays, hasUpdated, hasPublished and
     * supportsPubSubHubBub. <br />
     * <br />
     * After reseting, the local feed collection is reloaded from data base.
     * 
     * @return <code>true</code> if successful, <code>false</code> otherwise.
     */
    public boolean resetTableFeeds() {
        boolean success = runUpdate(RESET_TABLE_FEEDS) != -1 ? true : false;
        feeds = runQuery(new FeedRowConverter(), GET_FEEDS_WITH_TIMESTAMPS);
        return success;
    }


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

    public static void main(String[] args) {
        // final EvaluationFeedDatabase db = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder
        // .getInstance().getConfig());
        // String tableName = "eval_fixLearned_min_time_100_2011-11-04_19-28-03";
        // db.generateEvaluationSummary(tableName);
    }

}
