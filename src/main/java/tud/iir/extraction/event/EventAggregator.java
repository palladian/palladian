package tud.iir.extraction.event;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xml.sax.InputSource;

import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.Counter;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.knowledge.Source;
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

    /** default maximum count of threads */
    private static final int DEFAULT_MAX_THREADS = 20;

    /** default searchengine for news aggregation */
    private static final int DEFAULT_SEARCH_ENGINE = SourceRetrieverManager.GOOGLE_NEWS;

    /** resultCount */
    private int resultCount = 10;

    private List<Event> events = new ArrayList<Event>();

    private String query;

    private final Map<String, Event> eventMap;

    private int maxThreads = DEFAULT_MAX_THREADS;

    private int searchEngine = DEFAULT_SEARCH_ENGINE;

    public EventAggregator() {
        eventMap = new HashMap<String, Event>();
    }

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
                ThreadHelper.sleep(1000);
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
            ThreadHelper.sleep(1000);
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

    private void fetchPageContentIntoEvents(List<WebResult> webresults) {

        LOGGER.info("downloading " + webresults.size() + " pages");

        final URLDownloader downloader = new URLDownloader();
        downloader.setMaxThreads(5);

        for (final WebResult wr : webresults) {
            downloader.add(wr.getUrl());
            eventMap.put(wr.getUrl(), new Event(wr.getUrl()));
        }

        downloader.start(new URLDownloaderCallback() {
            @Override
            public void finished(String url, InputStream inputStream) {
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

    public ArrayList<WebResult> getNewsResultsFromGoogle(String searchQuery,
            String languageCode) {

        final ArrayList<WebResult> webresults = new ArrayList<WebResult>();

        final Crawler c = new Crawler();

        // set the preferred language
        // (http://www.google.com/cse/docs/resultsxml.html#languageCollections)
        String languageString = "lang_en";

        if (languageCode.length() > 0) {
            languageString = languageCode;
        }

        int rank = 1;
        int urlsCollected = 0;
        int grabCount = (int) Math.ceil(getResultCount() / 8.0); // divide by 8
        // because 8
        // results will
        // be responded
        // by
        // each query
        // Google returns max. 8 pages/64 results --
        // http://code.google.com/intl/de/apis/ajaxsearch/documentation/reference.html#_property_GSearch
        grabCount = Math.min(grabCount, 8);
        // System.out.println(grabSize);
        for (int i = 0; i < grabCount; i++) {

            // rsz=large will respond 8 results
            final String json = c
                    .download("http://ajax.googleapis.com/ajax/services/search/news?v=1.0&start="
                            + (i * 8)
                            + "&rsz=large&safe=off&lr="
                            + languageString + "&q=" + searchQuery);

            try {
                final JSONObject jsonOBJ = new JSONObject(json);

                // System.out.println(jsonOBJ.toString(1));
                // in the first iteration find the maximum of available pages
                // and limit the search to those
                if (i == 0) {
                    JSONArray pages;
                    if (jsonOBJ.getJSONObject("responseData") != null
                            && jsonOBJ.getJSONObject("responseData")
                                    .getJSONObject("cursor") != null
                            && jsonOBJ.getJSONObject("responseData")
                                    .getJSONObject("cursor").getJSONArray(
                                            "pages") != null) {
                        pages = jsonOBJ.getJSONObject("responseData")
                                .getJSONObject("cursor").getJSONArray("pages");
                        final int lastStartPage = pages.getJSONObject(
                                pages.length() - 1).getInt("start");
                        if (lastStartPage < grabCount) {
                            grabCount = lastStartPage + 1;
                        }
                    }
                }

                final JSONArray results = jsonOBJ.getJSONObject("responseData")
                        .getJSONArray("results");
                final int resultSize = results.length();
                for (int j = 0; j < resultSize; ++j) {
                    if (urlsCollected < getResultCount()) {
                        // TODO: webresult.setTitle(title);
                        final String title = null;
                        final String summary = (String) results
                                .getJSONObject(j).get("content");

                        final String currentURL = (String) results
                                .getJSONObject(j).get("unescapedUrl");

                        final WebResult webresult = new WebResult(
                                SourceRetrieverManager.GOOGLE, rank,
                                new Source(currentURL), title, summary);
                        rank++;

                        LOGGER.info("google retrieved url " + currentURL);
                        webresults.add(webresult);

                        ++urlsCollected;
                    } else {
                        break;
                    }
                }

            } catch (final JSONException e) {
                LOGGER.error(e.getMessage());
            }

            // srManager.addRequest(SourceRetrieverManager.GOOGLE);
            // LOGGER.info("google requests: " +
            // srManager.getRequestCount(SourceRetrieverManager.GOOGLE));
        }

        return webresults;
    }

    public Map<String, Event> getEventmap() {
        return eventMap;
    }

    /**
     * Sets the maximum number of parallel threads when aggregating or adding
     * multiple new feeds.
     * 
     * @param maxThreads
     */
    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public int getSearchEngine() {
        return searchEngine;
    }

    public void setSearchEngine(int searchEngine) {
        this.searchEngine = searchEngine;
    }

    public static void main(String[] args) {

        final EventAggregator ea = new EventAggregator();
        ea.setMaxThreads(10);
        ea.setQuery("pakistan flood");
        ea.aggregate();

    }

}
