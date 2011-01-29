package tud.iir.web.feeds.evaluation;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import tud.iir.persistence.DatabaseManager;
import tud.iir.persistence.ResultCallback;
import tud.iir.web.feeds.evaluation.ChartCreator.Policy;

/**
 * A database for feed reading evaluation.
 * 
 * @author Sandro Reichert
 */
public final class EvaluationDatabase extends DatabaseManager {

    /** the instance of this class */
    private final static EvaluationDatabase INSTANCE = new EvaluationDatabase();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EvaluationDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private final String psGetPollsFromAdaptiveMaxTime = "SELECT feedID, numberOfPoll, activityPattern, conditionalGetResponseSize, sizeOfPoll, pollTimestamp, checkInterval, newWindowItems, missedItems, windowSize, culmulatedDelay, culmulatedLateDelay, timeliness, timelinessLate FROM feed_evaluation2_adaptive_max_time";
    private final String psGetFeedSizeDistribution = "SELECT feedID, activityPattern, sizeOfPoll FROM feed_evaluation2_fix1440_max_min_poll WHERE numberOfPoll = 1 AND pollTimestamp <= ?";
    private final String psGetAverageUpdateIntervals = "SELECT feedID, activityPattern, averageUpdateInterval FROM feed_evaluation2_update_intervals";

    private final String psGetAvgScoreMinByPollFromAdaptivePoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_adaptive_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ?  AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgScoreMinByPollFromFixLearnedPoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix_learned_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgScoreMinByPollFromFix1440Poll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix1440_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgScoreMinByPollFromFix60Poll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix60_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgScoreMinByPollFromPorbabilisticPoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_probabilistic_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";

    private final String psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_adaptive_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix_learned_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix1440_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private final String psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_probabilistic_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";

    private final String psGetTransferVolumeByHourFromAdaptiveMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromFix1440MaxMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix1440_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromFix60MaxMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix60_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromFixLearnedMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromProbabilisticMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";

    private final String psGetTransferVolumeByHourFromAdaptiveMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromFixLearnedMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private final String psGetTransferVolumeByHourFromProbabilisticMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";

    /**
     * Private Constructor that prepares all prepared statements by calling
     * {@link EvaluationDatabase#prepareStatements()}.
     */
    public EvaluationDatabase() {

    }


    /**
     * Was just a simple test whether DB Connection works properly.
     * 
     * @return all poll from table adaptiveMaxTime
     */
    public List<EvaluationFeedPoll> getAllFeedPollsFromAdaptiveMaxTime() {
        LOGGER.trace(">getFeedPolls");
        final List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();

        DatabaseManager dbm = new DatabaseManager();

        ResultCallback<Map<String, Object>> callback = new ResultCallback<Map<String, Object>>() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID((Integer) object.get("feedID"));
                feedPoll.setNumberOfPoll((Integer) object.get("numberOfPoll"));
                feedPoll.setActivityPattern((Integer) object.get("activityPattern"));
                feedPoll.setConditionalGetResponseSize((Integer) object.get("conditionalGetResponseSize"));
                feedPoll.setSizeOfPoll((Integer) object.get("sizeOfPoll"));
                feedPoll.setPollTimestamp((Long) object.get("pollTimestamp"));
                feedPoll.setCheckInterval((Integer) object.get("checkInterval"));
                feedPoll.setNewWindowItems((Integer) object.get("newWindowItems"));
                feedPoll.setMissedItems((Integer) object.get("missedItems"));
                feedPoll.setWindowSize((Integer) object.get("windowSize"));
                feedPoll.setCumulatedDelay((Double) object.get("culmulatedDelay"));
                feedPoll.setCumulatedLateDelay((Double) object.get("culmulatedLateDelay"));
                feedPoll.setTimeliness((Double) object.get("timeliness"));
                feedPoll.setTimelinessLate((Double) object.get("timelinessLate"));
                result.add(feedPoll);
            }

        };

        dbm.runQuery(callback, psGetPollsFromAdaptiveMaxTime);


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
        final List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();

        DatabaseManager dbm = new DatabaseManager();

        ResultCallback<Map<String, Object>> callback = new ResultCallback<Map<String, Object>>() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID((Integer) object.get("feedID"));
                feedPoll.setActivityPattern((Integer) object.get("activityPattern"));
                feedPoll.setSizeOfPoll((Integer) object.get("sizeOfPoll"));
                result.add(feedPoll);
            }
        };

        dbm.runQuery(callback, psGetFeedSizeDistribution, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);

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
     * @param maxNumberOfPolls The highest numberOfPolls value to calculate the average value for.
     * @param ps The PreparedStatement to process.
     * @return a result set containing the average value (specified by the PreparedStatement {@link ps) for each
     *         numberOfPoll.
     */
    private List<EvaluationFeedPoll> getAverageValueByPoll(final int maxNumberOfPolls, final String ps) {
        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps.toString());
        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ps.setLong(1, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
            ps.setInt(2, maxNumberOfPolls);
            ps.setInt(3, maxNumberOfPolls);
            ResultSet resultSet = DatabaseManager.getInstance().runQuery(ps);
            while (resultSet.next()) {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setAverageValue(resultSet.getDouble(2));
                result.add(feedPoll);
            }
        } catch (SQLException e) {
            LOGGER.error(">getAverageValueByPoll processing PreparedStatement " + ps, e);
        }
        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps);
        return result;
    }


    /**
     * Queries the database to get the average percentage of new items per numberOfPoll for the given
     * {@link PollingStrategy}, MAX-policy and returns it as a result set. See
     * {@link EvaluationDatabase#psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll} for details on queries
     * 
     * @param strategy The {@link PollingStrategy} to get data for.
     * @param maxNumberOfPolls The number of polls to return. It is also used to make sure that every returned FeedID
     *            has that many polls.
     * @return a result set containing the average percentage of new items (averaged over all feeds that have at least
     *         {@link MAX_NUMBER_OF_POLLS} polls) for each numberOfPoll <= {@link MAX_NUMBER_OF_POLLS}.
     */
    public List<EvaluationFeedPoll> getAveragePercentageNewEntriesPerPollFromMaxPoll(final PollingStrategy strategy,
            final int maxNumberOfPolls) {

        String ps = "";
        switch (strategy) {
            case MOVING_AVERAGE:
                ps = psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll;
                break;
            case POST_RATE:
                ps = psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll;
                break;
            case FIX_LEARNED:
                ps = psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll;
                break;
            case FIX_1h:
                ps = psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll;
                break;
            case FIX_1d:
                ps = psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll;
                break;
            default:
                throw new IllegalStateException("unknown Algorithm: " + strategy.toString());
        }
        return getAverageValueByPoll(maxNumberOfPolls, ps);
    }


    /**
     * Queries the database to get the average scoreMin per numberOfPoll for the given {@link PollingStrategy},
     * MIN-policy and returns a result set containing the average scoreMin for each numberOfPoll. See
     * {@link EvaluationDatabase#psGetAvgScoreMinByPollFromAdaptivePoll} for details on query
     * 
     * @param strategy The {@link PollingStrategy} to get data for.
     * @param maxNumberOfPolls The highest numberOfPolls value to calculate the average scoreMin.
     * @return a result set containing the average scoreMin (averaged over all feeds that have at least
     *         {@link MAX_NUMBER_OF_POLLS} polls) for each numberOfPoll <= {@link MAX_NUMBER_OF_POLLS}.
     */
    public List<EvaluationFeedPoll> getAverageScoreMinPerPollFromMinPoll(final PollingStrategy strategy,
            final int maxNumberOfPolls) {

        String ps = "";
        switch (strategy) {
            case MOVING_AVERAGE:
                ps = psGetAvgScoreMinByPollFromAdaptivePoll;
                break;
            case POST_RATE:
                ps = psGetAvgScoreMinByPollFromPorbabilisticPoll;
                break;
            case FIX_LEARNED:
                ps = psGetAvgScoreMinByPollFromFixLearnedPoll;
                break;
            case FIX_1h:
                ps = psGetAvgScoreMinByPollFromFix60Poll;
                break;
            case FIX_1d:
                ps = psGetAvgScoreMinByPollFromFix1440Poll;
                break;
            default:
                throw new IllegalStateException("unknown Algorithm: " + strategy.toString());
        }
        return getAverageValueByPoll(maxNumberOfPolls, ps);
    }

    /**
     * Queries the database to get all polls for the given {@link PollingStrategy} and {@link Policy}. Returned FeedIDs
     * are between {@link FEED_ID_START} and {@link FEED_ID_LIMIT}.
     * 
     * @param policy The {@link Policy} to get data for.
     * @param strategy The {@link PollingStrategy} to get data for.
     * @param feedIDStart FeedIDs to retrieve from database are between these values.
     * @param feedIDLimit FeedIDs to retrieve from database are between these values.
     * @return List of {@link EvaluationFeedPoll} objects where each item is one poll of the given
     *         {@link PollingStrategy} for all FeedID between {@link feedIDStart} and {@link feedIDLimit}.
     */
    public List<EvaluationFeedPoll> getTransferVolumeByHourFromTime(final Policy policy,
            final PollingStrategy strategy, final int feedIDStart, final int feedIDLimit) {

        LOGGER.trace(">getTransferVolumeByHour processing Algorithm " + strategy.toString());
        String ps = "";

        switch (policy) {
            case MAX:
                switch (strategy) {
                    case MOVING_AVERAGE:
                        ps = psGetTransferVolumeByHourFromAdaptiveMaxTime;
                        break;
                    case POST_RATE:
                        ps = psGetTransferVolumeByHourFromProbabilisticMaxTime;
                        break;
                    case FIX_LEARNED:
                        ps = psGetTransferVolumeByHourFromFixLearnedMaxTime;
                        break;
                    case FIX_1h:
                        ps = psGetTransferVolumeByHourFromFix60MaxMinTime;
                        break;
                    case FIX_1d:
                        ps = psGetTransferVolumeByHourFromFix1440MaxMinTime;
                        break;
                    default:
                        throw new IllegalStateException("unknown Algorithm: " + strategy.toString());
                }
                break;
            case MIN:
                switch (strategy) {
                    case MOVING_AVERAGE:
                        ps = psGetTransferVolumeByHourFromAdaptiveMinTime;
                        break;
                    case POST_RATE:
                        ps = psGetTransferVolumeByHourFromProbabilisticMinTime;
                        break;
                    case FIX_LEARNED:
                        ps = psGetTransferVolumeByHourFromFixLearnedMinTime;
                        break;
                    case FIX_1h:
                        ps = psGetTransferVolumeByHourFromFix60MaxMinTime;
                        break;
                    case FIX_1d:
                        ps = psGetTransferVolumeByHourFromFix1440MaxMinTime;
                        break;
                    default:
                        throw new IllegalStateException("unknown Algorithm: " + strategy.toString());
                }
                break;
            default:
                throw new IllegalStateException("unknown Policy: " + policy.toString());
        }

        List<EvaluationFeedPoll> result = new LinkedList<EvaluationFeedPoll>();
        try {
            ps.setLong(1, FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
            ps.setInt(2, feedIDStart);
            ps.setInt(3, feedIDLimit);
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
            LOGGER.error(">getTransferVolumeByHour processing Algorithm " + strategy.toString(), e);
        }
        LOGGER.trace(">getTransferVolumeByHour processing Algorithm " + strategy.toString());
        return result;
    }


}
