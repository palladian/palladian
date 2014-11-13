package ws.palladian.helper;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.date.DateHelper;

/**
 * <p>
 * The ProgressMonitor eases the progress visualization needed in many long-running processes. Usage example:
 * 
 * <pre>
 * ProgressMonitor pm = new ProgressMonitor();
 * pm.startTask(&quot;fancy calculation&quot;, 10);
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     pm.increment();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressMonitor extends AbstractProgressReporter {

    private static final int PROGRESS_BAR_LENGTH = 50;

    private static final char PROGRESS_CHAR = '■';

    private final double showEveryPercent;

    private String description;

    private long totalSteps = -1;

    private long startTime;

    private long currentSteps;

    private double currentProgress;

    /** Prevents outputting the same percentage value again, as specified by showEveryPercent. */
    private int lastOutput = -1;

    private DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * 
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     */
    public ProgressMonitor(double showEveryPercent) {
        Validate.inclusiveBetween(0., 100., showEveryPercent, "showEveryPercent must be in range [0,100]");
        this.showEveryPercent = showEveryPercent;

        if (showEveryPercent%1 > 0) {
            format.applyPattern("##0.00");
        } else {
            format.applyPattern("##0");
        }
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
     * @param totalCount The total iterations to perform, greater/equal zero.
     */
    public ProgressMonitor(long totalCount) {
        this(totalCount, 1);

    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     */
    public ProgressMonitor(long totalCount, double showEveryPercent) {
        this(totalCount, showEveryPercent, null);
    }

    /**
     * <p>
     * Create a new {@link ProgressMonitor}.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
     * @param processName The name of the process, for identification purposes when outputting the bar.
     */
    public ProgressMonitor(long totalCount, double showEveryPercent, String processName) {
        Validate.isTrue(totalCount >= 0, "totalCount must be greater/equal zero");
        Validate.inclusiveBetween(0., 100., showEveryPercent, "showEveryPercent must be in range [0,100]");
        this.showEveryPercent = showEveryPercent;
        startTask(processName, totalCount);

        if (showEveryPercent%1 > 0) {
            format.applyPattern("##0.00");
        } else {
            format.applyPattern("##0");
        }
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
    }

    @Override
    public void increment(long steps) {
        synchronized (this) {
            currentSteps += steps;
            currentProgress = totalSteps > 0 ? (double)currentSteps / totalSteps : -1;
            printProgress();
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
     * <p>Prints the current progress.</p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    private void printProgress() {
        int output = (int)Math.floor(currentProgress * (100 / showEveryPercent));
        if (showEveryPercent == 0 || output != lastOutput) {
            long elapsedTime = System.currentTimeMillis() - startTime;
            long remainingTime = (long)(elapsedTime / currentProgress) - elapsedTime;
            List<String> statistics = new ArrayList<String>();
            if (currentProgress >= 0) {
                statistics.add(format.format(100 * currentProgress) + "%");
            }
            if (output > 0) { // do not give any time estimates at the beginning or end
                statistics.add("elapsed: " + DateHelper.formatDuration(0, elapsedTime, true).replaceAll(":\\d+ms",""));
                if (currentProgress >= 0 && remainingTime > 0) {
                    statistics.add("~remaining: " + DateHelper.formatDuration(0, remainingTime, true).replaceAll(":\\d+ms",""));
                }
            }
            StringBuilder progressString = new StringBuilder();
            if (description != null) {
                progressString.append(description).append(' ');
            }
            String progressBar = createProgressBar(currentProgress);
            progressString.append(progressBar);
            progressString.append(' ');
            progressString.append(StringUtils.join(statistics, ", "));
            System.out.println(progressString);
            lastOutput = output;
        }
    }

    /**
     * <p>Creates a progress bar.</p>
     * 
     * @param progress The progress in range [0,1]; negative values will lead to an empty progress bar.
     * @return The progress bar as string.
     */
    private static String createProgressBar(double progress) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = progress >= 0 ? (int)Math.round(progress * PROGRESS_BAR_LENGTH) : 0;
        stringBuilder.append(StringUtils.repeat(PROGRESS_CHAR, scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', Math.max(PROGRESS_BAR_LENGTH - scaledPercent, 0)));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    /**
     * <p>Get the number of decimal digits for a double value. Does not consider scientific notations currently.</p>
     * 
     * @param value The value.
     * @return The number of decimal digits.
     */
    private static int getDecimalDigitCount(double value) {
        String stringValue = Double.toString(value).replaceAll("0+$", ""); // remove trailing zeros
        int idx = stringValue.indexOf('.');
        return idx > 0 ? stringValue.length() - idx - 1 : 0;
    }

    public static void main(String[] args) {
        int totalCount = 1759600335;
        ProgressMonitor pm = new ProgressMonitor(totalCount, 1., "My Progress");
        for (int i = 1; i <= totalCount + 10; i++) {
            pm.increment();
        }
    }

}
