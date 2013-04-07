package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * The ProgressHelper eases the progress visualization needed in many long-running processes. Usage example:
 * 
 * <pre>
 * StopWatch stopWatch = new StopWatch();
 * for (int i = 0; i &lt; 10; i++) {
 *     performSophisticatedCalculations(i);
 *     ProgressHelper.showProgress(i, 10, 1, stopWatch);
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressHelper {

    private final static char PROGRESS_CHAR = '■';

    private ProgressHelper() {
        // no instances.
    }

    private static String createProgressBar(double percent) {
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
     * @param totalCount The total number of iterations.
     * @param showEveryPercent Specify how often to output the progress. Set to zero to output with each iteration.
     */
    public static void printProgress(long counter, long totalCount, double showEveryPercent) {
        String progress = getProgress(counter, totalCount, showEveryPercent);
        if (!progress.isEmpty()) {
            System.out.println(progress);
        }
    }

    /**
     * <p>
     * Prints the current progress to the System's standard output.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     * @param totalCount The total number of iterations.
     * @param showEveryPercent Specify how often to output the progress. Set to zero to output with each iteration.
     * @param stopWatch A {@link StopWatch} which allows an approximation of the estimated time until completion.
     */
    public static void printProgress(long counter, long totalCount, double showEveryPercent, StopWatch stopWatch) {
        String progress = getProgress(counter, totalCount, showEveryPercent, stopWatch);
        if (!progress.isEmpty()) {
            System.out.println(progress);
        }
    }

    /**
     * <p>
     * Create a progress indicator, which can e.g. be supplied to a logger.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     * @param totalCount The total number of iterations.
     * @param showEveryPercent Specify how often to output the progress. Set to zero to output with each iteration.
     * @return The current progress, or an empty string if no progress is to be generated.
     */
    public static String getProgress(long counter, long totalCount, double showEveryPercent) {
        return getProgress(counter, totalCount, showEveryPercent, null);
    }

    /**
     * <p>
     * Create a progress indicator, which can e.g. be supplied to a logger.
     * </p>
     * 
     * @param counter Counter for current iteration in a loop.
     * @param totalCount The total number of iterations.
     * @param showEveryPercent Specify how often to output the progress. Set to zero to output with each iteration.
     * @param stopWatch A {@link StopWatch} which allows an approximation of the estimated time until completion.
     * @return The current progress, or an empty string if no progress is to be generated.
     */
    public static String getProgress(long counter, long totalCount, double showEveryPercent, StopWatch stopWatch) {
        StringBuilder processString = new StringBuilder();
        try {
            if (showEveryPercent == 0 || counter % (showEveryPercent * totalCount / 100.0) < 1) {
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                processString.append(createProgressBar(percent));
                processString.append(" => ").append(percent).append("% (").append(totalCount - counter)
                        .append(" items remaining");
                if (stopWatch != null && percent > 0) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getTotalElapsedTime() / percent);
                    // if elapsed not possible (timer started long before progress helper used) =>
                    // long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / 10); => in case total
                    processString.append(", elapsed time: ").append(stopWatch.getTotalElapsedTimeString());
                    processString.append(", iteration time: ").append(stopWatch.getElapsedTimeString());
                    processString.append(", ~remaining: ").append(DateHelper.formatDuration(0, msRemaining));
                    stopWatch.start();
                }
                processString.append(")");
            }
        } catch (ArithmeticException e) {
            // LOGGER.error(e.getMessage());
        } catch (Exception e) {

        }
        return processString.toString();
    }

    public static void main(String[] args) {

        int totalCount = 1000;
        double showEvery = .5;

        for (int i = 1; i <= totalCount; i++) {
            ProgressHelper.printProgress(i, totalCount, showEvery);
        }

    }
}
