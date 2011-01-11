package tud.iir.web;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.helper.Counter;

/**
 * Allows simultaneous downloading of multiple URLs. The resulting InputStreams are cached by this class and can be
 * processed after all downloads are done.
 * 
 * TODO merge this into Crawler.
 * 
 * @author Philipp Katz
 * 
 */
public class URLDownloader {

    /**
     * Callback interface to be used with the URLDownloader. For each downloaded URL, the
     * {@link URLDownloaderCallback#finished(String, InputStream)} method is called.
     * 
     * @author Philipp Katz
     * 
     */
    public interface URLDownloaderCallback {

        void finished(String url, InputStream inputStream);

    }

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(URLDownloader.class);

    /** The crawler used for downloading. */
    private Crawler crawler = new Crawler();

    /** The stack to store URLs to be downloaded. */
    private Stack<String> urlStack = new Stack<String>();

    private Set<Document> downloadedDocuments = new HashSet<Document>();

    private int maxThreads = 10;

    private int maxFails = 10;

    /**
     * Start downloading the supplied URLs.
     * 
     * @param callback The callback to be called for each finished download. If the callback is null, the method will
     *            save the downloaded documents and return them when finished.
     */
    public Set<Document> start(final URLDownloaderCallback callback) {

        LOGGER.trace(">start");

        downloadedDocuments = new HashSet<Document>();

        // to count number of running Threads
        final Counter counter = new Counter();

        final Counter errors = new Counter();

        while (urlStack.size() > 0) {
            final String url = urlStack.pop();

            LOGGER.debug("process url " + url);

            // if maximum # of Threads are already running, wait here
            while (counter.getCount() >= getMaxThreads()) {
                LOGGER.trace("max # of Threads running. waiting ...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    return downloadedDocuments;
                }
            }

            if (errors.getCount() == getMaxFails()) {
                LOGGER.warn("max. fails of " + getMaxFails() + " reached. giving up.");
                return new HashSet<Document>();
            }

            counter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        LOGGER.trace("start downloading " + url);
                        InputStream inputStream = crawler.downloadInputStream(url);

                        if (callback == null) {
                            Document document = crawler.getWebDocument(inputStream, url);
                            synchronized (downloadedDocuments) {
                                downloadedDocuments.add(document);
                            }
                        } else {
                            callback.finished(url, inputStream);
                        }

                        LOGGER.trace("finished downloading " + url);
                    } catch (Exception e) {
                        LOGGER.warn(e.getMessage() + " for " + url);
                        errors.increment();
                    } finally {
                        counter.decrement();
                    }
                }
            };
            new Thread(runnable, "URLDownloaderThread:" + url).start();
        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (counter.getCount() > 0 || urlStack.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LOGGER.warn(e.getMessage());
                return downloadedDocuments;
            }
            LOGGER.debug("waiting ... threads:" + counter.getCount() + " stack:" + urlStack.size());
        }
        LOGGER.trace("<start");

        return downloadedDocuments;
    }

    public Set<Document> start() {
        return start(null);
    }
    /**
     * Add a URL to be downloaded.
     * 
     * @param urlString
     */
    public void add(String urlString) {
        if (!urlStack.contains(urlString)) {
            urlStack.push(urlString);
        }
    }

    /**
     * Add a collection of URLs to be downloaded.
     * 
     * @param urls A collection of URLs.
     */
    public void add(Collection<String> urls) {
        for (String url : urls) {
            if (!urlStack.contains(url)) {
                urlStack.push(url);
            }
        }
    }

    /**
     * Set the maximum number of simultaneous threads for downloading.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    /**
     * Set the maximum number of failures to stop the download.
     * 
     * @param maxFails
     */
    public void setMaxFails(int maxFails) {
        this.maxFails = maxFails;
    }

    public int getMaxFails() {
        return maxFails;
    }

    public static void main(String[] args) {

        // usage example ...:
        URLDownloader downloader = new URLDownloader();
        downloader.setMaxThreads(3);

        downloader.add("http://www.tagesschau.de/");
        downloader.add("http://www.spiegel.de/");
        downloader.add("http://www.taz.de/");
        downloader.add("http://www.spreeblick.com/");
        downloader.add("http://faz.net/");
        downloader.add("http://www.gizmodo.de/");
        downloader.add("http://www.heise.de/");
        downloader.add("http://www.readwriteweb.com/");

        downloader.start(new URLDownloaderCallback() {

            @Override
            public void finished(String url, InputStream inputStream) {
                System.out.println("finished " + url);
            }
        });

        System.out.println("done");

    }

}
