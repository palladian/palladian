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

    /** Percentage of posts that are new at poll time. */
    private double percentNew = -1;

    /** The delay to the time of the next new post in milliseconds. */
    private double newPostDelay = -1;

    /**
     * The factor by which delays AFTER the actual new post publish date are weighted more than delays BEFORE this time.
     */
    private double afterDelayWeight = 1.0;

    /** Number of posts that have been missed (not read early enough). */
    private int misses = -1;

    /** The check interval when the feed was polled. */
    private double checkInterval = -1;

    /** The total amount of data that was downloaded with the request. */
    private double downloadSize = 0;

    public void setBenchmarkType(int benchmarkType) {
        this.benchmarkType = benchmarkType;
    }

    public int getBenchmarkType() {
        return benchmarkType;
    }

    public double getPercentNew() {
        return percentNew;
    }

    public void setPercentNew(double percentNew) {
        this.percentNew = percentNew;
    }

    public void setNewPostDelay(double newPostDelay) {
        this.newPostDelay = newPostDelay;
    }

    public double getNewPostDelay() {
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

    public double getDownloadSize() {
        return downloadSize;
    }

    public void setDownloadSize(double downloadSize) {
        this.downloadSize = downloadSize;
    }

}
