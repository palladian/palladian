package tud.iir.news;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.news.evaluation.FeedBenchmarkFileReader;
import tud.iir.news.evaluation.FeedReaderEvaluator;
import tud.iir.news.updates.FixUpdateStrategy;
import tud.iir.news.updates.MAVUpdateStrategy;
import tud.iir.news.updates.PostRateUpdateStrategy;
import tud.iir.news.updates.UpdateStrategy;
import tud.iir.web.Crawler;

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
    public static final Integer MAX_THREAD_POOL_SIZE = 200;

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
     * its {@link Feed#getMaxCheckInterval()} or {@link Feed#getMinCheckInterval()} returns. Which one to use depends on
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

    /** The private constructor. */
    public FeedReader(FeedStore feedStore) {
        super();
        checkScheduler = new Timer();
        this.feedStore = feedStore;
        feedCollection = feedStore.getFeeds();
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

        // updateStrategy.update(feed, fps);

        // List<FeedItem> entries = feed.getEntries();
        //
        // int minCheckInterval = feed.getMinCheckInterval();
        // int maxCheckInterval = feed.getMaxCheckInterval();
        //
        // double newEntries = feed.getTargetPercentageOfNewEntries() * (feed.getWindowSize() - 1);
        //
        // // ######################### simple moving average ##############################
        // if (newEntries > 0) {
        // minCheckInterval = (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        // maxCheckInterval = (int) (entries.size() * fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        // } else {
        // if (fps.getIntervals().size() > 0) {
        // double averagePostGap = fps.getAveragePostGap();
        // // averagePostGap -= fps.getIntervals().get(0) / feed.getWindowSize();
        // // averagePostGap += fps.getDelayToNewestPost() / feed.getWindowSize();
        // averagePostGap -= fps.getIntervals().get(0) / fps.getIntervals().size();
        // averagePostGap += fps.getDelayToNewestPost() / fps.getIntervals().size();
        // minCheckInterval = (int) (averagePostGap / DateHelper.MINUTE_MS);
        // maxCheckInterval = (int) (entries.size() * averagePostGap / DateHelper.MINUTE_MS);
        // }
        // }
        //
        // feed.setMinCheckInterval(minCheckInterval);
        // feed.setMaxCheckInterval(maxCheckInterval);
        //
        // // in case only one entry has been found use default check time
        // if (entries.size() <= 1) {
        // feed.setMinCheckInterval(FeedReader.DEFAULT_CHECK_TIME / 2);
        // feed.setMaxCheckInterval(FeedReader.DEFAULT_CHECK_TIME);
        // }

        List<FeedItem> entries = feed.getItems();

        // learn the post distribution from the last seen entry to the newest one
        // distribution minute of the day : frequency of news in that minute
        Map<Integer, int[]> postDistribution = null;

        if (feed.getChecks() == 0) {
            postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

        } else {
            postDistribution = feed.getMeticulousPostDistribution();

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                postDistribution = FeedDatabase.getInstance().getFeedPostDistribution(feed);
            }

        }

        // update the minutes where an entry could have been posted
        int minuteCounter = 0;
        long timeLastSeenEntry = Long.MIN_VALUE;
        if (feed.getLastFeedEntry() != null) {
            timeLastSeenEntry = feed.getLastFeedEntry().getTime();
        }
        int startMinute = (int) DateHelper.getTimeOfDay(fps.getTimeOldestPost(), Calendar.MINUTE);
        for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + DateHelper.MINUTE_MS; t += DateHelper.MINUTE_MS, minuteCounter++) {
            // we have counted the chances for entries before the last seen entry already, so we skip them here
            if (t <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (startMinute + minuteCounter) % 1440;
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[1] = postsChances[1] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        // update the minutes where an entry was actually posted
        for (FeedItem entry : entries) {
            // we have counted the posts for entries before the last seen entry already, so we skip them here
            if (entry.getPublished() == null || entry.getPublished().getTime() <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[0] = postsChances[0] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        int t1 = 0, t2 = 0;
        for (Map.Entry<Integer, int[]> a : postDistribution.entrySet()) {
            t1 += a.getValue()[0];
            t2 += a.getValue()[1];
        }
        // System.out.println(t1 + "," + t2);

        feed.setMeticulousPostDistribution(postDistribution);

        // in benchmark mode we keep it in memory, in real usage, we store the distribution in the database
        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
        }

        // only use calculated update intervals if one full day of distribution is available already

        startMinute = 0;

        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);
        } else {
            startMinute = (int) DateHelper.getTimeOfDay(feed.getBenchmarkLookupTime(), Calendar.MINUTE);
        }

        // // estimate time to next entry and time until list is full with
        // only new but one entries

        // set to one month maximum
        int minCheckInterval = 31 * 1440;
        boolean minCheckIntervalFound = false;

        // set to six month maximum
        int maxCheckInterval = 6 * 31 * 1440;

        // add up all probabilities for the coming minutes until the
        // estimated post number is 1
        int currentMinute = startMinute;
        double estimatedPosts = 0;
        for (int c = 0; c < maxCheckInterval; c++) {

            int[] postsChances = postDistribution.get(currentMinute);
            double postProbability = 0;
            if (postsChances[1] > 0) {
                postProbability = (double) postsChances[0] / (double) postsChances[1];
            }
            estimatedPosts += postProbability;

            if (estimatedPosts >= 1 && !minCheckIntervalFound) {
                minCheckInterval = c;
                minCheckIntervalFound = true;
            }

            if (estimatedPosts >= entries.size()) {
                maxCheckInterval = c;
                break;
            }

            currentMinute = (currentMinute + 1) % 1440;
        }

        feed.setMinCheckInterval(minCheckInterval);
        feed.setMaxCheckInterval(maxCheckInterval);

        // /////////
        feed.setLastFeedEntry(new Date(fps.getTimeNewestPost()));
        feed.increaseChecks();
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
            FeedDatabase.getInstance().changeCheckApproach();
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
     * @throws NewsAggregatorException
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) throws NewsAggregatorException {

        FeedReader fchecker = new FeedReader(FeedDatabase.getInstance());
        fchecker.setUpdateStrategy(new FixUpdateStrategy(), true);
        fchecker.startContinuousReading();
        System.exit(0);

        FeedReader fch = new FeedReader(new FeedStoreDummy());
        fch.setUpdateStrategy(new FixUpdateStrategy(), true);
        Feed feed = new Feed("http://de.answers.yahoo.com/rss/allq");
        feed.setActivityPattern(FeedClassifier.CLASS_SLICED);

        FeedDownloader feedDownloader = new FeedDownloader();
        feedDownloader.updateItems(feed);
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

        FeedReader fc = new FeedReader(FeedDatabase.getInstance());
        FeedProcessingAction fpa = new FeedProcessingAction() {

            @Override
            public void performAction(Feed feed) {
                System.out.println("do stuff with " + feed.getFeedUrl());
                System.out.println("::: check interval: " + feed.getMaxCheckInterval() + ", checks: "
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

}