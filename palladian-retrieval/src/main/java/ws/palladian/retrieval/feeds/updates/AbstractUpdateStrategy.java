package ws.palladian.retrieval.feeds.updates;

/**
 * <p>
 * Update strategies determine when feeds are updated.
 * </p>
 * 
 * @author David Urbansky
 */
public abstract class AbstractUpdateStrategy implements UpdateStrategy {

    /**
     * What is the lowest allowed interval (in minutes) in which feeds should be read (independent of checking mode). -1
     * = no lowest
     * interval. For example, 10 means that the feed reading frequency is maximum every 10 minutes, so it can never be
     * read every 9 minutes or lower.
     */
    private final int lowestInterval;

    /**
     * What is the highest allowed interval in which feeds should be read (independent of checking mode). -1 = no
     * highest interval. For example, 100 means that the feed reading frequency is minimum every 100 minutes, so it can
     * never be read every 101 minutes or higher (at least every 100 minutes).
     */
    private final int highestInterval;

    /**
     * 
     * @param lowestInterval The lowestCheckInterval in minutes to set, -1 means no lowest interval.
     * @param highestInterval The highestCheckInterval to set, -1 means no highest interval.
     */
    public AbstractUpdateStrategy(int lowestInterval, int highestInterval) {
        this.lowestInterval = lowestInterval;
        this.highestInterval = highestInterval;
    }

    /**
     * @return The lowest allowed interval in which feeds should be read (independent of checking mode). -1 = no lowest
     *         interval.
     */
    public int getLowestInterval() {
        return lowestInterval;
    }

    /**
     * @return The highest allowed interval in which feeds should be read (independent of checking mode). -1 = no
     *         highest interval.
     */
    public int getHighestInterval() {
        return highestInterval;
    }

    /**
     * <p>
     * Check whether the computed highest check interval complies with the allowed highestCheckInterval.
     * </p>
     * 
     * @param updateInterval The computed highestCheckInterval.
     * @return The computed interval if it is in the limit.
     */
    protected int getAllowedInterval(int updateInterval) {
        int allowedInterval = updateInterval;
        if (getHighestInterval() != -1 && getHighestInterval() < updateInterval) {
            allowedInterval = getHighestInterval();
        }
        if (getLowestInterval() != -1 && getLowestInterval() > updateInterval) {
            allowedInterval = getLowestInterval();
        }
        return allowedInterval;
    }

}
