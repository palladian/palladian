package ws.palladian.retrieval.feeds.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.ResultIterator;
import ws.palladian.persistence.ResultSetCallback;
import ws.palladian.persistence.RowConverter;
import ws.palladian.persistence.helper.SqlHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;

/**
 * <p>
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and items in a relational database.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class FeedDatabase extends DatabaseManager implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String ADD_FEED_ITEM = "INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, authors = ?, description = ?, text = ?, itemHash = ?";
    private static final String ADD_FEED = "INSERT IGNORE INTO feeds SET feedUrl = ?, checks = ?, checkInterval = ?, newestItemHash = ?, unreachableCount = ?, unparsableCount = ?, lastFeedEntry = ?, activityPattern = ?, lastPollTime = ?, lastETag = ?, lastModified = ?, lastResult = ?, totalProcessingTime = ?, misses = ?, lastMissTimestamp = ?, blocked = ?, lastSuccessfulCheck = ?, windowSize = ?, hasVariableWindowSize = ?, totalItems = ?";
    private static final String UPDATE_FEED = "UPDATE feeds SET feedUrl = ?, checks = ?, checkInterval = ?, newestItemHash = ?, unreachableCount = ?, unparsableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastModified = ?, lastResult = ?, lastPollTime = ?, activityPattern = ?, totalProcessingTime = ?, misses = ?, lastMissTimestamp = ?, blocked = ?, lastSuccessfulCheck = ?, windowSize = ?, hasVariableWindowSize = ?, totalItems = ? WHERE id = ?";
    private static final String UPDATE_FEED_POST_DISTRIBUTION = "REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?";
    private static final String DELETE_FEED_BY_URL = "DELETE FROM feeds WHERE feedUrl = ?";
    private static final String GET_FEED_POST_DISTRIBUTION = "SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?";
    private static final String GET_FEEDS = "SELECT * FROM feeds"; // ORDER BY id ASC";
    private static final String GET_FEED_BY_URL = "SELECT * FROM feeds WHERE feedUrl = ?";
    private static final String GET_FEED_BY_ID = "SELECT * FROM feeds WHERE id = ?";
    // private static final String GET_ITEMS_BY_RAW_ID = "SELECT * FROM feed_items WHERE rawID = ?";
    private static final String GET_ITEMS_BY_RAW_ID_2 = "SELECT * FROM feed_items WHERE feedId = ? AND rawID = ?";
    private static final String CHANGE_CHECK_APPROACH = "UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, newestItemHash = '', checks = 0, lastFeedEntry = NULL";
    private static final String GET_ITEMS = "SELECT * FROM feed_items LIMIT ? OFFSET ?";
    private static final String GET_ALL_ITEMS = "SELECT * FROM feed_items";
    private static final String GET_ITEM_BY_ID = "SELECT * FROM feed_items WHERE id = ?";
    private static final String GET_ITEMS_FOR_FEED = "SELECT * FROM feed_items WHERE feedId = ? ORDER BY published DESC";
    private static final String DELETE_ITEM_BY_ID = "DELETE FROM feed_items WHERE id = ?";
    private static final String UPDATE_FEED_META_INFORMATION = "UPDATE feeds SET  siteUrl = ?, added = ?, title = ?, language = ?, feedSize = ?, httpHeaderSize = ?, supportsPubSubHubBub = ?, isAccessibleFeed = ?, feedFormat = ?, hasItemIds = ?, hasPubDate = ?, hasCloud = ?, ttl = ?, hasSkipHours = ?, hasSkipDays = ?, hasUpdated = ?, hasPublished = ? WHERE id = ?";

    private static final String ADD_FEED_POLL = "INSERT IGNORE INTO feed_polls SET id = ?, pollTimestamp = ?, httpETag = ?, httpDate = ?, httpLastModified = ?, httpExpires = ?, newestItemTimestamp = ?, numberNewItems = ?, windowSize = ?, httpStatusCode = ?, responseSize = ?";
    private static final String ADD_CACHE_ITEMS = "INSERT IGNORE INTO feed_item_cache SET id = ?, itemHash = ?, correctedPollTime = ?";
    private static final String GET_CACHE_ITEMS_BY_ID = "SELECT * FROM feed_item_cache WHERE id = ?";
    private static final String DELETE_CACHE_ITEMS_BY_ID = "DELETE FROM feed_item_cache WHERE id = ?";

    private static final String GET_INDHIST_MODEL_BY_ID = "SELECT * FROM feed_indhist_model WHERE feedId = ?;";

    /**
     * @param dataSource
     */
    protected FeedDatabase(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Truncate a string to 255 chars to store it as varchar(255). Additionally, control characters are removed.
     * In case the string is truncated, a message is written to error log.
     * 
     * @param input The string to truncate.
     * @param name The name of the input like "title" or "feedUrl", required to write meaningful log message.
     * @param feed Something to identify the feed. Use id or feedUrl. Required to write meaningful log message.
     * @return The input string, truncated to 255 chars if longer. <code>null</code> if input was <code>null</code>.
     */
    protected static String truncateToVarchar255(String input, String name, String feed) {
        String output = input;
        if (input != null) {
            output = StringHelper.removeControlCharacters(output);
            if (output.length() > 255) {
                output = output.substring(0, 255);
                LOGGER.error("Truncated " + name + " of feed " + feed + " to fit database. Original value was: "
                        + input);
            }
        }
        return output;
    }

    /**
     * Adds a feed and its meta information. The item cache is <b>not</b> not serialized!
     * 
     * @return <code>true</code> if feed and meta information have been added, <code>false</code> if at least one of
     *         feed or meta information have not been added.
     */
    @Override
    public boolean addFeed(Feed feed) {
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(feed.getFeedUrl(), "feedUrl", feed.getFeedUrl()));
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getNewestItemHash());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getUnparsableCount());
        parameters.add(SqlHelper.getTimestamp(feed.getLastFeedEntry()));
        parameters.add(feed.getActivityPattern().getIdentifier());
        parameters.add(SqlHelper.getTimestamp(feed.getLastPollTime()));
        parameters.add(truncateToVarchar255(feed.getLastETag(), "lastETag", feed.getFeedUrl()));
        parameters.add(SqlHelper.getTimestamp(feed.getHttpLastModified()));
//        if (feed.getLastFeedTaskResult() != null) {
            parameters.add(feed.getLastFeedTaskResult());
//        } else {
//            parameters.add(null);
//        }
        parameters.add(feed.getTotalProcessingTime());
        parameters.add(feed.getMisses());
        parameters.add(feed.getLastMissTime());
        parameters.add(feed.isBlocked());
        parameters.add(feed.getLastSuccessfulCheckTime());
        parameters.add(feed.getWindowSize());
        parameters.add(feed.hasVariableWindowSize());
        parameters.add(feed.getNumberOfItemsReceived());

        int result = runInsertReturnId(ADD_FEED, parameters);
        if (result > 0) {
            feed.setId(result);
            added = true;
        }

        if (added) {
            added = updateMetaInformation(feed);
            if (!added) {
                LOGGER.error("Feed id " + feed.getId() + " has been added but meta information could not be written!");
            }
        }

        return added;
    }

    @Override
    public boolean addFeedItem(FeedItem item) {
        boolean added = false;

        int result = runInsertReturnId(ADD_FEED_ITEM, getItemParameters(item));
        if (result > 0) {
            item.setId(result);
            added = true;
        }

        return added;
    }

    @Override
    public int addFeedItems(List<FeedItem> feedItems) {
        int added = 0;

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        for (FeedItem feedItem : feedItems) {
            List<Object> parameters = getItemParameters(feedItem);
            batchArgs.add(parameters);
        }

        // set the generated IDs back to the FeedItems and count number of added items
        int[] result = runBatchInsertReturnIds(ADD_FEED_ITEM, batchArgs);
        for (int i = 0; i < result.length; i++) {
            int id = result[i];
            if (id > 0) {
                feedItems.get(i).setId(id);
                added++;
            }
        }

        return added;
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public void changeCheckApproach() {
        runUpdate(CHANGE_CHECK_APPROACH);
    }

    public void clearFeedTables() {
        runUpdate("TRUNCATE TABLE feeds");
        runUpdate("TRUNCATE TABLE feed_items");
        runUpdate("TRUNCATE TABLE feeds_post_distribution");
        runUpdate("TRUNCATE TABLE feed_evaluation_polls");
    }

    public void deleteFeedItemById(int id) {
        runUpdate(DELETE_ITEM_BY_ID, id);
    }

    @Override
    public Feed getFeedById(int feedId) {
        return runSingleQuery(FeedRowConverter.INSTANCE, GET_FEED_BY_ID, feedId);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        return runSingleQuery(FeedRowConverter.INSTANCE, GET_FEED_BY_URL, feedUrl);
    }

    public FeedItem getFeedItemById(int id) {
        return runSingleQuery(FeedItemRowConverter.INSTANCE, GET_ITEM_BY_ID, id);
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        return runSingleQuery(FeedItemRowConverter.INSTANCE, GET_ITEMS_BY_RAW_ID_2, feedId, rawId);
    }

//    @Deprecated
//    public FeedItem getFeedItemByRawId(String rawId) {
//        return runSingleQuery(new FeedItemRowConverter(), GET_ITEMS_BY_RAW_ID, rawId);
//    }

    public ResultIterator<FeedItem> getFeedItems() {
        return runQueryWithIterator(FeedItemRowConverter.INSTANCE, GET_ALL_ITEMS);
    }

    /**
     * Get {@link FeedItem}s for the specified feed id. The result is sorted by publish date descendingly, i. e. newer
     * items first.
     * 
     * @param feedId
     * @return
     */
    public ResultIterator<FeedItem> getFeedItemsForFeedId(int feedId) {
        return runQueryWithIterator(FeedItemRowConverter.INSTANCE, GET_ITEMS_FOR_FEED, feedId);
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        return runQuery(FeedItemRowConverter.INSTANCE, GET_ITEMS, limit, offset);
    }

    /**
     * Get FeedItems by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_items table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery) {
        return runQuery(FeedItemRowConverter.INSTANCE, sqlQuery);
    }

    public Map<Integer, int[]> getFeedPostDistribution(Feed feed) {

        final Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                int minuteOfDay = resultSet.getInt("minuteOfDay");
                int posts = resultSet.getInt("posts");
                int chances = resultSet.getInt("chances");
                int[] postsChances = {posts, chances};
                postDistribution.put(minuteOfDay, postsChances);
            }
        };

        runQuery(callback, GET_FEED_POST_DISTRIBUTION, feed.getId());
        return postDistribution;
    }

    @Override
    public List<Feed> getFeeds() {
        List<Feed> feeds = runQuery(FeedRowConverter.INSTANCE, GET_FEEDS);
        for (Feed feed : feeds) {
            feed.setCachedItems(getCachedItemsById(feed.getId()));
        }
        return feeds;
    }

    private List<Object> getItemParameters(FeedItem entry) {
        List<Object> parameters = new ArrayList<Object>();
        parameters.add(entry.getFeedId());
        parameters.add(entry.getTitle());
        parameters.add(entry.getUrl());
        parameters.add(entry.getIdentifier());
        parameters.add(SqlHelper.getTimestamp(entry.getPublished()));
        parameters.add(entry.getAuthors());
        parameters.add(entry.getSummary());
        parameters.add(entry.getText());
        parameters.add(entry.getHash());
        return parameters;
    }

    /**
     * Update feed in database.
     * 
     * @param feed The feed to update
     * @param replaceCachedItems If <code>true</code>, the cached items are replaced by the ones contained in the feed.
     * @return <code>true</code> if (all) update(s) successful.
     */
    @Override
    public boolean updateFeed(Feed feed, boolean replaceCachedItems) {

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(truncateToVarchar255(feed.getFeedUrl(), "feedUrl", feed.getId() + ""));
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getNewestItemHash());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getUnparsableCount());
        parameters.add(feed.getLastFeedEntry());
        parameters.add(truncateToVarchar255(feed.getLastETag(), "lastETag", feed.getId() + ""));
        parameters.add(SqlHelper.getTimestamp(feed.getHttpLastModified()));
        if (feed.getLastFeedTaskResult() != null) {
            parameters.add(feed.getLastFeedTaskResult().toString());
        } else {
            parameters.add(null);
        }
        parameters.add(feed.getLastPollTime());
        parameters.add(feed.getActivityPattern().getIdentifier());
        parameters.add(feed.getTotalProcessingTime());
        parameters.add(feed.getMisses());
        parameters.add(feed.getLastMissTime());
        parameters.add(feed.isBlocked());
        parameters.add(feed.getLastSuccessfulCheckTime());
        parameters.add(feed.getWindowSize());
        parameters.add(feed.hasVariableWindowSize());
        parameters.add(feed.getNumberOfItemsReceived());
        parameters.add(feed.getId());

        updated = runUpdate(UPDATE_FEED, parameters) != -1;

        if (updated) {
            updated = updateMetaInformation(feed);
            if (!updated) {
                LOGGER.error("Updating meta information for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") failed.");
            }
        }

        if (updated && replaceCachedItems) {
            updated = deleteCachedItemById(feed.getId());
            if (!updated) {
                LOGGER.error("Deleting cached items for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") failed.");
            }
            if (updated) {
                updated = addCacheItems(feed);
                if (!updated) {
                    LOGGER.error("Adding new cached items for feed id " + feed.getId() + " (" + feed.getFeedUrl()
                            + ") failed.");
                }
            }
        }

        return updated;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        return updateFeed(feed, true);
    }

    @Override
    public boolean deleteFeedByUrl(String feedUrl) {
        return runUpdate(DELETE_FEED_BY_URL, feedUrl) == 1;
    }

    public void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(distributionEntry.getKey());
            parameters.add(distributionEntry.getValue()[0]);
            parameters.add(distributionEntry.getValue()[1]);
            runUpdate(UPDATE_FEED_POST_DISTRIBUTION, parameters);
        }
    }

    @Override
    public boolean updateMetaInformation(Feed feed) {
        List<Object> parameters = new ArrayList<Object>();

        // truncateToVarchar255(, "feedUrl", feed.getId()+"")

        parameters.add(truncateToVarchar255(feed.getMetaInformation().getSiteUrl(), "siteUrl", feed.getId() + ""));
        parameters.add(feed.getMetaInformation().getAddedSQLTimestamp());
        parameters.add(truncateToVarchar255(feed.getMetaInformation().getTitle(), "title", feed.getId() + ""));
        parameters.add(truncateToVarchar255(feed.getMetaInformation().getLanguage(), "language", feed.getId() + ""));
        parameters.add(feed.getMetaInformation().getByteSize());
        parameters.add(feed.getMetaInformation().getCgHeaderSize());
        parameters.add(feed.getMetaInformation().isSupportsPubSubHubBub());
        parameters.add(feed.getMetaInformation().isAccessible());
        parameters.add(feed.getMetaInformation().getFeedFormat());
        parameters.add(feed.getMetaInformation().hasItemIds());
        parameters.add(feed.getMetaInformation().hasPubDate());
        parameters.add(feed.getMetaInformation().hasCloud());
        parameters.add(feed.getMetaInformation().getRssTtl());
        parameters.add(feed.getMetaInformation().hasSkipHours());
        parameters.add(feed.getMetaInformation().hasSkipDays());
        parameters.add(feed.getMetaInformation().hasUpdated());
        parameters.add(feed.getMetaInformation().hasPublished());

        parameters.add(feed.getId());
        return runUpdate(UPDATE_FEED_META_INFORMATION, parameters) != -1;
    }

    /**
     * @return <code>true</code> if feed poll information have been added, <code>false</code> otherwise.
     */
    @Override
    public boolean addFeedPoll(PollMetaInformation pollMetaInfo) {

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(pollMetaInfo.getFeedID());
        parameters.add(pollMetaInfo.getPollSQLTimestamp());
        parameters.add(truncateToVarchar255(pollMetaInfo.getHttpETag(), "lastETag", pollMetaInfo.getFeedID() + ""));
        parameters.add(pollMetaInfo.getHttpDateSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpLastModifiedSQLTimestamp());
        parameters.add(pollMetaInfo.getHttpExpiresSQLTimestamp());
        parameters.add(pollMetaInfo.getNewestItemSQLTimestamp());
        parameters.add(pollMetaInfo.getNumberNewItems());
        parameters.add(pollMetaInfo.getWindowSize());
        parameters.add(pollMetaInfo.getHttpStatusCode());
        parameters.add(pollMetaInfo.getResponseSize());

        return runInsertReturnId(ADD_FEED_POLL, parameters) != -1;
    }

    /**
     * Add the feed's cached items (item hash and corrected publish date) to database.
     * 
     * @param feed
     * @return true if all items have been added.
     */
    private boolean addCacheItems(Feed feed) {

        List<List<Object>> batchArgs = new ArrayList<List<Object>>();
        Map<String, Date> cachedItems = feed.getCachedItems();
        for (String hash : cachedItems.keySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(hash);
            parameters.add(cachedItems.get(hash));
            batchArgs.add(parameters);
        }

        int[] result = runBatchInsertReturnIds(ADD_CACHE_ITEMS, batchArgs);

        return result.length == cachedItems.size();
    }

    /**
     * Get all cached items (hash, publish date) from this feed.
     * 
     * @param id The feed id.
     * @return All cached items (hash, publish date) or empty map if no item is cached. Never <code>null</code>.
     */
    private Map<String, Date> getCachedItemsById(int id) {
        Map<String, Date> cachedItems = new HashMap<String, Date>();

        List<CachedItem> itemList = runQuery(FeedCacheItemRowConverter.INSTANCE, GET_CACHE_ITEMS_BY_ID, id);
        for (CachedItem item : itemList) {
            cachedItems.put(item.getHash(), item.getCorrectedPublishDate());
        }

        return cachedItems;
    }

    /**
     * Deletes the feed's cached items (item hash and corrected publish date)
     * 
     * @param id the feed whose items are to delete
     * @return <code>true</code> if items were deleted, <code>false</code> in case of an error.
     */
    private boolean deleteCachedItemById(int id) {
        return runUpdate(DELETE_CACHE_ITEMS_BY_ID, id) >= 0;
    }

    /**
     * Load the average change rates for algorithm IndHist. For each hour of the day 0-23, there is a single value
     * representing the feeds average change rate in this hour.
     * 
     * @param feedId The feed the load the model for.
     * @return Array containing the average change rate per hour
     */
    public double[] getIndHistModel(int feedId) {
        // store hourly change rates, default is 0.0D
        double[] changeRate = new double[24];

        RowConverter<int[]> converter = new RowConverter<int[]>() {

            @Override
            public int[] convert(ResultSet resultSet) throws SQLException {
                // store hourly data as hourOfDay, newItems, observationPeriod
                int[] hourlyData = new int[3];

                hourlyData[0] = resultSet.getInt("hourOfDay");
                hourlyData[1] = resultSet.getInt("newItems");
                hourlyData[2] = resultSet.getInt("observationPeriodDays");

                return hourlyData;
            }
        };

        List<int[]> hourlyData = runQuery(converter, GET_INDHIST_MODEL_BY_ID, feedId);

        for (int[] oneHour : hourlyData) {
            // estimate changeRate per Hour as newItems/observationPeriod
            changeRate[oneHour[0]] = (double)oneHour[1] / (double)oneHour[2];
        }
        return changeRate;
    }

}
