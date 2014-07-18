package ws.palladian.retrieval.feeds;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.FeedUpdateMode;
import ws.palladian.retrieval.feeds.updates.MavUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * Settings for the {@link FeedReader}. Use the {@link Builder} to instantiate.
 * 
 * @author pk
 */
public interface FeedReaderSettings {

    /** Maximum number of feed reading threads at the same time. */
    int DEFAULT_NUM_THREADS = 200;

    /**
     * Defines the default time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds
     * should be read.
     */
    long DEFAULT_WAKEUP_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    UpdateStrategy DEFAULT_UPDATE_STRATEGY = new MavUpdateStrategy(-1, -1, FeedUpdateMode.MIN_DELAY);

    /**
     * The number of times a feed that has never been checked successfully is put into the queue regardless of its
     * update interval.
     */
    int DEFAULT_MAX_IMMEDIATE_RETRIES = 3;

    /**
     * Max allowed ratio of unreachableCount : checks. If feed was unreachable more often, don't schedule it in the
     * future.
     */
    int DEFAULT_CHECKS_TO_UNREACHABLE_RATIO = 10;

    /**
     * Max allowed ratio of unparsableCount : checks. If feed was unparsable more often, don't schedule it in the
     * future.
     */
    int DEFAULT_CHECKS_TO_UNPARSABLE_RATIO = 10;

    /**
     * Max allowed average time to process a feed. If processing takes longer on average, don't schedule it in the
     * future.
     */
    long DEFAULT_MAXIMUM_AVERAGE_PROCESSING_TIME_MS = TimeUnit.MINUTES.toMillis(10);

    /**
     * The maximum file size (1 MB) which is accepted for each feed being checked. If this size is exceeded, the
     * download is stopped.
     */
    long DEFAULT_MAXIMUM_FEED_SIZE = SizeUnit.MEGABYTES.toBytes(1);

    /** Warn if processing of a feed takes longer than this. */
    long DEFAULT_EXECUTION_WARN_TIME = TimeUnit.MINUTES.toMillis(3);

    /**
     * @return The store which provides persistence for the feed data, not <code>null</code>.
     */
    FeedStore getStore();

    /**
     * @return The action that should be performed for each feed that is read, not <code>null</code>.
     */
    FeedProcessingAction getAction();

    /**
     * @return The update strategy which determines when to check a feed, not <code>null</code>.
     */
    UpdateStrategy getUpdateStrategy();

    /**
     * @return The maximum number of threads for retrieving feeds, greater/equal one.
     */
    int getNumThreads();

    /**
     * @return The time in milliseconds when the feed reader should wake up the check scheduler to see which feeds
     *         should be read, greater/equal 1,000.
     */
    long getWakeUpInterval();

    int getMaxImmediateRetries();

    int getChecksToUnreachableRatio();

    int getChecksToUnparsableRatio();

    long getMaximumAvgProcessingTime();

    long getMaximumFeedSize();

    long getExecutionWarnTime();

    /**
     * <p>
     * A builder for {@link FeedReaderSettings} instances.
     * 
     * @author pk
     */
    static class Builder implements Factory<FeedReaderSettings> {
        FeedStore store;
        FeedProcessingAction action;
        UpdateStrategy updateStrategy = DEFAULT_UPDATE_STRATEGY;
        int numThreads = DEFAULT_NUM_THREADS;
        long wakeUpInterval = DEFAULT_WAKEUP_INTERVAL;
        int maxImmediateRetries = DEFAULT_MAX_IMMEDIATE_RETRIES;
        int checksToUnreachableRatio = DEFAULT_CHECKS_TO_UNREACHABLE_RATIO;
        int checksToUnparsableRatio = DEFAULT_CHECKS_TO_UNPARSABLE_RATIO;
        long maximumAvgProcessingTime = DEFAULT_MAXIMUM_AVERAGE_PROCESSING_TIME_MS;
        long maximumFeedSize = DEFAULT_MAXIMUM_FEED_SIZE;
        long executionWarnTime = DEFAULT_EXECUTION_WARN_TIME;

        public Builder setStore(FeedStore store) {
            this.store = store;
            return this;
        }

        public Builder setAction(FeedProcessingAction action) {
            this.action = action;
            return this;
        }

        public Builder setUpdateStrategy(UpdateStrategy updateStrategy) {
            this.updateStrategy = updateStrategy;
            return this;
        }

        public Builder setNumThreads(int numThreads) {
            this.numThreads = numThreads;
            return this;
        }

        public Builder setWakeUpInterval(long wakeUpInterval) {
            this.wakeUpInterval = wakeUpInterval;
            return this;
        }

        public Builder setMaxImmediateRetries(int maxImmediateRetries) {
            this.maxImmediateRetries = maxImmediateRetries;
            return this;
        }

        public Builder setChecksToUnreachableRatio(int checksToUnreachableRatio) {
            this.checksToUnreachableRatio = checksToUnreachableRatio;
            return this;
        }

        public Builder setChecksToUnparsableRatio(int checksToUnparsableRatio) {
            this.checksToUnparsableRatio = checksToUnparsableRatio;
            return this;
        }

        public Builder setMaximumAvgProcessingTime(long maximumAvgProcessingTime) {
            this.maximumAvgProcessingTime = maximumAvgProcessingTime;
            return this;
        }

        public Builder setMaximumFeedSize(long maximumFeedSize) {
            this.maximumFeedSize = maximumFeedSize;
            return this;
        }

        public Builder setExecutionWarnTime(long executionWarnTime) {
            this.executionWarnTime = executionWarnTime;
            return this;
        }

        @Override
        public FeedReaderSettings create() {
            Validate.notNull(store, "store must not be null");
            Validate.notNull(action, "action must not be null");
            Validate.notNull(updateStrategy, "updateStrategy must not be null");
            Validate.isTrue(numThreads >= 1, "numThreads must be greater/equal one");
            Validate.isTrue(wakeUpInterval >= 1000, "wakeUpInterval must be greater/equal 1,000");
            Validate.isTrue(maxImmediateRetries >= 1, "maxImmediateRetries must be greater/equal one");
            Validate.isTrue(checksToUnreachableRatio >= 1, "checksToUnreachableRatio must be greater/equal one");
            Validate.isTrue(checksToUnparsableRatio >= 1, "checksToUnparsableRatio must be greater/equal one");
            Validate.isTrue(maximumAvgProcessingTime >= 1000, "maximumAvgProcessingTime must be greater/equal 1,000");
            Validate.isTrue(maximumFeedSize >= 1, "maximumFeedSize must be greater/equal one");
            Validate.isTrue(executionWarnTime >= 1, "executionWarnTime must be greater/equal one");
            return new ImmutableFeedReaderSettings(this);
        }

    }

}
