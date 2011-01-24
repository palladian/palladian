package tud.iir.news;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.Counter;
import tud.iir.helper.DateHelper;
import tud.iir.helper.StopWatch;
import tud.iir.news.FeedContentClassifier.FeedContentType;
import tud.iir.web.Crawler;

/**
 * @deprecated Functionality from the NewsAggregator has been moved to other classes:
 * 
 * <ul>
 * <li>Downloading feeds and entries is done with {@link FeedDownloader}</li>
 * <li>Scraping page content for feeds is done with {@link FeedDownloader}</li>
 * <li>Adding feeds to database is done via {@link FeedImporter}</li>
 * <li>Aggregating feeds is done via {@link FeedReader}</li>
 * <ul>
 * 
 * TODO add a "lastSuccessfullAggregation" attribute to feed, so we can filter out obsolute feeds.<br>
 * TODO we should check if an entry was modified and update.<br>
 * TODO add a general filter to ignore specific types of feeds, for example by language, count of entries, URL pattern,
 * etc.<br>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
@Deprecated
public class NewsAggregator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(NewsAggregator.class);

    private static final int DEFAULT_MAX_THREADS = 20;

    /**
     * Maximum number of feed entries. Feeds with more entries will be ignored when adding. This is to speed up the
     * aggregation process, especially when downloading the linked web pages. There is no common, maximum limit for feed
     * entries, although there are hardly any feeds with more than 100 entries. On the other hand, as extreme example, I
     * found a feed with over 20,000 entries (http://ameblo.jp/ameblo_ror.xml). Hardcoded for now, could be moved to
     * a general filter class in the future, see todo-note.
     */
    // private static final int MAX_FEED_ENTRIES = 200;

    /**
     * Maximum number of concurrent threads for aggregation.
     */
    private int maxThreads = DEFAULT_MAX_THREADS;

    /**
     * If enabled, we use PageContentExtractor to get extract text for entries directly from their corresponding web
     * pages if necessary.
     */
    private boolean downloadPages = false;

    private final FeedStore store;

    /** Used for all downloading purposes. */
    private Crawler crawler = new Crawler();

    private FeedDownloader feedDownloader = new FeedDownloader();

    private NewsAggregator() {
        store = FeedDatabase.getInstance();
        loadConfig();
    }

    /** Used primarily for testing to set DummyFeedStore. */
    private NewsAggregator(FeedStore store) {
        this.store = store;
        loadConfig();
    }

    private void loadConfig() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        setMaxThreads(config.getInt("maxAggregationThreads", DEFAULT_MAX_THREADS));
        setDownloadPages(config.getBoolean("downloadAssociatedPages", false));
    }

    /**
     * Do the aggregation process. New entries from all known feeds will be aggregated. Use {@link #setMaxThreads(int)}
     * to set the number of maximum parallel threads.
     * 
     * TODO use Thread Pools?
     * http://developer.amd.com/documentation/articles/pages/1121200683.aspx
     * http://www.ibm.com/developerworks/library/j-jtp0730.html
     * 
     * @return number of aggregated new entries.
     */
    public int aggregate() {
        LOGGER.trace(">aggregate");

        List<Feed> feeds = store.getFeeds();
        LOGGER.info("# feeds in the store " + feeds.size());

        Stack<Feed> feedsStack = new Stack<Feed>();
        feedsStack.addAll(feeds);

        // count number of running Threads
        final Counter threadCounter = new Counter();

        // count number of new entries
        final Counter newEntriesTotal = new Counter();

        // count number of encountered errors
        final Counter errors = new Counter();

        // count number of scraped pages
        final Counter downloadedPages = new Counter();
        // final Counter scrapeErrors = new Counter();

        // stopwatch for aggregation process
        StopWatch stopWatch = new StopWatch();

        // reset traffic counter
        crawler.setTotalDownloadSize(0);

        while (feedsStack.size() > 0) {
            final Feed feed = feedsStack.pop();

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
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int newEntries = 0;
                    LOGGER.debug("aggregating entries from " + feed.getFeedUrl());
                    try {

                        // first, download feed with all entries, but without downloading link
                        List<FeedItem> downloadedItems = feedDownloader.getFeed(feed.getFeedUrl()).getItems();

                        // check, which we already have and add the missing ones.
                        List<FeedItem> toAdd = new ArrayList<FeedItem>();
                        for (FeedItem feedEntry : downloadedItems) {
                            boolean add = store.getFeedItemByRawId(feed.getId(), feedEntry.getRawId()) == null;
                            if (add) {
                                toAdd.add(feedEntry);
                            }
                        }
                        boolean fetchPages = isDownloadPages() && feed.getContentType() != FeedContentType.FULL;
                        if (fetchPages && !toAdd.isEmpty()) {
                            feedDownloader.fetchPageContentForEntries(toAdd);
                            downloadedPages.increment(toAdd.size());
                        }
                        for (FeedItem feedEntry : toAdd) {
                            store.addFeedItem(feed, feedEntry);
                            newEntries++;
                        }

                    } catch (FeedDownloaderException e) {
                        errors.increment();
                    } finally {
                        threadCounter.decrement();
                    }
                    if (newEntries > 0) {
                        LOGGER.info("# new entries in " + feed.getFeedUrl() + " " + newEntries);
                        newEntriesTotal.increment(newEntries);
                    }
                }
            };
            new Thread(runnable).start();
        }

        // keep on running until all Threads have finished and the stack is empty
        while (threadCounter.getCount() > 0 || feedsStack.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
            LOGGER.trace("waiting ... threads:" + threadCounter.getCount() + " stack:" + feedsStack.size());
        }
        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" # of aggregated feeds: " + feeds.size());
        LOGGER.info(" # new entries total: " + newEntriesTotal.getCount());
        LOGGER.info(" # errors: " + errors.getCount());
        LOGGER.info(" page downloading enabled: " + isDownloadPages());
        LOGGER.info(" # downloaded pages: " + downloadedPages);
        // LOGGER.info(" # scrape errors: " + scrapeErrors);
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: " + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB");
        LOGGER.info("-------------------------------");

        LOGGER.trace("<aggregate");
        return newEntriesTotal.getCount();
    }

    /**
     * Runs a continuous aggregation process. This is mainly intended for use as background process from the command
     * line.
     * 
     * @param waitMinutes the interval in seconds when the aggregation is done.
     * @return
     */
    public void aggregateContinuously(int waitMinutes) {
        while (true) {
            aggregate();
            LOGGER.info("sleeping for " + waitMinutes + " minutes");

            try {
                Thread.sleep(waitMinutes * DateHelper.MINUTE_MS);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
        }
    }

    /**
     * Sets the maximum number of parallel threads when aggregating or adding multiple new feeds.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * If enabled, we use {@link PageContentExtractor} to analyse feed type and to extract more text from feed entries
     * with only partial text representations. Keep in mind that this causes heavy traffic and therfor takes a lot more
     * time than a simple aggregation process from XML feeds only.
     * 
     * @param downloadPages
     */
    public void setDownloadPages(boolean downloadPages) {
        this.downloadPages = downloadPages;
    }

    public boolean isDownloadPages() {
        return downloadPages;
    }

    /**
     * Main method with command line interface.
     * 
     * @param args
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        CommandLineParser parser = new BasicParser();

        // CLI usage: NewsAggregator [-threads nn] [-noScraping] [-add <feed-Url>] [-addFile <file>] [-aggregate]
        // [-aggregateWait <minutes>]
        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("threads")
                .withDescription("maximum number of simultaneous threads").hasArg().withArgName("nn")
                .withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("downloadPages")
                .withDescription("download associated web page for each feed entry").create());
        options.addOption(OptionBuilder.withLongOpt("aggregate").withDescription("run aggregation process").create());
        options.addOption(OptionBuilder
                .withLongOpt("aggregateWait")
                .withDescription(
                        "run continuous aggregation process; wait for specified number of minutes between each aggregation step")
                .hasArg().withArgName("minutes").withType(Number.class).create());

        try {

            NewsAggregator aggregator = new NewsAggregator();

            CommandLine cmd = parser.parse(options, args);

            if (args.length < 1) {
                // no arguments given, print usage help in catch clause.
                throw new ParseException(null);
            }

            if (cmd.hasOption("threads")) {
                aggregator.setMaxThreads(((Number) cmd.getParsedOptionValue("threads")).intValue());
            }
            if (cmd.hasOption("downloadPages")) {
                aggregator.setDownloadPages(true);
            }
            if (cmd.hasOption("aggregate")) {
                aggregator.aggregate();
            }
            if (cmd.hasOption("aggregateWait")) {
                int waitMinutes = ((Number) cmd.getParsedOptionValue("aggregateWait")).intValue();
                aggregator.aggregateContinuously(waitMinutes);
            }

            return;

        } catch (ParseException e) {
            // print usage help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("NewsAggregator [options]", options);
        }

    }

}