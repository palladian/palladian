package tud.iir.news.evaluation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.news.evaluation.ChartCreator.Policy;
import tud.iir.news.evaluation.ChartCreator.PollingStrategy;
import tud.iir.persistence.DatabaseManager;

/**
 * A database for feed reading evaluation.
 * 
 * @author Sandro Reichert
 */
public final class EvaluationDatabase {

    /** the instance of this class */
    private final static EvaluationDatabase INSTANCE = new EvaluationDatabase();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EvaluationDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private PreparedStatement psGetPollsFromAdaptiveMaxTime;
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
    
    private PreparedStatement psGetTransferVolumeByHourFromAdaptiveMaxTime;
    private PreparedStatement psGetTransferVolumeByHourFromFix1440MaxMinTime;
    private PreparedStatement psGetTransferVolumeByHourFromFix60MaxMinTime;
    private PreparedStatement psGetTransferVolumeByHourFromFixLearnedMaxTime;
    private PreparedStatement psGetTransferVolumeByHourFromProbabilisticMaxTime;
    
    private PreparedStatement psGetTransferVolumeByHourFromAdaptiveMinTime;
    private PreparedStatement psGetTransferVolumeByHourFromFixLearnedMinTime;
    private PreparedStatement psGetTransferVolumeByHourFromProbabilisticMinTime;

    /**
     * Private Constructor that prepares all prepared statements by calling
     * {@link EvaluationDatabase#prepareStatements()}.
     */
    private EvaluationDatabase() {
        try {
            prepareStatements();
        } catch (SQLException e) {
            LOGGER.error("SQLException ", e);
        }
    }

    /**
     * @return The single instance of the EvaluationDatabase.
     */
    public static EvaluationDatabase getInstance() {
        return INSTANCE;
    }

    /**
     * Prepare all statements which are hard coded within this method.
     * 
     * @throws SQLException forwarded from {@link java.sql.Connection.prepareStatement(String arg0)}
     */
    private void prepareStatements() throws SQLException {
        Connection connection = DatabaseManager.getInstance().getConnection();
        
        psGetFeedSizeDistribution = connection
                .prepareStatement("SELECT feedID, activityPattern, sizeOfPoll FROM feed_evaluation2_fix1440_max_min_poll WHERE numberOfPoll = 1 AND pollTimestamp <= ? ");
        // only for testing purpose
        psGetPollsFromAdaptiveMaxTime = connection
                .prepareStatement("SELECT feedID, numberOfPoll, activityPattern, conditionalGetResponseSize, sizeOfPoll, pollTimestamp, checkInterval, newWindowItems, missedItems, windowSize, culmulatedDelay, culmulatedLateDelay, timeliness, timelinessLate FROM feed_evaluation2_adaptive_max_time");
        psGetAverageUpdateIntervals = connection
                .prepareStatement("SELECT feedID, activityPattern, averageUpdateInterval FROM feed_evaluation2_update_intervals");

        // for timeliness2 (ScoreMin vs. Polls)
        psGetAvgScoreMinByPollFromAdaptivePoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_adaptive_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ?  AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFixLearnedPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix_learned_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix1440Poll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix1440_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromFix60Poll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix60_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgScoreMinByPollFromPorbabilisticPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_probabilistic_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        
        // get average percentages of new entries by poll for MAX-policy
        psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_adaptive_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix_learned_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix1440_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll = connection
                .prepareStatement("SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_probabilistic_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll");
        
        // 6521 = Anzahl Stunden seit 01.01.2010 00:00 bis zum Start des Experiments am 28.09.2010 16:00
        // Wert wird verwendet, damit das Experiment von Stunde 1 bis 672 (4 Wochen) läuft
        psGetTransferVolumeByHourFromAdaptiveMaxTime  = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFix1440MaxMinTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix1440_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFix60MaxMinTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix60_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFixLearnedMaxTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromProbabilisticMaxTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        
        psGetTransferVolumeByHourFromAdaptiveMinTime  = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromFixLearnedMinTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
        psGetTransferVolumeByHourFromProbabilisticMinTime = connection
                .prepareStatement("SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC");
    }

    /**
     * Was just a simple test whether DB Connection works properly.
     * 
     * @return all poll from table adaptiveMaxTime
     */
    public List<EvaluationFeedPoll> getAllFeedPollsFromAdaptiveMaxTime() {
        LOGGER.trace(">getFeedPolls");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(psGetPollsFromAdaptiveMaxTime);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setNumberOfPoll(resultSet.getInt(2));
                feedPoll.setActivityPattern(resultSet.getInt(3));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(4));
                feedPoll.setSizeOfPoll(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setCheckInterval(resultSet.getInt(7));
                feedPoll.setNewWindowItems(resultSet.getInt(8));
                feedPoll.setMissedItems(resultSet.getInt(9));
                feedPoll.setWindowSize(resultSet.getInt(10));
                feedPoll.setCumulatedDelay(resultSet.getDouble(11));
                feedPoll.setCumulatedLateDelay(resultSet.getDouble(12));
                feedPoll.setTimeliness(resultSet.getDouble(13));
                feedPoll.setTimelinessLate(resultSet.getDouble(14));
                result.add(feedPoll);
            }
            resultSet.close();
        } catch (SQLException e) {
            LOGGER.error("getFeedPolls", e);
        }
        LOGGER.trace("<getFeedPolls " + result.size());
        return result;
    }

    /**
     * returns the result of "SELECT id, updateClass, sizeOfPoll FROM feed_evaluation2_fix1440_max_min_poll WHERE
     * numberOfPoll = 1 AND sizeOfPoll > 0 AND pollTimestamp <= {@link EvaluationDatabase#FeedReaderEvaluator /1000L}"
     * 
     * @return List contains for every FeedID one {@link EvaluationFeedPoll} object with the size of the first poll and
     *         its activity pattern.
     */
    public List<EvaluationFeedPoll> getFeedSizes() {
        LOGGER.trace(">getFeedSizes");
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            psGetFeedSizeDistribution.setLong(1, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
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

    /**
     * returns the result of "SELECT id, updateClass, averageUpdateInterval FROM feed_evaluation2_update_intervals"
     * 
     * @return List contains for every FeedID one {@link EvaluationItemIntervalItem} object with the activity pattern
     *         and the feed's average update interval.
     */
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
     * @return a result set containing the average value (specified by the PreparedStatement {@link ps) for each
     *         numberOfPoll.
     */
    private List<EvaluationFeedPoll> getAverageValueByPoll(final int MAX_NUMBER_OF_POLLS, final PreparedStatement ps) {
        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps.toString());
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ps.setLong(1, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
            ps.setInt(2, MAX_NUMBER_OF_POLLS);
            ps.setInt(3, MAX_NUMBER_OF_POLLS);
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
     * Queries the database to get the average percentage of new items per numberOfPoll for the given
     * {@link PollingStrategy}, MAX-policy and returns it as a result set. See
     * {@link EvaluationDatabase#psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll} for details on queries
     * 
     * @param STRATEGY The {@link PollingStrategy} to get data for.
     * @param MAX_NUMBER_OF_POLLS The number of polls to return. It is also used to make sure that every returned FeedID
     *            has that many polls.
     * @return a result set containing the average percentage of new items (averaged over all feeds that have at least
     *         {@link MAX_NUMBER_OF_POLLS} polls) for each numberOfPoll <= {@link MAX_NUMBER_OF_POLLS}.
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesPerPollFromMaxPoll(final PollingStrategy STRATEGY,
            final int MAX_NUMBER_OF_POLLS) {

        PreparedStatement ps = null;
        switch (STRATEGY) {
            case ADAPTIVE:
                ps = psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll;
                break;
            case PROBABILISTIC:
                ps = psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll;
                break;
            case FIX_LEARNED:
                ps = psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll;
                break;
            case FIX60:
                ps = psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll;
                break;
            case FIX1440:
                ps = psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll;
                break;
            default:
                throw new IllegalStateException("unknown Algorithm: " + STRATEGY.toString());
        }
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, ps);
    }


    /**
     * Queries the database to get the average scoreMin per numberOfPoll for the given {@link PollingStrategy},
     * MIN-policy and returns a result set containing the average scoreMin for each numberOfPoll. See
     * {@link EvaluationDatabase#psGetAvgScoreMinByPollFromAdaptivePoll} for details on query
     * 
     * @param STRATEGY The {@link PollingStrategy} to get data for.
     * @param MAX_NUMBER_OF_POLLS The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin (averaged over all feeds that have at least
     *         {@link MAX_NUMBER_OF_POLLS} polls) for each numberOfPoll <= {@link MAX_NUMBER_OF_POLLS}.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinPerPollFromMinPoll(final PollingStrategy STRATEGY,
            final int MAX_NUMBER_OF_POLLS) {

        PreparedStatement ps = null;
        switch (STRATEGY) {
            case ADAPTIVE:
                ps = psGetAvgScoreMinByPollFromAdaptivePoll;
                break;
            case PROBABILISTIC:
                ps = psGetAvgScoreMinByPollFromPorbabilisticPoll;
                break;
            case FIX_LEARNED:
                ps = psGetAvgScoreMinByPollFromFixLearnedPoll;
                break;
            case FIX60:
                ps = psGetAvgScoreMinByPollFromFix60Poll;
                break;
            case FIX1440:
                ps = psGetAvgScoreMinByPollFromFix1440Poll;
                break;
            default:
                throw new IllegalStateException("unknown Algorithm: " + STRATEGY.toString());
        }
        return getAverageValueByPoll(MAX_NUMBER_OF_POLLS, ps);
    }

    /**
     * Queries the database to get all polls for the given {@link STRATEGY} and {@link Policy}. Returned FeedIDs are
     * between {@link FEED_ID_START} and {@link FEED_ID_LIMIT}.
     * 
     * @param STRATEGY The {@link Policy} to get data for.
     * @param STRATEGY The {@link PollingStrategy} to get data for.
     * @param FEED_ID_START FeedIDs to retrieve from database are between these values.
     * @param FEED_ID_LIMIT FeedIDs to retrieve from database are between these values.
     * @return List of {@link EvaluationFeedPoll} objects where each item is one poll of the given {@link STRATEGY} for
     *         all FeedID between {@link FEED_ID_START} and {@link FEED_ID_LIMIT}.
     */
    public List<EvaluationFeedPoll> getTransferVolumeByHourFromTime(final Policy POLICY,
            final PollingStrategy STRATEGY, final int FEED_ID_START, final int FEED_ID_LIMIT) {

        LOGGER.trace(">getTransferVolumeByHour processing Algorithm " + STRATEGY.toString());
        PreparedStatement ps = null;

        switch (POLICY) {
            case MAX:
                switch (STRATEGY) {
                    case ADAPTIVE:
                        ps = psGetTransferVolumeByHourFromAdaptiveMaxTime;
                        break;
                    case PROBABILISTIC:
                        ps = psGetTransferVolumeByHourFromProbabilisticMaxTime;
                        break;
                    case FIX_LEARNED:
                        ps = psGetTransferVolumeByHourFromFixLearnedMaxTime;
                        break;
                    case FIX60:
                        ps = psGetTransferVolumeByHourFromFix60MaxMinTime;
                        break;
                    case FIX1440:
                        ps = psGetTransferVolumeByHourFromFix1440MaxMinTime;
                        break;
                    default:
                        throw new IllegalStateException("unknown Algorithm: " + STRATEGY.toString());
                }
                break;
            case MIN:
                switch (STRATEGY) {
                    case ADAPTIVE:
                        ps = psGetTransferVolumeByHourFromAdaptiveMinTime;
                        break;
                    case PROBABILISTIC:
                        ps = psGetTransferVolumeByHourFromProbabilisticMinTime;
                        break;
                    case FIX_LEARNED:
                        ps = psGetTransferVolumeByHourFromFixLearnedMinTime;
                        break;
                    case FIX60:
                        ps = psGetTransferVolumeByHourFromFix60MaxMinTime;
                        break;
                    case FIX1440:
                        ps = psGetTransferVolumeByHourFromFix1440MaxMinTime;
                        break;
                    default:
                        throw new IllegalStateException("unknown Algorithm: " + STRATEGY.toString());
                }
                break;
            default:
                throw new IllegalStateException("unknown Policy: " + POLICY.toString());
        }

        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ps.setLong(1, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
            ps.setInt(2, FEED_ID_START);
            ps.setInt(3, FEED_ID_LIMIT);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(7));
                feedPoll.setNewWindowItems(resultSet.getInt(8));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error(">getTransferVolumeByHour processing Algorithm " + STRATEGY.toString(), e);
        }
        LOGGER.trace(">getTransferVolumeByHour processing Algorithm " + STRATEGY.toString());
        return result;
    }

    
}
