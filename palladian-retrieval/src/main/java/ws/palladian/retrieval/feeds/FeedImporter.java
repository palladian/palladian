package ws.palladian.retrieval.feeds;

import java.net.MalformedURLException;
import java.net.URL;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.ProgressHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.discovery.DiscoveredFeed;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>The FeedImporter allows to add new feeds to the database.</p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 */
public class FeedImporter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedImporter.class);

    /** Maximum number of simultaneous threads when adding multiple feeds at once. */
    private int numThreads = 10;

    /** The store which keeps the feeds and items. */
    private final FeedStore store;

    /** The downloader for getting the feeds. */
    private FeedParser feedParser;

    /** Whether to store the items of the added feed, or only the feed data. */
    private boolean storeItems = false;

    /** If <code>true</code>, download feed, if <code>false</code>, just import it into database */
    private boolean downloadFeeds = true;

    public FeedImporter(FeedStore store) {
        this.store = store;
        feedParser = new RomeFeedParser();
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedInformation Usually, this is the feedUrl but it may contain additional information like format and
     *            sizeURL, separated by $$$. *
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedInformation) {
        LOGGER.trace(">addFeed " + feedInformation);
        boolean added = false;


        String cleanedURL = feedInformation;
        String siteURL = null;
        if (feedInformation.contains("$$$")) {
            String[] feedLine = feedInformation.split("\\$\\$\\$");
            if (feedLine.length != 3) {
                LOGGER.error("Skipping illeagal feedInformation: " + feedInformation);
                LOGGER.trace("<addFeed " + added);
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
            if (isDownloadFeeds()) {
                try {
                    feed = feedParser.getFeed(cleanedURL);

                    // classify the feed's activity pattern
                    FeedActivityPattern activityPattern = FeedClassifier.classify(feed);
                    feed.setActivityPattern(activityPattern);
                    infoMsg.append(" (activityPattern:").append(activityPattern).append(")");

                    feed.setWindowSize(feed.getItems().size());

                } catch (FeedParserException e) {
                    LOGGER.error("error adding feed " + cleanedURL + " " + e.getMessage());
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
                        for (FeedItem feedItem : feed.getItems()) {
                            added = store.addFeedItem(feedItem);
                        }
                    }

                    if (added) {
                        LOGGER.debug(infoMsg.toString());
                    } else {
                        LOGGER.error("database error while adding feed " + cleanedURL);
                    }

                } catch (MalformedURLException e) {
                    LOGGER.error("error adding feed " + cleanedURL + " " + e.getMessage());
                    added = false;
                }
            }

        } else {
            LOGGER.debug("i already have feed " + cleanedURL);
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    public int addDiscoveredFeeds(Collection<DiscoveredFeed> discoveredFeeds) {
        Set<String> feedUrls = new HashSet<String>();
        int counter = 1;
        for (DiscoveredFeed discoveredFeed : discoveredFeeds) {
            feedUrls.add(discoveredFeed.getFeedLink());
            ProgressHelper.printProgress(counter++, discoveredFeeds.size(), 1);
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
                LOGGER.warn("Encountered InterruptedException");
            }
        }

        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" added " + addCounter.get() + " new feeds");
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: " + HttpRetriever.getSessionDownloadSize(SizeUnit.MEGABYTES)
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
     * @return the downloadFeeds
     */
    public final boolean isDownloadFeeds() {
        return downloadFeeds;
    }

    /**
     * @param downloadFeeds the downloadFeeds to set
     */
    public final void setDownloadFeeds(boolean downloadFeeds) {
        this.downloadFeeds = downloadFeeds;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        CommandLineParser parser = new BasicParser();

        // CLI usage:
        // FeedImporter [-add <feed-Url>] [-addFile <file>] [-classifyText] [-storeItems] [-threads nn] [-noDownload]
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("add").withDescription("adds a feed").hasArg().withArgName(
                "feedUrl").create());
        options.addOption(OptionBuilder.withLongOpt("addFile").withDescription("add multiple feeds from supplied file")
                .hasArg().withArgName("file").create());
        options.addOption(OptionBuilder.withLongOpt("storeItems").withDescription(
                "also store the items of each added feed to the database").create());
        options.addOption(OptionBuilder.withLongOpt("threads").withDescription("number of threads").hasArg()
                .withArgName("nn").withType(Number.class).create());
        options.addOption(OptionBuilder
                .withLongOpt("noDownload")
                .withDescription(
                        "do not poll and downlaod feed, just add it to database. Must not be combined with -storeItems and -classifyText")
                        .create());

        try {

            FeedImporter importer = new FeedImporter(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()));

            CommandLine cmd = parser.parse(options, args);

            if (args.length < 1) {
                // no arguments given, print usage help in catch clause.
                throw new ParseException(null);
            }
            if (cmd.hasOption("noDownload")) {
                importer.setDownloadFeeds(false);
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
