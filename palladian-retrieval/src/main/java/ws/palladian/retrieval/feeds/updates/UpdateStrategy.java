package ws.palladian.retrieval.feeds.updates;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;

/**
 * <p>
 * Update strategies determine when feeds are updated.
 * </p>
 * 
 * @author David Urbansky
 */
public abstract class UpdateStrategy {

    /**
     * What is the lowest allowed interval (in minutes) in which feeds should be read (independent of checking mode). -1
     * = no lowest
     * interval. For example, 10 means that the feed reading frequency is maximum every 10 minutes, so it can never be
     * read every 9 minutes or lower.
     */
    private int lowestUpdateInterval = -1;

    /**
     * What is the highest allowed interval in which feeds should be read (independent of checking mode). -1 = no
     * highest interval. For example, 100 means that the feed reading frequency is minimum every 100 minutes, so it can
     * never be read every 101 minutes or higher (at least every 100 minutes).
     */
    private int highestUpdateInterval = -1;

    /**
     * <p>
     * Update the minimal and maximal update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode If the {@link UpdateStrategy} distinguishes between training and normal mode, set to
     *            <code>true</code> to use training mode. For normal mode, or if you don't know, set
     *            to <code>false</code>.
     */
    abstract public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode);

    /** Get the name of the strategy. */
    abstract public String getName();

    /** Does the strategy has an explicit training mode to learn a model? */
    abstract public boolean hasExplicitTrainingMode();

    /**
     * <p>
     * What is the lowest allowed interval (in minutes) in which feeds should be read (independent of checking mode). -1
     * = no lowest interval. For example, 10 means that the feed reading frequency is maximum every 10 minutes, so it
     * can never be read every 9 minutes or lower.
     * </p>
     * 
     * @param lowestCheckInterval The lowestCheckInterval to set.
     */
    public void setLowestUpdateInterval(int lowestUpdateInterval) {
        this.lowestUpdateInterval = lowestUpdateInterval;
    }

    /**
     * @return The lowest allowed interval in which feeds should be read (independent of checking mode). -1 = no lowest
     *         interval.
     */
    public int getLowestUpdateInterval() {
        return lowestUpdateInterval;
    }

    /**
     * <p>
     * What is the highest allowed interval in which feeds should be read (independent of checking mode). -1 = no
     * highest interval. For example, 100 means that the feed reading frequency is minimum every 100 minutes, so it can
     * never be read every 101 minutes or higher (at least every 100 minutes).
     * </p>
     * 
     * @param highestCheckInterval The highestCheckInterval to set.
     */
    public void setHighestUpdateInterval(int highestUpdateInterval) {
        this.highestUpdateInterval = highestUpdateInterval;
    }

    /**
     * @return The highest allowed interval in which feeds should be read (independent of checking mode). -1 = no
     *         highest interval.
     */
    public int getHighestUpdateInterval() {
        return highestUpdateInterval;
    }

    /**
     * <p>Check whether the computed highest check interval complies with the allowed highestCheckInterval.</p>
     * 
     * @param updateInterval The computed highestCheckInterval.
     * @return The computed interval if it is in the limit.
     */
    int getAllowedUpdateInterval(int updateInterval) {
        int allowedInterval = updateInterval;
        if (getHighestUpdateInterval() != -1 && getHighestUpdateInterval() < updateInterval) {
            allowedInterval = getHighestUpdateInterval();
        }
        if (getLowestUpdateInterval() != -1 && getLowestUpdateInterval() > updateInterval) {
            allowedInterval = getLowestUpdateInterval();
        }
        return allowedInterval;
    }
}
