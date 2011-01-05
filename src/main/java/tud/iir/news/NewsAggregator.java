package tud.iir.news;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.extraction.PageAnalyzer;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.Counter;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.XPathHelper;
import tud.iir.web.Crawler;
import tud.iir.web.HeaderInformation;
import tud.iir.web.URLDownloader;
import tud.iir.web.URLDownloader.URLDownloaderCallback;

import com.sun.syndication.feed.WireFeed;
import com.sun.syndication.feed.rss.Guid;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;

/**
 * NewsAggregator uses ROME library to fetch and parse feeds from the web. Feeds are stored persistently, aggregation
 * method fetches new entries.
 * 
 * TODO add a "lastSuccessfullAggregation" attribute to feed, so we can filter out obsolute feeds.
 * TODO we should check if an entry was modified and update.
 * TODO determine feed format for statistics? -->
 * https://rome.dev.java.net/apidocs/1_0/com/sun/syndication/feed/WireFeed.html#getFeedType()
 * TODO add a general filter to ignore specific types of feeds, for example by language, count of entries, URL pattern,
 * etc.
 * 
 * https://rome.dev.java.net/ *
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
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
    private static final int MAX_FEED_ENTRIES = 200;

    /**
     * Maximum number of concurrent threads for aggregation.
     */
    private int maxThreads = DEFAULT_MAX_THREADS;

    /**
     * If enabled, we use PageContentExtractor to get extract text for entries directly from their corresponding web
     * pages if necessary.
     */
    private boolean downloadPages = false;


    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * If enabled, we use the last poll time and the last ETag of the feed as HTTP headers when requesting the URL. This
     * way we can save bandwidth for feeds that support the HTTP 304 "Not modified" status code (about 83% of all feeds
     * do support either ETag or LastModifiedSince).
     */
    private boolean useBandwidthSavingHTTPHeaders = false;

    private final FeedStore store;


    /** Used for all downloading purposes. */
    private Crawler crawler = new Crawler();

    /** We keep an instance of the plain parsed XML feed to do more operations if SyndFeed fails. */
    private Document plainXMLFeed;

    public NewsAggregator() {
        store = FeedDatabase.getInstance();
        loadConfig();
    }

    /** Used primarily for testing to set DummyFeedStore. */
    public NewsAggregator(FeedStore store) {
        this.store = store;
        loadConfig();
    }

    private void loadConfig() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        setMaxThreads(config.getInt("maxAggregationThreads", DEFAULT_MAX_THREADS));
        setDownloadPages(config.getBoolean("downloadAssociatedPages", false));
    }

    /**
     * Downloads a feed from the web and parses with ROME.
     * 
     * To access feeds from outside use {@link #downloadFeed(String)}.
     * 
     * @param feedUrl
     * @return
     * @throws NewsAggregatorException when Feed could not be retrieved, e.g. when server is down or feed cannot be
     *             parsed.
     */
    private SyndFeed getFeedWithRome(Feed feed) throws NewsAggregatorException {
        return getFeedWithRome(feed.getFeedUrl(), feed);
    }
    private SyndFeed getFeedWithRome(String feedUrl) throws NewsAggregatorException {
        return getFeedWithRome(feedUrl, null);
    }

    private SyndFeed getFeedWithRome(String feedUrl, Feed feed) throws NewsAggregatorException {
        LOGGER.trace(">getFeedWithRome " + feedUrl);

        SyndFeed result;

        try {

            SyndFeedInput feedInput = new SyndFeedInput();

            // this preserves the "raw" feed data and gives direct access
            // to RSS/Atom specific elements
            // see http://wiki.java.net/bin/view/Javawsxml/PreservingWireFeeds
            feedInput.setPreserveWireFeed(true);

            // create the header information to download feed only if it has changed
            HeaderInformation headerInformation = null;

            if (isUseBandwidthSavingHTTPHeaders() && feed != null) {
                headerInformation = new HeaderInformation(feed.getLastPollTime(), feed.getLastETag());
            }

            // get the XML input via the crawler, this allows to input files with the "path/to/filename.xml" schema as
            // well, which we use inside Palladian.
            plainXMLFeed = crawler.getXMLDocument(feedUrl, false, headerInformation);
            if (plainXMLFeed == null) {
                if (crawler.getLastResponseCode() != HttpURLConnection.HTTP_NOT_MODIFIED) {
                    throw new NewsAggregatorException("could not get document from " + feedUrl);
                } else {
                    // TODO return cached document here (from disk or database)
                    LOGGER.debug("the feed was not modified: " + feedUrl);
                    return null;
                }
            }
            result = feedInput.build(plainXMLFeed);

        } catch (IllegalArgumentException e) {
            LOGGER.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
            throw new NewsAggregatorException(e);
        } catch (FeedException e) {
            LOGGER.error("getFeedWithRome " + feedUrl + " " + e.toString() + " " + e.getMessage());
            throw new NewsAggregatorException(e);
        }

        LOGGER.trace("<getFeedWithRome");
        return result;
    }

    /**
     * Get feed information about a Atom/RSS feed, using ROME library.
     * 
     * @param feedUrl
     * @return
     */
    private Feed getFeed(SyndFeed syndFeed, String feedUrl) {

        LOGGER.trace(">getFeed " + feedUrl);

        WireFeed wireFeed = syndFeed.originalWireFeed();

        Feed result = new Feed();
        result.setFeedUrl(feedUrl);
        if (syndFeed.getLink() != null) {
            result.setSiteUrl(syndFeed.getLink().trim());
        }
        if (syndFeed.getTitle() != null && syndFeed.getTitle().length() > 0) {
            result.setTitle(syndFeed.getTitle().trim());
        } else {
            // fallback, use feedUrl as title
            result.setTitle(feedUrl);
        }
        result.setLanguage(syndFeed.getLanguage());

        // determine feed format
        if (wireFeed instanceof com.sun.syndication.feed.rss.Channel) {
            result.setFormat(Feed.FORMAT_RSS);
        } else if (wireFeed instanceof com.sun.syndication.feed.atom.Feed) {
            result.setFormat(Feed.FORMAT_ATOM);
        }

        // get the size of the feed
        if (plainXMLFeed != null) {
            result.setByteSize(PageAnalyzer.getRawMarkup(plainXMLFeed).getBytes().length);
        }

        LOGGER.trace("<getFeed " + result);
        return result;

    }


    private Date getPublishDate(SyndEntry syndEntry) {

        Date pubDate = null;

        Node node = getFeedEntryNode(syndEntry);

        Node pubDateNode = XPathHelper.getChildNode(node, "*[contains(name(),'date') or contains(name(),'Date')]");

        try {
            pubDate = DateGetterHelper.findDate(pubDateNode.getTextContent()).getNormalizedDate();
        } catch (NullPointerException e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode);
        } catch (DOMException e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode);
        } catch (Exception e) {
            LOGGER.warn("date format could not be parsed correctly: " + pubDateNode);
        }

        return pubDate;
    }

    /**
     * <p>
     * Extracts the DOM node of the provided feed entry from the feed currently processed by the aggregator.
     * </p>
     * 
     * @param syndEntry The feed entry to extract.
     * @return The extracted DOM node representing the provided feed entry.
     */
    private Node getFeedEntryNode(SyndEntry syndEntry) {

        // for rss
        Node node = null;

        try {

            node = XPathHelper.getNode(plainXMLFeed, "//item[link=\"" + syndEntry.getLink() + "\"]");

            if (node == null) {
                node = XPathHelper.getNode(plainXMLFeed,
                        "//item[title=\"" + syndEntry.getTitle().replaceAll("\"", "&quot;") + "\"]");

                // for atom
                if (node == null) {
                    node = XPathHelper.getNode(plainXMLFeed, "//entry[id=\"" + syndEntry.getUri() + "\"]");
                }
            }

        } catch (Exception e) {
            LOGGER.error("synd entry was not complete, " + e.getMessage());
        }

        if (node == null) {
            System.out.println("STOP");
        }

        return node;
    }

    /**
     * Get entries of specified Atom/RSS feed.
     * 
     * @param feedUrl
     * @return
     */
    @SuppressWarnings("unchecked")
    private List<FeedItem> getEntries(SyndFeed syndFeed) {
        LOGGER.trace(">getEntries");

        List<FeedItem> result = new LinkedList<FeedItem>();

        // only try a certain amount of times to extract a pub date, if none is found don't keep trying
        int failedDateExtractions = 0;

        List<SyndEntry> syndEntries = syndFeed.getEntries();
        for (SyndEntry syndEntry : syndEntries) {

            FeedItem entry = new FeedItem();
            // remove HTML tags and unescape HTML entities from title
            String title = syndEntry.getTitle();
            if (title != null) {
                title = HTMLHelper.removeHTMLTags(title, true, true, true, true);
                title = StringEscapeUtils.unescapeHtml(title);
                title = title.trim();
            }

            entry.setTitle(title);

            // some feeds provide relative URLs -- convert.
            String entryLink = syndEntry.getLink();
            if (entryLink != null && entryLink.length() > 0) {
                entryLink = entryLink.trim();
                entryLink = Crawler.makeFullURL(syndFeed.getLink(), entryLink);
            }
            // TODO **** feedproxy URLs ****
            // some feeds use Google Feed Proxy ...
            // this URL -->
            // http://feedproxy.google.com/~r/typepad/romanmica/the_first_lemming/~3/x8XidemLPik/and-the-new-2011-msrp-of-the-hottest-subaru-wrx-sti-is.html
            // is redirected to this URL -->
            // http://www.tflcar.com/2010/07/and-the-new-2011-msrp-of-the-hottest-subaru-wrx-sti-is.html?utm_source=feedburner&utm_medium=feed&utm_campaign=Feed%3A+typepad%2Fromanmica%2Fthe_first_lemming+%28TFLcar.com%3A+Automotive+news%2C+views+and+reviews%29
            // should we resolve this while aggregating? problem --> costly, one HTTP requests for each entry
            // but: problematic when ranking based upon URLs
            // web service to resolve --> http://www.therealurl.net/
            // maybe better: we should already have the URL, when fetching the page content?!
            entry.setLink(entryLink);

            Date publishDate = syndEntry.getPublishedDate();
            if (publishDate == null) {

                // FIXME there are still some entries without date (David: why? does rome not get some date formats?)

                // try to find the date since Rome library failed
                publishDate = getPublishDate(syndEntry);

                // if no publish date is provided, we take the update instead
                if (publishDate == null) {
                    publishDate = syndEntry.getUpdatedDate();
                    failedDateExtractions++;
                } else {
                    LOGGER.debug("found publish date in original feed file: " + publishDate);
                }

            }
            entry.setPublished(publishDate);

            String entryText = getEntryText(syndEntry);
            entry.setItemText(entryText);

            // get ID information from raw feed entries
            String rawId = null;
            Object wireEntry = syndEntry.getWireEntry();
            if (wireEntry instanceof com.sun.syndication.feed.atom.Entry) {
                com.sun.syndication.feed.atom.Entry atomEntry = (com.sun.syndication.feed.atom.Entry) wireEntry;
                rawId = atomEntry.getId();
            } else if (wireEntry instanceof com.sun.syndication.feed.rss.Item) {
                com.sun.syndication.feed.rss.Item rssItem = (com.sun.syndication.feed.rss.Item) wireEntry;
                Guid guid = rssItem.getGuid();
                if (guid != null) {
                    rawId = guid.getValue();
                }
            }
            // fallback -- if we can get no ID from the feed,
            // we take the Link as identification instead
            if (rawId == null) {
                rawId = syndEntry.getLink();
                LOGGER.trace("id is missing, taking link instead");
            }
            if (rawId != null) {
                entry.setRawId(rawId.trim());
            } else {
                LOGGER.warn("could not get id for entry");
            }

            // Set raw xml content to feed entry.
            Node feedEntryNode = getFeedEntryNode(syndEntry);
            entry.setPlainXML(PageAnalyzer.getRawMarkup(feedEntryNode));

            // logger.trace(entry);
            result.add(entry);
        }

        LOGGER.trace("<getEntries");
        return result;
    }

    /**
     * Try to get the text content from SyndEntry; either from content/summary/description element. Returns null if no
     * text content exists.
     * 
     * @param syndEntry
     * @return
     */
    @SuppressWarnings("unchecked")
    private String getEntryText(SyndEntry syndEntry) {
        LOGGER.trace(">getEntryText");

        // get content from SyndEntry
        // either from content or from description
        String entryText = null;
        List<SyndContent> contents = syndEntry.getContents();
        if (contents != null) {
            for (SyndContent content : contents) {
                if (content.getValue() != null && content.getValue().length() != 0) {
                    entryText = content.getValue();
                }
            }
        }
        if (entryText == null) {
            if (syndEntry.getDescription() != null) {
                entryText = syndEntry.getDescription().getValue();
            }
        }

        // clean up --> strip out HTML tags, unescape HTML code
        if (entryText != null) {
            entryText = HTMLHelper.htmlToString(entryText, false);
            entryText = StringEscapeUtils.unescapeHtml(entryText);
            entryText = entryText.trim();
        }
        LOGGER.trace("<getEntryText ");
        return entryText;
    }

    /**
     * Adds a new feed for aggregation.
     * 
     * @param feedUrl
     * @return true, if feed was added.
     */
    public boolean addFeed(String feedUrl) {
        LOGGER.trace(">addFeed " + feedUrl);
        boolean added = false;

        Feed feed = store.getFeedByUrl(feedUrl);
        if (feed == null) {
            try {

                feed = downloadFeed(feedUrl);

                if (feed.getEntries().size() <= MAX_FEED_ENTRIES) {

                    // classify feed's text extent
                    FeedContentClassifier classifier = new FeedContentClassifier();
                    int textType = classifier.determineFeedTextType(feed);
                    feed.setTextType(textType);
                    
                    // classify the feed's activity pattern
                    feed.setActivityPattern(FeedClassifier.classify(feed.getEntries()));
                    
                    feed.setWindowSize(feed.getEntries().size());
                    
                    // set feed and site URL
                    feed.setFeedUrl(feedUrl,true);

                    // add feed & entries to the store
                    store.addFeed(feed);

                    for (FeedItem feedEntry : feed.getEntries()) {
                        store.addFeedEntry(feed, feedEntry);
                    }

                    LOGGER.info("added feed to store " + feedUrl + " (textType:"
                            + classifier.getReadableFeedTextType(textType) + ")");
                    added = true;
                }

            } catch (NewsAggregatorException e) {
                LOGGER.error("error adding feed " + feedUrl + " " + e.getMessage());
            }
        } else {
            LOGGER.info("i already have feed " + feedUrl);
        }

        LOGGER.trace("<addFeed " + added);
        return added;
    }

    public boolean updateFeed(Feed feed) {
        return store.updateFeed(feed);
    }

    /**
     * Add a Collection of feedUrls for aggregation. This process runs threaded. Use {@link #setMaxThreads(int)} to set
     * the maximum number of concurrently running threads.
     * 
     * @param feedUrls
     * @return Number of added feeds.
     */
    public int addFeeds(Collection<String> feedUrls) {

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

        // reset traffic counter
        crawler.setTotalDownloadSize(0);

        while (urlStack.size() > 0) {
            final String currentUrl = urlStack.pop();

            // if maximum # of Threads are already running, wait here
            while (threadCounter.getCount() >= maxThreads) {
                LOGGER.trace("max # of Threads running. waiting ...");
                ThreadHelper.sleep(1000);
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
            ThreadHelper.sleep(1000);
            LOGGER.trace("waiting ... threads:" + threadCounter.getCount() + " stack:" + urlStack.size());
        }

        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" added " + addCounter.getCount() + " new feeds");
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: " + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB");
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
        LOGGER.trace(">addFeedsFromFile");
        List<String> feedUrls = FileHelper.readFileToArray(filePath);
        LOGGER.info("adding " + feedUrls.size() + " feeds");
        int result = addFeeds(feedUrls);
        LOGGER.trace("<addFeedsFromFile " + result);
        return result;
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
                ThreadHelper.sleep(1000);
            }

            threadCounter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int newEntries = 0;
                    LOGGER.debug("aggregating entries from " + feed.getFeedUrl());
                    try {

                        // first, download feed with all entries, but without downloading link
                        List<FeedItem> downloadedEntries = downloadFeed(feed.getFeedUrl(), false).getEntries();

                        // check, which we already have and add the missing ones.
                        List<FeedItem> toAdd = new ArrayList<FeedItem>();
                        for (FeedItem feedEntry : downloadedEntries) {
                            boolean add = store.getFeedEntryByRawId(feed.getId(), feedEntry.getRawId()) == null;
                            if (add) {
                                // boolean fetchPage = isDownloadPages() && feed.getTextType() != Feed.TEXT_TYPE_FULL;
                                // if (fetchPage) {
                                // fetchPageContentForEntry(feedEntry);
                                // downloadedPages.increment();
                                // }
                                // store.addFeedEntry(feed, feedEntry);
                                // newEntries++;
                                toAdd.add(feedEntry);
                            }
                        }
                        boolean fetchPages = isDownloadPages() && feed.getTextType() != Feed.TEXT_TYPE_FULL;
                        if (fetchPages && !toAdd.isEmpty()) {
                            fetchPageContentForEntries(toAdd);
                            downloadedPages.increment(toAdd.size());
                        }
                        for (FeedItem feedEntry : toAdd) {
                            store.addFeedEntry(feed, feedEntry);
                            newEntries++;
                        }

                        // SyndFeed syndFeed = getFeedWithRome(feed.getFeedUrl());
                        // newEntries = addEntries(feed, syndFeed);
                    } catch (NewsAggregatorException e) {
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
            ThreadHelper.sleep(1000);
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
            ThreadHelper.sleep(waitMinutes * DateHelper.MINUTE_MS);
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
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @param useBandwidthSavingHTTPHeaders
     */
    public void setUseBandwidthSavingHTTPHeaders(boolean useBandwidthSavingHTTPHeaders) {
        this.useBandwidthSavingHTTPHeaders = useBandwidthSavingHTTPHeaders;
    }

    /**
     * WARNING: this does not work yet. In order to make it work we would need to cache the last successfully retrieved
     * documents.
     * 
     * @return
     */
    public boolean isUseBandwidthSavingHTTPHeaders() {
        return useBandwidthSavingHTTPHeaders;
    }

    /**
     * Returns a feed and its entries from a specified feed URL. Use {@link Feed#getEntries()} to get feed's entries.
     * 
     * @param feedUrl
     * @return
     * @throws NewsAggregatorException
     */
    public Feed downloadFeed(String feedUrl) throws NewsAggregatorException {
        return downloadFeed(feedUrl, isDownloadPages());
        // SyndFeed syndFeed = getFeedWithRome(feedUrl);
        // Feed feed = getFeed(syndFeed, feedUrl);
        // List<FeedEntry> entries = getEntries(syndFeed);
        //
        // if (isDownloadPages()) {
        // for (FeedEntry feedEntry : entries) {
        // LOGGER.debug("downloading page " + feedEntry.getLink());
        //
        // try {
        // PageContentExtractor extractor = new PageContentExtractor();
        // extractor.setDocument(feedEntry.getLink());
        // Document page = extractor.getResultDocument();
        // feedEntry.setPageContent(page);
        // } catch (PageContentExtractorException e) {
        // LOGGER.error("error downloading page " + feedEntry.getLink() + " : " + e);
        // }
        // }
        // }
        //
        // feed.setEntries(entries);
        // return feed;
    }

    public Feed downloadFeed(String feedUrl, boolean fetchPages) throws NewsAggregatorException {
        SyndFeed syndFeed = getFeedWithRome(feedUrl);
        Feed feed = getFeed(syndFeed, feedUrl);
        List<FeedItem> entries = getEntries(syndFeed);

        // if (fetchPages) {
        // dont fetch pages if we have masses of entries ...
        if (fetchPages && entries.size() <= MAX_FEED_ENTRIES) {
            // for (FeedEntry feedEntry : entries) {
            // fetchPageContentForEntry(feedEntry);
            // }
            fetchPageContentForEntries(entries);
        }

        feed.setEntries(entries);
        feed.setPlainXML(PageAnalyzer.getRawMarkup(plainXMLFeed));
        return feed;
    }

    public Feed downloadFeed(Feed feedData) throws NewsAggregatorException {
        SyndFeed syndFeed = getFeedWithRome(feedData);
        Feed feed = getFeed(syndFeed, feedData.getFeedUrl());
        List<FeedItem> entries = getEntries(syndFeed);
        feed.setEntries(entries);
        feed.setPlainXML(PageAnalyzer.getRawMarkup(plainXMLFeed));
        return feed;
    }


    /**
     * Fetch associated page content using {@link PageContentExtractor} for specified FeedEntries. Do not download
     * binary files, like PDFs, audio or video files. Downloading is done concurrently.
     * 
     * @param feedEntry
     */
    private void fetchPageContentForEntries(List<FeedItem> feedEntries) {
        LOGGER.debug("downloading " + feedEntries.size() + " pages");

        URLDownloader downloader = new URLDownloader();
        downloader.setMaxThreads(5);
        // PageContentExtractor extractor = new PageContentExtractor();
        final Map<String, FeedItem> entries = new HashMap<String, FeedItem>();

        for (FeedItem feedEntry : feedEntries) {
            String entryLink = feedEntry.getLink();

            if (entryLink == null) {
                continue;
            }

            // check type of linked file; ignore audio, video or pdf files ...
            String fileType = FileHelper.getFileType(entryLink);
            boolean ignore = FileHelper.isAudioFile(fileType) || FileHelper.isVideoFile(fileType)
            || fileType.equals("pdf");
            if (ignore) {
                LOGGER.debug("ignoring filetype " + fileType + " from " + entryLink);
            } else {
                downloader.add(feedEntry.getLink());
                entries.put(feedEntry.getLink(), feedEntry);
            }
        }

        downloader.start(new URLDownloaderCallback() {
            @Override
            public void finished(String url, InputStream inputStream) {
                try {
                    PageContentExtractor extractor = new PageContentExtractor();
                    extractor.setDocument(new InputSource(inputStream));
                    String pageText = extractor.getResultText();
                    entries.get(url).setPageText(pageText);
                    // feedEntry.setPageContent(page);
                } catch (PageContentExtractorException e) {
                    LOGGER.error("PageContentExtractorException " + e);
                }
            }
        });
        LOGGER.debug("finished downloading");
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
                .withDescription("maximum number of simultaneous threads").hasArg().withArgName("nn").withType(
                        Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("downloadPages").withDescription(
        "download associated web page for each feed entry").create());
        options.addOption(OptionBuilder.withLongOpt("add").withDescription("adds a feed").hasArg().withArgName(
        "feedUrl").create());
        options.addOption(OptionBuilder.withLongOpt("addFile").withDescription("add multiple feeds from supplied file")
                .hasArg().withArgName("file").create());
        options.addOption(OptionBuilder.withLongOpt("aggregate").withDescription("run aggregation process").create());
        options
        .addOption(OptionBuilder
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
            if (cmd.hasOption("add")) {
                aggregator.addFeed(cmd.getOptionValue("add"));
            }
            if (cmd.hasOption("addFile")) {
                aggregator.addFeedsFromFile(cmd.getOptionValue("addFile"));
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

    /**
     * <p>
     * 
     * </p>
     *
     * @return
     */
    public Document getPlainXMLFeed() {
        return plainXMLFeed;
    }

}