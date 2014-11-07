package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.feedSizeCalculator;

import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.csvToDbLoader.CsvToDbTask;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Main class to calculate feed size from gz files for every single poll and write to database table feed_polls, column
 * responseSize.
 * 
 * @author Sandro Reichert
 * 
 */
public class FeedSizeCalculator {

    /** The logger for this class. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedSizeCalculator.class);

    /**
     * Schedule all {@link CsvToDbTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * 1000;

    public FeedSizeCalculator() {
        checkScheduler = new Timer();
    }

    public void restoreFeedSizes() {
        Configuration config = ConfigHolder.getInstance().getConfig();
        FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class, config);
        TimerTask csvToDbScheduler = new FeedSizeCalculationScheduler(feedStore, FeedReader.DEFAULT_NUM_THREADS);
        checkScheduler.schedule(csvToDbScheduler, 0, wakeUpInterval);
    }

    /**
     * Calculate feed size from gz files for every single poll and write to database table feed_polls, column
     * responseSize.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        FeedSizeCalculator feedSizeCalculator = new FeedSizeCalculator();
        feedSizeCalculator.restoreFeedSizes();
    }

}
