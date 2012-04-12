package ws.palladian.retrieval.feeds.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.icwsm2011.PollData;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.updates.FixLearnedUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.FixUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.MavUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.PostRateUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * <p>
 * An evaluator for the FeedReader.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class FeedReaderEvaluator {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(FeedReaderEvaluator.class);

    /** Benchmark off. */
    public static final int BENCHMARK_OFF = 0;

    /**
     * Benchmark algorithms towards their prediction ability for the next post. We need all feeds to be in a certain
     * time frame for a fair comparison.
     */
    public static final int BENCHMARK_MIN_DELAY = 1;

    /**
     * Benchmark algorithms towards their prediction ability for the next almost filled post list.
     */
    public static final int BENCHMARK_MAX_COVERAGE = 2;

    /**
     * If true, some output will be generated to evaluate the reading approaches.
     */
    public static int benchmarkPolicy = BENCHMARK_OFF;

    /** We need all feeds to be in a certain time frame for a fair comparison. */
    public static final int BENCHMARK_TIME = 1;

    /** We use the polls and the feed time don't have to be aligned. */
    public static final int BENCHMARK_POLL = 2;

    public static int benchmarkMode = BENCHMARK_POLL;

    /** We just take a certain percentage of the feed benchmark data for performance reasons. */
    public static int benchmarkSamplePercentage = 10;

    /** The path to the folder with the feed post history files. */
    private static final String BENCHMARK_DATASET_PATH = "data/datasets/feedPosts/csv";

    /** The list of history files, will be loaded only once for the sake of performance. */
    private static File[] benchmarkDatasetFiles;

    /**
     * The time to start training update strategies in evaluation.
     * The timestamp almost all (but 172) feeds have been polled at least once. 2011-07-09 7:00:00 CEST. Be careful with
     * time zones since Unix timestamp assumes GMT.
     */
    public static final long BENCHMARK_TRAINING_START_TIME_MILLISECOND = 1310187600000L;

    /**
     * The time to stop training update strategies in evaluation.
     * 2011-07-16 06:59:59 CEST. Be careful with time zones since Unix timestamp assumes GMT.
     */
    public static final long BENCHMARK_TRAINING_STOP_TIME_MILLISECOND = 1310792399000L;

    /**
     * The time to start the 'real' evaluation.
     * The timestamp almost all feeds have been polled at least once. 2011-07-16 07:00:00 CEST. Be careful with
     * time zones since Unix timestamp assumes GMT.
     */
    public static final long BENCHMARK_START_TIME_MILLISECOND = 1310792400000L;

    /**
     * The time to stop the 'real' evaluation.
     * The timestamp we stopped the dataset gathering, minus a buffer to make sure all items published before the stop
     * time have been received. Dataset creation ran till 2011-08-05 13:49 CEST, we set stop time to 2011-08-05 07:00:00
     * CEST. Be careful with time zones since Unix timestamp assumes GMT.
     */
    public static final long BENCHMARK_STOP_TIME_MILLISECOND = 1312520400000L;

    public FeedReaderEvaluator() {
        LOGGER.info("load benchmark dataset file list");
        benchmarkDatasetFiles = FileHelper.getFiles(BENCHMARK_DATASET_PATH);
        // benchmarkDatasetFiles = FileHelper.getFiles(BENCHMARK_DATASET_PATH, ".csv", true, false);
    }

    public static int getBenchmarkPolicy() {
        return benchmarkPolicy;
    }

    public static void setBenchmarkPolicy(int benchmark) {
        FeedReaderEvaluator.benchmarkPolicy = benchmark;
    }

    public static int getBenchmarkMode() {
        return benchmarkMode;
    }

    public static void setBenchmarkMode(int benchmarkMode) {
        FeedReaderEvaluator.benchmarkMode = benchmarkMode;
    }

    public static String getBenchmarkName() {
        return FeedReaderEvaluator.benchmarkPolicy == FeedReaderEvaluator.BENCHMARK_MIN_DELAY ? "min" : "max";
    }

    public String getBenchmarkDatasetPath() {
        return BENCHMARK_DATASET_PATH;
    }

    /**
     * @return The name of the benchmark mode: poll or time.
     */
    public static String getBenchmarkModeString() {
        String benchmarkModeString = "0";
        if (FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_POLL) {
            benchmarkModeString = "poll";
        } else if (FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_TIME) {
            benchmarkModeString = "time";
        }
        return benchmarkModeString;
    }

    /**
     * <p>
     * Save the feed poll information for evaluation. We also save information that is redundant for performance
     * reasons.
     * </p>
     * 
     * <p>
     * For each update technique and evaluation mode another file must be written (8 in total = 4 techniques * 2
     * evaluation modes). This method writes only one file for the current settings.
     * </p>
     * 
     * <p>
     * Each file contains the following fields per line, fields are separated with a semicolon:
     * <ul>
     * <li>feed id</li>
     * <li>number of poll</li>
     * <li>feed activity pattern</li>
     * <li>conditional get response size in Byte (NULL if it neither ETag nor LastModifiedSince is supported)</li>
     * <li>size of poll in Byte</li>
     * <li>poll timestamp in seconds</li>
     * <li>check interval at poll time in minutes</li>
     * <li>number of new items in the window (missed are not counted)</li>
     * <li>number of missed news items</li>
     * <li>window size</li>
     * <li>cumulated delay in seconds (only for evaluation mode MIN interesting)</li>
     * </ul>
     * </p>
     * 
     * @deprecated Recorded evaluation results can be written to database, see
     *             {@link EvaluationFeedDatabase#addPollData(PollData, int, int, String)}
     */
    @Deprecated
    public static void writeRecordedMaps(FeedReader feedReader) {

        StopWatch sw = new StopWatch();

        String separator = ";";

        String filePath = "data/temp/feedReaderEvaluation_" + feedReader.getUpdateStrategyName() + "_"
                + getBenchmarkName() + "_" + getBenchmarkModeString() + "_"
                + FeedReaderEvaluator.benchmarkSamplePercentage + ".csv";

        try {
            FileWriter fileWriter = new FileWriter(filePath, true);

            // loop through all feeds
            for (Feed feed : feedReader.getFeeds()) {

                int numberOfPoll = 1;
                for (PollData pollData : feed.getPollDataSeries()) {

                    StringBuilder csv = new StringBuilder();

                    // feed related values
                    csv.append(feed.getId()).append(separator);
                    csv.append(numberOfPoll).append(separator);
                    csv.append(feed.getActivityPattern()).append(separator);

                    if (feed.getMetaInformation().getCgHeaderSize() != null) {
                        csv.append(feed.getMetaInformation().getCgHeaderSize()).append(separator);
                    } else {
                        csv.append("\\N").append(separator);
                    }

                    // poll related values
                    csv.append(pollData.getDownloadSize()).append(separator);
                    csv.append(pollData.getPollTimestamp() / 1000l).append(separator);
                    // csv.append(DateHelper.getTimeOfDay(pollData.getTimestamp(), Calendar.MINUTE)).append(separator);
                    csv.append(MathHelper.round(pollData.getCheckInterval(), 2)).append(separator);
                    csv.append(pollData.getNewWindowItems()).append(separator);
                    csv.append(pollData.getMisses()).append(separator);
                    csv.append(pollData.getWindowSize()).append(separator);
                    csv.append(pollData.getCumulatedDelay() / 1000l).append(separator);
                    // csv.append(pollData.getCumulatedLateDelay() / 1000l).append(separator);

                    // if (pollData.getTimeliness() != null) {
                    // csv.append(MathHelper.round(pollData.getTimeliness(), 4)).append(separator);
                    // } else {
                    // csv.append("\\N").append(separator);
                    // }
                    //
                    // if (pollData.getTimelinessLate() != null) {
                    // csv.append(MathHelper.round(pollData.getTimelinessLate(), 4)).append(separator);
                    // } else {
                    // csv.append("\\N").append(separator);
                    // }

                    csv.append("\n");

                    fileWriter.write(csv.toString());
                    fileWriter.flush();

                    numberOfPoll++;
                }

                // data is appended to the file so we can/must clear the poll data series here, that also saves us
                // memory
                feed.getPollDataSeries().clear();
            }

            fileWriter.flush();
            fileWriter.close();

        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }

        LOGGER.info("wrote record maps in " + sw.getElapsedTimeString());

    }

    /**
     * Find the history file with feed posts given the feed id. The file name starts with the feed id followed by an
     * underscore.
     * 
     * @param id The id of the feed.
     * @return The path to the file with the feed post history.
     * @deprecated The history files are written to db feed_evaluation_items
     */
    @Deprecated
    public static String findHistoryFile(String safeFeedName) {

        // read feed history file
        String historyFilePath = "";
        if (benchmarkDatasetFiles == null) {
            LOGGER.info("======================================================================================");
            benchmarkDatasetFiles = FileHelper.getFiles(BENCHMARK_DATASET_PATH);
        }
        for (File file : benchmarkDatasetFiles) {
            if (file.getName().startsWith(safeFeedName)) {
                historyFilePath = file.getAbsolutePath();
                break;
            }
        }

        return historyFilePath;
    }

    /**
     * <p>
     * Create (snapshots of) all combinations of dataset evaluations.<br>
     * Strategy: FIX, MAV, POSTRATE<br>
     * Policy: MIN_DELAY, MAX_COVERAGE<br>
     * Mode: POLL, TIME
     * </p>
     */
    public void createAllEvaluations(int benchmarkSample) {

        FeedReaderEvaluator.benchmarkSamplePercentage = benchmarkSample;

        UpdateStrategy[] strategies = { new FixUpdateStrategy(60), new FixUpdateStrategy(1440),
                new FixLearnedUpdateStrategy(), new MavUpdateStrategy(), new PostRateUpdateStrategy() };

        Integer[] policies = { BENCHMARK_MIN_DELAY, BENCHMARK_MAX_COVERAGE };
        Integer[] modes = { BENCHMARK_POLL, BENCHMARK_TIME };

        for (UpdateStrategy strategy : strategies) {

            for (Integer policy : policies) {

                // for FIX with a preset interval min_delay and max_coverage are the same and we skip one
                if (strategy instanceof FixUpdateStrategy && policy == BENCHMARK_MIN_DELAY) {
                    continue;
                }

                setBenchmarkPolicy(policy);

                for (Integer mode : modes) {

                    setBenchmarkMode(mode);

                    FeedReader fc = new FeedReader(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()));
                    fc.setUpdateStrategy(strategy, false);

                    LOGGER.info("start evaluation for strategy " + strategy.getName() + ", policy "
                            + policy + ", and mode " + mode);
                    fc.startContinuousReading(-1);

                }
            }
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // FeedReaderEvaluator fre = new FeedReaderEvaluator();
        // fre.createAllEvaluations(1);
        // System.exit(0);

        // if -1 => fixed learned
        int checkInterval = 60;

        UpdateStrategy updateStrategy = new FixUpdateStrategy(checkInterval);

        // updateStrategy = new MavUpdateStrategy();
        // updateStrategy = new PostRateUpdateStrategy();
        // checkType = UpdateStrategy.UPDATE_POST_RATE_MOVING_AVERAGE;

        FeedReaderEvaluator.benchmarkSamplePercentage = 100; // use just a percentage of

        FeedReader feedReader = new FeedReader(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder
                .getInstance().getConfig()));
        feedReader.setUpdateStrategy(updateStrategy, true);
        // setBenchmarkPolicy(BENCHMARK_MAX_COVERAGE);
        setBenchmarkPolicy(BENCHMARK_MIN_DELAY);
        setBenchmarkMode(BENCHMARK_POLL);
        // setBenchmarkMode(BENCHMARK_TIME);
        feedReader.startContinuousReading(-1);
    }

}