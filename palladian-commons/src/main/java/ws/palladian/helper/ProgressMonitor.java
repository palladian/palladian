package ws.palladian.helper;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * The ProgressMonitor eases the progress visualization needed in many long-running processes. Usage example:
 * 
 * <pre>
 * ProgressMonitor pm = new ProgressMonitor();
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     pm.incrementAndPrintProgress();
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressMonitor {

    private final static char PROGRESS_CHAR = '■';
    private final StopWatch stopWatch = new StopWatch();
    private final String processName;
    private final AtomicLong currentCount = new AtomicLong(0);
    private final long totalCount;
    private final double showEveryPercent;

    /**
     * <p>
     * Create a new {@link ProgressMonitor} showing the current progress with each percent.
     * </p>
     * 
     * @param totalCount The total iterations to perform, greater/equal zero.
     * @param showEveryPercent Step size for outputting the progress in range [0,100].
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
        this.totalCount = totalCount;
        this.showEveryPercent = showEveryPercent;
        this.processName = processName;
    }

    private String createProgressBar(double percent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = (int)Math.round(percent / 2);
        stringBuilder.append(StringUtils.repeat(PROGRESS_CHAR, scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', Math.max(50 - scaledPercent, 0)));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    /**
     * <p>
     * Prints the current progress to the System's standard output.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public void printProgress(long counter) {
        String progress = getProgress(counter);
        if (!progress.isEmpty()) {
            System.out.println(progress);
        }
    }

    /**
     * <p>
     * Increments the counter by one and prints the current progress to the System's standard output.
     * </p>
     * 
     */
    public void incrementAndPrintProgress() {
        printProgress(currentCount.incrementAndGet());
    }

    /**
     * <p>
     * Increments the counter by the step size and prints the current progress to the System's standard output.
     * </p>
     * 
     * @param steps The number of steps to increment the counter with.
     */
    public void incrementByAndPrintProgress(long steps) {
        for (long i = 0; i < steps; i++) {
            incrementAndPrintProgress();
        }
    }

    /**
     * <p>
     * Increments the counter by one and gets the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public String incrementAndGetProgress() {
        return getProgress(currentCount.incrementAndGet());
    }

    /**
     * <p>
     * Returns the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    private String getProgress(long counter) {
        try {
            if (showEveryPercent == 0 || counter % (showEveryPercent * totalCount / 100.0) < 1) {
                StringBuilder progressString = new StringBuilder();
                if (processName != null) {
                    progressString.append(processName).append(" ");
                }
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                progressString.append(createProgressBar(percent));
                progressString.append(" ").append(percent).append("% (");
                progressString.append(totalCount - counter).append(" remaining");
                if (stopWatch != null && percent > 0) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getTotalElapsedTime() / percent);
                    // if elapsed not possible (timer started long before progress helper used) =>
                    // long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / 10); => in case total
                    progressString.append(", elapsed: ").append(stopWatch.getTotalElapsedTimeString());
                    progressString.append(", iteration: ").append(stopWatch.getElapsedTimeString());
                    if (counter < totalCount) {
                        progressString.append(", ~remaining: ").append(DateHelper.formatDuration(0, msRemaining, true));
                    }
                    stopWatch.start();
                }
                progressString.append(")");
                return progressString.toString();
            }
        } catch (ArithmeticException e) {
        } catch (Exception e) {
        }
        return StringUtils.EMPTY;
    }

    public String getTotalElapsedTimeString() {
        return stopWatch.getTotalElapsedTimeString();
    }

    public long getCurrentCount() {
        return currentCount.get();
    }

    public static void main(String[] args) {
        int totalCount = 1759600335;
        ProgressMonitor pm = new ProgressMonitor(totalCount, .5, "My Progress");
        for (int i = 1; i <= totalCount; i++) {
            // ThreadHelper.deepSleep(200);
            pm.incrementAndPrintProgress();
        }
    }

}
