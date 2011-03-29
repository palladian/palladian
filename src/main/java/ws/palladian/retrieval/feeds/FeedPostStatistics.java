package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

/**
 * Capture some statistics about the posts of a feed.
 * 
 * @author David Urbansky
 * 
 */
public class FeedPostStatistics {

    /**
     * Record a list of checkInterval values for each feed: <minuteOfDay : number of posts in that minute.
     */
    private Map<Integer, Integer> postDistribution = new LinkedHashMap<Integer, Integer>();

    /** The delay to the newest post. */
    private long delayToNewestItem = -1;

    /** The interval between the last two items. */
    private long lastInterval = -1;

    /** The timestamp of the oldest post. */
    private long timeOldestItem = -1;

    /** The timestamp of the most recent post. */
    private long timeNewestItem = -1;

    /** The median time gap between subsequent posts. */
    private long medianPostInterval = -1;

    /** The median time gap between subsequent posts. */
    private long medianPostInterval2 = -1;

    /** The average time gap between subsequent posts. */
    private double averagePostInterval = -1;

    /** Intervals. */
    private List<Long> intervals = new ArrayList<Long>();

    /** The standard deviation from the average post gap. */
    private long postIntervalStandardDeviation = -1;

    /** The longest gap between two subsequent posts. */
    private long longestPostInterval = -1;

    /** The average number of entries per day. */
    private double avgItemsPerDay = -1;

    /** Whether or not the statistics are valid, that is, pub dates must have been found and parsed correctly. */
    private boolean validStatistics = false;

    public FeedPostStatistics(Feed feed) {
        calculateStatistics(feed);
    }

    private final void calculateStatistics(Feed feed) {

        List<FeedItem> feedEntries = feed.getItems();

        long timeOldestEntry = Long.MAX_VALUE;
        long timeNewestEntry = 0;
        long timeSecondNewestEntry = 0;

        // keep a list of times to find out the median of the time differences between posts, average is not good since one very old post can bias the value
        List<Long> timeList = new ArrayList<Long>();
        List<Long> timeList2 = new ArrayList<Long>();

        if (feedEntries == null) {
            return;
        }

        int c = 0;
        for (FeedItem entry : feedEntries) {
            c++;

            if (c <= feedEntries.size() - 5) {
                // continue;
            }

            Date pubDate = entry.getPublished();
            if (pubDate == null) {
                FeedReader.LOGGER.warn("entry does not have pub date, feed entry " + entry);
                continue;
            }
            long pubTime = pubDate.getTime();
            if (pubTime > timeNewestEntry) {
                timeSecondNewestEntry = timeNewestEntry;
                timeNewestEntry = pubTime;
            }
            if (pubTime > timeSecondNewestEntry && pubTime < timeNewestEntry) {
                timeSecondNewestEntry = pubTime;
            }
            if (pubTime < timeOldestEntry) {
                timeOldestEntry = pubTime;
            }
            timeList.add(pubTime);

            if (timeList.size() > 1) {
                timeList2.add(pubTime);
            }

        }

        if (FeedReaderEvaluator.getBenchmarkPolicy() != FeedReaderEvaluator.BENCHMARK_OFF) {
            timeList2.add(feed.getBenchmarkLookupTime());
        } else {
            timeList2.add(System.currentTimeMillis());
        }

        // fill list with interval
        intervals = new ArrayList<Long>();
        Collections.sort(timeList);
        Collections.sort(timeList2);

        for (int i = 0; i < timeList.size() - 1; i++) {
            intervals.add(timeList.get(i + 1) - timeList.get(i));
        }

        // in case no pub date was found correctly, we set the newest entry time to now so we know next time which entries are newer
        if (timeNewestEntry == 0) {
            timeNewestEntry = System.currentTimeMillis();
        }
        // in case no pub date was found correctly, we set the oldest entry time one week in the past
        if (timeOldestEntry == Long.MAX_VALUE) {
            timeOldestEntry = System.currentTimeMillis() - DateHelper.WEEK_MS;
        }

        // in benchmark mode we simulate the lookup time, otherwise it's the current time
        if (FeedReaderEvaluator.getBenchmarkPolicy() != FeedReaderEvaluator.BENCHMARK_OFF) {
            setDelayToNewestPost(feed.getBenchmarkLookupTime() - timeNewestEntry);
        } else {
            setDelayToNewestPost(System.currentTimeMillis() - timeNewestEntry);
        }

        setLastInterval(timeNewestEntry - timeSecondNewestEntry);
        setTimeNewestPost(timeNewestEntry);
        setTimeOldestPost(timeOldestEntry);

        if (timeList.size() > 1) {
            setMedianPostGap(MathHelper.getMedianDifference(timeList));
            setMedianPostGap2(MathHelper.getMedianDifference(timeList2));
            setAveragePostGap(getTimeRange() / ((double) feedEntries.size() - 1));
            setPostGapStandardDeviation(MathHelper.getStandardDeviation(timeList));
            setLongestPostGap(MathHelper.getLongestGap(CollectionHelper.toTreeSet(timeList)));
            setValidStatistics(true);
        }

        double avgEntriesPerDay = -1;
        avgEntriesPerDay = (double) feedEntries.size() / (double) getTimeRangeInDays();
        setAvgEntriesPerDay(avgEntriesPerDay);
    }

    public void setDelayToNewestPost(long delayToNewestPost) {
        this.delayToNewestItem = delayToNewestPost;
    }

    public long getDelayToNewestPost() {
        return delayToNewestItem;
    }

    public void setLastInterval(long lastInterval) {
        this.lastInterval = lastInterval;
    }

    public long getLastInterval() {
        return lastInterval;
    }

    public long getTimeRange() {
        return timeNewestItem - timeOldestItem;
    }

    public int getTimeRangeInDays() {
        return Math.max(1, (int) (getTimeRange() / DateHelper.DAY_MS));
    }

    public long getTimeDifferenceToNewestPost() {
        return System.currentTimeMillis() - timeNewestItem;
    }

    public Map<Integer, Integer> getPostDistribution() {
        return postDistribution;
    }

    public void setPostDistribution(final Map<Integer, Integer> postDistribution) {
        this.postDistribution = postDistribution;
    }

    public long getTimeOldestPost() {
        return timeOldestItem;
    }

    private void setTimeOldestPost(final long timeOldestPost) {
        this.timeOldestItem = timeOldestPost;
    }

    public long getTimeNewestPost() {
        return timeNewestItem;
    }

    private void setTimeNewestPost(final long timeNewestPost) {
        this.timeNewestItem = timeNewestPost;
    }

    public long getMedianPostGap() {
        return medianPostInterval;
    }

    private void setMedianPostGap(final long medianPostGap) {
        this.medianPostInterval = medianPostGap;
    }

    public long getMedianPostGap2() {
        return medianPostInterval2;
    }

    private void setMedianPostGap2(final long medianPostGap2) {
        this.medianPostInterval2 = medianPostGap2;
    }

    public double getAveragePostGap() {
        return averagePostInterval;
    }

    public void setAveragePostGap(double averagePostGap) {
        this.averagePostInterval = averagePostGap;
    }

    public List<Long> getIntervals() {
        return intervals;
    }

    public void setIntervals(List<Long> intervals) {
        this.intervals = intervals;
    }

    private void setPostGapStandardDeviation(final long postGapStandardDeviation) {
        this.postIntervalStandardDeviation = postGapStandardDeviation;
    }

    public long getPostGapStandardDeviation() {
        return postIntervalStandardDeviation;
    }

    private void setLongestPostGap(long longestPostGap) {
        this.longestPostInterval = longestPostGap;
    }

    public long getLongestPostGap() {
        return longestPostInterval;
    }

    private void setAvgEntriesPerDay(double avgEntriesPerDay) {
        this.avgItemsPerDay = avgEntriesPerDay;
    }

    public double getAvgEntriesPerDay() {
        return avgItemsPerDay;
    }

    private void setValidStatistics(boolean validStatistics) {
        this.validStatistics = validStatistics;
    }

    public boolean isValidStatistics() {
        return validStatistics;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FeedPostStatistics [longestPostGap=");
        builder.append((double) longestPostInterval / DateHelper.MINUTE_MS);
        builder.append("min. , medianPostGap=");
        builder.append((double) medianPostInterval / DateHelper.MINUTE_MS);
        builder.append("min. , time to newest post=");
        builder.append((double) getTimeDifferenceToNewestPost() / DateHelper.MINUTE_MS);
        builder.append("min. , postDistribution=");
        builder.append(postDistribution);
        builder.append(", postGapStandardDeviation=");
        builder.append(postIntervalStandardDeviation);
        builder.append(", timeNewestPost=");
        builder.append(timeNewestItem);
        builder.append(", timeOldestPost=");
        builder.append(timeOldestItem);
        builder.append("]");
        return builder.toString();
    }

}