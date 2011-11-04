package ws.palladian.retrieval.feeds.updates;

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
 * @author David Urbansky
 * 
 */
public class FixUpdateStrategy extends UpdateStrategy {

    /**
     * The check interval in minutes. If checkInterval = -1 the interval will be determined automatically at the first
     * immediate check of the feed by looking in its past.
     */
    private int checkInterval = -1;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        int fixedMinCheckInterval = 0;
        // int fixedMaxCheckInterval = 0;

        // determine check interval
        // if fixed learned (checkInterval = -1, we only set the interval at the very first poll)
        if (getCheckInterval() == -1) {

            // if fixed learned (checkInterval = -1, we only set the interval at the very first poll)
            if (feed.getChecks() == 0) {

                // the checkInterval for the feed must be determined now
                fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME;
                // int fixedMaxCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

                List<FeedItem> entries = feed.getItems();
                if (entries.size() > 1) {
                    // use average distance between pub dates and total difference between first and last entry
                    double avgPostGap = fps.getAveragePostGap();
                    if (avgPostGap != 0D) {
                        fixedMinCheckInterval = (int) (avgPostGap / DateHelper.MINUTE_MS);
                        // fixedMaxCheckInterval = entries.size() * fixedMinCheckInterval;
                    }
                }
            }
            // any subsequent poll
            else {
                fixedMinCheckInterval = feed.getUpdateInterval();
            }
        }
        // set fix interval, independent of feed, e.g. fix60 (fix1h)
        else {
            fixedMinCheckInterval = getCheckInterval();
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(fixedMinCheckInterval));
            // } else {
            // feed.setUpdateInterval(getAllowedUpdateInterval(fixedMaxCheckInterval));
        }
    }

    @Override
    public String getName() {
        if (getCheckInterval() == -1) {
            return "fixLearned";
        } else {
            return "fix" + getCheckInterval();
        }
    }

    /**
     * Set a fixed check interval in minutes. This is only effective if the checkType is set to {@link CHECK_FIXED}.
     * 
     * @param checkInterval Fixed check interval in minutes.
     */
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

}