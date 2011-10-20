package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.util.Date;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.FixUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * Starting Point to evaluate an {@link UpdateStrategy} on the TUDCS6 dataset. This class has similar functionalities
 * to {@link FeedReaderEvaluator}, both will be merged soon.
 * 
 * @author Sandro Reichert
 */
public class DatasetEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /**
     * The feed checker.
     */
    private final FeedReader feedReader;

    /**
     * The name of the database table to write evaluation results to.
     */
    private static String currentDbTable;

    public DatasetEvaluator() {
        final FeedStore feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder
                .getInstance().getConfig());
        // important: reseting the table has to be done >before< creating the FeedReader since the FeedReader reads the
        // table when creating the FeedReader. Any subsequent changes are ignored...
        ((EvaluationFeedDatabase) feedStore).resetTableFeeds();
        feedReader = new FeedReader(feedStore);
    }

    /**
     * @return The name of the database table to write evaluation results to.
     */
    public static String getEvaluationDbTableName() {
        return currentDbTable;
    }

    /**
     * Run evaluation of the given strategy on dataset TUDCS6.
     */
    public void runEvaluation() {
        LOGGER.debug("start reading feeds");
        feedReader.startContinuousReading();
    }

    /**
     * Does the initialization:
     * <ul>
     * <li>
     * Initializes all {@link Feed}s so that their lastPollTime is equal to
     * {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} and updateInterval is 0 and update feeds in
     * database.</li>
     * <li>
     * Set benchmark policy.</li>
     * <li>
     * Set {@link UpdateStrategy}.</li>
     * <li>
     * Set (database) table name to store evaluation information into.</li>
     * <li>
     * Creates this table.</li>
     * </ul>
     */
    private void initialize(int benchmarkPolicy, int benchmarkMode, int benchmarkSampleSize,
            UpdateStrategy updateStrategy, long wakeUpInterval) {
        for (Feed feed : feedReader.getFeeds()) {
            feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND));
            feed.setUpdateInterval(0);
            feedReader.updateFeed(feed);
        }
        FeedReaderEvaluator.setBenchmarkPolicy(benchmarkPolicy);
        FeedReaderEvaluator.setBenchmarkMode(benchmarkMode);
        FeedReaderEvaluator.benchmarkSamplePercentage = benchmarkSampleSize;
        feedReader.setUpdateStrategy(updateStrategy, true);
        feedReader.setWakeUpInterval(wakeUpInterval);

        // TODO do we need a processing action???
        // FeedProcessingAction fpa = new DatasetProcessingAction(feedStore);
        // feedChecker.setFeedProcessingAction(fpa);

        currentDbTable = "feed_evaluation_" + feedReader.getUpdateStrategyName() + "_"
                + FeedReaderEvaluator.getBenchmarkName() + "_" + FeedReaderEvaluator.getBenchmarkModeString() + "_"
                + FeedReaderEvaluator.benchmarkSamplePercentage + "_" + DateHelper.getCurrentDatetime();

        boolean created = ((EvaluationFeedDatabase) feedReader.getFeedStore())
                .createEvaluationDbTable(getEvaluationDbTableName());
        if (!created) {
            LOGGER.fatal("Database table " + getEvaluationDbTableName()
                    + " could not be created. Evaluation is impossible. Processing aborted.");
            System.exit(-1);
        }
    }

    /**
     * Start evaluation of an {@link UpdateStrategy}.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // long a = 1510L;
        // long b = 8L;
        // long c = a - b;
        // long d = Math.round((double) (a - b) / 1000);
        // System.out.println(c);
        // System.out.println(d);
        // System.exit(0);

        // TODO: get Strategy and parameters from command line args
        // UpdateStrategy updateStrategy = new MavStrategyDatasetCreation();
        // updateStrategy.setHighestUpdateInterval(360); // 6hrs
        // updateStrategy.setLowestUpdateInterval(0);
        int benchmarkPolicy = FeedReaderEvaluator.BENCHMARK_MIN_DELAY;
        int benchmarkMode = FeedReaderEvaluator.BENCHMARK_TIME;
        int benchmarkSampleSize = 100;

        UpdateStrategy updateStrategy = new FixUpdateStrategy();
        ((FixUpdateStrategy) updateStrategy).setCheckInterval(360); // required by Fix strategies only!

        long wakeUpInterval = (long) (1 * DateHelper.SECOND_MS);
        DatasetEvaluator evaluator = new DatasetEvaluator();
        evaluator.initialize(benchmarkPolicy, benchmarkMode, benchmarkSampleSize, updateStrategy, wakeUpInterval);
        evaluator.runEvaluation();
    }

}
