package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.persistence.CollectionFeedSource;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.FixLearnedUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.MAVSynchronizationUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.MavUpdateStrategy;
import ws.palladian.retrieval.feeds.updates.UpdateStrategy;

/**
 * <p>The FeedReader reads news from feeds in a database. It learns when it is necessary to check the feed again for news.</p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class FeedReader {

    /** The logger for this class. */
    static final Logger LOGGER = LoggerFactory.getLogger(FeedReader.class);

    /** Maximum number of feed reading threads at the same time. */
    public static final int DEFAULT_THREAD_POOL_SIZE = 200;

    private final int threadPoolSize;

    /** List of feeds that are read continuous. */
    private Collection<Feed> feedCollection;

    /** The action that should be performed for each feed that is read. */
    private FeedProcessingAction feedProcessingAction = null;

    /** Whether reading should continue or not. */
    private boolean stopped = false;

    /**
     * If a fixed checkInterval could not be learned, this one is taken (in minutes).
     */
    public static final int DEFAULT_CHECK_TIME = 60;

    /** The chosen check Approach */
    private UpdateStrategy updateStrategy = new MavUpdateStrategy();

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed
     * must be updated whenever the method {@link Feed#getLastPollTime()} return value is further away in the past then
     * its {@link Feed#getMaxCheckInterval()} or {@link Feed#getUpdateInterval()} returns. Which one to use depends on
     * the update strategy.
     */
    private final Timer checkScheduler;

    /** The feedstore. */
    private final FeedStore feedStore;

    /**
     * Defines the default time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds
     * should be read.
     */
    private static final long DEFAULT_WAKEUP_INTERVAL = TimeUnit.SECONDS.toMillis(60);

    /**
     * Defines the time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds should
     * be read.
     */
    private long wakeUpInterval = DEFAULT_WAKEUP_INTERVAL;

    /** The constructor. */
    public FeedReader(FeedStore feedStore) {
        checkScheduler = new Timer();
        this.feedStore = feedStore;
        feedCollection = feedStore.getFeeds();
        threadPoolSize = DEFAULT_THREAD_POOL_SIZE;
        feedProcessingAction = new DefaultFeedProcessingAction();
    }

    public FeedReader(FeedStore feedStore, int numThreads) {
        checkScheduler = new Timer();
        this.feedStore = feedStore;
        feedCollection = feedStore.getFeeds();
        threadPoolSize = numThreads;
        feedProcessingAction = new DefaultFeedProcessingAction();
    }

    /**
     * Filter feeds, to read only those that have an update class as in the given set.
     * 
     * @param updateClasses The update classes that should be read.
     */
    public void filterFeeds(Collection<Integer> updateClasses) {

        Collection<Feed> filteredFeedCollection = new ArrayList<Feed>();

        for (Feed feed : feedCollection) {
            if (updateClasses.contains(feed.getActivityPattern())) {
                filteredFeedCollection.add(feed);
            }
        }

        feedCollection = filteredFeedCollection;
    }

    // ================================
    // === Public interface methods ===
    // ================================

    /**
     * Continuously read feeds.
     * 
     * @param duration Time in milliseconds after it should stop reading, -1 means no time limit.
     */
    public void startContinuousReading(long duration) {

        LOGGER.info("start continuous reading of feeds for " + DateHelper.getTimeString(duration));
        StopWatch stopWatch = new StopWatch();
        stopWatch.setCountDown(duration);
        stopWatch.start();

        LOGGER.info("loaded " + feedCollection.size() + " feeds");

        // checkScheduler.schedule(schedulerTask, wakeUpInterval, wakeUpInterval);
//        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {

            SchedulerTask schedulerTask = new SchedulerTask(this);
            checkScheduler.schedule(schedulerTask, 0, wakeUpInterval);

//        } else {
//
//            EvaluationSchedulerTask schedulerTask = new EvaluationSchedulerTask(this);
//            checkScheduler.schedule(schedulerTask, 0, wakeUpInterval);
//
//        }

        LOGGER.debug("scheduled task, wake up every " + wakeUpInterval
                + " milliseconds to check all feeds whether they need to be read or not");

        while (!stopWatch.timeIsUp() && !stopped) {

                LOGGER.trace("time is not up, keep reading feeds");
            LOGGER.debug("current total traffic: " + HttpRetriever.getTraffic(SizeUnit.MEGABYTES) + " MB");

                try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    stopped = true;
                    break;
                }

        }
        LOGGER.info("stopped reading feeds after " + stopWatch.getElapsedTimeString());

        stopContinuousReading();

        LOGGER.info("cancelled all scheduled readings, total size downloaded (" + getUpdateStrategy() + "): "
                + HttpRetriever.getTraffic(SizeUnit.MEGABYTES) + " MB");
    }

    /** Start continuous reading without a time limit. */
    public void startContinuousReading() {
        startContinuousReading(-1);
    }

    /**
     * Stop all timers, no reading will be performed after stopping the reader.
     */
    public void stopContinuousReading() {
        stopped = true;
        LOGGER.info("stopped continuous reading");
        checkScheduler.cancel();
    }

    /**
     * Update the check interval depending on the chosen approach. Update the feed accordingly and return it.
     * 
     * @param feed The feed to update.
     * @param trainingMode If the {@link UpdateStrategy} distinguishes between training and normal mode, set to
     *            <code>true</code> to use training mode. For normal mode, or if you don't know, set
     *            to <code>false</code>.
     */
    public synchronized void updateCheckIntervals(Feed feed, boolean trainingMode) {

        FeedPostStatistics fps = new FeedPostStatistics(feed);

        updateStrategy.update(feed, fps, trainingMode);

        // don't do this here, fps might be invalid. The Feed does this himself
        // feed.setLastFeedEntry(new Date(fps.getTimeNewestPost()));
        feed.increaseChecks();
    }

//    /**
//     * Use the {@link FeedReader} for aggregating feed items.
//     */
//    public void aggregate() {
//        aggregate(-1);
//    }

//    /**
//     * Use the {@link FeedReader} for aggregating feed items for the specified duration.
//     * 
//     * @param duration time in milliseconds, -1 for no limit.
//     */
//    public void aggregate(long duration) {
//
//        final AtomicInteger newItems = new AtomicInteger();
//
//        FeedProcessingAction processingAction = new DefaultFeedProcessingAction() {
//
//            @Override
//            public boolean performAction(Feed feed, HttpResult httpResult) {
//                List<FeedItem> items = feed.getItems();
//                int addedItems = feedStore.addFeedItems(items);
//                newItems.addAndGet(addedItems);
//                return true;
//            }
//
//        };
//        setFeedProcessingAction(processingAction);
//        startContinuousReading(duration);
//
//        LOGGER.info("# of new entries : " + newItems);
//    }

    // XXX should be specified in constructor
    public void setFeedProcessingAction(FeedProcessingAction feedProcessingAction) {
        this.feedProcessingAction = feedProcessingAction;
    }

    // TODO add multiple feed actions

    /**
     * <p>
     * Set the approach for checking feeds for news. Once an approach is chosen it cannot be changed (meta information
     * is saved in the feed store) unless you reset the learned data.
     * </p>
     * 
     * @param updateStrategy The updating strategy for the feed reader.
     */
    // XXX should be specified in constructor
    public void setUpdateStrategy(UpdateStrategy updateStrategy) {
        this.updateStrategy = updateStrategy;
    }

    /**
     * Get the UpdateStrategy. If none has been set before, {@link FixLearnedUpdateStrategy} is used by default.
     * 
     * @return
     */
    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

//    /** Get the human readable name of the chosen check approach. */
//    public String getUpdateStrategyName() {
//        return getUpdateStrategy().getName();
//    }

    public FeedProcessingAction getFeedProcessingAction() {
        return feedProcessingAction;
    }

    public Collection<Feed> getFeeds() {
        return feedCollection;
    }

//    public void setStopped(boolean stopped) {
//        this.stopped = stopped;
//    }

//    public boolean isStopped() {
//        return stopped;
//    }

    public boolean updateFeed(Feed feed) {
        return feedStore.updateFeed(feed);
    }

    /**
     * Update feed in database.
     * 
     * @param feed The feed to update
     * @param updateMetaInformation If <code>true</code>, the feed's meta information are updated.
     * @param replaceCachedItems Of <code>true</code>, the cached items are replaced by the ones contained in the feed.
     * @return <code>true</code> if (all) update(s) successful.
     */
    public boolean updateFeed(Feed feed, boolean replaceCachedItems) {
        return feedStore.updateFeed(feed, replaceCachedItems);
    }
    
    /**
     * Re-read the feeds from the {@link FeedStore}. Feeds which are not yet considered by the {@link FeedReader} are
     * added, feeds which are no longer present in the {@link FeedStore} are removed. We need this synchronization logic
     * full of black magic, as the Feed instances cache all kind information which is not persisted to the store. So we
     * must not touch feeds which are currently cached by the {@link FeedReader}. Quick and dirty, and yet untested.
     * 
     * @return The delta of the added/removed feeds.
     */
    public int synchronizeWithStore() {

        // URLs of the feeds which are currently being read
        Set<String> currentFeedUrls = new HashSet<String>();
        for (Feed feed : feedCollection) {
            currentFeedUrls.add(feed.getFeedUrl());
        }

        // shallow copy of the current feed collection which will be modified;
        // we must not modify the existing collection, as this is used by the Threads
        List<Feed> newFeedCollection = new ArrayList<Feed>(this.feedCollection);

        // obtain a current list of feeds from the FeedStore
        List<Feed> storeFeedCollection = feedStore.getFeeds();
        
        // URLs of the feeds which are added
        Set<String> storeFeedUrls = new HashSet<String>();
        for (Feed feed : storeFeedCollection) {
            storeFeedUrls.add(feed.getFeedUrl());
        }

        // check, which feeds are currently not considered by the FeedReader and add them to the new collection
        int addedFeeds = 0;
        for (Feed feed : storeFeedCollection) {
            if (!currentFeedUrls.contains(feed.getFeedUrl())) {
                newFeedCollection.add(feed);
                addedFeeds++;
            }
        }

        // check, which feeds are no longer present in the FeedStore and remove them from the new collection
        int removedFeeds = 0;
        Iterator<Feed> iterator = newFeedCollection.iterator();
        while (iterator.hasNext()) {
            Feed feed = iterator.next();
            if (!storeFeedUrls.contains(feed.getFeedUrl())) {
                iterator.remove();
                removedFeeds++;
            }
        }

        // replace the existing collection
        this.feedCollection = newFeedCollection;

        LOGGER.info("added " + addedFeeds + " feeds to the FeedReader");
        LOGGER.info("removed " + removedFeeds + " feeds from the FeedReader");
        return addedFeeds - removedFeeds;

    }

//    public void setFeedStore(FeedStore feedStore) {
//        this.feedStore = feedStore;
//    }
//
    public FeedStore getFeedStore() {
        return feedStore;
    }

//    /**
//     * @param threadPoolSize the threadPoolSize to set
//     */
//    public void setThreadPoolSize(Integer threadPoolSize) {
//        this.threadPoolSize = threadPoolSize;
//    }

    /**
     * @return the threadPoolSize
     */
    public int getThreadPoolSize() {
        return threadPoolSize;
    }

    /**
     * @return The time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds
     *         should be read.
     */
    public final long getWakeUpInterval() {
        return wakeUpInterval;
    }

    /**
     * @param wakeUpInterval The time in milliseconds when the FeedReader should wake up the checkScheduler to see which
     *            feeds should be read.
     */
    public final void setWakeUpInterval(long wakeUpInterval) {
        this.wakeUpInterval = wakeUpInterval;
    }

    /**
     * Sample usage. Command line: parameters: checkType("cf" or "ca" or "cp") runtime(in minutes) checkInterval(only if
     * checkType=1),
     * 
     * @throws FeedParserException
     */
    public static void main(String[] args) throws FeedParserException {

        /**
         * Bug #14 sample code
         */
        FeedStore feedStore = new CollectionFeedSource();
        feedStore.addFeed(new Feed("http://lifehacker.com/excerpts.xml"));
        FeedReader feedReader = new FeedReader(feedStore);
        feedReader.setUpdateStrategy(new MAVSynchronizationUpdateStrategy());
        feedReader.startContinuousReading();
        System.exit(0);

//        FeedReader r = new FeedReader(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()), 1);
//        r.aggregate(1000 * 60 * 5);
//        System.exit(0);

        FeedReader fchecker = new FeedReader(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()));
        fchecker.setUpdateStrategy(new FixLearnedUpdateStrategy());
        fchecker.startContinuousReading();
        System.exit(0);

        FeedReader fch = new FeedReader(new CollectionFeedSource());
        fch.setUpdateStrategy(new FixLearnedUpdateStrategy());
        Feed feed = new Feed("http://de.answers.yahoo.com/rss/allq");
        feed.setActivityPattern(FeedActivityPattern.CLASS_SLICED);

        // FeedParser feedParser = new RomeFeedParser();
        // feedRetriever.updateFeed(feed);
        // feed.increaseChecks();
        fch.updateCheckIntervals(feed, false);
        System.exit(0);

//        Options options = new Options();
//
//        OptionGroup checkApproachOption = new OptionGroup();
//        checkApproachOption.addOption(OptionBuilder.withArgName("cf").withLongOpt("CHECK_FIXED")
//                .withDescription("check each feed at a fixed interval").create());
//        checkApproachOption.addOption(OptionBuilder.withArgName("ca").withLongOpt("CHECK_ADAPTIVE")
//                .withDescription("check each feed and learn its update times").create());
//        checkApproachOption.addOption(OptionBuilder.withArgName("cp").withLongOpt("CHECK_PROPABILISTIC")
//                .withDescription("check each feed and adapt to its update rate").create());
//        checkApproachOption.setRequired(true);
//        options.addOptionGroup(checkApproachOption);
//        options.addOption("r", "runtime", true,
//                "The runtime of the checker in minutes or -1 if it should run until aborted.");
//        options.addOption("ci", "checkInterval", true,
//                "Set a fixed check interval in minutes. This is only effective if the checkType is set to CHECK_FIXED.");
//        HelpFormatter formatter = new HelpFormatter();
//
//        CommandLineParser parser = new PosixParser();
//        CommandLine cmd = null;
//        try {
//            cmd = parser.parse(options, args);
//        } catch (ParseException e) {
//            LOGGER.debug("Command line arguments could not be parsed!");
//            formatter.printHelp("FeedReader", options);
//        }
//
//        int runtime = -1;
//        UpdateStrategy updateStrategy = new FixLearnedUpdateStrategy();
//        int checkInterval = -1;
//
//        if (cmd.hasOption("r")) {
//            runtime = Integer.valueOf(cmd.getOptionValue("r"));
//        } else {
//            formatter.printHelp("FeedReader", options);
//        }
//        if (cmd.hasOption("ci")) {
//            checkInterval = Integer.valueOf(cmd.getOptionValue("ci"));
//        }
//        if (cmd.hasOption("cf")) {
//            if (checkInterval == -1) { // emulate old usage of FixLearned as checkInterval = -1
//                updateStrategy = new FixLearnedUpdateStrategy();
//            } else {
//                updateStrategy = new FixUpdateStrategy(checkInterval);
//            }
//        } else if (cmd.hasOption("ca")) {
//            updateStrategy = new MavUpdateStrategy();
//        } else if (cmd.hasOption("cp")) {
//            updateStrategy = new PostRateUpdateStrategy();
//        }
//
//        FeedReader fc = new FeedReader(DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig()));
//        FeedProcessingAction fpa = new FeedProcessingAction() {
//
//            @Override
//            public boolean performAction(Feed feed, HttpResult httpResult) {
//                LOGGER.info("do stuff with " + feed.getFeedUrl());
//                LOGGER.info("::: update interval: " + feed.getUpdateInterval() + ", checks: " + feed.getChecks());
//                return true;
//            }
//
//            @Override
//            public boolean performActionOnException(Feed feed, HttpResult httpResult) {
//                // TODO Auto-generated method stub
//                return true;
//            }
//
//            @Override
//            public boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult) {
//                // TODO Auto-generated method stub
//                return false;
//            }
//
//            @Override
//            public boolean performActionOnError(Feed feed, HttpResult httpResult) {
//                // TODO Auto-generated method stub
//                return false;
//            }
//        };
//        fc.setUpdateStrategy(updateStrategy);
//        fc.setFeedProcessingAction(fpa);
//        fc.startContinuousReading(TimeUnit.MINUTES.toMillis(runtime));
    }

}