package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.gzProcessing;

import java.util.Timer;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;

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

    /**
     * Schedule all {@link GzFeedTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * 1000;

    public SessionIdFixer() {
        checkScheduler = new Timer();
    }

    public void removeFalseMisses() {

        Configuration config = ConfigHolder.getInstance().getConfig();
        EvaluationFeedDatabase feedStore = DatabaseManagerFactory.create(EvaluationFeedDatabase.class, config);
        SessionIdFixProcessingAction fpa = new SessionIdFixProcessingAction(feedStore);
        GzScheduler gzScheduler = new GzScheduler(feedStore, fpa);
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
