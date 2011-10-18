package ws.palladian.retrieval.feeds.evaluation;

import java.util.List;

/**
 * <p>
 * This class holds meta information about a poll of an news feed.
 * </p>
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 */
public class PollData {

    /**
     * <p>The type of benchmark where this poll data belongs to. The check interval for example is either min or max check
     * interval time depending on the benchmark type.</p>
     */
    private int benchmarkType = FeedReaderEvaluator.BENCHMARK_OFF;

    /** The time of the poll. */
    private long pollTimestamp = -1l;

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

    /**
     * A list containing the delays to all items.
     */
    private List<Long> itemDelays = null;

    /**
     * <p>
     * The type of benchmark where this poll data belongs to. The check interval for example is either min or max check
     * interval time depending on the benchmark type. Type may be FeedReaderEvaluator.BENCHMARK_OFF;
     * </p>
     * 
     * @param benchmarkType
     */
    public void setBenchmarkType(int benchmarkType) {
        this.benchmarkType = benchmarkType;
    }

    /**
     * <p>
     * The type of benchmark where this poll data belongs to. The check interval for example is either min or max check
     * interval time depending on the benchmark type. Type may be FeedReaderEvaluator.BENCHMARK_OFF;
     * </p>
     * 
     * @return
     */
    public int getBenchmarkType() {
        return benchmarkType;
    }

    /**
     * @param pollTimestamp The time of the poll.
     */
    public void setPollTimestamp(long pollTimestamp) {
        this.pollTimestamp = pollTimestamp;
    }

    /**
     * @return The time of the poll.
     */
    public long getPollTimestamp() {
        return pollTimestamp;
    }

    /**
     * @param cumulatedDelay The cumulated delay from all early and late lookups.
     */
    public void setCumulatedDelay(long cumulatedDelay) {
        this.cumulatedDelay = cumulatedDelay;
    }

    /**
     * @return The cumulated delay from all early and late lookups.
     */
    public long getCumulatedDelay() {
        return cumulatedDelay;
    }

    /**
     * @param cumulatedLateDelay The cumulated late delays for the current poll.
     */
    public void setCumulatedLateDelay(long cumulatedLateDelay) {
        this.cumulatedLateDelay = cumulatedLateDelay;
    }

    /**
     * @return The cumulated late delays for the current poll.
     */
    public long getCumulatedLateDelay() {
        return cumulatedLateDelay;
    }

    /**
     * @param afterDelayWeight The factor by which delays AFTER the actual new post publish date are weighted more than
     *            delays BEFORE this time.
     */
    public void setAfterDelayWeight(double afterDelayWeight) {
        this.afterDelayWeight = afterDelayWeight;
    }

    /**
     * @return The factor by which delays AFTER the actual new post publish date are weighted more than delays BEFORE
     *         this time.
     */
    public double getAfterDelayWeight() {
        return afterDelayWeight;
    }

    /**
     * @return Number of posts that have been missed (not read early enough).
     */
    public int getMisses() {
        return misses;
    }

    /**
     * @param misses Number of posts that have been missed (not read early enough).
     */
    public void setMisses(int misses) {
        this.misses = misses;
    }

    /**
     * @return The check interval when the feed was polled.
     */
    public double getCheckInterval() {
        return checkInterval;
    }

    /**
     * @param checkInterval The check interval when the feed was polled.
     */
    public void setCheckInterval(double checkInterval) {
        this.checkInterval = checkInterval;
    }

    /**
     * @return The window size at time of the poll.
     */
    public int getWindowSize() {
        return windowSize;
    }

    /**
     * @param windowSize The window size at time of the poll.
     */
    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * @return The total amount of data that was downloaded with the request.
     */
    public double getDownloadSize() {
        return downloadSize;
    }

    /**
     * @param downloadSize The total amount of data that was downloaded with the request.
     */
    public void setDownloadSize(double downloadSize) {
        this.downloadSize = downloadSize;
    }

    /**
     * @return Number of new items in the window.
     */
    public int getNewWindowItems() {
        return newWindowItems;
    }

    /**
     * @param newWindowItems Number of new items in the window.
     */
    public void setNewWindowItems(int newWindowItems) {
        this.newWindowItems = newWindowItems;
    }

    /**
     * @return The averaged timeliness for the poll if at least one new item has been found. This adds up all delays
     *         from early polls and late polls.
     */
    public Double getTimeliness() {
        return timeliness;
    }

    /**
     * @param timeliness The averaged timeliness for the poll if at least one new item has been found. This adds up all
     *            delays from early polls and late polls.
     */
    public void setTimeliness(Double timeliness) {
        this.timeliness = timeliness;
    }

    /**
     * @return The averaged timeliness for the poll if at least one new item has been found. This adds up all delays
     *         from late polls.
     */
    public Double getTimelinessLate() {
        return timelinessLate;
    }

    /**
     * @param timelinessLate The averaged timeliness for the poll if at least one new item has been found. This adds up
     *            all delays from late polls.
     */
    public void setTimelinessLate(Double timelinessLate) {
        this.timelinessLate = timelinessLate;
    }

    /**
     * @return A list containing the delays to all items.
     */
    public final List<Long> getItemDelays() {
        return itemDelays;
    }

    /**
     * @param itemDelays A list containing the delays to all items.
     */
    public final void setItemDelays(List<Long> itemDelays) {
        this.itemDelays = itemDelays;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PollData [benchmarkType=");
        builder.append(benchmarkType);
        builder.append(", pollTimestamp=");
        builder.append(pollTimestamp);
        builder.append(", newWindowItems=");
        builder.append(newWindowItems);
        builder.append(", cumulatedDelay=");
        builder.append(cumulatedDelay);
        builder.append(", cumulatedLateDelay=");
        builder.append(cumulatedLateDelay);
        builder.append(", timeliness=");
        builder.append(timeliness);
        builder.append(", timelinessLate=");
        builder.append(timelinessLate);
        builder.append(", afterDelayWeight=");
        builder.append(afterDelayWeight);
        builder.append(", misses=");
        builder.append(misses);
        builder.append(", checkInterval=");
        builder.append(checkInterval);
        builder.append(", windowSize=");
        builder.append(windowSize);
        builder.append(", downloadSize=");
        builder.append(downloadSize);
        builder.append(", itemDelays=");
        builder.append(itemDelays);
        builder.append("]");
        return builder.toString();
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
