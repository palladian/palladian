package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.feedSizeCalculator;

import java.util.Map;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.collections15.bag.HashBag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.FeedTaskResult;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * Scheduler to process all feeds in db once in a {@link FeedSizeCalculationTask}. Use to restore feedSizes in database
 * table feed_polls. The feedSize has not been calculated in realtime when creating the TUDCS6 dataset for performance
 * reasons.
 * 
 * @author Sandro Reichert
 */
public class FeedSizeCalculationScheduler extends TimerTask {
    /**
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.xml</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedSizeCalculationScheduler.class);

    /**
     * The thread pool managing threads that read feeds from the feed sources
     * provided by {@link #collectionOfFeeds}.
     */
    private transient final ExecutorService threadPool;

    /**
     * Tasks currently scheduled but not yet checked.
     */
    private transient final Map<Integer, Future<FeedTaskResult>> scheduledTasks;

    /**
     * The collection of all the feeds this scheduler should create update
     * threads for.
     */
    private transient final FeedReader feedReader;

    private boolean firstRun = true;

    private final HashBag<FeedTaskResult> feedResults = new HashBag<FeedTaskResult>();

    /** Count the number of processed feeds per scheduler iteration. */
    private int processedCounter = 0;

    /**
     * Creates a new {@code SchedulerTask} for a feed reader.
     * 
     * @param feedReader
     *            The feed reader containing settings and providing the
     *            collection of feeds to check.
     */
    public FeedSizeCalculationScheduler(final FeedReader feedReader) {
        super();
        threadPool = Executors.newFixedThreadPool(feedReader.getThreadPoolSize());
        this.feedReader = feedReader;
        scheduledTasks = new TreeMap<Integer, Future<FeedTaskResult>>();
    }

    /*
     * (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        LOGGER.debug("wake up to check feeds");
        int newlyScheduledFeedsCount = 0;
        StringBuilder scheduledFeedIDs = new StringBuilder();

        // schedule all feeds only once
        for (Feed feed : feedReader.getFeeds()) {
            if (firstRun) {

                // FIXME: remove dbug filter
                // if (feed.getId() == 1074) {
                    scheduledTasks.put(feed.getId(),
                            threadPool.submit(new FeedSizeCalculationTask(feed, (FeedDatabase) feedReader.getFeedStore())));
                    newlyScheduledFeedsCount++;
                // }


            } else {
                removeFeedTaskIfDone(feed.getId());
            }
        }
        if (firstRun) {
            firstRun = false;
        }

        int success = feedResults.getCount(FeedTaskResult.SUCCESS);
        int misses = feedResults.getCount(FeedTaskResult.MISS);
        int unreachable = feedResults.getCount(FeedTaskResult.UNREACHABLE);
        int unparsable = feedResults.getCount(FeedTaskResult.UNPARSABLE);
        int slow = feedResults.getCount(FeedTaskResult.EXECUTION_TIME_WARNING);
        int errors = feedResults.getCount(FeedTaskResult.ERROR);

        String logMsg = String.format("Newly scheduled: %6d, queue size: %6d, processed: %4d, "
                + "success: %4d, misses: %4d, unreachable: %4d, unparsable: %4d, slow: %4d, errors: %4d, ",
                newlyScheduledFeedsCount, scheduledTasks.size(), processedCounter, success, misses, unreachable,
                unparsable, slow, errors);

        LOGGER.info(" " + logMsg); // whitespace required to align lines in log file.

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Scheduled feed tasks for feedIDs " + scheduledFeedIDs.toString());
        }

        // reset logging
        processedCounter = 0;
        feedResults.clear();
        if (scheduledTasks.isEmpty()) {
            LOGGER.info("All tasks done. Exiting.");
            System.exit(0);
        }
    }

    /**
     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
     * 
     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
     */
    private void removeFeedTaskIfDone(final Integer feedId) {
        final Future<FeedTaskResult> future = scheduledTasks.get(feedId);
        if (future != null && future.isDone()) {
            scheduledTasks.remove(feedId);
            processedCounter++;
            try {
                feedResults.add(future.get());
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (ExecutionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
