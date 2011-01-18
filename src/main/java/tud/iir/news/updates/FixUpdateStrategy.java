package tud.iir.news.updates;

import java.util.List;

import tud.iir.helper.DateHelper;
import tud.iir.news.Feed;
import tud.iir.news.FeedClassifier;
import tud.iir.news.FeedItem;
import tud.iir.news.FeedPostStatistics;
import tud.iir.news.FeedReader;

/**
 * <p>
 * Update the check intervals in fixed mode.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class FixUpdateStrategy implements UpdateStrategy {

    /**
     * The check interval in minutes. If checkInterval = -1 the interval will be determined automatically at the first
     * immediate check of the feed by looking in its past.
     */
    private int checkInterval = -1;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        // if fixed learned (checkInterval = -1, we only set the interval at the very first poll)
        if (feed.getChecks() == 0 || getCheckInterval() != -1) {

            List<FeedItem> entries = feed.getItems();

            // the checkInterval for the feed must be determined now
            int fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME / 2;
            int fixedMaxCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

            if (entries.size() > 1) {
                // use average distance between pub dates and total difference
                // between first and last entry
                fixedMinCheckInterval = (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
                fixedMaxCheckInterval = entries.size() * fixedMinCheckInterval;

                if (feed.getActivityPattern() == FeedClassifier.CLASS_DEAD) {
                    fixedMinCheckInterval = 10 * 800 + (int) (Math.random() * 200);
                    fixedMaxCheckInterval = 10 * 1440 + (int) (Math.random() * 600);
                } else if (feed.getActivityPattern() == FeedClassifier.CLASS_CHUNKED) {

                    // for chunked entries the median post gap is likely to be zero so we set it to the time to the last
                    // post
                    fixedMinCheckInterval = (int) (fps.getTimeNewestPost() / DateHelper.MINUTE_MS);
                    fixedMaxCheckInterval = fixedMinCheckInterval;

                } else if (feed.getActivityPattern() == FeedClassifier.CLASS_ON_THE_FLY) {

                    fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME / 2;
                    fixedMaxCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

                }

            } else {
                fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME / 2;
                fixedMaxCheckInterval = FeedReader.DEFAULT_CHECK_TIME;
            }

            feed.setMinCheckInterval(fixedMinCheckInterval);
            feed.setMaxCheckInterval(fixedMaxCheckInterval);

        }

    }

    @Override
    public String getName() {
        return "fix" + getCheckInterval();
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