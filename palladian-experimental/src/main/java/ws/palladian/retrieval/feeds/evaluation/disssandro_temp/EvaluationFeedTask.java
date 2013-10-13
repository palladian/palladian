package ws.palladian.retrieval.feeds.evaluation.disssandro_temp;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.FeedTaskResult;
import ws.palladian.retrieval.feeds.evaluation.EvaluationFeedDatabase;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.evaluation.icwsm2011.PollData;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.AbstractUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

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
/**
 * @author Sandro Reichert
 * 
 */
public class EvaluationFeedTask implements Callable<FeedTaskResult> {

    /** The logger for this class. */
    private final static Logger LOGGER = LoggerFactory.getLogger(EvaluationFeedTask.class);

    /**
     * The feed processed by this task.
     */
    private Feed feed = null;

    /**
     * Direct access to the {@link FeedDatabase} is required to not extend the {@link FeedStore} interface by
     * (temporary)
     * evaluation methods.
     */
    private final EvaluationFeedDatabase feedDatabase;

    /**
     * the timestamp of the real poll done when creating the dataset. this timestamp is the closesd smaller or equal to
     * current simulation time.
     */
    private Timestamp currentRealPollTime;

    /**
     * Remember the last real poll time we go.
     */
    private Timestamp lastRealPollTime;

    /**
     * The time of the currenty simulated poll.
     */
    private long simulatedCurrentPollTime = 0;

    /**
     * Remember the hash of the newest {@link FeedItem} from the previous poll.
     */
    //private String lastNewestItemHash;

    /**
     * Remember the publish time of the newest item from the previous poll.
     */
    //private Timestamp lastNewestItemPublishTime;

    /**
     * Identifier to load the lastNumberOfPoll from the feed.
     */
    private static final String LAST_NUMBER_OF_POLL = "lastNumberOfPoll";

    /**
     * The number of the previous poll. All polls have an incremental counter starting at 1 for the first poll.
     */
    private int lastNumberOfPoll;

    /**
     * All polls that have at least one new item ha a sequential number starting from 1 at the first poll that contains
     * items.
     */
    private Integer lastNumberOfPollWithNewItem = 0;


    /**
     * The total number of items received till the last poll.
     */
    private int lastTotalItems;

    /**
     * Identifier to load the highest item sequence number of the previous poll from the feed.
     */
    private static final String LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER = "lastPollHighestItemSequenceNumber";

    /**
     * The highest item sequence number of the previous poll. All items have an sequence number (in chronological
     * order).
     */
    private int lastPollHighestItemSequenceNumber;

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

    private final UpdateStrategy updateStrategy;

    /**
     * Warn if processing of a feed takes longer than this.
     * TODO: align warning time to the feed's number of items in the dataset.
     */
    public static final long EXECUTION_WARN_TIME = TimeUnit.MINUTES.toMillis(10);

    /**
     * Creates a new retrieval task for a provided feed.
     * 
     * @param feed The feed retrieved by this task.
     */
    public EvaluationFeedTask(Feed feed, UpdateStrategy updateStrategy, EvaluationFeedDatabase feedDatabase) {
        // setName("FeedTask:" + feed.getFeedUrl());
        this.feed = feed;

        feed.setNumberOfItemsReceived(0);
        simulatedCurrentPollTime = FeedReaderEvaluator.BENCHMARK_TRAINING_START_TIME_MILLISECOND;
        feed.setUpdateInterval(0);

        backupFeed();

        this.updateStrategy = updateStrategy;
        this.feedDatabase = feedDatabase;
    }

    /**
     * Sets the current simulated poll time from timestamp of last iteration and the feed's current update interval
     * 
     */
    private void setSimulatedPollTime(Feed feed) {
        simulatedCurrentPollTime = feed.getLastPollTime().getTime() + feed.getUpdateInterval()
                * TimeUnit.MINUTES.toMillis(1);
    }

    /**
     * Backup some parameter values from the previous poll to be used in the next poll.
     */
    private void backupFeed() {
        //this.lastNewestItemHash = feed.getNewestItemHash();
        Date lastFeedEntry = feed.getLastFeedEntry();
        if (lastFeedEntry != null) {
            //this.lastNewestItemPublishTime = new Timestamp(lastFeedEntry.getTime());
        } else {
            //this.lastNewestItemPublishTime = null;
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
    }

    /** A collection of all intermediate results that can happen, e.g. when updating meta information or a data base. */
    private Set<FeedTaskResult> resultSet = new HashSet<FeedTaskResult>();

    /**
     * An approximation of the size of the poll in bytes.
     */
    private double downloadSize;

    /**
     * Some update strategies require an explicit training phase. If set to <code>true</code>, {@link AbstractUpdateStrategy} is
     * in training mode, if <code>false</code>, in normal mode.
     */
    private boolean trainingMode = false;


    @Override
    public FeedTaskResult call() {
        StopWatch timer = new StopWatch();
        try {
            Feed trainingFeed = new Feed();

            // do training if required by update strategy.
            if (updateStrategy.hasExplicitTrainingMode()) {

                trainingMode = true;

                // in training mode, we need an extra feed object since items are cached in the feed itself to do
                // duplicate detection, but we _want_ to identify all items in the first poll after training as new
                // items!
                trainingFeed.setId(feed.getId());
                trainingFeed.setActivityPattern(feed.getActivityPattern());

                while (simulatedCurrentPollTime <= FeedReaderEvaluator.BENCHMARK_TRAINING_STOP_TIME_MILLISECOND) {
                    // set time of current poll to feed
                    trainingFeed.setLastPollTime(new Date(simulatedCurrentPollTime));

                    Feed downloadedFeed = getSimulatedWindowFromDataset(new Timestamp(simulatedCurrentPollTime));

                    // abort processing if we dont have data
                    if (downloadedFeed == null) {
                        LOGGER.info("Feed id "
                                + feed.getId()
                                + ": can't load the simulated window for this feed. The first successful poll done when "
                                + "creating the dataset was later than this simulated poll. Stop processing immediately.");
                        // Set last poll time after benchmark stop time to prevent feed from being scheduled again. (Do
                        // not use trainingFeed here...)
                        feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND + 1));
                        resultSet.add(FeedTaskResult.SUCCESS);
                        doFinalLogging(timer);
                        return getResult();

                    }

                    // remember item sequence numbers
                    determineItemSequenceNumbers(downloadedFeed);

                    trainingFeed.setItems(downloadedFeed.getItems());
                    trainingFeed.setLastSuccessfulCheckTime(trainingFeed.getLastPollTime());
                    trainingFeed.setWindowSize(downloadedFeed.getItems().size());

                    updateCheckIntervals(trainingFeed, trainingMode);

                    // estimate time of next poll
                    setSimulatedPollTime(trainingFeed);
                }

                // write trained model from trainingFeed back to the feed object used in 'real' evaluation
                // debug info: in case training strategies change other stuff than additional data, copy it here
                feed.setAdditionalData(trainingFeed.getAdditionalData());
            }

            // training has been finished. reset all parameters that influence 'real' evaluation
            trainingMode = false;
            simulatedCurrentPollTime = FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND;
            feed.setChecks(0);
            feed.setLastPollTime(null);
            feed.setLastButOnePollTime(null);
            feed.setLastFeedEntry(null);
            feed.setLastButOneFeedEntry(null);


            // start 'real' evaluation
            while (simulatedCurrentPollTime <= FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND) {

                // set time of current poll to feed
                feed.setLastPollTime(new Date(simulatedCurrentPollTime));

                LOGGER.debug("Start processing of feed id " + feed.getId() + " (" + feed.getFeedUrl()
                        + "). Current simulated time is " + feed.getLastPollTime());
                int recentMisses = feed.getMisses();
                //boolean storeMetadata = false;

                // the simulated download of the feed.
                Feed downloadedFeed = getSimulatedWindowFromDataset(new Timestamp(simulatedCurrentPollTime));

                if (downloadedFeed == null) {
                    LOGGER.info("Feed id " + feed.getId()
                            + ": can't load the simulated window for this feed. The first successful poll done when "
                            + "creating the dataset was later than this simulated poll. Stop processing immediately.");
                    // Set last poll time after benchmark stop time to prevent feed from beeing scheduled again.
                    feed.setLastPollTime(new Date(FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND + 1));
                    resultSet.add(FeedTaskResult.SUCCESS);
                    doFinalLogging(timer);
                    return getResult();

                }

                // remember item sequence numbers
                determineItemSequenceNumbers(downloadedFeed);

                feed.setItems(downloadedFeed.getItems());
                feed.setLastSuccessfulCheckTime(feed.getLastPollTime());
                feed.setWindowSize(downloadedFeed.getItems().size());

                int numberNewItems = feed.getNewItems().size();

                // calculate number of current poll
                int currentNumberOfPoll = lastNumberOfPoll + 1;

                // set current numberOfPollWithNewItem
                Integer numberOfPollWithNewItem = null;
                if (numberNewItems > 0) {
                    numberOfPollWithNewItem = lastNumberOfPollWithNewItem + 1;
                }

                // Calculate cumulated delay. Ignore first poll that returned items. Important: use lastTotalItems, not
                // the current number of poll since some feeds like id 1270905 in TUDCS6 had an initial empty window,
                // that was likely to be caused by a server error.
                List<Long> itemDelays = new ArrayList<Long>();
                Long cumulatedDelay = null;
                if (lastTotalItems > 0 && numberNewItems > 0) {
                    cumulatedDelay = 0L;
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


                updateCheckIntervals(feed, trainingMode);

                LOGGER.debug("New checkinterval: " + feed.getUpdateInterval());

                Integer numPrePostBenchmarkItems = null;
                if (feed.getChecks() == 1) {
                    Date oldestKnownTimestamp = feed.getOldestFeedEntryCurrentWindow();
                    if (oldestKnownTimestamp == null) {
                        oldestKnownTimestamp = feed.getLastPollTime();
                        LOGGER.debug("FeedId " + feed.getId()
                                + " had no item at first poll, using alternative identification of dropped items.");
                    }
                    numPrePostBenchmarkItems = feedDatabase.getNumberOfPreBenchmarkItems(feed.getId(),
                            oldestKnownTimestamp);
                    // workaround variable window size: if we dropped some items and have misses in the first poll,
                    // the dropped items are excluded form calculating missed items.
                    lastPollHighestItemSequenceNumber = numPrePostBenchmarkItems;
                }

                // if all entries are new, we might have checked to late and missed some entries
                // TODO copied notice from DatasetProcessingAction, might be obsolete:
                // feed.getChecks()>1 may be replaced by newItems<feed.getNumberOfItemsReceived() to avoid writing a
                // MISS if a feed was empty and we now found one or more items. We have to define the MISS. If we say we
                // write a MISS every time it can happen that we missed a item, feed.getChecks()>1 is correct. If we say
                // there cant be a MISS before we see the first item, feed.getChecks()>1 has to be replaced. -- Sandro
                // 10.08.2011

                int currentMisses = 0;

                if (numberNewItems == feed.getWindowSize() && feed.getChecks() > 1 && numberNewItems > 0) {

                    currentMisses = feedDatabase.getNumberOfMissedItems(feed.getId(),
                            lastPollHighestItemSequenceNumber, currentPollLowestItemSequenceNumber);

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
                 * pending if its publishTime is newer than the last simulated poll that is within the benchmark
                 * interval.
                 */
                long nextSimulatedPollTime = feed.getLastPollTime().getTime() + feed.getUpdateInterval()
                        * TimeUnit.MINUTES.toMillis(1);
                Boolean noMorePolls = nextSimulatedPollTime > FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND;
                Integer pendingItems = null;
                if (noMorePolls) {
                    pendingItems = feedDatabase.getNumberOfPendingItems(feed.getId(),
                            feed.getLastPollTime(), feed.getLastFeedEntry());
                    numPrePostBenchmarkItems = feedDatabase.getNumberOfPostBenchmarkItems(feed.getId());
                }

                // summarize the simulated poll and store in database.
                PollData pollData = new PollData();
                pollData.setNumberOfPoll(currentNumberOfPoll);
                pollData.setNumberOfPollWithNewItem(numberOfPollWithNewItem);
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
                        DatasetEvaluator.getSimulatedPollsDbTableName(), true);

                if (!itemDelays.isEmpty()) {
                    feedDatabase.addSingleDelaysByFeed(feed.getId(), itemDelays,
                            DatasetEvaluator.getSimulatedPollsDbTableName(), true);
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(pollData.toString());
                }

                // store number of current poll and highest sequence number in feed
                Map<String, Object> additionalData = feed.getAdditionalData();
                additionalData.put(LAST_NUMBER_OF_POLL, currentNumberOfPoll);

                // if window size was 0, remember the highest sequence number seen so far.
                if (feed.getWindowSize() > 0) {
                    additionalData.put(LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER, currentPollHighestItemSequenceNumber);
                } else {
                    additionalData.put(LAST_POLL_HIGHEST_ITEM_SEQUENCE_NUMBER, lastPollHighestItemSequenceNumber);
                }
                feed.setAdditionalData(additionalData);

                // estimate time of next poll
                setSimulatedPollTime(feed);

                // store stuff for next iteration
                if (numberOfPollWithNewItem != null) {
                    lastNumberOfPollWithNewItem = numberOfPollWithNewItem;
                }
                lastRealPollTime = currentRealPollTime;
                backupFeed();


            }
            doFinalStuff(timer, false);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.error("Error processing feedID " + feed.getId() + ": " + th.getLocalizedMessage());
            resultSet.add(FeedTaskResult.ERROR);
            doFinalLogging(timer);
            return getResult();
        }
    }

    private void updateCheckIntervals(Feed feed, boolean trainingMode) {
        FeedPostStatistics fps = new FeedPostStatistics(feed);
        updateStrategy.update(feed, fps, trainingMode);
        feed.increaseChecks();
    }

    /**
     * Load the simulated window from database that is likely to be available at this point in time.
     * 
     * @param simulatedCurrentPollTimestamp The current simulated time.
     * @return A {@link Feed} that contains the simulated window
     */
    private Feed getSimulatedWindowFromDataset(Timestamp simulatedCurrentPollTimestamp) {
        Feed simulatedFeed = new Feed();

        simulatedFeed.setId(feed.getId());

        // get closed real poll that is in past of the simulated time.
        PollMetaInformation realPoll = new PollMetaInformation();
        if (lastNumberOfPoll == 0) {
            realPoll = feedDatabase.getEqualOrPreviousFeedPoll(feed.getId(), simulatedCurrentPollTimestamp);
        } else {
            realPoll = feedDatabase.getEqualOrPreviousFeedPollByTimeRange(feed.getId(), simulatedCurrentPollTimestamp,
                    lastRealPollTime);

        }


        // In few cases, we don't have any PollMetaInformation. This happens for feeds that were unparsable at all
        // polls.
        if (realPoll == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Feed id " + feed.getId() + " Unable to load PollMetaInformation at simulated timestamp "
                        + simulatedCurrentPollTimestamp.toString() + " lastPollTime was " + lastRealPollTime);
            }
            return null;
        }

        currentRealPollTime = realPoll.getPollSQLTimestamp();

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
        List<EvaluationFeedItem> simulatedWindow = new ArrayList<EvaluationFeedItem>();

        // if feed has a variableWindowSize or we haven't received an item so far, do expensive search
        if (feed.hasVariableWindowSize() == null || feed.hasVariableWindowSize()
                || feed.getOldestFeedEntryCurrentWindow() == null) {
            simulatedWindow = feedDatabase.getEvaluationItemsByIDCorrectedPublishTimeLimit(feed.getId(),
                    simulatedCurrentPollTimestamp, windowSize);
        }
        // the else statement is much faster for large feeds since we can make better use database indices
        else {
            simulatedWindow = feedDatabase.getEvaluationItemsByIDCorrectedPublishTimeRangeLimit(feed.getId(),
                    simulatedCurrentPollTimestamp, feed.getOldestFeedEntryCurrentWindow(), windowSize);
        }
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
        // reset sequence number
        currentPollLowestItemSequenceNumber = 0;

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
