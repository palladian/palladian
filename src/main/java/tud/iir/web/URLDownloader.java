package tud.iir.web;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Stack;

import org.apache.log4j.Logger;

import tud.iir.helper.Counter;
import tud.iir.helper.ThreadHelper;

/**
 * Allows simultanous downloading of multiple URLs. The resulting InputStreams
 * are cached by this class and can be processed after all downloads are done.
 * 
 * TODO merge this into Crawler.
 * 
 * @author Philipp Katz
 * 
 */
public class URLDownloader {

    public interface URLDownloaderCallback {

        void finished(String url, InputStream inputStream);

    }

    private static final Logger logger = Logger.getLogger(URLDownloader.class);

    private Crawler crawler = new Crawler();
    private Stack<String> urlStack = new Stack<String>();
    private HashMap<String, InputStream> downloadedUrls = new HashMap<String, InputStream>();

    private int maxThreads = 10;
    
    private int maxFails = 10;

    public void start(final URLDownloaderCallback callback) {

        logger.trace(">start");

        // to count number of running Threads
        final Counter counter = new Counter();
        
        final Counter errors = new Counter();

        while (urlStack.size() > 0) {
            final String url = urlStack.pop();

            // if maximum # of Threads are already running, wait here
            while (counter.getCount() >= getMaxThreads()) {
                logger.trace("max # of Threads running. waiting ...");
                ThreadHelper.sleep(1000);
            }
            
            if (errors.getCount() == getMaxFails()) {
                logger.warn("max. fails of " + getMaxFails() + " reached. giving up.");
                return;
            }

            counter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.trace("start downloading " + url);
                        InputStream inputStream = crawler.downloadInputStream(url);
                        callback.finished(url, inputStream);
                        logger.trace("finished downloading " + url);
                    } catch (Exception e) {
                        logger.warn(e.getMessage() + " for " + url);
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
            ThreadHelper.sleep(1000);
            logger.trace("waiting ... threads:" + counter.getCount() + " stack:" + urlStack.size());
        }
        logger.trace("<start");
    }

    public void start() {
        logger.trace(">start");

        // to count number of running Threads
        final Counter counter = new Counter();

        while (urlStack.size() > 0) {
            final String url = urlStack.pop();

            // if maximum # of Threads are already running, wait here
            while (counter.getCount() >= getMaxThreads()) {
                logger.trace("max # of Threads running. waiting ...");
                ThreadHelper.sleep(1000);
            }

            counter.increment();
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        logger.trace("start downloading " + url);
                        InputStream inputStream = crawler.downloadInputStream(url);
                        downloadedUrls.put(url, inputStream);
                        /*
                         * InputStream inputStream = urlObj.openStream();
                         * ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                         * byte[] buffer = new byte[1024];
                         * int length;
                         * while ((length = inputStream.read(buffer)) >= 0) {
                         * outputStream.write(buffer, 0, length);
                         * }
                         * inputStream.close();
                         * outputStream.close();
                         * ByteArrayInputStream result = new ByteArrayInputStream(outputStream.toByteArray());
                         * downloadedUrls.put(url, result);
                         */
                        logger.trace("finished downloading " + url);
                    } catch (Exception e) {
                        logger.warn(e.getMessage() + " for " + url);
                    } finally {
                        counter.decrement();
                    }
                }
            };
            new Thread(runnable).start();
        }

        // keep on running until all Threads have finished and
        // the Stack is empty
        while (counter.getCount() > 0 || urlStack.size() > 0) {
            ThreadHelper.sleep(1000);
            logger.trace("waiting ... threads:" + counter.getCount() + " stack:" + urlStack.size());
        }

        logger.trace("<start");
    }

    public void add(String urlString) {
        if (!urlStack.contains(urlString)) {
            urlStack.push(urlString);
        }
    }

    public InputStream get(String urlString) {
        return downloadedUrls.get(urlString);
    }

    public Collection<InputStream> getAll() {
        return downloadedUrls.values();
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }
    
    public void setMaxFails(int maxFails) {
        this.maxFails = maxFails;
    }
    
    public int getMaxFails() {
        return maxFails;
    }

    public static void main(String[] args) throws MalformedURLException {
        URLDownloader downloader = new URLDownloader();

        downloader.setMaxThreads(10);

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
