package ws.palladian.retrieval.feeds.updates;

import java.util.Date;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Implementation of AdaptiveTTL strategy. At every poll, the interval is calculated by
 * M*(lastPollTime-newestItemTimestamp). If we dont have any item, use {@link FeedReader#DEFAULT_CHECK_TIME} instead. M
 * is 0,1 by default and can be set to any value M>0. In Web caching, M is usually set to 0,1 or 0,2.
 * </p>
 * 
 * @author Sandro Reichert
 */
public class AdaptiveTTLUpdateStrategy extends UpdateStrategy {

    /**
     * A positive, nonzero weight that is multiplied with the interval pollTime-newestItem. In Web caching, this is
     * usually set to 0,1 or 0,2.
     */
    private double weightM = 0.2D;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        int checkInterval = 0;

        // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
        checkInterval = FeedReader.DEFAULT_CHECK_TIME;

        Date lowerBoundOfInterval = feed.getLastFeedEntry();
        Date upperBoundOfInterval = feed.getLastPollTime();

        long intervalLength = 0;
        if (lowerBoundOfInterval != null && upperBoundOfInterval != null) {
            intervalLength = upperBoundOfInterval.getTime() - lowerBoundOfInterval.getTime();
        }

        // make sure we have an interval > 0, do not set checkInterval to 0 if (corrected) publish date and pollTime are
        // equal.
        if (intervalLength > 0) {
            checkInterval = (int) (weightM * intervalLength / (DateHelper.MINUTE_MS));
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(checkInterval));
        }
    }

    /**
     * The algorithms name, containing the current value of weight M.
     */
    @Override
    public String getName() {
        String weight = (getWeightM() + "");
        if (weight.length() > 4) {
            weight = weight.substring(0, 4);
        }
        return "AdaptiveTTL_" + weight;
    }

    /**
     * The update strategy has two different modes. 0: Mode window (default). We use the first window and calculate the
     * fix interval from it. 1: Mode Poll, additionally, we use the timestamp of the first poll to calculate the
     * interval.
     * 
     * @return the fixLearnedMode
     */
    public final double getWeightM() {
        return weightM;
    }

    /**
     * A positive, nonzero weight that is multiplied with the interval pollTime-newestItem. In Web caching, this is
     * usually set to 0,1 or 0,2.
     * 
     * @param weightM the weight M > 0 to set
     * @throws IllegalArgumentException In case the value is smaller or equal to zero.
     */
    public final void setWeightM(double weightM) throws IllegalArgumentException {
        if (weightM <= 0) {
            throw new IllegalArgumentException("Unsupported weight \"" + weightM
                    + "\". Value has to be larger than zero.");
        }
        this.weightM = weightM;
    }

}