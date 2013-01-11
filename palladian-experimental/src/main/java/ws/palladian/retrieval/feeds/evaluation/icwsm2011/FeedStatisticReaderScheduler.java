package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>A scheduler task handles the distribution of feeds to worker threads that check the csv files for the ASCII encoding
 * problem in TUDCS2 dataset.</p>
 * <p>This class is based on ws.palladian.retrieval.feeds.SchedulerTask</p>
 * 
 * @author Sandro Reichert
 */
class FeedStatisticReaderScheduler {

    /**
     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.properties</tt>.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedStatisticReaderScheduler.class);

    private transient final FeedStore feedStore;

    /**
     * The thread pool managing threads that read feeds from the feed sources
     * provided by {@link #collectionOfFeeds}.
     */
    private transient final ExecutorService threadPool;

    private static final int THREAD_POOL_SIZE = 100;

    /**
     * Tasks currently scheduled but not yet checked.
     */
    private transient final Map<Integer, Future<?>> scheduledTasks;

    /**
     * Creates a new {@code EvaluationScheduler}.
     * 
     * @param feedReader
     *            The feed reader containing settings and providing the
     *            collection of feeds to check.
     */
    public FeedStatisticReaderScheduler(final FeedStore feedStore) {
        threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        this.feedStore = feedStore;
        scheduledTasks = new TreeMap<Integer, Future<?>>();
    }

    public void run() {
        LOGGER.info("Scheduling all feeds to be checked for the charset duplicates and duplicates within the window");

        for (Feed feed : feedStore.getFeeds()) {

            scheduledTasks.put(feed.getId(), threadPool.submit(new FeedStatisticReaderTask(feedStore, feed)));
        }

        while (!scheduledTasks.isEmpty()) {

            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage());
            }

            for (Feed feed : feedStore.getFeeds()) {

                removeFeedTaskIfDone(feed.getId());
            }
            LOGGER.info("Number of remaining tasks to be done: " + scheduledTasks.size());
        }
        LOGGER.info("All tasks done. Bye.");
        System.exit(0);

    }

    /**
     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
     * 
     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
     */
    private void removeFeedTaskIfDone(final Integer feedId) {
        final Future<?> future = scheduledTasks.get(feedId);
        if (future != null && future.isDone()) {
            scheduledTasks.remove(feedId);
            LOGGER.trace("Removed completed feed from feed task pool: " + feedId);
        }
    }

    public static void main(String[] args) {

        FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
        FeedStatisticReaderScheduler scheduler = new FeedStatisticReaderScheduler(feedStore);
        scheduler.run();

    }

}
