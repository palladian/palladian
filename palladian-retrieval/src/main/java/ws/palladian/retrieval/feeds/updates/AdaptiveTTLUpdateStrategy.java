package ws.palladian.retrieval.feeds.updates;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.FeedUpdateMode;

/**
 * <p>
 * Implementation of AdaptiveTTL strategy. At every poll, the interval is calculated by
 * M*(lastPollTime-newestItemTimestamp). If we don't have any item, use {@link FeedReader#DEFAULT_CHECK_TIME} instead. M
 * is 0,1 by default and can be set to any value M>0. In Web caching, M is usually set to 0.1 or 0.2.
 * </p>
 * 
 * @author Sandro Reichert
 */
public class AdaptiveTTLUpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdaptiveTTLUpdateStrategy.class);

    /**
     * A positive, nonzero weight that is multiplied with the interval pollTime-newestItem. In Web caching, this is
     * usually set to 0.1 or 0.2.
     */
    private final double weightM;

    /**
     * @param weightM A positive, nonzero weight that is multiplied with the interval pollTime-newestItem. In Web
     *            caching, this is usually set to 0.1 or 0.2.
     */
    public AdaptiveTTLUpdateStrategy(int lowestInterval, int highestInterval, double weightM) {
        super(lowestInterval, highestInterval);
        if (weightM <= 0) {
            throw new IllegalArgumentException("Unsupported weight \"" + weightM
                    + "\". Value has to be larger than zero.");
        }
        this.weightM = weightM;
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

        // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
        int checkInterval = DEFAULT_CHECK_TIME;

        Date lowerBoundOfInterval = feed.getLastFeedEntry();
        Date upperBoundOfInterval = feed.getLastPollTime();

        long intervalLength = 0;
        if (lowerBoundOfInterval != null && upperBoundOfInterval != null) {
            intervalLength = upperBoundOfInterval.getTime() - lowerBoundOfInterval.getTime();
        }

        // make sure we have an interval > 0, do not set checkInterval to 0 if (corrected) publish date and pollTime are
        // equal.
        if (intervalLength > 0) {
            checkInterval = (int)(weightM * intervalLength / TimeUnit.MINUTES.toMillis(1));
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(checkInterval));
        }
    }

    /**
     * The strategy's name, containing the current value of weight M.
     */
    @Override
    public String getName() {
        String weight = weightM + "";
        if (weight.length() > 4) {
            weight = weight.substring(0, 4);
        }
        return "AdaptiveTTL_" + weight;
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}