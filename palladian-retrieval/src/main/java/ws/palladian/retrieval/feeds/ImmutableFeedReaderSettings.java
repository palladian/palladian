package ws.palladian.retrieval.feeds;

import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

final class ImmutableFeedReaderSettings implements FeedReaderSettings {

    private FeedStore store;
    private FeedProcessingAction action;
    private UpdateStrategy updateStrategy;
    private int numThreads;
    private long wakeUpInterval;
    private int maxImmediateRetries;
    private int checksToUnreachableRatio;
    private int checksToUnparsableRatio;
    private long maximumAvgProcessingTime;
    private long maximumFeedSize;
    private long executionWarnTime;

    ImmutableFeedReaderSettings(Builder builder) {
        store = builder.store;
        action = builder.action;
        updateStrategy = builder.updateStrategy;
        numThreads = builder.numThreads;
        wakeUpInterval = builder.wakeUpInterval;
        maxImmediateRetries = builder.maxImmediateRetries;
        checksToUnreachableRatio = builder.checksToUnreachableRatio;
        checksToUnparsableRatio = builder.checksToUnparsableRatio;
        maximumAvgProcessingTime = builder.maximumAvgProcessingTime;
        maximumFeedSize = builder.maximumFeedSize;
        executionWarnTime = builder.executionWarnTime;
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

    @Override
    public int getMaxImmediateRetries() {
        return maxImmediateRetries;
    }

    @Override
    public int getChecksToUnreachableRatio() {
        return checksToUnreachableRatio;
    }

    @Override
    public int getChecksToUnparsableRatio() {
        return checksToUnparsableRatio;
    }

    @Override
    public long getMaximumAvgProcessingTime() {
        return maximumAvgProcessingTime;
    }

    @Override
    public long getMaximumFeedSize() {
        return maximumFeedSize;
    }

    @Override
    public long getExecutionWarnTime() {
        return executionWarnTime;
    }

}
