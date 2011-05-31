package ws.palladian.retrieval.feeds;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * @see FeedReader
 * 
 */
class FeedTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FeedTask.class);

    /**
     * The feed retrieved by this task.
     */
    private Feed feed = null;

    /**
     * The feed checker calling this task. // FIXME This is a workaround. Can be fixed by externalizing update
     * strategies to a true strategy pattern.
     */
    private final FeedReader feedReader;

    /**
     * Warn if processing of a feed takes longer than this.
     */
    public static final long EXECUTION_WARN_TIME = 3 * DateHelper.MINUTE_MS;

    /**
     * The result of this task.
     */
    private FeedTaskResult result = FeedTaskResult.OPEN;

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public FeedTask(Feed feed, FeedReader feedChecker) {
        // setName("FeedTask:" + feed.getFeedUrl());
        this.feed = feed;
        this.feedReader = feedChecker;
    }

    @Override
    public FeedTaskResult call() {
        try {
            StopWatch timer = new StopWatch();
            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl() + ")");
            int recentMisses = feed.getMisses();

            // parse the feed and get all its entries, do that here since that takes some time and this is a thread so
            // it can be done in parallel
            FeedRetriever feedRetriever = new FeedRetriever();
            try {
                feedRetriever.updateFeed(feed);
            } catch (FeedRetrieverException e) {
                LOGGER.error("update items of the feed didn't work well, " + e.getMessage());
                feed.setLastPollTime(new Date());
                feed.incrementUnreachableCount();
                feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
                feedReader.updateFeed(feed);
                writeUnparsableFeedToGZ(feed);
                LOGGER.debug("Finished processing of feed id " + feed.getId() + " took " + timer.getElapsedTimeString());
                result = FeedTaskResult.UNREACHABLE;
                return result;
            }

            // remember the time the feed has been checked
            feed.setLastPollTime(new Date());

            // classify feed if it has never been classified before, do it once a month for each feed to be informed
            // about
            // updates
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Activity Pattern: " + feed.getActivityPattern());
                LOGGER.debug("Current time: " + System.currentTimeMillis());
                LOGGER.debug("Last poll time: " + feed.getLastPollTime().getTime());
                LOGGER.debug("Current time - last poll time: "
                        + (System.currentTimeMillis() - feed.getLastPollTime().getTime()));
                LOGGER.debug("Milliseconds in a mont: " + DateHelper.MONTH_MS);
            }
            if (feed.getActivityPattern() == -1
                    || System.currentTimeMillis() - feed.getLastPollTime().getTime() > DateHelper.MONTH_MS) {
                FeedClassifier.classify(feed);
                feed.setByteSize(feed.getRawMarkup().getBytes().length);
            }


            feedReader.updateCheckIntervals(feed);
            feed.setLastSuccessfulCheckTime(feed.getLastPollTime());

            // perform actions on this feeds entries
            LOGGER.debug("Performing action on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
            feedReader.getFeedProcessingAction().performAction(feed);

            feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());

            // save the feed back to the database
            boolean dbSuccess = feedReader.updateFeed(feed);

            // since the feed is kept in memory we need to remove all items and the document stored in the feed
            feed.freeMemory();

            if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
                result = FeedTaskResult.EXECUTION_TIME_WARNING;
                LOGGER.warn("Processing feed id " + feed.getId() + " took very long: " + timer.getElapsedTimeString());
            } else if (recentMisses < feed.getMisses()) {
                result = FeedTaskResult.MISS;
            } else {
                result = FeedTaskResult.SUCCESS;
            }

            if (!dbSuccess) {
                result = FeedTaskResult.ERROR;
            }

            LOGGER.debug("Finished processing of feed id " + feed.getId() + " took " + timer.getElapsedTimeString());
            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
            result = FeedTaskResult.ERROR;
        }
        return result;
    }

    /**
     * Quickndirty write everything that we cant parse to a gz
     * 
     * @param unparsable feed to write to gz
     */
    private void writeUnparsableFeedToGZ(Feed feed) {
        // get the filename of the feed
        String safeFeedName = StringHelper.makeSafeName(
                feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst("www.", ""), 30);

        int slice = (int) Math.floor(feed.getId() / 1000.0);

        String folderPath = DatasetCreator.DATASET_PATH + slice + System.getProperty("file.separator") + feed.getId()
                + System.getProperty("file.separator");
        String filePath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";

        File postEntryFile = new File(filePath);
        if (!postEntryFile.exists()) {
            boolean directoriesCreated = new File(postEntryFile.getParent()).mkdirs();
            try {
                if (directoriesCreated) {
                    postEntryFile.createNewFile();
                } else {
                    LOGGER.error("could not create the directories " + filePath);
                }
            } catch (IOException e) {
                LOGGER.error("could not create the file " + filePath);
            }
        }

        filePath = folderPath + DateHelper.getCurrentDatetime("yyyy-MM-dd_HH-mm-ss") + "_unparsable.gz";
        LOGGER.debug("saving unparsable feed to: " + filePath);

        DocumentRetriever documentRetriever = new DocumentRetriever();
        documentRetriever.downloadAndSave(feed.getFeedUrl(), filePath, true);
    }

}