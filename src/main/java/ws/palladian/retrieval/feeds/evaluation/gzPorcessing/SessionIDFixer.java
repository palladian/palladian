package ws.palladian.retrieval.feeds.evaluation.gzPorcessing;

import java.util.Timer;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * TUDCS6 specific.<br />
 * Reconstruct the csv file from persisted gz files to eliminate false positive MISSes caused by altering window sizes
 * and wrong item hashes in case of seesionIDs in items' link and raw id attributes.
 * 
 * @author Sandro Reichert
 * 
 */
public class SessionIDFixer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SessionIDFixer.class);

    /**
     * Schedule all {@link GZFeedTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * DateHelper.SECOND_MS;

    public SessionIDFixer() {
        checkScheduler = new Timer();
    }

    public void removeFalseMisses() {

        final FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class);
        FeedReader feedChecker = new FeedReader(feedStore);

        SessionIDFixProcessingAction fpa = new SessionIDFixProcessingAction(feedStore);
        feedChecker.setFeedProcessingAction(fpa);

        GZScheduler gzScheduler = new GZScheduler(feedChecker);
        checkScheduler.schedule(gzScheduler, 0, wakeUpInterval);

    }

    /**
     * Run correction of false the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        SessionIDFixer sidFixer = new SessionIDFixer();
        sidFixer.removeFalseMisses();
    }

}
