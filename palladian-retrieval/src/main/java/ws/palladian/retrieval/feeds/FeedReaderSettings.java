package ws.palladian.retrieval.feeds;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Factory;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.MavUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * Settings for the {@link FeedReader}. Use the {@link Builder} to instantiate.
 * 
 * @author pk
 */
public interface FeedReaderSettings {

    static class Builder implements Factory<FeedReaderSettings> {
        FeedStore store;
        FeedProcessingAction action;
        UpdateStrategy updateStrategy = DEFAULT_UPDATE_STRATEGY;
        int numThreads = DEFAULT_NUM_THREADS;
        long wakeUpInterval = DEFAULT_WAKEUP_INTERVAL;

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

        @Override
        public FeedReaderSettings create() {
            Validate.notNull(store, "store must not be null");
            Validate.notNull(action, "action must not be null");
            Validate.notNull(updateStrategy, "updateStrategy must not be null");
            Validate.isTrue(numThreads >= 1, "numThreads must be greater/equal one");
            Validate.isTrue(wakeUpInterval >= 1000, "wakeUpInterval must be greater/equal 1,000");
            return new ImmutableFeedReaderSettings(this);
        }

    }

    /** Maximum number of feed reading threads at the same time. */
    int DEFAULT_NUM_THREADS = 200;

    /**
     * Defines the default time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds
     * should be read.
     */
    long DEFAULT_WAKEUP_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    UpdateStrategy DEFAULT_UPDATE_STRATEGY = new MavUpdateStrategy(-1, -1);

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

}
