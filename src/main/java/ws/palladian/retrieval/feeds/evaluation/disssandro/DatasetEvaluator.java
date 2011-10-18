package ws.palladian.retrieval.feeds.evaluation.disssandro;

import java.util.Date;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.FixUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * Starting Point to evaluate an {@link UpdateStrategy} on the TUDCS6 dataset.
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
    private final String currentDbTable;

    public DatasetEvaluator() {
        final FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance()
                .getConfig());

        feedReader = new FeedReader(feedStore);

        currentDbTable = "feed_evaluation_" + feedReader.getUpdateStrategyName() + "_"
                + FeedReaderEvaluator.getBenchmarkName() + "_" + FeedReaderEvaluator.getBenchmarkModeString() + "_"
                + FeedReaderEvaluator.benchmarkSample + System.currentTimeMillis();
    }

    /**
     * @return The name of the database table to write evaluation results to.
     */
    public String getCurrentDbTableName() {
        return currentDbTable;
    }

    private void createEvaluationDbTable() {
        final String sql = "CREATE TABLE `" + getCurrentDbTableName() + "` ("
                + "`feedID` INT(10) UNSIGNED NOT NULL,"
                + "`numberOfPoll` INT(10) UNSIGNED NOT NULL COMMENT 'how often has this feed been polled (retrieved AND READ)',"
                + "`activityPattern` INT(11) NOT NULL COMMENT 'activity pattern of the feed',"
                + "`sizeOfPoll` INT(11) NOT NULL COMMENT 'the estimated amount of bytes to transfer: HTTP header + XML document',"
                + "`pollTimestamp` BIGINT(20) UNSIGNED NOT NULL COMMENT 'the feed has been pooled AT this TIMESTAMP',"
                + "`checkInterval` INT(11) UNSIGNED DEFAULT NULL COMMENT 'TIME IN minutes we waited betwen LAST AND this CHECK',"
                + "`newWindowItems` INT(10) UNSIGNED NOT NULL COMMENT 'number of NEW items IN the window',"
                + "`missedItems` INT(10) NOT NULL COMMENT 'the number of NEW items we missed because there more NEW items since the LAST poll THAN fit INTO the window',"
                + "`windowSize` INT(10) UNSIGNED NOT NULL COMMENT 'the current size of the feed''s window (number of items FOUND)',"
                + "`cumulatedDelay` DOUBLE DEFAULT NULL COMMENT 'cumulated delay IN seconds, adds absolute delay of polls that were too late'"
                + ") ENGINE=INNODB DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;";

        Logger.getRootLogger().info(sql);
        DatabaseManager dbm = DatabaseManagerFactory.create(DatabaseManager.class, ConfigHolder.getInstance()
                .getConfig());
        dbm.runUpdate(sql);
    }

    /**
     * Run evaluation of the given strategy on dataset TUDCS6.
     */
    public void evaluate(UpdateStrategy updateStrategy) {


        initializeFeeds();

        FeedReaderEvaluator.setBenchmarkPolicy(FeedReaderEvaluator.BENCHMARK_TIME);
        feedReader.setUpdateStrategy(updateStrategy, true);

        // TODO do we need a processing action???
        // FeedProcessingAction fpa = new DatasetProcessingAction(feedStore);
        // feedChecker.setFeedProcessingAction(fpa);

        LOGGER.debug("start reading feeds");
        feedReader.startContinuousReading();
    }

    /**
     * Initializes all {@link Feed}s so that their lastPollTime is equal to
     * {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} and updateInterval is 0 and update feeds in
     * database.
     */
    private void initializeFeeds() {
        for (Feed feed : feedReader.getFeeds()) {
            feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND));
            feed.setUpdateInterval(0);
            feedReader.updateFeed(feed);
        }
    }

    /**
     * Start evaluation of an {@link UpdateStrategy}.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TODO: get Strategy and parameters from command line args
        // UpdateStrategy updateStrategy = new MavStrategyDatasetCreation();
        // updateStrategy.setHighestUpdateInterval(360); // 6hrs
        // updateStrategy.setLowestUpdateInterval(0);

        UpdateStrategy updateStrategy = new FixUpdateStrategy();
        ((FixUpdateStrategy) updateStrategy).setCheckInterval(60); // required by Fix strategies only!

        DatasetEvaluator evaluator = new DatasetEvaluator();
        evaluator.evaluate(updateStrategy);
    }

}
