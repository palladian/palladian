package ws.palladian.retrieval.feeds;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.http.impl.cookie.DateUtils;
import org.apache.log4j.Logger;

import sun.net.www.protocol.http.HttpURLConnection;
import ws.palladian.helper.HTTPHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.meta.MetaInformationExtractor;

/**
 * <p>
 * The {@link FeedReader} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all set {@link FeedProcessingAction}s.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * @see FeedReader
 * 
 */
class FeedTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(FeedTask.class);

    /**
     * The feed retrieved by this task.
     */
    private Feed feed = null;

    /**
     * The feed checker calling this task. // FIXME This is a workaround. Can be fixed by externalizing update
     * strategies to a true strategy pattern.
     */
    private final FeedReader feedReader;

    /**
     * Warn if processing of a feed takes longer than this.
     */
    public static final long EXECUTION_WARN_TIME = 3 * DateHelper.MINUTE_MS;

    /**
     * The result of this task.
     */
    private FeedTaskResult result = FeedTaskResult.OPEN;

    /**
     * Additional header elements used in HTTP requests.
     */
    private Map<String, String> requestHeaders = new HashMap<String, String>();

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public FeedTask(Feed feed, FeedReader feedChecker) {
        // setName("FeedTask:" + feed.getFeedUrl());
        this.feed = feed;
        this.feedReader = feedChecker;
        createBasicRequestHeaders();
    }

    /**
     * Create basic request headers.
     * Set cache-control: no-cache to prevent getting cached results.
     */
    private void createBasicRequestHeaders() {
        requestHeaders.put("cache-control", "no-cache");

    }

    // /**
    // * Replace the request headers by the given ones.
    // *
    // * @param requestHeaders New request headers to set.
    // */
    // private void setRequestHeaders(Map<String, String> requestHeaders) {
    // this.requestHeaders = requestHeaders;
    // }

    /**
     * Add a key value pair to request headers.
     * 
     * @param key The name of the header.
     * @param value The header's value.
     */
    private void addRequestHeader(String key, String value) {
        this.requestHeaders.put(key, value);
    }

    /**
     * Get the headers to use in a HTTP request.
     * 
     * @return The headers to use in a HTTP request.
     */
    private Map<String, String> getRequestHeaders() {
        return requestHeaders;
    }

    // TODO very long method, break into pieces.
    @Override
    public FeedTaskResult call() {
        StopWatch timer = new StopWatch();
        try {
            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl() + ")");
            int recentMisses = feed.getMisses();
            boolean storeMetadata = false;

            // update http request headers to use conditional requests (head requests)
            if (feed.getLastETag() != null && !feed.getLastETag().isEmpty()) {
                addRequestHeader("If-None-Match", feed.getLastETag());
            }
            if (feed.getHttpLastModified() != null) {
                addRequestHeader("If-Modified-Since", DateUtils.formatDate(feed.getHttpLastModified()));
            }

            DocumentRetriever documentRetriever = new DocumentRetriever();
            HttpResult httpResult = null;

            // remember the time the feed has been checked
            feed.setLastPollTime(new Date());

            try {
                httpResult = documentRetriever.httpGet(feed.getFeedUrl(), getRequestHeaders());
            } catch (HttpException e) {
                LOGGER.error("Could not get Document for feed id " + feed.getId() + " , " + e.getMessage());
                feed.incrementUnreachableCount();
                feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
                boolean dbSuccess = feedReader.updateFeed(feed);
                if (dbSuccess && result == FeedTaskResult.OPEN) {
                    result = FeedTaskResult.UNREACHABLE;
                } else {
                    result = FeedTaskResult.ERROR;
                }
                doFinalLogging(timer);
                return result;
            }

            if (httpResult.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                LOGGER.error("Could not get Document for feed id " + feed.getId()
                        + ". Server returned HTTP status code " + httpResult.getStatusCode());
                feed.incrementUnreachableCount();
                boolean actionSuccess = feedReader.getFeedProcessingAction().performActionOnHighHttpStatusCode(feed,
                        httpResult);
                if (actionSuccess && result == FeedTaskResult.OPEN) {
                    result = FeedTaskResult.UNREACHABLE;
                } else {
                    result = FeedTaskResult.ERROR;
                }
            } else {

                if (httpResult.getStatusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {

                    // TODO feedReader.updateCheckIntervals(feed); requires the old item timestamps and window size

                    feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                    boolean actionSuccess = feedReader.getFeedProcessingAction().performActionOnUnmodifiedFeed(feed,
                            httpResult);
                    if (!actionSuccess) {
                        result = FeedTaskResult.ERROR;
                    }

                } else {

                    // process and store http header information
                    feed.setLastETag(httpResult.getHeaderString("ETag"));
                    feed.setHttpLastModified(HTTPHelper.getDateFromHeader(httpResult, "Last-Modified"));

                    FeedRetriever feedRetriever = new FeedRetriever();
                    Feed downloadedFeed = null;
                    try {
                        // parse the feed and get all its entries, do that here since that takes some time and this is a
                        // thread so it can be done in parallel
                        downloadedFeed = feedRetriever.getFeed(httpResult);
                        feed.setItems(downloadedFeed.getItems());
                    } catch (FeedRetrieverException e) {
                        LOGGER.error("update items of feed id " + feed.getId() + " didn't work well, " + e.getMessage());
                        feed.incrementUnreachableCount();
                        feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
                        feedReader.updateFeed(feed);
                        LOGGER.debug("Performing action on error on feed: " + feed.getId() + "(" + feed.getFeedUrl()
                                + ")");
                        boolean actionSuccess = feedReader.getFeedProcessingAction().performActionOnException(feed,
                                httpResult);
                        if (actionSuccess && result == FeedTaskResult.OPEN) {
                            result = FeedTaskResult.UNREACHABLE;
                        } else {
                            result = FeedTaskResult.ERROR;
                        }
                        doFinalLogging(timer);
                        return result;
                    }
                    feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                    feed.setWindowSize(downloadedFeed.getItems().size());

                    // if (LOGGER.isDebugEnabled()) {
                    // LOGGER.debug("Activity Pattern: " + feed.getActivityPattern());
                    // LOGGER.debug("Current time: " + System.currentTimeMillis());
                    // LOGGER.debug("Last poll time: " + feed.getLastPollTime().getTime());
                    // LOGGER.debug("Current time - last poll time: "
                    // + (System.currentTimeMillis() - feed.getLastPollTime().getTime()));
                    // LOGGER.debug("Milliseconds in a month: " + DateHelper.MONTH_MS);
                    // }

                    // classify feed if it has never been classified before, do it once a month for each feed to be
                    // informed
                    // about updates
                    if (feed.getActivityPattern() == -1 || feed.getLastPollTime() != null
                            && (System.currentTimeMillis() - feed.getLastPollTime().getTime()) > DateHelper.MONTH_MS) {

                        storeMetadata = true;
                        FeedClassifier.classify(feed);
                        MetaInformationExtractor metaInfExt = new MetaInformationExtractor(httpResult);
                        metaInfExt.updateGeneralMetaInformation(feed);
                        feed.getMetaInformation().setTitle(downloadedFeed.getMetaInformation().getTitle());
                        feed.getMetaInformation().setByteSize(downloadedFeed.getMetaInformation().getByteSize());
                        feed.getMetaInformation().setLanguage(downloadedFeed.getMetaInformation().getLanguage());
                    }

                    feedReader.updateCheckIntervals(feed);

                    // perform actions on this feeds entries
                    LOGGER.debug("Performing action on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
                    boolean actionSuccess = feedReader.getFeedProcessingAction().performAction(feed, httpResult);
                    if (!actionSuccess) {
                        result = FeedTaskResult.ERROR;
                    }
                }
            }

            // /////////////////////////////////
            // final stuff to do in all cases //
            // /////////////////////////////////
            feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());

            // save the feed back to the database
            boolean dbSuccess = feedReader.updateFeed(feed, storeMetadata);
            if (!dbSuccess) {
                result = FeedTaskResult.ERROR;
            }

            // since the feed is kept in memory we need to remove all items and the document stored in the feed
            feed.freeMemory();

            if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
                LOGGER.warn("Processing feed id " + feed.getId() + " took very long: " + timer.getElapsedTimeString());
            }
            
            if(result == FeedTaskResult.OPEN){
                if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
                    result = FeedTaskResult.EXECUTION_TIME_WARNING;
                } else if (recentMisses < feed.getMisses()) {
                    result = FeedTaskResult.MISS;
                } else {
                    // finally, if no other status has been assigned, the task seems to bee successful
                    result = FeedTaskResult.SUCCESS;
                }
            }

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error("Error processing feedID " + feed.getId() + ": " + th);
            result = FeedTaskResult.ERROR;
        }
        doFinalLogging(timer);
        return result;
    }

    /**
     * Do final logging of result.
     * 
     * @param timer the {@link StopWatch} started when started processing the feed.
     */
    private void doFinalLogging(StopWatch timer) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Finished processing of feed id " + feed.getId() + ". Result: " + result
                    + ". Processing took " + timer.getElapsedTimeString());
        }
    }


}