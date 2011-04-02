package ws.palladian.retrieval.feeds.discovery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.feeds.discovery.DiscoveredFeed.Type;
import ws.palladian.retrieval.search.SourceRetriever;
import ws.palladian.retrieval.search.SourceRetrieverManager;

/**
 * FeedDiscovery works like the following:
 * <ol>
 * <li>Query search engine with some terms (see {@link SourceRetrieverManager} for available search engines)</li>
 * <li>Get root URLs for each hit</li>
 * <li>Check page for feeds using RSS/Atom autodiscovery feature</li>
 * </ol>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * 
 * @see http://tools.ietf.org/id/draft-snell-atompub-autodiscovery-00.txt
 * @see http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
 * 
 */
public class FeedDiscovery {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedDiscovery.class);

    /**
     * XPath to get Atom and RSS links; this is relatively complicated to conform to the Atom autodiscovery "standard".
     */
    private static final String FEED_XPATH = "//LINK[contains(translate(@rel, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'alternate') and "
            + "(translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/atom+xml' or "
            + "translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/rss+xml')]";

    private static final int DEFAULT_NUM_THREADS = 10;

    /** DocumentRetriever for downloading pages. */
    private DocumentRetriever documentRetriever = new DocumentRetriever();

    /** Define which search engine to use, see {@link SourceRetrieverManager} for available constants. */
    // private int searchEngine = SourceRetrieverManager.YAHOO_BOSS;
    private int searchEngine = SourceRetrieverManager.BING;

    private int numThreads = DEFAULT_NUM_THREADS;

    /** Store all urls for which we will do the autodiscovery. */
    private BlockingQueue<String> urlQueue = new LinkedBlockingQueue<String>();

    /** The path of the file where the discovered feeds should be written to. */
    private String resultFilePath = null;

    /** Store a collection of all queries that are used to retrieve urlQueue from a search engine. */
    private BlockingQueue<String> queryQueue = new LinkedBlockingQueue<String>();

    /** Store all discovered feed's URLs. */
//    private Set<DiscoveredFeed> feeds = new LinkedHashSet<DiscoveredFeed>();

    /** Number of search engine results to retrieve for each query. */
    private int numResults = 10;

    public FeedDiscovery() {

        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            setNumThreads(config.getInt("feedDiscovery.maxDiscoveryThreads", DEFAULT_NUM_THREADS));
            setSearchEngine(config.getInt("feedDiscovery.searchEngine", SourceRetrieverManager.YAHOO_BOSS));
        } else {
            LOGGER.warn("could not load configuration, use defaults");
        }

    }

    /**
     * Search for Sites by specified query.
     * 
     * @param query
     * @param totalResults
     * @return
     */
    private Set<String> searchSites(String query, int totalResults) {

        // create source retriever object
        SourceRetriever sourceRetriever = new SourceRetriever();

        // set maximum number of expected results
        sourceRetriever.setResultCount(totalResults);

        // set search result language to english
        sourceRetriever.setLanguage(SourceRetriever.LANGUAGE_ENGLISH);

        sourceRetriever.setSource(getSearchEngine());

        List<String> resultURLs = sourceRetriever.getURLs(query, true);

        Set<String> sites = new HashSet<String>();
        for (String resultUrl : resultURLs) {
            sites.add(UrlHelper.getDomain(resultUrl));
        }

        return sites;
    }

    /**
     * Discovers feed links in supplied page URL.
     * 
     * @param pageUrl
     * @return list of discovered feed URLs, empty list if no feeds are available, <code>null</code> if page could not
     *         be parsed.
     */
    public List<DiscoveredFeed> discoverFeeds(String pageUrl) {

        List<DiscoveredFeed> result = null;
        Document document = null;

        try {

            document = documentRetriever.getWebDocument(pageUrl, false);

        } catch (Throwable t) {
            // NekoHTML produces various types of Exceptions, just catch them all here and log them.
            LOGGER.error("error retrieving " + pageUrl + " : " + t.toString() + " ; " + t.getMessage());
        }

        if (document != null) {
            result = discoverFeeds(document);
        }

        return result;

    }

    /**
     * Uses autodiscovery feature with MIME types "application/atom+xml" and "application/rss+xml" to find linked feeds
     * in the specified Document.
     * 
     * @param document
     * @return list of discovered feed URLs or empty list.
     */
    public List<DiscoveredFeed> discoverFeeds(Document document) {

        List<DiscoveredFeed> result = new LinkedList<DiscoveredFeed>();

        String pageUrl = document.getDocumentURI();
        // fix for testing purposes
        if (!pageUrl.startsWith("http://") && !pageUrl.startsWith("https://") && !pageUrl.startsWith("file:")) {
            pageUrl = "file:" + pageUrl;
        }

        Node baseNode = XPathHelper.getXhtmlNode(document, "//HEAD/BASE/@href");
        String baseHref = null;
        if (baseNode != null) {
            baseHref = baseNode.getTextContent();
        }

        // get all Nodes from the Document containing feeds
        List<Node> feedNodes = XPathHelper.getXhtmlNodes(document, FEED_XPATH);

        // check for Atom/RSS
        for (Node feedNode : feedNodes) {

            DiscoveredFeed feed = new DiscoveredFeed();
            NamedNodeMap attributes = feedNode.getAttributes();

            // ignore if href attribute is missing
            Node hrefNode = attributes.getNamedItem("href");
            if (hrefNode == null) {
                LOGGER.warn("href attribute is missing");
                continue;
            }

            // ignore if href attribute is empty
            String feedUrl = hrefNode.getNodeValue();
            if (feedUrl.isEmpty()) {
                LOGGER.warn("href attribute is empty");
                continue;
            }

            // few feeds use a "Feed URI Scheme" which can look like
            // feed://example.com/entries.atom
            // feed:https://example.com/entries.atom
            // See ---> http://en.wikipedia.org/wiki/Feed_URI_scheme
            feedUrl = feedUrl.replace("feed://", "http://");
            feedUrl = feedUrl.replace("feed:", "");

            // make full URL
            feedUrl = UrlHelper.makeFullURL(pageUrl, baseHref, feedUrl);

            // validate URL
            UrlValidator urlValidator = new UrlValidator(new String[] { "http", "https", "file" });
            boolean isValidUrl = urlValidator.isValid(feedUrl);

            if (!isValidUrl) {
                LOGGER.warn("invalid url : " + feedUrl);
                continue;
            }

            feed.setFeedLink(feedUrl);
            feed.setPageLink(pageUrl);

            String type = attributes.getNamedItem("type").getNodeValue().toLowerCase();
            if (type.contains("atom")) {
                feed.setFeedType(Type.ATOM);
            } else if (type.contains("rss")) {
                feed.setFeedType(Type.RSS);
            }

            Node titleNode = attributes.getNamedItem("title");
            if (titleNode != null) {
                feed.setFeedTitle(titleNode.getNodeValue());
            }

            result.add(feed);

        }

         LOGGER.debug(result.size() + " feeds for " + pageUrl);
        return result;

    }

    /**
     * Find feeds in all pages on the urlQueue tack. We use threading here to check multiple pages simultaneously.
     */
    public void findFeeds() {

        // StopWatch sw = new StopWatch();
        LOGGER.info("start finding feeds with " + queryQueue.size() + " queries and " + numResults
                + " results per query = " + numResults * queryQueue.size() + " urlQueue to check for feeds");
        
        
        // do the search
        Thread searchThread = new Thread() {
             @Override
            public void run() {
                 String query;
                 StopWatch sw = new StopWatch();
                 int totalQueries = queryQueue.size();
                 int currentQuery = 0;
                 while ((query = queryQueue.poll()) != null) {
                     currentQuery++;
                     LOGGER.info("querying for " + query + "; query " + currentQuery + " / " + totalQueries);
                     Set<String> foundSites = searchSites(query, numResults);
                     urlQueue.addAll(foundSites);
                 }
                 LOGGER.info("finished queries in " + sw.getElapsedTimeString());                 
            }
        };
        searchThread.start();

        // do the autodiscovery in parallel
        Thread[] threads = new Thread[numThreads];
        for (int i = 0; i < numThreads; i++) {

            threads[i] = new Thread() {
                @Override
                public void run() {
                    while (queryQueue.size() > 0 || urlQueue.size() > 0) {
                        
                        String url = urlQueue.poll();
                        
                        // we got no new URL from the queue, search engine might still be busy, so wait a moment before
                        // we continue looping.
                        if (url == null) {
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                LOGGER.error(e);
                            }
                            continue;
                        }
                        
                        try {
                            List<DiscoveredFeed> discoveredFeeds = discoverFeeds(url);
                            addDiscoveredFeeds(discoveredFeeds);
                        } catch (Throwable t) {
                            LOGGER.error(t);
                        }
                    }
                }
            };
            threads[i].start();
        }

        // wait, until all Threads are finished
        try {
            searchThread.join();
            for (Thread thread : threads) {
                thread.join();
            }
        } catch (InterruptedException e) {
            LOGGER.error(e);
        }

    }

    private synchronized void addDiscoveredFeeds(List<DiscoveredFeed> discoveredFeeds) {
        if (discoveredFeeds != null) {
            for (DiscoveredFeed feed : discoveredFeeds) {
//                boolean added = feeds.add(feed);
//                if (added && getResultFilePath() != null) {
                    // FileHelper.appendFile(getResultFilePath(), feed.toCSV() + "\n");
                    FileHelper.appendFile(getResultFilePath(), feed.getFeedLink() + "\n");
//                }
            }
        }
    }

    /**
     * Specify the path for the result file. If file already exists, new entries will be appended. The result file will
     * be written continuously. If <code>null</code>, no result file will be written, the result can be retrieved via
     * {@link #getFeeds()}.
     * 
     * @param resultFilePath
     */
    public void setResultFilePath(String resultFilePath) {
        this.resultFilePath = resultFilePath;
    }

    public String getResultFilePath() {
        return resultFilePath;
    }

    /**
     * Add a query for the search engine.
     * 
     * @param query The query to add.
     */
    public void addQuery(String query) {
        this.queryQueue.add(query);
    }

    /**
     * Add queryQueue for the search engine.
     * 
     * @param queries A collection of queries.
     */
    public void addQueries(Collection<String> queries) {
        this.queryQueue.addAll(queries);
    }

    /**
     * Add multiple queries from a query file.
     * 
     * @param filePath
     */
    public void addQueries(String filePath) {
        List<String> queries = FileHelper.readFileToArray(filePath);
        addQueries(queries);
    }

    public Collection<String> getQueryQueue() {
        return queryQueue;
    }

    /**
     * Returns URLs of discovered feeds.
     * 
     * @return
     */
//    public List<DiscoveredFeed> getFeeds() {
//        return new ArrayList<DiscoveredFeed>(feeds);
//    }

    /**
     * Set max number of concurrent autodiscovery requests.
     * 
     * @param maxThreads
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * Set number of results to retrieve for each query.
     * 
     * @param numResults The number of results for one query.
     */
    public void setNumResults(int numResults) {
        this.numResults = numResults;
    }

    public void setSearchEngine(int searchEngine) {
        LOGGER.trace("using " + SourceRetrieverManager.getName(searchEngine));
        this.searchEngine = searchEngine;
    }

    public int getSearchEngine() {
        return searchEngine;
    }
    
    /**
     * Use the given queries and combine them, to get the specified targetCount of queries.
     * 
     * <ul>
     * <li>If targetCount is smaller than existing queries, existing queries are reduced to a random subset.</li>
     * <li>If targetCount is bigger than possible tuples or -1, all posible combinations are calculated.</li>
     * <li>Else, as many tuples as necessary are calculated, to get specified targetCount of queries.</li>
     * </ul>
     * 
     * For example:
     * queries: A, B, C
     * after combining: A, B, C, A B, A C, B C
     */
    public void combineQueries(int targetCount) {

        int availableQueries = queryQueue.size();

        List<String> singleQueries = new ArrayList<String>(queryQueue);
        List<String> combinedQueries = new ArrayList<String>(queryQueue);
        Collections.shuffle(singleQueries);

        int possibleCombinations = availableQueries * (availableQueries - 1) / 2;

        if (targetCount != -1 && availableQueries > targetCount) {

            // we have more queries than we want, create a random subset of specified size
            combinedQueries.addAll(singleQueries.subList(0, targetCount));

        } else if (targetCount == -1 || targetCount > possibleCombinations + availableQueries) {

            // target count is higher than possible tuple combinations; so just calculate all
            for (int i = 0; i < availableQueries; i++) {
                for (int j = i + 1; j < availableQueries; j++) {
                    combinedQueries.add(singleQueries.get(i) + " " + singleQueries.get(j));
                }
            }

        } else {

            // create combined queries
            Random random = new Random();
            while (combinedQueries.size() < targetCount) {
                // get a query term randomly by combining two single queries
                String term1 = singleQueries.get(random.nextInt(availableQueries));
                String term2 = singleQueries.get(random.nextInt(availableQueries));
                combinedQueries.add(term1 + " " + term2);
            }
        }

        Collections.shuffle(combinedQueries);
        queryQueue.clear();
        queryQueue.addAll(combinedQueries);

    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        FeedDiscovery discovery = new FeedDiscovery();

        CommandLineParser parser = new BasicParser();

        Options options = new Options();
        options.addOption(OptionBuilder.withLongOpt("numResults").withDescription("maximum results per query").hasArg()
                .withArgName("nn").withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("threads")
                .withDescription("maximum number of simultaneous threads").hasArg().withArgName("nn")
                .withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("outputFile").withDescription("output file for results").hasArg()
                .withArgName("filename").create());
        options.addOption(OptionBuilder.withLongOpt("query").withDescription("runs the specified queries").hasArg()
                .withArgName("query1[,query2,...]").create());
        options.addOption(OptionBuilder.withLongOpt("queryFile")
                .withDescription("runs the specified queries from the file (one query per line)").hasArg()
                .withArgName("filename").create());
        options.addOption(OptionBuilder.withLongOpt("check").withDescription("check specified URL for feeds").hasArg()
                .withArgName("url").create());
        options.addOption(OptionBuilder.withLongOpt("combineQueries")
                .withDescription("combine single queries to create more mixed queries").hasArg()
                .withArgName("nn").withType(Number.class).create());
        options.addOption(OptionBuilder.withLongOpt("searchEngine")
                .withDescription("search engine to use, see SourceRetrieverManager").hasArg().withArgName("n")
                .withType(Number.class).create());

        try {

            if (args.length < 1) {
                // no options supplied, go to catch clause, print help.
                throw new ParseException(null);
            }

            CommandLine cmd = parser.parse(options, args);

            if (cmd.hasOption("numResults")) {
                discovery.setNumResults(((Number) cmd.getParsedOptionValue("numResults")).intValue());
            }
            if (cmd.hasOption("threads")) {
                discovery.setNumThreads(((Number) cmd.getParsedOptionValue("threads")).intValue());
            }
            if (cmd.hasOption("outputFile")) {
                discovery.setResultFilePath(cmd.getOptionValue("outputFile"));
            }
            if (cmd.hasOption("query")) {

                List<String> queries = Arrays.asList(cmd.getOptionValue("query").replace("+", " ").split(","));
                discovery.addQueries(queries);

            }
            if (cmd.hasOption("queryFile")) {
                discovery.addQueries(cmd.getOptionValue("queryFile"));
            }
            if (cmd.hasOption("combineQueries")) {
                int targetCount = ((Number) cmd.getParsedOptionValue("combineQueries")).intValue();
                discovery.combineQueries(targetCount);
            }
            if (cmd.hasOption("searchEngine")) {
                int searchEngine = ((Number) cmd.getParsedOptionValue("searchEngine")).intValue();
                discovery.setSearchEngine(searchEngine);
            }

            discovery.findFeeds();

            // Collection<DiscoveredFeed> discoveredFeeds = discovery.getFeeds();
            // CollectionHelper.print(discoveredFeeds);

            if (cmd.hasOption("check")) {
                List<DiscoveredFeed> feeds = discovery.discoverFeeds(cmd.getOptionValue("check"));
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