package tud.iir.news.statistics;

import tud.iir.news.FeedChecker;

/**
 * This class holds meta information about a poll of an news feed.
 * 
 * @author David Urbansky
 */
public class PollData {

    /**
     * The type of benchmark where this poll data belongs to. The check interval for example is either min or max check
     * interval time depending on the benchmark type.
     */
    private int benchmarkType = FeedChecker.BENCHMARK_OFF;

    /** The time of the poll. */
    private long timestamp = -1l;

    /** Percentage of posts that are new at poll time. */
    private double percentNew = -1;

    /** The delay to the time of the next new post in milliseconds. */
    private long newPostDelay = -1;

    /**
     * The factor by which delays AFTER the actual new post publish date are weighted more than delays BEFORE this time.
     */
    private double afterDelayWeight = 1.0;

    /** Number of posts that have been missed (not read early enough). */
    private int misses = -1;

    /** The check interval when the feed was polled. */
    private double checkInterval = -1;

    /** The window size at time of the poll. */
    private int windowSize = -1;

        /** The total amount of data that was downloaded with the request. */
    private double downloadSize = 0;

    public void setBenchmarkType(int benchmarkType) {
        this.benchmarkType = benchmarkType;
    }

    public int getBenchmarkType() {
        return benchmarkType;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getPercentNew() {
        return percentNew;
    }

    public void setPercentNew(double percentNew) {
        this.percentNew = percentNew;
    }

    public void setNewPostDelay(long newPostDelay) {
        this.newPostDelay = newPostDelay;
    }

    public long getNewPostDelay() {
        return newPostDelay;
    }

    public void setAfterDelayWeight(double afterDelayWeight) {
        this.afterDelayWeight = afterDelayWeight;
    }

    public double getAfterDelayWeight() {
        return afterDelayWeight;
    }

    public int getMisses() {
        return misses;
    }

    public void setMisses(int misses) {
        this.misses = misses;
    }

    public double getCheckInterval() {
        return checkInterval;
    }

    public void setCheckInterval(double checkInterval) {
        this.checkInterval = checkInterval;
    }

    public int getWindowSize() {
        return windowSize;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    public double getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(double downloadSize) {
        this.downloadSize = downloadSize;
    }

    /**
     * Get the score of the poll. The score is either the weighted sum of delays if benchmark is set to
     * {@link FeedChecker.BENCHMARK_MIN_CHECK_TIME} (low is good) or a percentage if benchmark is set to
     * {@link FeedChecker.BENCHMARK_MAX_CHECK_TIME} (high is good).
     * 
     * @return
     */
    public double getScore() {

        double score = -1.0;

        if (getBenchmarkType() == FeedChecker.BENCHMARK_MIN_CHECK_TIME) {

            score = getNewPostDelay();

        } else if (getBenchmarkType() == FeedChecker.BENCHMARK_MAX_CHECK_TIME) {

            if (getPercentNew() <= 1.0) {
                score = getPercentNew();
            } else {
                score = 1 - getMisses() / getWindowSize();
            }

        }

        return score;
    }

}
