package tud.iir.news;

import java.util.Collection;
import java.util.List;
import java.util.Stack;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.log4j.Logger;

import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.news.FeedContentClassifier.FeedContentType;
import tud.iir.web.Crawler;

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
    private int maxThreads = 10;

    /** The store which keeps the feeds and items. */
    private final FeedStore store;

    /** The downloader for getting the feeds. */
    private FeedDownloader feedDownloader;

    /** Whether to store the items of the added feed, or only the feed data. */
    private boolean storeItems = false;

    /** Whether to classify the text extent of each added feed, see {@link FeedContentClassifier}. */
    private boolean classifyTextExtent = false;

    public FeedImporter(FeedStore store) {
        this.store = store;
        feedDownloader = new FeedDownloader();
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

                feed = feedDownloader.getFeed(feedUrl);

                // classify feed's text extent
                if (classifyTextExtent) {
                    FeedContentClassifier classifier = new FeedContentClassifier(feedDownloader);
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
                        store.addFeedEntry(feed, feedEntry);
                    }
                }

                LOGGER.info(infoMsg);
                added = true;

            } catch (FeedDownloaderException e) {
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
    public int addFeeds(Collection<String> feedUrls) {

        // TODO also use a thread pool here?

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
                        boolean added = addFeed(currentUrl);
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
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
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
        options.addOption(OptionBuilder.withLongOpt("add").withDescription("adds a feed").hasArg()
                .withArgName("feedUrl").create());
        options.addOption(OptionBuilder.withLongOpt("addFile").withDescription("add multiple feeds from supplied file")
                .hasArg().withArgName("file").create());
        options.addOption(OptionBuilder.withLongOpt("classifyText")
                .withDescription("classify the text entent of each feed").create());
        options.addOption(OptionBuilder.withLongOpt("storeItems")
                .withDescription("also store the items of each added feed to the database").create());
        options.addOption(OptionBuilder.withLongOpt("threads")
                .withDescription("maximum number of simultaneous threads").hasArg().withArgName("nn")
                .withType(Number.class).create());

        try {

            FeedImporter importer = new FeedImporter(FeedDatabase.getInstance());

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
                importer.setMaxThreads(((Number) cmd.getParsedOptionValue("threads")).intValue());
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
