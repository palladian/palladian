package tud.iir.news;

import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;

/**
 * A scheduler task handles the distribution of feeds to worker threads that read these feeds.
 * 
 * @author Klemens Muthmann
 * 
 */
class SchedulerTask extends TimerTask {

    private static final Logger LOGGER = Logger.getLogger(SchedulerTask.class);

    /**
     * The collection of all the feeds this scheduler should create update threads for.
     */
    private final FeedChecker feedChecker;

    /**
     * The thread pool managing threads that read feeds from the feed sources provided by {@link #collectionOfFeeds}.
     */
    private final ExecutorService threadPool;

    /**
     * Creates a new scheduler task with a maximum allowed number of threads and a list of feeds to read updates from.
     * 
     * @param collectionOfFeeds The collection of all the feeds this scheduler should create update threads for.
     * @param threadPoolSize The maximum number of threads to distribute reading feeds to.
     */
    public SchedulerTask(FeedChecker feedChecker) {
        super();
        threadPool = Executors.newFixedThreadPool(FeedChecker.MAX_THREAD_POOL_SIZE);
        this.feedChecker = feedChecker;
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        LOGGER.info("wake up to check feeds");
        Date now = new Date();
        int feedCount = 0;
        for (Feed feed : feedChecker.getFeeds()) {
            LOGGER.debug("Checking feed at address: " + feed.getFeedUrl());
            if (feed.getChecks() == 0
                    || feed.getLastChecked() == null
                    || now.getTime() - feed.getLastChecked().getTime() > feed.getMaxCheckInterval()
                            * DateHelper.MINUTE_MS) {
                threadPool.execute(new FeedTask(feed, feedChecker));
                feedCount++;
            }
            now.setTime(System.currentTimeMillis());
        }
        LOGGER.info("scheduled " + feedCount + " feeds for reading");
    }
}