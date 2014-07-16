package ws.palladian.retrieval.feeds;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>
 * The FeedImporter allows to add new feeds to the database.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class FeedImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedImporter.class);

    /** The store which keeps the feeds and items. */
    private final FeedStore store;

    /** The downloader for getting the feeds. */
    private final FeedParser feedParser;

    /** Whether to store the items of the added feed, or only the feed data. */
    private final boolean storeItems;

    /** If <code>true</code>, download feed, if <code>false</code>, just import it into database */
    private boolean downloadFeeds = true;

    public FeedImporter(FeedStore store, boolean downloadFeeds, boolean storeItems) {
        this.store = store;
        feedParser = new RomeFeedParser();
        this.downloadFeeds = downloadFeeds;
        this.storeItems = storeItems;
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedInformation Usually, this is the feedUrl but it may contain additional information like format and
     *            sizeURL, separated by $$$. *
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedInformation) {
        boolean added = false;

        String cleanedURL = feedInformation;
        String siteURL = null;
        if (feedInformation.contains("$$$")) {
            String[] feedLine = feedInformation.split("\\$\\$\\$");
            if (feedLine.length != 3) {
                LOGGER.error("Skipping illeagal feedInformation: {}", feedInformation);
                return false;
            }
            cleanedURL = feedLine[0];
            siteURL = feedLine[2];
        }

        Feed feed = store.getFeedByUrl(cleanedURL);
        if (feed == null) {
            StringBuilder infoMsg = new StringBuilder();
            infoMsg.append("added feed ").append(cleanedURL);

            boolean errorFree = true;
            if (downloadFeeds) {
                try {
                    feed = feedParser.getFeed(cleanedURL);

                    // classify the feed's activity pattern
                    FeedActivityPattern activityPattern = FeedClassifier.classify(feed);
                    feed.setActivityPattern(activityPattern);
                    infoMsg.append(" (activityPattern:").append(activityPattern).append(")");

                    feed.setWindowSize(feed.getItems().size());

                } catch (FeedParserException e) {
                    LOGGER.error("Error adding feed {}: {}", cleanedURL, e.getMessage());
                    errorFree = false;
                }
            } else {
                feed = new Feed();
            }

            if (errorFree) {
                // check for valid URLs
                try {
                    new URL(cleanedURL);
                    if (siteURL != null) {
                        new URL(siteURL);
                    }

                    // set feed URL
                    feed.setFeedUrl(cleanedURL);

                    // set site URL
                    feed.getMetaInformation().setSiteUrl(siteURL);

                    // add feed & entries to the store
                    added = store.addFeed(feed);

                    if (added && storeItems) {
                        store.addFeedItems(feed.getItems());
                    }

                    if (added) {
                        LOGGER.debug(infoMsg.toString());
                    } else {
                        LOGGER.error("Database error while adding feed {}", cleanedURL);
                    }

                } catch (MalformedURLException e) {
                    LOGGER.error("Error adding feed {}: {}", cleanedURL, e.getMessage());
                    added = false;
                }
            }

        } else {
            LOGGER.debug("I already have feed {}", cleanedURL);
        }

        return added;
    }

    /**
     * Add a Collection of feedUrls for aggregation. This process runs threaded. Use {@link #setNumThreads(int)} to set
     * the maximum number of concurrently running threads.
     * 
     * @param feedUrls
     * @return Number of added feeds.
     */
    public int addFeeds(Collection<String> feedUrls, int numThreads) {

        // Queue to store the URLs we will add
        final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>(feedUrls);

        // Counter for # of added Feeds
        final AtomicInteger addCounter = new AtomicInteger();

        // stop time for adding
        StopWatch stopWatch = new StopWatch();

        // start the specified number of threads for adding feeds simultaneously
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread() {
                @Override
                public void run() {
                    String feedUrl;
                    while ((feedUrl = urlQueue.poll()) != null) {
                        boolean added = addFeed(feedUrl);
                        if (added) {
                            addCounter.incrementAndGet();
                        }
                    }
                }
            };
            threads[i].start();
        }

        // wait until all Threads have finished work.
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                LOGGER.warn("Encountered InterruptedException");
            }
        }

        LOGGER.info("-------------------------------");
        LOGGER.info(" added {} new feeds", addCounter.get());
        LOGGER.info(" elapsed time: {}", stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: {} MB", HttpRetriever.getTraffic(SizeUnit.MEGABYTES));
        LOGGER.info("-------------------------------");

        return addCounter.get();

    }

    /**
     * Add feeds from a supplied file. The file must contain a newline separated list of feed URLs.
     * 
     * @param fileName The name of the file where the feed URLs are stored.
     * @return The number of feeds added.
     */
    public int addFeedsFromFile(String filePath, int numThreads) {
        List<String> feedUrls = FileHelper.readFileToArray(filePath);
        int added = addFeeds(feedUrls, numThreads);
        LOGGER.info("File contained {} entries, added {} feeds.", feedUrls.size(), added);
        return added;
    }

}
