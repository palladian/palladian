package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.evaluation.ChartCreator.Policy;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

/**
 * A database for feed reading evaluation of first feed paper.
 * 
 * @author Sandro Reichert
 */
public final class EvaluationDatabase extends DatabaseManager {

    /** the logger for this class */
    private static final Logger LOGGER = LoggerFactory.getLogger(EvaluationDatabase.class);

    // ////////////////// feed prepared statements ////////////////////
    private static final String psGetPollsFromAdaptiveMaxTime = "SELECT feedID, numberOfPoll, activityPattern, conditionalGetResponseSize, sizeOfPoll, pollTimestamp, checkInterval, newWindowItems, missedItems, windowSize, culmulatedDelay, culmulatedLateDelay, timeliness, timelinessLate FROM feed_evaluation2_adaptive_max_time";

    private static final String psGetFeedSizeDistribution = "SELECT feedID, activityPattern, sizeOfPoll FROM feed_evaluation2_fix1440_max_min_poll WHERE numberOfPoll = 1 AND pollTimestamp <= ?";
    private static final String psGetAverageUpdateIntervals = "SELECT feedID, activityPattern, averageUpdateInterval FROM feed_evaluation2_update_intervals";
    // for timeliness2 (ScoreMin vs. Polls)
    private static final String psGetAvgScoreMinByPollFromAdaptivePoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_adaptive_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ?  AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";

    private static final String psGetAvgScoreMinByPollFromFixLearnedPoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix_learned_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgScoreMinByPollFromFix1440Poll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix1440_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgScoreMinByPollFromFix60Poll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_fix60_max_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgScoreMinByPollFromPorbabilisticPoll = "SELECT numberOfPoll, AVG(timeliness) FROM feed_evaluation2_probabilistic_min_poll WHERE timeliness > 0 AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_min_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    // get average percentages of new entries by poll for MAX-policy
    private static final String psGetAvgPercentageNewEntriesByPollFromAdaptiveMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_adaptive_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";

    private static final String psGetAvgPercentageNewEntriesByPollFromFixLearnedMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix_learned_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgPercentageNewEntriesByPollFromFix1440MaxMinPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix1440_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgPercentageNewEntriesByPollFromFix60MaxMinPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_fix60_max_min_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    private static final String psGetAvgPercentageNewEntriesByPollFromPorbabilisticMaxPoll = "SELECT numberOfPoll, AVG(newWindowItems/windowSize) FROM feed_evaluation2_probabilistic_max_poll WHERE newWindowItems IS NOT NULL AND pollTimestamp <= ? AND numberOfPoll <= ? AND feedID IN (SELECT DISTINCT feedID FROM feed_evaluation2_adaptive_max_poll WHERE numberOfPoll = ?) GROUP BY numberOfPoll";
    // 6521 = Anzahl Stunden seit 01.01.2010 00:00 bis zum Start des Experiments am 28.09.2010 16:00
    // Wert wird verwendet, damit das Experiment von Stunde 1 bis 672 (4 Wochen) läuft
    private static final String psGetTransferVolumeByHourFromAdaptiveMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";

    private static final String psGetTransferVolumeByHourFromFix1440MaxMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix1440_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private static final String psGetTransferVolumeByHourFromFix60MaxMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix60_max_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private static final String psGetTransferVolumeByHourFromFixLearnedMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private static final String psGetTransferVolumeByHourFromProbabilisticMaxTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_max_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private static final String psGetTransferVolumeByHourFromAdaptiveMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_adaptive_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";

    private static final String psGetTransferVolumeByHourFromFixLearnedMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_fix_learned_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";
    private static final String psGetTransferVolumeByHourFromProbabilisticMinTime = "SELECT feedID, DAYOFYEAR(FROM_UNIXTIME(pollTimestamp))*24+HOUR(FROM_UNIXTIME(pollTimestamp))-6521 AS hourOfExperiment, sizeOfPoll, numberOfPoll, checkInterval, pollTimestamp, conditionalGetResponseSize, newWindowItems FROM feed_evaluation2_probabilistic_min_time WHERE pollTimestamp <= ? AND feedID BETWEEN ? AND ? ORDER BY feedID, pollTimestamp ASC";

    /**
     * @param dataSource
     */
    protected EvaluationDatabase(DataSource dataSource) {
        super(dataSource);
    }

    /**
     * Was just a simple test whether DB Connection works properly.
     * 
     * @return all poll from table adaptiveMaxTime
     */
    public List<EvaluationFeedPoll> getAllFeedPollsFromAdaptiveMaxTime() {
        LOGGER.trace(">getFeedPolls");

        RowConverter<EvaluationFeedPoll> converter = new RowConverter<EvaluationFeedPoll>() {

            @Override
            public EvaluationFeedPoll convert(ResultSet resultSet) throws SQLException {
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
                return feedPoll;
            }
        };
        List<EvaluationFeedPoll> result = runQuery(converter, psGetPollsFromAdaptiveMaxTime);

        LOGGER.trace("<getFeedPolls " + result.size());
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
     * returns the result of "SELECT id, updateClass, averageUpdateInterval FROM feed_evaluation2_update_intervals"
     * 
     * @return List contains for every FeedID one {@link EvaluationItemIntervalItem} object with the activity pattern
     *         and the feed's average update interval.
     */
    public List<EvaluationItemIntervalItem> getAverageUpdateIntervals() {
        LOGGER.trace(">getAverageUpdateIntervals");

        RowConverter<EvaluationItemIntervalItem> converter = new RowConverter<EvaluationItemIntervalItem>() {

            @Override
            public EvaluationItemIntervalItem convert(ResultSet resultSet) throws SQLException {
                EvaluationItemIntervalItem itemIntervalItem = new EvaluationItemIntervalItem();
                itemIntervalItem.setFeedID(resultSet.getInt(1));
                itemIntervalItem.setActivityPattern(resultSet.getInt(2));
                itemIntervalItem.setAverageUpdateInterval(resultSet.getLong(3));
                return itemIntervalItem;
            }
        };

        List<EvaluationItemIntervalItem> result = runQuery(converter, psGetAverageUpdateIntervals);

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

        RowConverter<EvaluationFeedPoll> converter = new RowConverter<EvaluationFeedPoll>() {

            @Override
            public EvaluationFeedPoll convert(ResultSet resultSet) throws SQLException {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setNumberOfPoll(resultSet.getInt(1));
                feedPoll.setAverageValue(resultSet.getDouble(2));
                return feedPoll;
            }
        };

        List<Object> args = new ArrayList<Object>();
        args.add(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
        args.add(maxNumberOfPolls);
        args.add(maxNumberOfPolls);
        List<EvaluationFeedPoll> result = runQuery(converter, ps, args);

        LOGGER.trace(">getAverageValueByPoll processing PreparedStatement " + ps);
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

        RowConverter<EvaluationFeedPoll> converter = new RowConverter<EvaluationFeedPoll>() {

            @Override
            public EvaluationFeedPoll convert(ResultSet resultSet) throws SQLException {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setActivityPattern(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                return feedPoll;
            }
        };

        final List<EvaluationFeedPoll> result = runQuery(converter, psGetFeedSizeDistribution,
                FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);

        LOGGER.trace("<getFeedSizes");
        return result;
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

        RowConverter<EvaluationFeedPoll> converter = new RowConverter<EvaluationFeedPoll>() {

            @Override
            public EvaluationFeedPoll convert(ResultSet resultSet) throws SQLException {
                EvaluationFeedPoll feedPoll = new EvaluationFeedPoll();
                feedPoll.setFeedID(resultSet.getInt(1));
                feedPoll.setHourOfExperiment(resultSet.getInt(2));
                feedPoll.setSizeOfPoll(resultSet.getInt(3));
                feedPoll.setNumberOfPoll(resultSet.getInt(4));
                feedPoll.setCheckInterval(resultSet.getInt(5));
                feedPoll.setPollTimestamp(resultSet.getLong(6));
                feedPoll.setConditionalGetResponseSize(resultSet.getInt(7));
                feedPoll.setNewWindowItems(resultSet.getInt(8));
                return feedPoll;
            }
        };
        List<Object> args = new ArrayList<Object>();
        args.add(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND / 1000L);
        args.add(feedIDStart);
        args.add(feedIDLimit);
        List<EvaluationFeedPoll> result = runQuery(converter, ps, args);

        LOGGER.trace(">getTransferVolumeByHour processing Algorithm " + strategy.toString());
        return result;
    }

}
