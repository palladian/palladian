package ws.palladian.retrieval.feeds.updates;

import java.util.Date;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Implementation of LRU-2 update strategy. At every poll, the new interval is set to the interval between the two
 * newest items known.
 * 
 * </p>
 * 
 * @author Sandro Reichert
 */
public class LRU2UpdateStrategy extends UpdateStrategy {


    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        int checkInterval = 0;

        // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
        checkInterval = FeedReader.DEFAULT_CHECK_TIME;

        Date lowerBoundOfInterval = feed.getLastButOneFeedEntry();
        Date upperBoundOfInterval = feed.getLastFeedEntry();

        long intervalLength = 0;
        if (lowerBoundOfInterval != null && upperBoundOfInterval != null) {
            intervalLength = upperBoundOfInterval.getTime() - lowerBoundOfInterval.getTime();
        }

        // make sure we have an interval > 0, do not set checkInterval to 0 if the last two items have the same
        // (corrected) publish date
        if (intervalLength > 0) {
            checkInterval = (int) (intervalLength / (DateHelper.MINUTE_MS));
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(checkInterval));
        }
    }

    /**
     * The algorithms name.
     */
    @Override
    public String getName() {
        return "LRU2";
    }

}