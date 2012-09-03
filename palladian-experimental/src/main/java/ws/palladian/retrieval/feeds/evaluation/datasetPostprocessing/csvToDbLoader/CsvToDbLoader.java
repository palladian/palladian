package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.csvToDbLoader;

import java.util.Timer;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Load csv data into database table "feed_evaluation_items"
 * 
 * @author Sandro Reichert
 * 
 */
public class CsvToDbLoader {

    /** The logger for this class. */
    @SuppressWarnings("unused")
    private static final Logger LOGGER = Logger.getLogger(CsvToDbLoader.class);

    /**
     * Schedule all {@link CsvToDbTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds are processed.
     */
    private final long wakeUpInterval = 60 * DateHelper.SECOND_MS;

    public CsvToDbLoader() {
        checkScheduler = new Timer();
    }

    public void loadDataToDb() {

        final FeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class,
                ConfigHolder.getInstance().getConfig());

        FeedReader feedChecker = new FeedReader(feedStore);
        CsvToDbScheduler csvToDbScheduler = new CsvToDbScheduler(feedChecker);
        checkScheduler.schedule(csvToDbScheduler, 0, wakeUpInterval);
    }

    /**
     * Run correction of false the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        CsvToDbLoader csvToDbLoader = new CsvToDbLoader();
        csvToDbLoader.loadDataToDb();
    }

}
