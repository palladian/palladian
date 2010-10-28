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
 * @author Sandro Reichert
 */
public class EvaluationDatabase {

    /** the instance of this class */
    private final static EvaluationDatabase INSTANCE = new EvaluationDatabase();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EvaluationDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
//    private PreparedStatement psGetEntries;
    private PreparedStatement psGetPolls;
    private PreparedStatement psGetFeedSizeDistribution;
    private PreparedStatement psGetAverageUpdateIntervals;
    
    private PreparedStatement psGetAvgScoreMinByPollFromAdaptivePoll;
    private PreparedStatement psGetAvgScoreMinByPollFromFixLearnedPoll;
    private PreparedStatement psGetAvgScoreMinByPollFromFix1440Poll;
    private PreparedStatement psGetAvgScoreMinByPollFromFix60Poll;
    private PreparedStatement psGetAvgScoreMinByPollFromFix720Poll;
    private PreparedStatement psGetAvgScoreMinByPollFromPorbabilisticPoll;
    
    private PreparedStatement psGetAvgScoreMaxByPollFromAdaptivePoll;
    private PreparedStatement psGetAvgScoreMaxByPollFromFixLearnedPoll;
    private PreparedStatement psGetAvgScoreMaxByPollFromFix1440Poll;
    private PreparedStatement psGetAvgScoreMaxByPollFromFix60Poll;
    private PreparedStatement psGetAvgScoreMaxByPollFromFix720Poll;
    private PreparedStatement psGetAvgScoreMaxByPollFromPorbabilisticPoll;
    
    private PreparedStatement psGetSumTransferVolumeByHourFromAdaptiveMaxTime;
    private PreparedStatement psGetSumTransferVolumeByHourFroProbabilisticMaxTime;

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
        
//        psGetEntries = connection
//                .prepareStatement("SELECT id, updateClass, supportsETag FROM feed_evaluation_1 LIMIT ? OFFSET ?");
        
        psGetFeedSizeDistribution = connection
                .prepareStatement("SELECT id, updateClass, sizeOfPoll FROM feed_evaluation_fix1440_max_min_poll WHERE numberOfPoll = 1 AND sizeOfPoll > 0");
        psGetPolls = connection
                .prepareStatement("SELECT id, updateClass, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize, numberOfPoll, pollTimestamp, pollHourOfDay, pollMinuteOfDay, checkInterval, windowSize, sizeOfPoll, numberMissedNewEntries, percentageNewEntries, delay, scoreMax, scoreMin FROM feed_evaluation_fix1440_max_min_poll");
        psGetAverageUpdateIntervals = connection
                .prepareStatement("SELECT id, updateClass, averageUpdateInterval FROM feed_evaluation_update_intervals");
     
        psGetAvgScoreMinByPollFromAdaptivePoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_adaptive_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFixLearnedPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix_learned_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix1440Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix1440_max_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix60Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix60_max_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix720Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix720_max_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromPorbabilisticPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_probabilistic_min_poll WHERE scoreMin > 0 AND numberOfPoll < ? GROUP BY numberOfPoll");
        
    
        psGetAvgScoreMaxByPollFromAdaptivePoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_adaptive_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMaxByPollFromFixLearnedPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix_learned_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMaxByPollFromFix1440Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix1440_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMaxByPollFromFix60Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix60_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMaxByPollFromFix720Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix720_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        psGetAvgScoreMaxByPollFromPorbabilisticPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_probabilistic_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");

    
        
        
        
        psGetSumTransferVolumeByHourFromAdaptiveMaxTime  = connection
                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_adaptive_max_time WHERE pollTimestamp <= 1288108800 GROUP BY DAY, pollHourOfDay");
        
        
//        TODO:
//        psGetSumTransferVolumeByHourFroFixLearnedMaxTime        
//                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix_learned_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix1440Poll  = connection
//                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix1440_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix60Poll  = connection
//                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix60_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix720Poll  = connection
//                .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix720_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
        
        psGetSumTransferVolumeByHourFroProbabilisticMaxTime = connection
                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_probabilistic_max_time WHERE pollTimestamp <= 1288108800 GROUP BY DAY, pollHourOfDay");

        
        
        
        
    }
    

    public List<EvaluationFeedPoll> getFeedPolls() {
        LOGGER.trace(">getFeedPolls");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {

            PreparedStatement ps = psGetPolls;

            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
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
                feedPoll.setScoreMax(resultSet.getFloat(17));
                feedPoll.setScoreMin(resultSet.getFloat(18));
                result.add(feedPoll);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedPolls", e);
        }
        LOGGER.trace("<getFeedPolls " + result.size());
        return result;
    }

    
    public List<EvaluationFeedPoll> getFeedSizes() {
        LOGGER.trace(">getFeedSizes");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetFeedSizeDistribution);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setActivityPattern(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getFloat(3));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getFeedSizes", e);
        }
        LOGGER.trace("<getFeedSizes");
        return result;
    }
    
    
    public List<EvaluationItemIntervalItem> getAverageUpdateIntervals() {
        LOGGER.trace(">getAverageUpdateIntervals");
        List<EvaluationItemIntervalItem> result = new LinkedList<EvaluationItemIntervalItem>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAverageUpdateIntervals);
            while (resultSet.next()) {
                EvaluationItemIntervalItem itemIntervalItem = new EvaluationItemIntervalItem();
                itemIntervalItem.setFeedID(resultSet.getInt(1));
                itemIntervalItem.setActivityPattern(resultSet.getInt(2));
                itemIntervalItem.setAverageUpdateInterval(resultSet.getLong(3));
                result.add(itemIntervalItem);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageUpdateIntervals", e);
        }
        LOGGER.trace("<getAverageUpdateIntervals");
        return result;
    }


    
    /**
     * gets the Results from table feed_evaluation_fix1440_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxFIX1440(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxFIX1440");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromFix1440Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromFix1440Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxFIX1440", e);
        }
        LOGGER.trace("<getAverageScoreMaxFIX1440");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix60_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxFIX60(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxFIX60");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromFix60Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromFix60Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxFIX60", e);
        }
        LOGGER.trace("<getAverageScoreMaxFIX60");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix720_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxFIX720(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxFIX720");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromFix720Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromFix720Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxFIX720", e);
        }
        LOGGER.trace("<getAverageScoreMaxFIX720");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix_learned_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxFIXlearned(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxFIXlearned");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromFixLearnedPoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromFixLearnedPoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxFIXlearned", e);
        }
        LOGGER.trace("<getAverageScoreMaxFIXlearned");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_adaptive_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxAdaptive(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxAdaptive");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromAdaptivePoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromAdaptivePoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxAdaptive", e);
        }
        LOGGER.trace("<getAverageScoreMaxAdaptive");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_probabilistic_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMaxProbabilistic(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMaxProbabilistic");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMaxByPollFromPorbabilisticPoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMaxByPollFromPorbabilisticPoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMaxProbabilistic", e);
        }
        LOGGER.trace("<getAverageScoreMaxProbabilistic");
        return result;
    }
    
    
    
    /**
     * gets the Results from table feed_evaluation_fix1440_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIX1440(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinFIX1440");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromFix1440Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromFix1440Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinFIX1440", e);
        }
        LOGGER.trace("<getAverageScoreMinFIX1440");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix60_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIX60(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinFIX60");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromFix60Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromFix60Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinFIX60", e);
        }
        LOGGER.trace("<getAverageScoreMinFIX60");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix720_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIX720(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinFIX720");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromFix720Poll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromFix720Poll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinFIX720", e);
        }
        LOGGER.trace("<getAverageScoreMinFIX720");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix_learned_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIXlearned(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinFIXlearned");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromFixLearnedPoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromFixLearnedPoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinFIXlearned", e);
        }
        LOGGER.trace("<getAverageScoreMinFIXlearned");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_adaptive_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinAdaptive(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinAdaptive");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromAdaptivePoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromAdaptivePoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinAdaptive", e);
        }
        LOGGER.trace("<getAverageScoreMinAdaptive");
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_probabilistic_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAverageScoreMinProbabilistic(final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace(">getAverageScoreMinProbabilistic");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetAvgScoreMinByPollFromPorbabilisticPoll.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetAvgScoreMinByPollFromPorbabilisticPoll);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getAverageScoreMinProbabilistic", e);
        }
        LOGGER.trace("<getAverageScoreMinProbabilistic");
        return result;
    }
    
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromAdaptiveMaxTime() {
        LOGGER.trace(">getSumTransferVolumeByHourFromAdaptiveMaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetSumTransferVolumeByHourFromAdaptiveMaxTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setScoreAVG(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromAdaptiveMaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromAdaptiveMaxTime");
        return result;
    }
    
    
    
    
    
}
