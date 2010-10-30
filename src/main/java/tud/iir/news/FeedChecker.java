package tud.iir.news;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;

import org.apache.log4j.Logger;

import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.news.statistics.PollData;
import tud.iir.web.Crawler;

/**
 * The FeedChecker reads news from feeds in a database. It learns when it is necessary to check the feed again for news.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public final class FeedChecker {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(FeedChecker.class);

    /** Maximum number of feed reading threads at the same time. */
    public static final Integer MAX_THREAD_POOL_SIZE = 200;

    /** Benchmark off. */
    public static final int BENCHMARK_OFF = 0;

    /**
     * Benchmark algorithms towards their prediction ability for the next post. We need all feeds to be in a certain
     * time frame for a fair comparison.
     */
    public static final int BENCHMARK_MIN_CHECK = 1;

    /**
     * Benchmark algorithms towards their prediction ability for the next almost filled post list.
     */
    public static final int BENCHMARK_MAX_CHECK = 2;

    /**
     * If true, some output will be generated to evaluate the reading approaches.
     */
    public static int benchmark = BENCHMARK_MAX_CHECK;

    /** We need all feeds to be in a certain time frame for a fair comparison. */
    public static final int BENCHMARK_TIME = 1;

    /** We use the polls and the feed time don't have to be aligned. */
    public static final int BENCHMARK_POLL = 2;

    public static int benchmarkMode = BENCHMARK_POLL;

    /** The path to the folder with the feed post history files. */
    private final String benchmarkDatasetPath = "G:\\Projects\\Programming\\Other\\clean\\";

    /** The list of history files, will be loaded only once for the sake of performance. */
    private File[] benchmarkDatasetFiles;

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
    private CheckApproach checkApproach = CheckApproach.CHECK_FIXED;

    /**
     * The check interval in minutes, only used if the checkApproach is {@link CheckApproach.CHECK_FIXED} if
     * checkInterval = -1 the
     * interval will be determined automatically at the first immediate check of the feed by looking in its past.
     */
    private int checkInterval = -1;

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed
     * must be updated whenever the method {@link Feed#getLastChecked()} return value is further away in the past then
     * its {@link Feed#getMaxCheckInterval()} or {@link Feed#getMinCheckInterval()} returns. Which one to use depends on
     * the update strategy.
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the FeedChecker should wake up the checkScheduler to see which feeds should
     * be
     * read.
     */
    private final int wakeUpInterval = 150 * DateHelper.SECOND_MS;

    /** The private constructor. */
    public FeedChecker(FeedStore feedStore) {
        super();
        checkScheduler = new Timer();
        feedCollection = feedStore.getFeeds();

        if (getBenchmark() != BENCHMARK_OFF) {
            LOGGER.info("load benchmark dataset file list");
            benchmarkDatasetFiles = FileHelper.getFiles(benchmarkDatasetPath);
        }

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
    public void startContinuousReading(int duration) {

        LOGGER.info("start continuous reading of feeds for " + DateHelper.getTimeString(duration));
        StopWatch stopWatch = new StopWatch();
        stopWatch.setCountDown(duration);
        stopWatch.start();

        LOGGER.debug("loaded " + feedCollection.size() + " feeds");

        // checkScheduler.schedule(schedulerTask, wakeUpInterval, wakeUpInterval);
        if (getBenchmark() == BENCHMARK_OFF) {

            SchedulerTask schedulerTask = new SchedulerTask(this);
            checkScheduler.schedule(schedulerTask, 0, wakeUpInterval);

        } else {
            // SchedulerTaskBenchmark schedulerTaskBenchmark = new SchedulerTaskBenchmark(this);
            // checkScheduler.schedule(schedulerTaskBenchmark, 0, 50);

            StopWatch sw = new StopWatch();
            NewsAggregator fa = new NewsAggregator();
            int feedHistoriesCompletelyRead = 0;
            for (Feed feed : getFeeds()) {

                // if (feed.getId() > 83535) {
                // break;
                // }
                // if (feed.getId() < 83531) {
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
                if (benchmarkMode == BENCHMARK_TIME) {
                    feed.setBenchmarkLookupTime(FeedBenchmarkFileReader.BENCHMARK_START_TIME);
                }

                int loopCount = 0;
                while (benchmarkMode == BENCHMARK_TIME
                        && feed.getBenchmarkLastLookupTime() < FeedBenchmarkFileReader.BENCHMARK_STOP_TIME
                        || benchmarkMode == BENCHMARK_POLL && !feed.historyFileCompletelyRead()) {

                    fbfr.updateEntriesFromDisk();
                    loopCount++;

                    // we do not include all empty polls in fixed mode because the evaluation files would get too big,
                    // since the interval is fixed we can simply copy the last poll until we reach the end
                    if (feed.historyFileCompletelyRead() && getCheckApproach() == CheckApproach.CHECK_FIXED) {
                        break;
                    }

                    // FIXME
                    /*
                     * if (loopCount >= 10000) {
                     * break;
                     * }
                     */
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
                    writeRecordedMaps();
                    // break;
                }

                // save the feed back to the database
                fa.updateFeed(feed);

                feed.freeMemory();
                feed.setLastHeadlines("");
                feed.setMeticulousPostDistribution(null);
            }

            LOGGER.info("finished reading feeds from disk in " + sw.getElapsedTimeString());
            LOGGER.info("writing evaluation results...");
            writeRecordedMaps();
            LOGGER.info("...done");
        }

        LOGGER.debug("scheduled task, wake up every " + wakeUpInterval
                + " milliseconds to check all feeds whether they need to be read or not");

        while (!stopWatch.timeIsUp() && !isStopped()) {

            if (benchmark == BENCHMARK_OFF) {
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

        List<FeedEntry> entries = feed.getEntries();

        // the fixed minCheckInterval is (timeNewestEntry -
        // timeOldestEntry)/60*(feedLength-1)
        // the fixed maxCheckInterval is (timeNewestEntry - timeOldestEntry)/60

        FeedPostStatistics fps = new FeedPostStatistics(entries);

        // get percentage of new feed posts
        double pnTarget = feed.getTargetPercentageOfNewEntries();

        // calculate new update times depending on approach chosen
        if (CheckApproach.CHECK_FIXED.equals(checkApproach)
                && (checkInterval == -1 && feed.getChecks() > 0 || checkInterval != -1)) {

            // the checkInterval for the feed must have been determined at the
            // first check so don't do anything now OR the checkInterval is
            // fixed and does not need to be changed
            if (checkInterval != -1) {
                feed.setMinCheckInterval(checkInterval);
                feed.setMaxCheckInterval(checkInterval);
            }

        } else if (CheckApproach.CHECK_FIXED.equals(checkApproach)
                && checkInterval == -1
                || (CheckApproach.CHECK_ADAPTIVE.equals(checkApproach) || CheckApproach.CHECK_PROBABILISTIC
                        .equals(checkApproach)) && feed.getChecks() == 0) {

            updateIntervalFixed(feed, fps);

        }

        // for on-the-fly updates switch from probabilistic to adaptive
        else if ((CheckApproach.CHECK_ADAPTIVE.equals(checkApproach) || CheckApproach.CHECK_PROBABILISTIC
                .equals(checkApproach)
                && (feed.getActivityPattern() == FeedClassifier.CLASS_ON_THE_FLY || !feed.oneFullDayHasBeenSeen()))
                && feed.getChecks() > 0) {

            updateIntervalAdaptive(feed, pnTarget, fps);

        }

        if (CheckApproach.CHECK_PROBABILISTIC.equals(checkApproach)
                && feed.getActivityPattern() != FeedClassifier.CLASS_ON_THE_FLY) {

            updateIntervalProbabilistic(feed, fps);

        }

        // updateEvaluationMaps(feed, pnTarget, fps);

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
    public void setCheckApproach(CheckApproach checkApproach, boolean resetLearnedValues) {
        if (!this.checkApproach.equals(checkApproach) && resetLearnedValues) {
            FeedDatabase.getInstance().changeCheckApproach();
        }
        this.checkApproach = checkApproach;
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

    public CheckApproach getCheckApproach() {
        return checkApproach;
    }

    /** Get the human readable name of the chosen check approach. */
    public String getCheckApproachName() {

        String className = "unknown";

        if (CheckApproach.CHECK_FIXED.equals(checkApproach) && checkInterval == -1) {
            className = "fixed_learned";
        } else if (CheckApproach.CHECK_FIXED.equals(checkApproach) && checkInterval != -1) {
            className = "fixed_" + checkInterval;
        } else if (CheckApproach.CHECK_ADAPTIVE.equals(checkApproach)) {
            className = "adaptive";
        } else if (CheckApproach.CHECK_PROBABILISTIC.equals(checkApproach)) {
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

    protected Collection<Feed> getFeeds() {
        return feedCollection;
    }

    // ======================
    // === Helper methods ===
    // ======================

    /**
     * <p>
     * Save the feed poll information for evaluation.
     * </p>
     * 
     * <p>
     * For each update technique and evaluation mode another file must be written (8 in total = 4 techniques * 2
     * evaluation modes). This method writes only one file for the current settings.
     * </p>
     * 
     * <p>
     * Each file contains the following fields per line, fields are separated with a semicolon:
     * <ul>
     * <li>feed id</li>
     * <li>feed activity pattern</li>
     * <li>etag support (0/1)</li>
     * <li>conditional get support (0/1)</li>
     * <li>eTag response size in Byte</li>
     * <li>conditional get response size in Byte</li>
     * <li>number of poll</li>
     * <li>poll timestamp in seconds</li>
     * <li>poll hour of the day</li>
     * <li>poll minute of the day</li>
     * <li>check interval at poll time in minutes</li>
     * <li>window size</li>
     * <li>size of poll in Byte</li>
     * <li>number of missed news posts</li>
     * <li>percentage of new entries, 1 = all new (only for evaluation mode MAX interesting)</li>
     * <li>delay in seconds (only for evaluation mode MIN interesting)</li>
     * <li>score_max (only for evaluation mode MAX interesting)</li>
     * <li>score_min (only for evaluation mode MIN interesting)</li>
     * </ul>
     * </p>
     * 
     */
    private void writeRecordedMaps() {

        StopWatch sw = new StopWatch();

        String separator = ";";

        String benchmarkModeString = "0";
        if (benchmarkMode == BENCHMARK_POLL) {
            benchmarkModeString = "poll";
        } else if (benchmarkMode == BENCHMARK_TIME) {
            benchmarkModeString = "time";
        }

        String filePath = "data/temp/feedReaderEvaluation_" + getCheckApproachName() + "_" + getBenchmarkName()
        + "_"
        + benchmarkModeString + ".csv";

        try {
            FileWriter fileWriter = new FileWriter(filePath, true);

            DecimalFormat format = new DecimalFormat("0.#################");
            format.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));

            // loop through all feeds
            for (Feed feed : getFeeds()) {

                int numberOfPoll = 1;
                for (PollData pollData : feed.getPollDataSeries()) {

                    StringBuilder csv = new StringBuilder();

                    // feed related values
                    csv.append(feed.getId()).append(separator);
                    csv.append(feed.getActivityPattern()).append(separator);
                    if (feed.geteTagSupport()) {
                        csv.append("1").append(separator);
                    } else {
                        csv.append("0").append(separator);
                    }
                    if (feed.getCgSupport()) {
                        csv.append("1").append(separator);
                    } else {
                        csv.append("0").append(separator);
                    }
                    csv.append(feed.geteTagHeaderSize()).append(separator);
                    csv.append(feed.getCgHeaderSize()).append(separator);

                    // poll related values
                    csv.append(numberOfPoll).append(separator);
                    csv.append(pollData.getTimestamp() / 1000l).append(separator);
                    csv.append(DateHelper.getTimeOfDay(pollData.getTimestamp(), Calendar.HOUR)).append(separator);
                    csv.append(DateHelper.getTimeOfDay(pollData.getTimestamp(), Calendar.MINUTE)).append(separator);
                    csv.append(MathHelper.round(pollData.getCheckInterval(), 2)).append(separator);
                    csv.append(pollData.getWindowSize()).append(separator);
                    csv.append(pollData.getDownloadSize()).append(separator);
                    csv.append(pollData.getMisses()).append(separator);
                    csv.append(MathHelper.round(pollData.getPercentNew(), 2)).append(separator);
                    csv.append(pollData.getNewPostDelay() / 1000l).append(separator);

                    Double scoreMax = pollData.getScore(BENCHMARK_MAX_CHECK);
                    if (scoreMax != null) {
                        csv.append(format.format(MathHelper.round(scoreMax, 2))).append(
                                separator);
                    } else {
                        csv.append("\"N").append(separator);
                    }

                    Double scoreMin = pollData.getScore(BENCHMARK_MIN_CHECK);
                    if (scoreMin != null) {
                        csv.append(format.format(MathHelper.round(scoreMin, 4))).append(
                                separator);
                    } else {
                        csv.append("\"N").append(separator);
                    }
                    csv.append("\n");

                    fileWriter.write(csv.toString());
                    fileWriter.flush();

                    numberOfPoll++;
                }

                // data is appended to the file so we can/must clear the poll data series here, that also saves us
                // memory
                feed.getPollDataSeries().clear();
            }

            fileWriter.flush();
            fileWriter.close();

        } catch (IOException e) {
            LOGGER.error(filePath + ", " + e.getMessage());
        }

        LOGGER.info("wrote record maps in " + sw.getElapsedTimeString());

    }

    /**
     * Update the check intervals in fixed mode.
     * 
     * @param feed The feed.
     * @param entries The feed entries.
     * @param fps The feed post statistics.
     */
    private void updateIntervalFixed(Feed feed, FeedPostStatistics fps) {

        List<FeedEntry> entries = feed.getEntries();

        // the checkInterval for the feed must be determined now
        int fixedMinCheckInterval = DEFAULT_CHECK_TIME / 2;
        int fixedMaxCheckInterval = DEFAULT_CHECK_TIME;

        if (entries.size() > 1) {
            // use average distance between pub dates and total difference
            // between first and last entry
            fixedMinCheckInterval = (int) (fps.getTimeRange() / (entries.size() - 1l) / DateHelper.MINUTE_MS);
            fixedMaxCheckInterval = (int) (fps.getTimeRange() / DateHelper.MINUTE_MS);

            // use median
            if (fps.getMedianPostGap() != -1 && fps.getMedianPostGap() > DateHelper.MINUTE_MS) {
                fixedMinCheckInterval = (int) (fps.getMedianPostGap() / DateHelper.MINUTE_MS);
                // fixedMaxCheckInterval = fixedMinCheckInterval * (entries.size() - 1);
                fixedMaxCheckInterval = fixedMinCheckInterval * entries.size();
            }

            if (feed.getActivityPattern() == FeedClassifier.CLASS_DEAD) {
                fixedMinCheckInterval = 800 + (int) (Math.random() * 200);
                fixedMaxCheckInterval = 1440 + (int) (Math.random() * 600);
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
            fixedMinCheckInterval *= 3;
            fixedMaxCheckInterval *= 3;
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

        List<FeedEntry> entries = feed.getEntries();

        int minCheckInterval = feed.getMinCheckInterval();
        int maxCheckInterval = feed.getMaxCheckInterval();

        // the factor by which the max checkInterval is multiplied, ranges between 2 and 0.5
        double fMax = 1.0;

        // all news are new, we should halve the checkInterval
        if (pnTarget > 1) {
            fMax = 0.5;
        }
        // some entries are not new so we increase the checkInterval
        else {
            fMax = 2 - pnTarget;
        }
        maxCheckInterval *= fMax;

        // the factor by which the min checkInterval is multiplied, ranges between 2 and 0.5
        double fMin = 1.0;

        double newEntries = pnTarget * (feed.getWindowSize() - 1);
        // all news are new, we should halve the checkInterval
        if (newEntries >= 1) {
            fMin = 1.0 / newEntries;
            minCheckInterval *= fMin;
        }
        // we have not found any new entry so we increase the min checkInterval
        else {
            // TODO test with median post gap
            // minCheckInterval += fps.getMedianPostGap() / (2 * DateHelper.MINUTE_MS);
            minCheckInterval += fps.getAveragePostGap() / (2 * DateHelper.MINUTE_MS);
        }

        // for chunked or on the fly updates the min and max intervals are the same
        // if (feed.getUpdateClass() != FeedClassifier.CLASS_CHUNKED
        // && feed.getUpdateClass() != FeedClassifier.CLASS_ON_THE_FLY) {
        // minCheckInterval = maxCheckInterval / Math.max(1, entries.size() - 1);
        // } else {
        // minCheckInterval = maxCheckInterval;
        // }

        // minCheckInterval = maxCheckInterval / Math.max(1, entries.size() - 1);

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

        List<FeedEntry> entries = feed.getEntries();

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
            for (FeedEntry entry : entries) {
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
            if (getBenchmark() == BENCHMARK_OFF) {
                FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            }

        } else if (feed.getChecks() > 0) {

            // learn the post distribution from the last seen entry to the newest one
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = feed.getMeticulousPostDistribution();

            // in benchmark mode we keep it in memory
            if (getBenchmark() == BENCHMARK_OFF) {
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
            for (FeedEntry entry : entries) {
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
            if (getBenchmark() == BENCHMARK_OFF) {
                FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            }
            feed.setMeticulousPostDistribution(postDistribution);

            // only use calculated update intervals if one full day of distribution is available already
            if (feed.oneFullDayHasBeenSeen()) {

                int startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);

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

    public static int getBenchmark() {
        return benchmark;
    }

    public static void setBenchmark(int benchmark) {
        FeedChecker.benchmark = benchmark;
    }

    public static int getBenchmarkMode() {
        return benchmarkMode;
    }

    public static void setBenchmarkMode(int benchmarkMode) {
        FeedChecker.benchmarkMode = benchmarkMode;
    }

    private String getBenchmarkName() {
        return benchmark == BENCHMARK_MIN_CHECK ? "min" : "max";
    }

    public String getBenchmarkDatasetPath() {
        return benchmarkDatasetPath;
    }

    public void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public boolean isStopped() {
        return stopped;
    }

    /**
     * Find the history file with feed posts given the feed id. The file name starts with the feed id followed by an
     * underscore.
     * 
     * @param id The id of the feed.
     * @return The path to the file with the feed post history.
     */
    public String findHistoryFile(String safeFeedName) {

        // read feed history file
        String historyFilePath = "";
        if (benchmarkDatasetFiles == null) {
            System.out
            .println("======================================================================================");
            benchmarkDatasetFiles = FileHelper.getFiles(benchmarkDatasetPath);
        }
        for (File file : benchmarkDatasetFiles) {
            if (file.getName().startsWith(safeFeedName)) {
                historyFilePath = file.getAbsolutePath();
                break;
            }
        }

        return historyFilePath;
    }

    /**
     * Sample usage. Command line: parameters: checkType("cf" or "ca" or "cp") runtime(in minutes) checkInterval(only if
     * checkType=1),
     */
    @SuppressWarnings("static-access")
    public static void main(String[] args) {

        // FeedChecker fchecker = new FeedChecker(FeedDatabase.getInstance());
        // fchecker.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        // fchecker.startContinuousReading();
        // System.exit(0);
        //
        // FeedChecker fch = new FeedChecker(new FeedStoreDummy());
        // fch.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        // Feed feed = new Feed("http://de.answers.yahoo.com/rss/allq");
        // feed.setUpdateClass(FeedClassifier.CLASS_SLICED);
        // feed.updateEntries(false);
        // // feed.increaseChecks();
        // fch.updateCheckIntervals(feed);
        // System.exit(0);

        // Options options = new Options();
        //
        // OptionGroup checkApproachOption = new OptionGroup();
        // checkApproachOption.addOption(OptionBuilder.withArgName("cf").withLongOpt("CHECK_FIXED").withDescription(
        // "check each feed at a fixed interval").create());
        // checkApproachOption.addOption(OptionBuilder.withArgName("ca").withLongOpt("CHECK_ADAPTIVE").withDescription(
        // "check each feed and learn its update times").create());
        // checkApproachOption.addOption(OptionBuilder.withArgName("cp").withLongOpt("CHECK_PROPABILISTIC")
        // .withDescription("check each feed and adapt to its update rate").create());
        // checkApproachOption.setRequired(true);
        // options.addOptionGroup(checkApproachOption);
        // options.addOption("r", "runtime", true,
        // "The runtime of the checker in minutes or -1 if it should run until aborted.");
        // options
        // .addOption("ci", "checkInterval", true,
        // "Set a fixed check interval in minutes. This is only effective if the checkType is set to CHECK_FIXED.");
        // HelpFormatter formatter = new HelpFormatter();
        //
        // CommandLineParser parser = new PosixParser();
        // CommandLine cmd = null;
        // try {
        // cmd = parser.parse(options, args);
        // } catch (ParseException e) {
        // LOGGER.debug("Command line arguments could not be parsed!");
        // formatter.printHelp("FeedChecker", options);
        // }
        //
        // int runtime = -1;
        // CheckApproach checkType = CheckApproach.CHECK_FIXED;
        // int checkInterval = -1;
        //
        // if (cmd.hasOption("r")) {
        // runtime = Integer.valueOf(cmd.getOptionValue("r"));
        // } else {
        // formatter.printHelp("FeedChecker", options);
        // }
        // if (cmd.hasOption("cf")) {
        // checkType = CheckApproach.CHECK_FIXED;
        // } else if (cmd.hasOption("ca")) {
        // checkType = CheckApproach.CHECK_ADAPTIVE;
        // } else if (cmd.hasOption("cp")) {
        // checkType = CheckApproach.CHECK_PROBABILISTIC;
        // }
        // if (cmd.hasOption("ci")) {
        // checkInterval = Integer.valueOf(cmd.getOptionValue("ci"));
        // }

        // // benchmark settings ////
        CheckApproach checkType = CheckApproach.CHECK_FIXED;
        // checkType = CheckApproach.CHECK_ADAPTIVE;
        checkType = CheckApproach.CHECK_PROBABILISTIC;
        int checkInterval = -1;
        int runtime = 9000;
        // //////////////////////////

        FeedChecker fc = new FeedChecker(FeedDatabase.getInstance());
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
        // setBenchmark(BENCHMARK_MAX_CHECK);
        setBenchmark(BENCHMARK_MIN_CHECK);
        // setBenchmarkMode(BENCHMARK_TIME);
        fc.startContinuousReading(runtime * DateHelper.MINUTE_MS);
    }

}