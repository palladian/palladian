package ws.palladian.retrieval;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * @author Philipp Katz
 */
public class HttpCrawler {

    public static interface CrawlAction {

        void pageCrawled(HttpResult result);

    }

    private final class RetrievalTask implements Runnable {

        private final Queue<String> urlQueue;

        public RetrievalTask(Queue<String> urlQueue) {
            this.urlQueue = urlQueue;
        }

        @Override
        public void run() {
            for (;;) {
                String url = urlQueue.poll();
                if (url == null) {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        // ignore.
                    }
                    continue;
                }
                LOGGER.debug("Fetching {}", url);
                try {
                    HttpResult result = httpRetriever.httpGet(url);
                    action.pageCrawled(result);
                    // extract new links
                    Document document = htmlParser.parse(result);
                    Set<String> links = HtmlHelper.getLinks(document, true, true);
                    int retrievedLinks = links.size();
                    CollectionHelper.filter(links, urlFilter);
                    int addedLinks = add(links);
                    LOGGER.debug("Extracted {} new, filtered {}, added {} URLs from {}", new Object[] {retrievedLinks,
                            links.size(), addedLinks, url});
                } catch (Throwable t) {
                    LOGGER.error("Encountered {} for {}", t.getMessage(), url);
                } finally {
                    checkedUrls.add(url);
                }
            }
        }
    }

    private final class MonitoringTask implements Runnable {
        @Override
        public void run() {
            for (;;) {
                LOGGER.info("Queue: {}, processed: {}", urlQueue.size(), checkedUrls.size());
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpCrawler.class);

    private static final int NUM_THREADS = 10;

    public static final Filter<String> ACCEPT_ALL_FILTER = new Filter<String>() {
        @Override
        public boolean accept(String item) {
            return true;
        }
    };

    private final Queue<String> urlQueue;

    private final Set<String> checkedUrls;

    private final HttpRetriever httpRetriever;

    private final DocumentParser htmlParser;

    private final Filter<String> urlFilter;

    private final CrawlAction action;

    public HttpCrawler(Filter<String> urlFilter, CrawlAction action) {
        urlQueue = new ConcurrentLinkedQueue<String>();
        checkedUrls = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        htmlParser = ParserFactory.createHtmlParser();
        this.urlFilter = urlFilter;
        this.action = action;
    }

    public boolean add(String url) {
        return add(Collections.singleton(url)) == 1;
    }

    public int add(Collection<String> urls) {
        int added = 0;
        synchronized (urlQueue) {
            for (String url : urls) {
                if (checkedUrls.contains(url)) {
                    continue;
                }
                if (!urlQueue.contains(url)) {
                    urlQueue.add(url);
                    added++;
                }
            }
        }
        return added;
    }

    public void start() {
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(new RetrievalTask(urlQueue)).start();
        }
        new Thread(new MonitoringTask()).start();
    }

    public static void main(String[] args) {
        Filter<String> urlFilter = new Filter<String>() {
            @Override
            public boolean accept(String item) {
                return item.startsWith("http://geizhals.de/?cat=monlcd19wide&pg=");
            }
        };
        HttpCrawler crawler = new HttpCrawler(urlFilter, new CrawlAction() {
            @Override
            public void pageCrawled(HttpResult result) {
                System.out.println("Fetched " + result.getUrl());
                // save the result to disk:
                // String filePath = "/path/" + result.getUrl().hashCode();
                // HttpHelper.saveToFile(result, filePath, true);
            }
        });
        crawler.add("http://geizhals.de/?cat=monlcd19wide");
        crawler.start();
    }

}
