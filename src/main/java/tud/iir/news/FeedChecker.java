package tud.iir.news;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.helper.CollectionHelper;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.ThreadHelper;
import tud.iir.web.Crawler;

/**
 * The FeedChecker reads news from feeds in a database. It learns when it is necessary to check the feed again for news.
 * 
 * @author David Urbansky
 * 
 */
public final class FeedChecker {

    /** the instance of this class, this class is singleton */
    private static final FeedChecker INSTANCE = new FeedChecker();

    /** the logger for this class */
    public static final Logger LOGGER = Logger.getLogger(FeedChecker.class);

    /** symbols to separate headlines */
    private static final String TITLE_SEPARATION = "<###>";

    /** benchmark off */
    private static final int BENCHMARK_OFF = 0;

    /** benchmark algorithms towards their prediction ability for the next post */
    private static final int BENCHMARK_MIN_CHECK_TIME = 1;

    /**
     * benchmark algorithms towards their prediction ability for the next almost filled post list
     */
    private static final int BENCHMARK_MAX_CHECK_TIME = 2;

    /**
     * if true, some output will be generated to evaluate the reading approaches
     */
    private int benchmark = BENCHMARK_MAX_CHECK_TIME;

    /** list of feeds that are read continuous */
    private List<Feed> feedList = null;

    /** the action that should be performed for each feed that is read */
    private FeedProcessingAction feedProcessingAction = null;

    /**
     * Record a list of percentage new values for each feed: feedID;pn1;...;pnItarationN.
     */
    private final Map<Integer, String> pnMapEvaluation = new LinkedHashMap<Integer, String>();

    /**
     * Store for each feed after which iteration the probabilistic approach took over.
     * In the beginning the adaptive approach is used until at least one full day has been seen.
     */
    private final Map<Integer, Integer> probabilisticSwitchMap = new HashMap<Integer, Integer>();

    /**
     * record a list of checkInterval values for each feed: feedID;ci1;...;ciItarationN
     */
    private final Map<Integer, String> ciMapEvaluation = new LinkedHashMap<Integer, String>();

    /**
     * record a list of checkInterval values for each feed: feedID <minuteOfDay : number of posts in that minute>
     */
    private final Map<Integer, LinkedHashMap<Integer, Integer>> postDistributionMapEvaluation = new LinkedHashMap<Integer, LinkedHashMap<Integer, Integer>>();

    /**
     * Record a list of time differences of real first news times and check times for each feed. feedID : list of time
     * differences
     */
    private final Map<Integer, ArrayList<Integer>> timeDiffMapEvaluation = new LinkedHashMap<Integer, ArrayList<Integer>>();

    /**
     * Record a list of dates the feed was checked before a new entry appeared for each feed. feedID : list of dates the
     * feed was checked before a new entry
     * appeared
     */
    private final Map<Integer, HashSet<Date>> tempCheckTimeMapEvaluation = new LinkedHashMap<Integer, HashSet<Date>>();

    /** list references to all timers so we can stop them */
    private final Set<Timer> timers = new HashSet<Timer>();

    /**
     * if a fixed checkInterval could not be learned, this one is taken (in minutes)
     */
    private static final int DEFAULT_CHECK_TIME = 10;

    // ////////////////// feed checking approaches ////////////////////
    /** the chosen check approach */
    private int checkApproach = CHECK_FIXED;

    /**
     * the check interval in minutes, only used if the checkApproach is {@link CHECK_FIXED} if checkInterval = -1 the
     * interval will be determined automatically
     * at the first immediate check of the feed by looking in its past
     */
    private int checkInterval = -1;

    /** check each feed at a fixed interval */
    public static final int CHECK_FIXED = 1;

    /** check each feed and adapt to its update rate */
    public static final int CHECK_ADAPTIVE = 2;

    /** check each feed and learn its update times */
    public static final int CHECK_PROBABILISTIC = 3;

    /** the private constructor */
    private FeedChecker() {
    }

    /**
     * The FeedReader is singleton, get the instance here.
     * 
     * @return The FeedReader instance.
     */
    public static FeedChecker getInstance() {
        return INSTANCE;
    }

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

        // get all feeds
        feedList = FeedDatabase.getInstance().getFeeds();

        // create timer tasks for each feed
        for (Feed feed : feedList) {
            addTimer(feed, feed.getMaxCheckInterval());
            // break;
        }
        int loopNumber = 0;
        while (!stopWatch.timeIsUp()) {

            ThreadHelper.sleep(1 * DateHelper.MINUTE_MS);
            LOGGER.trace("time is not up, keep reading feeds");

            if (benchmark != BENCHMARK_OFF) {
                loopNumber++;

                // write the record maps every 5 hours
                if (loopNumber % 60 == 0) {
                    writeRecordedMaps();
                }
            }
        }
        LOGGER.info("stopped reading feeds after " + stopWatch.getElapsedTimeString());

        if (benchmark != BENCHMARK_OFF) {
            writeRecordedMaps();
        }

        stopContinuousReading();

        LOGGER.info("cancelled all scheduled readings, total size downloaded ("
                + FeedChecker.getInstance().getCheckApproach() + "): "
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
        for (Timer timer : timers) {
            timer.cancel();
        }
    }

    /**
     * Add a timer for a feed.
     * 
     * @param feed The feed.
     * @param delay The delay after which the feed is read again.
     */
    public synchronized void addTimer(Feed feed, int delay) {
        Timer timer = new Timer();
        LOGGER.debug("set new timer for " + feed.getFeedUrl() + " to run in " + delay + " minutes");
        timer.schedule(new FeedTask(timer, feed), DateHelper.MINUTE_MS * (long) delay);
        timers.add(timer);
        // timer.schedule(new FeedTask(timer,feed),5000);
    }

    /**
     * Get a separated string with the headlines of all feed entries.
     * 
     * @param entries Feed entries.
     * @return A separated string with the headlines of all feed entries.
     */
    private StringBuilder getNewEntryTitles(List<FeedEntry> entries) {

        StringBuilder titles = new StringBuilder();
        for (FeedEntry entry : entries) {
            titles.append(entry.getTitle()).append(TITLE_SEPARATION);
        }

        return titles;
    }

    /**
     * Calculate the target percentage of new entries as follows: Percentage of new entries = pn = newEntries /
     * totalEntries Target Percentage = pTarget =
     * newEntries / (totalEntries - 1) A target percentage of 1 means that all entries but one are new and this is
     * exactly what we want.
     * 
     * Example 1: newEntries = 3 totalEntries = 4 pn = 0.75 pTarget = 3 / (4-1) = 1
     * 
     * Example 2: newEntries = 7 totalEntries = 10 pn = 0.7 pTarget = 7 / (10-1) ~ 0.78
     * 
     * The target percentage depends on the number of total entries and is not always the same as the examples show.
     * 
     * @param feed The feed.
     * @param entries The entries of the feed.
     * @return The percentage of news calculated as explained.
     */
    private double getTargetPercentageOfNewEntries(Feed feed) {

        List<FeedEntry> entries = feed.getEntries();

        // compare old and new entry titles to get percentage pn of new entries
        String[] oldTitlesArray = feed.getLastHeadlines().split(TITLE_SEPARATION);
        Set<String> oldTitles = CollectionHelper.toHashSet(oldTitlesArray);

        // get new entry titles
        StringBuilder titles = getNewEntryTitles(entries);
        Set<String> currentTitles = CollectionHelper.toHashSet(titles.toString().split(TITLE_SEPARATION));

        // count number of same titles
        int overlap = 0;
        for (String oldTitle : oldTitles) {
            for (String newTitle : currentTitles) {
                if (oldTitle.equalsIgnoreCase(newTitle)) {
                    overlap++;
                    LOGGER.trace("equal headline: " + oldTitle);
                    LOGGER.trace("with headline:  " + newTitle);
                }
            }
        }

        // number of really new headlines
        int newEntries = currentTitles.size() - overlap;

        // percentage of new entries - 1 entry, this is our target, if we know
        // at least one entry we know that we did not miss any
        double pnTarget = 1;

        if (currentTitles.size() > 1) {
            pnTarget = newEntries / ((double) currentTitles.size() - 1);
        } else {
            // in this special case we just look at the feed the default check time
            // pnTarget = -1;
            LOGGER.warn("only one title found in " + feed.getFeedUrl());
        }

        feed.setLastHeadlines(titles.toString());

        return pnTarget;
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
        double pnTarget = getTargetPercentageOfNewEntries(feed);

        // calculate new update times depending on approach chosen
        if (checkApproach == CHECK_FIXED && (checkInterval == -1 && feed.getChecks() > 0 || checkInterval != -1)) {

            // the checkInterval for the feed must have been determined at the
            // first check so don't do anything now OR the checkInterval is
            // fixed and does not need to be changed
            if (checkInterval != -1) {
                feed.setMinCheckInterval(checkInterval);
                feed.setMaxCheckInterval(checkInterval);
            }

        } else if ((checkApproach == CHECK_FIXED && checkInterval == -1 || checkApproach == CHECK_ADAPTIVE || checkApproach == CHECK_PROBABILISTIC)
                && feed.getChecks() == 0) {

            updateIntervalFixed(feed, fps);

        }

        // for on-the-fly updates switch from probabilistic to adaptive
        else if ((checkApproach == CHECK_ADAPTIVE || checkApproach == CHECK_PROBABILISTIC && (feed.getUpdateClass() == FeedClassifier.CLASS_ON_THE_FLY || !feed
                .oneFullDayHasBeenSeen()))
                && feed.getChecks() > 0) {

            updateIntervalAdaptive(feed, pnTarget);

        }

        if (checkApproach == CHECK_PROBABILISTIC && feed.getUpdateClass() != FeedClassifier.CLASS_ON_THE_FLY) {

            updateIntervalProbabilistic(feed, fps);

        }

        updateEvaluationMaps(feed, pnTarget, fps);

        feed.setLastFeedEntry(new Date(fps.getTimeNewestPost()));
        feed.increaseChecks();
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
            fixedMinCheckInterval = (int) (fps.getTimeRange() / (entries.size() - 1)) / DateHelper.MINUTE_MS;
            fixedMaxCheckInterval = (int) (fps.getTimeRange() / DateHelper.MINUTE_MS);

            // use median
            if (fps.getMedianPostGap() != -1) {
                fixedMinCheckInterval = (int) (fps.getMedianPostGap() / DateHelper.MINUTE_MS);
                fixedMaxCheckInterval = fixedMinCheckInterval * (entries.size() - 1);
            }
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
    private void updateIntervalAdaptive(Feed feed, double pnTarget) {

        List<FeedEntry> entries = feed.getEntries();

        // the factor by which the checkInterval is multiplied, ranges between 2 and 0.5
        double f = 1.0;

        // all news are new, we should halve the checkInterval
        if (pnTarget > 1) {
            f = 0.5;
        }
        // some entries are not new so we increase the checkInterval
        else {
            f = 2 - pnTarget;
        }

        int minCheckInterval = feed.getMinCheckInterval();
        int maxCheckInterval = feed.getMaxCheckInterval();
        maxCheckInterval *= f;

        // for chunked or on the fly updates the min and max intervals are the same
        if (feed.getUpdateClass() != FeedClassifier.CLASS_CHUNKED
                && feed.getUpdateClass() != FeedClassifier.CLASS_ON_THE_FLY) {
            minCheckInterval = maxCheckInterval / Math.max(1, entries.size() - 1);
        } else {
            minCheckInterval = maxCheckInterval;
        }

        feed.setMinCheckInterval(minCheckInterval);
        feed.setMaxCheckInterval(maxCheckInterval);

        // in case only one entry has been found use default check time
        if (entries.size() == 1) {
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
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

            // update the minutes where an entry could have been posted
            for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + DateHelper.MINUTE_MS; t += DateHelper.MINUTE_MS) {
                int minuteOfDay = (int) DateHelper.getTimeOfDay(t, Calendar.MINUTE);
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

            FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);

        } else if (feed.getChecks() > 0) {

            // learn the post distribution from the last seen entry to the newest one
            // distribution minute of the day : frequency of news in that minute
            Map<Integer, int[]> postDistribution = FeedDatabase.getInstance().getFeedPostDistribution(feed);

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

            FeedDatabase.getInstance().updateFeedPostDistribution(feed, postDistribution);
            feed.setMeticulousPostDistribution(postDistribution);

            // only use calculated update intervals if one full day of distribution is available already
            if (feed.oneFullDayHasBeenSeen()) {

                int startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);

                // // estimate time to next entry and time until list is full with
                // only new but one entries

                // set to thirty days maximum
                int minCheckInterval = 30 * 1440;
                boolean minCheckIntervalFound = false;

                // set to one hundred days maximum
                int maxCheckInterval = 100 * 1440;

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

                    if (estimatedPosts >= entries.size() - 1) {
                        maxCheckInterval = c;
                        break;
                    }

                    currentMinute = (currentMinute + 1) % 1440;
                }

                feed.setMinCheckInterval(minCheckInterval);
                feed.setMaxCheckInterval(maxCheckInterval);

                // remember at which iteration the probabilistic approach took over
                if (benchmark != BENCHMARK_OFF) {
                    Integer iteration = probabilisticSwitchMap.get(feed.getId());
                    if (iteration == null) {
                        probabilisticSwitchMap.put(feed.getId(), feed.getChecks());
                    }
                }

            }
        }

    }

    /**
     * Update evaluation maps.
     * 
     * @param feed The feed.
     * @param entries The feed entries.
     * @param pnTarget The percentage of new entries.
     * @param fps The feed post statistics.
     */
    private void updateEvaluationMaps(Feed feed, double pnTarget, FeedPostStatistics fps) {

        List<FeedEntry> entries = feed.getEntries();

        if (benchmark == BENCHMARK_MAX_CHECK_TIME) {

            // update map with percentage new scores
            String pns = pnMapEvaluation.get(feed.getId());
            if (pns == null) {
                pns = String.valueOf(pnTarget);
            } else {
                pns += ";" + pnTarget;
            }
            pnMapEvaluation.put(feed.getId(), pns);

            // update map with check intervals
            String cis = ciMapEvaluation.get(feed.getId());
            if (cis == null) {
                cis = String.valueOf(feed.getMaxCheckInterval());
            } else {
                cis += ";" + feed.getMaxCheckInterval();
            }
            ciMapEvaluation.put(feed.getId(), cis);

            // update the minutes where an entry was actually posted
            // LinkedHashMap<Integer, Integer> minuteMap = postDistributionMapEvaluation.get(feed.getId());
            // if (minuteMap == null) {
            // minuteMap = new LinkedHashMap<Integer, Integer>();
            // for (int i = 0; i < 1440; i++) {
            // minuteMap.put(i, 0);
            // }
            // }
            // for (FeedEntry entry : entries) {
            // if (entry.getPublished() == null) {
            // continue;
            // }
            // int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
            // int posts = minuteMap.get(minuteOfDay);
            // posts = posts + 1;
            // minuteMap.put(minuteOfDay, posts);
            // }
            // postDistributionMapEvaluation.put(feed.getId(), minuteMap);

        } else if (benchmark == BENCHMARK_MIN_CHECK_TIME) {

            // if no entry was new we add the check time to the check time map
            // in order to calculate the difference between this date and the
            // real pub
            // date of the post
            if (pnTarget == 0) {
                HashSet<Date> dateSet = tempCheckTimeMapEvaluation.get(feed.getId());
                if (dateSet == null) {
                    dateSet = new HashSet<Date>();
                }
                dateSet.add(new Date());
                tempCheckTimeMapEvaluation.put(feed.getId(), dateSet);

                // if a new entry was found, add the positive time difference
                // and all the saved attempts to retrieve a new entry
            } else {
                ArrayList<Integer> timeDiffList = timeDiffMapEvaluation.get(feed.getId());
                if (timeDiffList == null) {
                    timeDiffList = new ArrayList<Integer>();
                }
                int positiveTimeDifference = (int) (fps.getTimeDifferenceToNewestPost() / DateHelper.MINUTE_MS);
                timeDiffList.add(positiveTimeDifference);

                // int sumNegativeTimeDifferences = 0;
                for (Date d : tempCheckTimeMapEvaluation.get(feed.getId())) {
                    // sumNegativeTimeDifferences +=
                    timeDiffList.add((int) ((fps.getTimeNewestPost() - d.getTime()) / DateHelper.MINUTE_MS));
                }
                timeDiffMapEvaluation.put(feed.getId(), timeDiffList);
            }
        }
    }

    /**
     * Save the feed ids and the percentage new scores in a csv for evaluation.
     */
    private void writeRecordedMaps() {
        StringBuilder csv = new StringBuilder();
        int totalChecks = 0;
        for (Entry<Integer, String> entry : pnMapEvaluation.entrySet()) {
            Feed feed = FeedDatabase.getInstance().getFeedByID(entry.getKey());

            // the feed id
            csv.append(entry.getKey()).append(";");

            // the feed url
            csv.append(feed.getFeedUrl()).append(";");

            // the feed class
            csv.append(feed.getUpdateClass()).append(";");

            // which iteration the probabilistic approach took over
            Integer iteration = probabilisticSwitchMap.get(entry.getKey());
            if (iteration == null) {
                iteration = -1;
            }
            csv.append(iteration).append(";");

            // the pn data
            csv.append(entry.getValue()).append("\n");

            // count how many time the feed has been checked
            totalChecks += entry.getValue().split(";").length;
        }

        // total number of megabytes dowloaded
        csv.append("total downloaded MB:;").append(Crawler.getSessionDownloadSize(Crawler.MEGA_BYTES)).append(" MB")
                .append("\n");

        // total number of checks
        csv.append("total checks:;").append(totalChecks).append("\n");
        FileHelper.writeToFile("data/temp/feedCheckerPnMap_Evaluation_" + getCheckApproachName() + "_"
                + System.currentTimeMillis() + ".csv", csv);

        csv = new StringBuilder();
        for (Entry<Integer, String> entry : ciMapEvaluation.entrySet()) {
            csv.append(entry.getKey()).append(";").append(entry.getValue()).append("\n");
        }
        FileHelper.writeToFile("data/temp/feedCheckerCiMap_" + getCheckApproachName() + "_"
                + System.currentTimeMillis() + ".csv", csv);

        csv = new StringBuilder();
        for (Entry<Integer, LinkedHashMap<Integer, Integer>> entry : postDistributionMapEvaluation.entrySet()) {
            csv.append(entry.getKey());
            for (Entry<Integer, Integer> minuteEntry : entry.getValue().entrySet()) {
                csv.append(";").append(minuteEntry.getValue());
            }
            csv.append("\n");
        }
        FileHelper.writeToFile("data/temp/feedCheckerPdMap_" + getCheckApproachName() + "_"
                + System.currentTimeMillis() + ".csv", csv);
    }

    public void setFeedProcessingAction(FeedProcessingAction feedProcessingAction) {
        this.feedProcessingAction = feedProcessingAction;
    }

    public FeedProcessingAction getFeedProcessingAction() {
        return feedProcessingAction;
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
    public void setCheckApproach(int checkApproach, boolean resetLearnedValues) {
        if (this.checkApproach != checkApproach && resetLearnedValues) {
            FeedDatabase.getInstance().changeCheckApproach();
        }
        this.checkApproach = checkApproach;
    }

    public int getCheckApproach() {
        return checkApproach;
    }

    /** Get the human readable name of the chosen check approach. */
    public String getCheckApproachName() {

        String className = "unknown";

        if (checkApproach == CHECK_FIXED && checkInterval == -1) {
            className = "fixed_learned";
        } else if (checkApproach == CHECK_FIXED && checkInterval != -1) {
            className = "fixed_" + checkInterval;
        } else if (checkApproach == CHECK_ADAPTIVE) {
            className = "adaptive";
        } else if (checkApproach == CHECK_PROBABILISTIC) {
            className = "probabilistic";
        }

        return className;
    }

    /**
     * Set a fixed check interval in minutes. This is only effective if the checkType is set to {@link CHECK_FIXED}.
     * 
     * @param checkInterval Fixed check interval in minutes.
     */
    public void setCheckInterval(int checkInterval) {
        this.checkInterval = checkInterval;
    }

    public int getCheckInterval() {
        return checkInterval;
    }

    /**
     * Sample usage. Command line: parameters: checkType(1-3) runtime(in minutes) checkInterval(only if checkType=1),
     * example: 1 10 2 TODO: simplify command
     * line use: http://commons.apache.org/cli/usage.html
     */
    public static void main(String[] args) {

        // FeedAggregator fa = new FeedAggregator();
        // fa.setUseScraping(false);
        // try {
        // Feed f = fa.getFeed("http://www.usa.gov/rss/updates.xml");
        // List<FeedEntry> entries = fa.getEntries(f.getFeedUrl());
        // for (FeedEntry e : entries) {
        // System.out.println("p: " + e.getAddedSQLTimestamp());
        // }
        // System.out.println(f);
        // } catch (FeedAggregatorException e) {
        // e.printStackTrace();
        //
        // }
        // System.exit(0);

        if (args.length > 0) {

            int checkType = Integer.valueOf(args[0]);
            int runtime = Integer.valueOf(args[1]);
            int checkInterval = -1;
            if (args.length > 2) {
                checkInterval = Integer.valueOf(args[2]);
            }

            FeedChecker fc = FeedChecker.getInstance();
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

        } else {

            FeedChecker fc = FeedChecker.getInstance();
            FeedProcessingAction fpa = new FeedProcessingAction() {

                @Override
                public void performAction(Feed feed) {
                    System.out.println("do stuff with " + feed.getFeedUrl());
                    System.out.println("::: check interval: " + feed.getMaxCheckInterval() + ", checks: "
                            + feed.getChecks());

                }
            };
            fc.setCheckApproach(FeedChecker.CHECK_PROBABILISTIC, true);
            fc.setCheckInterval(1);
            fc.setFeedProcessingAction(fpa);
            // fc.startContinuousReading(2 * DateHelper.HOUR_MS);
            fc.startContinuousReading(20 * DateHelper.MINUTE_MS);

        }
    }
}