package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;

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
    private String processName;
    private int currentCount = 0;
    private int totalCount = 0;
    private double showEveryPercent = 1.;
    private boolean compactRemaining = false;

    public ProgressMonitor(int totalCount, double showEveryPercent) {
        this.totalCount = totalCount;
        this.showEveryPercent = showEveryPercent;
    }

    public ProgressMonitor(int totalCount, double showEveryPercent, String processName) {
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
    public void printProgress(int counter) {
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
     * @param counter Counter for current iteration in a loop.
     */
    public void incrementAndPrintProgress() {
        currentCount++;
        printProgress(currentCount);
    }

    /**
     * <p>
     * Increments the counter by one and gets the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public String incrementAndGetProgress() {
        currentCount++;
        return getProgress(currentCount);
    }

    /**
     * <p>
     * Returns the current progress.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     */
    public String getProgress(int counter) {
        StringBuilder processString = new StringBuilder();
        try {
            if (showEveryPercent == 0 || counter % (showEveryPercent * totalCount / 100.0) < 1) {
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                processString.append(createProgressBar(percent));
                processString.append(" => ").append(percent).append("% (");

                if (processName != null) {
                    processString.append(processName).append(", ");
                }

                processString.append(totalCount - counter).append(" items remaining");
                if (stopWatch != null && percent > 0) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getTotalElapsedTime() / percent);
                    // if elapsed not possible (timer started long before progress helper used) =>
                    // long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / 10); => in case total
                    processString.append(", elapsed time: ").append(stopWatch.getTotalElapsedTimeString());
                    processString.append(", iteration time: ").append(stopWatch.getElapsedTimeString());
                    processString.append(", ~remaining: ").append(
                            DateHelper.formatDuration(0, msRemaining, compactRemaining));
                    stopWatch.start();
                }
                processString.append(")");
            }
        } catch (ArithmeticException e) {
        } catch (Exception e) {
        }

        return processString.toString();
    }

    public boolean isCompactRemaining() {
        return compactRemaining;
    }

    /**
     * <p>
     * Sets whether the remaining time should be shown in compact format.
     * </p>
     * 
     * @param compactRemaining True if the remaining time should be shown in compact format.
     */
    public void setCompactRemaining(boolean compactRemaining) {
        this.compactRemaining = compactRemaining;
    }

    public static void main(String[] args) {
        int totalCount = 1000;
        ProgressMonitor pm = new ProgressMonitor(1000, .5, "My Progress");
        for (int i = 1; i <= totalCount; i++) {
            ThreadHelper.deepSleep(200);
            pm.incrementAndPrintProgress();
        }
    }
}