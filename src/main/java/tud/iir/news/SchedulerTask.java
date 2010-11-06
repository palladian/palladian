package tud.iir.news;

import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;

/**
 * A scheduler task handles the distribution of feeds to worker threads that read these feeds.
 * 
 * @author Klemens Muthmann
 * 
 */
class SchedulerTask extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(SchedulerTask.class);

    /**
     * The collection of all the feeds this scheduler should create update threads for.
     */
    private final FeedReader feedChecker;

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
    public SchedulerTask(FeedReader feedChecker) {
        super();
        threadPool = Executors.newFixedThreadPool(FeedReader.MAX_THREAD_POOL_SIZE);
        this.feedChecker = feedChecker;
        scheduledTasks = new HashMap<Integer, Future<?>>();
    }

    private static int threadPoolQueueSize = 0;
    private static int threadsAlive = 0;

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        LOGGER.debug("wake up to check feeds");
        int feedCount = 0;
        for (Feed feed : feedChecker.getFeeds()) {
            LOGGER.debug("checking feed at address: " + feed.getFeedUrl());

            // check whether feed is in the queue already
            if (isScheduled(feed.getId())) {
                continue;
            }

            if (needsLookup(feed)) {
                //                incrementThreadPoolSize();
                scheduledTasks.put(feed.getId(), threadPool.submit(new FeedTask(feed, feedChecker)));
                feedCount++;
            }

        }

        LOGGER.info("scheduled " + feedCount + " feeds for reading");
    }

    public static synchronized void incrementThreadPoolSize() {
        threadPoolQueueSize++;
        LOGGER.info("inc queue size to: " + threadPoolQueueSize);
    }

    public static synchronized void decrementThreadPoolSize() {
        threadPoolQueueSize--;
        LOGGER.info("dec queue size to: " + threadPoolQueueSize);
    }

    public static synchronized void incrementThreadsAlive() {
        threadsAlive++;
        LOGGER.info("inc threads alive to: " + threadsAlive);
    }

    public static synchronized void decrementThreadsAlive() {
        threadsAlive--;
        LOGGER.info("dec threads alive to: " + threadsAlive);
    }

    /**
     * <p>
     * Returns whether the last time the provided feed was checked for updates is further in the past than its update
     * interval.
     * </p>
     * 
     * @param feed The feed to check.
     * @return {@code true} if this feeds check interval is over and {@code false} otherwise.
     */
    private Boolean needsLookup(Feed feed) {
        long now = System.currentTimeMillis();
        return feed.getChecks() == 0 || feed.getLastPollTime() == null
                || now - feed.getLastPollTime().getTime() > feed.getMaxCheckInterval() * DateHelper.MINUTE_MS;
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