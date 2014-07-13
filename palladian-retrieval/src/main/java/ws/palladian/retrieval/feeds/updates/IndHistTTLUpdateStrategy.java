package ws.palladian.retrieval.feeds.updates;

import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * An implementation of the update strategy called IndHist/TTL [BGR2006]
 * 
 * 
 * <p>
 * [BGR2006] Bright, L.; Gal, A. & Raschid, L. Adaptive pull-based policies for wide area data delivery ACM Transactions
 * on Database Systems, ACM, 2006, 31, 631-671, http://doi.acm.org/10.1145/1138394.1138399
 * </p>
 * 
 * @author Sandro Reichert
 * 
 */
public class IndHistTTLUpdateStrategy extends IndHistUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IndHistUpdateStrategy.class);

    /**
     * Threshold. If, in a certain timeWindow, (number of new entries) / (number of predicted updates by IndHist)
     * exceeds threshold, use TTL, other wise use IndHist. Common value is 2.0
     */
    private final double tBurst;

    /**
     * A time window in hours, used for decision which algorithm to use. Common value is 1 or 24 (1 day)
     */
    private final int timeWindowHours;

    /**
     * Weight to be forwarded to {@link AdaptiveTTLUpdateStrategy}.
     */
    private final double adaptiveTTLWeightM;

    /**
     * To get the real number of items in the last window, we need to store them in the feed as additional data, using
     * this identifier.
     */
    private final String IndHistTTL_WINDOW_IDENTIFIER = "IndHistTTLWindow";

    private final FeedUpdateMode updateMode;

    /**
     * @param thresholdTheta The threshold theta to be used. Usually, 0 <= theta < 1.
     *            If the algorithms estimates that this many new entries are pending, the next poll is scheduled.<br />
     *            See [BGR2006] Bright, L.; Gal, A. &amp; Raschid, L. Adaptive pull-based policies for wide area data
     *            delivery ACM Transactions on Database Systems, ACM, 2006, 31, 631-671,
     *            http://doi.acm.org/10.1145/1138394.1138399
     * @param feedDb The db to load the model from.
     * @param tBurst Threshold. If, in a certain timeWindow, (number of new entries) / (number of predicted updates by
     *            IndHist) exceeds threshold, use TTL, other wise use IndHist. Common value is 2.0
     * @param timeWindowHours A time window in hours, used for decision which algorithm to use. Common value is 1 or 24
     *            (1 day)
     * @param adaptiveTTLWeightM Weight to be forwarded to {@link AdaptiveTTLUpdateStrategy}.
     */
    public IndHistTTLUpdateStrategy(int lowestInterval, int highestInterval, double thresholdTheta,
            FeedDatabase feedDb, double tBurst, int timeWindowHours, double adaptiveTTLWeightM, FeedUpdateMode updateMode) {
        super(lowestInterval, highestInterval, thresholdTheta, feedDb);
        this.tBurst = tBurst;
        this.timeWindowHours = timeWindowHours;
        this.adaptiveTTLWeightM = adaptiveTTLWeightM;
        this.updateMode = updateMode;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode Flag to indicate whether the update model should be trained or not.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {
        if (trainingMode) {
            getModelFromDB(feed);
            setTrainingCompleted(feed);
        } else {

            if (feed.getLastPollTime() == null) {
                LOGGER.error("Feed id " + feed.getId()
                        + " has no lastPollTime. Cant predict next poll. Setting interval to standard.");
                feed.setUpdateInterval(getAllowedInterval(DEFAULT_CHECK_TIME));

            } else {
                // normal case
                // We need to decide which sub-strategy to use. To do so, we have a look at the time window before the
                // last poll and decide whether a burst happened. Therefore, we need
                // 1) the number of items predicted within this window
                // 2) the number of items received within this window
                // 3) select the strategy

                // 1) get the number of items predicted within this window
                double predictedNumberItemsInWindow = calculatePredictedNumUpdates(feed);

                // 2) get the number of items received within this window
                // 2a) get items seen atlast poll(s)
                List<Long> itemTimestamps = getRecentItems(feed);

                // 2b) add new items of current poll
                itemTimestamps = addNewItems(feed, itemTimestamps);

                // 2c) remove timestamps older than time window
                itemTimestamps = removeOldTimestamps(feed, itemTimestamps);

                // 2d) set timestamps back to feed to read at next poll.
                feed.addAdditionalData(IndHistTTL_WINDOW_IDENTIFIER, itemTimestamps);

                // 3) now its time to select the strategy
                int realNumberItemsInWindow = itemTimestamps.size();
                if ((realNumberItemsInWindow > 0 && predictedNumberItemsInWindow == 0)
                        || ((realNumberItemsInWindow / predictedNumberItemsInWindow) > tBurst)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Feed id " + feed.getId() + ", pollTime " + feed.getLastPollTime()
                                + " using strategy AdaptiveTTL.");
                    }

                    // use Adaptive TTL
                    AdaptiveTTLUpdateStrategy ttl = new AdaptiveTTLUpdateStrategy(getLowestInterval(),
                            getHighestInterval(), adaptiveTTLWeightM, updateMode);
                    ttl.update(feed, fps, trainingMode);

                } else {
                    // use IndHist
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Feed id " + feed.getId() + ", pollTime " + feed.getLastPollTime()
                                + " using strategy IndHist.");
                    }

                    updateCheckInterval(feed);
                }
            }
        }
    }

    /**
     * Remove all timestamps from the list that belong to items older than the time window
     * 
     * @param feed
     * @param itemTimestamps The list to process.
     * @return the processed list.
     */
    private List<Long> removeOldTimestamps(Feed feed, List<Long> itemTimestamps) {
        Iterator<Long> it = itemTimestamps.iterator();
        while (it.hasNext()) {
            Long timestamp = it.next();
            Long windowStartTime = feed.getLastPollTime().getTime() - timeWindowHours * TimeUnit.HOURS.toMillis(1);
            if (timestamp < windowStartTime) {
                it.remove();
            }
        }
        return itemTimestamps;
    }

    /**
     * Adds the timestamps of the feed's new items to the one received in previous polls
     * 
     * @param feed
     * @param itemTimestamps The feed's item timestamps from previous polls
     * @return same list with added timestamps.
     */
    private List<Long> addNewItems(Feed feed, List<Long> itemTimestamps) {
        if (itemTimestamps == null) {
            itemTimestamps = new LinkedList<Long>();
        }
        for (FeedItem item : feed.getNewItems()) {
            if (item.getCorrectedPublishedDate() != null) {
                itemTimestamps.add(item.getCorrectedPublishedDate().getTime());
            } else {
                // this should never happen
                itemTimestamps.add(feed.getLastPollTime().getTime());
            }
        }
        return itemTimestamps;
    }

    /**
     * To identify bursts and choose which strategy to use, the predicted number of updates during the time window
     * before last poll time is required.
     * 
     * @param feed
     * @return The predicted number of updates in the time window before lastPollTime.
     */
    private double calculatePredictedNumUpdates(Feed feed) {
        // get the trained model for the current feed
        double[] hourlyRates = getModelFromFeed(feed);
        double dailyRate = getDailyRate(hourlyRates);

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(feed.getLastPollTime());
        int pollHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        int simulatedHour = pollHourOfDay;
        double predictedNumberItemsInWindow = 0.0;

        // used to sum up intervals till historyMinutes = getTimeWindowHours()
        int historyMinutes = 0;

        // number of minutes already passed in the current hour
        int currentMinutes = calendar.get(Calendar.MINUTE) * 60;

        // number of new elements within currentMinutes
        // in case time window is set in minutes, modify next line if currentMinutes < timeWindow
        predictedNumberItemsInWindow = hourlyRates[simulatedHour] * currentMinutes / 60;
        historyMinutes = currentMinutes;

        // go backward in time
        simulatedHour = (24 + simulatedHour - 1) % 24;

        // add full days till we would see more than the windowSize
        while (historyMinutes + 1440 < timeWindowHours * 60) {
            historyMinutes += 1440;
            predictedNumberItemsInWindow += dailyRate;
        }

        // add full hours till we would see more than the windowSize
        while (historyMinutes + 60 < timeWindowHours * 60) {
            historyMinutes += 60;
            predictedNumberItemsInWindow += hourlyRates[simulatedHour];
            simulatedHour = (24 + simulatedHour - 1) % 24;
        }

        // add part of oldest hour
        predictedNumberItemsInWindow += ((timeWindowHours * 60) - historyMinutes) * (hourlyRates[simulatedHour] / 60);

        return predictedNumberItemsInWindow;
    }

    /**
     * Load {@link #IndHistTTL_WINDOW_IDENTIFIER} from feed
     * 
     * @param feed
     * @return the itemTimestamps that have been stored by this strategy before or <code>null</code> in case this is the
     *         first poll.
     */
    @SuppressWarnings("unchecked")
    private List<Long> getRecentItems(Feed feed) {
        List<Long> itemTimestamps = null;
        try {
            itemTimestamps = (List<Long>)feed.getAdditionalData(IndHistTTL_WINDOW_IDENTIFIER);
        } catch (Exception e) {
            LOGGER.error("Could not load " + IndHistTTL_WINDOW_IDENTIFIER + " from feed.");
        }
        return itemTimestamps;
    }

    /**
     * Returns the update strategy's name "IndHistTTL", followed by the parameters used, separated by underscores.
     * Parameter order: theta, AdaptiveTTL_M, T_burst and the time window in hours
     */
    @Override
    public String getName() {
        return "IndHisTTL" + thresholdTheta + "_" + adaptiveTTLWeightM + "_" + tBurst + "_" + timeWindowHours;
    }

}
