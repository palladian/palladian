package ws.palladian.retrieval.wiki;

import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.wiki.data.WikiPage;

/**
 * Example how to implement a page consumer class that processes new or updated pages, produced by a
 * {@link MediaWikiCrawler}
 * 
 * @author Sandro Reichert
 */
public class PageConsumer implements Runnable {

    /** The global logger */
    private static final Logger LOGGER = Logger.getLogger(PageConsumer.class);

    /** do not call LOGGER.isDebugEnabled() 1000 times */
    private static final boolean DEBUG = LOGGER.isDebugEnabled();

    /** do not call LOGGER.isInfoEnabled() 1000 times */
    // private static final boolean INFO = LOGGER.isInfoEnabled();

    /** Flag, checked periodically to stop the thread if set to true. */
    private boolean stopThread = false;

    /**
     * Synchronized FIFO queue to read new or updated pages from. The queue is filled by one or multiple
     * {@link MediaWikiCrawler}s.
     */
    private final LinkedBlockingQueue<WikiPage> pageQueue;


    /**
     * @param pageQueue Synchronized FIFO queue to read new or updated pages from. This queue is filled by one or
     *            multiple {@link MediaWikiCrawler}s.
     */
    public PageConsumer(LinkedBlockingQueue<WikiPage> pageQueue) {
        this.pageQueue = pageQueue;
    }

    /**
     * @param page The {@link WikiPage} to process
     */
    protected void consume(WikiPage page) {
        if (DEBUG) {
            LOGGER.debug("Processing WikiPage \"" + page.getTitle() + "\"");
        }
    }

    /**
     * Helper to stop this {@link PageConsumer} {@link Thread}. Sets the internal flag to stop the current
     * {@link Thread}. The stop-flag is checked periodically.
     */
    public synchronized void stopPageConsumer() {
        stopThread = true;
    }

    /**
     * Helper to stop this {@link PageConsumer} {@link Thread}. If {@code true} is returned, the current thread
     * should stop.
     * 
     * @return {@code true} if the current thread should stop.
     */
    private synchronized boolean threadShouldStop() {
        return stopThread;
    }

    @Override
    public void run() {
        if (DEBUG) {
            LOGGER.debug("Start processing WikiPages.");
        }

        while (true) {
            if (threadShouldStop()) {
                break;
            }

            if (DEBUG) {
                LOGGER.debug("Current page queue length = " + pageQueue.size());
            }

            try {
                consume(pageQueue.take());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (DEBUG) {
            LOGGER.debug("PageConsumer has been stopped. Goodbye!");
        }
    }

}
