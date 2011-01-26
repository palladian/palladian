package tud.iir.web.feeds.evaluation;


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
    private int benchmarkType = FeedReaderEvaluator.BENCHMARK_OFF;

    /** The time of the poll. */
    private long timestamp = -1l;

    /** Number of new items in the window. */
    private int newWindowItems = -1;

    /** The cumulated delay from all early and late lookups. */
    private long cumulatedDelay = -1;

    /** The cumulated late delays for the current poll. */
    private long cumulatedLateDelay = -1;

    /**
     * The averaged timeliness for the poll if at least one new item has been found. This adds up all delays from early
     * polls and late polls.
     */
    private Double timeliness = null;

    /**
     * The averaged timeliness for the poll if at least one new item has been found. This adds up all delays from late
     * polls.
     */
    private Double timelinessLate = null;

    /**
     * The factor by which delays AFTER the actual new post publish date are weighted more than delays BEFORE this time.
     */
    private double afterDelayWeight = 1.0;

    /** Number of posts that have been missed (not read early enough). */
    private int misses = 0;

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

    public void setCumulatedDelay(long cumulatedDelay) {
        this.cumulatedDelay = cumulatedDelay;
    }

    public long getCumulatedDelay() {
        return cumulatedDelay;
    }

    public void setCumulatedLateDelay(long cumulatedLateDelay) {
        this.cumulatedLateDelay = cumulatedLateDelay;
    }

    public long getCumulatedLateDelay() {
        return cumulatedLateDelay;
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

    public int getNewWindowItems() {
        return newWindowItems;
    }

    public void setNewWindowItems(int newWindowItems) {
        this.newWindowItems = newWindowItems;
    }

    public Double getTimeliness() {
        return timeliness;
    }

    public void setTimeliness(Double timeliness) {
        this.timeliness = timeliness;
    }

    public Double getTimelinessLate() {
        return timelinessLate;
    }

    public void setTimelinessLate(Double timelinessLate) {
        this.timelinessLate = timelinessLate;
    }

    /**
     * Get the score of the poll. The score is either the weighted sum of delays if benchmark is set to
     * {@link FeedChecker.BENCHMARK_MIN_CHECK} (low is good) or a percentage if benchmark is set to
     * {@link FeedChecker.BENCHMARK_MAX_CHECK} (high is good).
     * 
     * @return
     */
    /*
     * public Double getScore(int benchmark) {
     * Double score = null;
     * if (benchmark == FeedReaderEvaluator.BENCHMARK_MIN_DELAY) {
     * if (getSurroundingIntervalsLength() != null && getSurroundingIntervalsLength() > 0) {
     * score = 1.0 / Math
     * .sqrt((Math.abs(getCumulatedDelay()) / (double) getSurroundingIntervalsLength() + 1.0));
     * // score = 1.0 - Math.abs(getNewPostDelay()) / (double) getCurrentIntervalSize();
     * }
     * } else if (benchmark == FeedReaderEvaluator.BENCHMARK_MAX_COVERAGE) {
     * score = 1.0 / Math.sqrt(getMisses() + 1);
     * // if (getMisses() == 0) {
     * // // get the percentage of new entries new/total, since percentNew is the number of new/(total-1) we need
     * // to calculate back
     * // score = getPercentNew() * (getWindowSize() - 1) / getWindowSize();
     * // } else if (getMisses() > 0) {
     * // if (getWindowSize() > 0) {
     * // score = 1.0 - getMisses() / (double)getWindowSize();
     * // } else {
     * // Logger.getRootLogger().warn("window size = 0 for feed");
     * // }
     * // }
     * }
     * return score;
     * }
     */

}
