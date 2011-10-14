package ws.palladian.retrieval.feeds.evaluation.disssandro;

import java.util.Date;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.MavStrategyDatasetCreation;
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
    private FeedReader feedReader;

    /**
     * Run evaluation of the given strategy on dataset TUDCS6.
     */
    public void evaluate(UpdateStrategy updateStrategy) {
        final FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance()
                .getConfig());

        feedReader = new FeedReader(feedStore);

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
     * Initializes all Feeds so that their lastPollTime is equal to
     * {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} and updateInterval is 0.
     */
    private void initializeFeeds() {
        for (Feed feed : feedReader.getFeeds()) {
            feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND));
            feed.setUpdateInterval(0);
        }
    }

    /**
     * Start evaluation of an {@link UpdateStrategy}.
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TODO: get Strategy and parameters from command line args
        MavStrategyDatasetCreation updateStrategy = new MavStrategyDatasetCreation();
        updateStrategy.setHighestUpdateInterval(360); // 6hrs
        updateStrategy.setLowestUpdateInterval(0);

        DatasetEvaluator evaluator = new DatasetEvaluator();
        evaluator.evaluate(updateStrategy);
    }

}
