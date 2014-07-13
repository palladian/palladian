package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.csvToDbLoader;

import java.util.Timer;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReaderSettings;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;

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
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvToDbLoader.class);

    /**
     * Schedule all {@link CsvToDbTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds are processed.
     */
    private final long wakeUpInterval = 60 * 1000;

    public CsvToDbLoader() {
        checkScheduler = new Timer();
    }

    public void loadDataToDb() {

        Configuration config = ConfigHolder.getInstance().getConfig();
        EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, config);

        CsvToDbScheduler csvToDbScheduler = new CsvToDbScheduler(feedStore, FeedReaderSettings.DEFAULT_NUM_THREADS);
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
