package ws.palladian.web.feeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.Counter;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.web.Crawler;
import ws.palladian.web.SourceRetriever;
import ws.palladian.web.SourceRetrieverManager;

/**
 * FeedDiscovery works like the following:
 * <ol>
 * <li>Query search engine with some terms (I use Yahoo, as I can get large amounts of results)</li>
 * <li>Get root URLs for hits</li>
 * <li>Check page for feeds using RSS/Atom autodiscovery feature</li>
 * </ol>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 */
public class FeedDiscovery {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDiscovery.class);

    private static final int MAX_NUMBER_OF_THREADS = 10;

    /** crawler for downloading pages. */
    private Crawler crawler = new Crawler();

    private boolean debugDump = false;

    /** Whether to extract all feeds on a page, or just the first, "preferred" one. */
    private boolean onlyPreferred = true;

    /** Define which search engine to use, see {@link SourceRetrieverManager} for available constants. */
    private int searchEngine = SourceRetrieverManager.YAHOO_BOSS;

    private int maxThreads = MAX_NUMBER_OF_THREADS;

    /** Store all sites for which we will do the autodiscovery. */
    private Set<String> sites = Collections.synchronizedSet(new HashSet<String>());

    /** The path of the file where the discovered feeds should be written to. */
    private String resultFilePath = "";

    /**
     * If true and resultFilePath != "", the feeds will be written to the file as they are discovered. That prevents
     * losing all data on a crash.
     */
    private boolean writeResultFileContinuously = true;

    /** Store a collection of all queries that are used to retrieve sites from a search engine. */
    private List<String> queries = Collections.synchronizedList(new ArrayList<String>());

    /** Store all discovered feeds URLs. */
    private Set<String> feeds = Collections.synchronizedSet(new HashSet<String>());

    /** Limit results for each query. */
    private int resultLimit = 10;

    /** If true, given queries will be combined with each other. For n queries that makes n + n-1 + n-2... */
    private boolean combineQueries = false;

    /** Ignore list, will be matched with URLs. */
    private Set<String> ignoreList = new HashSet<String>();

    // //////////////////////////////
    // some statistics
    // //////////////////////////////

    /** total # of sites we checked */
    private Counter totalSites = new Counter();
    /** # of sites containing a feed */
    private Counter feedSites = new Counter();
    /** # of sites with Atom feed */
    private Counter atomFeeds = new Counter();
    /** # of sites with RSS feed */
    private Counter rssFeeds = new Counter();
    /** # of sites with multiple feeds */
    private Counter multipleFeeds = new Counter();
    /** # of errors */
    private Counter errors = new Counter();
    /** # of ignored feeds */
    private Counter ignoredFeeds = new Counter();

    /** total time spent for searching */
    private long searchTime = 0;
    /** total time spent for discovery process */
    private long discoveryTime = 0;

    @SuppressWarnings("unchecked")
    public FeedDiscovery() {
        LOGGER.trace(">FeedDiscovery");

        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            setMaxThreads(config.getInt("feedDiscovery.maxDiscoveryThreads", MAX_NUMBER_OF_THREADS));
            setIgnores(config.getList("feedDiscovery.discoveryIgnoreList"));
            setOnlyPreferred(config.getBoolean("feedDiscovery.onlyPreferred", true));
            setSearchEngine(config.getInt("feedDiscovery.searchEngine", SourceRetrieverManager.YAHOO_BOSS));
        } else {
            LOGGER.warn("could not load configuration, use defaults");
        }

        LOGGER.trace("<FeedDiscovery");
    }

    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }

    public void setWriteResultFileContinuously(boolean writeResultFileOnTheFly) {
        this.writeResultFileContinuously = writeResultFileOnTheFly;
    }

    public boolean isWriteResultFileContinuously() {
        return writeResultFileContinuously;
    }

    /**
     * Add a query for the search engine.
     * 
     * @param query The query to add.
     */
    public void addQuery(String query) {
        this.queries.add(query);
    }

    /**
     * Add queries for the search engine.
     * 
     * @param queries A collection of queries.
     */
    public void addQueries(Collection<String> queries) {
        this.queries.addAll(queries);
    }

    /**
     * Search for Sites by specified query.
     * 
     * @param query
     * @param totalResults
     * @return
     */
    private Set<String> searchSites(String query, int totalResults) {
        LOGGER.trace(">searchSites " + query + " " + totalResults);

        StopWatch stopWatch = new StopWatch();

        LOGGER.info("querying for " + query + " num results " + totalResults);

        // create source retriever object
        SourceRetriever sourceRetriever = new SourceRetriever();

        // set maximum number of expected results
        sourceRetriever.setResultCount(totalResults);

        // set search result language to english
        sourceRetriever.setLanguage(SourceRetriever.LANGUAGE_ENGLISH);

        // set the query source to the Bing search engine
        // sourceRetriever.setSource(SourceRetrieverManager.GOOGLE_BLOGS);
        sourceRetriever.setSource(getSearchEngine());

        // search for "Jim Carrey" in exact match mode (second parameter = true)
        List<String> resultURLs = sourceRetriever.getURLs(query, true);

        // print the results
        // CollectionHelper.print(resultURLs);

        Set<String> sites = new HashSet<String>();
        for (String resultUrl : resultURLs) {
            // sites.add(Helper.getRootUrl(resultUrl));
            sites.add(Crawler.getDomain(resultUrl));
        }

        stopWatch.stop();
        searchTime += stopWatch.getElapsedTime();

        LOGGER.trace("<searchSites");
        return sites;
    }

    /**
     * Discovers feed links in supplied page URL.
     * 
     * @param pageUrl
     * @return list of discovered feed URLs, empty list if no feeds are
     *         available, <code>null</code> if page could not be parsed.
     */
    public List<String> discoverFeeds(String pageUrl) {

        LOGGER.trace(">discoverFeeds " + pageUrl);

        // logger.debug("-------------");
        // logger.debug("pageUrl: " + pageUrl);

        List<String> result = null;

        try {

            // Crawler crawler = new Crawler();

            Document doc = crawler.getWebDocument(pageUrl, false);
            result = discoverFeeds(doc);
            // traffic.increment((int) crawler.getTotalDownloadSize());

        } catch (Throwable t) {
            // NekoHTML produces various types of Exceptions, just catch them
            // all here and log them.
            LOGGER.error("error at " + pageUrl + " " + t.toString() + " : " + t.getMessage());
            errors.increment();
        }

        // logger.debug("-------------");
        LOGGER.trace("<discoverFeeds");

        return result;
    }

    /**
     * Uses autodiscovery feature with MIME types "application/atom+xml" and
     * "application/rss+xml" to find linked feeds in the specified Document. If
     * Atom and RSS feeds are present, we prefer Atom :)
     * 
     * @param document
     * @return list of discovered feed URLs or empty list.
     */
    protected List<String> discoverFeeds(Document document) {

        List<String> result = new LinkedList<String>();

        totalSites.increment();

        String pageUrl = document.getDocumentURI();
        // fix for testing purposes
        if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://") && !pageUrl.startsWith("file:")) {
            pageUrl = "file:" + pageUrl;
        }

        // ////// for debugging
        if (debugDump) {
            HTMLHelper.writeXmlDump(document, "dumps/" + pageUrl.replaceAll("https?://", "") + ".xml");
        }

        Node baseNode = XPathHelper.getXhtmlNode(document, "//HEAD/BASE/@href");
        String baseHref = null;
        if (baseNode != null) {
            baseHref = baseNode.getTextContent();
        }

        // get all Nodes from the Document containing feeds
        // this XPath relatively complicated to be in conformance to the Atom autodiscovery "standard".
        // see: http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
        List<Node> feedNodes = XPathHelper
        .getXhtmlNodes(
                document,
                "//LINK[contains(translate(@rel, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'alternate') and "
                + "(translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/atom+xml' or "
                + "translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/rss+xml')]");

        List<Node> atomNodes = new ArrayList<Node>();
        List<Node> rssNodes = new ArrayList<Node>();
        List<Node> resultNodes = null;

        // check for Atom/RSS
        for (Node feedNode : feedNodes) {

            String type = feedNode.getAttributes().getNamedItem("type").getNodeValue().toLowerCase();
            if (type.contains("atom")) {
                atomNodes.add(feedNode);
                atomFeeds.increment();
            } else if (type.contains("rss")) {
                rssNodes.add(feedNode);
                rssFeeds.increment();
            }

        }

        if (onlyPreferred && feedNodes.size() > 0) {
            // we only want to have the 1st, preferred feed
            LOGGER.trace("taking preferred feed");
            resultNodes = new ArrayList<Node>(Arrays.asList(new Node[] { feedNodes.get(0) }));
        } else if (atomNodes.size() > 0) {
            // we have Atom feeds, so we take them
            LOGGER.trace("taking Atom feeds");
            resultNodes = atomNodes;
        } else if (rssNodes.size() > 0) {
            // we have no Atom feeds, so we take the RSS feeds
            LOGGER.trace("taking RSS feeds");
            resultNodes = rssNodes;
        } else {
            LOGGER.trace("no feeds found");
        }

        if (feedNodes.size() > 1) {
            multipleFeeds.increment();
            LOGGER.trace("found multiple feeds");
        }

        if (resultNodes != null) {

            feedSites.increment();
            for (Node feedNode : resultNodes) {

                String feedHref = feedNode.getAttributes().getNamedItem("href").getNodeValue();

                // there are actualy some pages with an empty href attribute! so ignore them here.
                if (feedHref == null || feedHref.length() == 0) {
                    continue;
                }

                // few sites use a "Feed URI Scheme" which can look like
                // feed://example.com/entries.atom
                // feed:https://example.com/entries.atom
                // See ---> http://en.wikipedia.org/wiki/Feed_URI_scheme
                feedHref = feedHref.replace("feed://", "http://");
                feedHref = feedHref.replace("feed:", "");

                // make full URL
                String feedUrl = Crawler.makeFullURL(pageUrl, baseHref, feedHref);

                // validate URL
                UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https", "file" });
                boolean isValidUrl = urlValidator.isValid(feedUrl);

                if (isValidUrl) {
                    LOGGER.debug("found feed: " + feedUrl);
                    if (isIgnored(feedUrl)) {
                        LOGGER.info("ignoring " + feedUrl);
                        ignoredFeeds.increment();
                    } else {
                        result.add(feedUrl);
                    }
                } else {
                    LOGGER.info("invalid url " + feedUrl);
                }
            }
            // some statistical information
            LOGGER.info(pageUrl + " has atom:" + atomNodes.size() + ", rss:" + rssNodes.size() + ", extracted:"
                    + result.size());
        } else {
            LOGGER.info(pageUrl + " has no feed");
        }

        return result;

    }

    /**
     * Use the given queries and combine them.
     * A set of n queries is increased to have n + n-1 + n-2 ... queries.
     * For example:
     * queries: A, B, C
     * after combining: A, B, C, A B, A C, B C
     * 
     */
    private void combineQueries() {
        LOGGER.debug("combine queries");

        Collection<String> combinedQueries = new HashSet<String>();

        for (int i = 0; i < queries.size(); i++) {
            for (int j = i + 1; j < queries.size(); j++) {
                combinedQueries.add(queries.get(i) + " " + queries.get(j));
            }
        }

        LOGGER.debug("combined queries, originally " + queries.size() + ", now " + queries.size()
                + combinedQueries.size());

        queries.addAll(combinedQueries);
    }

    /**
     * Fill the stack of sites for discovering feeds.
     * Use the given queries to query search a search engine.
     */
    private void getSitesUsingQueries() {

        if (isCombineQueries()) {
            combineQueries();
        }

        for (String query : queries) {
            Set<String> foundSites = searchSites(query, resultLimit);
            sites.addAll(foundSites);
        }

        // free memory
        queries.clear();
    }

    /**
     * Find feeds in all pages on the sites tack. We use threading here which
     * yields in much faster results.
     */
    public void findFeeds() {

        LOGGER.trace(">findFeeds");

        // getSitesUsingQueries();

        if (isCombineQueries()) {
            combineQueries();
        }

        StopWatch stopWatch = new StopWatch();

        // to count number of running Threads
        final Counter counter = new Counter();

        LOGGER.info("start finding feeds with " + queries.size() + " queries and " + resultLimit
                + " results per query = " + resultLimit * queries.size() + " sites to check for feeds");

        while (queries.size() > 0) {
            String query = getQueryFromStack();

            Set<String> foundSites = searchSites(query, resultLimit);
            sites.addAll(foundSites);

            while (sites.size() > 0) {

                final String site = getSiteFromStack();

                // if maximum # of Threads are already running, wait here
                while (counter.getCount() >= maxThreads) {
                    LOGGER.trace("max # of Threads running. waiting ...");

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        LOGGER.warn(e.getMessage());
                        break;
                    }
                }

                counter.increment();
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            List<String> discoveredFeeds = discoverFeeds(site);
                            if (discoveredFeeds != null) {
                                for (String feed : discoveredFeeds) {
                                    if (feeds.add(feed)) {
                                        if (isWriteResultFileContinuously()) {
                                            FileHelper.appendFile(getResultFilePath(), feed + "\n");
                                        }
                                    }
                                }
                            }
                        } finally {
                            counter.decrement();
                        }
                    }
                };
                new Thread(runnable).start();

            }
        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (counter.getCount() > 0 || sites.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
            LOGGER.trace("waiting ... threads:" + counter.getCount() + " stack:" + sites.size());
        }
        LOGGER.trace("done");

        stopWatch.stop();
        discoveryTime += stopWatch.getElapsedTime();

        LOGGER.info("found " + feeds.size() + " feeds");
        LOGGER.trace("<findFeeds");
    }

    /**
     * Returns URLs of discovered feeds.
     * 
     * @return
     */
    public Collection<String> getFeeds() {
        LOGGER.trace(">getFeeds");
        LOGGER.trace("<getFeeds " + feeds.size());
        return feeds;
    }

    /**
     * Saves the discovered feeds to a file.
     * 
     * @param resultFile The file where the feeds should be saved to.
     */
    public void saveToFile() {
        FileHelper.writeToFile(getResultFilePath(), feeds);
    }

    /**
     * Dump all XML files.
     * 
     * @param debugDump
     */
    public void setDebugDump(boolean debugDump) {
        this.debugDump = debugDump;
    }

    /**
     * Set max number of concurrent autodiscovery requests.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * Limit the number of results for each query.
     * 
     * @param resultLimit
     *            The number of websites to query. This does not neccesarily
     *            mean that we get totalResults of feeds per query, as some
     *            sites do not offer a feed and some offer multiple feeds.
     */
    public void setResultLimit(int resultLimit) {
        this.resultLimit = resultLimit;
    }

    /**
     * Add to entry ignore list. Any feed url containing this string will be
     * ignored.
     * 
     * @param ignore
     * @return
     */
    public boolean addIgnore(String ignore) {
        return this.ignoreList.add(ignore.toLowerCase());
    }

    public void setIgnores(Collection<String> ignores) {
        LOGGER.trace("setting ignores to " + ignores);
        this.ignoreList = new HashSet<String>(ignores);
    }

    /**
     * Disable this option, to extract <i>all</i> available feeds on each webpage. Elsewise we only extract the
     * <i>preferred</i> feed, which means the first one mentioned on the page.
     * 
     * @see http://tools.ietf.org/id/draft-snell-atompub-autodiscovery-00.txt
     * 
     * @param allFeeds
     */
    public void setOnlyPreferred(boolean onlyPreferred) {
        this.onlyPreferred = onlyPreferred;
    }

    public boolean isOnlyPreferred() {
        return onlyPreferred;
    }

    public void setSearchEngine(int searchEngine) {
        LOGGER.trace("using " + SourceRetrieverManager.getName(searchEngine));
        this.searchEngine = searchEngine;
    }

    public int getSearchEngine() {
        return searchEngine;
    }

    /**
     * Returns some statistics about the dicovery process.
     * 
     * @return
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        String newLine = System.getProperty("line.separator");
        sb.append("----------------------------------------------").append(newLine);
        sb.append("    # sites checked:   " + totalSites).append(newLine);
        sb.append("    # sites with feed: " + feedSites).append(newLine);
        sb.append("             # atom: " + atomFeeds).append(newLine);
        sb.append("             # rss:  " + rssFeeds).append(newLine);
        sb.append("    # sites with multiple feeds: " + multipleFeeds).append(newLine);
        sb.append("    # ignored feeds: " + ignoredFeeds).append(newLine);
        sb.append("    # errors: " + errors).append(newLine).append(newLine);
        if (searchTime != 0) {
            sb.append("    total time for searching: " + DateHelper.getTimeString(searchTime)).append(newLine);
        }
        if (discoveryTime != 0) {
            sb.append("    total time for discovery: " + DateHelper.getTimeString(discoveryTime)).append(newLine)
            .append(newLine);
        }
        /*
         * if (traffic.getCount() != 0) {
         * sb.append("    traffic for discovery: " + ((float) traffic.getCount() / (1024 * 1024)) + " MB").append(
         * newLine);
         * }
         */
        if (crawler.getTotalDownloadSize() > 0) {
            sb.append("    traffic for discovery: " + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB").append(
                    newLine);
        }
        sb.append("----------------------------------------------");
        return sb.toString();
    }

    private synchronized String getSiteFromStack() {
        String result = null;
        if (sites.iterator().hasNext()) {
            result = sites.iterator().next();
            sites.remove(result);
        }
        return result;
    }

    private synchronized String getQueryFromStack() {
        String result = null;
        if (queries.iterator().hasNext()) {
            result = queries.iterator().next();
            queries.remove(result);
        }
        return result;
    }

    public void setCombineQueries(boolean combineQueries) {
        this.combineQueries = combineQueries;
    }

    public boolean isCombineQueries() {
        return combineQueries;
    }

    private boolean isIgnored(String feedUrl) {
        for (String ignore : ignoreList) {
            if (feedUrl.toLowerCase().contains(ignore)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        FeedDiscovery discovery = new FeedDiscovery();

        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("resultLimit").withDescription("maximum results per query")
                .hasArg().withArgName("nn").withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("threads")
                .withDescription("maximum number of simultaneous threads").hasArg().withArgName("nn")
                .withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("dump").withDescription("write XML dumps of visited pages")
                .create());
        options.addOption(OptionBuilder.withLongOpt("outputFile").withDescription("output file for results").hasArg()
                .withArgName("filename").create());
        options.addOption(OptionBuilder.withLongOpt("outputCont").withDescription("write the output file continuously")
                .create());
        options.addOption(OptionBuilder.withLongOpt("query").withDescription("runs the specified queries").hasArg()
                .withArgName("query1[,query2,...]").create());
        options.addOption(OptionBuilder.withLongOpt("queryFile")
                .withDescription("runs the specified queries from the file (one query per line)").hasArg()
                .withArgName("filename").create());
        options.addOption(OptionBuilder.withLongOpt("check").withDescription("check specified URL for feeds").hasArg()
                .withArgName("url").create());
        options.addOption(OptionBuilder.withLongOpt("combineQueries")
                .withDescription("combine single queries to create more mixed queries").create());

        try {

            if (args.length < 1) {
                // no options supplied, go to catch clause, print help.
                throw new ParseException(null);
            }

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("resultLimit")) {
                discovery.setResultLimit(((Number) cmd.getParsedOptionValue("resultLimit")).intValue());
            }
            if (cmd.hasOption("threads")) {
                discovery.setMaxThreads(((Number) cmd.getParsedOptionValue("threads")).intValue());
            }
            if (cmd.hasOption("dump")) {
                discovery.setDebugDump(true);
            }
            if (cmd.hasOption("outputFile")) {
                discovery.setResultFilePath(cmd.getOptionValue("outputFile"));
            }
            if (cmd.hasOption("outputCont")) {
                discovery.setWriteResultFileContinuously(true);
            }
            if (cmd.hasOption("combineQueries")) {
                discovery.setCombineQueries(true);
            }
            if (cmd.hasOption("query")) {

                List<String> queries = Arrays.asList(cmd.getOptionValue("query").replace("+", " ").split(","));
                discovery.addQueries(queries);

            }
            if (cmd.hasOption("queryFile")) {

                // load queries from the file
                List<String> queries = FileHelper.readFileToArray(cmd.getOptionValue("queryFile"));
                discovery.addQueries(queries);

            }

            discovery.findFeeds();

            Collection<String> discoveredFeeds = discovery.getFeeds();
            CollectionHelper.print(discoveredFeeds);
            System.out.println(discovery.getStatistics());

            // write result to file if a file path was specified and results were not written continuously already
            if (discovery.getResultFilePath().length() > 0 && !discovery.isWriteResultFileContinuously()) {
                discovery.saveToFile();
                System.out.println("wrote result to " + discovery.getResultFilePath());
            }

            if (cmd.hasOption("check")) {
                List<String> feeds = discovery.discoverFeeds(cmd.getOptionValue("check"));
                if (feeds.size() > 0) {
                    CollectionHelper.print(feeds);
                } else {
                    System.out.println("no feeds found");
                }
            }

            // done, exit.
            return;

        } catch (ParseException e) {
            // print usage help
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("FeedDiscovery [options]", options);
        }

    }

}