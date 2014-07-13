package ws.palladian.retrieval.feeds;

import static ws.palladian.retrieval.feeds.FeedTaskResult.ERROR;
import static ws.palladian.retrieval.feeds.FeedTaskResult.EXECUTION_TIME_WARNING;
import static ws.palladian.retrieval.feeds.FeedTaskResult.MISS;
import static ws.palladian.retrieval.feeds.FeedTaskResult.OPEN;
import static ws.palladian.retrieval.feeds.FeedTaskResult.SUCCESS;
import static ws.palladian.retrieval.feeds.FeedTaskResult.UNPARSABLE;
import static ws.palladian.retrieval.feeds.FeedTaskResult.UNREACHABLE;

import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.cookie.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;
import ws.palladian.retrieval.helper.HttpHelper;

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
 */
class FeedTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = LoggerFactory.getLogger(FeedTask.class);

    /**
     * The maximum file size (1 MB) which is accepted for each feed being checked. If this size is exceeded, the
     * download is stopped.
     */
    public static final long MAXIMUM_FEED_SIZE = SizeUnit.MEGABYTES.toBytes(1);

    /** Warn if processing of a feed takes longer than this. */
    public static final long EXECUTION_WARN_TIME = TimeUnit.MINUTES.toMillis(3);

    /** The feed retrieved by this task. */
    private final Feed feed;

    /** The store providing persistence for the feeds. */
    private final FeedStore store;

    /** Callback for the actions to perform when processing a feed (and various other cases). */
    private final FeedProcessingAction action;

    /** The strategy to determine a feed's update behaviour. */
    private final UpdateStrategy updateStrategy;

    /** A collection of all intermediate results that can happen, e.g. when updating meta information or a data base. */
    private final Set<FeedTaskResult> resultSet = new HashSet<FeedTaskResult>();

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     * @param action
     * @param updateStrategy
     */
    public FeedTask(Feed feed, FeedStore store, FeedProcessingAction action, UpdateStrategy updateStrategy) {
        this.feed = feed;
        this.store = store;
        this.action = action;
        this.updateStrategy = updateStrategy;
    }

    @Override
    public FeedTaskResult call() {
        StopWatch timer = new StopWatch();
        try {
            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl() + ")");
            int recentMisses = feed.getMisses();

            HttpResult httpResult = null;
            try {
                HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
                httpRetriever.setMaxFileSize(MAXIMUM_FEED_SIZE);
                // remember the time the feed has been checked
                feed.setLastPollTime(new Date());
                // download the document (not necessarily a feed)
                HttpRequest request = createRequest();
                httpResult = httpRetriever.execute(request);
            } catch (HttpException e) {
                LOGGER.error("Could not get Document for feed id " + feed.getId() + " , " + e.getMessage());
                feed.incrementUnreachableCount();
                resultSet.add(UNREACHABLE);

                doFinalStuff(timer);
                return getResult();
            }

            // process the returned header first
            // case 1: client or server error, statuscode >= 400
            if (httpResult.getStatusCode() >= HttpURLConnection.HTTP_BAD_REQUEST) {
                LOGGER.error("Could not get Document for feed id " + feed.getId()
                        + ". Server returned HTTP status code " + httpResult.getStatusCode());
                feed.incrementUnreachableCount();
                resultSet.add(UNREACHABLE);

                try {
                    action.onError(feed, httpResult);
                } catch (Exception e) {
                    resultSet.add(ERROR);
                }

            } else {

                // case 2: document has not been modified since last request
                if (httpResult.getStatusCode() == HttpURLConnection.HTTP_NOT_MODIFIED) {

                    updateCheckIntervals(feed);
                    feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                    try {
                        action.onUnmodified(feed, httpResult);
                    } catch (Exception e) {
                        resultSet.add(ERROR);
                    }

                    // case 3: default case, try to process the feed.
                } else {

                    // store http header information
                    feed.setLastETag(httpResult.getHeaderString("ETag"));
                    feed.setHttpLastModified(HttpHelper.getDateFromHeader(httpResult, "Last-Modified", false));

                    FeedParser feedParser = new RomeFeedParser();
                    Feed downloadedFeed = null;
                    try {
                        // parse the feed and get all its entries, do that here since that takes some time and this is a
                        // thread so it can be done in parallel
                        downloadedFeed = feedParser.getFeed(httpResult);
                    } catch (FeedParserException e) {
                        LOGGER.error("update items of feed id " + feed.getId() + " didn't work well, " + e.getMessage());
                        feed.incrementUnparsableCount();
                        resultSet.add(UNPARSABLE);
                        LOGGER.debug("Performing action on exception on feed: " + feed.getId() + "("
                                + feed.getFeedUrl() + ")");
                        try {
                            action.onException(feed, httpResult);
                        } catch (Exception e2) {
                            resultSet.add(ERROR);
                        }

                        doFinalStuff(timer);
                        return getResult();
                    }
                    Date httpDate = HttpHelper.getDateFromHeader(httpResult, "Date", true);
//                    feed.setHttpDateLastPoll(httpDate);
                    feed.setItems(downloadedFeed.getItems());
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

                    updateCheckIntervals(feed);

                    // perform actions on this feeds entries.
                    LOGGER.debug("Performing action on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
                    try {
                        action.onModified(feed, httpResult);
                    } catch (Exception e) {
                        resultSet.add(ERROR);
                    }
                }

                if (recentMisses < feed.getMisses()) {
                    resultSet.add(MISS);
                } else {
                    // finally, if no other status has been assigned, the task seems to bee successful
                    resultSet.add(SUCCESS);
                }
            }

            doFinalStuff(timer);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.error("Error processing feedID " + feed.getId() + ": " + th);
            resultSet.add(ERROR);
            doFinalLogging(timer);
            return getResult();
        }
    }

    /**
     * Sets the feed task result and processing time of this task, saves the feed to database, does the final logging
     * and frees the feed's memory.
     * 
     * @param timer The {@link StopWatch} to estimate processing time
     * @param storeMetadata Specify whether metadata should be updated in database.
     */
    private void doFinalStuff(StopWatch timer) {
        if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
            LOGGER.warn("Processing feed id " + feed.getId() + " took very long: " + timer.getElapsedTimeString());
            resultSet.add(EXECUTION_TIME_WARNING);
        }

        // Caution. The result written to db may be wrong since we can't write a db-error to db that occurs while
        // updating the database. This has no effect as long as we do not restart the FeedReader in this case.
        feed.setLastFeedTaskResult(getResult());
        feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
        updateFeed();

        doFinalLogging(timer);
        // since the feed is kept in memory we need to remove all items and the document stored in the feed
        feed.freeMemory();
    }

    /**
     * Update http request headers to use conditional requests (head requests). This is done only in case the last
     * FeedTaskResult was success, miss or execution time warning. In all other cases, no conditional header is build.
     */
    private HttpRequest createRequest() {
        HttpRequest request = new HttpRequest(HttpMethod.GET, feed.getFeedUrl());
        request.addHeader("cache-control", "no-cache");

        if (Arrays.asList(SUCCESS, MISS, EXECUTION_TIME_WARNING).contains(feed.getLastFeedTaskResult())) {
            if (feed.getLastETag() != null && !feed.getLastETag().isEmpty()) {
                request.addHeader("If-None-Match", feed.getLastETag());
            }
            if (feed.getHttpLastModified() != null) {
                request.addHeader("If-Modified-Since", DateUtils.formatDate(feed.getHttpLastModified()));
            }
        }
        return request;
    }

    /**
     * Decide the status of this FeedTask. This is done here to have a fixed ranking on the values.
     * 
     * @return The (current) result of the feed task.
     */
    private FeedTaskResult getResult() {
        if (resultSet.contains(ERROR)) {
            return ERROR;
        } else if (resultSet.contains(UNREACHABLE)) {
            return UNREACHABLE;
        } else if (resultSet.contains(UNPARSABLE)) {
            return UNPARSABLE;
        } else if (resultSet.contains(EXECUTION_TIME_WARNING)) {
            return EXECUTION_TIME_WARNING;
        } else if (resultSet.contains(MISS)) {
            return MISS;
        } else if (resultSet.contains(SUCCESS)) {
            return SUCCESS;
        } else {
            return OPEN;
        }
    }

    /**
     * Do final logging of result to error or debug log, depending on the FeedTaskResult.
     * 
     * @param timer the {@link StopWatch} started when started processing the feed.
     */
    private void doFinalLogging(StopWatch timer) {
        FeedTaskResult result = getResult();
        String msg = "Finished processing of feed id " + feed.getId() + ". Result: " + result + ". Processing took "
                + timer.getElapsedTimeString();
        if (result == FeedTaskResult.ERROR) {
            LOGGER.error(msg);
        } else if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(msg);
        }
    }

    /**
     * Save the feed back to the database. In case of database errors, add error to {@link #resultSet}.
     * 
     * @param storeMetadata
     */
    private void updateFeed() {
        boolean dbSuccess = store.updateFeed(feed, feed.hasNewItem());
        if (!dbSuccess) {
            resultSet.add(FeedTaskResult.ERROR);
        }
    }

    /**
     * Update the check interval depending on the chosen approach. Update the feed accordingly and return it.
     * 
     * @param feed The feed to update.
     */
    private void updateCheckIntervals(Feed feed) {
        updateStrategy.update(feed, new FeedPostStatistics(feed), false);
        feed.increaseChecks();
    }

}
