package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedProcessingAction;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.FeedTaskResult;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.evaluation.PollData;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>
 * {@link EvaluationFeedTask}s are used to evaluate update strategies on a given dataset such as TUDCS6. The
 * {@link SchedulerTask} schedules {@link EvaluationFeedTask}s for each {@link Feed}. The {@link EvaluationFeedTask}
 * will run every time the feed is checked and also performs all set {@link FeedProcessingAction}s.
 * </p>
 * This class is based on the {@link FeedTask}.
 * Http header information are discarded since they can't be confidently restored from dataset (they may have changed
 * between two polls when creating the dataset).
 * 
 * @author Sandro Reichert
 * @see FeedReader
 * 
 */
public class EvaluationFeedTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = Logger.getLogger(EvaluationFeedTask.class);

    /**
     * The feed processed by this task.
     */
    private Feed feed = null;

    /**
     * The feed checker calling this task. // FIXME This is a workaround. Can be fixed by externalizing update
     * strategies to a true strategy pattern.
     */
    private final FeedReader feedReader;

    /**
     * Direct access to the {@link FeedDatabase} is required to not extend the {@link FeedStore} interface by (temporary)
     * evaluation methods.
     */
    private final EvaluationFeedDatabase feedDatabase;

    /**
     * Remember the last simulated time the feed has been polled.
     */
    private final Timestamp lastPollTime;

    /**
     * Remember the hash of the newest {@link FeedItem} from the previous poll.
     */
    private final String lastNewestItemHash;

    /**
     * Remember the publish time of the newest item from the previous poll.
     */
    private final Timestamp lastNewestItemPublishTime;

    /**
     * Identifier to load the lastNumberOfPoll from the feed.
     */
    private static final String LAST_NUMBER_OF_POLL = "lastNumberOfPoll";

    /**
     * The number of the previous poll. All polls have an incremental counter starting at 1 for the first poll.
     */
    private final int lastNumberOfPoll;

    /**
     * The total number of items received till the last poll.
     */
    private final int lastTotalItems;

    /**
     * Identifier to load the highest item sequence number of the previous poll from the feed.
     */
    private static final String LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER = "lastPollHighestItemSequenceNumber";

    /**
     * The highest item sequence number of the previous poll. All items have an sequence number (in chronological
     * order).
     */
    private final int lastPollHighestItemSequenceNumber;

    /**
     * The highest item sequence number of the current window. All items have an sequence number (in chronological
     * order).
     */
    private int currentPollHighestItemSequenceNumber = 0;

    /**
     * The lowest item sequence number of the current window. All items have an sequence number (in chronological
     * order).
     */
    private int currentPollLowestItemSequenceNumber = 0;

    /**
     * Warn if processing of a feed takes longer than this.
     */
    public static final long EXECUTION_WARN_TIME = 3 * DateHelper.MINUTE_MS;

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public EvaluationFeedTask(Feed feed, FeedReader feedChecker) {
        // setName("FeedTask:" + feed.getFeedUrl());
        this.feed = feed;
        this.lastPollTime = feed.getLastPollTimeSQLTimestamp();
        this.lastNewestItemHash = feed.getNewestItemHash();
                Date lastFeedEntry = feed.getLastFeedEntry();
        if (lastFeedEntry != null) {
            this.lastNewestItemPublishTime = new Timestamp(lastFeedEntry.getTime());
        } else {
            this.lastNewestItemPublishTime = null;
        }
        this.lastTotalItems = feed.getNumberOfItemsReceived();


        // load arbitrary, additional data from feed
        Map<String, Object> additionalData = feed.getAdditionalData();
        Object lastNumberOfPoll = null;
        Object lastPollHighestItemSequenceNumber = null;
        if (additionalData != null) {
            lastNumberOfPoll = additionalData.get(LAST_NUMBER_OF_POLL);
            lastPollHighestItemSequenceNumber = additionalData.get(LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER);
        }
        if (lastNumberOfPoll != null) {
            this.lastNumberOfPoll = (Integer) lastNumberOfPoll;
        } else {
            this.lastNumberOfPoll = 0;
        }
        if (lastPollHighestItemSequenceNumber != null) {
            this.lastPollHighestItemSequenceNumber = (Integer) lastPollHighestItemSequenceNumber;
        } else {
            this.lastPollHighestItemSequenceNumber = 0;
        }

        this.feedReader = feedChecker;
        this.feedDatabase = (EvaluationFeedDatabase) feedReader.getFeedStore();
    }

    /** A collection of all intermediate results that can happen, e.g. when updating meta information or a data base. */
    private Set<FeedTaskResult> resultSet = new HashSet<FeedTaskResult>();

    /**
     * An approximation of the size of the poll in bytes.
     */
    private double downloadSize;

    @Override
    public FeedTaskResult call() {
        StopWatch timer = new StopWatch();
        try {
            // calculate "current", i.e. simulated time.
            long simulatedCurrentPollTime = feed.getLastPollTime().getTime() + feed.getUpdateInterval()
                    * DateHelper.MINUTE_MS;
            feed.setLastPollTime(new Date(simulatedCurrentPollTime));

            LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl()
                    + "). Current simulated time is " + feed.getLastPollTime());
            int recentMisses = feed.getMisses();
            boolean storeMetadata = false;

            // the simulated download of the feed.
            Feed downloadedFeed = getSimulatedWindowFromDataset();
            
            if (downloadedFeed == null) {
                LOGGER.info("Feed id " + feed.getId()
                        + ": we don't have any poll data about this feed. Stop processing immediately.");
                // Set last poll time after benchmark stop time to prevent feed from beeing scheduled again.
                feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND + 1));
                resultSet.add(FeedTaskResult.SUCCESS);
                doFinalLogging(timer);
                return getResult();

            }

            // remember item sequence numbers
            determineItemSequenceNumbers(downloadedFeed);

            // TODO: do we really need HttpDate? This information is not provided by some feeds and we need
            // simulate/assume that all servers provide this date and that all have synchronized clocks.
            // feed.setHttpDateLastPoll(downloadedFeed.getLastPollTime());

            // TODO: do we really need the httpDate?
            // for (FeedItem item : downloadedFeed.getItems()) {
            // item.setHttpDate(feed.getHttpDateLastPoll());
            // }

            feed.setItems(downloadedFeed.getItems());
            feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
            feed.setWindowSize(downloadedFeed.getItems().size());

            // calculate number of current poll
            int currentNumberOfPoll = lastNumberOfPoll + 1;

            // Calculate cumulated delay. Ignore first poll that returned items. Important: use lastTotalItems, not
            // the current number of poll since some feeds like id 1270905 in TUDCS6 had an initial empty window, 
            // that was likely to be caused by a server error. 
            List<Long> itemDelays = new ArrayList<Long>();
            Long cumulatedDelay = 0L;            
            if (lastTotalItems > 0) {
                for (FeedItem item : feed.getNewItems()) {
                    // delay per new item in seconds
                    Long delay = Math.round((double) (feed.getLastPollTime().getTime() - item
                            .getCorrectedPublishedDate().getTime()) / 1000);
                    cumulatedDelay += delay;
                    itemDelays.add(delay);
                }
            }



            if (LOGGER.isDebugEnabled() && downloadedFeed.getItems().size() > 0) {
                StringBuilder itemTimestamps = new StringBuilder();
                int index = 1;
                itemTimestamps.append("Current window at ");
                itemTimestamps.append(feed.getLastPollTime());
                itemTimestamps.append("\n");
                for (FeedItem item : downloadedFeed.getItems()) {
                    itemTimestamps.append(index).append(": ");
                    itemTimestamps.append(((EvaluationFeedItem) item).getSequenceNumber()).append(": ");
                    itemTimestamps.append(item.getCorrectedPublishedDate());
                    itemTimestamps.append("\n");
                    index++;
                }
                LOGGER.debug(itemTimestamps.toString());
            }

            // TODO: in sumulation required??
            // storeMetadata = generateMetaInformation(httpResult, downloadedFeed);

            feedReader.updateCheckIntervals(feed);

            LOGGER.debug("New checkinterval: " + feed.getUpdateInterval());

            Integer numPrePostBenchmarkItems = null;
            if (feed.getChecks() == 1){
                numPrePostBenchmarkItems = feedDatabase.getNumberOfPreBenchmarkItems(feed.getId(),
                        feed.getLastPollTimeSQLTimestamp(), feed.getOldestFeedEntryCurrentWindowSqlTimestamp());
            }
            
            
            // if all entries are new, we might have checked to late and missed some entries
            // TODO copied notice from DatasetProcessingAction, might be obsolete:
            // feed.getChecks()>1 may be replaced by newItems<feed.getNumberOfItemsReceived() to avoid writing a
            // MISS if a feed was empty and we now found one or more items. We have to define the MISS. If we say we
            // write a MISS every time it can happen that we missed a item, feed.getChecks()>1 is correct. If we say
            // there cant be a MISS before we see the first item, feed.getChecks()>1 has to be replaced. -- Sandro
            // 10.08.2011

            int numberNewItems = feed.getNewItems().size();
            int currentMisses = 0;

            if (numberNewItems == feed.getWindowSize() && feed.getChecks() > 1 && numberNewItems > 0) {

                currentMisses = feedDatabase.getNumberOfMissedItems(feed.getId(), lastPollHighestItemSequenceNumber,
                        currentPollLowestItemSequenceNumber);

                feed.increaseMisses();
                LOGGER.debug("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                        + feed.getChecks() + ", new misses: " + currentMisses);

                // if (LOGGER.isDebugEnabled()) {
                // StringBuilder itemTimestamps = new StringBuilder();
                // int index = 1;
                // itemTimestamps.append("Missed items: \n");
                // for (FeedItem item : missedItems) {
                // itemTimestamps.append(index).append(": ");
                // itemTimestamps.append(item.getCorrectedPublishedDate());
                // itemTimestamps.append("\n");
                // index++;
                // }
                // LOGGER.debug(itemTimestamps.toString());
                // }

            }

            if (recentMisses < feed.getMisses()) {
                resultSet.add(FeedTaskResult.MISS);
            } else {
                // finally, if no other status has been assigned, the task seems to bee successful
                resultSet.add(FeedTaskResult.SUCCESS);
            }

            /**
             * Check whether we will poll the feed again. If not, determine the number of pending items. An item is
             * pending if its publishTime is newer than the last simulated poll that is within the benchmark interval.
             */
            long nextSimulatedPollTime = feed.getLastPollTimeSQLTimestamp().getTime() + feed.getUpdateInterval()
                    * DateHelper.MINUTE_MS;
            Boolean noMorePolls = nextSimulatedPollTime > FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND;
            Integer pendingItems = null;
            if (noMorePolls){
                // List<FeedItem> pendingItemsList = feedDatabase.getPendingItems(feed.getId(),
                // feed.getLastPollTimeSQLTimestamp(),
                // feed.getLastFeedEntrySQLTimestamp());
                pendingItems = feedDatabase.getNumberOfPendingItems(feed.getId(), feed.getLastPollTimeSQLTimestamp(),
                        feed.getLastFeedEntrySQLTimestamp());
                numPrePostBenchmarkItems = feedDatabase.getNumberOfPostBenchmarkItems(feed.getId());
            }
            
            
            // TODO pending items ermitteln: alle die neuer als aktuelles Fenster sind. nur ermitteln, wenn nächster
            // Poll nach Ende Simulationsdauer ist

            // summarize the simulated poll and store in database.
            PollData pollData = new PollData();
            pollData.setNumberOfPoll(currentNumberOfPoll);
            pollData.setDownloadSize(downloadSize);
            pollData.setPollTimestamp(simulatedCurrentPollTime);
            pollData.setCheckInterval(feed.getUpdateInterval());
            pollData.setNewWindowItems(numberNewItems);
            pollData.setMisses(currentMisses);
            pollData.setWindowSize(feed.getWindowSize());
            pollData.setCumulatedDelay(cumulatedDelay);
            pollData.setItemDelays(itemDelays);
            pollData.setPendingItems(pendingItems);
            pollData.setDroppedItems(numPrePostBenchmarkItems);

            feedDatabase.addPollData(pollData, feed.getId(), feed.getActivityPattern(),
                    DatasetEvaluator.simulatedPollsDbTableName(), true);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(pollData.toString());
            }

            // store number of current poll and highest sequence number in feed
            Map<String, Object> additionalData = new HashMap<String, Object>();
            additionalData.put(LAST_NUMBER_OF_POLL, currentNumberOfPoll);

            // if window size was 0, remember the highest sequence number seen so far.
            if (feed.getWindowSize() > 0) {
                additionalData.put(LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER, currentPollHighestItemSequenceNumber);
            } else {
                additionalData.put(LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER, lastPollHighestItemSequenceNumber);
            }
            feed.setAdditionalData(additionalData);
            
            doFinalStuff(timer, storeMetadata);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.fatal("Error processing feedID " + feed.getId() + ": " + th);
            resultSet.add(FeedTaskResult.ERROR);
            doFinalLogging(timer);
            return getResult();
        }
    }

    /**
     * Load the simulated window from database that is likely to be available at this point in time.
     * 
     * @return A {@link Feed} that contains the simulated window
     */
    private Feed getSimulatedWindowFromDataset() {
        Feed simulatedFeed = new Feed();

        simulatedFeed.setId(feed.getId());
        
        // get closed real poll that is in past of the simulated time.
        PollMetaInformation realPoll = feedDatabase.getEqualOrPreviousFeedPoll(feed.getId(), feed.getLastPollTimeSQLTimestamp());

        // In few cases, we don't have any PollMetaInformation. This happens for feeds that were unparsable at all
        // polls.
        if (realPoll == null) {
            return null;
        }

        // this is the size of the poll we did when creating the dataset. Since we did not stored the sizes of all
        // single items, we do not know the size of the current simulated poll but use the real poll as an approximation
        downloadSize = 0;
        if (realPoll.getResponseSize() != null) {
            downloadSize = realPoll.getResponseSize();
        }

        // assume that the windowSize has not changed between current and previous poll
        int windowSize = 0;
        if (realPoll.getWindowSize() != null) {
            windowSize = realPoll.getWindowSize();
        }

        // load the simulated window from dataset. We can use feed.getLastPollTimeSQLTimestamp() only because we set it
        // to the current simulated poll before.
        List<EvaluationFeedItem> simulatedWindow = feedDatabase.getEvaluationItemsByIDCorrectedPublishTimeLimit(
                feed.getId(),
                feed.getLastPollTimeSQLTimestamp(),
                windowSize);

        // a bit ugly: Feed takes lists of FeedItem only...
        List<FeedItem> castedList = new ArrayList<FeedItem>(simulatedWindow.size());
        castedList.addAll(simulatedWindow);

        simulatedFeed.setItems(castedList);
        
        return simulatedFeed;
    }

    /**
     * Determine {@link #currentPollHighestItemSequenceNumber} and {@link #currentPollLowestItemSequenceNumber} from
     * feed.
     */
    private void determineItemSequenceNumbers(Feed feed) {
        for (FeedItem item : feed.getItems()) {
            if (((EvaluationFeedItem) item).getSequenceNumber() > currentPollHighestItemSequenceNumber) {
                currentPollHighestItemSequenceNumber = ((EvaluationFeedItem) item).getSequenceNumber();
            }
            if (currentPollLowestItemSequenceNumber == 0
                    || ((EvaluationFeedItem) item).getSequenceNumber() < currentPollLowestItemSequenceNumber) {
                currentPollLowestItemSequenceNumber = ((EvaluationFeedItem) item).getSequenceNumber();
            }
        }

    }

    /**
     * Classify feed and process general meta data like feed title, language, size, format.
     * Everything in this method is done only if it has never been done before or once every month.
     * 
     * @param httpResult
     * @param downloadedFeed
     * @return
     */
    // private boolean generateMetaInformation(HttpResult httpResult, Feed downloadedFeed) {
    // boolean metadataCreated = false;
    //
    // if (feed.getActivityPattern() == FeedClassifier.CLASS_UNKNOWN || feed.getLastPollTime() != null
    // && (System.currentTimeMillis() - feed.getLastPollTime().getTime()) > DateHelper.MONTH_MS) {
    //
    // metadataCreated = true;
    // feed.setActivityPattern(FeedClassifier.classify(feed));
    // MetaInformationExtractor metaInfExt = new MetaInformationExtractor(httpResult);
    // metaInfExt.updateFeedMetaInformation(feed.getMetaInformation());
    // feed.getMetaInformation().setTitle(downloadedFeed.getMetaInformation().getTitle());
    // feed.getMetaInformation().setByteSize(downloadedFeed.getMetaInformation().getByteSize());
    // feed.getMetaInformation().setLanguage(downloadedFeed.getMetaInformation().getLanguage());
    // }
    // return metadataCreated;
    // }

    /**
     * Sets the feed task result and processing time of this task, saves the feed to database, does the final logging
     * and frees the feed's memory.
     * 
     * @param timer The {@link StopWatch} to estimate processing time
     * @param storeMetadata Specify whether metadata should be updated in database.
     */
    private void doFinalStuff(StopWatch timer, boolean storeMetadata) {
        if (timer.getElapsedTime() > EXECUTION_WARN_TIME) {
            LOGGER.warn("Processing feed id " + feed.getId() + " took very long: " + timer.getElapsedTimeString());
            resultSet.add(FeedTaskResult.EXECUTION_TIME_WARNING);
        }

        // Caution. The result written to db may be wrong since we can't write a db-error to db that occurs while
        // updating the database. This has no effect as long as we do not restart the FeedReader in this case.
        feed.setLastFeedTaskResult(getResult());
        feed.increaseTotalProcessingTimeMS(timer.getElapsedTime());
        // updateFeed(storeMetadata);

        doFinalLogging(timer);
        // since the feed is kept in memory we need to remove all items and the document stored in the feed
        feed.freeMemory();
    }

    /**
     * Decide the status of this FeedTask. This is done here to have a fixed ranking on the values.
     * 
     * @return The (current) result of the feed task.
     */
    private FeedTaskResult getResult() {
        FeedTaskResult result = null;
        if (resultSet.contains(FeedTaskResult.ERROR)) {
            result = FeedTaskResult.ERROR;
        } else if (resultSet.contains(FeedTaskResult.UNREACHABLE)) {
            result = FeedTaskResult.UNREACHABLE;
        } else if (resultSet.contains(FeedTaskResult.UNPARSABLE)) {
            result = FeedTaskResult.UNPARSABLE;
        } else if (resultSet.contains(FeedTaskResult.EXECUTION_TIME_WARNING)) {
            result = FeedTaskResult.EXECUTION_TIME_WARNING;
        } else if (resultSet.contains(FeedTaskResult.MISS)) {
            result = FeedTaskResult.MISS;
        } else if (resultSet.contains(FeedTaskResult.SUCCESS)) {
            result = FeedTaskResult.SUCCESS;
        } else {
            result = FeedTaskResult.OPEN;
        }

        return result;
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
        // LOGGER.info(msg);
    }

    /**
     * Save the feed back to the database. In case of database errors, add error to {@link #resultSet}.
     * 
     * @param storeMetadata
     */
    // private void updateFeed(boolean storeMetadata) {
    // boolean dbSuccess = feedReader.updateFeed(feed, storeMetadata, feed.hasNewItem());
    // if (!dbSuccess) {
    // resultSet.add(FeedTaskResult.ERROR);
    // }
    // }

}
