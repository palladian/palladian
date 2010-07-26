package tud.iir.news;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import tud.iir.helper.DateHelper;
import tud.iir.helper.MathHelper;

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

    /** The timestamp of the oldest post. */
    private long timeOldestPost = -1;

    /** The timestamp of the most recent post. */
    private long timeNewestPost = -1;

    /** The median time gap between subsequent posts. */
    private long medianPostGap = -1;

    /** The standard deviation from the average post gap. */
    private long postGapStandardDeviation = -1;

    /** The longest gap between two subsequent posts. */
    private long longestPostGap = -1;

    /** The average number of entries per day. */
    private double avgEntriesPerDay = -1;

    /** Whether or not the statistics are valid, that is, pub dates must have been found and parsed correctly. */
    private boolean validStatistics = false;

    public FeedPostStatistics(List<FeedEntry> feedEntries) {
        calculateStatistics(feedEntries);
    }

    private final void calculateStatistics(List<FeedEntry> feedEntries) {

        long timeOldestEntry = Long.MAX_VALUE;
        long timeNewestEntry = 0;

        // keep a list of times to find out the median of the time differences between posts, average is not good since one very old post can bias the value
        TreeSet<Long> timeList = new TreeSet<Long>();

        for (FeedEntry entry : feedEntries) {
            Date pubDate = entry.getPublished();
            if (pubDate == null) {
                FeedChecker.LOGGER.warn("entry does not have pub date, feed entry " + entry);
                continue;
            }
            long pubTime = pubDate.getTime();
            if (pubTime > timeNewestEntry) {
                timeNewestEntry = pubTime;
            }
            if (pubTime < timeOldestEntry) {
                timeOldestEntry = pubTime;
            }
            timeList.add(pubTime);
        }

        // in case no pub date was found correctly, we set the newest entry time to now so we know next time which entries are newer
        if (timeNewestEntry == 0) {
            timeNewestEntry = System.currentTimeMillis();
        }
        // in case no pub date was found correctly, we set the oldest entry time one week in the past
        if (timeOldestEntry == Long.MAX_VALUE) {
            timeOldestEntry = System.currentTimeMillis() - DateHelper.WEEK_MS;
        }

        setTimeNewestPost(timeNewestEntry);
        setTimeOldestPost(timeOldestEntry);

        if (!timeList.isEmpty()) {
            setMedianPostGap(MathHelper.getMedianDifference(timeList));
            setPostGapStandardDeviation(MathHelper.getStandardDeviation(timeList));
            setLongestPostGap(MathHelper.getLongestGap(timeList));
            setValidStatistics(true);
        }

        double avgEntriesPerDay = -1;
        avgEntriesPerDay = (double) feedEntries.size() / (double) getTimeRangeInDays();
        setAvgEntriesPerDay(avgEntriesPerDay);
    }

    public long getTimeRange() {
        return timeNewestPost - timeOldestPost;
    }

    public int getTimeRangeInDays() {
        return Math.max(1, (int) (getTimeRange() / DateHelper.DAY_MS));
    }

    public long getTimeDifferenceToNewestPost() {
        return System.currentTimeMillis() - timeNewestPost;
    }

    public Map<Integer, Integer> getPostDistribution() {
        return postDistribution;
    }

    public void setPostDistribution(final Map<Integer, Integer> postDistribution) {
        this.postDistribution = postDistribution;
    }

    public long getTimeOldestPost() {
        return timeOldestPost;
    }

    private void setTimeOldestPost(final long timeOldestPost) {
        this.timeOldestPost = timeOldestPost;
    }

    public long getTimeNewestPost() {
        return timeNewestPost;
    }

    private void setTimeNewestPost(final long timeNewestPost) {
        this.timeNewestPost = timeNewestPost;
    }

    public long getMedianPostGap() {
        return medianPostGap;
    }

    private void setMedianPostGap(final long medianPostGap) {
        this.medianPostGap = medianPostGap;
    }

    private void setPostGapStandardDeviation(final long postGapStandardDeviation) {
        this.postGapStandardDeviation = postGapStandardDeviation;
    }

    public long getPostGapStandardDeviation() {
        return postGapStandardDeviation;
    }

    private void setLongestPostGap(long longestPostGap) {
        this.longestPostGap = longestPostGap;
    }

    public long getLongestPostGap() {
        return longestPostGap;
    }

    private void setAvgEntriesPerDay(double avgEntriesPerDay) {
        this.avgEntriesPerDay = avgEntriesPerDay;
    }

    public double getAvgEntriesPerDay() {
        return avgEntriesPerDay;
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
        builder.append((double) longestPostGap / DateHelper.MINUTE_MS);
        builder.append("min. , medianPostGap=");
        builder.append((double) medianPostGap / DateHelper.MINUTE_MS);
        builder.append("min. , time to newest post=");
        builder.append((double) getTimeDifferenceToNewestPost() / DateHelper.MINUTE_MS);
        builder.append("min. , postDistribution=");
        builder.append(postDistribution);
        builder.append(", postGapStandardDeviation=");
        builder.append(postGapStandardDeviation);
        builder.append(", timeNewestPost=");
        builder.append(timeNewestPost);
        builder.append(", timeOldestPost=");
        builder.append(timeOldestPost);
        builder.append("]");
        return builder.toString();
    }

}