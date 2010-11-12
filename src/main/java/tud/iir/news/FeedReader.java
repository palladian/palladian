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
import tud.iir.helper.ThreadHelper;
import tud.iir.news.evaluation.FeedBenchmarkFileReader;
import tud.iir.news.evaluation.FeedReaderEvaluator;
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
    private static final int DEFAULT_CHECK_TIME = 60;

    /** The chosen check Approach */
    private UpdateStrategy updateStrategy = UpdateStrategy.UPDATE_FIXED;

    /**
     * The check interval in minutes, only used if the checkApproach is {@link UpdateStrategy.CHECK_FIXED} if
     * checkInterval = -1 the
     * interval will be determined automatically at the first immediate check of the feed by looking in its past.
     */
    private int checkInterval = -1;

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed
     * must be updated whenever the method {@link Feed#getLastPollTime()} return value is further away in the past then
     * its {@link Feed#getMaxCheckInterval()} or {@link Feed#getMinCheckInterval()} returns. Which one to use depends on
     * the update strategy.
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the FeedReader should wake up the checkScheduler to see which feeds should
     * be
     * read.
     */
    private final long wakeUpInterval = 150 * DateHelper.SECOND_MS;

    /** The private constructor. */
    public FeedReader(FeedStore feedStore) {
        super();
        checkScheduler = new Timer();
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
                    if (feed.historyFileCompletelyRead() && getCheckApproach() == UpdateStrategy.UPDATE_FIXED) {
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
                ThreadHelper.sleep(1 * DateHelper.MINUTE_MS);
            }

        }
        LOGGER.info("stopped reading feeds after " + stopWatch.getElapsedTimeString());

        stopContinuousReading();

        LOGGER.info("cancelled all scheduled readings, total size downloaded (" + getCheckApproach() + "): "
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

        // the fixed minCheckInterval is (timeNewestEntry -
        // timeOldestEntry)/60*(feedLength-1)
        // the fixed maxCheckInterval is (timeNewestEntry - timeOldestEntry)/60

        FeedPostStatistics fps = new FeedPostStatistics(feed);

        // get percentage of new feed posts
        double pnTarget = feed.getTargetPercentageOfNewEntries();

        // calculate new update times depending on approach chosen
        if (UpdateStrategy.UPDATE_FIXED.equals(updateStrategy)
                && (checkInterval == -1 && feed.getChecks() > 0 || checkInterval != -1)) {

            // the checkInterval for the feed must have been determined at the
            // first check so don't do anything now OR the checkInterval is
            // fixed and does not need to be changed
            if (checkInterval != -1) {
                feed.setMinCheckInterval(checkInterval);
                feed.setMaxCheckInterval(checkInterval);
            }

        } else if (UpdateStrategy.UPDATE_FIXED.equals(updateStrategy) && checkInterval == -1
                // || (UpdateStrategy.UPDATE_ADAPTIVE.equals(checkApproach) || UpdateStrategy.UPDATE_PROBABILISTIC
                // .equals(checkApproach)) && feed.getChecks() == 0
        ) {

            updateIntervalFixed(feed, fps);

        }

        // for on-the-fly updates switch from probabilistic to adaptive
        else if ((UpdateStrategy.UPDATE_MOVING_AVERAGE.equals(updateStrategy) || (UpdateStrategy.UPDATE_POST_RATE
                .equals(updateStrategy) && feed.getActivityPattern() == FeedClassifier.CLASS_ON_THE_FLY))
        /* && feed.getChecks() > 0 */) {

            updateIntervalAdaptive(feed, pnTarget, fps);

        }

        if (UpdateStrategy.UPDATE_POST_RATE.equals(updateStrategy)
                && feed.getActivityPattern() != FeedClassifier.CLASS_ON_THE_FLY) {

            updateIntervalProbabilistic(feed, fps);

        }

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
     * @param checkApproach The updating approach, can be one of {@link CHECK_FIXED}, {@link CHECK_ADAPTIVE}, or
     *            {@link CHECK_PROBABILISTIC}
     * @param resetLearnedValues If true, learned and calculated values such as check intervals etc. are reset and are
     *            retrained using the new check approach.
     */
    public void setCheckApproach(UpdateStrategy checkApproach, boolean resetLearnedValues) {
        if (!this.updateStrategy.equals(checkApproach) && resetLearnedValues) {
            FeedDatabase.getInstance().changeCheckApproach();
        }
        this.updateStrategy = checkApproach;
    }

    /**
     * Set a fixed check interval in minutes. This is only effective if the checkType is set to {@link CHECK_FIXED}.
     * 
     * @param checkInterval Fixed check interval in minutes.
     */
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    // ======================
    // === Getter methods ===
    // ======================

    public UpdateStrategy getCheckApproach() {
        return updateStrategy;
    }

    /** Get the human readable name of the chosen check approach. */
    public String getCheckApproachName() {

        String className = "unknown";

        if (UpdateStrategy.UPDATE_FIXED.equals(updateStrategy) && checkInterval == -1) {
            className = "fixed_learned";
        } else if (UpdateStrategy.UPDATE_FIXED.equals(updateStrategy) && checkInterval != -1) {
            className = "fixed_" + checkInterval;
        } else if (UpdateStrategy.UPDATE_MOVING_AVERAGE.equals(updateStrategy)) {
            className = "adaptive";
        } else if (UpdateStrategy.UPDATE_POST_RATE.equals(updateStrategy)) {
            className = "probabilistic";
        }

        return className;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    public FeedProcessingAction getFeedProcessingAction() {
        return feedProcessingAction;
    }

    public Collection<Feed> getFeeds() {
        return feedCollection;
    }

    // ======================
    // === Helper methods ===
    // ======================

    /**
     * Update the check intervals in fixed mode.
     * 
     * @param feed The feed.
     * @param entries The feed entries.
     * @param fps The feed post statistics.
     */
    private void updateIntervalFixed(Feed feed, FeedPostStatistics fps) {

        List<FeedItem> entries = feed.getEntries();

        // the checkInterval for the feed must be determined now
        int fixedMinCheckInterval = DEFAULT_CHECK_TIME / 2;
        int fixedMaxCheckInterval = DEFAULT_CHECK_TIME;

        if (entries.size() > 1) {
            // use average distance between pub dates and total difference
            // between first and last entry
            fixedMinCheckInterval = (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
            fixedMaxCheckInterval = entries.size() * fixedMinCheckInterval;

            // use median
            //            if (fps.getMedianPostGap() != -1 && fps.getMedianPostGap() > DateHelper.MINUTE_MS) {
            //                double minInterval = fps.getMedianPostGap() / (double) DateHelper.MINUTE_MS;
            //                fixedMinCheckInterval = (int) minInterval;
            //                // fixedMaxCheckInterval = fixedMinCheckInterval * (entries.size() - 1);
            //                fixedMaxCheckInterval = (int) (minInterval * entries.size());
            //            }

            if (feed.getActivityPattern() == FeedClassifier.CLASS_DEAD) {
                fixedMinCheckInterval = 10 * 800 + (int) (Math.random() * 200);
                fixedMaxCheckInterval = 10 * 1440 + (int) (Math.random() * 600);
            } else if (feed.getActivityPattern() == FeedClassifier.CLASS_CHUNKED) {

                // for chunked entries the median post gap is likely to be zero so we set it to the time to the last
                // post
                fixedMinCheckInterval = (int) (fps.getTimeNewestPost() / DateHelper.MINUTE_MS);
                fixedMaxCheckInterval = fixedMinCheckInterval;

            } else if (feed.getActivityPattern() == FeedClassifier.CLASS_ON_THE_FLY) {

                fixedMinCheckInterval = 60;
                fixedMaxCheckInterval = 120;

            }

        } else {
            fixedMinCheckInterval = 60;
            fixedMaxCheckInterval = 120;
        }

        feed.setMinCheckInterval(fixedMinCheckInterval);
        feed.setMaxCheckInterval(fixedMaxCheckInterval);
    }

    /**
     * Update the intervals in adaptive mode.
     * 
     * @param feed The feed.
     * @param entries The entries of the feed.
     * @param pnTarget The percentage of new post entries.
     */
    private void updateIntervalAdaptive(Feed feed, double pnTarget, FeedPostStatistics fps) {

        List<FeedItem> entries = feed.getEntries();

        int minCheckInterval = feed.getMinCheckInterval();
        int maxCheckInterval = feed.getMaxCheckInterval();

        double newEntries = pnTarget * (feed.getWindowSize() - 1);

        // ########################## linear wave #########################
        // the factor by which the max checkInterval is multiplied, ranges between 2 and 0.5
        // double fMax = 1.0;
        // // all news are new, we should halve the checkInterval
        // if (pnTarget > 1) {
        // fMax = 0.5;
        // }
        // // some entries are not new so we increase the checkInterval
        // else {
        // // if (pnTarget == 0) {
        // // pnTarget = 1.0 / feed.getWindowSize();
        // // }
        // // if (pnTarget < 0.1) {
        // // pnTarget = 0.1;
        // // }
        // // fMax = 1 / pnTarget;
        // fMax = 2 - pnTarget;
        // }
        // maxCheckInterval *= fMax;

        // ######################### simple moving average for max policy ##############################
        if (newEntries > 0) {
            maxCheckInterval = entries.size() * (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        } else {
            double averagePostGap = fps.getAveragePostGap();
            averagePostGap -= fps.getIntervals().get(0) / feed.getWindowSize();
            averagePostGap += fps.getDelayToNewestPost() / feed.getWindowSize();
            maxCheckInterval = (int) (entries.size() * averagePostGap / DateHelper.MINUTE_MS);
        }

        // ######################### avg ##############################
        // the factor by which the min checkInterval is multiplied, ranges between 2 and 0.5
        // double fMin = 1.0;
        // all news are new, we should halve the checkInterval
        // if (newEntries >= 1) {
        // fMin = 1.0 / newEntries;
        // minCheckInterval *= fMin;
        // }
        // // we have not found any new entry so we increase the min checkInterval
        // else {
        // // minCheckInterval += fps.getMedianPostGap() / (2 * DateHelper.MINUTE_MS);
        // minCheckInterval += fps.getAveragePostGap() / (2 * DateHelper.MINUTE_MS);
        // }

        // ######################### simple moving average for min policy ##############################
        if (newEntries > 0) {
            minCheckInterval = (int) (fps.getAveragePostGap() / DateHelper.MINUTE_MS);
        } else {
            double averagePostGap = fps.getAveragePostGap();
            averagePostGap -= fps.getIntervals().get(0) / feed.getWindowSize();
            averagePostGap += fps.getDelayToNewestPost() / feed.getWindowSize();
            minCheckInterval = (int) (averagePostGap / DateHelper.MINUTE_MS);
        }

        // ######################### exponential moving average ##############################
        // Double[] weights = new Double[5];
        // weights[0] = 0.086;
        // weights[1] = 0.107;
        // weights[2] = 0.143;
        // weights[3] = 0.216;
        // weights[4] = 0.447;
        // long minCheckIntervalTemp = 0;
        // if (newEntries > 0) {
        // for (int i = 0; i < fps.getIntervals().size(); i++) {
        // minCheckIntervalTemp += (weights[i] * fps.getIntervals().get(i));
        // }
        // } else {
        //
        // List<Long> intervals = new ArrayList<Long>();
        //
        // // shift intervals
        // for (int i = 1; i < fps.getIntervals().size(); i++) {
        // intervals.add(fps.getIntervals().get(i));
        // }
        // intervals.add(fps.getDelayToNewestPost());
        //
        // for (int i = 0; i < intervals.size(); i++) {
        // minCheckIntervalTemp += (weights[i] * intervals.get(i));
        // }
        //
        // }
        //
        // minCheckInterval = (int) (minCheckIntervalTemp / DateHelper.MINUTE_MS);

        feed.setMinCheckInterval(minCheckInterval);
        feed.setMaxCheckInterval(maxCheckInterval);

        // in case only one entry has been found use default check time
        if (entries.size() <= 1) {
            feed.setMinCheckInterval(DEFAULT_CHECK_TIME / 2);
            feed.setMaxCheckInterval(DEFAULT_CHECK_TIME);
        }
    }

    /**
     * Update the intervals in probabilistic mode.
     * 
     * @param feed The feed.
     * @param entries The entries of the feed.
     * @param fps The feed post statistics of the feed.
     */
    private void updateIntervalProbabilistic(Feed feed, FeedPostStatistics fps) {

        List<FeedItem> entries = feed.getEntries();

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
            // we have counted the chances for entries before the last seen
            // entry already, so we skip them here
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
            // we have counted the posts for entries before the last seen
            // entry already, so we skip them here
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
            // System.out.println(a.getKey()+":"+a.getValue()[0]+","+a.getValue()[1]);
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
        if (true || feed.oneFullDayHasBeenSeen()) {

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
        }
    }

    private void updateIntervalProbabilistic_(Feed feed, FeedPostStatistics fps) {

        List<FeedItem> entries = feed.getEntries();

        if (feed.getChecks() == 0) {

            // learn the post distribution from the past to get initial check intervals
            // distribution minute of the day : frequency of news in that minute (items,chances)
            Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

            // update the minutes where an entry could have been posted
            int minuteCounter = 0;
            int startMinute = (int) DateHelper.getTimeOfDay(fps.getTimeOldestPost(), Calendar.MINUTE);
            for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + DateHelper.MINUTE_MS; t += DateHelper.MINUTE_MS, minuteCounter++) {
                int minuteOfDay = (startMinute + minuteCounter) % 1440;
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[1] = postsChances[1] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }
            // for (Map.Entry<Integer, int[]> a : postDistribution.entrySet()) {
            // System.out.println(a.getKey()+":"+a.getValue()[0]+","+a.getValue()[1]);
            // }

            // update the minutes where an entry was actually posted
            for (FeedItem entry : entries) {
                if (entry.getPublished() == null) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[0] = postsChances[0] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            feed.setMeticulousPostDistribution(postDistribution);

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            }

        } else if (feed.getChecks() > 0) {

            // learn the post distribution from the last seen entry to the newest one
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = feed.getMeticulousPostDistribution();

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                postDistribution = FeedDatabase.getInstance().getFeedPostDistribution(feed);
            }

            // update the minutes where an entry could have been posted
            long timeLastSeenEntry = feed.getLastFeedEntry().getTime();

            for (long t = fps.getTimeOldestPost(); t <= fps.getTimeNewestPost(); t += DateHelper.MINUTE_MS) {
                // we have counted the chances for entries before the last seen
                // entry already, so we skip them here
                if (t <= timeLastSeenEntry) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(t, Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[1] = postsChances[1] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            // update the minutes where an entry was actually posted
            for (FeedItem entry : entries) {
                // we have counted the posts for entries before the last seen
                // entry already, so we skip them here
                if (entry.getPublished() == null || entry.getPublished().getTime() <= timeLastSeenEntry) {
                    continue;
                }
                int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
                int[] postsChances = postDistribution.get(minuteOfDay);
                postsChances[0] = postsChances[0] + 1;
                postDistribution.put(minuteOfDay, postsChances);
            }

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            }
            feed.setMeticulousPostDistribution(postDistribution);

            // only use calculated update intervals if one full day of distribution is available already
            if (feed.oneFullDayHasBeenSeen()) {

                int startMinute = 0;

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

                // for (Map.Entry<Integer, int[]> a : postDistribution.entrySet()) {
                // System.out.println(a.getKey()+":"+a.getValue()[0]+","+a.getValue()[1]);
                // }

                // remember at which iteration the probabilistic approach took over
                // if (benchmark != BENCHMARK_OFF) {
                // Integer iteration = probabilisticSwitchMap.get(feed.getId());
                // if (iteration == null) {
                // probabilisticSwitchMap.put(feed.getId(), feed.getChecks());
                // }
                // }
            }
        }
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public boolean isStopped() {
        return stopped;
    }

    /**
     * Sample usage. Command line: parameters: checkType("cf" or "ca" or "cp") runtime(in minutes) checkInterval(only if
     * checkType=1),
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        FeedReader fchecker = new FeedReader(FeedDatabase.getInstance());
        fchecker.setCheckApproach(UpdateStrategy.UPDATE_FIXED, true);
        fchecker.startContinuousReading();
        System.exit(0);

        FeedReader fch = new FeedReader(new FeedStoreDummy());
        fch.setCheckApproach(UpdateStrategy.UPDATE_FIXED, true);
        Feed feed = new Feed("http://de.answers.yahoo.com/rss/allq");
        feed.setActivityPattern(FeedClassifier.CLASS_SLICED);
        feed.updateEntries(false);
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
        UpdateStrategy checkType = UpdateStrategy.UPDATE_FIXED;
        int checkInterval = -1;

        if (cmd.hasOption("r")) {
            runtime = Integer.valueOf(cmd.getOptionValue("r"));
        } else {
            formatter.printHelp("FeedReader", options);
        }
        if (cmd.hasOption("cf")) {
            checkType = UpdateStrategy.UPDATE_FIXED;
        } else if (cmd.hasOption("ca")) {
            checkType = UpdateStrategy.UPDATE_MOVING_AVERAGE;
        } else if (cmd.hasOption("cp")) {
            checkType = UpdateStrategy.UPDATE_POST_RATE;
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
        fc.setCheckApproach(checkType, true);
        fc.setCheckInterval(checkInterval);
        fc.setFeedProcessingAction(fpa);
        fc.startContinuousReading(runtime * DateHelper.MINUTE_MS);
    }

}