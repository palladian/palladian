package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.gzProcessing;

import java.util.Timer;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Reconstruct the csv file from persisted gz files to eliminate false positive MISSes caused by altering window sizes
 * and wrong item hashes in case of seesionIDs in items' link and raw id attributes.
 * 
 * See package-info for further details.
 * 
 * @author Sandro Reichert
 * 
 */
public class SessionIdFixer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SessionIdFixer.class);

    /**
     * Schedule all {@link GzFeedTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * DateHelper.SECOND_MS;

    public SessionIdFixer() {
        checkScheduler = new Timer();
    }

    public void removeFalseMisses() {

        final FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
        FeedReader feedChecker = new FeedReader(feedStore);

        SessionIdFixProcessingAction fpa = new SessionIdFixProcessingAction(feedStore);
        feedChecker.setFeedProcessingAction(fpa);

        GzScheduler gzScheduler = new GzScheduler(feedChecker);
        checkScheduler.schedule(gzScheduler, 0, wakeUpInterval);

    }

    /**
     * Run correction of false the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        SessionIdFixer sidFixer = new SessionIdFixer();
        sidFixer.removeFalseMisses();
    }

}
