package ws.palladian.retrieval;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.Callback;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.ThreadHelper;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HtmlHelper;

/**
 * <p>
 * A simple web crawler which can crawl web pages within a domain or crawl cross domain.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class Crawler {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Crawler.class);

    /** The document retriever. */
    private DocumentRetriever documentRetriever;

    /** Maximum number of threads used during crawling. */
    private int maxThreads = 10;

    /** Number of active threads. */
    private AtomicInteger threadCount = new AtomicInteger(0);

    private ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

    // ///////////////////////////////////////////////////////
    // ////////////////// crawl settings ////////////////////
    // ///////////////////////////////////////////////////////

    /** Whether to crawl within a certain domain. */
    private boolean inDomain = true;

    /** Whether to crawl outside of current domain. */
    private boolean outDomain = true;

    /** Only follow domains that have one or more of these regexps in their URL. */
    private final Set<Pattern> whiteListUrlRegexps = new HashSet<Pattern>();

    /** Regexps that must not be contained in the URLs or they won't be followed. */
    private final Set<Pattern> blackListUrlRegexps = new HashSet<Pattern>();

    /** Remove those parts from every retrieved URL. */
    private final Set<Pattern> urlModificationRegexps = new HashSet<Pattern>();

    /** Do not look for more URLs if visited stopCount pages already, -1 for infinity. */
    private int stopCount = -1;
    private Set<String> urlStack = Collections.synchronizedSet(new HashSet<String>());
    private final Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<String>());

    /** All urls that have been visited or extracted. */
    private final Set<String> seenUrls = new HashSet<String>();

    private final Set<String> urlRules = new HashSet<String>();

    /** If true, all query params in the URL ?= will be stripped. */
    private boolean stripQueryParams = true;

    /** The callback that is called after the crawler finished crawling. */
    private Callback crawlerCallbackOnFinish = null;

    public Crawler() {
        documentRetriever = new DocumentRetriever();
    }

    public Crawler(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
    }

    /**
     * Visit a certain web page and grab URLs.
     * 
     * @param currentURL A URL.
     */
    protected void crawl(String currentURL) {

        LOGGER.info("catch from stack: {}", currentURL);

        Document document = documentRetriever.getWebDocument(currentURL);

        if (document != null) {
            Set<String> links = HtmlHelper.getLinks(document, inDomain, outDomain);

            if (urlStack.isEmpty() || visitedUrls.isEmpty() || (System.currentTimeMillis() / 1000) % 5 == 0) {
                LOGGER.info("retrieved {} links from {} || stack size: {}, visited: {}", new Object[] {links.size(),
                        currentURL, urlStack.size(), visitedUrls.size()});
            }

            addUrlsToStack(links, currentURL);
        } else {
            LOGGER.error("could not get " + currentURL + ", putting it back on the stack for later");
            addUrlToStack(currentURL, currentURL);
        }

    }

    /**
     * <p>
     * Stop the crawler.
     * </p>
     */
    public void stopCrawl() {
        setStopCount(0);
    }

    /**
     * Start the crawling process.
     */
    private void startCrawl() {

        // crawl
        final AtomicLong lastCrawlTime = new AtomicLong(System.currentTimeMillis());
        long silentStopTime = TimeUnit.MINUTES.toMillis(10);
        while ((stopCount == -1 || visitedUrls.size() < stopCount)
                && ((System.currentTimeMillis() - lastCrawlTime.get()) < silentStopTime)) {

            try {
                final String url = getUrlFromStack();

                Thread ct = new Thread("CrawlThread-" + url) {
                    @Override
                    public void run() {
                        crawl(url);
                        lastCrawlTime.set(System.currentTimeMillis());
                    }
                };

                if (!executor.isShutdown()) {
                    executor.submit(ct);
                }
                ThreadHelper.deepSleep(1000);
            } catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

        }

        // wait for the threads to finish
        executor.shutdown();

        // wait until all threads are finish
        LOGGER.info("waiting for all threads to finish...");
        StopWatch sw = new StopWatch();
        try {
            while (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                LOGGER.debug("wait crawling");
            }
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }
        LOGGER.info("...all threads finished in " + sw.getTotalElapsedTimeString());

        // LOGGER.info("-----------------------------------------------");
        // LOGGER.info("-----------------------------------------------");
        // LOGGER.info("-------------------URL DUMP--------------------");
        // Iterator<String> urlDumpIterator = urlDump.iterator();
        // while (urlDumpIterator.hasNext()) {
        // LOGGER.info(urlDumpIterator.next());
        // }

        if (crawlerCallbackOnFinish != null) {
            crawlerCallbackOnFinish.callback();
        }

    }

    /**
     * Start the crawling process.
     * 
     * @param urlStack The URLs to crawl.
     * @param inDomain Follow links that point to other pages within the given domain.
     * @param outDomain Follow outbound links.
     */
    public void startCrawl(Set<String> urlStack, boolean inDomain, boolean outDomain) {
        this.urlStack.clear();
        this.urlStack.addAll(urlStack);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    /**
     * Start the crawling process.
     * 
     * @param startURL The URL where the crawler should start.
     * @param inDomain Follow links that point to other pages within the given domain.
     * @param outDomain Follow outbound links.
     */
    public void startCrawl(String startURL, boolean inDomain, boolean outDomain) {
        urlStack.clear();
        urlStack.add(startURL);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    private synchronized String getUrlFromStack() {
        Iterator<String> iterator = urlStack.iterator();
        if (iterator.hasNext()) {
            String url = iterator.next();
            urlStack.remove(url);
            visitedUrls.add(url);
            return url;
        }
        return null;
    }

    public void setStopCount(int number) {
        this.stopCount = number;
    }

    public void addWhiteListRegexp(String regexp) {
        whiteListUrlRegexps.add(Pattern.compile(regexp));
    }

    public void addBlackListRegexp(String regexp) {
        blackListUrlRegexps.add(Pattern.compile(regexp));
    }

    public void addBlackListRegexps(Set<String> blackListRegexps) {
        for (String string : blackListRegexps) {
            addBlackListRegexp(string);
        }
    }

    public Set<Pattern> getUrlModificationRegexps() {
        return urlModificationRegexps;
    }

    public void addUrlModificationRegexps(Set<String> urlModificationRegexps) {
        for (String string : urlModificationRegexps) {
            this.urlModificationRegexps.add(Pattern.compile(string));
        }
    }

    public void addUrlRule(String rule) {
        urlRules.add(rule);
    }

    private synchronized void addUrlsToStack(Set<String> urls, String sourceURL) {
        for (String url : urls) {
            addUrlToStack(url, sourceURL);
        }
    }

    private String cleanUrl(String url) {
        url = UrlHelper.removeSessionId(url);
        url = UrlHelper.removeAnchors(url);
        if (isStripQueryParams()) {
            url = url.replaceAll("\\?.*", "");
        }
        for (Pattern pattern : urlModificationRegexps) {
            url = pattern.matcher(url).replaceAll("");
        }
        return url;
    }

    private synchronized void addUrlToStack(String url, String sourceUrl) {

        url = cleanUrl(url);

        // check URL first
        if (url != null && url.length() < 400 && !visitedUrls.contains(url)) {

            boolean follow = true;

            // check whether the url should be followed
            if (!whiteListUrlRegexps.isEmpty()) {
                follow = false;
                for (Pattern regexp : whiteListUrlRegexps) {
                    if (regexp.matcher(url).find()) {
                        follow = true;
                        break;
                    }
                }
            }
            if (!blackListUrlRegexps.isEmpty()) {
                for (Pattern regexp : blackListUrlRegexps) {
                    if (regexp.matcher(url).find()) {
                        follow = false;
                        break;
                    }
                }
            }

            if (follow) {
                urlStack.add(url);
            }

            seenUrls.add(url);
        }
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
        executor = Executors.newFixedThreadPool(maxThreads);
    }

    public int getThreadCount() {
        return threadCount.get();
    }

    public Callback getCrawlerCallbackOnFinish() {
        return crawlerCallbackOnFinish;
    }

    public void setCrawlerCallbackOnFinish(Callback crawlerCallbackOnFinish) {
        this.crawlerCallbackOnFinish = crawlerCallbackOnFinish;
    }

    public void addCrawlerCallback(RetrieverCallback<Document> crawlerCallback) {
        documentRetriever.addRetrieverCallback(crawlerCallback);
    }

    public DocumentRetriever getDocumentRetriever() {
        return documentRetriever;
    }

    public void setDocumentRetriever(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
    }

    public boolean isStripQueryParams() {
        return stripQueryParams;
    }

    public void setStripQueryParams(boolean stripQueryParams) {
        this.stripQueryParams = stripQueryParams;
    }

    public static void main(String[] args) {

        // ///////////////// simple usage ///////////////////
        // create the crawler object
        Crawler crawler = new Crawler();

        // create a callback that is triggered for every crawled page
        RetrieverCallback<Document> crawlerCallback = new RetrieverCallback<Document>() {

            @Override
            public void onFinishRetrieval(Document document) {
                LOGGER.info("downloaded the page " + document.getDocumentURI());
            }
        };
        crawler.addCrawlerCallback(crawlerCallback);

        // stop after 1000 pages have been crawled (default is unlimited)
        crawler.setStopCount(1000);

        // set the maximum number of threads to 1
        crawler.setMaxThreads(1);

        // the crawler should automatically use different proxies after every
        // 3rd request (default is no proxy switching)
        // crawler.getDocumentRetriever().setSwitchProxyRequests(3);
        //
        // // set a list of proxies to choose from
        // List<String> proxyList = new ArrayList<String>();
        // proxyList.add("83.244.106.73:8080");
        // proxyList.add("83.244.106.73:80");
        // proxyList.add("67.159.31.22:8080");
        // crawler.getDocumentRetriever().setProxyList(proxyList);

        // start the crawling process from a certain page, true = follow links
        // within the start domain, true = follow outgoing links
        crawler.startCrawl("http://www.dmoz.org/", true, true);
        // //////////////////////////////////////////////////
    }

}
