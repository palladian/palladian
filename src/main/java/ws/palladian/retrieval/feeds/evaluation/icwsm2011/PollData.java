package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.sql.Timestamp;
import java.util.List;

import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;

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

    /** All polls have a sequential number starting from 1 at the first poll. */
    private Integer numberOfPoll = -1;

    /**
     * All polls that have at least one new item ha a sequential number starting from 1 at the first poll that contains
     * items.
     */
    private Integer numberOfPollWithNewItem = null;

    /** The time of the poll. */
    private long pollTimestamp = -1l;

    /** Number of new items in the window. */
    private int newWindowItems = -1;

    /**
     * The cumulated delay: the sum of the time difference in seconds between pollTime and publishTime of all new items.
     * <code>null</code> if there are no new items.
     */
    private Long cumulatedDelay = -1L;

    /** Number of items that have been missed (not read early enough). */
    private int misses = 0;

    /**
     * The number of items whos publishTime is newer than the last simulated poll that is within the benchmark interval.
     * This is relevant to the very last poll only.
     */
    private Integer pendingItems = null;

    /**
     * 
     * The number of items that have not been seen in evaluation mode. This is relevant to the first and last poll only.
     * Usually, {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} is set a couple of hours later than the
     * creation of the dataset was started. Therefore, for some feeds we have more than one window at the first
     * simulated poll. The same is true for {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}: For some feeds,
     * we have items that are newer than the end of the benchmark period.
     */
    private Integer droppedItems = null;

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
     * @return All polls have a sequential number starting from 1 at the first poll.
     */
    public final Integer getNumberOfPoll() {
        return numberOfPoll;
    }

    /**
     * @param numberOfPoll All polls have a sequential number starting from 1 at the first poll.
     */
    public final void setNumberOfPoll(Integer numberOfPoll) {
        this.numberOfPoll = numberOfPoll;
    }

    /**
     * @return the numberOfPollWithNewItem All polls that have at least one new item ha a sequential number starting
     *         from 1 at the first poll that contains items.
     */
    public final Integer getNumberOfPollWithNewItem() {
        return numberOfPollWithNewItem;
    }

    /**
     * @param numberOfPollWithNewItem All polls that have at least one new item ha a sequential number starting from 1
     *            at the first poll that contains items.
     */
    public final void setNumberOfPollWithNewItem(Integer numberOfPollWithNewItem) {
        this.numberOfPollWithNewItem = numberOfPollWithNewItem;
    }

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
     * @return The time of the poll as {@link Timestamp}.
     */
    public Timestamp getPollSQLTimestamp() {
        return new Timestamp(getPollTimestamp());
    }

    /**
     * @param cumulatedDelay The cumulated delay: the sum of the time difference in seconds between pollTime and
     *            publishTime of all new items. <code>null</code> if there are no new items.
     */
    public void setCumulatedDelay(Long cumulatedDelay) {
        this.cumulatedDelay = cumulatedDelay;
    }

    /**
     * @return The cumulated delay: the sum of the time difference in seconds between pollTime and publishTime of all
     *         new items. <code>null</code> if there are no new items.
     */
    public Long getCumulatedDelay() {
        return cumulatedDelay;
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
     * The number of items whos publishTime is newer than the last simulated poll that is within the benchmark interval.
     * This is relevant to the very last poll only.
     * 
     * @return The number of pending items.
     */
    public final Integer getPendingItems() {
        return pendingItems;
    }

    /**
     * The number of items whos publishTime is newer than the last simulated poll that is within the benchmark interval.
     * This is relevant to the very last poll only.
     * 
     * @param pendingItems The number of pending items.
     */
    public final void setPendingItems(Integer pendingItems) {
        this.pendingItems = pendingItems;
    }

    /**
     * The number of items that have not been seen in evaluation mode. This is relevant to the first and last poll only.
     * Usually, {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} is set a couple of hours later than the
     * creation of the dataset was started. Therefore, for some feeds we have more than one window at the first
     * simulated poll. The same is true for {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}: For some feeds,
     * we have items that are newer than the end of the benchmark period.
     * 
     * @return The number of items that have not been seen in evaluation mode. This is relevant to the
     *         first and last poll only.
     */
    public final Integer getPreBenchmarkItems() {
        return droppedItems;
    }

    /**
     * The number of items that have not been seen in evaluation mode. This is relevant to the first and last poll only.
     * Usually, {@link FeedReaderEvaluator#BENCHMARK_START_TIME_MILLISECOND} is set a couple of hours later than the
     * creation of the dataset was started. Therefore, for some feeds we have more than one window at the first
     * simulated poll. The same is true for {@link FeedReaderEvaluator#BENCHMARK_STOP_TIME_MILLISECOND}: For some feeds,
     * we have items that are newer than the end of the benchmark period.
     * 
     * @param droppedItems The number of items that have not been seen in evaluation mode. This is relevant to the
     *            first and last poll only.
     */
    public final void setDroppedItems(Integer droppedItems) {
        this.droppedItems = droppedItems;
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
        builder.append(", numberOfPoll=");
        builder.append(numberOfPoll);
        builder.append(", pollTimestamp=");
        builder.append(pollTimestamp);
        builder.append(", newWindowItems=");
        builder.append(newWindowItems);
        builder.append(", cumulatedDelay=");
        builder.append(cumulatedDelay);
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
