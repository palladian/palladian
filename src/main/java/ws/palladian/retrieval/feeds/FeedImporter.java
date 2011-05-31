package ws.palladian.retrieval.feeds;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.SizeUnit;
import ws.palladian.retrieval.feeds.FeedContentClassifier.FeedContentType;
import ws.palladian.retrieval.feeds.discovery.DiscoveredFeed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * The FeedImporter allows to add new feeds to the database.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 */
public class FeedImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedImporter.class);

    /** Maximum number of simultaneous threads when adding multiple feeds at once. */
    private int numThreads = 10;

    /** The store which keeps the feeds and items. */
    private final FeedStore store;

    /** The downloader for getting the feeds. */
    private FeedRetriever feedRetriever;

    /** Whether to store the items of the added feed, or only the feed data. */
    private boolean storeItems = false;

    /** Whether to classify the text extent of each added feed, see {@link FeedContentClassifier}. */
    private boolean classifyTextExtent = false;

    public FeedImporter(FeedStore store) {
        this.store = store;
        feedRetriever = new FeedRetriever();
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedUrl The url of the feed.
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedUrl) {
        LOGGER.trace(">addFeed " + feedUrl);
        boolean added = false;

        Feed feed = store.getFeedByUrl(feedUrl);
        if (feed == null) {
            try {

                StringBuilder infoMsg = new StringBuilder();
                infoMsg.append("added feed ").append(feedUrl);

                feed = feedRetriever.getFeed(feedUrl);

                // classify feed's text extent
                if (classifyTextExtent) {
                    FeedContentClassifier classifier = new FeedContentClassifier(feedRetriever);
                    FeedContentType contentType = classifier.determineContentType(feed);
                    feed.setContentType(contentType);
                    infoMsg.append(" (contentType:").append(contentType).append(")");
                }

                // classify the feed's activity pattern
                int activityPattern = FeedClassifier.classify(feed.getItems());
                feed.setActivityPattern(activityPattern);
                infoMsg.append(" (activityPattern:").append(activityPattern).append(")");

                feed.setWindowSize(feed.getItems().size());

                // set feed and site URL
                feed.setFeedUrl(feedUrl, true);

                // add feed & entries to the store
                store.addFeed(feed);

                if (storeItems) {
                    for (FeedItem feedEntry : feed.getItems()) {
                        store.addFeedItem(feed, feedEntry);
                    }
                }

                LOGGER.info(infoMsg);
                added = true;

            } catch (FeedRetrieverException e) {
                LOGGER.error("error adding feed " + feedUrl + " " + e.getMessage());
            }
        } else {
            LOGGER.info("i already have feed " + feedUrl);
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    public int addDiscoveredFeeds(Collection<DiscoveredFeed> discoveredFeeds) {
        Set<String> feedUrls = new HashSet<String>();
        for (DiscoveredFeed discoveredFeed : discoveredFeeds) {
            feedUrls.add(discoveredFeed.getFeedLink());
        }
        return addFeeds(feedUrls);
    }

    /**
     * Add a Collection of feedUrls for aggregation. This process runs threaded. Use {@link #setNumThreads(int)} to set
     * the maximum number of concurrently running threads.
     * 
     * @param feedUrls
     * @return Number of added feeds.
     */
    public int addFeeds(Collection<String> feedUrls) {

        // Queue to store the URLs we will add
        final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>();
        urlQueue.addAll(feedUrls);

        // Counter for # of added Feeds
        final AtomicInteger addCounter = new AtomicInteger();

        // stop time for adding
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

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
                LOGGER.error(e);
            }
        }

        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" added " + addCounter.get() + " new feeds");
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: " + DocumentRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES)
                + " MB");
        LOGGER.info("-------------------------------");

        return addCounter.get();

    }

    /**
     * Add feeds from a supplied file. The file must contain a newline separated list of feed URLs.
     * 
     * @param fileName The name of the file where the feed URLs are stored.
     * @return The number of feeds added.
     */
    public int addFeedsFromFile(String filePath) {
        List<String> feedUrls = FileHelper.readFileToArray(filePath);
        int added = addFeeds(feedUrls);
        LOGGER.info("file contained " + feedUrls.size() + " entries;");
        LOGGER.info("added " + added + " feeds.");
        return added;
    }

    /**
     * Set, whether to store the items of the added feeds.
     * 
     * @param storeItems
     */
    public void setStoreItems(boolean storeItems) {
        this.storeItems = storeItems;
    }

    /**
     * Set the maximum number of threads when adding multiple feeds.
     * 
     * @param numThreads
     */
    public void setNumThreads(int maxThreads) {
        this.numThreads = maxThreads;
    }

    /**
     * Set, whether to classify the text extent of the added feeds. See {@link FeedContentClassifier} for more
     * information.
     * 
     * @param classifyTextExtent
     */
    public void setClassifyTextExtent(boolean classifyTextExtent) {
        this.classifyTextExtent = classifyTextExtent;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        CommandLineParser parser = new BasicParser();

        // CLI usage: FeedImporter [-add <feed-Url>] [-addFile <file>] [-classifyText] [-storeItems] [-threads nn]
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("add").withDescription("adds a feed").hasArg().withArgName(
                "feedUrl").create());
        options.addOption(OptionBuilder.withLongOpt("addFile").withDescription("add multiple feeds from supplied file")
                .hasArg().withArgName("file").create());
        options.addOption(OptionBuilder.withLongOpt("classifyText").withDescription(
                "classify the text entent of each feed").create());
        options.addOption(OptionBuilder.withLongOpt("storeItems").withDescription(
                "also store the items of each added feed to the database").create());
        options.addOption(OptionBuilder.withLongOpt("threads").withDescription("number of threads").hasArg()
                .withArgName("nn").withType(Number.class).create());

        try {

            FeedImporter importer = new FeedImporter(DatabaseManagerFactory.create(FeedDatabase.class));

            CommandLine cmd = parser.parse(options, args);

            if (args.length < 1) {
                // no arguments given, print usage help in catch clause.
                throw new ParseException(null);
            }

            if (cmd.hasOption("classifyText")) {
                importer.setClassifyTextExtent(true);
            }
            if (cmd.hasOption("storeItems")) {
                importer.setStoreItems(true);
            }
            if (cmd.hasOption("threads")) {
                importer.setNumThreads(((Number) cmd.getParsedOptionValue("threads")).intValue());
            }
            if (cmd.hasOption("add")) {
                importer.addFeed(cmd.getOptionValue("add"));
            }
            if (cmd.hasOption("addFile")) {
                importer.addFeedsFromFile(cmd.getOptionValue("addFile"));
            }

            return;

        } catch (ParseException e) {
            // print usage help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(FeedImporter.class.getName() + " [options]", options);
        }

    }

}
