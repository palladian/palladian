package tud.iir.web.feeds.persistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StopWatch;
import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.ResultCallback;
import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedItem;

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
public class FeedDatabase extends DatabaseManager implements FeedStore {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String psAddFeedItem = "INSERT IGNORE INTO feed_items SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, text = ?, pageText = ?";
    private static final String psAddFeed = "INSERT IGNORE INTO feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, activityPattern = ?";
    private static final String psUpdateFeed = "UPDATE feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, lastEtag = ?, lastPollTime = ?, activityPattern = ? WHERE id = ?";
    private static final String psUpdateFeedPostDistribution = "REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?";
    private static final String psGetFeedPostDistribution = "SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?";
    private static final String psGetFeeds = "SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern, lastEtag, lastPollTime, supportsETag, supportsLMS,conditionalGetResponseSize FROM feeds";
    private static final String psGetFeedByUrl = "SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern FROM feeds WHERE feedUrl = ?";
    private static final String psGetFeedByID = "SELECT feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, activityPattern FROM feeds WHERE id = ?";
    private static final String psGetItemsByRawId = "SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items WHERE rawID = ?";
    private static final String psGetItemsByRawId2 = "SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items WHERE feedId = ? AND rawID = ?";
    private static final String psChangeCheckApproach = "UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL";
    private static final String psGetItems = "SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items LIMIT ? OFFSET ?";
    private static final String psGetAllItems = "SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_items";
    private static final String psGetItemById = "SELECT * FROM feed_items WHERE id = ?";
    private static final String psDeleteItemById = "DELETE FROM feed_items WHERE id = ?";

    @Override
    public boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getActivityPattern());
        int result = runUpdateReturnId(psAddFeed, parameters);
        if (result > 0) {
            feed.setId(result);
            added = true;
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        LOGGER.trace(">updateFeed " + feed);

        if (feed.getId() == -1) {
            LOGGER.debug("feed does not exist and is added therefore");
            return addFeed(feed);
        }

        boolean updated = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getFeedUrl());
        parameters.add(feed.getSiteUrl());
        parameters.add(feed.getTitle());
        parameters.add(feed.getContentType().getIdentifier());
        parameters.add(feed.getLanguage());
        parameters.add(feed.getChecks());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getUpdateInterval());
        parameters.add(feed.getLastHeadlines());
        parameters.add(feed.getUnreachableCount());
        parameters.add(feed.getLastFeedEntrySQLTimestamp());
        parameters.add(feed.getLastETag());
        parameters.add(feed.getLastPollTimeSQLTimestamp());
        parameters.add(feed.getActivityPattern());
        parameters.add(feed.getId());

        int result = runUpdate(psUpdateFeed, parameters);
        if (result == 1) {
            updated = true;
        }

        LOGGER.trace("<updateFeed " + updated);

        return updated;
    }

    public Map<Integer, int[]> getFeedPostDistribution(Feed feed) {
        final Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

        ResultCallback<Map<String, Object>> callback = new ResultCallback<Map<String, Object>>() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                int minuteOfDay = (Integer) object.get("minuteOfDay");
                int posts = (Integer) object.get("posts");
                int chances = (Integer) object.get("chances");

                int[] postsChances = { posts, chances };
                postDistribution.put(minuteOfDay, postsChances);

            }
        };
        runQuery(callback, psGetFeedPostDistribution, feed.getId());

        return postDistribution;
    }

    public void updateFeedPostDistribution(Feed feed, Map<Integer, int[]> postDistribution) {
        for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
            List<Object> parameters = new ArrayList<Object>();
            parameters.add(feed.getId());
            parameters.add(distributionEntry.getKey());
            parameters.add(distributionEntry.getValue()[0]);
            parameters.add(distributionEntry.getValue()[1]);
            runUpdate(psUpdateFeedPostDistribution, parameters);
        }
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals,
     * checks, lastHeadlines etc.
     */
    public void changeCheckApproach() {
        runUpdate(psChangeCheckApproach);
    }

    @Override
    public List<Feed> getFeeds() {
        return runQuery(new FeedRowConverter(), psGetFeeds);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        return runSingleQuery(new FeedRowConverter(), psGetFeedByUrl, feedUrl);
    }

    @Override
    public Feed getFeedByID(int feedID) {
        return runSingleQuery(new FeedRowConverter(), psGetFeedByID, feedID);
    }

    @Override
    public boolean addFeedItem(Feed feed, FeedItem entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;

        List<Object> parameters = new ArrayList<Object>();
        parameters.add(feed.getId());
        parameters.add(entry.getTitle());
        parameters.add(entry.getLink());
        parameters.add(entry.getRawId());
        parameters.add(entry.getPublishedSQLTimestamp());
        parameters.add(entry.getItemText());
        parameters.add(entry.getPageText());

        int result = runUpdateReturnId(psAddFeedItem, parameters);
        if (result > 0) {
            entry.setId(result);
            added = true;
        }

        LOGGER.trace("<addEntry " + added);
        return added;
    }

    @Deprecated
    public FeedItem getFeedItemByRawId(String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemsByRawId, rawId);
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemsByRawId2, feedId, rawId);
    }

    public FeedItem getFeedItemById(int id) {
        return runSingleQuery(new FeedItemRowConverter(), psGetItemById, id);
    }

    /**
     * Get the specified count of feed items, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedItems(int limit, int offset) {
        return runQuery(new FeedItemRowConverter(), psGetItems, limit, offset);
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
        List<FeedItem> result = runQuery(new FeedItemRowConverter(), sqlQuery);
        return result;
    }

//    public List<FeedItem> getFeedItemsForEvaluation(String sqlQuery) {
//        List<FeedItem> result = new LinkedList<FeedItem>();
//        try {
//            ResultSet rs = connection.createStatement().executeQuery(sqlQuery);
//            while (rs.next()) {
//                FeedItem entry = getFeedItem(rs);
//                result.add(entry);
//                entry.putFeature("relevant", rs.getFloat("relevant"));
//            }
//            rs.close();
//        } catch (SQLException e) {
//            LOGGER.error(sqlQuery + " : " + e);
//
//        }
//        return result;
//    }


    public void deleteFeedItemById(int id) {
        runUpdate(psDeleteItemById, id);
    }

    /**
     * Allows to iterate over all available news in the database without buffering the contents of the whole result in
     * memory first, using a standard Iterator interface. The underlying Iterator implementation does not allow
     * modifications, so a call to {@link Iterator#remove()} will cause an {@link UnsupportedOperationException}. The
     * ResultSet which is used by the implementation is closed, after the last element has been retrieved.
     * 
     * @return
     */
//    public Iterator<FeedItem> getFeedItems() {
//
//        final ResultSet rs = DatabaseManager.getInstance().runQuery(psGetAllItems);
//        return new Iterator<FeedItem>() {
//
//            /** reference to the next FeedEntry which can be retrieved via next(). */
//            private FeedItem next = null;
//
//            @Override
//            public boolean hasNext() {
//                boolean hasNext = true;
//                try {
//                    if (next == null) {
//                        if (rs.next()) {
//                            next = getFeedItem(rs);
//                        } else {
//                            if (!rs.isClosed()) {
//                                rs.close();
//                            }
//                            hasNext = false;
//                        }
//                    }
//                } catch (SQLException e) {
//                    LOGGER.error(e);
//                }
//                return hasNext;
//            }
//
//            @Override
//            public FeedItem next() {
//                if (!hasNext()) {
//                    throw new NoSuchElementException();
//                }
//                try {
//                    return next;
//                } finally {
//                    // set the reference to null, so that the next entry is retrieved by hasNext().
//                    next = null;
//                }
//            }
//
//            @Override
//            public void remove() {
//                // we do not allow modifications.
//                throw new UnsupportedOperationException();
//            }
//
//        };
//    }
    
    public void getFeedItems(ResultCallback<FeedItem> callback) {
        runQuery(callback, new FeedItemRowConverter(), psGetAllItems);
    }

    public void clearFeedTables() {
        LOGGER.trace(">cleanTables");
        runUpdate("TRUNCATE TABLE feed_items");
        runUpdate("TRUNCATE TABLE feeds");
        LOGGER.trace("<cleanTables");
    }

    public static void main(String[] args) {

        // clear feed specfic tables
        // FeedDatabase.getInstance().clearFeedTables();
        // System.exit(0);

        StopWatch sw = new StopWatch();
        FeedDatabase fd = new FeedDatabase();
        System.out.println(sw.getElapsedTimeString());
        
        
        List<Feed> feeds = fd.getFeeds();
        CollectionHelper.print(feeds);
        
        
        // List<FeedEntry> result = fd.getFeedEntries(100, -1);
        // for (FeedEntry feedEntry : result) {
        // System.out.println(feedEntry);
        // }
        // System.out.println(result.size());

        // Iterator<FeedItem> iterator = fd.getFeedItems();
        System.out.println(sw.getElapsedTimeString());

//        int counter = 0;
//        while (iterator.hasNext()) {
//            System.out.println(iterator.next());
//            counter++;
//        }
//        System.out.println(counter);
//        System.out.println(sw.getElapsedTimeString());

        System.exit(0);
        // FeedItem dummy = new FeedItem();
        // dummy.setId(123);
        // List<Tag> tags = fd.getTags(dummy);
        // System.out.println(tags);

    }

}