package tud.iir.news.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

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
    private PreparedStatement psGetAvgScoreMinByPollFromPorbabilisticPoll;
    
    private PreparedStatement psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll;
    private PreparedStatement psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll;
    private PreparedStatement psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll;
    private PreparedStatement psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll;
    private PreparedStatement psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll;
    
    private PreparedStatement psGetSumTransferVolumeByHourFromAdaptiveMaxTime;
    private PreparedStatement psGetTransferVolumeByHourFromFix1440Time;
    private PreparedStatement psGetTransferVolumeByHourFromFix60Time;
    private PreparedStatement psGetTransferVolumeByHourFromFixLearnedMaxTime;
    private PreparedStatement psGetSumTransferVolumeByHourFromProbabilisticMaxTime;
    
    private PreparedStatement psGetSumTransferVolumeByHourFromAdaptiveMinTime;
    private PreparedStatement psGetTransferVolumeByHourFromFixLearnedMinTime;
    private PreparedStatement psGetSumTransferVolumeByHourFromProbabilisticMinTime;

    private PreparedStatement psGetTransferVolumeByHourFromProbabilisticMinEtag304Time;
    private PreparedStatement psGetTransferVolumeByHourFromAdaptiveMinEtag304Time;
    
    

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
                .prepareStatement("SELECT id, updateClass, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize, numberOfPoll, pollTimestamp, pollHourOfDay, pollMinuteOfDay, checkInterval, windowSize, sizeOfPoll, numberMissedNewEntries, percentageNewEntries, delay, scoreMax, scoreMin FROM feed_evaluation_adaptive_max_time");
        psGetAverageUpdateIntervals = connection
                .prepareStatement("SELECT id, updateClass, averageUpdateInterval FROM feed_evaluation_update_intervals");
     
        
        // for timeliness2 (ScoreMin vs. Polls)
        psGetAvgScoreMinByPollFromAdaptivePoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_adaptive_min_poll WHERE scoreMin > 0 AND numberOfPoll <= ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFixLearnedPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix_learned_min_poll WHERE scoreMin > 0 AND numberOfPoll <= ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix1440Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix1440_max_min_poll WHERE scoreMin > 0 AND numberOfPoll <= ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix60Poll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_fix60_max_min_poll WHERE scoreMin > 0 AND numberOfPoll <= ? GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromPorbabilisticPoll  = connection
                .prepareStatement("SELECT numberOfPoll, AVG(scoreMin) FROM feed_evaluation_probabilistic_min_poll WHERE scoreMin > 0 AND numberOfPoll <= ? GROUP BY numberOfPoll");
        

        
// // original
//        psGetAvgScoreMaxByPollFromAdaptivePoll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_adaptive_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFixLearnedPoll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix_learned_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix1440Poll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix1440_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix60Poll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix60_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromFix720Poll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_fix720_max_min_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");
//        psGetAvgScoreMaxByPollFromPorbabilisticPoll  = connection
        // .prepareStatement("SELECT numberOfPoll, AVG(scoreMax) FROM feed_evaluation_probabilistic_max_poll WHERE scoreMax IS NOT NULL AND numberOfPoll < ? GROUP BY numberOfPoll");


        // get average percentages of new entries by poll for MAX-policy
        psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(percentageNewEntries*(windowSize-1)/windowSize) FROM feed_evaluation_adaptive_max_poll WHERE percentageNewEntries IS NOT NULL AND numberOfPoll <= ? AND id IN (SELECT DISTINCT id FROM feed_evaluation_adaptive_max_poll WHERE numberOfPoll = 1000) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(percentageNewEntries*(windowSize-1)/windowSize) FROM feed_evaluation_fix_learned_max_poll WHERE percentageNewEntries IS NOT NULL AND numberOfPoll <= ? AND id IN (SELECT DISTINCT id FROM feed_evaluation_adaptive_max_poll WHERE numberOfPoll = 1000) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(percentageNewEntries*(windowSize-1)/windowSize) FROM feed_evaluation_fix1440_max_min_poll WHERE percentageNewEntries IS NOT NULL AND numberOfPoll <= ? AND id IN (SELECT DISTINCT id FROM feed_evaluation_adaptive_max_poll WHERE numberOfPoll = 1000) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(percentageNewEntries*(windowSize-1)/windowSize) FROM feed_evaluation_fix60_max_min_poll WHERE percentageNewEntries IS NOT NULL AND numberOfPoll <= ? AND id IN (SELECT DISTINCT id FROM feed_evaluation_adaptive_max_poll WHERE numberOfPoll = 1000) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(percentageNewEntries*(windowSize-1)/windowSize) FROM feed_evaluation_probabilistic_max_poll WHERE percentageNewEntries IS NOT NULL AND numberOfPoll <= ? AND id IN (SELECT DISTINCT id FROM feed_evaluation_adaptive_max_poll WHERE numberOfPoll = 1000) GROUP BY numberOfPoll");
    
        
        
        // 6521 = Anzahl Stunden seit 01.01.2010 00:00 bis zum Start des Experiments am 28.09.2010 16:00
        // Wert wird verwendet, damit das Experiment von Stunde 1 bis 672 (4 Wochen) läuft
        psGetSumTransferVolumeByHourFromAdaptiveMaxTime  = connection
        // BACKUP
        // .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_adaptive_max_time WHERE pollTimestamp <= ? GROUP BY DAY, pollHourOfDay");
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_adaptive_max_time WHERE pollTimestamp <= ? ORDER BY id, pollTimestamp ASC");

        psGetTransferVolumeByHourFromFix1440Time  = connection
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_fix1440_max_min_time WHERE pollTimestamp <= ? ORDER BY id, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFix60Time  = connection
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_fix60_max_min_time WHERE pollTimestamp <= ? AND id BETWEEN ? AND ? ORDER BY id, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFixLearnedMaxTime = connection
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_fix_learned_max_time WHERE pollTimestamp <= ? AND id BETWEEN ? AND ? ORDER BY id, pollTimestamp ASC");
        psGetSumTransferVolumeByHourFromProbabilisticMaxTime = connection
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_probabilistic_max_time WHERE pollTimestamp <= ? ORDER BY id, pollTimestamp ASC");
        // .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_probabilistic_max_time WHERE pollTimestamp <= ? AND id = 1 ORDER BY id, pollTimestamp ASC");
        
        // uncomment to calculate Volume for class zombie!! 
//        psGetSumTransferVolumeByHourFromAdaptiveMaxTime  = connection
//                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_adaptive_max_time WHERE pollTimestamp <= 1288108800 AND updateClass = 4 GROUP BY DAY, pollHourOfDay");
//        psGetTransferVolumeByHourFromFix1440Time  = connection
//                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll FROM feed_evaluation_fix1440_max_min_time WHERE pollTimestamp <= 1288108800 AND updateClass = 4 ORDER BY id, pollTimestamp ASC");
//        psGetTransferVolumeByHourFromFix60Time  = connection
//                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll FROM feed_evaluation_fix60_max_min_time WHERE pollTimestamp <= 1288108800 AND id BETWEEN ? AND ? AND updateClass = 4 ORDER BY id, pollTimestamp ASC");
//        psGetTransferVolumeByHourFromFix720Time  = connection
//                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll FROM feed_evaluation_fix720_max_min_time WHERE pollTimestamp <= 1288108800 AND updateClass = 4 ORDER BY id, pollTimestamp ASC");
//        psGetTransferVolumeByHourFromFixLearnedMaxTime = connection
//                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp FROM feed_evaluation_fix_learned_max_time WHERE pollTimestamp <= 1288108800 AND id BETWEEN ? AND ? AND updateClass = 4 ORDER BY id, pollTimestamp ASC"); 
//        psGetSumTransferVolumeByHourFromProbabilisticMaxTime = connection
//                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_probabilistic_max_time WHERE pollTimestamp <= 1288108800 AND updateClass = 4 GROUP BY DAY, pollHourOfDay");

        
        
        
        
        psGetSumTransferVolumeByHourFromAdaptiveMinTime  = connection
                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_adaptive_min_time WHERE pollTimestamp <= 1288108800 GROUP BY DAY, pollHourOfDay");
        psGetTransferVolumeByHourFromFixLearnedMinTime = connection
                .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_fix_learned_min_time WHERE pollTimestamp <= 1288108800 AND id BETWEEN ? AND ? ORDER BY id, pollTimestamp ASC");
        psGetSumTransferVolumeByHourFromProbabilisticMinTime = connection
                .prepareStatement("SELECT DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp)) AS DAY, pollHourOfDay, SUM(sizeOfPoll) FROM feed_evaluation_probabilistic_min_time WHERE pollTimestamp <= 1288108800 GROUP BY DAY, pollHourOfDay");
        

        psGetTransferVolumeByHourFromAdaptiveMinEtag304Time = connection
        .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_probabilistic_min_time WHERE pollTimestamp <= 1288108800 AND id BETWEEN ? AND ? ORDER BY id, pollTimestamp ASC");
        psGetTransferVolumeByHourFromAdaptiveMinEtag304Time = connection
        .prepareStatement("SELECT id, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+pollHourOfDay-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, supportsETag, supportsConditionalGet, eTagResponseSize, conditionalGetResponseSize FROM feed_evaluation_adaptive_min_time WHERE pollTimestamp <= 1288108800 AND id BETWEEN ? AND ? ORDER BY id, pollTimestamp ASC");
        
        
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
                feedPoll.setETagResponseSize(resultSet.getInt(5));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(6));
                feedPoll.setNumberOfPoll(resultSet.getInt(7));
                feedPoll.setPollTimestamp(resultSet.getLong(8));
                feedPoll.setPollHourOfDay(resultSet.getInt(9));
                feedPoll.setPollMinuteOfDay(resultSet.getInt(10));
                feedPoll.setCheckInterval(resultSet.getInt(11));
                feedPoll.setWindowSize(resultSet.getInt(12));
                feedPoll.setSizeOfPoll(resultSet.getInt(13));
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
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
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
     * Private helper to processes a prepared Statement to get an average value like scoreMin or percentaegNewEntries by
     * poll and returns a result set containing the average value for each numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average value for.
     * @param ps The PreparedStatement to process.
     * @return a result set containing the average value for each numberOfPoll.
     */
    private List<EvaluationFeedPoll> getAverageValueByPoll(final int MAX_NUMBER_OF_POLLS, final PreparedStatement ps) {
        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps.toString());
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ps.setInt(1, MAX_NUMBER_OF_POLLS);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setAverageValue(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error(">getAverageValueByPoll processing PreparedStatement " + ps.toString(), e);
        }
        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps.toString());
        return result;
    }
    

    /**
     * gets the Results from table feed_evaluation_fix1440_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesByPollFromFix1440MaxMinPoll(
            final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS,
                psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll);
    }
    

    /**
     * gets the Results from table feed_evaluation_fix60_max_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesByPollFromFIX60MaxMinPoll(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS,
                psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll);
    }
    

    
    /**
     * gets the Results from table feed_evaluation_fix_learned_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesByPollFromFIXlearnedMaxPoll(
            final int MAX_NUMBER_OF_POLLS) {
        LOGGER.trace("<getAverageScoreMaxFIXlearned");
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS,
                psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll);
    }
    

    /**
     * gets the Results from table feed_evaluation_adaptive_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesByPollFromAdaptiveMaxPoll(
            final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS,
                psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll);
    }
    

    /**
     * gets the Results from table feed_evaluation_probabilistic_min_poll
     * 
     * @return List<EvaluationFeedPoll> where each EvaluationFeedPoll has numberOfPoll and scoreAvg
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesByPollFromProbabilisticMaxPoll(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS,
                psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll);
    }



    /**
     * Queries the database to get the average scoreMin by numberOfPoll from table feed_evaluation_fix1440_max_min_poll
     * and returns a result set containing the average scoreMin for each numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin for each numberOfPoll.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIX1440(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, psGetAvgScoreMinByPollFromFix1440Poll);
    }

    /**
     * Queries the database to get the average scoreMin by numberOfPoll from table feed_evaluation_fix60_max_min_poll
     * and returns a result set containing the average scoreMin for each numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin for each numberOfPoll.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIX60(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, psGetAvgScoreMinByPollFromFix60Poll);
    }

    /**
     * Queries the database to get the average scoreMin by numberOfPoll from table feed_evaluation_fix_learned_min_poll
     * and returns a result set containing the average scoreMin for each numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin for each numberOfPoll.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinFIXlearned(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, psGetAvgScoreMinByPollFromFixLearnedPoll);
    }

    /**
     * Queries the database to get the average scoreMin by numberOfPoll from table feed_evaluation_adaptive_min_poll
     * and returns a result set containing the average scoreMin for each numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin for each numberOfPoll.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinAdaptive(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, psGetAvgScoreMinByPollFromAdaptivePoll);
    }

    /**
     * Queries the database to get the average scoreMin by numberOfPoll from table
     * feed_evaluation_probabilistic_min_poll and returns a result set containing the average scoreMin for each
     * numberOfPoll.
     * 
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin for each numberOfPoll.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinProbabilistic(final int MAX_NUMBER_OF_POLLS) {
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, psGetAvgScoreMinByPollFromPorbabilisticPoll);
    }
    
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromAdaptiveMaxTime(final long TIMESTAMP_MAX) {
        LOGGER.trace(">getSumTransferVolumeByHourFromAdaptiveMaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetSumTransferVolumeByHourFromAdaptiveMaxTime.setLong(1, TIMESTAMP_MAX);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetSumTransferVolumeByHourFromAdaptiveMaxTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                // feedPoll.setHourOfExperiment(resultSet.getInt(1));
                // feedPoll.setCulmulatedSizeofPolls(resultSet.getLong(4));

                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));

                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromAdaptiveMaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromAdaptiveMaxTime");
        return result;
    }
    

    
    
    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromFix1440MaxMinTime(final long TIMESTAMP_MAX) {
        LOGGER.trace(">getSumTransferVolumeByHourFromFix1440MaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromFix1440Time.setLong(1, TIMESTAMP_MAX);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromFix1440Time);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromFix1440MaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromFix1440MaxTime");
        return result;
    }
    
    
    
    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromFix60MaxMinTime(final long TIMESTAMP_MAX,
            final int FEED_ID_START, final int FEED_ID_LIMIT) {
        LOGGER.trace(">getSumTransferVolumeByHourFromFix60MaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromFix60Time.setLong(1, TIMESTAMP_MAX);
            psGetTransferVolumeByHourFromFix60Time.setInt(2, FEED_ID_START);
            psGetTransferVolumeByHourFromFix60Time.setInt(3, FEED_ID_LIMIT);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromFix60Time);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromFix60MaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromFix60MaxTime");
        return result;
    }
            
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromFixLearnedMaxTime(final long TIMESTAMP_MAX,
            final int FEED_ID_START, final int FEED_ID_LIMIT) {
        LOGGER.trace(">getSumTransferVolumeByHourFromFixLearnedMaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromFixLearnedMaxTime.setLong(1, TIMESTAMP_MAX);
            psGetTransferVolumeByHourFromFixLearnedMaxTime.setInt(2, FEED_ID_START);
            psGetTransferVolumeByHourFromFixLearnedMaxTime.setInt(3, FEED_ID_LIMIT);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromFixLearnedMaxTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromFixLearnedMaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromFixLearnedMaxTime");
        return result;
    }
      
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromProbabilisticMaxTime(final long TIMESTAMP_MAX) {
        LOGGER.trace(">getSumTransferVolumeByHourFromProbabilisticMaxTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetSumTransferVolumeByHourFromProbabilisticMaxTime.setLong(1, TIMESTAMP_MAX);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetSumTransferVolumeByHourFromProbabilisticMaxTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromProbabilisticMaxTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromProbabilisticMaxTime");
        return result;
    }

    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromAdaptiveMinTime() {
        LOGGER.trace(">getSumTransferVolumeByHourFromAdaptiveMinTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetSumTransferVolumeByHourFromAdaptiveMinTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setHourOfExperiment(resultSet.getInt(1));
                feedPoll.setCulmulatedSizeofPolls(resultSet.getLong(4));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromAdaptiveMinTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromAdaptiveMinTime");
        return result;
    }
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromFixLearnedMinTime(final int FEED_ID_START, final int FEED_ID_LIMIT) {
        LOGGER.trace(">getSumTransferVolumeByHourFromFixLearnedMinTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromFixLearnedMinTime.setInt(1, FEED_ID_START);
            psGetTransferVolumeByHourFromFixLearnedMinTime.setInt(2, FEED_ID_LIMIT);            
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromFixLearnedMinTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromFixLearnedMinTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromFixLearnedMinTime");
        return result;
    }
      
    

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getSumTransferVolumeByHourFromProbabilisticMinTime() {
        LOGGER.trace(">getSumTransferVolumeByHourFromProbabilisticMinTime");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetSumTransferVolumeByHourFromProbabilisticMinTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setHourOfExperiment(resultSet.getInt(1));
                feedPoll.setCulmulatedSizeofPolls(resultSet.getLong(4));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromProbabilisticMinTime", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromProbabilisticMinTime");
        return result;
    }    

    
    
   

    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getTransferVolumeByHourFromProbabilisticMinEtag304Time(final int FEED_ID_START, final int FEED_ID_LIMIT) {
        LOGGER.trace(">getSumTransferVolumeByHourFromProbabilisticMinEtag304Time");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromProbabilisticMinEtag304Time.setInt(1, FEED_ID_START);
            psGetTransferVolumeByHourFromProbabilisticMinEtag304Time.setInt(2, FEED_ID_LIMIT);            
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromProbabilisticMinEtag304Time);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getSumTransferVolumeByHourFromProbabilisticMinEtag304Time", e);
        }
        LOGGER.trace("<getSumTransferVolumeByHourFromProbabilisticMinEtag304Time");
        return result;
    }
      


    /**
     * @return List<EvaluationFeedPoll> 
     */
    public List<EvaluationFeedPoll> getTransferVolumeByHourFromAdaptiveMinEtag304Time(final int FEED_ID_START, final int FEED_ID_LIMIT) {
        LOGGER.trace(">getTransferVolumeByHourFromAdaptiveMinEtag304Time");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetTransferVolumeByHourFromAdaptiveMinEtag304Time.setInt(1, FEED_ID_START);
            psGetTransferVolumeByHourFromAdaptiveMinEtag304Time.setInt(2, FEED_ID_LIMIT);            
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetTransferVolumeByHourFromAdaptiveMinEtag304Time);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setSupportsETag(resultSet.getBoolean(7));
                feedPoll.setSupportsConditionalGet(resultSet.getBoolean(8));
                feedPoll.setETagResponseSize(resultSet.getInt(9));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(10));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error("getTransferVolumeByHourFromAdaptiveMinEtag304Time", e);
        }
        LOGGER.trace("<getTransferVolumeByHourFromAdaptiveMinEtag304Time");
        return result;
    }
      
    
    
}
