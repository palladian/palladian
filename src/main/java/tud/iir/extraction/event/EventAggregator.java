package tud.iir.extraction.event;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.Counter;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.URLDownloader;
import tud.iir.web.WebResult;
import tud.iir.web.URLDownloader.URLDownloaderCallback;

/**
 * @author Martin Wunderwald
 */
public class EventAggregator {
    /** The logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(EventAggregator.class);

    /** Used for all downloading purposes. */
    private static Crawler crawler = new Crawler();

    /** default maximum count of threads. */
    private static final int DEFAULT_MAX_THREADS = 20;

    /** default searchengine for news aggregation. */
    private static final int DEFAULT_SEARCH_ENGINE = SourceRetrieverManager.GOOGLE_NEWS;

    /** the result count. */
    private int resultCount = 10;

    /** aaggregated events. **/
    private List<Event> events = new ArrayList<Event>();

    /** the search query. **/
    private String query;

    /** the eventMap holding aggregated events. **/
    private Map<String, Event> eventMap;

    /** max Threads. **/
    private int maxThreads = DEFAULT_MAX_THREADS;

    /** search enging of your choice. **/
    private int searchEngine = DEFAULT_SEARCH_ENGINE;

    /**
     * Constructor.
     */
    public EventAggregator() {
        eventMap = new HashMap<String, Event>();
    }

    /**
     * run the aggregation.
     */
    public void aggregate() {

        final SourceRetriever sourceRetriever = new SourceRetriever();

        // setting resultCount
        sourceRetriever.setResultCount(resultCount);

        // set search result language to english
        sourceRetriever.setLanguage(SourceRetriever.LANGUAGE_ENGLISH);

        final Event tmp = new Event();
        tmp.setWebresults(sourceRetriever.getWebResults(query,
                DEFAULT_SEARCH_ENGINE, false));
        events.add(tmp);
        LOGGER.info("query: " + query);

        // initiating eventStack
        final Stack<Event> eventStack = new Stack<Event>();
        eventStack.addAll(events);

        // count number of running Threads
        final Counter threadCounter = new Counter();

        // count number of new entries
        final Counter newEntriesTotal = new Counter();

        // count number of encountered errors
        final Counter errors = new Counter();

        // count number of scraped pages
        final Counter downloadedPages = new Counter();
        // final Counter scrapeErrors = new Counter();

        // stopwatch for aggregation process
        final StopWatch stopWatch = new StopWatch();

        // reset traffic counter
        crawler.setTotalDownloadSize(0);

        while (eventStack.size() > 0) {
            final Event event = eventStack.pop();

            // if maximum # of Threads are already running, wait here
            while (threadCounter.getCount() >= maxThreads) {
                LOGGER.trace("max # of Threads running. waiting ...");
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    break;
                }
            }

            threadCounter.increment();
            final Runnable runnable = new Runnable() {
                @Override
                public void run() {

                    LOGGER.info("aggregating news");
                    try {

                        fetchPageContentIntoEvents(event.getWebresults());
                        downloadedPages.increment(event.getWebresults().size());
                        newEntriesTotal.increment();

                    } catch (final Exception e) {
                        errors.increment();
                        LOGGER.error(e);
                    } finally {
                        threadCounter.decrement();
                    }

                }
            };
            new Thread(runnable).start();
        }
        while (threadCounter.getCount() > 0 || eventStack.size() > 0) {
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                LOGGER.warn(e.getMessage());
                break;
            }
            LOGGER.trace("waiting ... threads:" + threadCounter.getCount()
                    + " stack:" + eventStack.size());
        }
        stopWatch.stop();

        LOGGER.info("-------------------------------");
        LOGGER.info(" # of aggregated events: " + events.size());
        LOGGER.info(" # new entries total: " + newEntriesTotal.getCount());
        LOGGER.info(" # errors: " + errors.getCount());
        LOGGER.info(" # downloaded pages: " + downloadedPages);
        // LOGGER.info(" # scrape errors: " + scrapeErrors);
        LOGGER.info(" elapsed time: " + stopWatch.getElapsedTimeString());
        LOGGER.info(" traffic: "
                + crawler.getTotalDownloadSize(Crawler.MEGA_BYTES) + " MB");
        LOGGER.info("-------------------------------");
        LOGGER.trace("<aggregate");

    }

    /**
     * Fetches page content into a map of events.
     *
     * @param webresults
     */
    private void fetchPageContentIntoEvents(final List<WebResult> webresults) {

        LOGGER.info("downloading " + webresults.size() + " pages");

        final URLDownloader downloader = new URLDownloader();
        downloader.setMaxThreads(5);

        for (final WebResult wr : webresults) {
            downloader.add(wr.getUrl());
            eventMap.put(wr.getUrl(), new Event(wr.getUrl()));
        }

        downloader.start(new URLDownloaderCallback() {
            @Override
            public void finished(final String url, final InputStream inputStream) {
                try {
                    final PageContentExtractor extractor = new PageContentExtractor();
                    extractor.setDocument(new InputSource(inputStream));
                    // Document page = extractor.getResultDocument();
                    eventMap.get(url).setText(extractor.getResultText());
                    eventMap.get(url).setTitle(extractor.getResultTitle());

                } catch (final PageContentExtractorException e) {
                    LOGGER.error("PageContentExtractorException " + e);
                }
            }
        });
        LOGGER.info("finished downloading");

    }

    /**
     * @return the event map
     */
    public final Map<String, Event> getEventmap() {
        return eventMap;
    }

    /**
     * Sets the maximum number of parallel threads when aggregating or adding
     * multiple new feeds.
     *
     * @param maxThreads
     */
    public final void setMaxThreads(final int maxThreads) {
        this.maxThreads = maxThreads;
    }

    /**
     * @return the resultCount
     */
    public final int getResultCount() {
        return resultCount;
    }

    /**
     * @param resultCount
     *            the resultCount to set
     */
    public final void setResultCount(final int resultCount) {
        this.resultCount = resultCount;
    }

    /**
     * @return the events
     */
    public final List<Event> getEvents() {
        return events;
    }

    /**
     * @param events
     *            the events to set
     */
    public final void setEvents(final List<Event> events) {
        this.events = events;
    }

    /**
     * @return the query
     */
    public final String getQuery() {
        return query;
    }

    /**
     * @param query
     *            the query to set
     */
    public final void setQuery(final String query) {
        this.query = query;
    }

    /**
     * @return the eventMap
     */
    public final Map<String, Event> getEventMap() {
        return eventMap;
    }

    /**
     * @param eventMap
     *            the eventMap to set
     */
    public final void setEventMap(final Map<String, Event> eventMap) {
        this.eventMap = eventMap;
    }

    /**
     * @return the searchEngine
     */
    public final int getSearchEngine() {
        return searchEngine;
    }

    /**
     * @param searchEngine
     *            the searchEngine to set
     */
    public final void setSearchEngine(final int searchEngine) {
        this.searchEngine = searchEngine;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        final EventAggregator eag = new EventAggregator();
        eag.setMaxThreads(10);
        eag.setQuery("pakistan flood");
        eag.aggregate();

    }

}
