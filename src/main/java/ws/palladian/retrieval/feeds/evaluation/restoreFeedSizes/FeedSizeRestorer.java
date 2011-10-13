package ws.palladian.retrieval.feeds.evaluation.restoreFeedSizes;

import java.util.Timer;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.csvToDb.CsvToDbTask;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Load csv data into database table "feed_evaluation_items"
 * 
 * @author Sandro Reichert
 * 
 */
public class FeedSizeRestorer {

    /** The logger for this class. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(FeedSizeRestorer.class);

    /**
     * Schedule all {@link CsvToDbTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * DateHelper.SECOND_MS;

    public FeedSizeRestorer() {
        checkScheduler = new Timer();
    }

    @SuppressWarnings("deprecation")
    public void restoreFeedSizes() {

        final FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class);
        FeedReader feedChecker = new FeedReader(feedStore);
        FeedSizeRestoreScheduler csvToDbScheduler = new FeedSizeRestoreScheduler(feedChecker);
        checkScheduler.schedule(csvToDbScheduler, 0, wakeUpInterval);
    }

    /**
     * Run correction of false the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        FeedSizeRestorer feedSizeRestorer = new FeedSizeRestorer();
        feedSizeRestorer.restoreFeedSizes();
    }

}
