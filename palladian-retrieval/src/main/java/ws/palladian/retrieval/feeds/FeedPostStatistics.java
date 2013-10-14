package ws.palladian.retrieval.feeds;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.math.Stats;
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

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedPostStatistics.class);

    /** The delay to the newest post. */
    private long delayToNewestItem = -1;

    /** The timestamp of the oldest post. */
    private long timeOldestItem = -1;

    /** The timestamp of the most recent post. */
    private long timeNewestItem = -1;

    /** The median time gap between subsequent posts. */
    private long medianPostInterval = -1;

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

    public FeedPostStatistics(Feed feed) {
        if (feed.getLastPollTime() != null) {
            lastPollTime = feed.getLastPollTime().getTime();
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

        if (feedPubdates == null) {
            return;
        }

        StringBuilder warnings = new StringBuilder();
        for (Date correctedPubDate : feedPubdates) {
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
        }

        Collections.sort(timeList);

        intervals = new ArrayList<Long>();
        for (int i = 1; i < timeList.size(); i++) {
            intervals.add(timeList.get(i) - timeList.get(i - 1));
        }
        Stats timeDistanceStats = new Stats(intervals);

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
                LOGGER.warn(warnings.toString());
            }

            // in benchmark mode we simulate the lookup time, otherwise it's the current time
            if (FeedReaderEvaluator.getBenchmarkPolicy() != FeedReaderEvaluator.BENCHMARK_OFF) {
                delayToNewestItem = feed.getBenchmarkLookupTime() - timeNewestEntry;
            } else {
                long pollTime = 0;
                if (feed.getLastPollTime() != null) {
                    pollTime = feed.getLastPollTime().getTime();
                } else {
                    pollTime = System.currentTimeMillis();
                }
                delayToNewestItem = pollTime - timeNewestEntry;
            }

            timeNewestItem = timeNewestEntry;
            timeOldestItem = timeOldestEntry;

            if (timeList.size() > 1) {
                medianPostInterval = (long)timeDistanceStats.getMedian();
                averagePostInterval = getTimeRange() / ((double)feedPubdates.size() - 1);
                // XXX this was the code before, but this calculates the standard deviation for the timestamps, not the
                // intervals! I changed it, test work fine. -- Philipp, 2013-10-13
                // postIntervalStandardDeviation = (long)MathHelper.getStandardDeviation(timeArray);
                postIntervalStandardDeviation = (long)timeDistanceStats.getStandardDeviation();
                longestPostInterval = (long)timeDistanceStats.getMax();
                validStatistics = true;
            }
        }

        avgItemsPerDay = (double)feedPubdates.size() / (double)getTimeRangeInDays();
    }

    public long getDelayToNewestPost() {
        return delayToNewestItem;
    }

    long getTimeRange() {
        return timeNewestItem - timeOldestItem;
    }

    int getTimeRangeInDays() {
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
    private long getTimeDifferenceNewestPostToCurrentTime() {
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

    public long getTimeOldestPost() {
        return timeOldestItem;
    }

    public long getTimeNewestPost() {
        return timeNewestItem;
    }

    public long getMedianPostGap() {
        return medianPostInterval;
    }

    public double getAveragePostGap() {
        return averagePostInterval;
    }

    public List<Long> getIntervals() {
        return intervals;
    }

    public long getPostGapStandardDeviation() {
        return postIntervalStandardDeviation;
    }

    public long getLongestPostGap() {
        return longestPostInterval;
    }

    public double getAvgEntriesPerDay() {
        return avgItemsPerDay;
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
        builder.append("min. , postGapStandardDeviation=");
        builder.append(postIntervalStandardDeviation);
        builder.append(", timeNewestPost=");
        builder.append(timeNewestItem);
        builder.append(", timeOldestPost=");
        builder.append(timeOldestItem);
        builder.append("]");
        return builder.toString();
    }

}