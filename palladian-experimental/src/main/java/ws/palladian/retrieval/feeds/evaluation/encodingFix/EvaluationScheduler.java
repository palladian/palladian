//package ws.palladian.retrieval.feeds.evaluation.encodingFix;
//
//import java.util.Map;
//import java.util.TreeMap;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.Future;
//
//import org.apache.log4j.Logger;
//
//import ws.palladian.helper.ConfigHolder;
//import ws.palladian.persistence.DatabaseManagerFactory;
//import ws.palladian.retrieval.feeds.Feed;
//import ws.palladian.retrieval.feeds.FeedReader;
//import ws.palladian.retrieval.feeds.evaluation.gzPorcessing.ClassifyFromCsv;
//import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
//import ws.palladian.retrieval.feeds.persistence.FeedStore;
//
///**
// * <p>A scheduler task handles the distribution of feeds to a) worker threads that check the csv files for the ASCII encoding problem in TUDCS2 dataset or b) worker threads that remove superfluous feed size and item size columns with "-1" values. This class is based on ws.palladian.retrieval.feeds.SchedulerTask</p>
//
// * @author Klemens Muthmann
// * @author Sandro Reichert
// * 
// */
//class EvaluationScheduler {
//
//    /**
//     * The logger for objects of this class. Configure it using <tt>src/main/resources/log4j.properties</tt>.
//     */
//    private static final Logger LOGGER = Logger.getLogger(EvaluationScheduler.class);
//
//    /**
//     * The collection of all the feeds this scheduler should create update
//     * threads for.
//     */
//    private transient final FeedReader feedReader;
//
//    /**
//     * The thread pool managing threads that read feeds from the feed sources
//     * provided by {@link #collectionOfFeeds}.
//     */
//    private transient final ExecutorService threadPool;
//
//    /**
//     * Tasks currently scheduled but not yet checked.
//     */
//    private transient final Map<Integer, Future<?>> scheduledTasks;
//
//    /**
//     * Creates a new {@code EvaluationScheduler}.
//     * 
//     * @param feedReader
//     *            The feed reader containing settings and providing the
//     *            collection of feeds to check.
//     */
//    public EvaluationScheduler(final FeedReader feedReader) {
//        threadPool = Executors.newFixedThreadPool(feedReader.getThreadPoolSize());
//        this.feedReader = feedReader;
//        scheduledTasks = new TreeMap<Integer, Future<?>>();
//    }
//
//    public void run() {
//        LOGGER.info("Scheduling all feeds to be checked for the charset duplicates and duplicates within the window");
//
//        for (Feed feed : feedReader.getFeeds()) {
//
//            // FIXME remove filter
//            // if (feed.getActivityPattern() == 9) {
//                scheduledTasks.put(feed.getId(), threadPool.submit(new ClassifyFromCsv(feed)));
//            // }
//        }
//
//        while (!scheduledTasks.isEmpty()) {
//
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                LOGGER.error(e.getMessage());
//            }
//
//            for (Feed feed : feedReader.getFeeds()) {
//                removeFeedTaskIfDone(feed.getId());
//            }
//            LOGGER.info("Number of remaining tasks to be done: " + scheduledTasks.size());
//        }
//        LOGGER.info("All tasks done. Bye.");
//        System.exit(0);
//
//    }
//
//    /**
//     * Removes the feed's {@link FeedTask} from the queue if it is contained and already done.
//     * 
//     * @param feedId The feed to check and remove if the {@link FeedTask} is done.
//     */
//    private void removeFeedTaskIfDone(final Integer feedId) {
//        final Future<?> future = scheduledTasks.get(feedId);
//        if (future != null && future.isDone()) {
//            scheduledTasks.remove(feedId);
//            LOGGER.trace("Removed completed feed from feed task pool: " + feedId);
//        }
//    }
//
//    public static void main(String[] args) {
//
//        FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
//        FeedReader feedChecker = new FeedReader(feedStore);
//        EvaluationScheduler scheduler = new EvaluationScheduler(feedChecker);
//        scheduler.run();
//
//    }
//
//}
