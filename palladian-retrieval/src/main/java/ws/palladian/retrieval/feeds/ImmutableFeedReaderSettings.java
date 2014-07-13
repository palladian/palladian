package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

final class ImmutableFeedReaderSettings implements FeedReaderSettings {

    private FeedStore store;
    private FeedProcessingAction action;
    private UpdateStrategy updateStrategy;
    private int numThreads;
    private long wakeUpInterval;

    ImmutableFeedReaderSettings(Builder builder) {
        store = builder.store;
        action = builder.action;
        updateStrategy = builder.updateStrategy;
        numThreads = builder.numThreads;
        wakeUpInterval = builder.wakeUpInterval;
    }

    @Override
    public FeedStore getStore() {
        return store;
    }

    @Override
    public FeedProcessingAction getAction() {
        return action;
    }

    @Override
    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

    @Override
    public int getNumThreads() {
        return numThreads;
    }

    @Override
    public long getWakeUpInterval() {
        return wakeUpInterval;
    }

}