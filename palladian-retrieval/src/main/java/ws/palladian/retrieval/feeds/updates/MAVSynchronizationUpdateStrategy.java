package ws.palladian.retrieval.feeds.updates;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Update the check intervals as moving average with synchronization.<br />
 * <br />
 * </p>
 * 
 * @author Sandro Reichert
 * 
 */
public class MAVSynchronizationUpdateStrategy extends UpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MAVSynchronizationUpdateStrategy.class);

    /**
     * Controls the usage of the optional RSS element time to live. In all cases: If a feed's ttl is set to -1, it is
     * ignored anyway. FeedReader's interval bounds are used anyway, so in case ttl is smaller than FeedReaders lower
     * bound, ttl is overwritten by FeedReaders lower bound.
     * Cases of configuration:<br />
     * If set to 0, ignore attribute. <br />
     * If set to 1, use ttl as a lower bound for the checkInterval calculated from the feed and do not use a shorter
     * interval than ttl. <br />
     * If set to 2, use it as checkInterval, and do not calculate interval from feed. <br />
     */
    private static int rssTTLmode;

    /**
     * Create MAVSync strategy ignoring RSS ttl element.
     */
    public MAVSynchronizationUpdateStrategy() {
        this(0);
    }

    /**
     * Control the usage of the optional RSS element time to live. In all cases: If a feed's ttl is set to -1, it is
     * ignored anyway. FeedReader's interval bounds are used anyway, so in case ttl is smaller than FeedReaders lower
     * bound, ttl is overwritten by FeedReaders lower bound.
     * Cases of configuration:<br />
     * If set to 0, ignore attribute. <br />
     * If set to 1, use ttl as a lower bound for the checkInterval calculated from the feed and do not use a shorter
     * interval than ttl. <br />
     * If set to 2, use it as checkInterval, and do not calculate interval from feed. <br />
     * 
     * @param rssTTLmode see text.
     */
    public MAVSynchronizationUpdateStrategy(int rssTTLmode) {
        super();
        if (rssTTLmode < 0 || rssTTLmode > 2) {
            throw new IllegalArgumentException("Wrong usage of rssTTLmode! Value " + rssTTLmode + " not supported.");
        }
        this.rssTTLmode = rssTTLmode;
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

        // set default value to be used if we can't compute an interval from feed (e.g. feed has no items)
        int checkIntervalMinutes = FeedReader.DEFAULT_CHECK_TIME;

        List<FeedItem> entries = feed.getItems();

        // ------- first, get interval from last window and check whether synchronization is possible -------

        // get interval from last window
        Date intervalStartTime = feed.getOldestFeedEntryCurrentWindow();
        Date intervalStopTime = feed.getLastFeedEntry();
        long intervalLengthMillisecond = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
        int windowIntervalMinutes = 0;
        if (entries.size() >= 2 && intervalLengthMillisecond > 0) {
            windowIntervalMinutes = (int)(intervalLengthMillisecond / ((entries.size() - 1) * TimeUnit.MINUTES
                    .toMillis(1)));
        }

        // check whether synchronization is possible
        // the time of the next poll
        long synchronizedPollTime = 0L;
        if (intervalStopTime != null) { // activity pattern Empty
            synchronizedPollTime = intervalStopTime.getTime() + windowIntervalMinutes * TimeUnit.MINUTES.toMillis(1);
        }

        // the resulting checkInterval between last and next poll
        checkIntervalMinutes = (int)((synchronizedPollTime - feed.getLastPollTime().getTime()) / TimeUnit.MINUTES
                .toMillis(1));

        // If checkInterval is within bounds, we can use it, otherwise we use alternative calculation
        if (checkIntervalMinutes == getAllowedUpdateInterval(checkIntervalMinutes)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Feedid " + feed.getId() + " could do synchronization step at poll " + feed.getChecks()
                        + 1);
            }
        }
        // ------- second, use last window and last poll time to get interval and next poll time -------
        else {
            checkIntervalMinutes = FeedReader.DEFAULT_CHECK_TIME;
            intervalStopTime = feed.getLastPollTime();
            intervalLengthMillisecond = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
            if (entries.size() >= 1 && intervalLengthMillisecond > 0) {
                checkIntervalMinutes = (int)(intervalLengthMillisecond / (entries.size() * TimeUnit.MINUTES.toMillis(1)));
            }

        }

        // check for RSS ttl usage
        if (rssTTLmode != 0 && feed.getMetaInformation() != null) {

            // get value from feed.
            Integer rssTTL = feed.getMetaInformation().getRssTtl();

            // check if feed contains a valid RSS ttl value
            if (rssTTL != null && rssTTL >= 0) {

                // use ttl value as lower bound
                if (rssTTLmode == 1 && checkIntervalMinutes < rssTTL) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Feed " + feed.getId() + " set interval from " + checkIntervalMinutes
                                + " to rssTTL " + rssTTL);
                    }
                    checkIntervalMinutes = rssTTL;
                }
                // set ttl value as interval
                else if (rssTTLmode == 2) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Feed " + feed.getId() + " set interval from " + checkIntervalMinutes
                                + " to rssTTL " + rssTTL);
                    }
                    checkIntervalMinutes = rssTTL;
                }
            }
        }

        feed.setUpdateInterval(getAllowedUpdateInterval(checkIntervalMinutes));

    }

    /**
     * Strategy name + value of RSS ttl mode.
     */
    @Override
    public String getName() {
        return "MAVSync_" + rssTTLmode;

    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}