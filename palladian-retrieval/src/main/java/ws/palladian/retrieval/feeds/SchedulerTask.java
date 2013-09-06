package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CountMap;

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

    /**
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerTask.class);

    /**
     * The collection of all the feeds this scheduler should create update
     * threads for.
     */
    private transient final FeedReader feedReader;

    /**
     * The thread pool managing threads that read feeds from the feed sources
     * provided by {@link #collectionOfFeeds}.
     */
    private transient final ExecutorService threadPool;

    /**
     * Tasks currently scheduled but not yet checked.
     */
    private transient final Map<Integer, Future<FeedTaskResult>> scheduledTasks;

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


    // //////////// Monitoring constants \\\\\\\\\\\\\\\\\


    private Long lastWakeUpTime = null;

    // XXX do we still need this? The content is never read.
    private final CountMap<FeedTaskResult> feedResults = CountMap.create();

    /**
     * Creates a new {@code SchedulerTask} for a feed reader.
     * 
     * @param feedReader
     *            The feed reader containing settings and providing the
     *            collection of feeds to check.
     */
    public SchedulerTask(final FeedReader feedReader) {
        super();
        threadPool = Executors.newFixedThreadPool(feedReader.getThreadPoolSize());
        this.feedReader = feedReader;
        scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        LOGGER.debug("wake up to check feeds");

        // schedule all feeds
        for (Feed feed : getFeeds()) {

            // remove completed FeedTasks
            removeFeedTaskIfDone(feed.getId());
            if (needsLookup(feed)) {
                if (!scheduledTasks.containsKey(feed.getId())) {
                    scheduledTasks.put(feed.getId(), threadPool.submit(new FeedTask(feed, feedReader)));
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
            feedList.addAll(feedReader.getFeeds());
            Collections.shuffle(feedList);
            return feedList;
        } else {
            return feedReader.getFeeds();
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
    private Boolean needsLookup(final Feed feed) {
        final long now = System.currentTimeMillis();
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
                + (feed.getLastPollTime() != null ? "\nnow - lastPollTime: " + (now - feed.getLastPollTime().getTime())
                        + "\nUpdate Interval Exceeded "
                        + (now - feed.getLastPollTime().getTime() > TimeUnit.MINUTES.toMillis(feed.getUpdateInterval()))
                        : ""));

        // check whether the feed needs to be blocked
        if (!feed.isBlocked()) {
            if (feed.getChecks() + feed.getUnreachableCount() + feed.getUnparsableCount() >= 3
                    && feed.getAverageProcessingTime() >= MAXIMUM_AVERAGE_PROCESSING_TIME_MS) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") takes on average too long to process and is therefore blocked (never scheduled again)!"
                        + " Average processing time was " + feed.getAverageProcessingTime() + " milliseconds.");
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
            } else if (feed.getChecks() < feed.getUnreachableCount() / CHECKS_TO_UNREACHABLE_RATIO) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unreachable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unreachableCount = " + feed.getUnreachableCount());
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
            } else if (feed.getChecks() < feed.getUnparsableCount() / CHECKS_TO_UNPARSABLE_RATIO) {
                LOGGER.error("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unparsable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unparsableCount = " + feed.getUnparsableCount());
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
            }
        }

        boolean isBlocked = feed.isBlocked();
        boolean immediateRetry = (feed.getChecks() == 0) && (feed.getUnreachableCount() <= MAX_IMMEDIATE_RETRIES)
                && (feed.getUnparsableCount() <= MAX_IMMEDIATE_RETRIES);
        boolean notYetPolled = (feed.getLastPollTime() == null);
        boolean regularSchedule = !notYetPolled
                && (now - feed.getLastPollTime().getTime() > TimeUnit.MINUTES.toMillis(feed.getUpdateInterval()));

        Boolean ret = !isBlocked && (immediateRetry || notYetPolled || regularSchedule);
        if (ret == true) {
            LOGGER.trace("Feed with id: " + feed.getId() + " need lookup.");
        } else {
            LOGGER.trace("Feed with id: " + feed.getId() + " needs no lookup.");
        }
        return ret;
    }

    /**
     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
     * 
     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
     */
    private void removeFeedTaskIfDone(final Integer feedId) {
        final Future<FeedTaskResult> future = scheduledTasks.get(feedId);
        if (future != null && future.isDone()) {
            scheduledTasks.remove(feedId);
            try {
                feedResults.add(future.get());
            } catch (InterruptedException e) {
                LOGGER.error("Encountered InterruptedException");
            } catch (ExecutionException e) {
                LOGGER.error("Encountered ExecutionException", e);
            }
        }
    }

}
