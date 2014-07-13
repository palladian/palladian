package ws.palladian.retrieval.feeds.updates;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;

/**
 * <p>
 * Use the moving average to predict the next feed update.
 * </p>
 * 
 * @author David Urbansky
 * 
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
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
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

        // ########################## linear wave #########################
        // the factor by which the max checkInterval is multiplied, ranges between 2 and 0.5
        // double fMax = 1.0;
        // // all news are new, we should halve the checkInterval
        // if (pnTarget > 1) {
        // fMax = 0.5;
        // }
        // // some entries are not new so we increase the checkInterval
        // else {
        // // if (pnTarget == 0) {
        // // pnTarget = 1.0 / feed.getWindowSize();
        // // }
        // // if (pnTarget < 0.1) {
        // // pnTarget = 0.1;
        // // }
        // // fMax = 1 / pnTarget;
        // fMax = 2 - pnTarget;
        // }
        // maxCheckInterval *= fMax;

        // ######################### simple moving average for max policy ##############################
        // if (newEntries > 0) {
        // maxCheckInterval = entries.size() * (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        // } else {
        // double averagePostGap = fps.getAveragePostGap();
        // if (fps.getIntervals().size() > 0) {
        // averagePostGap -= fps.getIntervals().get(0) / feed.getWindowSize();
        // averagePostGap += fps.getDelayToNewestPost() / feed.getWindowSize();
        // maxCheckInterval = (int) (entries.size() * averagePostGap / DateHelper.MINUTE_MS);
        // }
        // }

        // ######################### avg ##############################
        // the factor by which the min checkInterval is multiplied, ranges between 2 and 0.5
        // double fMin = 1.0;
        // all news are new, we should halve the checkInterval
        // if (newEntries >= 1) {
        // fMin = 1.0 / newEntries;
        // minCheckInterval *= fMin;
        // }
        // // we have not found any new entry so we increase the min checkInterval
        // else {
        // // minCheckInterval += fps.getMedianPostGap() / (2 * DateHelper.MINUTE_MS);
        // minCheckInterval += fps.getAveragePostGap() / (2 * DateHelper.MINUTE_MS);
        // }

        // ######################### last interval for min policy ##############################
        // if (newEntries > 0) {
        // minCheckInterval = (int) (fps.getLastInterval() / DateHelper.MINUTE_MS);
        // } else {
        // minCheckInterval = (int) (fps.getDelayToNewestPost() / DateHelper.MINUTE_MS);
        // }

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
                        averagePostGap -= fps.getIntervals().get(0) / fps.getIntervals().size();
                        averagePostGap += fps.getDelayToNewestPost() / fps.getIntervals().size();
                    }
                    minCheckInterval = (int) (averagePostGap / TimeUnit.MINUTES.toMillis(1));
                    maxCheckInterval = (int) (entries.size() * averagePostGap / TimeUnit.MINUTES.toMillis(1));
                }
            }
        }

        // TODO get the current delay to the latest post? Because if we just take the average interval we might look too
        // late. I think I have tested this though and the results were worse than just taking the average interval
        // minCheckInterval -= (fps.getDelayToNewestPost() / DateHelper.MINUTE_MS);

        // ######################### simple moving median for min policy ##############################
        // if (newEntries > 0) {
        // minCheckInterval = (int) (fps.getMedianPostGap() / DateHelper.MINUTE_MS);
        // } else {
        // minCheckInterval = (int) (fps.getMedianPostGap2() / DateHelper.MINUTE_MS);
        // }

        // ######################### exponential moving average ##############################
        // Double[] weights = new Double[5];
        // weights[0] = 0.086;
        // weights[1] = 0.107;
        // weights[2] = 0.143;
        // weights[3] = 0.216;
        // weights[4] = 0.447;
        // long minCheckIntervalTemp = 0;
        // if (newEntries > 0) {
        // for (int i = 0; i < fps.getIntervals().size(); i++) {
        // minCheckIntervalTemp += weights[i + 1] * fps.getIntervals().get(i);
        // }
        // } else {
        //
        // List<Long> intervals = new ArrayList<Long>();
        //
        // // shift intervals
        // for (int i = 1; i < fps.getIntervals().size(); i++) {
        // intervals.add(fps.getIntervals().get(i));
        // }
        // intervals.add(fps.getDelayToNewestPost());
        //
        // for (int i = 0; i < intervals.size(); i++) {
        // minCheckIntervalTemp += weights[i + 1] * intervals.get(i);
        // }
        //
        // }
        //
        // minCheckInterval = (int) (minCheckIntervalTemp / DateHelper.MINUTE_MS);

        // ######################### linear moving average ##############################
        // double m = feed.getWindowSize() * feed.getWindowSize() / 2 - 0.5 * feed.getWindowSize();
        // long minCheckIntervalTemp = 0;
        // if (newEntries > 0) {
        // for (int i = 0; i < fps.getIntervals().size(); i++) {
        // minCheckIntervalTemp += i / m * fps.getIntervals().get(i);
        // }
        // } else {
        //
        // List<Long> intervals = new ArrayList<Long>();
        //
        // // shift intervals
        // for (int i = 1; i < fps.getIntervals().size(); i++) {
        // intervals.add(fps.getIntervals().get(i));
        // }
        // intervals.add(fps.getDelayToNewestPost());
        //
        // for (int i = 0; i < intervals.size(); i++) {
        // minCheckIntervalTemp += i / m * intervals.get(i);
        // }
        //
        // }
        //
        // minCheckInterval = (int) (minCheckIntervalTemp / DateHelper.MINUTE_MS);

        // ########################### linear regression global weights ####################################
        // LinearRegression lo = new LinearRegression();
        //
        // Instance instance;
        // if (newEntries > 0) {
        // instance = new Instance(4);
        // instance.setDataset(instances);
        // for (int i = 0; i < fps.getIntervals().size() - 1; i++) {
        // instance.setValue(i, fps.getIntervals().get(i));
        // }
        // instance.setClassValue(fps.getLastInterval());
        // instances.add(instance);
        // nothingFoundCount = 0;
        // } else {
        // nothingFoundCount++;
        // }
        //
        // try {
        // lo.buildClassifier(instances);
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        //
        // if (instances.numInstances() > 4) {
        // /*
        // * for (int i = 0; i < lo.coefficients().length; i++) {
        // * System.out.println(lo.coefficients()[i]);
        // * }
        // */
        //
        // instance = new Instance(4);
        // for (int i = 0; i < fps.getIntervals().size() - 1; i++) {
        // instance.setValue(i, fps.getIntervals().get(i));
        // }
        // // instance.setClassMissing();
        //
        // try {
        // // System.out.println(lo.classifyInstance(instance) + "_" + feed.getId());
        // minCheckInterval = (int) (lo.classifyInstance(instance) / DateHelper.MINUTE_MS);
        // if (minCheckInterval < 0) {
        // minCheckInterval = nothingFoundCount * (DEFAULT_CHECK_TIME / 2);
        // }
        // } catch (Exception e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }
        // /////////////////////

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
