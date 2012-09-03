package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.util.Collection;
import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.FeedTaskResult;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

/**
 * <p>
 * A scheduler task handles the distribution of feeds to worker threads that read these feeds. Feeds are scheduled as
 * long as {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND} is reached.
 * </p>
 * Class based on {@link ws.palladian.retrieval.feeds.SchedulerTask}
 * 
 * @author Sandro Reichert
 * 
 */
public class EvaluationSchedulerTask extends TimerTask {

    /**
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     */
    private static final Logger LOGGER = Logger.getLogger(EvaluationSchedulerTask.class);

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
     * If wake up interval exceeds this time, do some warning.
     */
    private static final long SCHEDULER_INTERVAL_WARNING_TIME_MS = 2 * DateHelper.MINUTE_MS;

    /** Count the number of processed feeds per scheduler iteration. */
    private int processedCounter = 0;

    /** Count the number of feeds that have been blocked during the current scheduler iteration. */
    private int blockedCounter = 0;

    /** Count consecutive delay warnings. */
    private int consecutiveDelays = 0;

    private Long lastWakeUpTime = null;

    private final HashBag<FeedTaskResult> feedResults = new HashBag<FeedTaskResult>();

    /** Number of Feeds that are expected to be processed per minute */
    private static int HIGH_LOAD_THROUGHPUT = 0;

    /** 2 percent of the feeds processed per interval are allowed to be unreachable */
    private static final int MAX_UNREACHABLE_PERCENTAGE_DEFAULT = 2;

    /** This many percent of the feeds processed per interval are allowed to be unreachable */
    private static int maxUnreachablePercentage = MAX_UNREACHABLE_PERCENTAGE_DEFAULT;

    /** 2 percent of the feeds processed per interval are allowed to be unparsable. */
    private static final int MAX_UNPARSABLE_PERCENTAGE_DEFAULT = 2;

    /** This many percent of the feeds processed per interval are allowed to be unparsable. */
    private static int maxUnparsablePercentage = MAX_UNREACHABLE_PERCENTAGE_DEFAULT;

    /** 10 percent of the feeds processed per interval are allowed to be slow. */
    private static final int MAX_SLOW_PERCENTAGE_DEFAULT = 10;

    /** This many percent of the feeds processed per interval are allowed to be slow. */
    private static int maxSlowPercentage = MAX_SLOW_PERCENTAGE_DEFAULT;

    /**
     * Creates a new {@code SchedulerTask} for a feed reader.
     * 
     * @param feedReader
     *            The feed reader containing settings and providing the
     *            collection of feeds to check.
     */
    public EvaluationSchedulerTask(final FeedReader feedReader) {
        super();
        threadPool = Executors.newFixedThreadPool(feedReader.getThreadPoolSize());
        this.feedReader = feedReader;
        scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();


        // configure monitoring and logging
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        if (config != null) {
            maxSlowPercentage = config.getInt("schedulerTask.maxSlowPercentage", MAX_SLOW_PERCENTAGE_DEFAULT);
            maxUnreachablePercentage = config.getInt("schedulerTask.maxUnreachablePercentage",
                    MAX_UNREACHABLE_PERCENTAGE_DEFAULT);
            maxUnparsablePercentage = config.getInt("schedulerTask.maxUnparsablePercentage",
                    MAX_UNPARSABLE_PERCENTAGE_DEFAULT);

        }

        // on average, one thread has 5 minutes to process a feed. This is very long but in some algorithms we simulate
        // more than 10k polls
        HIGH_LOAD_THROUGHPUT = (int) (0.2 * feedReader.getThreadPoolSize() * (feedReader.getWakeUpInterval() / DateHelper.MINUTE_MS));
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        LOGGER.debug("wake up to check feeds");
        long currentWakeupTime = System.currentTimeMillis();
        int newlyScheduledFeedsCount = 0;
        int alreadyScheduledFeedCount = 0;
        StringBuilder scheduledFeedIDs = new StringBuilder();
        StringBuilder alreadyScheduledFeedIDs = new StringBuilder();

        // schedule all feeds
        for (Feed feed : getFeeds()) {

            // schedule only once
            if (lastWakeUpTime == null) {
                scheduledTasks.put(feed.getId(), threadPool.submit(new EvaluationFeedTask(feed, feedReader)));
                newlyScheduledFeedsCount++;

                if (LOGGER.isDebugEnabled()) {
                    scheduledFeedIDs.append(feed.getId()).append(",");
                }
            } else {
                // remove completed FeedTasks
                boolean completed = removeFeedTaskIfDone(feed.getId());
                if (completed) {
                    feed.freeMemory();
                }
            }
        }

        // monitoring and logging
        String wakeupInterval = "first start";
        if (lastWakeUpTime != null) {
            wakeupInterval = DateHelper.getRuntime(lastWakeUpTime, currentWakeupTime);
        }

        int success = feedResults.getCount(FeedTaskResult.SUCCESS);
        int misses = feedResults.getCount(FeedTaskResult.MISS);
        int unreachable = feedResults.getCount(FeedTaskResult.UNREACHABLE);
        int unparsable = feedResults.getCount(FeedTaskResult.UNPARSABLE);
        int slow = feedResults.getCount(FeedTaskResult.EXECUTION_TIME_WARNING);
        int errors = feedResults.getCount(FeedTaskResult.ERROR);

        String logMsg = String.format("Newly scheduled: %6d, delayed: %6d, queue size: %6d, processed: %4d, "
                + "success: %4d, misses: %4d, unreachable: %4d, unparsable: %4d, slow: %4d, errors: %4d, "
                + "blocked: %4d, wake up interval: %10s", newlyScheduledFeedsCount, alreadyScheduledFeedCount,
                scheduledTasks.size(), processedCounter, success, misses, unreachable, unparsable, slow, errors,
                blockedCounter, wakeupInterval);

        // error handling
        boolean errorOccurred = false;
        StringBuilder detectedErrors = new StringBuilder();

        if (errors > 0) {
            errorOccurred = true;
            detectedErrors.append("Too many feeds with errors. ");
        }

        if ((lastWakeUpTime != null) && ((currentWakeupTime - lastWakeUpTime) > SCHEDULER_INTERVAL_WARNING_TIME_MS)) {
            errorOccurred = true;
            detectedErrors.append("Wakeup Interval was too high. ");
        }

        // max 10% of the feeds, but at least 10 feeds are allowed to be slow
        if (slow > Math.max(10, maxSlowPercentage * processedCounter / 100)) {
            errorOccurred = true;
            detectedErrors.append("Too many feeds with long processing time. ");
        }

        // max 3% of the feeds, but at least 10 feeds are allowed to be unreachable
        if (unreachable > Math.max(10, maxUnreachablePercentage * processedCounter / 100)) {
            errorOccurred = true;
            detectedErrors.append("Too many feeds are unreachable. ");
        }

        // max 3% of the feeds, but at least 10 feeds are allowed to be unparsable
        if (unparsable > Math.max(10, maxUnparsablePercentage * processedCounter / 100)) {
            errorOccurred = true;
            detectedErrors.append("Too many feeds are unparsable. ");
        }

        if (alreadyScheduledFeedCount > 10 && (processedCounter < HIGH_LOAD_THROUGHPUT)) {
            consecutiveDelays++;
            if (consecutiveDelays >= 3) {
                errorOccurred = true;
                detectedErrors.append("Throughput too low -> Too many delayed feeds. ");
                consecutiveDelays = 0;
            }
        } else {
            consecutiveDelays = 0;
        }

        if (errorOccurred) {
            logMsg += ", detected errors: " + detectedErrors.toString();
            LOGGER.error(logMsg);
        } else {
            LOGGER.info(" " + logMsg); // whitespace required to align lines in log file.
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scheduled feed tasks for feedIDs " + scheduledFeedIDs.toString());
            if (alreadyScheduledFeedCount > 0) {
                LOGGER.debug("Could not schedule feedIDs that are already in queue: "
                        + alreadyScheduledFeedIDs.toString());
            }
        }

        // reset logging
        processedCounter = 0;
        blockedCounter = 0;
        lastWakeUpTime = currentWakeupTime;
        feedResults.clear();

        if (scheduledTasks.isEmpty()){
            // important! empty queue!!
            ((EvaluationFeedDatabase) feedReader.getFeedStore()).processBatchInsertQueue();

            LOGGER.info("All EvaluationFeedTasks done.");
            feedReader.setStopped(true);
        }
    }

    /**
     * Get all feeds from database.
     * TODO: use copy of all feeds and remove feeds that do not need to be scheduled anymore to speed up evaluation
     * 
     * @return
     */
    private Collection<Feed> getFeeds() {
        return ((EvaluationFeedDatabase) feedReader.getFeedStore()).getFeedsWithTimestamps();
    }

    /**
     * Returns whether the feed should be scheduled or not. If not, the simulated polltime has reached the
     * {@link FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND} so processing of this feed is completed.
     * 
     * @param feed
     *            The feed to check.
     * @return {@code true} if the simulated polltime has reached the
     *         {@link FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND} and {@code false} otherwise.
     */
    private Boolean needsLookup(final Feed feed) {
        long lastSimulatedPollTime = feed.getLastPollTime().getTime();
        long nextSimulatedPollTime = lastSimulatedPollTime + feed.getUpdateInterval() * DateHelper.MINUTE_MS;

        Boolean needsLookup = nextSimulatedPollTime <= FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND
                && !feed.isBlocked();

        final long now = System.currentTimeMillis();
        LOGGER.trace("Checking if feed with id: "
                + feed.getId()
                + " needs lookup!\n FeedChecks: "
                + feed.getChecks()
                + "\nLastPollTime: "
                + feed.getLastPollTime()
                + "\nSimulated now: "
                + nextSimulatedPollTime
                + "\nUpdateInterval: "
                + feed.getUpdateInterval()
                * DateHelper.MINUTE_MS
                + (feed.getLastPollTime() != null ? "\nnow - lastPollTime: " + (now - feed.getLastPollTime().getTime())
                        + "\nUpdate Interval Exceeded "
                        + (now - feed.getLastPollTime().getTime() > feed.getUpdateInterval() * DateHelper.MINUTE_MS)
                        : ""));

        if (needsLookup == true) {
            LOGGER.trace("Feed with id: " + feed.getId() + " need lookup.");
        } else {
            LOGGER.trace("Feed with id: " + feed.getId() + " needs no lookup, BENCHMARK_STOP_TIME has been reached "
                    + "(or feed has been blocked when creating the dataset).");
        }
        return needsLookup;
    }

    /**
     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
     * 
     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
     * @return <code>true</code> if task is completed and has been removed.
     */
    private boolean removeFeedTaskIfDone(final Integer feedId) {
        boolean completed = false;
        final Future<FeedTaskResult> future = scheduledTasks.get(feedId);
        if (future != null && future.isDone()) {
            scheduledTasks.remove(feedId);
            processedCounter++;
            completed = true;
            try {
                feedResults.add(future.get());
            } catch (InterruptedException e) {
                LOGGER.error("Cant get FeedTaskResult of feedId " + feedId + ". Error: " + e.getLocalizedMessage());
            } catch (ExecutionException e) {
                LOGGER.error("Cant get FeedTaskResult of feedId " + feedId + ". Error: " + e.getLocalizedMessage());
            } catch (CancellationException e) {
                LOGGER.error("Cant get FeedTaskResult of feedId " + feedId + ". Error: " + e.getLocalizedMessage());
            }
        }
        return completed;
    }


}
