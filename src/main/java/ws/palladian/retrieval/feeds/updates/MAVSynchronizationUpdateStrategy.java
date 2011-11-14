package ws.palladian.retrieval.feeds.updates;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

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
    private static final Logger LOGGER = Logger.getLogger(MAVSynchronizationUpdateStrategy.class);


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
        
        // set default value to be used if we cant compute an interval from feed (e.g. feed has no items)
        int checkIntervalMinutes = FeedReader.DEFAULT_CHECK_TIME;

        List<FeedItem> entries = feed.getItems();

        
        // ------- first, get interval from last window and check whether synchronization is possible -------
       
        // get interval from last window
        Date intervalStartTime = feed.getOldestFeedEntryCurrentWindow();
        Date intervalStopTime = feed.getLastFeedEntry();
        long intervalLengthMillisecond = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
        int windowIntervalMinutes = 0;
        if (entries.size() >= 2 && intervalLengthMillisecond > 0) {
            windowIntervalMinutes = (int) (intervalLengthMillisecond / ((entries.size() - 1) * DateHelper.MINUTE_MS));
        }

        // check whether synchronization is possible
        // the time of the next poll
        long synchronizedPollTime = 0L;
        if (intervalStopTime != null) { // activity pattern Empty
            synchronizedPollTime = intervalStopTime.getTime() + windowIntervalMinutes * DateHelper.MINUTE_MS;
        }

        // the resulting checkInterval between last and next poll
        checkIntervalMinutes = (int) ((synchronizedPollTime - feed.getLastPollTime().getTime()) / DateHelper.MINUTE_MS);

        // If checkInterval is within bounds, we can use it, otherwise we use alternative calculation
        if (checkIntervalMinutes == getAllowedUpdateInterval(checkIntervalMinutes)) {
            feed.setUpdateInterval(checkIntervalMinutes);
            LOGGER.debug("Feedid " + feed.getId() + " doing synchronization step in poll " + feed.getChecks() + 1);
        }
        // ------- second, use last window and last poll time to get interval and next poll time -------
        else {
            checkIntervalMinutes = FeedReader.DEFAULT_CHECK_TIME;
            intervalStopTime = feed.getLastPollTime();
            intervalLengthMillisecond = DateHelper.getIntervalLength(intervalStartTime, intervalStopTime);
            if (entries.size() >= 1 && intervalLengthMillisecond > 0) {
                checkIntervalMinutes = (int) (intervalLengthMillisecond / (entries.size() * DateHelper.MINUTE_MS));
            }
            feed.setUpdateInterval(getAllowedUpdateInterval(checkIntervalMinutes));
        }
    }

    @Override
    public String getName() {
        return "MAVSync";

    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }

}