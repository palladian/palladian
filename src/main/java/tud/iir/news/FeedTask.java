package tud.iir.news;

import java.util.Date;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;

/**
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @see FeedReader
 * 
 */
class FeedTask implements Runnable {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FeedTask.class);

    /**
     * The feed retrieved by this task.
     */
    private Feed feed = null;

    /**
     * The feed checker calling this task. // FIXME This is a workaround. Can be fixed by externalizing update
     * strategies to a true strategy pattern.
     */
    private final FeedReader feedReader;

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public FeedTask(Feed feed, FeedReader feedChecker) {
        this.feed = feed;
        this.feedReader = feedChecker;
    }

    @Override
    public void run() {
        LOGGER.trace("beginning of feed task");

        LOGGER.debug(DateHelper.getCurrentDatetime() + ": running feed task " + feed.getId() + "(" + feed.getFeedUrl()
                + ")");


        // parse the feed and get all its entries, do that here since that takes some time and this is a thread so
        // it can be done in parallel
        FeedDownloader feedDownloader = new FeedDownloader();
        try {
            feedDownloader.updateItems(feed);
        } catch (NewsAggregatorException e) {
            LOGGER.error("update items of the feed didn't work well, " + e.getMessage());
            return;
        }

        // remember the time the feed has been checked
        feed.setLastPollTime(new Date());

        // classify feed if it has never been classified before, do it once a month for each feed to be informed about
        // updates
        LOGGER.debug("Activity Pattern: "+feed.getActivityPattern());
        LOGGER.debug("Current time: "+System.currentTimeMillis());
        LOGGER.debug("Last poll time: "+feed.getLastPollTime().getTime());
        LOGGER.debug("Current time - last poll time: "+(System.currentTimeMillis() - feed.getLastPollTime().getTime()));
        LOGGER.debug("Milliseconds in a mont: "+DateHelper.MONTH_MS);
        if (feed.getActivityPattern() == -1
                || System.currentTimeMillis() - feed.getLastPollTime().getTime() > DateHelper.MONTH_MS) {
            FeedClassifier.classify(feed);
        }

        feedReader.updateCheckIntervals(feed);

        // perform actions on this feeds entries
        LOGGER.debug("Performing action on feed: "+feed.getId() + "(" + feed.getFeedUrl()
                + ")");
        feedReader.getFeedProcessingAction().performAction(feed);

        // save the feed back to the database
        feedReader.updateFeed(feed);

        // LOGGER.info("End of Thread");
        // SchedulerTask.decrementThreadsAlive();
    }

}