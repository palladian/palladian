package tud.iir.news;

import java.util.List;

/**
 * The {@link FeedChecker} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author klemens.muthmann@googlemail.com
 * @see FeedChecker
 * 
 */
class FeedTask implements Runnable {

    /**
     * <p>
     * The feed retrieved by this task.
     * </p>
     */
    private Feed feed = null;

    /**
     * <p>
     * Creates a new retrieval task for a provided feed.
     * </p>
     *
     * @param feed The feed retrieved by this task.
     */
    public FeedTask(Feed feed) {
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

        } catch (NewsAggregatorException e) {
            FeedChecker.LOGGER.error(e.getMessage());
        }

        // perform actions on this feeds entries
        FeedChecker.getInstance().getFeedProcessingAction().performAction(feed);

        // save the feed back to the database
        fa.updateFeed(feed);
    }

}