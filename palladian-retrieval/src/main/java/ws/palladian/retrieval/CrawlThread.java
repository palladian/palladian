package ws.palladian.retrieval;

import org.apache.log4j.Logger;

/**
 * One thread for the crawler.
 * 
 * @author David Urbansky
 */
class CrawlThread extends Thread {
    /**
     * <p>
     * The logger for this class.
     * </p>
     */
    private static final Logger LOGGER = Logger.getLogger(CrawlThread.class);
    /**
     * <p>
     * The crawler to run by this thread.
     * </p>
     */
    private final transient Crawler crawler;
    /**
     * <p>
     * The URL of the page the crawler should start on.
     * </p>
     */
    private final transient String url;

    /**
     * <p>
     * Creates a new thread to run a crawler on starting on a specific Uniform Resource Locator (URL).
     * </p>
     * 
     * @param crawler The crawler to run by this thread.
     * @param url The URL of the page the crawler should start on.
     * @param threadGroup
     * @param threadName
     */
    public CrawlThread(Crawler crawler, String url, ThreadGroup threadGroup, String threadName) {
        super(threadGroup, threadName);
        LOGGER.debug(this);
        this.crawler = crawler;
        this.url = url;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
        crawler.crawl(url);
        crawler.decreaseThreadCount();
    }
}
