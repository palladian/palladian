package ws.palladian.retrieval;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.Callback;
import ws.palladian.helper.date.DateHelper;

public class Crawler {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Crawler.class);

    private DocumentRetriever documentRetriever;

    public DocumentRetriever getDocumentRetriever() {
        return documentRetriever;
    }

    public void setDocumentRetriever(DocumentRetriever documentRetriever) {
        this.documentRetriever = documentRetriever;
    }

    /** maximum number of threads used during crawling */
    private int maxThreads = 10;

    /** number of active threads */
    private int threadCount = 0;

    // ////////////////// crawl settings ////////////////////
    /** whether to crawl within a certain domain */
    private boolean inDomain = true;

    /** whether to crawl outside of current domain */
    private boolean outDomain = true;

    /** only follow domains that have one or more of these strings in their url */
    private Set<String> onlyFollow = new HashSet<String>();

    /** do not look for more URLs if visited stopCount pages already, -1 for infinity */
    private int stopCount = -1;
    private Set<String> urlStack = null;
    private Set<String> visitedURLs = new HashSet<String>();

    /** all urls that have been visited or extracted */
    private Set<String> seenURLs = new HashSet<String>();

    private Set<String> urlRules = new HashSet<String>();
    private Set<String> urlDump = new HashSet<String>();

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

        LOGGER.info("catch from stack: " + currentURL);

        // System.out.println("process "+currentURL+" \t stack size: "+urlStack.size()+" dump size: "+urlDump.size());
        Document document = documentRetriever.getWebDocument(currentURL);

        Set<String> links = PageAnalyzer.getLinks(document, inDomain, outDomain);
        LOGGER.info("\n\nretrieved " + links.size() + " links from " + currentURL + " || stack size: "
                + urlStack.size() + " dump size: " + urlDump.size() + ", visited: " + visitedURLs.size());

        // moved this method call to setDocument method ... Philipp, 2010-06-13
        // callCrawlerCallback(document);

        addURLsToStack(links, currentURL);
    }

    public void saveURLDump(String filename) {
        String urlDumpString = "URL crawl from " + DateHelper.getCurrentDatetime("dd.MM.yyyy") + " at "
        + DateHelper.getCurrentDatetime("HH:mm:ss") + "\n";
        urlDumpString += "Number of urls: " + urlDump.size() + "\n\n";

        try {
            FileWriter fileWriter = new FileWriter(filename);
            fileWriter.write(urlDumpString);

            Iterator<String> urlDumpIterator = urlDump.iterator();
            while (urlDumpIterator.hasNext()) {
                fileWriter.write(urlDumpIterator.next() + "\n");
                fileWriter.flush();
            }

            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            LOGGER.error(filename + ", " + e.getMessage());
        }
    }

    private void startCrawl() {

        // do the crawling
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

            String url = getURLFromStack();
            Thread ct = new CrawlThread(this, url, tg, "CrawlThread" + System.currentTimeMillis());
            ct.start();
            increaseThreadCount();

            // if stack is still empty, let all threads finish before checking
            // in loop again
            while (urlStack.isEmpty() && getThreadCount() > 0) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return;
                }
            }

            // crawl(urlIterator.next());
            /*
             * if ((urlDump.size() % 2000 < 1000) && !saved && urlDump.size() > 10) {
             * saveURLDump("data/crawl/"+Logger.getInstance().getDateString
             * ()+"_dmoz_urldump"+dumpNumber+".txt"); saved = true; ++dumpNumber; } else if (urlDump.size() % 2000 >
             * 1000) { saved = false; }
             */
            // urlIterator = urlStack.iterator();
        }

        // wait for the threads to finish
        while (getThreadCount() > 0) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                return;
            }
        }

        LOGGER.info("-----------------------------------------------");
        LOGGER.info("-----------------------------------------------");
        LOGGER.info("-------------------URL DUMP--------------------");
        Iterator<String> urlDumpIterator = urlDump.iterator();
        while (urlDumpIterator.hasNext()) {
            LOGGER.info(urlDumpIterator.next());
        }

        if (crawlerCallbackOnFinish != null) {
            crawlerCallbackOnFinish.callback();
        }

    }

    public void startCrawl(Set<String> urlStack, boolean inDomain, boolean outDomain) {
        this.urlStack = urlStack;
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    public void startCrawl(String startURL, boolean inDomain, boolean outDomain) {
        urlStack = new HashSet<String>();
        urlStack.add(startURL);
        this.inDomain = inDomain;
        this.outDomain = outDomain;
        startCrawl();
    }

    private synchronized String getURLFromStack() {
        String url = urlStack.iterator().next();
        removeURLFromStack(url);
        return url;
    }

    private synchronized void removeURLFromStack(String url) {
        urlStack.remove(url);
        visitedURLs.add(url);
    }

    public void setStopCount(int number) {
        this.stopCount = number;
    }

    public void addOnlyFollow(String follow) {
        onlyFollow.add(follow);
    }

    public void addURLRule(String rule) {
        urlRules.add(rule);
    }

    private synchronized void addURLsToStack(Set<String> urls, String sourceURL) {
        for (String url : urls) {
            addURLToStack(url, sourceURL);
        }
    }

    private synchronized void addURLToStack(String url, String sourceURL) {

        // check URL first
        if (url != null && url.length() < 400 && !visitedURLs.contains(url)) {

            boolean follow = true;

            if (onlyFollow.size() > 0) {
                follow = false;
                Iterator<String> followIterator = onlyFollow.iterator();
                while (followIterator.hasNext()) {
                    String followString = followIterator.next();
                    if (url.indexOf(followString) > -1) {
                        follow = true;
                        break;
                    }
                }
            }

            if (follow) {
                urlStack.add(url);
            } else if (!seenURLs.contains(url)) {
                sourceURL = sourceURL.replace("/", " ").trim();
                if (checkURLRules(sourceURL)) {
                    urlDump.add(url + " " + sourceURL);
                }
            }

            seenURLs.add(url);
        }
    }

    private boolean checkURLRules(String url) {
        boolean valid = false;

        Iterator<String> urlRulesIterator = urlRules.iterator();
        while (urlRulesIterator.hasNext()) {
            String rule = urlRulesIterator.next();
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
        return threadCount;
    }

    public void increaseThreadCount() {
        this.threadCount++;
    }

    public void decreaseThreadCount() {
        this.threadCount--;
    }

    public Callback getCrawlerCallbackOnFinish() {
        return crawlerCallbackOnFinish;
    }

    public void setCrawlerCallbackOnFinish(Callback crawlerCallbackOnFinish) {
        this.crawlerCallbackOnFinish = crawlerCallbackOnFinish;
    }

    private void addCrawlerCallback(CrawlerCallback crawlerCallback) {
        documentRetriever.addCrawlerCallback(crawlerCallback);
    }

    public static void main(String[] args) {

        // ////////////////////////// how to use a crawler
        // ////////////////////////////
        // create the crawler object
        Crawler c = new Crawler();

        // create a callback that is triggered for every crawled page
        CrawlerCallback crawlerCallback = new CrawlerCallback() {

            @Override
            public void crawlerCallback(Document document) {
                // TODO write page to database
            }
        };
        c.addCrawlerCallback(crawlerCallback);

        // stop after 1000 pages have been crawled (default is unlimited)
        c.setStopCount(1000);

        // set the maximum number of threads to 1
        c.setMaxThreads(1);

        // the crawler should automatically use different proxies after every
        // 3rd request (default is no proxy switching)
        c.getDocumentRetriever().setSwitchProxyRequests(3);

        // set a list of proxies to choose from
        List<String> proxyList = new ArrayList<String>();
        proxyList.add("83.244.106.73:8080");
        proxyList.add("83.244.106.73:80");
        proxyList.add("67.159.31.22:8080");
        c.getDocumentRetriever().setProxyList(proxyList);

        // start the crawling process from a certain page, true = follow links
        // within the start domain, true = follow outgoing links
        c.startCrawl("http://www.dmoz.org/", true, true);
        // //////////////////////////how to use a crawler
        // ////////////////////////////
    }


}
