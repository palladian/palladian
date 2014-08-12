package ws.palladian.retrieval;

import java.util.Collection;
import java.util.Collections;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Consumer;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.retrieval.helper.FixedIntervalRequestThrottle;
import ws.palladian.retrieval.helper.NoThrottle;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * @author Philipp Katz
 */
public class HttpCrawler {

    public static final class NoRetryPolicy implements RetryPolicy {
        public static final RetryPolicy INSTANCE = new NoRetryPolicy();

        @Override
        public boolean shouldRetry(int attempt, HttpResult result) {
            return false;
        }
    }

    public static interface RetryPolicy {
        /**
         * Decide, whether to retry after an unsuccessful attempt (i.e. HTTP error code).
         * 
         * @param attempt The attempt (starting with one).
         * @param result The {@link HttpResult}.
         * @return <code>true</code> to perform a further attempt, <code>false</code> to give up.
         */
        boolean shouldRetry(int attempt, HttpResult result);
    }

    private final class RetrievalTask implements Runnable {

        @Override
        public void run() {
            for (;;) {
                String url = takeNextUrl();
                LOGGER.debug("Fetching {}", url);
                for (int attempt = 1;; attempt++) {
                    try {
                        throttle.hold();
                        HttpResult result = httpRetriever.httpGet(url);
                        if (result.errorStatus()) {
                            if (retryPolicy.shouldRetry(attempt, result)) {
                                LOGGER.info("Attempt {} for {}", attempt, url);
                                continue;
                            }
                            LOGGER.info("Giving up for {}", url);
                            break; // policy say: no more retries
                        } else {
                            action.process(result);
                            // extract new links
                            Document document = htmlParser.parse(result);
                            Set<String> links = HtmlHelper.getLinks(document, true, true);
                            int retrievedLinks = links.size();
                            CollectionHelper.remove(links, urlFilter);
                            int addedLinks = add(links);
                            LOGGER.debug("Extracted {} new, filtered {}, added {} URLs from {}", new Object[] {
                                    retrievedLinks, links.size(), addedLinks, url});
                            break;
                        }
                    } catch (Throwable t) {
                        LOGGER.error("Encountered {} for {}", t.getMessage(), url);
                    }
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

    private final Queue<String> urlQueue;

    private final Set<String> checkedUrls;

    private final HttpRetriever httpRetriever;

    private final DocumentParser htmlParser;

    private final Filter<String> urlFilter;

    private final Consumer<HttpResult> action;

    private final RequestThrottle throttle;

    private final RetryPolicy retryPolicy;

    public HttpCrawler(Filter<String> urlFilter, Consumer<HttpResult> action) {
        this(urlFilter, action, NoThrottle.INSTANCE);
    }

    public HttpCrawler(Filter<String> urlFilter, Consumer<HttpResult> action, RequestThrottle throttle) {
        this(urlFilter, action, throttle, NoRetryPolicy.INSTANCE);
    }

    public HttpCrawler(Filter<String> urlFilter, Consumer<HttpResult> action, RequestThrottle throttle,
            RetryPolicy retryPolicy) {
        urlQueue = new ConcurrentLinkedQueue<String>();
        checkedUrls = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        htmlParser = ParserFactory.createHtmlParser();
        this.urlFilter = urlFilter;
        this.action = action;
        this.throttle = throttle;
        this.retryPolicy = retryPolicy;
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

    private String takeNextUrl() {
        for (;;) {
            String url = urlQueue.poll();
            if (url != null) {
                checkedUrls.add(url);
                return url;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // ignore.
            }
        }
    }

    public void start() {
        for (int i = 0; i < NUM_THREADS; i++) {
            new Thread(new RetrievalTask()).start();
        }
        new Thread(new MonitoringTask()).start();
    }

    public static void main(String[] args) {
        Filter<String> urlFilter = Filters.regex("http://www.breakingnews.com/topic/.*");
        HttpCrawler crawler = new HttpCrawler(urlFilter, new Consumer<HttpResult>() {
            @Override
            public void process(HttpResult result) {
                System.out.println("Fetched " + result.getUrl());
            }
        }, new FixedIntervalRequestThrottle(100, TimeUnit.MILLISECONDS));
        crawler.add("http://www.breakingnews.com");
        crawler.start();
    }

}
