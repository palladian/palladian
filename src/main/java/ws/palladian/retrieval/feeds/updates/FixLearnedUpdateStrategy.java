package ws.palladian.retrieval.feeds.updates;

import java.util.Date;
import java.util.List;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Update the check intervals in fixed mode.
 * </p>
 * 
 * @author Sandro Reichert
 * 
 */
public class FixLearnedUpdateStrategy extends UpdateStrategy {

    // /**
    // * The check interval in minutes. If checkInterval = -1 the interval will be determined automatically at the first
    // * immediate check of the feed by looking in its past.
    // */
    // private int checkInterval = -1;

    /**
     * The update strategy has two different modes. 0: Mode window (default). We use the first window and calculate the
     * fix interval from it. 1: Mode Poll, additionally, we use the timestamp of the first poll to calculate the
     * interval.
     */
    private int fixLearnedMode = 0;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        int fixedMinCheckInterval = 0;

        // determine check interval at the very first poll
        if (feed.getChecks() == 0) {

            // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
            fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

            List<FeedItem> entries = feed.getItems();
            Date lowerBoundOfInterval = feed.getOldestFeedEntryCurrentWindow();
            Date upperBoundOfInterval = feed.getLastFeedEntry();
            if (fixLearnedMode == 1) {
                upperBoundOfInterval = feed.getLastPollTime();
            }

            long intervalLength = 0;
            if (upperBoundOfInterval != null && lowerBoundOfInterval != null) {
                intervalLength = upperBoundOfInterval.getTime() - lowerBoundOfInterval.getTime();
            }

            if (entries.size() > 0 && intervalLength > 0) {
                fixedMinCheckInterval = (int) (intervalLength / (entries.size() * DateHelper.MINUTE_MS));
            }
        }
        // any subsequent poll
        else {
            fixedMinCheckInterval = feed.getUpdateInterval();
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(fixedMinCheckInterval));
        }
    }

    @Override
    public String getName() {
        if (fixLearnedMode == 0) {
            return "fixLearnedW";
        } else {
            return "fixLearnedP";
        }
    }

    /**
     * The update strategy has two different modes. 0: Mode window (default). We use the first window and calculate the
     * fix interval from it. 1: Mode Poll, additionally, we use the timestamp of the first poll to calculate the
     * interval.
     * 
     * @return the fixLearnedMode
     */
    public final int getFixLearnedMode() {
        return fixLearnedMode;
    }

    /**
     * The update strategy has two different modes. 0: Mode window (default). We use the first window and calculate the
     * fix interval from it. 1: Mode Poll, additionally, we use the timestamp of the first poll to calculate the
     * interval.
     * 
     * @param fixLearnedMode the fixLearnedMode to set
     * @throws IllegalArgumentException In case the value is smaller or equal to zero.
     */
    public final void setFixLearnedMode(int fixLearnedMode) throws IllegalArgumentException {
        if (fixLearnedMode < 0 || fixLearnedMode > 1) {
            throw new IllegalArgumentException("Unsupported mode \"" + fixLearnedMode
                    + "\". Use 0 for mode window or 1 for mode poll");
        }
        this.fixLearnedMode = fixLearnedMode;
    }

}