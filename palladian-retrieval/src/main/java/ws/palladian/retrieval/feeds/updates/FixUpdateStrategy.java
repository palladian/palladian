package ws.palladian.retrieval.feeds.updates;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedUpdateMode;

/**
 * <p>
 * Update the check intervals in fixed mode.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 */
public class FixUpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private final static Logger LOGGER = LoggerFactory.getLogger(FixUpdateStrategy.class);

    /**
     * The check interval in minutes.
     */
    private final int checkInterval;
    
    /**
     * Create strategy and set a fixed check interval in minutes larger than zero.
     * 
     * @param checkInterval Fixed check interval in minutes. Value has to be larger than zero.
     * @throws IllegalArgumentException In case the value is smaller or equal to zero.
     */
    public FixUpdateStrategy(int lowestInterval, int highestInterval, int checkInterval) {
        super(lowestInterval, highestInterval);
        if (checkInterval <= 0) {
            throw new IllegalArgumentException("A fixed check interval smaller or equal to zero is not supported.");
        }
        this.checkInterval = checkInterval;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode Ignored parameter. The strategy does not support an explicit training mode.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        // default value
        int fixedMinCheckInterval = DEFAULT_CHECK_TIME;

        // set fix interval, independent of feed, e.g. fix60 (fix1h)
        if (checkInterval > 0) {
            fixedMinCheckInterval = checkInterval;
        } else {
            LOGGER.error("Fix interval has not been initialized, using defaul value " + DEFAULT_CHECK_TIME);
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(fixedMinCheckInterval));
        }
    }

    @Override
    public String getName() {
        return "fix" + checkInterval;
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}