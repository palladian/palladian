package tud.iir.news;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.apache.log4j.Logger;

import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;

public class FeedImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedImporter.class);

    private int maxThreads = 10;

    private final FeedStore store;
    private FeedDownloader feedDownloader;

    public FeedImporter(FeedStore store) {
        this.store = store;
        feedDownloader = new FeedDownloader();
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedUrl The url of the feed.
     * @param storeItem Whether to store the items from the feed in the database.
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedUrl, boolean storeItems) {
        LOGGER.trace(">addFeed " + feedUrl);
        boolean added = false;

        Feed feed = store.getFeedByUrl(feedUrl);
        if (feed == null) {
            try {

                feed = feedDownloader.getFeed(feedUrl);

                // classify feed's text extent
                FeedContentClassifier classifier = new FeedContentClassifier();
                int textType = classifier.determineFeedTextType(feed);
                feed.setTextType(textType);

                // classify the feed's activity pattern
                feed.setActivityPattern(FeedClassifier.classify(feed.getItems()));

                feed.setWindowSize(feed.getItems().size());

                // set feed and site URL
                feed.setFeedUrl(feedUrl, true);

                // add feed & entries to the store
                store.addFeed(feed);

                if (storeItems) {
                    for (FeedItem feedEntry : feed.getItems()) {
                        store.addFeedEntry(feed, feedEntry);
                    }
                }

                LOGGER.info("added feed to store " + feedUrl + " (textType:"
                        + classifier.getReadableFeedTextType(textType) + ")");
                added = true;

            } catch (NewsAggregatorException e) {
                LOGGER.error("error adding feed " + feedUrl + " " + e.getMessage());
            }
        } else {
            LOGGER.info("i already have feed " + feedUrl);
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    /**
     * Add a Collection of feedUrls for aggregation. This process runs threaded. Use {@link #setMaxThreads(int)} to set
     * the maximum number of concurrently running threads.
     * 
     * @param feedUrls
     * @return Number of added feeds.
     */
    public int addFeeds(Collection<String> feedUrls, final boolean storeItems) {

        // Stack to store the URLs we will add
        final Stack<String> urlStack = new Stack<String>();
        urlStack.addAll(feedUrls);

        // Counter for active Threads
        final Counter threadCounter = new Counter();

        // Counter for # of added Feeds
        final Counter addCounter = new Counter();

        // stop time for adding
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        while (urlStack.size() > 0) {
            final String currentUrl = urlStack.pop();

            // if maximum # of Threads are already running, wait here
            while (threadCounter.getCount() >= maxThreads) {
                LOGGER.trace("max # of Threads running. waiting ...");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                }

            }

            threadCounter.increment();
            Thread addThread = new Thread() {
                @Override
                public void run() {
                    try {
                        boolean added = addFeed(currentUrl, storeItems);
                        if (added) {
                            addCounter.increment();
                        }
                    } finally {
                        threadCounter.decrement();
                    }
                }
            };
            addThread.start();

        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (threadCounter.getCount() > 0 || urlStack.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
            LOGGER.trace("waiting ... threads:" + threadCounter.getCount() + " stack:" + urlStack.size());
        }

        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" added " + addCounter.getCount() + " new feeds");
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: " + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + " MB");
        LOGGER.info("-------------------------------");

        return addCounter.getCount();

    }

    /**
     * Add feeds from a supplied file. The file must contain a newline separated list of feed URLs.
     * 
     * @param fileName The name of the file where the feed URLs are stored.
     * @return The number of feeds added.
     */
    public int addFeedsFromFile(String filePath, boolean storeItems) {
        LOGGER.trace(">addFeedsFromFile");
        List<String> feedUrls = FileHelper.readFileToArray(filePath);
        LOGGER.info("adding " + feedUrls.size() + " feeds");
        int result = addFeeds(feedUrls, storeItems);
        LOGGER.trace("<addFeedsFromFile " + result);
        return result;
    }

}
