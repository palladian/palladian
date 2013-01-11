package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.updates.AdaptiveTTLUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.FixLearnedUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.IndHistUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.LIHZUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.MAVSynchronizationUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * Do a batch evaluation on a single update strategy, simulate several interval bounds such as 1, 5, 15, 60 minutes as
 * lower bound and 1 day, 1 week and 4 weeks as upper bound. It is presumed, that the given {@link UpdateStrategy} has
 * already been evaluated by {@link DatasetEvaluator} with {@link IntervalBoundsEvaluator#DEFAULT_LOWER_BOUND} and
 * {@link IntervalBoundsEvaluator#DEFAULT_UPPER_BOUND}, so some evaluation results can be copied from this initial
 * evaluation.
 * 
 * Example with 2 feeds A and B: In the first evaluation, done by {@link DatasetEvaluator}, the lower bound was 1 minute
 * and the upper bound was 1 day. Lets assume feed A has been checked exactly every hour, feed B has been checked in
 * intervals ranging from 2 minutes to 8 hours. Now, IntervalBoundsEvaluator evaluates the same strategy with bounds 5
 * minutes and 1 day. These new bounds only affect feed B, since feed a has always been checked hourly. Therefore, feed
 * A's evaluation results are copied from the first run. Feed B has to be evaluated again. This strategy saves a lot or
 * computing resources!
 * 
 * @author Sandro Reichert
 * 
 */
public class IntervalBoundsEvaluator extends DatasetEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalBoundsEvaluator.class);

    private static final int[] lowerBounds = { 1, 5, 15, 60 };

    private static final int[] upperBounds = { 1440, 10080, 43200 };

    private static final int DEFAULT_LOWER_BOUND = 1;

    private static final int DEFAULT_UPPER_BOUND = 43200;

    public IntervalBoundsEvaluator(EvaluationFeedDatabase feedStore) {
        super(feedStore);
    }

    /**
     * Start evaluation of an {@link UpdateStrategy}, doing multiple evaluations using several interval bounds
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
        logMsg.append("Initialize IntervalBoundsEvaluator. Evaluating strategy ");
        String sourceTableName = "";

        EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder
                .getInstance().getConfig());

        try {
            // read update strategy
            String strategy = config.getString("datasetEvaluator.updateStrategy");
            // Fix Learned
            if (strategy.equalsIgnoreCase("FixLearned")) {
                updateStrategy = new FixLearnedUpdateStrategy();
                int fixLearnedMode = config.getInt("datasetEvaluator.fixLearnedMode");
                ((FixLearnedUpdateStrategy) updateStrategy).setFixLearnedMode(fixLearnedMode);
                logMsg.append(updateStrategy.getName());
            }
            // Adaptive TTL
            else if (strategy.equalsIgnoreCase("AdaptiveTTL")) {
                updateStrategy = new AdaptiveTTLUpdateStrategy();
                double weightM = config.getDouble("datasetEvaluator.adaptiveTTLweightM");
                ((AdaptiveTTLUpdateStrategy) updateStrategy).setWeightM(weightM);
                logMsg.append(updateStrategy.getName());
            }
            // MAVSync
            else if (strategy.equalsIgnoreCase("MAVSync")) {
                updateStrategy = new MAVSynchronizationUpdateStrategy();
                logMsg.append(updateStrategy.getName());
                // TODO: read feedItemBufferSize from config
            }
            // IndHist
            else if (strategy.equalsIgnoreCase("IndHist")) {
                double indHistTheta = config.getDouble("datasetEvaluator.indHistTheta");
                updateStrategy = new IndHistUpdateStrategy(indHistTheta, feedStore);
                logMsg.append(updateStrategy.getName());

            }
            // LIHZUpdateStrategy
            else if (strategy.equalsIgnoreCase("LIHZ")) {
                double indHistTheta = config.getDouble("datasetEvaluator.indHistTheta");
                updateStrategy = new LIHZUpdateStrategy(indHistTheta);
                logMsg.append(updateStrategy.getName());

            }

            // Unknown strategy
            else {
                fatalErrorOccurred = true;
                LOGGER.error("Cant read updateStrategy from config.");
            }

            // read table name to copy evaluation results from
            sourceTableName = config.getString("IntervalBoundsEvaluator.sourceTableName");
            logMsg.append(", copying some evaluation results from table \"").append(sourceTableName).append("\"");

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

            for (int lowerBound : lowerBounds) {
                for (int upperBound : upperBounds) {

                    // skip default setting, this has been done before
                    if (lowerBound == DEFAULT_LOWER_BOUND && upperBound == DEFAULT_UPPER_BOUND) {
                        continue;
                    }

                    // set current interval bounds
                    logMsg = new StringBuilder();
                    logMsg.append("Doing evaluation with");
                    updateStrategy.setLowestUpdateInterval(lowerBound);
                    updateStrategy.setHighestUpdateInterval(upperBound);
                    logMsg.append(" minCheckInterval = ");
                    logMsg.append(lowerBound);
                    logMsg.append(", maxCheckInterval = ");
                    logMsg.append(upperBound);
                    LOGGER.info(logMsg.toString());

                    // recreate EvaluationFeedDatabase with empty caches and buffers for each run
                    feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, ConfigHolder.getInstance()
                            .getConfig());
                    IntervalBoundsEvaluator evaluator = new IntervalBoundsEvaluator(feedStore);
                    String timestamp = evaluator.initialize(benchmarkPolicy, benchmarkMode, benchmarkSampleSize,
                            updateStrategy, wakeUpInterval);


                    // since we have done one evaluation without interval bounds (lower bound 1 minute, no upper bound),
                    // we can re-use some evaluation results from this run. we only need to evaluate feeds that are
                    // affected by the current interval bounds. This saves a lot of processing time!
                    String intervalBoundsTable = updateStrategy.getName() + "_" + lowerBound + "_" + upperBound + "_"
                            + timestamp;

                    LOGGER.info("Start copying data from prior run \"" + sourceTableName
                            + "\". This may take several hours.");

                    // create empty table to store feed ids to process
                    feedStore.createIntervalBoundsTable(intervalBoundsTable);
                    // determine feeds to process in this run, insert them into intervalBoundsTable
                    int numFeeds = feedStore.determineFeedsToProcess(intervalBoundsTable, sourceTableName, lowerBound,
                            upperBound);
                    // prepare table feeds with feeds to process
                    feedStore.truncateTableFeeds();
                    int numFeedsCopied = feedStore.copyFeedsToProcess(intervalBoundsTable);
                    // copy simulated polls from prior evaluation to evaluation result tables of the current run.
                    int numCopiedPolls = feedStore.copySimulatedPollData(getSimulatedPollsDbTableName(), sourceTableName,
                            intervalBoundsTable);
                    // copy single delays from prior evaluation to evaluation result tables of the current run.
                    int numCopiedDelays = feedStore.copySimulatedSingleDelays(getSimulatedPollsDbTableName(),
                            sourceTableName, intervalBoundsTable);

                    LOGGER.info(numFeeds + " feeds need to be processed. Copied " + numFeedsCopied
                            + " feeds to table feeds, " + numCopiedPolls + " simulated polls and " + numCopiedDelays
                            + " delays from table " + sourceTableName);

                    // since we modified table feeds, we need to reload it from db
                    feedStore.reloadFeedsFromDB();

                    // now run the evaluation on all 'remaining' feeds contained in table feeds and create summary.
                    evaluator.runEvaluation();
                    evaluator.generateEvaluationSummary();

                }
            }
            // clean up, restore all feeds from backup.
            feedStore.truncateTableFeeds();
            feedStore.restoreFeedsFromBackup();

            LOGGER.info("All iterations done. Bye.");

            // this is a normal exit
            System.exit(0);
        } else {

            LOGGER.error("Exiting.");
        }
    }

}
