package ws.palladian.retrieval.feeds.updates;

import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * An implementation of the update strategy called IndHist [BGR2006]
 * 
 * 
 * <p>
 * [BGR2006] Bright, L.; Gal, A. & Raschid, L. Adaptive pull-based policies for wide area data delivery, 
 * ACM Transactions on Database Systems, ACM, 2006, 31, 631-671, http://doi.acm.org/10.1145/1138394.1138399
 * </p>
 * 
 * @author Sandro Reichert
 * 
 */
public class IndHistUpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(IndHistUpdateStrategy.class);

    /**
     * The data base to load the model from.
     */
    private final FeedDatabase feedDb;

    /**
     * Identifier to be used to store the trained model as additional date with the feed.
     */
    private static final String MODEL_IDENTIFIER = "IndHistModel";

    /**
     * The threshold theta. If the algorithms estimates that this many new entries are pending, the next poll is
     * scheduled.
     */
    protected final double thresholdTheta;

    /**
     * @param thresholdTheta The threshold theta to be used. Usually, 0 <= theta < 1.
     *            If the algorithms estimates that this many new entries are pending, the next poll is scheduled.<br />
     *            See [BGR2006] Bright, L.; Gal, A. &amp; Raschid, L. Adaptive pull-based policies for wide area data
     *            delivery ACM Transactions on Database Systems, ACM, 2006, 31, 631-671,
     *            http://doi.acm.org/10.1145/1138394.1138399
     * @param feedDb The db to load the model from.
     */
    public IndHistUpdateStrategy(int lowestInterval, int highestInterval, double thresholdTheta, FeedDatabase feedDb) {
        super(lowestInterval, highestInterval);
        this.thresholdTheta = thresholdTheta;
        this.feedDb = feedDb;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics. Ignored.
     * @param trainingMode Flag to indicate whether the update model should be trained or not.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        if (trainingMode) {
            getModelFromDB(feed);
            setTrainingCompleted(feed);
        } else {
            updateCheckInterval(feed);
        }
    }

    /**
     * Update the feed's check interval in normal (non-training) mode, using the trained model.
     * 
     * @param feed The feed to update.
     */
    protected void updateCheckInterval(Feed feed) {
        if (feed.getLastPollTime() == null) {
            LOGGER.error("Feed id " + feed.getId()
                    + " has no lastPollTime. Cant predict next poll. Setting interval to standard.");
            feed.setUpdateInterval(getAllowedInterval(DEFAULT_CHECK_TIME));

        } else {
            int checkInterval = DEFAULT_CHECK_TIME;

            // The trained model for the current feed
            double[] hourlyRates = getModelFromFeed(feed);
            double dailyRate = getDailyRate(hourlyRates);

            // empty feeds
            if (dailyRate == 0.0) {
                feed.setUpdateInterval(getAllowedInterval(checkInterval));

            } else {
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(feed.getLastPollTime());

                // normal case
                int pollHourOfDay = calendar.get(Calendar.HOUR_OF_DAY);
                int simulatedHour = pollHourOfDay;
                double pendingItems = 0.0;
                checkInterval = 0;

                // ---- Do we need to perform the next poll in the same hour the current poll has been done?
                // number of seconds already passed in the current hour
                int currentSeconds = calendar.get(Calendar.MINUTE) * 60 + calendar.get(Calendar.SECOND);
                int remainingSeconds = 3600 - currentSeconds;
                // number of new elements
                double remainingHourPendingItems = hourlyRates[simulatedHour] * remainingSeconds / 3600;

                // we expect more new items in the current hour than our threshold, so we only have a look at this
                // hour
                if (remainingHourPendingItems >= thresholdTheta) {

                    checkInterval += (60 * thresholdTheta / hourlyRates[simulatedHour]);

                } else {
                    // add remaining part of current hour
                    pendingItems += remainingHourPendingItems;
                    checkInterval += (int) (remainingSeconds / 60);
                    simulatedHour = (simulatedHour + 1) % 24;

                    // loop complete days if possible. stop looping if we would exceed the threshold in this hour or
                    // if we reach the checkInterval's upper bound (getHighestUpdateInterval() == -1 in case there
                    // is no upper bound)
                    while (pendingItems + dailyRate < thresholdTheta
                            && (checkInterval + 1440 < getHighestInterval() || getHighestInterval() == -1)) {
                        pendingItems += dailyRate;
                        checkInterval += 1440;
                    }

                    // loop over hours, add full hours only. stop looping if we would exceed the threshold in this
                    // hour or if we reach the checkInterval's upper bound (getHighestUpdateInterval() == -1 in case
                    // there is no upper bound)
                    while (pendingItems + hourlyRates[simulatedHour] < thresholdTheta
                            && (checkInterval + 60 < getHighestInterval() || getHighestInterval() == -1)) {
                        pendingItems += hourlyRates[simulatedHour];
                        simulatedHour = (simulatedHour + 1) % 24;
                        checkInterval += 60;
                    }

                    // add part of last hour
                    checkInterval += (60 * (thresholdTheta - pendingItems) / hourlyRates[simulatedHour]);
                }

                // finally, set the feed's interval
                feed.setUpdateInterval(getAllowedInterval(checkInterval));
            }
        }
    }


    /**
     * Loads the feed's model from db and stores model as additional data with the feed. The model has been trained
     * externally since this is much faster.
     * 
     * @param feed
     */
    protected void getModelFromDB(Feed feed) {
        // The trained model for the current feed
        double[] hourlyRates;

        // step 1: Load model from db. The model has been trained externally since this is much faster.
        hourlyRates = feedDb.getIndHistModel(feed.getId());

        // step 2: store model in feed to load it in normal mode.
        Map<String, Object> additionalData = feed.getAdditionalData();
        if (additionalData.containsKey(MODEL_IDENTIFIER)) {
            LOGGER.warn("Feed id " + feed.getId() + " already containd a model for strategy IndHist, identified by \""
                    + MODEL_IDENTIFIER + "\". This is not expected since the model is to be trained only once. "
                    + "Current model has been replaced by model from database.");
        }
        additionalData.put(MODEL_IDENTIFIER, hourlyRates);
        feed.setAdditionalData(additionalData);
    }

    /**
     * Updates the chechInterval properly to indicate that training is completed.
     * 
     * @param feed
     */
    protected void setTrainingCompleted(Feed feed) {
        // set checkInterval to reach the end of the training period, indicating
        long currentPollTimestamp = 0L;
        if (feed.getLastPollTime() == null) {
            LOGGER.error("Feed id " + feed.getId() + " has no lastPollTime. ");
        } else {
            currentPollTimestamp = feed.getLastPollTime().getTime();
        }
        // add +1 to make sure the resulting date is in future of BENCHMARK_START_TIME_MILLISECOND. It would be
        // better to directly set the date of the next poll but this is not supported at the moment and would push
        // even more parameters into class feed.
        int checkInterval = (int) ((FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND - currentPollTimestamp) / TimeUnit.MINUTES.toMillis(1)) + 1;
        // we do not need to check for update interval bounds since this is a hack to indicate training period is
        // completed...
        feed.setUpdateInterval(checkInterval);
    }

    /**
     * Get model from the feed. In case there is no model, get it from db.
     * 
     * @param feed
     * @return The trained model for the current feed, i.e. hourly update rates for hours 0-23.
     */
    protected double[] getModelFromFeed(Feed feed) {
        double[] hourlyRates = new double[24];
        Map<String, Object> additionalData = feed.getAdditionalData();
        if (!additionalData.containsKey(MODEL_IDENTIFIER)) {
            LOGGER.error("Feed id " + feed.getId() + " contains no model for strategy IndHist, reloading it from db.");
            getModelFromDB(feed);
            if (!additionalData.containsKey(MODEL_IDENTIFIER)) {
                LOGGER.error("Feed id " + feed.getId()
                        + " db contains no model for strategy IndHist, assuming update rate of 0.0 .");
                return hourlyRates;
            }
        }
        return (double[]) feed.getAdditionalData().get(MODEL_IDENTIFIER);
    }

    /**
     * Sum up all hourly rates to get the daily update rate.
     * 
     * @param hourlyRates the hourly update rates.
     * @return the daily update rate.
     */
    protected double getDailyRate(double[] hourlyRates) {
        double dailyRate = 0.0;
        for (double hourlyRate : hourlyRates) {
            dailyRate += hourlyRate;
        }
        return dailyRate;
    }

    /**
     * @return always <code>true</code>.
     */
    @Override
    public boolean hasExplicitTrainingMode() {
        return true;
    }

    /**
     * Returns the update strategy's name "IndHist", followed by an underscore and the used threshold theta, e.g.
     * "IndHist_0.4"
     */
    @Override
    public String getName() {
        return "IndHist_" + thresholdTheta;

    }

}