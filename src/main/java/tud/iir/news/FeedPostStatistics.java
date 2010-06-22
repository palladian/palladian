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
     * record a list of checkInterval values for each feed: <minuteOfDay : number of posts in that minute
     */
    private Map<Integer, Integer> postDistribution = new LinkedHashMap<Integer, Integer>();

    /** the timestamp of the oldest post */
    private long timeOldestPost = -1;

    /** the timestamp of the most recent post */
    private long timeNewestPost = -1;

    /** the median time gap between subsequent posts */
    private long medianPostGap = -1;

    /** the standard deviation from the average post gap */
    private long postGapStandardDeviation = -1;

    /** the longest gap between two subsequent posts */
    private long longestPostGap = -1;

    public FeedPostStatistics(final List<FeedEntry> feedEntries) {
        calculateStatistics(feedEntries);
    }

    private final void calculateStatistics(final List<FeedEntry> feedEntries) {

        long timeOldestEntry = Long.MAX_VALUE;
        long timeNewestEntry = 0;

        // keep a list of times to find out the median of the time differences between posts, average is not good since one very old post can bias the value
        final TreeSet<Long> timeList = new TreeSet<Long>();

        for (FeedEntry entry : feedEntries) {
            final Date pubDate = entry.getPublished();
            if (pubDate == null) {
                FeedChecker.LOGGER.warn("entry does not have pub date, feed entry " + entry);
                continue;
            }
            final long pubTime = pubDate.getTime();
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
        }
    }

    public long getTimeRange() {
        return timeNewestPost - timeOldestPost;
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

    public final void setTimeOldestPost(final long timeOldestPost) {
        this.timeOldestPost = timeOldestPost;
    }

    public long getTimeNewestPost() {
        return timeNewestPost;
    }

    public final void setTimeNewestPost(final long timeNewestPost) {
        this.timeNewestPost = timeNewestPost;
    }

    public long getMedianPostGap() {
        return medianPostGap;
    }

    public final void setMedianPostGap(final long medianPostGap) {
        this.medianPostGap = medianPostGap;
    }

    public final void setPostGapStandardDeviation(final long postGapStandardDeviation) {
        this.postGapStandardDeviation = postGapStandardDeviation;
    }

    public long getPostGapStandardDeviation() {
        return postGapStandardDeviation;
    }

    public void setLongestPostGap(long longestPostGap) {
        this.longestPostGap = longestPostGap;
    }

    public long getLongestPostGap() {
        return longestPostGap;
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