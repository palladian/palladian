package ws.palladian.helper;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.slf4j.Logger;
import ws.palladian.helper.collection.FixedSizeQueue;
import ws.palladian.helper.date.DateHelper;

/**
 * <p>
 * The ProgressMonitor eases the progress visualization needed in many long-running processes. Usage example:
 * <p>
 *
 * <pre>
 * ProgressMonitor pm = new ProgressMonitor();
 * pm.startTask(&quot;fancy calculation&quot;, 10);
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     pm.increment();
 * }
 * </pre>
 * <p>
 * </p>
 * <p>
 * XXX think about deprecation and extensibility, see https://bitbucket.org/palladian/palladian/commits/4c5fc38de8adfd5bd17e00e34881fe9b69d11a12#general-comments
 *
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressMonitor extends AbstractProgressReporter {
    private static final int PROGRESS_BAR_LENGTH = 50;

    private static final char PROGRESS_CHAR = '■';

    /**
     * Increments of percentages when the progress should be shown, e.g. every 1%.
     */
    private final double showEveryPercent;

    /**
     * Whether to output more progress information during each report.
     */
    private boolean enhancedStats = true;

    /**
     * Format of the percent output.
     */
    private final NumberFormat percentFormat;

    /**
     * Format of the number of remaining items.
     */
    private final NumberFormat itemFormat;

    private String description;

    private long totalSteps = -1;

    private long startTime;

    private long lastPrintTime;

    private long currentSteps;

    private double currentProgress;

    private Logger logger = null;

    /**
     * Keep track of the last 3 iterations
     */
    // private List<Long> lastIterationTimes;
    private FixedSizeQueue<Long> lastIterationTimes;
    private final static int LAST_ITERATION_WINDOW = 3;

    /**
     * Prevents outputting the same percentage value again, as specified by showEveryPercent.
     */
    private int lastOutput = -1;

    /**
     * A callback instance that is called when progress is written
     */
    private ProgressCallback callback;

    /**
     * A callback that is called when the progress is incremented.
     */
    private ProgressCallback incrementCallback;

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     *
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     */
    public ProgressMonitor(double showEveryPercent) {
        Validate.inclusiveBetween(0., 100., showEveryPercent, "showEveryPercent must be in range [0,100]");
        this.showEveryPercent = showEveryPercent;
        boolean decimalPrecision = showEveryPercent % 1 > 0;
        this.percentFormat = new DecimalFormat(decimalPrecision ? "##0.00" : "##0", new DecimalFormatSymbols(Locale.US));
        this.itemFormat = new DecimalFormat("###,###,###,##0", new DecimalFormatSymbols(Locale.US));
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor} which updates the status every one percent.
     * </p>
     */
    public ProgressMonitor() {
        this(1.);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor} showing the current progress with each percent.
     * </p>
     *
     * @param totalSteps The total iterations to perform, greater/equal zero.
     */
    public ProgressMonitor(long totalSteps) {
        this(totalSteps, 1);

    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     *
     * @param totalSteps       The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     */
    public ProgressMonitor(long totalSteps, double showEveryPercent) {
        this(totalSteps, showEveryPercent, null);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     *
     * @param totalSteps       The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     * @param processName      The name of the process, for identification purposes when outputting the bar.
     */
    public ProgressMonitor(long totalSteps, double showEveryPercent, String processName) {
        this(totalSteps, showEveryPercent, processName, null);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     *
     * @param totalSteps       The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     * @param processName      The name of the process, for identification purposes when outputting the bar.
     * @param logger           The logger to use instead of printing to the console.
     */
    public ProgressMonitor(long totalSteps, double showEveryPercent, String processName, Logger logger) {
        this(showEveryPercent);
        setLogger(logger);
        Validate.isTrue(totalSteps >= 0, "totalSteps must be greater/equal zero");
        startTask(processName, totalSteps);
    }

    /**
     * <p>
     * Increments the counter by one and prints the current progress to the System's standard output.
     * </p>
     */
    public void incrementAndPrintProgress() {
        increment();
    }

    /**
     * <p>
     * Increments the counter by the step size and prints the current progress to the System's standard output.
     * </p>
     *
     * @param steps The number of steps to increment the counter with.
     */
    public void incrementByAndPrintProgress(long steps) {
        increment(steps);
    }

    @Override
    public void startTask(String description, long totalSteps) {
        this.description = description;
        this.totalSteps = totalSteps;
        this.startTime = System.currentTimeMillis();
        this.lastPrintTime = 0;
        lastIterationTimes = FixedSizeQueue.create(LAST_ITERATION_WINDOW);
    }

    @Override
    public void increment(long steps) {
        synchronized (this) {
            currentSteps += steps;
            currentProgress = totalSteps > 0 ? (double) currentSteps / totalSteps : -1;
            printProgress();

            // call the callback if it is set
            if (incrementCallback != null) {
                incrementCallback.callback(this);
            }
        }
    }

    @Override
    public void add(double progress) {
        synchronized (this) {
            currentProgress = Math.min(currentProgress + progress, 1);
            printProgress();
        }
    }

    @Override
    public void finishTask() {
        synchronized (this) {
            currentSteps = totalSteps;
            currentProgress = 1;
            printProgress();
        }
    }

    @Override
    public double getProgress() {
        return currentProgress;
    }

    /**
     * <p>
     * Prints the current progress.
     * </p>
     */
    private void printProgress() {
        int output = (int) Math.floor(currentProgress * (100 / showEveryPercent));
        if (showEveryPercent == 0 || output != lastOutput) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            double percentLeft = 1.0 - currentProgress;
            double iterationsLeft = Math.min(percentLeft * totalSteps, 100 * percentLeft / showEveryPercent);
            List<String> statistics = new ArrayList<>();
            if (currentProgress >= 0) {
                statistics.add(percentFormat.format(100 * currentProgress) + "%");
            }
            if (output > 0) { // do not give any time estimates at the beginning or end

                if (totalSteps > 0 && isEnhancedStats()) {
                    statistics.add(itemFormat.format(totalSteps - currentSteps) + " items left");
                }
                statistics.add("elapsed: " + DateHelper.formatDuration(0, elapsedTime, true).replaceAll(":\\d+ms", ""));
                // if (lastIterationTimes == null) {
                // lastIterationTimes = new ArrayList<>();
                // }
                long iterationTime = elapsedTime - lastPrintTime;
                lastIterationTimes.add(0, iterationTime);
                if (isEnhancedStats()) {
                    statistics.add("iteration: " + DateHelper.formatDuration(0, iterationTime, true).replaceAll(":\\d+ms", ""));
                }
                if (currentProgress >= 0 && iterationsLeft > 0) {
                    double remainingTime = getAverageIterationTime() * iterationsLeft;
                    statistics.add("~remaining: " + DateHelper.formatDuration(0, (long) remainingTime, true).replaceAll(":\\d+ms", ""));
                }
            }
            StringBuilder progressString = new StringBuilder();
            if (description != null) {
                progressString.append(description).append(' ');
            }
            String progressBar = createProgressBar(currentProgress);
            progressString.append(progressBar);
            progressString.append(' ');
            progressString.append(StringUtils.join(statistics, " | "));
            if (logger != null) {
                logger.info(progressString.toString());
            } else {
                System.out.println(progressString);
            }
            lastOutput = output;
            lastPrintTime = elapsedTime;

            // call the callback if it is set
            if (callback != null) {
                callback.callback(this);
            }
        }
    }

    private double getAverageIterationTime() {
        double time = 0.;
        int count = lastIterationTimes.size();
        // for (int i = 0; i < count; i++) {
        // time += lastIterationTimes.get(i);
        // }
        // lastIterationTimes = lastIterationTimes.subList(0, count);
        for (Long lastIterationTime : lastIterationTimes) {
            time += lastIterationTime;
        }
        return time / count;
    }

    /**
     * <p>
     * Creates a progress bar.
     * </p>
     *
     * @param progress The progress in range [0,1]; negative values will lead to an empty progress bar.
     * @return The progress bar as string.
     */
    private static String createProgressBar(double progress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = progress >= 0 ? (int) Math.round(progress * PROGRESS_BAR_LENGTH) : 0;
        stringBuilder.append(StringUtils.repeat(PROGRESS_CHAR, scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', Math.max(PROGRESS_BAR_LENGTH - scaledPercent, 0)));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public boolean isEnhancedStats() {
        return enhancedStats;
    }

    public void setEnhancedStats(boolean enhancedStats) {
        this.enhancedStats = enhancedStats;
    }

    public void setCallback(ProgressCallback callback) {
        this.callback = callback;
    }

    public void setIncrementCallback(ProgressCallback callback) {
        this.incrementCallback = callback;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public static void main(String[] args) {
        // int totalSteps = 1700335346;
        // ProgressMonitor pm = new ProgressMonitor(0.1);
        // pm.startTask("My Progress", totalSteps);
        // for (int i = 1; i < totalSteps; i++) {
        // pm.increment();
        // }

        int totalSteps = 10;
        ProgressMonitor pm = new ProgressMonitor(0.1);
        pm.startTask("My Progress", totalSteps);
        for (int i = 0; i < totalSteps; i++) {
            pm.increment();
            ThreadHelper.deepSleep(250);
        }
    }
}