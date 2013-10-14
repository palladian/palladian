package ws.palladian.retrieval.feeds.updates;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedUpdateMode;

/**
 * <p>
 * Implementation of LRU-2 update strategy. At every poll, the new interval is set to the interval between the two
 * newest items known.
 * 
 * </p>
 * 
 * @author Sandro Reichert
 */
public class LRU2UpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(LRU2UpdateStrategy.class);

    public LRU2UpdateStrategy(int lowestInterval, int highestInterval) {
        super(lowestInterval, highestInterval);
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

        if (trainingMode) {
            LOGGER.warn("Update strategy " + getName() + " does not support an explicit training mode.");
        }

        int checkInterval = 0;

        // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
        checkInterval = DEFAULT_CHECK_TIME;

        Date lowerBoundOfInterval = feed.getLastButOneFeedEntry();
        Date upperBoundOfInterval = feed.getLastFeedEntry();

        long intervalLength = 0;
        if (lowerBoundOfInterval != null && upperBoundOfInterval != null) {
            intervalLength = upperBoundOfInterval.getTime() - lowerBoundOfInterval.getTime();
        }

        // make sure we have an interval > 0, do not set checkInterval to 0 if the last two items have the same
        // (corrected) publish date
        if (intervalLength > 0) {
            checkInterval = (int)(intervalLength / TimeUnit.MINUTES.toMillis(1));
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(checkInterval));
        }
    }

    /**
     * The algorithms name.
     */
    @Override
    public String getName() {
        return "LRU2";
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}