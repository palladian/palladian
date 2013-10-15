package ws.palladian.retrieval.feeds;

import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.MavUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * <p>
 * The FeedReader reads news from feeds in a database. It learns when it is necessary to check the feed again for news.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class FeedReader {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedReader.class);

    /** Maximum number of feed reading threads at the same time. */
    public static final int DEFAULT_NUM_THREADS = 200;

    /**
     * Defines the default time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds
     * should be read.
     */
    public static final long DEFAULT_WAKEUP_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    private final int numThreads;

    /** The action that should be performed for each feed that is read. */
    private final FeedProcessingAction feedProcessingAction;

    /** The chosen check approach */
    private final UpdateStrategy updateStrategy;

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed must be updated whenever the method {@link Feed#getLastPollTime()} return value is further away in the past
     * then its {@link Feed#getMaxCheckInterval()} or {@link Feed#getUpdateInterval()} returns. Which one to use depends
     * on the update strategy.
     */
    private final Timer checkScheduler;

    /** The {@link FeedStore}. */
    private final FeedStore feedStore;

    /**
     * Defines the time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds should
     * be read.
     */
    private final long wakeUpInterval;

    /**
     * <p>
     * Create a new feed reader.
     * </p>
     * 
     * @param feedStore The store which provides persistence for the feeds, not <code>null</code>.
     * @param processingAction The action to perform when a feed was read, not <code>null</code>.
     * @param updateStrategy The strategy for determining the update behavior, not <code>null</code>.
     * @param numThreads The number of threads for retrieving feeds, greater/equal one.
     * @param wakeUpInterval The time in milliseconds when the feed reader should wake up the check scheduler to see
     *            which feeds should be read, greater/equal 1,000.
     */
    public FeedReader(FeedStore feedStore, FeedProcessingAction processingAction, UpdateStrategy updateStrategy,
            int numThreads, long wakeUpInterval) {
        Validate.notNull(feedStore, "feedStore must not be null");
        Validate.notNull(processingAction, "processingAction must not be null");
        Validate.notNull(updateStrategy, "updateStrategy must not be null");
        Validate.isTrue(numThreads >= 1, "numThreads must be greater/equal one");
        Validate.isTrue(wakeUpInterval >= 1000, "wakeUpInterval must be greater/equal 1,000");
        checkScheduler = new Timer();
        this.feedStore = feedStore;
        this.feedProcessingAction = processingAction;
        this.updateStrategy = updateStrategy;
        this.numThreads = numThreads;
        this.wakeUpInterval = wakeUpInterval;
    }

    /**
     * <p>
     * Create a new feed reader using the {@link MavUpdateStrategy}, {@value #DEFAULT_NUM_THREADS} threads for reading
     * and a wake up interval of {@value #DEFAULT_WAKEUP_INTERVAL}.
     * </p>
     * 
     * @param feedStore The store which provides persistence for the feeds, not <code>null</code>.
     * @param processingAction The action to perform when a feed was read, not <code>null</code>.
     */
    public FeedReader(FeedStore feedStore, FeedProcessingAction processingAction) {
        this(feedStore, processingAction, new MavUpdateStrategy(-1, -1), DEFAULT_NUM_THREADS, DEFAULT_WAKEUP_INTERVAL);
    }

    /**
     * <p>
     * Create a new feed reader using the {@link MavUpdateStrategy}, {@value #DEFAULT_NUM_THREADS} threads for reading
     * and a wake up interval of {@value #DEFAULT_WAKEUP_INTERVAL}.
     * </p>
     * 
     * @param feedStore The store which provides persistence for the feeds, not <code>null</code>.
     * @param processingAction The action to perform when a feed was read, not <code>null</code>.
     * @param updateStrategy The strategy for determining the update behavior, not <code>null</code>.
     */
    public FeedReader(FeedStore feedStore, FeedProcessingAction processingAction, UpdateStrategy updateStrategy) {
        this(feedStore, processingAction, updateStrategy, DEFAULT_NUM_THREADS, DEFAULT_WAKEUP_INTERVAL);
    }

    /**
     * <p>
     * Start reading.
     * </p>
     */
    public void start() {
        SchedulerTask schedulerTask = new SchedulerTask(numThreads, feedStore, feedProcessingAction, updateStrategy);
        checkScheduler.schedule(schedulerTask, 0, wakeUpInterval);

        LOGGER.debug(
                "Scheduled task, wake up every {} milliseconds to check all feeds whether they need to be read or not",
                wakeUpInterval);
    }

    /**
     * <p>
     * Stop reading.
     * </p>
     */
    public void stop() {
        checkScheduler.cancel();
        LOGGER.info("Cancelled all scheduled readings, total size downloaded ({}): {} MB", updateStrategy,
                HttpRetriever.getTraffic(SizeUnit.MEGABYTES));
    }

}
