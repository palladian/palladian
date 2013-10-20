package ws.palladian.retrieval;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.Callback;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;

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
    private final Set<Pattern> blackListUrlRegexps = CollectionHelper.newHashSet();

    /** Do not look for more URLs if visited stopCount pages already, -1 for infinity. */
    private int stopCount = -1;
    private Set<String> urlStack = null;
    private final Set<String> visitedURLs = new HashSet<String>();

    /** All urls that have been visited or extracted. */
    private final Set<String> seenUrls = new HashSet<String>();


    private final Set<String> urlRules = new HashSet<String>();
    private final Set<String> urlDump = new HashSet<String>();

    /** The callback that is called after the crawler finished crawling. */
    private Callback crawlerCallbackOnFinish = null;

    public Crawler() {
        documentRetriever = new DocumentRetriever();
    }

    /**
     * Visit a certain web page and grab URLs.
     * 
     * @param currentURL A URL.
     */
    protected void crawl(String currentURL) {

        LOGGER.info("catch from stack: {}", currentURL);

        // System.out.println("process "+currentURL+" \t stack size: "+urlStack.size()+" dump size: "+urlDump.size());
        Document document = documentRetriever.getWebDocument(currentURL);

        Set<String> links = HtmlHelper.getLinks(document, inDomain, outDomain);

        LOGGER.info("retrieved {} links from {} || stack size: {} dump size: {}, visited: {}",
                new Object[] {links.size(), currentURL, urlStack.size(), urlDump.size(), visitedURLs.size()});

        addUrlsToStack(links, currentURL);
    }

    /**
     * Save the crawled URLs.
     * 
     * @param filename The path where the URLs should be saved to.
     */
    public final void saveUrlDump(String filename) {
        String urlDumpString = "URL crawl from " + DateHelper.getCurrentDatetime("dd.MM.yyyy") + " at "
                + DateHelper.getCurrentDatetime("HH:mm:ss") + "\n";
        urlDumpString += "Number of urls: " + urlDump.size() + "\n\n";

        FileHelper.writeToFile(filename, urlDump);
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
        final ThreadGroup tg = new ThreadGroup("crawler threads");

        while (!urlStack.isEmpty() && (stopCount == -1 || visitedURLs.size() < stopCount)) {

            int maxThreadsNow = getMaxThreads();
            if (urlStack.size() <= maxThreads) {
                maxThreadsNow = 1;
            }
            while (getThreadCount() >= maxThreadsNow) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return;
                }
            }

            final String url = getUrlFromStack();
            Thread ct = new Thread("CrawlThread" + System.currentTimeMillis()) {
                @Override
                public void run() {
                    crawl(url);
                    threadCount.decrementAndGet();
                }
            };
            ct.start();
            threadCount.incrementAndGet();

            // if stack is still empty, let all threads finish before checking
            // in loop again
            int wc = 0;
            while (urlStack.isEmpty() && getThreadCount() > 0 && wc < 60) {
                try {
                    wc++;
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return;
                }
            }
        }

        // wait for the threads to finish
        int wc = 0;
        while (getThreadCount() > 0 && wc < 180) {
            try {
                LOGGER.info("wait a second ({} more times, {} threds active)", 180 - wc, getThreadCount());
                wc++;
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                return;
            }
        }

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
        this.urlStack = urlStack;
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
        urlStack = new HashSet<String>();
        urlStack.add(startURL);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    private synchronized String getUrlFromStack() {
        String url = urlStack.iterator().next();
        removeUrlFromStack(url);
        return url;
    }

    private synchronized void removeUrlFromStack(String url) {
        urlStack.remove(url);
        visitedURLs.add(url);
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

    public void addUrlRule(String rule) {
        urlRules.add(rule);
    }

    private synchronized void addUrlsToStack(Set<String> urls, String sourceURL) {
        for (String url : urls) {
            addUrlToStack(url, sourceURL);
        }
    }

    private synchronized void addUrlToStack(String url, String sourceUrl) {

        // check URL first
        if (url != null && url.length() < 400 && !visitedURLs.contains(url)) {

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
            } else if (!seenUrls.contains(url)) {
                sourceUrl = sourceUrl.replace("/", " ").trim();
                if (checkUrlRules(sourceUrl)) {
                    urlDump.add(url + " " + sourceUrl);
                }
            }

            seenUrls.add(url);
        }
    }

    private boolean checkUrlRules(String url) {
        boolean valid = false;

        for (String rule : urlRules) {
            url = url.replace("/", " ");
            if (url.indexOf(rule) > 0) {
                valid = true;
                break;
            }
        }

        return valid;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
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
