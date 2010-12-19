package tud.iir.news.updates;

import tud.iir.helper.DateHelper;
import tud.iir.news.Feed;
import tud.iir.news.FeedPostStatistics;
import tud.iir.news.FeedReader;

/**
 * <p>
 * Use a combination of moving average and post rate. When a new item was found we switch to the strategy that made the
 * better guess, that is, the difference between the predicted item post time and the real item post time was smaller.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class MAVPRUpdateStrategy implements UpdateStrategy {

    /** The prediction for the next item of the post rate strategy. */
    private int prCheckIntervalPrediction;

    /** The prediction for the next item of the moving average strategy. */
    private int mavCheckIntervalPrediction;

    /** Whether or not to use the post rate. */
    private boolean usePostRate = false;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        double newEntries = feed.getTargetPercentageOfNewEntries() * (feed.getWindowSize() - 1);

        // determine winner of last prediction
        double diffPR = feed.getBenchmarkLastLookupTime() + prCheckIntervalPrediction * DateHelper.MINUTE_MS
                - fps.getTimeNewestPost();
        double diffMAV = feed.getBenchmarkLastLookupTime() + mavCheckIntervalPrediction * DateHelper.MINUTE_MS
                - fps.getTimeNewestPost();

        if (newEntries > 0) {
            if (Math.abs(diffPR) < Math.abs(diffMAV)) {
                usePostRate = true;
            } else {
                usePostRate = false;
            }
        }
        
        int mavMin = FeedReader.DEFAULT_CHECK_TIME / 2;
        int mavMax = FeedReader.DEFAULT_CHECK_TIME;
        UpdateStrategy mav = new MAVUpdateStrategy();
        mav.update(feed, fps);
        mavMin = feed.getMinCheckInterval();
        mavMax = feed.getMaxCheckInterval();
        mavCheckIntervalPrediction = feed.getCheckInterval();
        
        int prMin = FeedReader.DEFAULT_CHECK_TIME / 2;
        int prMax = FeedReader.DEFAULT_CHECK_TIME;
        UpdateStrategy pr = new PostRateUpdateStrategy();
        pr.update(feed, fps);
        prMin = feed.getMinCheckInterval();
        prMax = feed.getMaxCheckInterval();
        prCheckIntervalPrediction = feed.getMinCheckInterval();
        
        if (usePostRate) {
            pr.update(feed, fps);
            feed.setMinCheckInterval(mavMin);
            feed.setMaxCheckInterval(mavMax);
        } else {
            mav.update(feed, fps);
            feed.setMinCheckInterval(prMin);
            feed.setMaxCheckInterval(prMax);
        }

    }

    @Override
    public String getName() {
        return "mavpr";
    }

}