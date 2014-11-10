package ws.palladian.retrieval.feeds.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedActivityPattern;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.EvaluationFeedItem;
import ws.palladian.retrieval.feeds.evaluation.disssandro_temp.IntervalBoundsEvaluator;
import ws.palladian.retrieval.feeds.evaluation.icwsm2011.PollData;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedEvaluationItemRowConverter;
import ws.palladian.retrieval.feeds.persistence.FeedPollRowConverter;
import ws.palladian.retrieval.feeds.persistence.FeedRowConverter;

/**
 * TUDCS6 specific evaluation code.
 * 
 * @author Sandro Reichert
 */
public class EvaluationFeedDatabase extends FeedDatabase {


    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationFeedDatabase.class);

    private static final String TABLE_REPLACEMENT = "###TABLE_NAME###";

    /**
     * Get all feeds that have item timestamps or no items at all, ORDER BY totalItems DESC to speed up parallel
     * processing.
     */
    private static final String GET_FEEDS_WITH_TIMESTAMPS = "SELECT * FROM feeds WHERE hasPubDate = 1 OR hasUpdated = 1 OR hasPublished = 1 OR totalItems = 0 ORDER BY totalItems DESC";
    private static final String ADD_EVALUATION_ITEMS = "INSERT IGNORE INTO feed_evaluation_items SET feedId = ?, sequenceNumber = ?, pollTimestamp = ?, extendedItemHash = ?, publishTime = ?, correctedPublishTime = ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID = "SELECT * FROM feed_evaluation_items WHERE feedId = ? ORDER BY feedId ASC, sequenceNumber ASC LIMIT ?, ?;";
    private static final String GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_LIMIT = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND correctedPublishTime <= ? ORDER BY sequenceNumber DESC LIMIT 0, ?";
    private static final String GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_RANGE_LIMIT = "SELECT * FROM feed_evaluation_items WHERE feedId = ? AND correctedPublishTime <= ? AND correctedPublishTime >= ? ORDER BY sequenceNumber DESC LIMIT 0, ?";
    private static final String ADD_EVALUATION_POLL = "INSERT IGNORE INTO `###TABLE_NAME###` SET feedId = ?, numberOfPoll = ?, numPollNewItem = ?, activityPattern = ?, sizeOfPoll = ?, pollTimestamp = ?, checkInterval = ?, newWindowItems = ?, missedItems = ?, windowSize = ?, cumulatedDelay = ?, pendingItems = ?, droppedItems = ?";
    /** reset table feeds except activityPattern and blocked. */
    private static final String RESET_TABLE_FEEDS = "UPDATE feeds SET checks = DEFAULT, unreachableCount = DEFAULT, unparsableCount = DEFAULT, misses = DEFAULT, windowSize = DEFAULT, lastPollTime = DEFAULT, lastSuccessfulCheck = DEFAULT, lastMissTimestamp = DEFAULT, lastFeedEntry = DEFAULT, totalProcessingTime = DEFAULT, newestItemHash = DEFAULT, lastETag = DEFAULT, lastModified = DEFAULT, lastResult = DEFAULT, feedFormat = DEFAULT, feedSize = DEFAULT, title = DEFAULT, LANGUAGE = DEFAULT, httpHeaderSize = DEFAULT";

    private static final String TRUNCATE_TABLE_FEEDS = "TRUNCATE TABLE feeds;";

    private static final String GET_NUMBER_MISSED_ITEMS_BY_ID_SEQUENCE_NUMBERS = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND sequenceNumber > ? AND sequenceNumber < ?";

    private static final String GET_NUMBER_PENDING_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ? AND correctedPublishTime <= ?";
    private static final String GET_NUMBER_PRE_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND correctedPublishTime < ?";
    private static final String GET_NUMBER_POST_BENCHMARK_ITEMS_BY_ID = "SELECT count(*) FROM feed_evaluation_items WHERE feedId = ? AND pollTimestamp > ? AND correctedPublishTime > ?";

    private static final String GET_EVALUATION_NEWEST_ITEM_HASHES = "SELECT f1.`feedId` , f1.`extendedItemHash` FROM `feed_evaluation_items` f1 JOIN (SELECT `feedId` , MAX( `sequenceNumber` ) AS maxsn FROM `feed_evaluation_items` GROUP BY `feedId`) AS f2 ON f1.`feedId` = f2.`feedId` AND f1.`sequenceNumber` = f2.maxsn";

    private static final String OPTIMIZE_TABLE = "OPTIMIZE TABLE `###TABLE_NAME###`;";
    
    private static final String ADD_INDEX_TO_EVALUATION_TABLE = "ALTER TABLE `###TABLE_NAME###` ADD INDEX `feedId_idx` (`feedId`);";

    private static final String ADD_SINGLE_DELAY = "INSERT IGNORE INTO `###TABLE_NAME###` SET feedId = ?, singleDelay = ?";

    private static final String GET_TRANSFER_VOLUME_BY_STRATEGY = "SELECT CEIL(TIMESTAMPDIFF(HOUR,'2011-07-16 07:00:00',pollTimestamp)) AS 'hourOfExperiment', CEIL(SUM(sizeOfPoll)/1048576) AS 'hourlyVolumeMB' FROM `###TABLE_NAME###` GROUP BY CEIL(TIMESTAMPDIFF(HOUR,'2011-07-16 07:00:00',pollTimestamp));";

    private static final String CREATE_INTERVAL_BOUNDS_TABLE = "CREATE TABLE `###TABLE_NAME###` (`feedId` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT COMMENT 'The feeds internal identifier.', PRIMARY KEY (`feedId`)) ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";
    
    private static final String UPDATE_FEED_POLL = "UPDATE feed_polls SET httpETag = ?, httpDate = ?, httpLastModified = ?, httpExpires = ?, newestItemTimestamp = ?, numberNewItems = ?, windowSize = ?, httpStatusCode = ?, responseSize = ? WHERE id = ? AND pollTimestamp = ?";
    
    private static final String GET_PREVIOUS_FEED_POLL_BY_ID_AND_TIME = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp < ? ORDER BY pollTimestamp DESC LIMIT 0,1";
    
    private static final String GET_PREVIOUS_OR_EQUAL_FEED_POLL_BY_ID_AND_TIMERANGE = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp <= ? AND pollTimestamp >= ? ORDER BY pollTimestamp DESC LIMIT 0,1";
    
    private static final String GET_PREVIOUS_OR_EQUAL_FEED_POLL_BY_ID_AND_TIME = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp <= ? ORDER BY pollTimestamp DESC LIMIT 0,1";
    
    private static final String GET_FEED_POLL_BY_ID_TIMESTAMP = "SELECT * FROM feed_polls WHERE id = ? AND pollTimestamp = ?";
    
    private static final String GET_FEED_POLLS_BY_ID = "SELECT * FROM feed_polls WHERE id = ?";

    /**
     * It is assumed that table feeds_TUDCS6 contains all feeds and their meta data.
     */
    private static final String COPY_FEEDS_INTERVAL_BOUNDS = "INSERT INTO feeds SELECT * FROM feeds_TUDCS6 WHERE id IN (SELECT feedId FROM `###TABLE_NAME###`);";

    private static final String RESTORE_ALL_FEEDS_FROM_BACKUP = "INSERT INTO feeds SELECT * FROM feeds_TUDCS6";
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
     * Used as postfix for table names; table contains the delay to each item except the ones in the first poll
     */
    private static final String DELAY_POSTFIX = "_delays";

    /**
     * Used as postfix for table names; table contains the number of initial items per feed.
     */
    private static final String INITIAL_ITEMS_POSTFIX = "_init";

    /**
     * All feeds from database that do have item timestamps or no items at all, ORDER BY totalItems DESC to speed up
     * parallel processing.
     */
    private List<Feed> feeds;

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
    private final ConcurrentHashMap<Integer, List<EvaluationFeedItem>> CACHED_NEWEST_WINDOWS = new ConcurrentHashMap<Integer, List<EvaluationFeedItem>>();

    /**
     * Capacity of {@link #BATCH_INSERT_QUEUE}. Size of the queue is defined as sum of all insert operation, i.e. the
     * sum
     * over the sizes of all outer lists of all SQL statements.
     */
    private static final int QUEUE_CAPACITY = 1000;
    
    protected EvaluationFeedDatabase(DataSource dataSource) {
        super(dataSource);
        feeds = runQuery(FeedRowConverter.INSTANCE, GET_FEEDS_WITH_TIMESTAMPS);
        initializeNewestItemHashes();
    }

    /**
     * Initialize/load {@link #NEWEST_ITEM_HASHES} from db.
     */
    private void initializeNewestItemHashes() {

        RowConverter<EvaluationFeedItem> converter = new RowConverter<EvaluationFeedItem>() {
            @Override
            public EvaluationFeedItem convert(ResultSet resultSet) throws SQLException {

                EvaluationFeedItem item = new EvaluationFeedItem();

                item.setFeedId(resultSet.getInt("feedId"));
                item.setHash(resultSet.getString("extendedItemHash"));

                return item;

            }
        };

        List<EvaluationFeedItem> newestHashes = runQuery(converter, GET_EVALUATION_NEWEST_ITEM_HASHES);

        for (EvaluationFeedItem item : newestHashes) {
            NEWEST_ITEM_HASHES.put(item.getFeedId(), item.getHash());
        }

    }

    /**
     * Explicitly reload the local feed collection from database. This is required only in case the database has been
     * modified, e.g., by {@link EvaluationFeedDatabase#copySimulatedSingleDelays(String, String, String)}.
     */
    public void reloadFeedsFromDB() {
        feeds = runQuery(FeedRowConverter.INSTANCE, GET_FEEDS_WITH_TIMESTAMPS);
    }

    /**
     * @return All feeds from database that do have item timestamps or no items at all, ORDER BY totalItems DESC to
     *         speed up parallel processing.
     */
    public List<Feed> getFeedsWithTimestamps() {
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
            parameters.add(SqlHelper.getTimestamp(item.getPollTimestamp()));

            // generate extended item hash
            String pollTime = DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", item.getPollTimestamp().getTime());
            String extendetItemHash = pollTime + "_" + item.getHash();
            parameters.add(extendetItemHash);
            parameters.add(SqlHelper.getTimestamp(item.getPublished()));
            parameters.add(SqlHelper.getTimestamp(item.getCorrectedPublishedDate()));
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
            Timestamp correctedPublishTime, Date correctedPublishTimeLowerBound, int window) {

        // try to get simulated window from local cache
        List<EvaluationFeedItem> simulatedWindow = getSimulatedWindowFromCache(feedId);

        // if we didn't found it, load it from db and cache the response.
        if (simulatedWindow == null) {

            simulatedWindow = runQuery(new FeedEvaluationItemRowConverter(),
                    GET_EVALUATION_ITEMS_BY_ID_CORRECTED_PUBLISH_TIME_RANGE_LIMIT, feedId, correctedPublishTime,
                    SqlHelper.getTimestamp(correctedPublishTimeLowerBound), window);

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
     * Creates two tables to write evaluation data into. In the base table, a row represents one simulated poll.
     * 
     * @param The base name of the table to create.
     * @return <code>true</code> tables have been successfully created, <code>false</code> otherwise.
     */
    public boolean createEvaluationBaseTable(String evaluationTableName) {
        boolean success = true;
        final String baseSQL = "CREATE TABLE `"
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
            LOGGER.debug(baseSQL);
        }

        success = runUpdate(baseSQL) != -1 ? true : false;

        final String delaysSQL = "CREATE TABLE `"
                + evaluationTableName + DELAY_POSTFIX
                + "` ("
                + "`feedId` INT(10) UNSIGNED NOT NULL,"
                + "`singleDelay` DOUBLE NOT NULL COMMENT 'Delay to a single item in seconds.'"
                + ") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";

        success = success && runUpdate(delaysSQL) != -1 ? true : false;
        return success;
    }

    /**
     * Add a set of delays in seconds to single items by a feed.
     * 
     * @param feedId The feed the delays belong to.
     * @param delays The delays in second to different items.
     * @param tableName The name of the table to add the information to.
     * @param useQueue If set to <code>true</code>, do not add poll now but put to a queue for batch insert.
     */
    public void addSingleDelaysByFeed(int feedId, Collection<Long> delays, String tableName, boolean useQueue) {

        final String replacedSqlStatement = replaceTableName(ADD_SINGLE_DELAY, tableName + DELAY_POSTFIX);
        List<List<Object>> batchArgs = new ArrayList<List<Object>>();

        for (Long delay : delays) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feedId);
            parameters.add(delay);

            if (useQueue) {
                addPollDataToQueue(replacedSqlStatement, parameters);
            } else {
                batchArgs.add(parameters);
            }
        }
        if (!useQueue) {
            runBatchInsertReturnIds(replacedSqlStatement, batchArgs);
        }
    }

    /**
     * Create two tables to write evaluation results such as average delay or polls per item of a single strategy to.
     * 
     * 
     * @param baseTableName The base name of the tables to create.
     * @return <code>true</code> if both tables have been successfully created, <code>false</code> otherwise.
     */
    private boolean createEvaluationResultTables(String baseTableName) {
    
        LOGGER.info("Creating evaluation result tables.");

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
        sqlBuilder.append("`medianDelayMinutes` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'Median delay to a newly found item in minutes.',");
        sqlBuilder.append("`totalMisses` INT DEFAULT NULL COMMENT 'Cumulated number of items that have been missed.',");
        sqlBuilder.append("`recall` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'tp/(tp+fn) where tp is sum of items found by the algorithm and fn is the sum ");
        sqlBuilder.append("of misses and pending items (that are in the time span between the last simulated poll ");
        sqlBuilder.append("and the end of the benchmark period.'");
        sqlBuilder.append(", PRIMARY KEY (`feedId`)");
        sqlBuilder.append(") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

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
        sqlBuilder.append("`medianDelayMinutes` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'Median delay to a newly found item in minutes. ");
        sqlBuilder.append("In mode feeds, this is the median of medians per feed.',");
        sqlBuilder.append("`totalMisses` INT DEFAULT NULL COMMENT 'Cumulated number of items that have been missed.',");
        sqlBuilder.append("`recall` DOUBLE DEFAULT NULL ");
        sqlBuilder.append("COMMENT 'tp/(tp+fn) where tp is sum of items found by the algorithm and fn is the sum ");
        sqlBuilder.append("of misses and pending items (that are in the time span between the last simulated poll ");
        sqlBuilder.append("and the end of the benchmark period.'");
        sqlBuilder.append(") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");

        final String sqlAvg = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sqlAvg);
        }

        boolean resultAvg = runUpdate(sqlAvg) != -1 ? true : false;
        return resultFeeds && resultAvg;
    }

    /**
     * Creates evaluation results totalMisses from the given table.
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @param outputTableName The name of the table to write evaluation data to.
     * @param modeFeeds If <code>true</code>, average over all items per feed and subsequently over all feeds. If
     *            <code>false</code>, directly average over all items.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean generateBasicEvaluationResultsPerStrategyModeFeeds(String sourceTableName) {

        LOGGER.info("Calculating totalMisses in mode feeds.");

        String outputTableName = sourceTableName + "_" + MODE_FEEDS;

        // estimate totalMisses and recall and insert into table
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` (feedId, totalMisses)");
        sqlBuilder.append("SELECT ");
        sqlBuilder.append("feedId,");
        sqlBuilder.append("SUM(missedItems) AS 'totalMisses' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` ");
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
     * Creates evaluation results totalMisses from the given table, modus items
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean generateBasicEvaluationResultsPerStrategyModeItems(String sourceTableName) {

        LOGGER.info("Calculating totalMisses in mode items.");

        String outputTableName = sourceTableName + AVG_POSTFIX;

        // estimate totalMisses and recall and insert into table
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` (mode, totalMisses) ");
        sqlBuilder.append("SELECT '");
        sqlBuilder.append(MODE_ITEMS);
        sqlBuilder.append("', SUM(missedItems) AS 'totalMisses' ");
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
     * Create a table that contains the number of initial items per feed.<br />
     * <br />
     * e.g.<br />
     * Create temp table to count initial items per feed.<br />
     * CREATE TABLE temp_initial2 AS
     * SELECT feedId, newWindowItems AS 'initial'
     * FROM `z_eval_AdaptiveTTL_4.0_1_40320_2011-11-27_21-31-14`
     * WHERE numPollNewItem = 1
     * GROUP BY feedId;
     * 
     * ALTER TABLE temp_initial2 ADD PRIMARY KEY (`feedId`);
     * 
     * @param sourceTableName
     * @return <code>true</code> if table has been created and indexed, <code>false</code> on any error.
     */
    private boolean createInitialItemsTempTable(String sourceTableName) {
        boolean success = true;

        // create temptable to count initial items per feed
        String tempTableName = sourceTableName + INITIAL_ITEMS_POSTFIX;
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE `");
        sqlBuilder.append(tempTableName);
        sqlBuilder.append("` AS ");
        sqlBuilder.append("SELECT feedId, newWindowItems AS 'initial' FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` WHERE numPollNewItem = 1 GROUP BY feedId;");

        String sql = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        success = success && runUpdate(sql) != -1 ? true : false;

        // add index to temptable
        sqlBuilder = new StringBuilder();
        sqlBuilder.append("ALTER TABLE `");
        sqlBuilder.append(tempTableName);
        sqlBuilder.append("` ADD PRIMARY KEY (`feedId`);");

        sql = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        success = success && runUpdate(sql) != -1 ? true : false;
        return success;
    }

    /**
     * calculate recall per feed (mode feeds) from the given table and write results to db.<br />
     * <br />
     * e.g.<br />
     * Calculate recall ignoring initial items .<br />
     * CREATE TABLE test7 AS
     * SELECT a.feedId,
     * (SUM(a.newWindowItems)-b.initial)/(SUM(a.missedItems)+SUM(a.newWindowItems)+SUM(a.pendingItems)-b.initial) AS
     * 'Recall'
     * FROM `z_eval_AdaptiveTTL_4.0_1_40320_2011-11-27_21-31-14` a, temp_initial2 b
     * WHERE a.feedId = b.feedId
     * GROUP BY a.feedId;
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setRecallModeFeeds(String sourceTableName) {
        LOGGER.info("Calculating recall in mode feeds.");

        String feedsTableName = sourceTableName + "_" + MODE_FEEDS;
        String tempTableName = sourceTableName + INITIAL_ITEMS_POSTFIX;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(feedsTableName);
        sqlBuilder.append("` u ");
        sqlBuilder.append("SET Recall = (");
        sqlBuilder.append("SELECT (SUM(a.newWindowItems)-b.initial)/");
        sqlBuilder.append("(SUM(a.missedItems) + SUM(a.newWindowItems) + SUM(a.pendingItems) - b.initial) ");
        sqlBuilder.append("AS 'Recall' ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` a, `");
        sqlBuilder.append(tempTableName);
        sqlBuilder.append("` b ");
        sqlBuilder.append("WHERE u.feedId = a.feedId AND a.feedId = b.feedId ");
        sqlBuilder.append("GROUP BY a.feedId);");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }

        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Calculate "recall" in mode items and update evaluation summary table xx_avg. <br />
     * <br />
     * e.g.<br />
     * UPDATE `z_eval_AdaptiveTTL_4.0_1_40320_2011-11-27_21-31-14_avg` u
     * SET u.recall = (SELECT (SUM(newWindowItems)-(SELECT SUM(initial) FROM temp_initial))/
     * (SUM(missedItems)+SUM(newWindowItems)+SUM(pendingItems)-(SELECT SUM(initial) FROM temp_initial)) AS 'Recall'
     * FROM `z_eval_AdaptiveTTL_4.0_1_40320_2011-11-27_21-31-14`)
     * WHERE MODE LIKE '%items%';
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setRecallModeItems(String sourceTableName) {

        LOGGER.info("Calculating avgDelay in mode items.");

        String outputTableName = sourceTableName + AVG_POSTFIX;
        String initialItemsTableName = sourceTableName + INITIAL_ITEMS_POSTFIX;
        String subQuery = "(SELECT SUM(initial) FROM `" + initialItemsTableName + "`)";

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SET recall = (");
        sqlBuilder.append("SELECT (SUM(newWindowItems)-");
        sqlBuilder.append(subQuery);
        sqlBuilder.append(") / (SUM(missedItems)+SUM(newWindowItems)+SUM(pendingItems)-");
        sqlBuilder.append(subQuery);
        sqlBuilder.append(") as 'Recall' FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("`)");
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

        LOGGER.info("Calculating avgDelayMinutes in mode feeds.");
        
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

        LOGGER.info("Calculating avgDelay in mode items.");

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
     * Calculate "medianDelay", the median of all items (mode items) from the given table and write results to db. <br />
     * <br />
     * Median is based on http://dev.mysql.com/doc/refman/5.0/en/group-by-functions.html, see postings by Terry Woods on
     * October 14 2009 10:49pm and bugfix by Tom Van Vleck on January 29 2010 8:47pm
     * 
     * @param baseTableName The name of the table that contains the simulated poll data. Name is used to get names of
     *            other tables that contain the delays and average values.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setMedianDelayModeItems(String baseTableName) {

        LOGGER.info("Calculating median delay in mode items.");

        String outputTableName = baseTableName + AVG_POSTFIX;
        String delayTableName = baseTableName + DELAY_POSTFIX;

        // make sure to set variables on same connection where they are used.
        final String initializeVariablesSQL = "SET @rownum:=0;";

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("update `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SET medianDelayMinutes = (");
        sqlBuilder.append("SELECT AVG(a.singleDelay)/60 ");
        sqlBuilder.append("FROM (SELECT @rownum:=@rownum+1 AS rownum,singleDelay ");
        sqlBuilder.append("FROM (SELECT @rownum:=0) r, `");
        sqlBuilder.append(delayTableName);
        sqlBuilder.append("` ORDER BY singleDelay) a, ");
        sqlBuilder.append("(SELECT 0.5+COUNT(*)/2 median FROM `");
        sqlBuilder.append(delayTableName);
        sqlBuilder.append("`) b ");
        sqlBuilder.append("WHERE a.rownum BETWEEN (b.median - 0.5) AND (b.median +0.5)) ");
        sqlBuilder.append("WHERE MODE LIKE '%");
        sqlBuilder.append(MODE_ITEMS);
        sqlBuilder.append("%';");

        final String getMedianSQL = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMedianSQL);
        }
        return runTwoUpdates(initializeVariablesSQL, getMedianSQL) != -1 ? true : false;
    }

    /**
     * Calculate "medianDelay" per feed, the median delay of all items that belong to a single feed write results to db. <br />
     * <br />
     * Median is based on http://dev.mysql.com/doc/refman/5.0/en/group-by-functions.html, see posting by Alexey
     * Kruchinin on June 12 2010 6:57pm
     * 
     * @param baseTableName The name of the table that contains the simulated poll data. Name is used to get names of
     *            other tables that contain the delays and average values.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    @SuppressWarnings("unused")
    private boolean setMedianDelayPerFeed(String baseTableName) {
        boolean success = true;

        LOGGER.info("Calculating median delay per feed.");

        // everything is done in 4 steps
        // 1: create temp table to store medians per feed
        // 2: calculate median delay per feed from delay table and insert results into temporary table
        // 3: update table with values per feed with the ones written to temporary table
        // 4: clean up, drop temporary table

        final String medianTable = baseTableName + "_medians";
        final String delayTable = baseTableName + DELAY_POSTFIX;
        final String feedsTable = baseTableName + "_" + MODE_FEEDS;
        
        // create temporary table to store all median delays to
        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("CREATE TABLE `");
        sqlBuilder.append(medianTable);
        sqlBuilder.append("` (");
        sqlBuilder.append("`feedId` INT(10) UNSIGNED NOT NULL, ");
        sqlBuilder.append("`medianDelay` DOUBLE NOT NULL COMMENT 'median delay in seconds of items of this feed.' ");
        sqlBuilder.append(") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;");
        
        final String createTableSQL = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(createTableSQL);
        }
        success = success && runUpdate(createTableSQL) != -1 ? true : false;
        

        // calculate median delay per feed from delay table and insert results into temporary table
        // make sure to set variables on same connection where they are used.
        final String initializeVariablesSQL = "SET @myvar:=0, @rownum:=0;";

        sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(medianTable);
        sqlBuilder.append("` ");
        sqlBuilder.append("SELECT result.feedId, AVG(singleDelay)/60 AS median ");
        sqlBuilder.append("FROM (SELECT middle_rows.feedId, numerated_rows.rownum, numerated_rows.singleDelay ");
        sqlBuilder.append("FROM (SELECT IF(@myvar = feedId, @rownum := @rownum + 1, @rownum := 0) AS rownum, ");
        sqlBuilder.append("@myvar := feedId AS feedId_alias, singleDelay ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(delayTable);
        sqlBuilder.append("` ORDER BY feedId, singleDelay ");
        sqlBuilder.append(") numerated_rows, ");
        sqlBuilder.append("(SELECT feedId, COUNT(*)/2 median FROM `");
        sqlBuilder.append(delayTable);
        sqlBuilder.append("` GROUP BY feedId ");
        sqlBuilder.append(") middle_rows ");
        sqlBuilder.append("WHERE numerated_rows.rownum BETWEEN ");
        sqlBuilder.append("(middle_rows.median - IF( median = ROUND(median) , 1, 0 ) - 0.5) ");
        sqlBuilder.append("AND (middle_rows.median - IF( median = ROUND(median) , 0, 0.5 )) ");
        sqlBuilder.append("AND numerated_rows.feedId_alias = middle_rows.feedId ");
        sqlBuilder.append(") result GROUP BY feedId; ");

        final String getMediansSQL = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMediansSQL);
        }
        success = success && runTwoUpdates(initializeVariablesSQL, getMediansSQL) != -1 ? true : false;
        
        
        // update table with values per feed with the ones written to temporary table
        sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE `");
        sqlBuilder.append(feedsTable);
        sqlBuilder.append("` d ");
        sqlBuilder.append("SET medianDelayMinutes = ( ");
        sqlBuilder.append("SELECT medianDelay FROM `");
        sqlBuilder.append(medianTable);
        sqlBuilder.append("` m WHERE m.feedId = d.feedId);");

        final String updateFeedsSQL = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(updateFeedsSQL);
        }
        success = success && runUpdate(updateFeedsSQL) != -1 ? true : false;

        // clean up, drop temporary table
        sqlBuilder = new StringBuilder();
        sqlBuilder.append("DROP TABLE `");
        sqlBuilder.append(medianTable);
        sqlBuilder.append("`;");

        final String dropMediansSQL = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(dropMediansSQL);
        }
        success = success && runUpdate(dropMediansSQL) != -1 ? true : false;

        return success;
    }

    /**
     * Calculate median of all median delays per feed and update table with average values. <br />
     * <br />
     * Median is based on http://dev.mysql.com/doc/refman/5.0/en/group-by-functions.html, see postings by Terry Woods on
     * October 14 2009 10:49pm and bugfix by Tom Van Vleck on January 29 2010 8:47pm
     * 
     * @param baseTableName The name of the table that contains the simulated poll data. Name is used to get names of
     *            other tables that contain the delays and average values.
     * @return <code>true</code> if result table has been created and filled with results, <code>false</code> on any
     *         error.
     */
    @SuppressWarnings("unused")
    private boolean setMedianDelayModeFeeds(String baseTableName) {

        LOGGER.info("Calculating median delay from all medians per feed in mode feeds.");

        String outputTableName = baseTableName + AVG_POSTFIX;
        String feedsTableName = baseTableName + "_" + MODE_FEEDS;

        // make sure to set variables on same connection where they are used.
        final String initializeVariablesSQL = "SET @rownum:=0;";

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SET medianDelayMinutes = (");
        sqlBuilder.append("SELECT AVG(a.medianDelayMinutes) ");
        sqlBuilder.append("FROM (SELECT @rownum:=@rownum+1 AS rownum, medianDelayMinutes ");
        sqlBuilder.append("FROM (SELECT @rownum:=0) r, `");
        sqlBuilder.append(feedsTableName);
        sqlBuilder.append("`  WHERE medianDelayMinutes IS NOT NULL ORDER BY medianDelayMinutes) a, ");
        sqlBuilder.append("(SELECT 0.5+COUNT(*)/2 median FROM `");
        sqlBuilder.append(feedsTableName);
        sqlBuilder.append("` WHERE medianDelayMinutes IS NOT NULL) b ");
        sqlBuilder.append("WHERE a.rownum BETWEEN (b.median - 0.5) AND (b.median +0.5) ");
        sqlBuilder.append(") WHERE MODE LIKE '%");
        sqlBuilder.append(MODE_FEEDS);
        sqlBuilder.append("%';");

        final String getMedianSQL = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(getMedianSQL);
        }
        return runTwoUpdates(initializeVariablesSQL, getMedianSQL) != -1 ? true : false;
    }

    /**
     * <p>
     * Quick'n'dirty helper to run two update operations on same connection.
     * </p>
     * 
     * @param sql1 First statement to execute. Must not contain parameter markers.
     * @param sql2 Second statement to execute. Must not contain parameter markers.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runTwoUpdates(String sql1, String sql2) {

        int affectedRows;
        Connection connection = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;

        try {
            connection = getConnection();
            ps1 = connection.prepareStatement(sql1);
            // fillPreparedStatement(ps1, args);
            affectedRows = ps1.executeUpdate();

            ps2 = connection.prepareStatement(sql2);
            affectedRows += ps2.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error("Could not update sql \"" + sql1 + "\" or sql \"" + sql2 + "\", error: " + e.getMessage());
            affectedRows = -1;
        } finally {
            close(connection, ps1);
            close(null, ps2);
        }

        return affectedRows;
    }

    /**
     * Calculate "polls per item" per feed (mode feeds) from the given table and write results to db. <br />
     * <br />
     * e.g.<br />
     * UPDATE `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_feeds_OK` u
     * SET PPI = (
     * SELECT COUNT(*)/SUM(newWindowItems) AS 'PPI'
     * FROM `feed_eval_fix1440_min_time_100_2011-10-28_22-09-45_OK` s
     * WHERE (numPollNewItem > 1 OR numPollNewItem IS NULL)
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

        LOGGER.info("Calculating PPI in mode feeds.");

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
        sqlBuilder.append("WHERE (numPollNewItem > 1 OR numPollNewItem IS NULL) ");
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
     * WHERE numPollNewItem > 1 OR numPollNewItem IS NULL;
     * 
     * @param sourceTableName The name of the table to read simulated poll data from.
     * @return <code>true</code> if result tables have been filled with results, <code>false</code> on any
     *         error.
     */
    private boolean setPPIModeItems(String sourceTableName) {

        LOGGER.info("Calculating PPI in mode items.");

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
        sqlBuilder.append("WHERE numPollNewItem > 1 OR numPollNewItem IS NULL) ");
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
     * Generate average for PPI, avgDelay, recall and sum total misses over all feeds for one strategy.
     * 
     * @param baseTableName The name of the table that contains simulated poll data.
     * @return <code>true</code> if table with average values has been updated, <code>false</code> on any error.
     */
    private boolean createPerStrategyAveragesModeFeeds(String baseTableName) {

        LOGGER.info("Calculating global average for PPI, avgDelay recall and sum total misses over all feeds.");
        boolean success = true;

        // generate global average for PPI, avgDelay and sum total misses over all feeds
        String feedsTableName = baseTableName + "_" + MODE_FEEDS;
        String outputTableName = baseTableName + AVG_POSTFIX;

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("SELECT '");
        sqlBuilder.append(MODE_FEEDS);
        sqlBuilder.append("', AVG(PPI), AVG(avgDelayMinutes), null, SUM(totalMisses), null ");
        sqlBuilder.append("FROM `");
        sqlBuilder.append(feedsTableName);
        sqlBuilder.append("` ");
        sqlBuilder.append("WHERE PPI IS NOT NULL AND avgDelayMinutes IS NOT NULL;");

        String sql = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        success = success && runUpdate(sql) != -1 ? true : false;

        // set recall
        sqlBuilder = new StringBuilder();
        sqlBuilder.append("UPDATE `");
        sqlBuilder.append(outputTableName);
        sqlBuilder.append("` u ");
        sqlBuilder.append("SET recall = (SELECT AVG(recall) FROM `");
        sqlBuilder.append(feedsTableName);
        sqlBuilder.append("` s)");
        sqlBuilder.append("WHERE MODE LIKE '%");
        sqlBuilder.append(MODE_FEEDS);
        sqlBuilder.append("%';");

        sql = sqlBuilder.toString();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        success = success && runUpdate(sql) != -1 ? true : false;

        return success;
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

        // generate average values per feed, they are added to the same table
        result = result && generateBasicEvaluationResultsPerStrategyModeFeeds(baseTableName);
        result = result && setRecallModeFeeds(baseTableName);
        result = result && setAvgDelayModeFeeds(baseTableName);
        result = result && setPPIModeFeeds(baseTableName);

        // this is deactivated since it takes years to compute...
        // adding an index to table _delays dosn't solve the problem
        // result = result && setMedianDelayPerFeed(baseTableName);

        // generate average for PPI, avgDelay recall and sum total misses over all feeds
        result = result && createPerStrategyAveragesModeFeeds(baseTableName);

        // finally, calculate median delay in mode feeds
        // result = result && setMedianDelayModeFeeds(baseTableName);

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
        result = result && setMedianDelayModeItems(baseTableName);
        result = result && setPPIModeItems(baseTableName);
        result = result && setRecallModeItems(baseTableName);
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
        LOGGER.info("Adding index to table " + baseTableName);
        return runUpdate(sql) != -1 ? true : false;
    }

    /**
     * Run SQL optimize on table. This can give a dramatically performance boost if large amounts if data have been
     * written to more than one innodb table in parallel and table has many fragments. <br />
     * Example: Evaluating TUDCS6, two tables were filled with data in parallel, one had 85 million entries, the other
     * one 10 million. After optimizing, speedup was 100 (!) for a query with sum(), avg() and group by.
     * 
     * @param tableName The name of the table to optimize.
     * @return
     */
    private boolean optimizeTable(String tableName) {
        final String sql = replaceTableName(OPTIMIZE_TABLE, tableName);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }

        LOGGER.info("Optimizing table " + tableName);
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
        // make sure all data is written to db
        processBatchInsertQueue();

        // do some optimization
        success = success && optimizeTable(baseTableName);
        success = success && optimizeTable(baseTableName + DELAY_POSTFIX);
        success = success && addIndexToEvalTable(baseTableName);

        // create summary
        success = success && createEvaluationResultTables(baseTableName);
        success = success && createInitialItemsTempTable(baseTableName);
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
    public void addPollData(PollData pollData, int feedId, FeedActivityPattern activityPattern, String tableName, boolean useQueue) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feedId);
        parameters.add(pollData.getNumberOfPoll());
        parameters.add(pollData.getNumberOfPollWithNewItem());
        parameters.add(activityPattern.getIdentifier());
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
        feeds = runQuery(FeedRowConverter.INSTANCE, GET_FEEDS_WITH_TIMESTAMPS);
        return success;
    }

    /**
     * Truncate table feeds.
     * 
     * @return <code>true</code> if successful, <code>false</code> otherwise.
     */
    public boolean truncateTableFeeds() {
        return runUpdate(TRUNCATE_TABLE_FEEDS) != -1 ? true : false;
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
    public int getNumberOfPendingItems(int feedId, Date newestPollTime, Date lastFeedEntry) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_PENDING_ITEMS_BY_ID, feedId, SqlHelper.getTimestamp(newestPollTime),
                SqlHelper.getTimestamp(lastFeedEntry), new Timestamp(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND));

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
     * Usually, {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} is set a (couple of hours) later than the
     * creation of the dataset was started. Therefore, for some feeds we have more than one window at the first
     * simulated poll.<br />
     * Detail: Get the number of items that have a publish date older than oldestFeedEntry
     * 
     * @param feedId The feed to get the information for.
     * @param oldestFeedEntry The timestamp of the oldest entry in the first simulated poll.
     * @return The number of items prior to the first simulated poll.
     */
    public int getNumberOfPreBenchmarkItems(int feedId, Date oldestFeedEntry) {
        RowConverter<Integer> converter = new RowConverter<Integer>() {

            @Override
            public Integer convert(ResultSet resultSet) throws SQLException {
                return resultSet.getInt(1);
            }
        };

        Integer numItems = runSingleQuery(converter, GET_NUMBER_PRE_BENCHMARK_ITEMS_BY_ID, feedId, SqlHelper.getTimestamp(oldestFeedEntry));

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

    /**
     * Load the hourly transfer volume from the given table.
     * 
     * @param tableName The table to load data from
     * @return An array containing the our of the experiment as index and as object the respective data volume in Mb
     *         that has been transferred in this hour
     */
    public int[] getTransferVolumePerHour(String tableName, int totalExperimentHours) {
        // using hourOfExperiment as index and transferred volume as value
        int[] volumes = new int[totalExperimentHours];

        RowConverter<int[]> converter = new RowConverter<int[]>() {

            @Override
            public int[] convert(ResultSet resultSet) throws SQLException {
                // store hourly data as hourOfDay, newItems, observationPeriod
                int[] hourlyData = new int[2];

                hourlyData[0] = resultSet.getInt("hourOfExperiment");
                hourlyData[1] = resultSet.getInt("hourlyVolumeMB");

                return hourlyData;
            }
        };

        List<int[]> hourlyData = runQuery(converter, replaceTableName(GET_TRANSFER_VOLUME_BY_STRATEGY, tableName));
        for (int[] oneHour : hourlyData) {
            volumes[oneHour[0]] = oneHour[1];
        }
        return volumes;
    }

    /**
     * Create a table to store feed ids in that need to be processed in a particular run of
     * {@link IntervalBoundsEvaluator}
     * 
     * @param tableName The name of the table to create.
     * @return <code>true</code> if table has been successfully created, <code>false</code> otherwise.
     */
    public boolean createIntervalBoundsTable(String tableName) {
        return runUpdate(replaceTableName(CREATE_INTERVAL_BOUNDS_TABLE, tableName)) != -1 ? true : false;
    }

    /**
     * Determine all feeds in sourceTableName with a checkInterval > upperBound OR upperBound < lowerBound and add them
     * to intervalBoundsTable. These feeds need to be processed by {@link IntervalBoundsEvaluator}
     * 
     * @param intervalBoundsTable Name of the table to write feed ids to. Should contain update strategy name and
     *            interval bounds
     * @param sourceTableName Name of the table to read data from. It is intended that this table contains data
     *            generated by the same update strategy, but without interval bounds set (lower bound might have been 1
     *            minute).
     * @param lowerBound The lower bound currently used by an update strategy.
     * @param upperBound The upper bound currently used by an update strategy.
     * @return The number of feeds inserted.
     */
    public int determineFeedsToProcess(String intervalBoundsTable, String sourceTableName, int lowerBound, int upperBound) {

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(intervalBoundsTable);
        sqlBuilder.append("` SELECT DISTINCT(feedId) FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` WHERE checkInterval > ");
        sqlBuilder.append(upperBound);
        sqlBuilder.append(" OR checkInterval < ");
        sqlBuilder.append(lowerBound);
        sqlBuilder.append(";");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql);
    }

    /**
     * Fill table 'feeds' with all feeds in intervalBoundsTable
     * 
     * @param intervalBoundsTable Name of the table to read the feed ids from.
     * @return The number of feeds inserted into table 'feeds'.
     */
    public int copyFeedsToProcess(String intervalBoundsTable) {
        return runUpdate(replaceTableName(COPY_FEEDS_INTERVAL_BOUNDS, intervalBoundsTable));
    }

    /**
     * Insert all feeds from feeds_tudcs6 into table feeds, used as a clean-up.
     * 
     * @return The number of feeds inserted into table 'feeds'.
     */
    public int restoreFeedsFromBackup() {
        return runUpdate(RESTORE_ALL_FEEDS_FROM_BACKUP);
    }

    /**
     * Copy simulated polls from sourceTableName to targetTableName, ignoring feed ids contained in intervalBoundsTable.
     * 
     * @param targetTableName Name of table to copy simulated poll data to.
     * @param sourceTableName Name of table to copy simulated poll data from.
     * @param intervalBoundsTable Name of table that contains feed ids to skip.
     * @return The number of simulated single polls inserted into table targetTableName.
     */
    public int copySimulatedPollData(String targetTableName, String sourceTableName, String intervalBoundsTable) {

        // INSERT INTO `<newTable>` SELECT * FROM `z_eval_MAVSync_min_time_100_2011-11-13_00-56-23` WHERE feedId NOT IN
        // (SELECT feedId FROM MAVSync_Todo_10080);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(targetTableName);
        sqlBuilder.append("` SELECT * FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append("` WHERE feedId NOT IN (SELECT feedId FROM `");
        sqlBuilder.append(intervalBoundsTable);
        sqlBuilder.append("`);");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql);
    }

    /**
     * Copy single delays from sourceTableName to targetTableName, ignoring feed ids contained in intervalBoundsTable.
     * 
     * @param targetTableName Base name of table to copy single delays to. Used to infer the name of the table
     *            writing delay information to, usually XX_delays.
     * @param sourceTableName Name of table that contains the simulated poll data. Used to infer the name of the table
     *            containing delay information.
     * @param intervalBoundsTable Name of table that contains feed ids to skip.
     * @return The number of single delays inserted into table targetTableName.
     */
    public int copySimulatedSingleDelays(String targetTableName, String sourceTableName, String intervalBoundsTable) {

        // INSERT INTO `<newTable>_delays` SELECT * FROM `z_eval_MAVSync_min_time_100_2011-11-13_00-56-23_delays` WHERE
        // feedId NOT IN (SELECT feedId FROM MAVSync_Todo_10080);

        StringBuilder sqlBuilder = new StringBuilder();
        sqlBuilder.append("INSERT INTO `");
        sqlBuilder.append(targetTableName);
        sqlBuilder.append(DELAY_POSTFIX);
        sqlBuilder.append("` SELECT * FROM `");
        sqlBuilder.append(sourceTableName);
        sqlBuilder.append(DELAY_POSTFIX);
        sqlBuilder.append("` WHERE feedId NOT IN (SELECT feedId FROM `");
        sqlBuilder.append(intervalBoundsTable);
        sqlBuilder.append("`);");

        final String sql = sqlBuilder.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(sql);
        }
        return runUpdate(sql);
    }
    
    /**
     * Update a feed poll identified by id and pollTimestamp
     * 
     * @return <code>true</code> if feed poll information have been added, <code>false</code> otherwise.
     */
    public boolean updateFeedPoll(PollMetaInformation pollMetaInfo) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(pollMetaInfo.getHttpETag(), "lastETag", pollMetaInfo.getFeedID() + ""));
        parameters.add(pollMetaInfo.getHttpDateSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpLastModifiedSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpExpiresSQLTimestamp());
        parameters.add(pollMetaInfo.getNewestItemSQLTimestamp());
        parameters.add(pollMetaInfo.getNumberNewItems());
        parameters.add(pollMetaInfo.getWindowSize());
        parameters.add(pollMetaInfo.getHttpStatusCode());
        parameters.add(pollMetaInfo.getResponseSize());
        parameters.add(pollMetaInfo.getFeedID());
        parameters.add(pollMetaInfo.getPollSQLTimestamp());

        return runUpdate(UPDATE_FEED_POLL, parameters) != -1;
    }
    

    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * Instead of requesting the poll at the specified timestamp, the previous poll is returned whose
     * poll timestamp is older--not equal--to {@code simulatedPoll}.
     * 
     * @param feedID The feed to get information about.
     * @param simulatedPoll The timestamp to get the poll that was done chronologically previous to the simulated poll.
     * @return Information about a single poll that was earlier than the provided timestamp.
     * @see #getEqualOrPreviousFeedPoll(int, Timestamp)
     */
    public PollMetaInformation getPreviousFeedPoll(int feedID, Timestamp simulatedPoll) {
        return runSingleQuery(FeedPollRowConverter.INSTANCE, GET_PREVIOUS_FEED_POLL_BY_ID_AND_TIME, feedID, simulatedPoll);
    }
    
    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * Instead of requesting the poll at the specified timestamp, the previous poll is returned whose
     * poll timestamp is older or equal to {@code simulatedPoll}. To speedup query, use the timestamp of the last poll
     * since the requested poll is newer or equal to the last poll.
     * 
     * @param feedID The feed to get information about.
     * @param simulatedPoll The timestamp to get the poll that was done at the same time or chronologically previous to
     *            the simulated poll.
     * @return Information about a single poll that was earlier or at the same time than the provided timestamp.
     * @see #getPreviousFeedPoll(int, Timestamp)
     */
    public PollMetaInformation getEqualOrPreviousFeedPollByTimeRange(int feedID, Timestamp simulatedPoll,
            Timestamp lastPoll) {
        return runSingleQuery(FeedPollRowConverter.INSTANCE, GET_PREVIOUS_OR_EQUAL_FEED_POLL_BY_ID_AND_TIMERANGE, feedID,
                simulatedPoll, lastPoll);
    }
    
    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * Instead of requesting the poll at the specified timestamp, the previous poll is returned whose
     * poll timestamp is older or equal to {@code simulatedPoll}.
     * 
     * @param feedID The feed to get information about.
     * @param simulatedPoll The timestamp to get the poll that was done at the same time or chronologically previous to
     *            the simulated poll.
     * @return Information about a single poll that was earlier or at the same time than the provided timestamp.
     * @see #getPreviousFeedPoll(int, Timestamp)
     */
    public PollMetaInformation getEqualOrPreviousFeedPoll(int feedID, Timestamp simulatedPoll) {
        return runSingleQuery(FeedPollRowConverter.INSTANCE, GET_PREVIOUS_OR_EQUAL_FEED_POLL_BY_ID_AND_TIME, feedID,
                simulatedPoll);
    }
    
    /**
     * Get information about a single poll, identified by feedID and pollTimestamp, from table feed_polls.
     * 
     * @param feedID The feed to get information about.
     * @param timestamp The timestamp of the poll to get information about.
     * @return Information about a single poll.
     */
    public PollMetaInformation getFeedPoll(int feedID, Timestamp timestamp) {
        return runSingleQuery(FeedPollRowConverter.INSTANCE, GET_FEED_POLL_BY_ID_TIMESTAMP, feedID, timestamp);
    }
    
    /**
     * Get all polls that have been made to one feed.
     * 
     * @param feedID The feed to get information about.
     * @return A list with information about a all polls.
     */
    public List<PollMetaInformation> getFeedPollsByID(int feedID) {
        return runQuery(FeedPollRowConverter.INSTANCE, GET_FEED_POLLS_BY_ID, feedID);
    }

    public static void main(String[] args) {
        // final EvaluationFeedDatabase db = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder
        // .getInstance().getConfig());
        // String tableName = "eval_fixLearned_min_time_100_2011-11-04_19-28-03";
        // db.generateEvaluationSummary(tableName);
    }

}
