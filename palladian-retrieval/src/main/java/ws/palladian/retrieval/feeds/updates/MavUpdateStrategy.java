package ws.palladian.retrieval.feeds.updates;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * Use the moving average to predict the next feed update.
 * </p>
 *
 * @author David Urbansky
 */
public class MavUpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MavUpdateStrategy.class);

    private final FeedUpdateMode updateMode;

    public MavUpdateStrategy(int lowestInterval, int highestInterval, FeedUpdateMode updateMode) {
        super(lowestInterval, highestInterval);
        Validate.notNull(updateMode, "updateMode must not be null");
        this.updateMode = updateMode;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     *
     * @param feed         The feed to update.
     * @param fps          This feeds feed post statistics.
     * @param trainingMode Ignored parameter. The strategy does not support an explicit training mode.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {
        if (trainingMode) {
            LOGGER.warn("Update strategy " + getName() + " does not support an explicit training mode.");
        }

        List<FeedItem> entries = feed.getItems();

        int minCheckInterval = feed.getUpdateInterval();
        int maxCheckInterval = feed.getUpdateInterval();

        // ######################### simple moving average ##############################
        if (feed.hasNewItem()) {
            minCheckInterval = (int) (fps.getAveragePostGap() / TimeUnit.MINUTES.toMillis(1));
            maxCheckInterval = (int) (entries.size() * fps.getAveragePostGap() / TimeUnit.MINUTES.toMillis(1));
        } else {
            if (fps.getIntervals().size() > 0) {
                double averagePostGap = fps.getAveragePostGap();
                if (averagePostGap == 0D) {
                    // in case of feeds with pattern chunked and on-the-fly that have only one "distinct" timestamp
                    minCheckInterval = getHighestInterval();
                } else {
                    // ignore negative delays caused by items with pubdates in the future
                    if (fps.getDelayToNewestPost() > 0) {
                        averagePostGap -= fps.getIntervals().get(0) / (double) fps.getIntervals().size();
                        averagePostGap += fps.getDelayToNewestPost() / (double) fps.getIntervals().size();
                    }
                    minCheckInterval = (int) (averagePostGap / TimeUnit.MINUTES.toMillis(1));
                    maxCheckInterval = (int) (entries.size() * averagePostGap / TimeUnit.MINUTES.toMillis(1));
                }
            }
        }

        if (updateMode == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(minCheckInterval));
        } else {
            feed.setUpdateInterval(getAllowedInterval(maxCheckInterval));
        }

        // in case only one entry has been found use default check time
        if (entries.size() <= 1) {
            if (updateMode == FeedUpdateMode.MIN_DELAY) {
                feed.setUpdateInterval(getAllowedInterval(DEFAULT_CHECK_TIME / 2));
            } else {
                feed.setUpdateInterval(getAllowedInterval(DEFAULT_CHECK_TIME));
            }
        }
    }

    @Override
    public String getName() {
        return "mav";
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }
}
