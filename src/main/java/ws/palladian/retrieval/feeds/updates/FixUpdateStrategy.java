package ws.palladian.retrieval.feeds.updates;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedReader;

/**
 * <p>
 * Update the check intervals in fixed mode.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 */
public class FixUpdateStrategy extends UpdateStrategy {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FixUpdateStrategy.class);

    /**
     * The check interval in minutes.
     */
    private int checkInterval = -1;

    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

        // default value
        int fixedMinCheckInterval = FeedReader.DEFAULT_CHECK_TIME;

        // set fix interval, independent of feed, e.g. fix60 (fix1h)
        if (getCheckInterval() > 0) {
            fixedMinCheckInterval = getCheckInterval();
        } else {
            LOGGER.error("Fix interval has not been initialized, using defaul value " + FeedReader.DEFAULT_CHECK_TIME);
        }

        // set the (new) check interval to feed
        if (feed.getUpdateMode() == Feed.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedUpdateInterval(fixedMinCheckInterval));
        }
    }

    @Override
    public String getName() {
            return "fix" + getCheckInterval();
    }

    /**
     * Set a fixed check interval in minutes.
     * 
     * @param checkInterval Fixed check interval in minutes. Value has to be larger than zero.
     * @throws IllegalArgumentException In case the value is smaller or equal to zero.
     */
    public void setCheckInterval(int checkInterval) throws IllegalArgumentException {
        if (checkInterval <= 0) {
            throw new IllegalArgumentException("A fixed check interval smaller or equal to zero is not supported.");
        }
        this.checkInterval = checkInterval;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

}