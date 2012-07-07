package ws.palladian.retrieval.feeds.updates;

import java.util.concurrent.TimeUnit;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Use a combination of moving average and post rate. When a new item was found we switch to the strategy that made the
 * better guess, that is, the difference between the predicted item post time and the real item post time was smaller.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class MavPrUpdateStrategy extends UpdateStrategy {

    /** The prediction for the next item of the post rate strategy. */
    private int prCheckIntervalPrediction;

    /** The prediction for the next item of the moving average strategy. */
    private int mavCheckIntervalPrediction;

    /** Whether or not to use the post rate. */
    private boolean usePostRate = false;

    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        // determine winner of last prediction
        double diffPR = feed.getBenchmarkLastLookupTime() + prCheckIntervalPrediction * TimeUnit.MINUTES.toMillis(1)
        - fps.getTimeNewestPost();
        double diffMAV = feed.getBenchmarkLastLookupTime() + mavCheckIntervalPrediction * TimeUnit.MINUTES.toMillis(1)
        - fps.getTimeNewestPost();

        if (feed.hasNewItem()) {
            if (Math.abs(diffPR) < Math.abs(diffMAV)) {
                usePostRate = true;
            } else {
                usePostRate = false;
            }
        }

        int mavInterval = FeedReader.DEFAULT_CHECK_TIME;
        UpdateStrategy mav = new MavUpdateStrategy();
        mav.update(feed, fps, trainingMode);
        mavInterval = feed.getUpdateInterval();
        mavCheckIntervalPrediction = feed.getUpdateInterval();

        int prInterval = FeedReader.DEFAULT_CHECK_TIME;
        UpdateStrategy pr = new PostRateUpdateStrategy();
        pr.update(feed, fps, trainingMode);
        prInterval = feed.getUpdateInterval();
        prCheckIntervalPrediction = feed.getUpdateInterval();

        if (usePostRate) {
            pr.update(feed, fps, trainingMode);
            feed.setUpdateInterval(getAllowedUpdateInterval(mavInterval));

        } else {
            mav.update(feed, fps, trainingMode);
            feed.setUpdateInterval(getAllowedUpdateInterval(prInterval));
        }

    }

    @Override
    public String getName() {
        return "mavpr";
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}