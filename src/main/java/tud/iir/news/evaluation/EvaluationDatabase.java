package tud.iir.news.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.news.FeedEntry;
import tud.iir.persistence.DatabaseManager;



/**
 * 
 * 
 * @author Sandro Reichert
 */
public class EvaluationDatabase {
    
    /** the instance of this class */
    private final static EvaluationDatabase INSTANCE = new EvaluationDatabase();
    
    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EvaluationDatabase.class);
    
    // ////////////////// feed prepared statements ////////////////////
        private PreparedStatement psGetEntries;
        private PreparedStatement psGetFeeds;
    
    
    private EvaluationDatabase() {
        try {
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    public static EvaluationDatabase getInstance() {
        return INSTANCE;
    }
    
    
    
    private void prepareStatements() throws SQLException {
        // // prepared statements for feeds
        Connection connection = DatabaseManager.getInstance().getConnection();
        /* TODO: hier meinen Kram einfügen*/
        psGetEntries = connection.prepareStatement("SELECT id, updateClass, supportsETag FROM feed_evaluation_1 LIMIT ? OFFSET ?");
        psGetFeeds = connection
        .prepareStatement("SELECT id, updateClass, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize, numberOfPoll, pollTimestamp, pollHourOfDay, pollMinuteOfDay, checkInterval, windowSize, sizeOfPoll, numberMissedNewEntries, percentageNewEntries, delay, score FROM feed_evaluation_1");
    
    }
    
    
    public List<EvaluationDataObject> getFeedPolls() {
        LOGGER.trace(">getFeedPolls");
        List<EvaluationDataObject> result = new LinkedList<EvaluationDataObject>();
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
                EvaluationDataObject feedPoll = new EvaluationDataObject();
                feedPoll.setId(resultSet.getInt(1));
                feedPoll.setActivityPattern(resultSet.getInt(2));
                feedPoll.setSupportsETag(resultSet.getBoolean(3));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(4));
                feedPoll.seteTagResponseSize(resultSet.getInt(5));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(6));
                feedPoll.setNumberOfPoll(resultSet.getInt(7));
                feedPoll.setPollTimestamp(resultSet.getLong(8));
                feedPoll.setPollHourOfDay(resultSet.getInt(9));
                feedPoll.setPollMinuteOfDay(resultSet.getInt(10));
                feedPoll.setCheckInterval(resultSet.getFloat(11));
                feedPoll.setWindowSize(resultSet.getInt(12));
                feedPoll.setSizeOfPoll(resultSet.getFloat(13));
                feedPoll.setNumberMissedNewEntries(resultSet.getInt(14));
                feedPoll.setPercentageNewEntries(resultSet.getFloat(15));
                feedPoll.setDelay(resultSet.getDouble(16));
                feedPoll.setScoreMin(resultSet.getFloat(17));
//                feedPoll.setScoreMin(resultSet.getFloat(18));
                result.add(feedPoll);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedPolls", e);
        }
        LOGGER.trace("<getFeedPolls " + result.size());
        return result;
    }
    
    
    
    
    
    
    public List<FeedEntry> getFeedPolls_TEMP(int limit, int offset) {
        LOGGER.trace(">getFeedPolls");
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
    

    
//    public void clearFeedTables() {
//        LOGGER.trace(">cleanTables");
//        DatabaseManager.getInstance().runUpdate("TRUNCATE TABLE feed_evaluation_1");
//        LOGGER.trace("<cleanTables");
//    }
    
    
}


