package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A scheduler task handles the distribution of feeds to worker threads that read these feeds.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
class SchedulerTask extends TimerTask {

    /** The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerTask.class);

    /**
     * The number of times a feed that has never been checked successfully is put into the queue regardless of its
     * update interval.
     */
    private static final int MAX_IMMEDIATE_RETRIES = 3;

    /**
     * Max allowed ratio of unreachableCount : checks. If feed was unreachable more often, don't schedule it in the
     * future.
     */
    private static final int CHECKS_TO_UNREACHABLE_RATIO = 10;

    /**
     * Max allowed ratio of unparsableCount : checks. If feed was unparsable more often, don't schedule it in the
     * future.
     */
    private static final int CHECKS_TO_UNPARSABLE_RATIO = 10;

    /**
     * Max allowed average time to process a feed. If processing takes longer on average, don't schedule it in the
     * future.
     */
    private static final long MAXIMUM_AVERAGE_PROCESSING_TIME_MS = TimeUnit.MINUTES.toMillis(10);

//    /** The store providing persistence for the feeds. */
//    private final FeedStore store;

//    /** The actions to perform when processing a feed (and various other cases). */
//    private final FeedProcessingAction action;

//    /** The strategy to determine a feed's update behaviour. */
//    private final UpdateStrategy updateStrategy;

    /** The thread pool managing threads that read feeds from the feed sources provided by {@link #collectionOfFeeds}. */
    private final ExecutorService threadPool;

    /** Tasks currently scheduled but not yet checked. */
    private final Map<Integer, Future<FeedTaskResult>> scheduledTasks;

    private final Long lastWakeUpTime = null;

    private final FeedReaderSettings settings;

    /**
     * <p>
     * Creates a new {@code SchedulerTask} for a feed reader.
     * </p>
     * 
     * @param All necessary settings, not <code>null</code>.
     */
    SchedulerTask(FeedReaderSettings settings) {
        this.threadPool = Executors.newFixedThreadPool(settings.getNumThreads());
        this.scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();
        this.settings = settings;
    }
    
//    /**
//     * <p>
//     * Creates a new {@code SchedulerTask} for a feed reader.
//     * </p>
//     * 
//     * @param threadPoolSize Maximum number of {@link FeedTask}s to schedule.
//     * @param store The store with the feeds.
//     * @param action The action to perform for each read feed.
//     * @param updateStrategy The strategy to use for determining update behavior.
//     */
//    public SchedulerTask(int threadPoolSize, FeedStore store, FeedProcessingAction action, UpdateStrategy updateStrategy) {
//        this.threadPool = Executors.newFixedThreadPool(threadPoolSize);
//        this.scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();
//        this.store = store;
//        this.action = action;
//        this.updateStrategy = updateStrategy;
//    }

    @Override
    public void run() {
        LOGGER.debug("wake up to check feeds");

        // schedule all feeds
        for (Feed feed : getFeeds()) {

            // remove completed FeedTasks
            removeFeedTaskIfDone(feed.getId());
            if (needsLookup(feed)) {
                if (!scheduledTasks.containsKey(feed.getId())) {
                    scheduledTasks.put(feed.getId(),
                            threadPool.submit(new FeedTask(settings, feed)));
                }
            }
        }
    }

    /**
     * Get all feeds from database. When called the first time, the feeds are shuffled randomly, on all subsequent
     * calls, the ordering received from database is preserved.
     * <p>
     * Background: In some feed lists, there are several hundred feeds hosted by the same provider like feedburner. The
     * shuffle is required to avoid polling one provider with several hundred threads in parallel since some providers
     * tend to block those parallel requests.
     * </p>
     * 
     * @return
     */
    private Collection<Feed> getFeeds() {
        if (lastWakeUpTime == null) {
            List<Feed> feedList = new ArrayList<Feed>();
            feedList.addAll(settings.getStore().getFeeds());
            Collections.shuffle(feedList);
            return feedList;
        } else {
            return settings.getStore().getFeeds();
        }
    }

    /**
     * Returns whether the last time the provided feed was checked for updates
     * is further in the past than its update interval.
     * 
     * @param feed
     *            The feed to check.
     * @return {@code true} if this feeds check interval is over and {@code false} otherwise.
     */
    private boolean needsLookup(Feed feed) {
        final long now = System.currentTimeMillis();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking if feed with id: "
                    + feed.getId()
                    + " needs lookup!\n FeedChecks: "
                    + feed.getChecks()
                    + "\nLastPollTime: "
                    + feed.getLastPollTime()
                    + "\nNow: "
                    + now
                    + "\nUpdateInterval: "
                    + TimeUnit.MINUTES.toMillis(feed.getUpdateInterval())
                    + (feed.getLastPollTime() != null ? "\nnow - lastPollTime: "
                            + (now - feed.getLastPollTime().getTime())
                            + "\nUpdate Interval Exceeded "
                            + (now - feed.getLastPollTime().getTime() > TimeUnit.MINUTES.toMillis(feed
                                    .getUpdateInterval())) : ""));
        }

        // check whether the feed needs to be blocked
        if (!feed.isBlocked()) {
            if (feed.getChecks() + feed.getUnreachableCount() + feed.getUnparsableCount() >= 3
                    && feed.getAverageProcessingTime() >= MAXIMUM_AVERAGE_PROCESSING_TIME_MS) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") takes on average too long to process and is therefore blocked (never scheduled again)!"
                        + " Average processing time was " + feed.getAverageProcessingTime() + " milliseconds.");
                feed.setBlocked(true);
                settings.getStore().updateFeed(feed);
            } else if (feed.getChecks() < feed.getUnreachableCount() / CHECKS_TO_UNREACHABLE_RATIO) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unreachable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unreachableCount = " + feed.getUnreachableCount());
                feed.setBlocked(true);
                settings.getStore().updateFeed(feed);
            } else if (feed.getChecks() < feed.getUnparsableCount() / CHECKS_TO_UNPARSABLE_RATIO) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unparsable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unparsableCount = " + feed.getUnparsableCount());
                feed.setBlocked(true);
                settings.getStore().updateFeed(feed);
            }
        }

        boolean isBlocked = feed.isBlocked();
        boolean immediateRetry = feed.getChecks() == 0 && feed.getUnreachableCount() <= MAX_IMMEDIATE_RETRIES
                && feed.getUnparsableCount() <= MAX_IMMEDIATE_RETRIES;
        boolean notYetPolled = feed.getLastPollTime() == null;
        boolean regularSchedule = !notYetPolled
                && now - feed.getLastPollTime().getTime() > TimeUnit.MINUTES.toMillis(feed.getUpdateInterval());

        boolean ret = !isBlocked && (immediateRetry || notYetPolled || regularSchedule);
        if (ret == true) {
            LOGGER.trace("Feed with id: {} needs lookup.", feed.getId());
        } else {
            LOGGER.trace("Feed with id: {} needs no lookup.", feed.getId());
        }
        return ret;
    }

    /**
     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
     * 
     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
     */
    private void removeFeedTaskIfDone(int feedId) {
        final Future<FeedTaskResult> future = scheduledTasks.get(feedId);
        if (future != null && future.isDone()) {
            scheduledTasks.remove(feedId);
        }
    }

}
