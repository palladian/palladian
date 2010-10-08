package tud.iir.news;

import java.util.Date;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;

/**
 * The {@link FeedChecker} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @see FeedChecker
 * 
 */
class FeedTask implements Runnable {

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
    private final FeedChecker feedChecker;

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public FeedTask(Feed feed, FeedChecker feedChecker) {
        this.feed = feed;
        this.feedChecker = feedChecker;
    }

    @Override
    public void run() {
        //        LOGGER.info("Beginning of thread.");
        //        SchedulerTask.decrementThreadPoolSize();
        //        SchedulerTask.incrementThreadsAlive();
        NewsAggregator fa = new NewsAggregator();

        // parse the feed and get all its entries, do that here since that takes some time and this is a thread so
        // it can be done in parallel
        // if no benchmark is running, read the entries from the web otherwise from disk
        if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_OFF) {
            feed.updateEntries(false);
        } else {
            feed.updateEntriesFromDisk(feedChecker.findHistoryFile(feed.getId()));
        }

        // classify feed if it has never been classified before
        if (feed.getUpdateClass() == -1) {
            FeedClassifier.classify(feed);
        }

        // remember the time the feed has been checked
        feed.setLastChecked(new Date());

        feedChecker.updateCheckIntervals(feed);

        // perform actions on this feeds entries
        feedChecker.getFeedProcessingAction().performAction(feed);

        if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MIN_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMinCheckInterval() * DateHelper.MINUTE_MS);
        } else if (FeedChecker.getBenchmark() == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {
            feed.addToBenchmarkLookupTime(feed.getMaxCheckInterval() * DateHelper.MINUTE_MS);
        }

        // save the feed back to the database
        fa.updateFeed(feed);

        // LOGGER.info("End of Thread");
        // SchedulerTask.decrementThreadsAlive();
    }

}