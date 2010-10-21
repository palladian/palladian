package tud.iir.news;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import tud.iir.helper.MathHelper;

/**
 * <p>
 * A scheduler task handles the distribution of feeds to worker threads that read these feeds in benchmark mode of the
 * {@link FeedChecker}.
 * </p>
 * <p>
 * The main difference to {@link SchedulerTask} is that we process feeds serially instead of parallel. The reason is
 * that we want to step through the benchmark files very quickly and need to wake the SchedulerTaskBenchmark extremely
 * frequently. In parallel mode there is no CPU time for the single tasks to finish.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
class SchedulerTaskBenchmark extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(SchedulerTaskBenchmark.class);

    /**
     * The collection of all the feeds this scheduler should create update threads for.
     */
    private final FeedChecker feedChecker;

    /**
     * The thread pool managing threads that read feeds from the feed sources provided by {@link #collectionOfFeeds}.
     */
    private final ExecutorService threadPool;

    private final Map<Integer, Future<?>> scheduledTasks;

    /**
     * Creates a new scheduler task with a maximum allowed number of threads and a list of feeds to read updates from.
     * 
     * @param collectionOfFeeds The collection of all the feeds this scheduler should create update threads for.
     * @param threadPoolSize The maximum number of threads to distribute reading feeds to.
     */
    public SchedulerTaskBenchmark(FeedChecker feedChecker) {
        super();
        threadPool = Executors.newFixedThreadPool(FeedChecker.MAX_THREAD_POOL_SIZE);
        this.feedChecker = feedChecker;
        scheduledTasks = new HashMap<Integer, Future<?>>();
    }

    /**
    * 
    */
    @Override
    public void run() {
        LOGGER.info("wake up to check feeds");
        int feedCount = 0;
        int feedHistoriesCompletelyRead = 0;
        for (Feed feed : feedChecker.getFeeds()) {

            // check whether feed is in the queue already, if it is scheduled wait for it to finish
            if (isScheduled(feed.getId())) {
                break;
            }

            if (needsLookup(feed)) {
                LOGGER.debug("checking feed at address: " + feed.getFeedUrl());

                scheduledTasks.put(feed.getId(), threadPool.submit(new FeedTask(feed, feedChecker)));
                feedCount++;
                break;
            }

            if (feed.historyFileCompletelyRead()) {
                feedHistoriesCompletelyRead++;

                if (feedHistoriesCompletelyRead == feedChecker.getFeeds().size()) {
                    LOGGER.info("all feed history files read");
                    feedChecker.stopContinuousReading();
                }
            }
        }


        LOGGER.debug(MathHelper.round(100 * feedHistoriesCompletelyRead / feedChecker.getFeeds().size(), 2)
                + "% of history files completely read (absolute: " + feedHistoriesCompletelyRead + ")");

        LOGGER.info("scheduled " + feedCount + " feeds for reading");
    }

    /**
     * <p>
     * Returns whether the last time the provided feed was checked for updates is further in the past than its update
     * interval. For benchmarking we don't wait but lookup until we have seen everything.
     * </p>
     * 
     * @param feed The feed to check.
     * @return {@code true} if this feeds check interval is over and {@code false} otherwise.
     */
    private Boolean needsLookup(Feed feed) {
        if (feed.historyFileCompletelyRead()) {
            return false;
        }

        return true;
    }

    /**
     * <p>
     * Checks if this feed is already queued for updates. If this is the case one should not queue it a second time to
     * reduce traffic.
     * </p>
     * 
     * @param feedId
     *            The id of the feed to check.
     * @return {@code true} if this feed is already queued and {@code false} otherwise.
     */
    private Boolean isScheduled(final Integer feedId) {
        Future<?> future = scheduledTasks.get(feedId);
        if (future == null) {
            return false;
        } else {
            if (future.isDone()) {
                scheduledTasks.remove(feedId);
                return false;
            } else {
                return true;
            }
        }
    }
}