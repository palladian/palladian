package tud.iir.news;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * 
 */
class FeedTask extends TimerTask {

    /** keep a reference to the timer in order to cancel it after the first run so that the thread can be garbage collected */
    private Timer timer = null;

    private Feed feed = null;

    public FeedTask(Timer timer, Feed feed) {
        this.timer = timer;
        this.feed = feed;
    }

    @Override
    public void run() {
        NewsAggregator fa = new NewsAggregator();
        fa.setUseScraping(false);

        try {
            // parse the feed and get all its entries, do that here since that takes some time and this is a thread so it can be done in parallel
            List<FeedEntry> entries = fa.getEntries(feed.getFeedUrl());
            feed.setEntries(entries);

            // classify feed if it has never been classified before
            if (feed.getUpdateClass() == -1) {
                FeedClassifier.classify(feed);
            }

            FeedChecker.getInstance().updateCheckIntervals(feed);

        } catch (FeedAggregatorException e) {
            FeedChecker.LOGGER.error(e.getMessage());
        }

        // perform actions on this feeds entries
        FeedChecker.getInstance().getFeedProcessingAction().performAction(feed);

        // save the feed back to the database
        fa.updateFeed(feed);

        // set up timer for the next reading
        FeedChecker.getInstance().addTimer(feed, feed.getMaxCheckInterval());

        timer.cancel();
    }

}