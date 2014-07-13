package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.DefaultFeedProcessingAction;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.FeedReaderSettings;
import ws.palladian.retrieval.feeds.evaluation.ChartCreator;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.updates.AbstractUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.AdaptiveTTLUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.FeedUpdateMode;
import ws.palladian.retrieval.feeds.updates.FixLearnedUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.FixUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.IndHistTTLUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.IndHistUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.LIHZUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.LRU2UpdateStrategy;
import ws.palladian.retrieval.feeds.updates.MAVSynchronizationUpdateStrategy;

/**
 * Starting Point to evaluate an {@link AbstractUpdateStrategy} on a dataset such as TUDCS6
 * (http://areca.co/10/Feed-Item-Dataset-TUDCS6)
 * 
 * <p>
 * The evaluation is required be configured using palladian.properties, see description provided there.
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
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetCreator.class);

    private EvaluationFeedDatabase feedStore;

    /**
     * The name of the database table to write simulated poll data to. This name is specific for an update strategy,
     * its parameters and the time it has been created, e.g. "eval_fixLearned_min_time_100_2011-11-02_14-59-58".
     */
    private static String simulatedPollsDbTable;

    private FeedReader feedReader;

    public DatasetEvaluator(EvaluationFeedDatabase feedStore) {
        // important: reseting the table has to be done >before< creating the FeedReader since the FeedReader reads the
        // table in it's constructor. Any subsequent changes are ignored...
        feedStore.resetTableFeeds();
        this.feedStore = feedStore;
    }

    /**
     * @return The name of the database table to write simulated poll data to. This name is specific for an update
     *         strategy, its parameters and the time it has been created, e.g.
     *         "eval_fixLearned_min_time_100_2011-11-02_14-59-58".
     */
    public static String getSimulatedPollsDbTableName() {
        return simulatedPollsDbTable;
    }
    
    /**
     * Run evaluation of the given strategy on dataset TUDCS6.
     */
    public void runEvaluation() {
        LOGGER.debug("start reading feeds");
        feedReader.start();
    }

    /**
     * Does the initialization:
     * <ul>
     * <li>
     * Initializes all {@link Feed}s so that their lastPollTime is equal to
     * {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} and updateInterval is 0 and update feeds in
     * database. furthermore, removes all feeds that do not contain publish timestamps</li>
     * <li>
     * Set benchmark policy.</li>
     * <li>
     * Set {@link AbstractUpdateStrategy}.</li>
     * <li>
     * Set (database) table name to store evaluation information into.</li>
     * <li>
     * Creates this table.</li>
     * </ul>
     * 
     * @return The current timestamp added to the table's name.
     */
    protected String initialize(int benchmarkPolicy, int benchmarkMode, int benchmarkSampleSize,
            AbstractUpdateStrategy updateStrategy, long wakeUpInterval) {
        // Collection<Feed> feeds = (feedStore).getFeedsWithTimestamps();
        // for (Feed feed : feeds) {
            // feed.setNumberOfItemsReceived(0);
            // feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND));
            // feed.setUpdateInterval(0);
        // }
        FeedReaderEvaluator.setBenchmarkPolicy(benchmarkPolicy);
        FeedReaderEvaluator.setBenchmarkMode(benchmarkMode);
        FeedReaderEvaluator.benchmarkSamplePercentage = benchmarkSampleSize;
        FeedReaderSettings.Builder settingsBuilder = new FeedReaderSettings.Builder();
        settingsBuilder.setStore(feedStore);
        settingsBuilder.setAction(new DefaultFeedProcessingAction());
        settingsBuilder.setUpdateStrategy(updateStrategy);
        settingsBuilder.setWakeUpInterval(wakeUpInterval);
        feedReader = new FeedReader(settingsBuilder.create());

        String timestamp = DateHelper.getCurrentDatetime();

        simulatedPollsDbTable = "eval_" + updateStrategy.getName() + "_" + updateStrategy.getLowestInterval() + "_"
                + updateStrategy.getHighestInterval() + "_" + timestamp;

        boolean created = ((EvaluationFeedDatabase)feedStore).createEvaluationBaseTable(getSimulatedPollsDbTableName());
        if (!created) {
            LOGGER.error("Database table " + getSimulatedPollsDbTableName()
                    + " could not be created. Evaluation is impossible. Processing aborted.");
            System.exit(-1);
        }
        return timestamp;
    }

    /**
     * Get evaluation results from {@link #simulatedPollsDbTable} and write to two tables with the same name plus
     * postfix "_feeds" and "_avg" for intermediate und final results. Additionally, a csv file with cumulated transfer
     * volumes per hour is written.
     */
    protected void generateEvaluationSummary() {
        LOGGER.info("Start generating evaluation summary. This may take a while. Seriously!");
        boolean dataWritten = feedStore.generateEvaluationSummary(getSimulatedPollsDbTableName());
        if (dataWritten) {
            LOGGER.info("Evaluation results have been written to database.");
        } else {
            LOGGER.error("Evaluation results have NOT been written to database!");
        }
        ChartCreator chartCreator = new ChartCreator(200, 200);
        String[] dbTable = {getSimulatedPollsDbTableName()};
        chartCreator.transferVolumeCreator((EvaluationFeedDatabase)feedStore, dbTable);

    }

    /**
     * Start evaluation of an {@link AbstractUpdateStrategy}.
     * 
     * @param args
     */
    public static void main(String[] args) {

        // load configuration from palladian.properies
        Configuration config = ConfigHolder.getInstance().getConfig();
        AbstractUpdateStrategy updateStrategy = null;
        int benchmarkMode = -1;
        boolean fatalErrorOccurred = false;
        StringBuilder logMsg = new StringBuilder();
        logMsg.append("Initialize DatasetEvaluator. Evaluating strategy ");

        final EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, config);

        try {

            // read interval bounds
            int minInterval = config.getInt("datasetEvaluator.minCheckInterval");
            int maxInterval = config.getInt("datasetEvaluator.maxCheckInterval");

            // validate interval bounds
            if (minInterval >= maxInterval || minInterval < 1 || maxInterval < 1) {
                fatalErrorOccurred = true;
                LOGGER.error("Please set interval bounds bounds properly.");
            }
            // set interval bounds
            else {
                logMsg.append(", minCheckInterval = ");
                logMsg.append(minInterval);
                logMsg.append(", maxCheckInterval = ");
                logMsg.append(maxInterval);
            }

            // read update strategy and interval in case of "Fix"
            String strategy = config.getString("datasetEvaluator.updateStrategy");
            // Fix
            if (strategy.equalsIgnoreCase("Fix")) {
                int fixInterval = config.getInt("datasetEvaluator.fixCheckInterval");

                // check for conflicting interval bounds
                if (fixInterval < minInterval || fixInterval > maxInterval) {
                    fatalErrorOccurred = true;
                    LOGGER.error("Defined fixInterval and interval bounds have conflict! "
                            + "Make sure minInterval <= fixInterval <= maxInterval.");
                }
                updateStrategy = new FixUpdateStrategy(minInterval, maxInterval, fixInterval, FeedUpdateMode.MIN_DELAY);
                logMsg.append(updateStrategy.getName());
            }
            // Fix Learned
            else if (strategy.equalsIgnoreCase("FixLearned")) {
                int fixLearnedMode = config.getInt("datasetEvaluator.fixLearnedMode");
                updateStrategy = new FixLearnedUpdateStrategy(minInterval, maxInterval, fixLearnedMode, FeedUpdateMode.MIN_DELAY);
                logMsg.append(updateStrategy.getName());
            }
            // Adaptive TTL
            else if (strategy.equalsIgnoreCase("AdaptiveTTL")) {
                double weightM = config.getDouble("datasetEvaluator.adaptiveTTLweightM");
                updateStrategy = new AdaptiveTTLUpdateStrategy(minInterval, maxInterval, weightM, FeedUpdateMode.MIN_DELAY);
                logMsg.append(updateStrategy.getName());
            }
            // LRU-2
            else if (strategy.equalsIgnoreCase("LRU2")) {
                updateStrategy = new LRU2UpdateStrategy(-1, -1, FeedUpdateMode.MIN_DELAY);
                logMsg.append(updateStrategy.getName());
            }
            // MAVSync
            else if (strategy.equalsIgnoreCase("MAVSync")) {
                int rssTTLmode = config.getInt("datasetEvaluator.rssTTLMode");
                updateStrategy = new MAVSynchronizationUpdateStrategy(minInterval, maxInterval, rssTTLmode);
                logMsg.append(updateStrategy.getName());

                // TODO: read feedItemBufferSize from config

            }
            // IndHist
            else if (strategy.equalsIgnoreCase("IndHist")) {
                double indHistTheta = config.getDouble("datasetEvaluator.indHistTheta");
                updateStrategy = new IndHistUpdateStrategy(minInterval, maxInterval, indHistTheta, feedStore);
                logMsg.append(updateStrategy.getName());

            }
            // IndHistTTL
            else if (strategy.equalsIgnoreCase("IndHistTTL")) {
                double indHistTheta = config.getDouble("datasetEvaluator.indHistTheta");
                double tBurst = config.getDouble("datasetEvaluator.indHistTTLburst");
                int timeWindowHours = config.getInt("datasetEvaluator.indHistTTLtimeWindowHours");
                double weightM = config.getDouble("datasetEvaluator.adaptiveTTLweightM");
                updateStrategy = new IndHistTTLUpdateStrategy(minInterval, maxInterval, indHistTheta, feedStore,
                        tBurst, timeWindowHours, weightM, FeedUpdateMode.MIN_DELAY);
                logMsg.append(updateStrategy.getName());

            }
            // LIHZUpdateStrategy
            else if (strategy.equalsIgnoreCase("LIHZ")) {
                double indHistTheta = config.getDouble("datasetEvaluator.indHistTheta");
                updateStrategy = new LIHZUpdateStrategy(minInterval, maxInterval, indHistTheta);
                logMsg.append(updateStrategy.getName());

            }

            // Unknown strategy
            else {
                fatalErrorOccurred = true;
                LOGGER.error("Cant read updateStrategy from config.");
            }

            // read and set benchmark mode
            String mode = config.getString("datasetEvaluator.benchmarkMode");
            if (mode.equalsIgnoreCase("time")) {
                benchmarkMode = FeedReaderEvaluator.BENCHMARK_TIME;
            } else if (mode.equalsIgnoreCase("poll")) {
                benchmarkMode = FeedReaderEvaluator.BENCHMARK_POLL;
            } else {
                fatalErrorOccurred = true;
                LOGGER.error("Cant read benchmarkMode from config.");
            }
            logMsg.append(", benchmarkMode = ");
            logMsg.append(mode);

        } catch (Exception e) {
            fatalErrorOccurred = true;
            LOGGER.error("Could not load DatasetEvaluator configuration: " + e.getLocalizedMessage());
        }

        if (!fatalErrorOccurred) {

            LOGGER.info(logMsg.toString());

            // set some defaults not provided by config file
            int benchmarkPolicy = FeedReaderEvaluator.BENCHMARK_MIN_DELAY;
            int benchmarkSampleSize = 100;
            // FeedReader wakeupInterval, used for debugging
            long wakeUpInterval = (60 * 1000);


            DatasetEvaluator evaluator = new DatasetEvaluator(feedStore);
            evaluator.initialize(benchmarkPolicy, benchmarkMode, benchmarkSampleSize, updateStrategy, wakeUpInterval);
            evaluator.runEvaluation();
            evaluator.generateEvaluationSummary();
            // this is a normal exit
            System.exit(0);
        } else {

            LOGGER.error("Exiting.");
        }
    }

}
