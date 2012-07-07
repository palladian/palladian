package ws.palladian.retrieval.feeds.updates;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;

/**
 * <p>
 * Use the moving average to predict the next feed update, with modifications required to us it in
 * {@link DatasetCreator}.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 */
public class MavStrategyDatasetCreation extends UpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(MavStrategyDatasetCreation.class);

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

        int minCheckInterval = feed.getUpdateInterval();
        // int maxCheckInterval = feed.getUpdateInterval();

        // double newEntries = feed.getTargetPercentageOfNewEntries() * (feed.getWindowSize() - 1);
        boolean hasNewItem = feed.hasNewItem();

        // ######################### simple moving average ##############################

        double averagePostGap = fps.getAveragePostGap();
        if (averagePostGap <= 0D || !fps.isValidStatistics()) {

            // There is sometimes a weird behavior of some feeds that suddenly change their window size to zero.
            // In this case, we double the checkInterval that was used in the last check. We add one since the interval
            // may be zero.
            if (feed.getWindowSize() == 0 && feed.hasVariableWindowSize()) {
                minCheckInterval = 2 * feed.getUpdateInterval() + 1;
                LOGGER.warn("Feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + ") changed its windowSize to 0. Try to double checkInterval to " + minCheckInterval + ".");

                // in case of feeds with pattern chunked and on-the-fly that have only one "distinct" timestamp
            } else {
                minCheckInterval = getHighestUpdateInterval();
            }
        } else if (hasNewItem) {
            minCheckInterval = (int) (averagePostGap / TimeUnit.MINUTES.toMillis(1));
            // maxCheckInterval = (int) (entries.size() * fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        } else {
            if (fps.getIntervals().size() > 0) {
                // ignore negative delays caused by items with pubdates in the future
                if (fps.getDelayToNewestPost() > 0) {
                    averagePostGap -= fps.getIntervals().get(0) / (fps.getIntervals().size());
                    averagePostGap += fps.getDelayToNewestPost() / (fps.getIntervals().size());
                }

                minCheckInterval = (int) (averagePostGap / TimeUnit.MINUTES.toMillis(1));

                // maxCheckInterval = (int) (entries.size() * averagePostGap / DateHelper.MINUTE_MS);
            }
        }

        // TODO get the current delay to the latest post? Because if we just take the average interval we might look too
        // late. I think I have tested this though and the results were worse than just taking the average interval
        // minCheckInterval -= (fps.getDelayToNewestPost() / DateHelper.MINUTE_MS);

        // if (feed.getUpdateMode() == Feed.MIN_DELAY) {
        feed.setUpdateInterval(getAllowedUpdateInterval(minCheckInterval));
        // } else {
        // feed.setUpdateInterval(getAllowedUpdateInterval(maxCheckInterval));
        // }

    }

    /**
     * Get a random offset that is in [0, maxValue].
     * 
     * @param maxValue The maximum returned value
     * @return the offset which is 0 <= offset <= maxValue
     */
    protected int getRandomOffset(int maxValue) {
        return (int) Math.floor(Math.random() * maxValue);
    }

    /**
     * <p>
     * Check whether the computed highest check interval complies with the allowed highest check interval. If
     * getHighestUpdateInterval() < updateInterval is true, we return the highest check interval, but we subtract a
     * random offset to the default check time to avoid a peak in the number of feeds that have exactly the same update
     * interval (in our experiments, more than 10.000 feeds got the default check time) If updateInterval is shorter
     * than the allowed lowest update interval, we return the lowest. Here, we do nor add a random offset since these
     * feeds need to be polled as soon as possible.
     * </p>
     * 
     * @param updateInterval The computed highestCheckInterval.
     * @return The computed interval if it is in the limit.
     */
    @Override
    int getAllowedUpdateInterval(int updateInterval) {
        int allowedInterval = updateInterval;
        if (getHighestUpdateInterval() != -1 && getHighestUpdateInterval() <= updateInterval) {
            allowedInterval = getHighestUpdateInterval() - getRandomOffset(getHighestUpdateInterval() / 2);
        }
        if (getLowestUpdateInterval() != -1 && getLowestUpdateInterval() > updateInterval) {
            allowedInterval = getLowestUpdateInterval();
        }
        return allowedInterval;
    }

    @Override
    public String getName() {
        return "mav_creation";
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}
