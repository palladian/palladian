package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.util.Date;

import org.apache.commons.configuration.PropertiesConfiguration;
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
 * <p>
 * The evaluation is required be configured using palladian.properties:
 * <ul>
 * <li>
 * datasetEvaluator.updateStrategy = Fix</li>
 * <li>
 * datasetEvaluator.fixCheckInterval = 60</li>
 * <li>
 * datasetEvaluator.minCheckInterval = 1</li>
 * <li>
 * datasetEvaluator.maxCheckInterval = 1440</li>
 * <li>
 * datasetEvaluator.benchmarkMode = time</li>
 * <li>
 * feedReader.threadPoolSize = 250</li>
 * </ul>
 * </p>
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
     * The name of the database table to write simulated poll data to.
     */
    private static String simulatedPollsDbTable;

    public DatasetEvaluator() {
        final FeedStore feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder
                .getInstance().getConfig());
        // important: reseting the table has to be done >before< creating the FeedReader since the FeedReader reads the
        // table when creating the FeedReader. Any subsequent changes are ignored...
        ((EvaluationFeedDatabase) feedStore).resetTableFeeds();
        feedReader = new FeedReader(feedStore);
    }

    /**
     * @return The name of the database table to write simulated poll data to.
     */
    public static String simulatedPollsDbTableName() {
        return simulatedPollsDbTable;
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
            // feedReader.updateFeed(feed, false, false);
        }
        FeedReaderEvaluator.setBenchmarkPolicy(benchmarkPolicy);
        FeedReaderEvaluator.setBenchmarkMode(benchmarkMode);
        FeedReaderEvaluator.benchmarkSamplePercentage = benchmarkSampleSize;
        feedReader.setUpdateStrategy(updateStrategy, true);
        feedReader.setWakeUpInterval(wakeUpInterval);

        simulatedPollsDbTable = "eval_" + feedReader.getUpdateStrategyName() + "_"
                + FeedReaderEvaluator.getBenchmarkName() + "_" + FeedReaderEvaluator.getBenchmarkModeString() + "_"
                + FeedReaderEvaluator.benchmarkSamplePercentage + "_" + DateHelper.getCurrentDatetime();

        boolean created = ((EvaluationFeedDatabase) feedReader.getFeedStore())
                .createEvaluationDbTable(simulatedPollsDbTableName());
        if (!created) {
            LOGGER.fatal("Database table " + simulatedPollsDbTableName()
                    + " could not be created. Evaluation is impossible. Processing aborted.");
            System.exit(-1);
        }
    }

    /**
     * Get evaluation results from {@link #simulatedPollsDbTable} and write to two tables with the same name plus
     * postfix "_feeds" or "_items" for these two different averaging modes.
     */
    private void writeResultsToDB() {
        String modeFeedsName = simulatedPollsDbTableName() + "_feeds";
        boolean tableCreated = ((EvaluationFeedDatabase) feedReader.getFeedStore())
                .createEvaluationResultsPerStrategyTable(modeFeedsName, true);
        boolean dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore())
                .generateBasicEvaluationResultsPerStrategy(simulatedPollsDbTableName(), modeFeedsName, true);
        dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore()).setAvgDelay(simulatedPollsDbTableName(),
                modeFeedsName, true);
        dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore()).setPPI(simulatedPollsDbTableName(),
                modeFeedsName, true);

        modeFeedsName = simulatedPollsDbTableName() + "_items";
        tableCreated = ((EvaluationFeedDatabase) feedReader.getFeedStore()).createEvaluationResultsPerStrategyTable(
                modeFeedsName, false);
        dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore()).generateBasicEvaluationResultsPerStrategy(
                simulatedPollsDbTableName(), modeFeedsName, false);
        dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore()).setAvgDelay(simulatedPollsDbTableName(),
                modeFeedsName, false);
        dataWritten = ((EvaluationFeedDatabase) feedReader.getFeedStore()).setPPI(simulatedPollsDbTableName(),
                modeFeedsName, false);

        // boolean createdItems = ((EvaluationFeedDatabase) feedReader.getFeedStore())
        // .generateBasicEvaluationResultsPerStrategy(simulatedPollsDbTableName(), false);

        if (dataWritten) {
            LOGGER.info("Evaluation results for averaging mode feeds have been written to database.");
        } else {
            LOGGER.fatal("Evaluation results for averaging mode feeds have NOT been written to database.");
        }
        // if (createdItems) {
        // LOGGER.info("Evaluation results for averaging mode items have been written to database.");
        // } else {
        // LOGGER.fatal("Evaluation results for averaging mode items have NOT been written to database.");
        // }

    }

    /**
     * Start evaluation of an {@link UpdateStrategy}.
     * 
     * @param args
     */
    public static void main(String[] args) {

        // load configuration from palladian.properies
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        UpdateStrategy updateStrategy = null;
        int benchmarkMode = -1;
        boolean fatalErrorOccurred = false;
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("Initialize DatasetEvaluator. Evaluating strategy ");
        
        try {
            // read update strategy and interval in case of "Fix"
            String strategy = config.getString("datasetEvaluator.updateStrategy");
            // Fix
            if (strategy.equalsIgnoreCase("Fix")) {
                updateStrategy = new FixUpdateStrategy();
                int fixInterval = config.getInt("datasetEvaluator.fixCheckInterval");
                ((FixUpdateStrategy) updateStrategy).setCheckInterval(fixInterval);
                logMsg.append("Fix");
                logMsg.append(fixInterval);
                logMsg.append(" ");
            }
            // Fix Learned
            else if (strategy.equalsIgnoreCase("FixLearned")) {
                updateStrategy = new FixUpdateStrategy();
                ((FixUpdateStrategy) updateStrategy).setCheckInterval(-1);
                logMsg.append("Fix Learned");
            }
            // Unknown strategy
            else {
                fatalErrorOccurred = true;
                LOGGER.fatal("Cant read updateStrategy from config.");
            }


            // read interval bounds
            int minInterval = config.getInt("datasetEvaluator.minCheckInterval");
            int maxInterval = config.getInt("datasetEvaluator.maxCheckInterval");

            // validate interval bounds
            if (minInterval >= maxInterval || minInterval < 1 || maxInterval < 1) {
                fatalErrorOccurred = true;
                LOGGER.fatal("Please set interval bounds bounds properly.");
            }
            // set interval bounds
            else {
            updateStrategy.setLowestUpdateInterval(minInterval);
            updateStrategy.setHighestUpdateInterval(maxInterval);
                logMsg.append(",minCheckInterval = ");
                logMsg.append(minInterval);
                logMsg.append(", maxCheckInterval = ");
                logMsg.append(maxInterval);
            }

            // read and set benchmark mode
            String mode = config.getString("datasetEvaluator.benchmarkMode");
            if (mode.equalsIgnoreCase("time")) {
                benchmarkMode = FeedReaderEvaluator.BENCHMARK_TIME;
            } else if (mode.equalsIgnoreCase("poll")) {
                benchmarkMode = FeedReaderEvaluator.BENCHMARK_POLL;
            } else {
                fatalErrorOccurred = true;
                LOGGER.fatal("Cant read benchmarkMode from config.");
            }
            logMsg.append(", benchmarkMode = ");
            logMsg.append(mode);
            
        } catch (Exception e) {
            fatalErrorOccurred = true;
            LOGGER.fatal("Could not load DatasetEvaluator configuration: " + e.getLocalizedMessage());
        }

        if (!fatalErrorOccurred) {

            LOGGER.info(logMsg.toString());

            // set some defaults not provided by config file
            int benchmarkPolicy = FeedReaderEvaluator.BENCHMARK_MIN_DELAY;
            int benchmarkSampleSize = 100;
            // FeedReader wakeupInterval, used for debugging
            long wakeUpInterval = (long) (60 * DateHelper.SECOND_MS);


            DatasetEvaluator evaluator = new DatasetEvaluator();
            evaluator.initialize(benchmarkPolicy, benchmarkMode, benchmarkSampleSize, updateStrategy, wakeUpInterval);
            evaluator.runEvaluation();
            evaluator.writeResultsToDB();
            // this is a normal exit
            System.exit(0);
        } else {

            LOGGER.fatal("Exiting.");
        }
    }

}
