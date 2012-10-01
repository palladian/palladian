package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * The ProgressHelper eases the progress visualization needed in many long-running processes.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class ProgressHelper {
    
    private ProgressHelper() {
        // no instances.
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent) {
        return showProgress(counter, totalCount, showEveryPercent, null, null);
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, Logger logger) {
        return showProgress(counter, totalCount, showEveryPercent, logger, null);
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, StopWatch stopWatch) {
        return showProgress(counter, totalCount, showEveryPercent, null, stopWatch);
    }

    /**
     * 
     * @param counter Counter for current iteration in a loop.
     * @param totalCount The total number of iterations.
     * @param showEveryPercent Specify how often to output the progress. Set to zero to output whith each iteration.
     * @param logger A {@link Logger} for outputting the progress information. If <code>null</code>, the progress will be sent to {@link System#out}.
     * @param stopWatch A {@link StopWatch} which allows an approximation of the estimated time until completion.
     * @return
     */
    public static String showProgress(long counter, long totalCount, int showEveryPercent, Logger logger,
            StopWatch stopWatch) {

        StringBuilder processString = new StringBuilder();
        try {
            if (showEveryPercent == 0 || counter % (showEveryPercent * totalCount / 100.0) < 1) {
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                processString.append(createProgressBar(percent));
                processString.append(" => ").append(percent).append("% (").append(totalCount - counter)
                        .append(" items remaining");
                if (stopWatch != null && percent > 0) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime() / percent);
                    processString.append(", elapsed time: ").append(stopWatch.getElapsedTimeString());
                    processString.append(", est. time remaining: ").append(DateHelper.getRuntime(0, msRemaining));
//                    long msRemaining = (long)((100 / percent) * stopWatch.getElapsedTime());
//                    processString.append(", iteration time: ").append(stopWatch.getElapsedTimeString());
//                    processString.append(", est. time remaining: ").append(DateHelper.getRuntime(0, msRemaining));
//                    stopWatch.start();
                }
                processString.append(")");

                if (logger != null) {
                    logger.info(processString);
                } else {
                    System.out.println(processString);
                }
            }
        } catch (ArithmeticException e) {
            // LOGGER.error(e.getMessage());
        }

        return processString.toString();
    }
    
    private static String createProgressBar(double percent) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append('[');
        int scaledPercent = (int) Math.round(percent / 2);
        stringBuilder.append(StringUtils.repeat('=', scaledPercent));
        stringBuilder.append(StringUtils.repeat(' ', 50 - scaledPercent));
        stringBuilder.append(']');
        return stringBuilder.toString();
    }

    public static void main(String[] args) {

        StopWatch stopWatch = new StopWatch();
        for (int i = 1; i <= 10; i++) {
            ThreadHelper.deepSleep(10000);
            ProgressHelper.showProgress(i, 10, 1, stopWatch);
        }

        System.exit(0);

        int totalCount = 5000;
        int showEvery = 5;

        for (int i = 1; i <= totalCount; i++) {
            ProgressHelper.showProgress(i, totalCount, showEvery);
        }

    }
}
