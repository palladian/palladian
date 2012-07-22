package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.ArrayUtils;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

/**
 * <p>
 * Capture some statistics about the posts of a feed. When reading the statistics, make sure
 * {@link #isValidStatistics()} returns <code>true</code>!
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
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

//    /** The median time gap between subsequent posts. */
//    private long medianPostInterval2 = -1;

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

    /** The timestamp of the feed's most recent poll. */
    private Long lastPollTime = null;

    /** The httpDate from the header of the feed's most recent poll. */
    private Long lastPollHttpDate = null;

    public FeedPostStatistics(Feed feed) {
        if (feed.getLastPollTime() != null) {
            lastPollTime = feed.getLastPollTime().getTime();
        }
        if (feed.getHttpDateLastPoll() != null) {
            lastPollHttpDate = feed.getHttpDateLastPoll().getTime();
        }
        calculateStatistics(feed);
    }

    private final void calculateStatistics(Feed feed) {

        Collection<Date> feedPubdates = feed.getCorrectedItemTimestamps();

        long timeOldestEntry = Long.MAX_VALUE;
        long timeNewestEntry = 0;
        long timeSecondNewestEntry = 0;

        // keep a list of times to find out the median of the time differences between posts, average is not good since
        // one very old post can bias the value
        List<Long> timeList = new ArrayList<Long>();
//        List<Long> timeList2 = new ArrayList<Long>();

        if (feedPubdates == null) {
            return;
        }

        StringBuilder warnings = new StringBuilder();
        int c = 0;
        for (Date correctedPubDate : feedPubdates) {
            c++;

            if (c <= feedPubdates.size() - 5) {
                // continue;
            }

            long pubTime = correctedPubDate.getTime();
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

//            if (timeList.size() > 1) {
//                timeList2.add(pubTime);
//            }
        }

//        if (FeedReaderEvaluator.getBenchmarkPolicy() != FeedReaderEvaluator.BENCHMARK_OFF) {
//            timeList2.add(feed.getBenchmarkLookupTime());
//        } else {
//            timeList2.add(System.currentTimeMillis());
//        }

        // fill list with interval
//        intervals = new ArrayList<Long>();
        Collections.sort(timeList);
//        Collections.sort(timeList2);
        CollectionHelper.removeNulls(timeList);

//        for (int i = 0; i < timeList.size() - 1; i++) {
//            intervals.add(timeList.get(i + 1) - timeList.get(i));
//        }
        long[] timeArray = ArrayUtils.toPrimitive(timeList.toArray(new Long[0]));
        intervals = Arrays.asList(ArrayUtils.toObject(MathHelper.getDistances(timeArray)));

        // FIXME: do we really need to set these fake values? In case the feed has an empty window, we set two fake
        // timestamps and calculate some statistics that are not valid. I think this code is very old. In the past, we
        // ignored empty and single item feeds. -- Sandro 15.07.2011
        if (timeList.size() > 0) {
            // in case no pub date was found correctly, we set the newest entry time to now so we know next time which
            // entries are newer
            if (timeNewestEntry == 0) {
                timeNewestEntry = System.currentTimeMillis();
                warnings.append("\nDid not find a valid timestamp, setting timeNewestEntry to current timestamp. Feed id: "
                        + feed.getId());
            }
            // in case no pub date was found correctly, we set the oldest entry time one week in the past
            if (timeOldestEntry == Long.MAX_VALUE) {
                timeOldestEntry = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
                warnings.append("\nDid not find a valid timestamp, setting timeOldestEntry to current timestamp - one week. Feed id: "
                        + feed.getId());
            }

            if (warnings.length() > 0) {
                FeedReader.LOGGER.warn(warnings);
            }

            // in benchmark mode we simulate the lookup time, otherwise it's the current time
            if (FeedReaderEvaluator.getBenchmarkPolicy() != FeedReaderEvaluator.BENCHMARK_OFF) {
                setDelayToNewestPost(feed.getBenchmarkLookupTime() - timeNewestEntry);
            } else {
                long pollTime = 0;
                if (feed.getLastPollTime() != null) {
                    pollTime = feed.getLastPollTime().getTime();
                } else {
                    pollTime = System.currentTimeMillis();
                }
                setDelayToNewestPost(pollTime - timeNewestEntry);
            }

            setLastInterval(timeNewestEntry - timeSecondNewestEntry);
            setTimeNewestPost(timeNewestEntry);
            setTimeOldestPost(timeOldestEntry);

            if (timeList.size() > 1) {
                setMedianPostGap(MathHelper.getMedianDifference(timeArray));
//                setMedianPostGap2(MathHelper.getMedianDifference(timeList2));
                setAveragePostGap(getTimeRange() / ((double)feedPubdates.size() - 1));
                setPostGapStandardDeviation((long)MathHelper.getStandardDeviation(timeArray));
//                setLongestPostGap(MathHelper.getLongestGap(new TreeSet<Long>(timeList)));
                setLongestPostGap(MathHelper.getLongestGap(timeArray));
                setValidStatistics(true);
            }
        }

        double avgEntriesPerDay = -1;
        avgEntriesPerDay = (double)feedPubdates.size() / (double)getTimeRangeInDays();
        setAvgEntriesPerDay(avgEntriesPerDay);
    }

    private void setDelayToNewestPost(long delayToNewestPost) {
        this.delayToNewestItem = delayToNewestPost;
    }

    public long getDelayToNewestPost() {
        return delayToNewestItem;
    }

    private void setLastInterval(long lastInterval) {
        this.lastInterval = lastInterval;
    }

    public long getLastInterval() {
        return lastInterval;
    }

    private long getTimeRange() {
        return timeNewestItem - timeOldestItem;
    }

    private int getTimeRangeInDays() {
        return Math.max(1, (int)(getTimeRange() / TimeUnit.DAYS.toMillis(1)));
    }

    /**
     * The difference between the publish date of the newest item and the current system time. Be careful when
     * processing persisted data since you may want to get the time between the publish date of the newest item and the
     * time the feed has been polled the last time.
     * 
     * @return The difference between the publish date of the newest item and the current system time.
     * @see #getTimeDifferenceNewestPostToLastPollTime()
     */
    public long getTimeDifferenceNewestPostToCurrentTime() {
        return System.currentTimeMillis() - timeNewestItem;
    }

    /**
     * The difference between the publish date of the newest item and the time of the last poll. This is safe when
     * processing persisted data. In case the http date is unknown or was not set by the server,
     * {@link #getTimeDifferenceNewestPostToCurrentTime()} is returned for convenience.
     * 
     * @return The difference between the publish date of the newest item and the time of the last poll or the value
     *         returned by {@link #getTimeDifferenceNewestPostToCurrentTime()}.
     */
    public long getTimeDifferenceNewestPostToLastPollTime() {
        if (lastPollTime == null) {
            return getTimeDifferenceNewestPostToCurrentTime();
        } else {
            return lastPollTime - timeNewestItem;
        }
    }

    /**
     * The difference between the publish date of the newest item and the http header's date value of the last poll.
     * This is safe when processing persisted data. In case the http date is unknown or was not set by the server,
     * {@link #getTimeDifferenceNewestPostToLastPollTime()} is returned for convenience.
     * 
     * @return The difference between the publish date of the newest item and the time of the last poll or the value
     *         returned by {@link #getTimeDifferenceNewestPostToLastPollTime()}.
     */
    public long getTimeDifferenceNewestPostToLastPollHttpDate() {
        if (lastPollHttpDate == null) {
            return getTimeDifferenceNewestPostToLastPollTime();
        } else {
            return lastPollHttpDate - timeNewestItem;
        }
    }

//    private Map<Integer, Integer> getPostDistribution() {
//        return postDistribution;
//    }

//    private void setPostDistribution(final Map<Integer, Integer> postDistribution) {
//        this.postDistribution = postDistribution;
//    }

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

//    private long getMedianPostGap2() {
//        return medianPostInterval2;
//    }

//    private void setMedianPostGap2(final long medianPostGap2) {
//        this.medianPostInterval2 = medianPostGap2;
//    }

    public double getAveragePostGap() {
        return averagePostInterval;
    }

    /**
     * @param averagePostGap arithmet. average
     */
    private void setAveragePostGap(double averagePostGap) {
        this.averagePostInterval = averagePostGap;
    }

    public List<Long> getIntervals() {
        return intervals;
    }

//    private void setIntervals(List<Long> intervals) {
//        this.intervals = intervals;
//    }

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
        builder.append((double)longestPostInterval / TimeUnit.MINUTES.toMillis(1));
        builder.append("min. , medianPostGap=");
        builder.append((double)medianPostInterval / TimeUnit.MINUTES.toMillis(1));
        builder.append("min. , time to newest post=");
        builder.append((double)getTimeDifferenceNewestPostToCurrentTime() / TimeUnit.MINUTES.toMillis(1));
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