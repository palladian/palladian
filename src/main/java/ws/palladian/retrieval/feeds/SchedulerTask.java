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

import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.ProcessHelper;
import ws.palladian.helper.SendMail;
import ws.palladian.helper.date.DateHelper;

/**
 * <p>A scheduler task handles the distribution of feeds to worker threads that read these feeds.
 * It has an integrated monitoring component that logs and sends error notifications via email.</p>
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
    private static final long MAXIMUM_AVERAGE_PROCESSING_TIME_MS = 10 * DateHelper.MINUTE_MS;


    // //////////// Monitoring constants \\\\\\\\\\\\\\\\\

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

    /** By default, do not send error reports via email */
    private static final boolean ERROR_MAIL_NOTIFICATION_DEFAULT = false;

    /** If true, send error reports via email */
    private static boolean errorMailNotification;

    /** The receipient of email notifications. */
    private static String emailRecipient;

    /** The receipient of email notifications. */
    private static final String EMAIL_RECEIPIENT_DEFAULT = "user@example.org";

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
    public SchedulerTask(final FeedReader feedReader) {
        super();
        threadPool = Executors.newFixedThreadPool(feedReader.getThreadPoolSize());
        this.feedReader = feedReader;
        scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();

        // configure monitoring and logging

        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        errorMailNotification = ERROR_MAIL_NOTIFICATION_DEFAULT;
        if (config != null) {
            errorMailNotification = config.getBoolean("schedulerTask.errorMailNotification",
                    ERROR_MAIL_NOTIFICATION_DEFAULT);
            emailRecipient = config.getString("schedulerTask.emailRecipient", EMAIL_RECEIPIENT_DEFAULT);
            maxSlowPercentage = config.getInt("schedulerTask.maxSlowPercentage", MAX_SLOW_PERCENTAGE_DEFAULT);
            maxUnreachablePercentage = config.getInt("schedulerTask.maxUnreachablePercentage",
                    MAX_UNREACHABLE_PERCENTAGE_DEFAULT);
            maxUnparsablePercentage = config.getInt("schedulerTask.maxUnparsablePercentage",
                    MAX_UNPARSABLE_PERCENTAGE_DEFAULT);

        }

        // on average, one thread should process at least 3 feeds per minute
        HIGH_LOAD_THROUGHPUT = (int) (3 * feedReader.getThreadPoolSize() * (feedReader.getWakeUpInterval() / DateHelper.MINUTE_MS));
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
                + "blocked: %4d, wake up interval: %10s",
                newlyScheduledFeedsCount, alreadyScheduledFeedCount, scheduledTasks.size(), processedCounter, 
                success, misses, unreachable, unparsable, slow, errors, blockedCounter, wakeupInterval);

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

        // FIXME: needs to be fine tuned?
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

            if(errorMailNotification){
                String hostname = ProcessHelper.runCommand("hostname");
                String recipient = emailRecipient; // FIXME: multiple recipients
                                                   // philipp.katz@tu-dresden.de,
                                                   // david.urbansky@tu-dresden.de,
                                                   // klemens.muthmann@tu-dresden.de";
                String subject = "FeedReader " + hostname + " notification "
                        + DateHelper.getCurrentDatetime("yyyy-MM-dd HH:mm:ss");

                SendMail mailer = new SendMail();
                mailer.send("notification@palladian.ws", recipient, subject, logMsg);
            }
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
                + feed.getUpdateInterval()
                * DateHelper.MINUTE_MS
                + (feed.getLastPollTime() != null ? "\nnow - lastPollTime: " + (now - feed.getLastPollTime().getTime())
                        + "\nUpdate Interval Exceeded "
                        + (now - feed.getLastPollTime().getTime() > feed.getUpdateInterval() * DateHelper.MINUTE_MS)
                        : ""));

        // check whether the feed needs to be blocked
        if (!feed.isBlocked()) {
            if (feed.getChecks() + feed.getUnreachableCount() + feed.getUnparsableCount() >= 3
                    && feed.getAverageProcessingTime() >= MAXIMUM_AVERAGE_PROCESSING_TIME_MS) {
                LOGGER.fatal("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") takes on average too long to process and is therefore blocked (never scheduled again)!"
                        + " Average processing time was " + feed.getAverageProcessingTime() + " milliseconds.");
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
                blockedCounter++;
            } else if (feed.getChecks() < feed.getUnreachableCount() / CHECKS_TO_UNREACHABLE_RATIO) {
                LOGGER.fatal("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unreachable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unreachableCount = " + feed.getUnreachableCount());
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
                blockedCounter++;
            } else if (feed.getChecks() < feed.getUnparsableCount() / CHECKS_TO_UNPARSABLE_RATIO) {
                LOGGER.fatal("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") has been unparsable too often and is therefore blocked (never scheduled again)!"
                        + " checks = " + feed.getChecks() + ", unparsableCount = " + feed.getUnparsableCount());
                feed.setBlocked(true);
                feedReader.updateFeed(feed);
                blockedCounter++;
            }
        }

        boolean isBlocked = feed.isBlocked();
        boolean immediateRetry = (feed.getChecks() == 0) && (feed.getUnreachableCount() <= MAX_IMMEDIATE_RETRIES)
                && (feed.getUnparsableCount() <= MAX_IMMEDIATE_RETRIES);
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
