package ws.palladian.retrieval.feeds.evaluation.disssandro;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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
    private final FeedDatabase feedDatabase;

    /**
     * Remember the last simulated time the feed has been polled.
     */
    private final Timestamp lastPollTime;

    /**
     * Remember the hash of the newest {@link FeedItem} from the previous poll.
     */
    private final String lastNewestItemHash;

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
        this.feedReader = feedChecker;
        this.feedDatabase = (FeedDatabase) feedReader.getFeedStore();
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

            // bis hier: alle Items des aktuellen Fensters wurden dem downloadedFeed übergeben
            // http header information are discarded since they can't be confidently restored from dataset (they
            // may have changed between two polls when creating the dataset)

            // the simulated download of the feed.
            Feed downloadedFeed = getSimulatedWindowFromDataset();

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

            List<Long> itemDelays = new ArrayList<Long>();
            Long cumulatedDelay = 0L;
            for (FeedItem item : feed.getNewItems()) {
                // delay per new item in millisecond
                Long delay = feed.getLastPollTime().getTime() - item.getPublished().getTime();
                cumulatedDelay += delay;
                itemDelays.add(delay);
            }



            if (LOGGER.isDebugEnabled() && downloadedFeed.getItems().size() > 0) {
                StringBuilder itemTimestamps = new StringBuilder();
                int index = 1;
                itemTimestamps.append("Current window at ");
                itemTimestamps.append(feed.getLastPollTime());
                itemTimestamps.append("\n");
                for (FeedItem item : downloadedFeed.getItems()) {
                    itemTimestamps.append(index).append(": ");
                    itemTimestamps.append(item.getPublished());
                    itemTimestamps.append("\n");
                    index++;
                }
                LOGGER.debug(itemTimestamps.toString());
            }

            // if (LOGGER.isDebugEnabled()) {
            // LOGGER.debug("Activity Pattern: " + feed.getActivityPattern());
            // LOGGER.debug("Current time: " + System.currentTimeMillis());
            // LOGGER.debug("Last poll time: " + feed.getLastPollTime().getTime());
            // LOGGER.debug("Current time - last poll time: "
            // + (System.currentTimeMillis() - feed.getLastPollTime().getTime()));
            // LOGGER.debug("Milliseconds in a month: " + DateHelper.MONTH_MS);
            // }

            // TODO: in sumulation required??
            // storeMetadata = generateMetaInformation(httpResult, downloadedFeed);

            feedReader.updateCheckIntervals(feed);

            LOGGER.debug("New checkinterval: " + feed.getUpdateInterval());

            // if all entries are new, we might have checked to late and missed some entries, we mark that by a
            // special line
            // TODO copied notice from DatasetProcessingAction, might be obsolete
            // feed.getChecks()>1 may be replaced by newItems<feed.getNumberOfItemsReceived() to avoid writing a
            // MISS if a feed was empty and we now found one or more items. We have to define the MISS. If we say we
            // write a MISS every time it can happen that we missed a item, feed.getChecks()>1 is correct. If we say
            // there cant be a MISS before we see the first item, feed.getChecks()>1 has to be replaced. -- Sandro
            // 10.08.2011

            int numberNewItems = feed.getNewItems().size();
            if (numberNewItems == feed.getWindowSize() && feed.getChecks() > 1 && numberNewItems > 0) {

                // FIXME: count real number of misses here
                // get Entries between #lastNewestItemHash and oldest Item from current poll

                feed.increaseMisses();
                LOGGER.fatal("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                        + feed.getChecks() + ", misses: " + feed.getMisses());
            }

            // TODO: in sumulation required??
            // perform actions on this feeds entries.
            // LOGGER.debug("Performing action on feed: " + feed.getId() + "(" + feed.getFeedUrl() + ")");
            // boolean actionSuccess = feedReader.getFeedProcessingAction().performAction(feed, httpResult);
            // if (!actionSuccess) {
            // resultSet.add(FeedTaskResult.ERROR);
            // }


            if (recentMisses < feed.getMisses()) {
                resultSet.add(FeedTaskResult.MISS);
            } else {
                // finally, if no other status has been assigned, the task seems to bee successful
                resultSet.add(FeedTaskResult.SUCCESS);
            }


            PollData pollData = new PollData();
            pollData.setCheckInterval(feed.getUpdateInterval());
            pollData.setCumulatedLateDelay(cumulatedDelay);
            pollData.setDownloadSize(downloadSize);
            pollData.setItemDelays(itemDelays);
            pollData.setNewWindowItems(numberNewItems);
            pollData.setPollTimestamp(simulatedCurrentPollTime);
            pollData.setWindowSize(feed.getWindowSize());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(pollData.toString());
            }
            
            
            
            feed.getPollDataSeries().add(pollData);
            doFinalStuff(timer, storeMetadata);
            return getResult();

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals. Errors are logged only and not written to database.
        } catch (Throwable th) {
            LOGGER.error("Error processing feedID " + feed.getId() + ": " + th);
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
        
        // get closed poll that is in future of the simulated time.
        PollMetaInformation futurePoll = feedDatabase.getNextFeedPoll(feed.getId(), feed.getLastPollTimeSQLTimestamp());

        // this is the size of the poll we did when creating the dataset. Since we did not stored the sizes of all
        // single items, we do not know the size of the current simulated poll but use the real poll as an approximation
        downloadSize = futurePoll.getResponseSize();

        // assume that the windowSize has not changed between current and future poll
        int windowSize = futurePoll.getWindowSize();

        // TODO use feedID 1297 for debugging since feed does not provide item timestamps
        // load the last window from dataset
        List<FeedItem> simulatedWindow = feedDatabase.getEvaluationItemsByIDPollTimeLimit(feed.getId(),
                feed.getLastPollTimeSQLTimestamp(),
                windowSize);
        simulatedFeed.setItems(simulatedWindow);
        
        return simulatedFeed;
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