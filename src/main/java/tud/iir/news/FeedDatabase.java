package tud.iir.news;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.classification.controlledtagging.Tag;
import tud.iir.classification.controlledtagging.TagComparator;
import tud.iir.helper.StopWatch;
import tud.iir.persistence.DatabaseManager;

/**
 * The FeedDatabase is an implementation of the FeedStore that stores feeds and entries in a relational database.
 * 
 * TODO change schema to InnoDB?
 * TODO move Tag storage from DB to Lucene index.
 * TODO change name from "FeedEntry" to "News", update DB schema accordingly.
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

    /** the database connection */
    private Connection connection;

    // ////////////////// feed prepared statements ////////////////////
    private PreparedStatement psAddFeedEntry;
    private PreparedStatement psAddFeed;
    private PreparedStatement psUpdateFeed;
    private PreparedStatement psUpdateFeedPostDistribution;
    private PreparedStatement psGetFeedPostDistribution;
    private PreparedStatement psGetFeeds;
    private PreparedStatement psGetFeedByUrl;
    private PreparedStatement psGetFeedByID;
    private PreparedStatement psGetEntryByRawId;
    private PreparedStatement psGetEntryByRawId2;
    private PreparedStatement psChangeCheckApproach;
    private PreparedStatement psGetEntries;
    private PreparedStatement psGetAllEntries;
    private PreparedStatement psGetEntrysTags;
    private PreparedStatement psGetTagId;
    private PreparedStatement psInsertTag;
    private PreparedStatement psTagFeedEntry;
    private PreparedStatement getEntryIdByTag;
    private PreparedStatement psGetEntryById;
    private PreparedStatement psDeleteEntryById;

    private FeedDatabase() {
        try {
            connection = DatabaseManager.getInstance().getConnection();
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
        psAddFeedEntry = connection
        .prepareStatement("INSERT IGNORE INTO feed_entries SET feedId = ?, title = ?, link = ?, rawId = ?, published = ?, text = ?, pageText = ?");
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
        psGetEntryByRawId = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_entries WHERE rawID = ?");
        psGetEntryByRawId2 = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_entries WHERE feedId = ? AND rawID = ?");
        psChangeCheckApproach = connection
        .prepareStatement("UPDATE feeds SET minCheckInterval = 5, maxCheckInterval = 1, lastHeadlines = '', checks = 0, lastFeedEntry = NULL");

        psGetEntries = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_entries LIMIT ? OFFSET ?");
        psGetAllEntries = connection
        .prepareStatement("SELECT id, feedId, title, link, rawId, published, text, pageText, added FROM feed_entries");
        psGetEntryById = connection.prepareStatement("SELECT * FROM feed_entries WHERE id = ?");
        psDeleteEntryById = connection.prepareStatement("DELETE FROM feed_entries WHERE id = ?");

        // tagging specific
        psGetEntrysTags = connection
        // .prepareStatement("SELECT name, weight FROM feed_entries_tags, feed_entry_tag WHERE feed_entries_tags = feed_entry_tag.tagId AND feed_entry_tag.entryId = ?");
        .prepareStatement("SELECT name, weight FROM feed_entries_tags ets, feed_entry_tag et WHERE ets.id = et.tagId AND et.entryId = ?");

        psGetTagId = connection.prepareStatement("SELECT id FROM feed_entries_tags WHERE name = ?");
        psInsertTag = connection.prepareStatement("INSERT INTO feed_entries_tags SET name = ?");
        psTagFeedEntry = connection
        .prepareStatement("INSERT INTO feed_entry_tag SET entryId = ?, tagId = ?, weight = ?");
        getEntryIdByTag = connection
        .prepareStatement("SELECT entryId FROM feed_entry_tag, feed_entries_tags WHERE tagId = feed_entries_tags.id AND name = ?");
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
            ps.setInt(4, feed.getFormat());
            ps.setInt(5, feed.getTextType());
            ps.setString(6, feed.getLanguage());
            ps.setInt(7, feed.getChecks());
            ps.setInt(8, feed.getMinCheckInterval());
            ps.setInt(9, feed.getMaxCheckInterval());
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
        feed.setFormat(resultSet.getInt("format"));
        feed.setTextType(resultSet.getInt("textType"));
        feed.setLanguage(resultSet.getString("language"));
        feed.setAdded(resultSet.getTimestamp("added"));
        feed.setChecks(resultSet.getInt("checks"));
        feed.setMinCheckInterval(resultSet.getInt("minCheckInterval"));
        feed.setMaxCheckInterval(resultSet.getInt("maxCheckInterval"));
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
    public synchronized boolean addFeedEntry(Feed feed, FeedItem entry) {
        LOGGER.trace(">addEntry " + entry + " to " + feed);
        boolean added = false;
        try {
            psAddFeedEntry.setLong(1, feed.getId());
            psAddFeedEntry.setString(2, entry.getTitle());
            psAddFeedEntry.setString(3, entry.getLink());
            psAddFeedEntry.setString(4, entry.getRawId());
            psAddFeedEntry.setTimestamp(5, entry.getPublishedSQLTimestamp());
            psAddFeedEntry.setString(6, entry.getItemText());
            psAddFeedEntry.setString(7, entry.getPageText());

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
    @Deprecated
    public synchronized FeedItem getFeedEntryByRawId(String rawId) {
        LOGGER.trace(">getEntryByRawId");
        FeedItem result = null;
        try {
            psGetEntryByRawId.setString(1, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntryByRawId);
            if (resultSet.next()) {
                result = getFeedEntry(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryByRawId", e);
        }
        LOGGER.trace("<getEntryByRawId");
        return result;
    }

    @Override
    public synchronized FeedItem getFeedEntryByRawId(int feedId, String rawId) {
        LOGGER.trace(">getEntryByRawId");
        FeedItem result = null;
        try {
            psGetEntryByRawId2.setInt(1, feedId);
            psGetEntryByRawId2.setString(2, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntryByRawId2);
            if (resultSet.next()) {
                result = getFeedEntry(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryByRawId", e);
        }
        LOGGER.trace("<getEntryByRawId");
        return result;
    }

    public FeedItem getFeedEntryById(int id) {
        LOGGER.trace(">getEntryById");
        FeedItem result = null;
        try {
            psGetEntryById.setInt(1, id);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntryById);
            if (resultSet.next()) {
                result = getFeedEntry(resultSet);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getEntryById", e);
        }
        LOGGER.trace("<getEntryById");
        return result;
    }

    /**
     * Get the specified count of feed entries, starting at offset.
     * 
     * @param limit
     * @param offset
     * @return
     */
    public List<FeedItem> getFeedEntries(int limit, int offset) {
        LOGGER.trace(">getFeedEntries");
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            psGetEntries.setInt(1, limit);
            psGetEntries.setInt(2, offset);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetEntries);

            while (resultSet.next()) {
                result.add(getFeedEntry(resultSet));
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedEntries", e);
        }
        LOGGER.trace("<getFeedEntries");
        return result;
    }

    /**
     * Get FeedEntries by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_entries table.
     * 
     * @param sqlQuery
     * @return
     */
    @Override
    public List<FeedItem> getFeedEntries(String sqlQuery) {
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            ResultSet rs = connection.createStatement().executeQuery(sqlQuery);
            while (rs.next()) {
                result.add(getFeedEntry(rs));
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.error(sqlQuery + " : " + e);

        }
        return result;
    }

    public List<FeedItem> getFeedEntriesForEvaluation(String sqlQuery) {
        List<FeedItem> result = new LinkedList<FeedItem>();
        try {
            ResultSet rs = connection.createStatement().executeQuery(sqlQuery);
            while (rs.next()) {
                FeedItem entry = getFeedEntry(rs);
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
    private FeedItem getFeedEntry(ResultSet resultSet) throws SQLException {

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

    /**
     * Get tags for specified FeedEntry. Result as sorted descendingly.
     * 
     * @param entry
     * @return
     */
    public List<Tag> getTags(FeedItem entry) {
        List<Tag> tags = new ArrayList<Tag>();

        try {
            psGetEntrysTags.setInt(1, entry.getId());
            ResultSet rs = psGetEntrysTags.executeQuery();
            while (rs.next()) {
                String tagName = rs.getString("name");
                float tagWeight = rs.getFloat("weight");
                Tag tag = new Tag(tagName, tagWeight);
                tags.add(tag);
            }
            Collections.sort(tags, new TagComparator());
            rs.close();
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        return tags;
    }

    public void assignTags(FeedItem entry, List<Tag> tags) {

        try {

            // if we have no tags, we add a relation with -1 to indicate that no tags could be assigned.
            if (tags.isEmpty()) {
                psTagFeedEntry.setInt(1, entry.getId());
                psTagFeedEntry.setInt(2, -1);
                psTagFeedEntry.setFloat(3, 0);
                psTagFeedEntry.executeUpdate();
            }

            for (Tag tag : tags) {

                // get tag ID from database, or add tag to DB if not exists
                psGetTagId.setString(1, tag.getName());
                ResultSet rsGetTagId = psGetTagId.executeQuery();
                int tagId;
                if (rsGetTagId.next()) {

                    tagId = rsGetTagId.getInt(1);

                } else {

                    psInsertTag.setString(1, tag.getName());
                    psInsertTag.executeUpdate();
                    tagId = DatabaseManager.getInstance().getLastInsertID();

                }
                rsGetTagId.close();

                // add relation between tag and feed
                psTagFeedEntry.setInt(1, entry.getId());
                psTagFeedEntry.setInt(2, tagId);
                psTagFeedEntry.setFloat(3, tag.getOriginalWeight());
                psTagFeedEntry.executeUpdate();

            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
    }

    @Override
    public Set<Integer> getFeedEntryIdsTaggedAs(String tag) {
        StopWatch sw = new StopWatch();

        Set<Integer> entryIds = new HashSet<Integer>();

        try {
            getEntryIdByTag.setString(1, tag);
            ResultSet rs = getEntryIdByTag.executeQuery();
            while (rs.next()) {
                entryIds.add(rs.getInt(1));
            }
            rs.close();
        } catch (SQLException e) {
            LOGGER.error(e);
        }

        LOGGER.trace("getEntryIdsTaggedAs " + tag + " : " + sw.getElapsedTimeString());
        return entryIds;
    }

    public void deleteFeedEntryById(int id) {

        try {
            psDeleteEntryById.setInt(1, id);
            DatabaseManager.getInstance().runUpdate(psDeleteEntryById);
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
    public Iterator<FeedItem> getFeedEntries() {

        final ResultSet rs = DatabaseManager.getInstance().runQuery(psGetAllEntries);
        return new Iterator<FeedItem>() {

            /** reference to the next FeedEntry which can be retrieved via next(). */
            private FeedItem next = null;

            @Override
            public boolean hasNext() {
                boolean hasNext = true;
                try {
                    if (next == null) {
                        if (rs.next()) {
                            next = getFeedEntry(rs);
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
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_entries");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feeds");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_entries_tags");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_entry_tag");
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

        Iterator<FeedItem> iterator = fd.getFeedEntries();
        System.out.println(sw.getElapsedTimeString());

        int counter = 0;
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
            counter++;
        }
        System.out.println(counter);
        System.out.println(sw.getElapsedTimeString());

        System.exit(0);
        FeedItem dummy = new FeedItem();
        dummy.setId(123);
        List<Tag> tags = fd.getTags(dummy);
        System.out.println(tags);

    }

}