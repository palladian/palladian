package tud.iir.news;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import tud.iir.persistence.DatabaseManager;

/**
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and entries in a relational database.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class FeedDatabase implements FeedStore {

    /** the instance of this class */
    private final static FeedDatabase INSTANCE = new FeedDatabase();
    
    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);
    
    // ////////////////// feed prepared statements ////////////////////
    private PreparedStatement psAddFeedEntry;
    private PreparedStatement psAddFeed;
    private PreparedStatement psUpdateFeed;
    private PreparedStatement psUpdateFeed_fixed_learned;
    private PreparedStatement psUpdateFeed_adaptive;
    private PreparedStatement psUpdateFeed_probabilistic;
    private PreparedStatement psUpdateFeedPostDistribution;
    private PreparedStatement psGetFeedPostDistribution;
    private PreparedStatement psGetFeeds;
    private PreparedStatement psGetFeeds_fixed_learned;
    private PreparedStatement psGetFeeds_adaptive;
    private PreparedStatement psGetFeeds_probabilistic;
    private PreparedStatement psGetFeedByUrl;
    private PreparedStatement psGetFeedByID;
    private PreparedStatement psGetEntryByRawId;
    private PreparedStatement psChangeCheckApproach;
    private PreparedStatement psGetEntries;
    
    private FeedDatabase() {
        try {
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    public static FeedDatabase getInstance() {
        return INSTANCE;
    }
    
    private void prepareStatements() throws SQLException {
        // // prepared statements for feeds
        Connection connection = DatabaseManager.getInstance().getConnection();
        psAddFeedEntry = connection.prepareStatement("INSERT IGNORE INTO feed_entries SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, text = ?, pageText = ?, tags = ?");
        psAddFeed = connection.prepareStatement("INSERT IGNORE INTO feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, updateClass = ?");
        psUpdateFeed = connection.prepareStatement("UPDATE feeds SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, updateClass = ? WHERE id = ?");
        psUpdateFeed_fixed_learned = connection.prepareStatement("UPDATE feeds_fixed_learned SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, updateClass = ? WHERE id = ?");
        psUpdateFeed_adaptive = connection.prepareStatement("UPDATE feeds_adaptive SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, updateClass = ? WHERE id = ?");
        psUpdateFeed_probabilistic = connection.prepareStatement("UPDATE feeds_probabilistic SET feedUrl = ?, siteUrl = ?, title = ?, format = ?, textType = ?, language = ?, checks = ?, minCheckInterval = ?, maxCheckInterval = ?, lastHeadlines = ?, unreachableCount = ?, lastFeedEntry = ?, updateClass = ? WHERE id = ?");
        psUpdateFeedPostDistribution = connection.prepareStatement("REPLACE INTO feeds_post_distribution SET feedID = ?, minuteOfDay = ?, posts = ?, chances = ?");
        psGetFeedPostDistribution = connection.prepareStatement("SELECT minuteOfDay, posts, chances FROM feeds_post_distribution WHERE feedID = ?");
        psGetFeeds = connection.prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds");
        psGetFeeds_fixed_learned = connection.prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds_fixed_learned");
        psGetFeeds_adaptive = connection.prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds_adaptive");
        psGetFeeds_probabilistic = connection.prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds_probabilistic");
        psGetFeedByUrl = connection.prepareStatement("SELECT id, feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds WHERE feedUrl = ?");
        psGetFeedByID = connection.prepareStatement("SELECT feedUrl, siteUrl, title, format, textType, language, added, checks, minCheckInterval, maxCheckInterval, lastHeadlines, unreachableCount, lastFeedEntry, updateClass FROM feeds WHERE id = ?");
        psGetEntryByRawId = connection.prepareStatement("SELECT id, title, link, rawId, published, text, pageText, added, tags FROM feed_entries WHERE rawID = ?");
        psChangeCheckApproach = connection.prepareStatement("UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL");
        
        psGetEntries = connection.prepareStatement("SELECT id, title, link, rawId, published, text, pageText, added, tags FROM feed_entries LIMIT ? OFFSET ?");
    }
    

    

    @Override
    public synchronized boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;
        try {
            psAddFeed.setString(1, feed.getFeedUrl());
            psAddFeed.setString(2, feed.getSiteUrl());
            psAddFeed.setString(3, feed.getTitle());
            psAddFeed.setInt(4, feed.getFormat());
            psAddFeed.setInt(5, feed.getTextType());
            psAddFeed.setString(6, feed.getLanguage());
            psAddFeed.setInt(7, feed.getChecks());
            psAddFeed.setInt(8, feed.getMinCheckInterval());
            psAddFeed.setInt(9, feed.getMaxCheckInterval());
            psAddFeed.setString(10, feed.getLastHeadlines());
            psAddFeed.setInt(11, feed.getUnreachableCount());
            psAddFeed.setTimestamp(12, feed.getLastFeedEntrySQLTimestamp());
            psAddFeed.setInt(13, feed.getUpdateClass());
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

            // switch (feedChecker.getCheckApproach()) {
            // case CHECK_FIXED:
            // if (feedChecker.getCheckInterval() == -1) {
            // ps = psUpdateFeed_fixed_learned;
            // }
            // break;
            // case CHECK_ADAPTIVE:
            // ps = psUpdateFeed_adaptive;
            // break;
            // case CHECK_PROBABILISTIC:
            // ps = psUpdateFeed_probabilistic;
            // break;
            // default:
            // break;
            // }

            ps.setString(1, feed.getFeedUrl());
            ps.setString(2, feed.getSiteUrl());
            ps.setString(3, feed.getTitle());
            ps.setInt(4, feed.getFormat());
            ps.setInt(5, feed.getTextType());
            ps.setString(6, feed.getLanguage());
            ps.setInt(7, feed.getChecks());
            ps.setInt(8, feed.getMinCheckInterval());
            ps.setInt(9, feed.getMaxCheckInterval());
            ps.setString(10, feed.getLastHeadlines());
            ps.setInt(11, feed.getUnreachableCount());
            ps.setTimestamp(12, feed.getLastFeedEntrySQLTimestamp());
            ps.setInt(13, feed.getUpdateClass());
            ps.setLong(14, feed.getId());

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
     * When the check approach is switched we need to reset learned and calculated values such as check intervals, checks, lastHeadlines etc.
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

//            switch (FeedChecker.getInstance().getCheckApproach()) {
//                case FeedChecker.CHECK_FIXED:
//                    if (FeedChecker.getInstance().getCheckInterval() == -1) {
//                        ps = DatabaseManager.getInstance().psGetFeeds_fixed_learned;
//                    }
//                    break;
//                case FeedChecker.CHECK_ADAPTIVE:
//                    ps = DatabaseManager.getInstance().psGetFeeds_adaptive;
//                    break;
//                case FeedChecker.CHECK_PROBABILISTIC:
//                    ps = DatabaseManager.getInstance().psGetFeeds_probabilistic;
//                    break;
//            }

            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                Feed feed = new Feed();
                feed.setId(resultSet.getInt(1));
                feed.setFeedUrl(resultSet.getString(2));
                feed.setSiteUrl(resultSet.getString(3));
                feed.setTitle(resultSet.getString(4));
                feed.setFormat(resultSet.getInt(5));
                feed.setTextType(resultSet.getInt(6));
                feed.setLanguage(resultSet.getString(7));
                feed.setAdded(resultSet.getTimestamp(8));
                feed.setChecks(resultSet.getInt(9));
                feed.setMinCheckInterval(resultSet.getInt(10));
                feed.setMaxCheckInterval(resultSet.getInt(11));
                feed.setLastHeadlines(resultSet.getString(12));
                feed.setUnreachableCount(resultSet.getInt(13));
                feed.setLastFeedEntry(resultSet.getTimestamp(14));
                feed.setUpdateClass(resultSet.getInt(15));
                result.add(feed);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeeds", e);
        }
        LOGGER.trace("<getFeeds " + result.size());
        return result;
    }

    @Override
    public synchronized Feed getFeedByUrl(String feedUrl) {
        LOGGER.trace(">getFeedByUrl " + feedUrl);
        Feed feed = null;
        try {
            psGetFeedByUrl.setString(1, feedUrl);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetFeedByUrl);
            if (resultSet.next()) {
                feed = new Feed();
                feed.setId(resultSet.getInt(1));
                feed.setFeedUrl(resultSet.getString(2));
                feed.setSiteUrl(resultSet.getString(3));
                feed.setTitle(resultSet.getString(4));
                feed.setFormat(resultSet.getInt(5));
                feed.setTextType(resultSet.getInt(6));
                feed.setLanguage(resultSet.getString(7));
                feed.setAdded(resultSet.getTimestamp(8));
                feed.setChecks(resultSet.getInt(9));
                feed.setMinCheckInterval(resultSet.getInt(10));
                feed.setMaxCheckInterval(resultSet.getInt(11));
                feed.setLastHeadlines(resultSet.getString(12));
                feed.setUnreachableCount(resultSet.getInt(13));
                feed.setLastFeedEntry(resultSet.getTimestamp(14));
                feed.setUpdateClass(resultSet.getInt(15));
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
                feed = new Feed();
                feed.setId(feedID);
                feed.setFeedUrl(resultSet.getString(1));
                feed.setSiteUrl(resultSet.getString(2));
                feed.setTitle(resultSet.getString(3));
                feed.setFormat(resultSet.getInt(4));
                feed.setTextType(resultSet.getInt(5));
                feed.setLanguage(resultSet.getString(6));
                feed.setAdded(resultSet.getTimestamp(7));
                feed.setChecks(resultSet.getInt(8));
                feed.setMinCheckInterval(resultSet.getInt(9));
                feed.setMaxCheckInterval(resultSet.getInt(10));
                feed.setLastHeadlines(resultSet.getString(11));
                feed.setUnreachableCount(resultSet.getInt(12));
                feed.setLastFeedEntry(resultSet.getTimestamp(13));
                feed.setUpdateClass(resultSet.getInt(14));
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedByID", e);
        }
        LOGGER.trace("<getFeedByID " + feed);

        return feed;
    }

    @Override
    public synchronized boolean addEntry(Feed feed, FeedEntry entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;
        try {
            psAddFeedEntry.setLong(1, feed.getId());
            psAddFeedEntry.setString(2, entry.getTitle());
            psAddFeedEntry.setString(3, entry.getLink());
            psAddFeedEntry.setString(4, entry.getRawId());
            psAddFeedEntry.setTimestamp(5, entry.getAddedSQLTimestamp());
            psAddFeedEntry.setString(6, entry.getContent());
            psAddFeedEntry.setString(7, entry.getPageContent());
            // store tags in comma separated column
            psAddFeedEntry.setString(8, StringUtils.join(entry.getTags(), ","));

            // check affected rows
            int update = DatabaseManager.getInstance().runUpdate(psAddFeedEntry);
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

    @Override
    public synchronized FeedEntry getEntryByRawId(String rawId) {
        LOGGER.trace(">getEntryByRawId");
        FeedEntry result = null;
        try {
            psGetEntryByRawId.setString(1, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntryByRawId);
            if (resultSet.next()) {
                result = new FeedEntry();
                result.setId(resultSet.getInt(1));
                result.setTitle(resultSet.getString(2));
                result.setLink(resultSet.getString(3));
                result.setRawId(resultSet.getString(4));
                result.setPublished(resultSet.getDate(5));
                result.setContent(resultSet.getString(6));
                result.setPageContent(resultSet.getString(7));
                result.setAdded(resultSet.getDate(8));
                String tags = resultSet.getString(9);
                if (tags != null) {
                    result.setTags(new LinkedList<String>(Arrays.asList(tags.split(","))));
                }
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryByRawId", e);
        }
        LOGGER.trace("<getEntryByRawId");
        return result;
    }
    
    public List<FeedEntry> getFeedEntries(int limit, int offset) {
        LOGGER.trace(">getFeedEntries");
        List<FeedEntry> result = new LinkedList<FeedEntry>();
        try {
            psGetEntries.setInt(1, limit);
            psGetEntries.setInt(2, offset);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntries);
            while (resultSet.next()) {
                FeedEntry entry = new FeedEntry();
                entry.setId(resultSet.getInt(1));
                entry.setTitle(resultSet.getString(2));
                entry.setLink(resultSet.getString(3));
                entry.setRawId(resultSet.getString(4));
                entry.setPublished(resultSet.getDate(5));
                entry.setContent(resultSet.getString(6));
                entry.setPageContent(resultSet.getString(7));
                entry.setAdded(resultSet.getDate(8));
                String tags = resultSet.getString(9);
                if (tags != null) {
                    entry.setTags(new LinkedList<String>(Arrays.asList(tags.split(","))));
                }
                result.add(entry);
            }
        } catch (SQLException e) {
            LOGGER.error("getFeedEntries", e);
        }
        LOGGER.trace("<getFeedEntries");
        return result;
    }

    // public List<Entry> getEntries(Feed feed, int limit) {
    // logger.trace(">getEntries " + feed + " limit:" + limit);
    // List<Entry> result = new LinkedList<Entry>();
    // try {
    // PreparedStatement ps;
    // String sql = "SELECT id, title, link, rawId, published, text FROM entries WHERE feedId = ? ORDER BY published DESC";
    // if (limit == -1) {
    // ps = connection.prepareStatement(sql);
    // } else {
    // ps = connection.prepareStatement(sql + " LIMIT 0, ?");
    // ps.setInt(2, limit);
    // }
    // ps.setLong(1, feed.getId());
    // ResultSet resultSet = ps.executeQuery();
    // while (resultSet.next()) {
    // Entry entry = new Entry();
    // entry.setId(resultSet.getLong(1));
    // entry.setTitle(resultSet.getString(2));
    // entry.setLink(resultSet.getString(3));
    // entry.setRawId(resultSet.getString(4));
    // entry.setPublished(resultSet.getTimestamp(5));
    // entry.setText(resultSet.getString(6));
    // result.add(entry);
    // }
    // resultSet.close();
    // } catch (SQLException e) {
    // logger.error("getEntries", e);
    // }
    // logger.trace("<getEntries " + result.size());
    // return result;
    // }
    //	
    // public List<Entry> getEntries(Feed feed) {
    // logger.trace(">getEntries " + feed);
    // logger.trace("<getEntries");
    // return this.getEntries(feed, -1);
    // }

    public void clearFeedTables() {
        LOGGER.trace(">cleanTables");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_entries");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feeds");
        LOGGER.trace("<cleanTables");
    }
    
    public static void main(String[] args) {
        // clear feed specfic tables
        // FeedDatabase.getInstance().clearFeedTables();
        FeedDatabase fd = FeedDatabase.getInstance();
        List<FeedEntry> result = fd.getFeedEntries(100, -1);
        for (FeedEntry feedEntry : result) {
            System.out.println(feedEntry);
        }
        System.out.println(result.size());
    }

}