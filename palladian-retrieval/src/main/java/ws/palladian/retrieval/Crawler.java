package ws.palladian.retrieval;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.Callback;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.helper.NoThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;

import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

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

    /** Silent Stop Time */
    private int silentStopTime = 10;

    /** Number of active threads. */
    private AtomicInteger threadCount = new AtomicInteger(0);

    private ExecutorService executor = Executors.newFixedThreadPool(maxThreads);

    /** The number of milliseconds each host gets between two requests. */
    private RequestThrottle requestThrottle = NoThrottle.INSTANCE;

    // ///////////////////////////////////////////////////////
    // ////////////////// crawl settings ////////////////////
    // ///////////////////////////////////////////////////////

    /** Whether to crawl within a certain domain. */
    private boolean inDomain = true;

    /** Whether to crawl outside of current domain. */
    private boolean outDomain = true;

    /** Whether to crawl sub domains of current domain. */
    private boolean subDomain = false;

    /** Only follow domains that have one or more of these regexps in their URL. */
    protected final Set<Pattern> whiteListUrlRegexps = new HashSet<>();

    /** Regexps that must not be contained in the URLs or they won't be followed. */
    protected final Set<Pattern> blackListUrlRegexps = new HashSet<>();

    /** Replace certain patterns in each retrieved URL. */
    private final LinkedHashMap<Pattern, String> urlModificationRegexps = new LinkedHashMap<>();

    /** Do not look for more URLs if visited stopCount pages already, -1 for infinity. */
    private int stopCount = -1;
    private Set<String> urlStack = Collections.synchronizedSet(new HashSet<>());
    private Set<String> visitedUrls = Collections.synchronizedSet(new HashSet<>());

    /** If true, all query params in the URL ?= will be stripped. */
    private boolean stripQueryParams = true;

    /** If true, rel="nofollow" links will indeed not be followed. */
    private boolean respectNoFollow = false;

    /** The callback that is called after the crawler finished crawling. */
    private Callback crawlerCallbackOnFinish = null;

    /** A map of consumers with <filetype, consumer> to react to certain file types. */
    private Map<String, Consumer<String>> fileTypeConsumers = null;

    public Crawler() {
        documentRetriever = new DocumentRetriever();
    }

    public Crawler(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
    }

    public RequestThrottle getRequestThrottle() {
        return requestThrottle;
    }

    public void setRequestThrottle(RequestThrottle requestThrottle) {
        this.requestThrottle = requestThrottle;
    }

    /**
     * Visit a certain web page and grab URLs.
     * 
     * @param currentUrl A URL.
     */
    protected void crawl(String currentUrl) {

        LOGGER.info("catch from stack: {}", currentUrl);

        requestThrottle.hold();

        // file type consumers?
        if (getFileTypeConsumers() != null) {
            String fileType = FileHelper.getFileType(currentUrl);
            Consumer<String> stringConsumer = getFileTypeConsumers().get(fileType);
            if (stringConsumer != null) {
                stringConsumer.process(currentUrl);
                return;
            }
        }

        Document document = documentRetriever.getWebDocument(currentUrl);

        if (document != null) {
            Set<String> links = HtmlHelper.getLinks(document, document.getDocumentURI(), inDomain, outDomain, "", respectNoFollow, subDomain);
            // check if we can get more links out of it

            if (urlStack.isEmpty() || visitedUrls.isEmpty() || (System.currentTimeMillis() / 1000) % 5 == 0) {
                LOGGER.info("retrieved {} links from {} || stack size: {}, visited: {}", new Object[] {links.size(),
                        currentUrl, urlStack.size(), visitedUrls.size()});
            }

            addUrlsToStack(links);
        } else if (documentRetriever.getDownloadFilter().accept(currentUrl)){
            LOGGER.error("could not get " + currentUrl + ", putting it back on the stack for later");
            addUrlToStack(currentUrl);
        }

    }

    public void setSilentStopTime(int stopTimeInMinutes){
        silentStopTime = stopTimeInMinutes;
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
        long silentStopTimeMillis = TimeUnit.MINUTES.toMillis(silentStopTime);
        while ((stopCount == -1 || visitedUrls.size() < stopCount)
                && ((System.currentTimeMillis() - lastCrawlTime.get()) < silentStopTimeMillis)) {

            try {
                final String url = getUrlFromStack();
                if (url != null) {
                    Thread ct = new Thread("CrawlThread-" + url) {
                        @Override
                        public void run() {
                            try {
                                crawl(url);
                                lastCrawlTime.set(System.currentTimeMillis());
                            } catch (Throwable t) {
                                // whatever
                                t.printStackTrace();
                            }
                        }
                    };

                    if (!executor.isShutdown()) {
                        executor.submit(ct);
                    }
                }

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
    public void startCrawl(Set<String> urlStack, boolean inDomain, boolean outDomain, boolean subDomain) {
        this.urlStack.clear();
        this.urlStack.addAll(urlStack);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        this.subDomain = subDomain;
        startCrawl();
    }


    /**
     * Start the crawling process.
     *
     * @param startURL The URL where the crawler should start.
     * @param inDomain Follow links that point to other pages within the given domain.
     * @param outDomain Follow outbound links.
     */
    public void startCrawl(String startURL, boolean inDomain, boolean outDomain, boolean subDomain) {
        urlStack.clear();
        urlStack.add(startURL);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        this.subDomain = subDomain;
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

    public int getStopCount() {
        return this.stopCount;
    }

    public void addWhiteListRegexp(String regexp) {
        whiteListUrlRegexps.add(Pattern.compile(regexp));
    }

    public void addWhiteListRegexps(Set<String> whiteListRegexps) {
        for (String string : whiteListRegexps) {
            addWhiteListRegexp(string);
        }
    }

    public void addBlackListRegexp(String regexp) {
        blackListUrlRegexps.add(Pattern.compile(regexp));
    }

    public void addBlackListRegexps(Set<String> blackListRegexps) {
        for (String string : blackListRegexps) {
            addBlackListRegexp(string);
        }
    }

    public Map<Pattern, String> getUrlModificationRegexps() {
        return urlModificationRegexps;
    }

    public void addUrlModificationRegexps(LinkedHashMap<Pattern, String> urlModificationRegexps) {
        this.urlModificationRegexps.putAll(urlModificationRegexps);
    }

    private synchronized void addUrlsToStack(Set<String> urls) {
        for (String url : urls) {
            addUrlToStack(url);
        }
    }

    private String cleanUrl(String url) {
        url = UrlHelper.removeSessionId(url);
        url = UrlHelper.removeAnchors(url);
        if (isStripQueryParams()) {
            url = url.replaceAll("\\?.*", "");
        }
        for (Map.Entry<Pattern, String> entry : urlModificationRegexps.entrySet()) {
            try {
                url = entry.getKey().matcher(url).replaceAll(entry.getValue());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return url;
    }

    protected synchronized void addUrlToStack(String url) {

        url = cleanUrl(url);

        // check URL first
        if (url != null && url.length() < 400 && !visitedUrls.contains(url) && documentRetriever.getDownloadFilter().accept(url)) {

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

    public void addCrawlerCallback(Consumer<Document> crawlerCallback) {
        documentRetriever.addRetrieverCallback(crawlerCallback);
    }

    public Map<String, Consumer<String>> getFileTypeConsumers() {
        return fileTypeConsumers;
    }

    public void setFileTypeConsumers(Map<String, Consumer<String>> fileTypeConsumers) {
        this.fileTypeConsumers = fileTypeConsumers;
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

    public boolean isRespectNoFollow() {
        return respectNoFollow;
    }

    public void setRespectNoFollow(boolean respectNoFollow) {
        this.respectNoFollow = respectNoFollow;
    }

    public void setStripQueryParams(boolean stripQueryParams) {
        this.stripQueryParams = stripQueryParams;
    }

    public Set<String> getUrlStack() {
        return urlStack;
    }

    public void setUrlStack(Set<String> urlStack) {
        this.urlStack = urlStack;
    }

    public Set<String> getVisitedUrls() {
        return visitedUrls;
    }

    public void setVisitedUrls(Set<String> visitedUrls) {
        this.visitedUrls = visitedUrls;
    }

    public static void main(String[] args) throws UnknownHostException {

        // ///////////////// simple usage ///////////////////
        // create the crawler object
        Crawler crawler = new Crawler();

        // create a callback that is triggered for every crawled page
        Consumer<Document> crawlerCallback = new Consumer<Document>() {
            @Override
            public void process(Document item) {
                LOGGER.info("downloaded the page " + item.getDocumentURI());
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
        crawler.startCrawl("http://www.dmoz.org/", true, true, true);
        // //////////////////////////////////////////////////
    }

}
