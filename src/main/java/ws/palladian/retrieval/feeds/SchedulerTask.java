package ws.palladian.retrieval.feeds;

import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections15.bag.HashBag;
import org.apache.log4j.Logger;

import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.SendMail;
import ws.palladian.helper.date.DateHelper;

/**
 * A scheduler task handles the distribution of feeds to worker threads that
 * read these feeds.
 * 
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
class SchedulerTask extends TimerTask {

    /**
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     */
    private static final Logger LOGGER = Logger.getLogger(SchedulerTask.class);

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
     * max allowed ratio of unreachableCount : checks, if feed was unreachable more often, don't schedule it in the
     * future.
     */
    private static final int CHECKS_TO_UNREACHABLE_RATIO = 10;

    /**
     * Max allowed average time to process a feed. If processing takes longer on average, don't schedule it in the
     * future.
     */
    private static final long MAXIMUM_PROCESSING_TIME_MS = 10 * DateHelper.MINUTE_MS;

    /**
     * If wake up interval exceeds this time, do some warning.
     */
    private static final long SCHEDULER_INTERVAL_WARNING_TIME_MS = 2 * DateHelper.MINUTE_MS;

    /**
     * Count the number of processed feeds per scheduler iteration.
     */
    private int processedCounter = 0;

    private Long lastWakeUpTime = null;

    private HashBag<FeedTaskResult> feedResults = new HashBag<FeedTaskResult>();

    /** If true, send error reports via email */
    private boolean errorMailNotification = false;

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

    /**
     * Creates a new {@code SchedulerTask} for a feed reader.
     * 
     * @param feedReader
     *            The feed reader containing settings and providing the
     *            collection of feeds to check.
     * @param errorMailNotification If set to true, error messages will be sent via mail.
     */
    public SchedulerTask(final FeedReader feedReader, boolean errorMailNotification) {
        this(feedReader);
        this.errorMailNotification = errorMailNotification;
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
        StringBuffer scheduledFeedIDs = new StringBuffer();
        StringBuffer alreadyScheduledFeedIDs = new StringBuffer();

        // schedule all feeds
        for (Feed feed : feedReader.getFeeds()) {

            // remove completed FeedTasks
            removeFeedTaskIfDone(feed.getId());
            if (needsLookup(feed)) {
                if (scheduledTasks.containsKey(feed.getId())) {
                    alreadyScheduledFeedCount++;

                    if (LOGGER.isDebugEnabled()) {
                        alreadyScheduledFeedIDs.append(feed.getId()).append(",");
                    }
                } else {
                    scheduledTasks.put(feed.getId(), threadPool.submit(new FeedTask(feed, feedReader)));
                    newlyScheduledFeedsCount++;

                    if (LOGGER.isDebugEnabled()) {
                        scheduledFeedIDs.append(feed.getId()).append(",");
                    }
                }
            }
        }

        // logging
        String wakeupInterval = "first start";
        if (lastWakeUpTime != null) {
            wakeupInterval = DateHelper.getRuntime(lastWakeUpTime, currentWakeupTime);
        }

        int success = feedResults.getCount(FeedTaskResult.SUCCESS);
        int misses = feedResults.getCount(FeedTaskResult.MISS);
        int unreachable = feedResults.getCount(FeedTaskResult.UNREACHABLE);
        int errors = feedResults.getCount(FeedTaskResult.ERROR);

        String logMsg = String.format("Scheduled: %6d, delayed: %6d, queue size: %6d, processed: %4d"
                + ", success: %4d, misses: %4d, unreachable: %4d, errors: %4d, wake up interval: %10s",
                newlyScheduledFeedsCount, alreadyScheduledFeedCount, scheduledTasks.size(), processedCounter, success,
                misses, unreachable, errors, wakeupInterval);

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

        // FIXME: needs to be fine tuned?
        if (alreadyScheduledFeedCount > 10 && processedCounter < 1000) {
            errorOccurred = true;
            detectedErrors.append("Throughput too low. ");
        }

        if (errorOccurred) {
            logMsg += ", detected errors: " + detectedErrors.toString();
            LOGGER.error(logMsg);

            if(errorMailNotification){
                String hostname = ProcessHelper.runCommand("hostname");
                String recipient = "sandro.reichert@tu-dresden.de"; // FIXME: multiple recipients
                                                                    // philipp.katz@tu-dresden.de,
                                                                    // david.urbansky@tu-dresden.de,
                                                                    // klemens.muthmann@tu-dresden.de";
                String subject = "FeedReader " + hostname + " notification "
                        + DateHelper.getCurrentDatetime("yyyy-MM-dd HH:mm:ss");

                SendMail mailer = new SendMail();
                mailer.send("notification@palladian.ws", recipient, subject, logMsg);
            }
        } else {
            LOGGER.info(logMsg);
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
        lastWakeUpTime = currentWakeupTime;
        feedResults.clear();
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
                + feed.getUpdateInterval()
                * DateHelper.MINUTE_MS
                + (feed.getLastPollTime() != null ? "\nnow - lastPollTime: " + (now - feed.getLastPollTime().getTime())
                        + "\nUpdate Interval Exceeded "
                        + (now - feed.getLastPollTime().getTime() > feed.getUpdateInterval() * DateHelper.MINUTE_MS)
                        : ""));

        // check whether the feed needs to be blocked
        if (!feed.isBlocked()) {
            if (feed.getAverageProcessingTime() >= MAXIMUM_PROCESSING_TIME_MS) {
                LOGGER.fatal("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") takes on average too long to process and is therefore blocked (never scheduled again)!"
                        + " Average processing time was " + feed.getAverageProcessingTime() + " milliseconds.");
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
            }

            if (feed.getChecks() < feed.getUnreachableCount() / CHECKS_TO_UNREACHABLE_RATIO) {
                LOGGER.fatal("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + "has been unreachable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unreachableCount = " + feed.getUnreachableCount());
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
            }
        }

        boolean isBlocked = feed.isBlocked();
        boolean immediateRetry = (feed.getChecks() == 0) && (feed.getUnreachableCount() <= MAX_IMMEDIATE_RETRIES);
        boolean notYetPolled = (feed.getLastPollTime() == null);
        boolean regularSchedule = !notYetPolled
                && (now - feed.getLastPollTime().getTime() > feed.getUpdateInterval() * DateHelper.MINUTE_MS);

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
            processedCounter++;
            try {
                feedResults.add(future.get());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {

    }

}
