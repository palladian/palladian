package tud.iir.web.feeds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import tud.iir.helper.StopWatch;
import tud.iir.persistence.DatabaseManager;
import tud.iir.web.feeds.FeedContentClassifier.FeedContentType;

/**
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and items in a relational database.
 * 
 * TODO change schema to InnoDB?
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class FeedDatabase implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);

    /** the database connection */
    private Connection connection;

    // ////////////////// feed prepared statements ////////////////////
    private PreparedStatement psAddFeedItem;
    private PreparedStatement psAddFeed;
    private PreparedStatement psUpdateFeed;
    private PreparedStatement psUpdateFeedPostDistribution;
    private PreparedStatement psGetFeedPostDistribution;
    private PreparedStatement psGetFeeds;
    private PreparedStatement psGetFeedByUrl;
    private PreparedStatement psGetFeedByID;
    private PreparedStatement psGetItemsByRawId;
    private PreparedStatement psGetItemsByRawId2;
    private PreparedStatement psChangeCheckApproach;
    private PreparedStatement psGetItems;
    private PreparedStatement psGetAllItems;
    private PreparedStatement psGetItemById;
    private PreparedStatement psDeleteItemById;

    protected FeedDatabase() {
        try {
            connection = DatabaseManager.getInstance().getConnection();
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    static class SingletonHolder {
        static FeedDatabase instance = new FeedDatabase();
    }

    public static FeedDatabase getInstance() {
        return SingletonHolder.instance;
    }

    private void prepareStatements() throws SQLException {

        // // prepared statements for feeds
        Connection connection = DatabaseManager.getInstance().getConnection();
        psAddFeedItem = connection
        .prepareStatement("INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, text = ?, pageText = ?");
        psAddFeed = connection
        .prepareStatement("INSERT IGNORE INTO feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, activityPattern = ?");
        psUpdateFeed = connection
        .prepareStatement("UPDATE feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastPollTime = ?, activityPattern = ? WHERE id = ?");
        psUpdateFeedPostDistribution = connection
        .prepareStatement("REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?");
        psGetFeedPostDistribution = connection
        .prepareStatement("SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?");
        psGetFeeds = connection
        .prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern, lastEtag, lastPollTime, supportsETag, supportsLMS,conditionalGetResponseSize FROM feeds");
        psGetFeedByUrl = connection
        .prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern FROM feeds WHERE feedUrl = ?");
        psGetFeedByID = connection
        .prepareStatement("SELECT feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern FROM feeds WHERE id = ?");
        psGetItemsByRawId = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items WHERE rawID = ?");
        psGetItemsByRawId2 = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items WHERE feedId = ? AND rawID = ?");
        psChangeCheckApproach = connection
        .prepareStatement("UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL");

        psGetItems = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items LIMIT ? OFFSET ?");
        psGetAllItems = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items");
        psGetItemById = connection.prepareStatement("SELECT * FROM feed_items WHERE id = ?");
        psDeleteItemById = connection.prepareStatement("DELETE FROM feed_items WHERE id = ?");

    }

    @Override
    public synchronized boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;
        try {
            psAddFeed.setString(1, feed.getFeedUrl());
            psAddFeed.setString(2, feed.getSiteUrl());
            psAddFeed.setString(3, feed.getTitle());
            //            psAddFeed.setInt(4, feed.getFormat());
            psAddFeed.setInt(5, feed.getContentType().getIdentifier());
            psAddFeed.setString(6, feed.getLanguage());
            psAddFeed.setInt(7, feed.getChecks());
            psAddFeed.setInt(8, feed.getUpdateInterval());
            psAddFeed.setInt(9, feed.getUpdateInterval());
            psAddFeed.setString(10, feed.getLastHeadlines());
            psAddFeed.setInt(11, feed.getUnreachableCount());
            psAddFeed.setTimestamp(12, feed.getLastFeedEntrySQLTimestamp());
            psAddFeed.setInt(13, feed.getActivityPattern());
            int update = DatabaseManager.getInstance().runUpdate(psAddFeed);
            if (update == 1) {
                int id = DatabaseManager.getInstance().getLastInsertID();
                // update Feed object's ID
                feed.setId(id);
                if (id != -1) {
                    added = true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("addFeed", e);
        }
        LOGGER.trace("<addFeed " + added);
        return added;
    }

    @Override
    public synchronized boolean updateFeed(Feed feed) {
        LOGGER.trace(">updateFeed " + feed);

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        try {
            PreparedStatement ps = psUpdateFeed;

            ps.setString(1, feed.getFeedUrl());
            ps.setString(2, feed.getSiteUrl());
            ps.setString(3, feed.getTitle());
            //            ps.setInt(4, feed.getFormat());
            ps.setInt(5, feed.getContentType().getIdentifier());
            ps.setString(6, feed.getLanguage());
            ps.setInt(7, feed.getChecks());
            ps.setInt(8, feed.getUpdateInterval());
            ps.setInt(9, feed.getUpdateInterval());
            ps.setString(10, feed.getLastHeadlines());
            ps.setInt(11, feed.getUnreachableCount());
            ps.setTimestamp(12, feed.getLastFeedEntrySQLTimestamp());
            ps.setString(13, feed.getLastETag());
            ps.setTimestamp(14, feed.getLastPollTimeSQLTimestamp());
            ps.setInt(15, feed.getActivityPattern());
            ps.setLong(16, feed.getId());

            DatabaseManager.getInstance().runUpdate(ps);
            updated = true;

        } catch (SQLException e) {
            LOGGER.error("updateFeed", e);
            updated = false;
        }
        LOGGER.trace("<updateFeed " + updated);

        return updated;
    }

    public synchronized Map<Integer, int[]> getFeedPostDistribution(Feed feed) {
        Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        try {
            psGetFeedPostDistribution.setLong(1, feed.getId());
            ResultSet rs = DatabaseManager.getInstance().runQuery(psGetFeedPostDistribution);

            while (rs.next()) {
                int minuteOfDay = rs.getInt("minuteOfDay");
                int posts = rs.getInt("posts");
                int chances = rs.getInt("chances");

                int[] postsChances = { posts, chances };
                postDistribution.put(minuteOfDay, postsChances);
            }
            rs.close();

        } catch (SQLException e) {
            LOGGER.error("could not update feed post distribution for " + feed.getFeedUrl());
        }

        return postDistribution;
    }

    public synchronized void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        try {
            for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
                psUpdateFeedPostDistribution.setLong(1, feed.getId());
                psUpdateFeedPostDistribution.setInt(2, distributionEntry.getKey());
                psUpdateFeedPostDistribution.setInt(3, distributionEntry.getValue()[0]);
                psUpdateFeedPostDistribution.setInt(4, distributionEntry.getValue()[1]);
                DatabaseManager.getInstance().runUpdate(psUpdateFeedPostDistribution);
            }
        } catch (SQLException e) {
            LOGGER.error("could not update feed post distribution for " + feed.getFeedUrl());
        }
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public synchronized void changeCheckApproach() {
        DatabaseManager.getInstance().runUpdate(psChangeCheckApproach);
    }

    @Override
    public List<Feed> getFeeds() {
        LOGGER.trace(">getFeeds");
        List<Feed> result = new LinkedList<Feed>();
        try {

            PreparedStatement ps = psGetFeeds;

            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                Feed feed = getFeed(resultSet);
                result.add(feed);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeeds", e);
        }
        LOGGER.trace("<getFeeds " + result.size());
        return result;
    }

    // create Feed from ResultSet
    private Feed getFeed(ResultSet resultSet) throws SQLException {

        Feed feed = new Feed();
        feed.setId(resultSet.getInt("id"));
        feed.setFeedUrl(resultSet.getString("feedUrl"));
        feed.setSiteUrl(resultSet.getString("siteUrl"));
        feed.setTitle(resultSet.getString("title"));
        //        feed.setFormat(resultSet.getInt("format"));
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

    @Override
    public synchronized Feed getFeedByUrl(String feedUrl) {
        LOGGER.trace(">getFeedByUrl " + feedUrl);
        Feed feed = null;
        try {
            psGetFeedByUrl.setString(1, feedUrl);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetFeedByUrl);
            if (resultSet.next()) {
                feed = getFeed(resultSet);
            } else {
                LOGGER.debug("feed with " + feedUrl + " not found.");
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedByUrl", e);
        }
        LOGGER.trace("<getFeedByUrl " + feed);

        return feed;
    }

    @Override
    public synchronized Feed getFeedByID(int feedID) {
        LOGGER.trace(">getFeedByID " + feedID);
        Feed feed = null;
        try {
            psGetFeedByID.setInt(1, feedID);

            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetFeedByID);
            if (resultSet.next()) {
                feed = getFeed(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedByID", e);
        }
        LOGGER.trace("<getFeedByID " + feed);

        return feed;
    }

    @Override
    public synchronized boolean addFeedItem(Feed feed, FeedItem entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;
        try {
            psAddFeedItem.setLong(1, feed.getId());
            psAddFeedItem.setString(2, entry.getTitle());
            psAddFeedItem.setString(3, entry.getLink());
            psAddFeedItem.setString(4, entry.getRawId());
            psAddFeedItem.setTimestamp(5, entry.getPublishedSQLTimestamp());
            psAddFeedItem.setString(6, entry.getItemText());
            psAddFeedItem.setString(7, entry.getPageText());

            // check affected rows
            int update = DatabaseManager.getInstance().runUpdate(psAddFeedItem);
            if (update == 1) {
                // get id of last insert
                int id = DatabaseManager.getInstance().getLastInsertID();
                entry.setId(id);
                if (id != -1) {
                    added = true;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("addEntry", e);
        }

        LOGGER.trace("<addEntry " + added);
        return added;
    }

    @Deprecated
    public synchronized FeedItem getFeedItemByRawId(String rawId) {
        LOGGER.trace(">getEntryByRawId");
        FeedItem result = null;
        try {
            psGetItemsByRawId.setString(1, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetItemsByRawId);
            if (resultSet.next()) {
                result = getFeedItem(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryByRawId", e);
        }
        LOGGER.trace("<getEntryByRawId");
        return result;
    }

    @Override
    public synchronized FeedItem getFeedItemByRawId(int feedId, String rawId) {
        LOGGER.trace(">getEntryByRawId");
        FeedItem result = null;
        try {
            psGetItemsByRawId2.setInt(1, feedId);
            psGetItemsByRawId2.setString(2, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetItemsByRawId2);
            if (resultSet.next()) {
                result = getFeedItem(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryByRawId", e);
        }
        LOGGER.trace("<getEntryByRawId");
        return result;
    }

    public FeedItem getFeedItemById(int id) {
        LOGGER.trace(">getEntryById");
        FeedItem result = null;
        try {
            psGetItemById.setInt(1, id);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetItemById);
            if (resultSet.next()) {
                result = getFeedItem(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryById", e);
        }
        LOGGER.trace("<getEntryById");
        return result;
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        LOGGER.trace(">getFeedEntries");
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            psGetItems.setInt(1, limit);
            psGetItems.setInt(2, offset);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetItems);

            while (resultSet.next()) {
                result.add(getFeedItem(resultSet));
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedEntries", e);
        }
        LOGGER.trace("<getFeedEntries");
        return result;
    }

    /**
     * Get FeedItems by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_items table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedItems(String sqlQuery) {
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            ResultSet rs = connection.createStatement().executeQuery(sqlQuery);
            while (rs.next()) {
                result.add(getFeedItem(rs));
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.error(sqlQuery + " : " + e);

        }
        return result;
    }

    public List<FeedItem> getFeedItemsForEvaluation(String sqlQuery) {
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            ResultSet rs = connection.createStatement().executeQuery(sqlQuery);
            while (rs.next()) {
                FeedItem entry = getFeedItem(rs);
                result.add(entry);
                entry.putFeature("relevant", rs.getFloat("relevant"));
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.error(sqlQuery + " : " + e);

        }
        return result;
    }

    // create FeedEntry from ResultSet
    private FeedItem getFeedItem(ResultSet resultSet) throws SQLException {

        FeedItem entry = new FeedItem();

        entry.setId(resultSet.getInt("id"));
        entry.setFeedId(resultSet.getInt("feedId"));
        entry.setTitle(resultSet.getString("title"));
        entry.setLink(resultSet.getString("link"));
        entry.setRawId(resultSet.getString("rawId"));
        entry.setPublished(resultSet.getTimestamp("published"));
        entry.setItemText(resultSet.getString("text"));
        entry.setPageText(resultSet.getString("pageText"));
        entry.setAdded(resultSet.getTimestamp("added"));

        return entry;
    }

    public void deleteFeedItemById(int id) {

        try {
            psDeleteItemById.setInt(1, id);
            DatabaseManager.getInstance().runUpdate(psDeleteItemById);
        } catch (SQLException e) {
            LOGGER.error(e);
        }

    }

    /**
     * Allows to iterate over all available news in the database without buffering the contents of the whole result in
     * memory first, using a standard Iterator interface. The underlying Iterator implementation does not allow
     * modifications, so a call to {@link Iterator#remove()} will cause an {@link UnsupportedOperationException}. The
     * ResultSet which is used by the implementation is closed, after the last element has been retrieved.
     * 
     * @return
     */
    public Iterator<FeedItem> getFeedItems() {

        final ResultSet rs = DatabaseManager.getInstance().runQuery(psGetAllItems);
        return new Iterator<FeedItem>() {

            /** reference to the next FeedEntry which can be retrieved via next(). */
            private FeedItem next = null;

            @Override
            public boolean hasNext() {
                boolean hasNext = true;
                try {
                    if (next == null) {
                        if (rs.next()) {
                            next = getFeedItem(rs);
                        } else {
                            if (!rs.isClosed()) {
                                rs.close();
                            }
                            hasNext = false;
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.error(e);
                }
                return hasNext;
            }

            @Override
            public FeedItem next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                try {
                    return next;
                } finally {
                    // set the reference to null, so that the next entry is retrieved by hasNext().
                    next = null;
                }
            }

            @Override
            public void remove() {
                // we do not allow modifications.
                throw new UnsupportedOperationException();
            }

        };
    }

    public void clearFeedTables() {
        LOGGER.trace(">cleanTables");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_items");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feeds");
        LOGGER.trace("<cleanTables");
    }

    public static void main(String[] args) {

        // clear feed specfic tables
        // FeedDatabase.getInstance().clearFeedTables();
        // System.exit(0);

        StopWatch sw = new StopWatch();
        FeedDatabase fd = FeedDatabase.getInstance();
        System.out.println(sw.getElapsedTimeString());
        // List<FeedEntry> result = fd.getFeedEntries(100, -1);
        // for (FeedEntry feedEntry : result) {
        // System.out.println(feedEntry);
        // }
        // System.out.println(result.size());

        Iterator<FeedItem> iterator = fd.getFeedItems();
        System.out.println(sw.getElapsedTimeString());

        int counter = 0;
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
            counter++;
        }
        System.out.println(counter);
        System.out.println(sw.getElapsedTimeString());

        System.exit(0);
        // FeedItem dummy = new FeedItem();
        // dummy.setId(123);
        // List<Tag> tags = fd.getTags(dummy);
        // System.out.println(tags);

    }

}