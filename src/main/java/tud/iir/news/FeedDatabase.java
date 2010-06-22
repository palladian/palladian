package tud.iir.news;

import java.sql.CallableStatement;
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
 * 
 */
public class FeedDatabase implements FeedStore {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(FeedDatabase.class);
    private final static FeedDatabase INSTANCE = new FeedDatabase();

    private FeedDatabase() {
    }

    public static FeedDatabase getInstance() {
        return INSTANCE;
    }

    @Override
    public synchronized boolean addFeed(Feed feed) {
        LOGGER.trace(">addFeed " + feed);
        boolean added = false;
        try {
            PreparedStatement ps = DatabaseManager.getInstance().psAddFeed;
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
            int update = DatabaseManager.getInstance().runUpdate(ps);
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
            PreparedStatement ps = DatabaseManager.getInstance().psUpdateFeed;

            switch (FeedChecker.getInstance().getCheckApproach()) {
                case FeedChecker.CHECK_FIXED:
                    if (FeedChecker.getInstance().getCheckInterval() == -1) {
                        ps = DatabaseManager.getInstance().psUpdateFeed_fixed_learned;
                    }
                    break;
                case FeedChecker.CHECK_ADAPTIVE:
                    ps = DatabaseManager.getInstance().psUpdateFeed_adaptive;
                    break;
                case FeedChecker.CHECK_PROBABILISTIC:
                    ps = DatabaseManager.getInstance().psUpdateFeed_probabilistic;
                    break;
                default:
                    break;
            }

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

        PreparedStatement ps = DatabaseManager.getInstance().psGetFeedPostDistribution;
        try {
            ps.setLong(1, feed.getId());
            ResultSet rs = DatabaseManager.getInstance().runQuery(ps);

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
        PreparedStatement ps = DatabaseManager.getInstance().psUpdateFeedPostDistribution;
        try {
            for (java.util.Map.Entry<Integer, int[]> distributionEntry : postDistribution.entrySet()) {
                ps.setLong(1, feed.getId());
                ps.setInt(2, distributionEntry.getKey());
                ps.setInt(3, distributionEntry.getValue()[0]);
                ps.setInt(4, distributionEntry.getValue()[1]);
                DatabaseManager.getInstance().runUpdate(ps);
            }
        } catch (SQLException e) {
            LOGGER.error("could not update feed post distribution for " + feed.getFeedUrl());
        }
    }

    /**
     * When the check approach is switched we need to reset learned and calculated values such as check intervals, checks, lastHeadlines etc.
     */
    public synchronized void changeCheckApproach() {
        DatabaseManager.getInstance().runUpdate(DatabaseManager.getInstance().psChangeCheckApproach);
    }

    @Override
    public List<Feed> getFeeds() {
        LOGGER.trace(">getFeeds");
        List<Feed> result = new LinkedList<Feed>();
        try {

            PreparedStatement ps = DatabaseManager.getInstance().psGetFeeds;

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
            PreparedStatement ps = DatabaseManager.getInstance().psGetFeedByUrl;
            ps.setString(1, feedUrl);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
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
            	LOGGER.info("feed with " + feedUrl + " not found.");
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
            PreparedStatement ps = DatabaseManager.getInstance().psGetFeedByID;
            ps.setInt(1, feedID);

            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
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
            PreparedStatement ps = DatabaseManager.getInstance().psAddFeedEntry;
            ps.setLong(1, feed.getId());
            ps.setString(2, entry.getTitle());
            ps.setString(3, entry.getLink());
            ps.setString(4, entry.getRawId());
            ps.setTimestamp(5, entry.getAddedSQLTimestamp());
            ps.setString(6, entry.getText());
            ps.setString(7, entry.getPageText());
            // store tags in comma separated column
            ps.setString(8, StringUtils.join(entry.getTags(), ","));

            // check affected rows
            int update = DatabaseManager.getInstance().runUpdate(ps);
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
            PreparedStatement ps = DatabaseManager.getInstance().psGetEntryByRawId;
            ps.setString(1, rawId);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            if (resultSet.next()) {
                result = new FeedEntry();
                result.setId(resultSet.getInt(1));
                result.setTitle(resultSet.getString(2));
                result.setLink(resultSet.getString(3));
                result.setRawId(resultSet.getString(4));
                result.setPublished(resultSet.getDate(5));
                result.setText(resultSet.getString(6));
                result.setPageText(resultSet.getString(7));
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

    public void cleanTables() {
        LOGGER.trace(">cleanTables");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_entries");
        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feeds");
        LOGGER.trace("<cleanTables");
    }

	//@Override
	public synchronized void assignConcept(String concept, Feed feed) {
		LOGGER.trace(">assignFeedDomain" + feed.getId() + " " + concept);
		boolean assigned = false;
		try {
			CallableStatement pc = DatabaseManager.getInstance().pcAssignFeedConcept;
			pc.setInt(1, feed.getId());
			pc.setString(2, concept);
			int result = pc.executeUpdate();
			if (result == 1) {
				assigned = true;
			}
		} catch (SQLException e) {
			LOGGER.error("assignConcept", e);
		}
		
		LOGGER.trace("<assignFeedDomain " + assigned);
	}

}