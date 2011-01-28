package tud.iir.web.feeds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.helper.ConfigHolder;
import tud.iir.helper.Counter;
import tud.iir.helper.DateHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.web.Crawler;
import tud.iir.web.feeds.FeedContentClassifier.FeedContentType;
import tud.iir.web.feeds.evaluation.FeedBenchmarkFileReader;
import tud.iir.web.feeds.evaluation.FeedReaderEvaluator;
import tud.iir.web.feeds.persistence.CollectionFeedSource;
import tud.iir.web.feeds.persistence.FeedDatabase;
import tud.iir.web.feeds.persistence.FeedStore;
import tud.iir.web.feeds.updates.FixUpdateStrategy;
import tud.iir.web.feeds.updates.MAVUpdateStrategy;
import tud.iir.web.feeds.updates.PostRateUpdateStrategy;
import tud.iir.web.feeds.updates.UpdateStrategy;


/**
 * The FeedReader reads news from feeds in a database. It learns when it is necessary to check the feed again for news.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public final class FeedReader {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(FeedReader.class);

    /** Maximum number of feed reading threads at the same time. */
    public static final Integer DEFAULT_THREAD_POOL_SIZE = 200;

    private Integer threadPoolSize = DEFAULT_THREAD_POOL_SIZE;

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
    private UpdateStrategy updateStrategy = new FixUpdateStrategy();

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed
     * must be updated whenever the method {@link Feed#getLastPollTime()} return value is further away in the past then
     * its {@link Feed#getMaxCheckInterval()} or {@link Feed#getUpdateInterval()} returns. Which one to use depends on
     * the update strategy.
     */
    private Timer checkScheduler;

    /** The feedstore. */
    private FeedStore feedStore;

    /**
     * Defines the time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds should
     * be read.
     */
    private final long wakeUpInterval = 150 * DateHelper.SECOND_MS;

    /** The constructor. */
    public FeedReader(FeedStore feedStore) {
        super();
        checkScheduler = new Timer();
        this.feedStore = feedStore;
        feedCollection = feedStore.getFeeds();
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        threadPoolSize = config.getInteger("feedReader.threadPoolSize", DEFAULT_THREAD_POOL_SIZE);
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

        LOGGER.debug("loaded " + feedCollection.size() + " feeds");

        // checkScheduler.schedule(schedulerTask, wakeUpInterval, wakeUpInterval);
        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {

            SchedulerTask schedulerTask = new SchedulerTask(this);
            checkScheduler.schedule(schedulerTask, 0, wakeUpInterval);

        } else {
            // SchedulerTaskBenchmark schedulerTaskBenchmark = new SchedulerTaskBenchmark(this);
            // checkScheduler.schedule(schedulerTaskBenchmark, 0, 50);

            StopWatch sw = new StopWatch();
            int feedHistoriesCompletelyRead = 0;
            int feedCounter = 0;
            boolean modSkip = FeedReaderEvaluator.benchmarkSample > 50 && FeedReaderEvaluator.benchmarkSample < 100;
            int mod = 100 / FeedReaderEvaluator.benchmarkSample;
            if (modSkip) {
                mod = 100 / (100 - FeedReaderEvaluator.benchmarkSample);
            }

            for (Feed feed : getFeeds()) {

                feedCounter++;

                // skip some feeds if we want to take a sample only
                if (!modSkip && feedCounter % mod != 0 || modSkip && feedCounter % mod == 0) {
                    continue;
                }

                // we skip OTF feeds
                if (feed.getActivityPattern() == FeedClassifier.CLASS_ON_THE_FLY) {
                    continue;
                }

                // int dbgid = 1;
                // if (feed.getId() > dbgid) {
                // break;
                // }
                // if (feed.getId() < dbgid) {
                // continue;
                // }
                StopWatch swf = new StopWatch();
                FeedBenchmarkFileReader fbfr = new FeedBenchmarkFileReader(feed, this);

                if (fbfr.getTotalEntries() == 0) {
                    LOGGER.info("no entries in feed (file not found?): " + feed.getId() + " (" + feed.getFeedUrl()
                            + ")");
                    continue;
                }

                // in time mode, we have a certain interval we want to observe the feeds in, otherwise we just take the
                // first real poll that is available
                if (FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_TIME) {
                    feed.setBenchmarkLookupTime(FeedReaderEvaluator.BENCHMARK_START_TIME_MILLISECOND);
                }

                int loopCount = 0;
                boolean keepLooping = true;
                while (keepLooping) {

                    fbfr.updateEntriesFromDisk();
                    loopCount++;

                    // we do not include all empty polls in fixed mode because the evaluation files would get too big,
                    // since the interval is fixed we can simply copy the last poll until we reach the end
                    if (feed.historyFileCompletelyRead() && getUpdateStrategy() instanceof FixUpdateStrategy) {
                        break;
                    }

                    keepLooping = FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_TIME
                    && feed.getBenchmarkLastLookupTime() < FeedReaderEvaluator.BENCHMARK_STOP_TIME_MILLISECOND;
                    if (!keepLooping) {
                        keepLooping = FeedReaderEvaluator.benchmarkMode == FeedReaderEvaluator.BENCHMARK_POLL
                        && !feed.historyFileCompletelyRead();
                    }
                }

                feedHistoriesCompletelyRead++;

                if (feedHistoriesCompletelyRead == getFeeds().size()) {
                    LOGGER.info("all feed history files read");
                }

                long timePerFeed = sw.getElapsedTime() / feedHistoriesCompletelyRead;

                LOGGER.info(loopCount + " loops in " + swf.getElapsedTime() + "ms in feed " + feed.getId());

                if (feedHistoriesCompletelyRead % 100 == 0) {
                    LOGGER.info(MathHelper.round(100 * feedHistoriesCompletelyRead / getFeeds().size(), 2)
                            + "% of history files completely read (absolute: " + feedHistoriesCompletelyRead + ")");
                    LOGGER.info("time per feed: " + timePerFeed);
                }
                if (feedHistoriesCompletelyRead % 500 == 0) {
                    FeedReaderEvaluator.writeRecordedMaps(this);
                }

                // save the feed back to the database
                // fa.updateFeed(feed);

                feed.freeMemory();
                feed.setLastHeadlines("");
                feed.setMeticulousPostDistribution(null);
            }

            LOGGER.info("finished reading feeds from disk in " + sw.getElapsedTimeString());
            LOGGER.info("writing evaluation results...");
            FeedReaderEvaluator.writeRecordedMaps(this);
            LOGGER.info("...done");
            setStopped(true);
        }

        LOGGER.debug("scheduled task, wake up every " + wakeUpInterval
                + " milliseconds to check all feeds whether they need to be read or not");

        while (!stopWatch.timeIsUp() && !isStopped()) {

            if (FeedReaderEvaluator.benchmarkPolicy == FeedReaderEvaluator.BENCHMARK_OFF) {
                LOGGER.trace("time is not up, keep reading feeds");
                LOGGER.debug("current total traffic: " + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + " MB");

                try {
                    Thread.sleep(1 * DateHelper.MINUTE_MS);
                } catch (InterruptedException e) {
                    LOGGER.warn(e.getMessage());
                    setStopped(true);
                    break;
                }
            }

        }
        LOGGER.info("stopped reading feeds after " + stopWatch.getElapsedTimeString());

        stopContinuousReading();

        LOGGER.info("cancelled all scheduled readings, total size downloaded (" + getUpdateStrategy() + "): "
                + Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES) + " MB");
        // System.out.println("abc");
    }

    /** Start continuous reading without a time limit. */
    public void startContinuousReading() {
        startContinuousReading(-1);
    }

    /**
     * Stop all timers, no reading will be performed after stopping the reader.
     */
    public void stopContinuousReading() {
        setStopped(true);
        LOGGER.info("stopped continuous reading");
        checkScheduler.cancel();
    }

    /**
     * Update the check interval depending on the chosen approach. Update the feed accordingly and return it. TODO this
     * method is insanely long, break it down!
     * 
     * @param feed The feed to update.
     * @param entries A list of entries of that feed. They are given in order to save the time here to retrieve them
     *            first.
     * @return The updated feed.
     */
    public synchronized void updateCheckIntervals(Feed feed) {

        FeedPostStatistics fps = new FeedPostStatistics(feed);

        updateStrategy.update(feed, fps);

        feed.setLastFeedEntry(new Date(fps.getTimeNewestPost()));
        feed.increaseChecks();
    }

    /**
     * Use the {@link FeedReader} for aggregating feed items.
     * @param downloadPages
     */
    public void aggregate(boolean downloadPages) {
        aggregate(-1, downloadPages);
    }

    /**
     * Use the {@link FeedReader} for aggregating feed items for the specified duration.
     * @param duration time in milliseconds, -1 for no limit.
     * @param downloadPages
     */
    public void aggregate(long duration, final boolean downloadPages) {

        final FeedDownloader feedDownloader = new FeedDownloader();
        final Counter newItems = new Counter();

        FeedProcessingAction processingAction = new FeedProcessingAction() {

            @Override
            public void performAction(Feed feed) {

                List<FeedItem> items = feed.getItems();
                LOGGER.debug("aggregating entries from " + feed.getFeedUrl());

                // check, which we already have and add the missing ones.
                List<FeedItem> toAdd = new ArrayList<FeedItem>();
                for (FeedItem item : items) {
                    boolean add = feedStore.getFeedItemByRawId(feed.getId(), item.getRawId()) == null;
                    if (add) {
                        toAdd.add(item);
                    }
                }
                boolean fetchPages = downloadPages && feed.getContentType() != FeedContentType.FULL;
                if (fetchPages && !toAdd.isEmpty()) {
                    feedDownloader.scrapePages(toAdd);
                    // downloadedPages.increment(toAdd.size());
                }
                for (FeedItem feedEntry : toAdd) {
                    feedStore.addFeedItem(feed, feedEntry);
                    newItems.increment();
                }

            }
        };
        setFeedProcessingAction(processingAction);
        startContinuousReading(duration);

        LOGGER.info("# of new entries : " + newItems);

    }

    // ======================
    // === Setter methods ===
    // ======================


    public void setFeedProcessingAction(FeedProcessingAction feedProcessingAction) {
        this.feedProcessingAction = feedProcessingAction;
    }

    // TODO add multiple feed actions

    /**
     * Set the approach for checking feeds for news. Once an approach is chosen it cannot be changed (meta information
     * is saved in the feed store) unless you
     * reset the learned data.
     * 
     * @param updateStrategy The updating strategy for the feed reader.
     * @param resetLearnedValues If true, learned and calculated values such as check intervals etc. are reset and are
     *            retrained using the new check approach.
     */
    public void setUpdateStrategy(UpdateStrategy updateStrategy, boolean resetLearnedValues) {
        if (!this.updateStrategy.equals(updateStrategy) && resetLearnedValues) {
            new FeedDatabase().changeCheckApproach();
        }
        this.updateStrategy = updateStrategy;
    }

    public UpdateStrategy getUpdateStrategy() {
        return updateStrategy;
    }

    /** Get the human readable name of the chosen check approach. */
    public String getUpdateStrategyName() {
        return getUpdateStrategy().getName();
    }

    public FeedProcessingAction getFeedProcessingAction() {
        return feedProcessingAction;
    }

    public Collection<Feed> getFeeds() {
        return feedCollection;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public boolean isStopped() {
        return stopped;
    }

    public void updateFeed(Feed feed) {
        getFeedStore().updateFeed(feed);
    }

    /**
     * Sample usage. Command line: parameters: checkType("cf" or "ca" or "cp") runtime(in minutes) checkInterval(only if
     * checkType=1),
     * 
     * @throws FeedDownloaderException
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws FeedDownloaderException {

        FeedReader r = new FeedReader(new FeedDatabase());
        r.setThreadPoolSize(1);
        r.aggregate(1000 * 60 * 5, false);
        System.exit(0);

        FeedReader fchecker = new FeedReader(new FeedDatabase());
        fchecker.setUpdateStrategy(new FixUpdateStrategy(), true);
        fchecker.startContinuousReading();
        System.exit(0);

        FeedReader fch = new FeedReader(new CollectionFeedSource());
        fch.setUpdateStrategy(new FixUpdateStrategy(), true);
        Feed feed = new Feed("http://de.answers.yahoo.com/rss/allq");
        feed.setActivityPattern(FeedClassifier.CLASS_SLICED);

        FeedDownloader feedDownloader = new FeedDownloader();
        feedDownloader.updateFeed(feed);
        // feed.increaseChecks();
        fch.updateCheckIntervals(feed);
        System.exit(0);

        Options options = new Options();

        OptionGroup checkApproachOption = new OptionGroup();
        checkApproachOption.addOption(OptionBuilder.withArgName("cf").withLongOpt("CHECK_FIXED")
                .withDescription("check each feed at a fixed interval").create());
        checkApproachOption.addOption(OptionBuilder.withArgName("ca").withLongOpt("CHECK_ADAPTIVE")
                .withDescription("check each feed and learn its update times").create());
        checkApproachOption.addOption(OptionBuilder.withArgName("cp").withLongOpt("CHECK_PROPABILISTIC")
                .withDescription("check each feed and adapt to its update rate").create());
        checkApproachOption.setRequired(true);
        options.addOptionGroup(checkApproachOption);
        options.addOption("r", "runtime", true,
        "The runtime of the checker in minutes or -1 if it should run until aborted.");
        options.addOption("ci", "checkInterval", true,
        "Set a fixed check interval in minutes. This is only effective if the checkType is set to CHECK_FIXED.");
        HelpFormatter formatter = new HelpFormatter();

        CommandLineParser parser = new PosixParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.debug("Command line arguments could not be parsed!");
            formatter.printHelp("FeedReader", options);
        }

        int runtime = -1;
        UpdateStrategy updateStrategy = new FixUpdateStrategy();
        int checkInterval = -1;

        if (cmd.hasOption("r")) {
            runtime = Integer.valueOf(cmd.getOptionValue("r"));
        } else {
            formatter.printHelp("FeedReader", options);
        }
        if (cmd.hasOption("cf")) {
            updateStrategy = new FixUpdateStrategy();
            ((FixUpdateStrategy) updateStrategy).setCheckInterval(checkInterval);
        } else if (cmd.hasOption("ca")) {
            updateStrategy = new MAVUpdateStrategy();
        } else if (cmd.hasOption("cp")) {
            updateStrategy = new PostRateUpdateStrategy();
        }
        if (cmd.hasOption("ci")) {
            checkInterval = Integer.valueOf(cmd.getOptionValue("ci"));
        }

        FeedReader fc = new FeedReader(new FeedDatabase());
        FeedProcessingAction fpa = new FeedProcessingAction() {

            @Override
            public void performAction(Feed feed) {
                System.out.println("do stuff with " + feed.getFeedUrl());
                System.out
                .println("::: update interval: " + feed.getUpdateInterval() + ", checks: "
                        + feed.getChecks());
            }
        };
        fc.setUpdateStrategy(updateStrategy, true);
        fc.setFeedProcessingAction(fpa);
        fc.startContinuousReading(runtime * DateHelper.MINUTE_MS);
    }

    public void setFeedStore(FeedStore feedStore) {
        this.feedStore = feedStore;
    }

    public FeedStore getFeedStore() {
        return feedStore;
    }

    /**
     * @param threadPoolSize the threadPoolSize to set
     */
    public void setThreadPoolSize(Integer threadPoolSize) {
        this.threadPoolSize = threadPoolSize;
    }

    /**
     * @return the threadPoolSize
     */
    public Integer getThreadPoolSize() {
        return threadPoolSize;
    }

}