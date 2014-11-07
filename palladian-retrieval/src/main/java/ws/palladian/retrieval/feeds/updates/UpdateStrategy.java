package ws.palladian.retrieval.feeds.updates;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;

public interface UpdateStrategy {

    /** If a fixed checkInterval could not be learned, this one is taken (in minutes). */
    int DEFAULT_CHECK_TIME = 60;

    /**
     * <p>
     * Update the minimal and maximal update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode If the {@link UpdateStrategy} distinguishes between training and normal mode, set to
     *            <code>true</code> to use training mode. For normal mode, or if you don't know, set to
     *            <code>false</code>. See also {@link #hasExplicitTrainingMode()}.
     */
    void update(Feed feed, FeedPostStatistics fps, boolean trainingMode);

    /**
     * @return The readable name of the strategy.
     */
    String getName();

    /**
     * @return <code>true</code> in case the strategy has an explicit training mode to learn a model, <code>false</code>
     *         otherwise.
     */
    boolean hasExplicitTrainingMode();

}
