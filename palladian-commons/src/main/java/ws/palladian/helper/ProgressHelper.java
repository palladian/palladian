package ws.palladian.helper;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * The ProgressHelper eases the progress visualization needed in many long-running processes.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class ProgressHelper {

    public static String showProgress(long counter, long totalCount, int showEveryPercent) {
        return showProgress(counter, totalCount, showEveryPercent, null, null);
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, Logger logger) {
        return showProgress(counter, totalCount, showEveryPercent, logger, null);
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, StopWatch stopWatch) {
        return showProgress(counter, totalCount, showEveryPercent, null, stopWatch);
    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, Logger logger,
            StopWatch stopWatch) {

        String processString = "";
        try {
            if (counter % (showEveryPercent * totalCount / 100.0) < 1) {
                double percent = MathHelper.round(100 * counter / (double)totalCount, 2);
                processString = percent + "% ("
                        + (totalCount - counter) + " items remaining";

                if (stopWatch != null) {
                    long msRemaining = (long)((100 - percent) * stopWatch.getElapsedTime());
                    processString += ", iteration time: " + stopWatch.getElapsedTimeString()
                            + ", est. time remaining: " + DateHelper.getRuntime(0, msRemaining) + ")";
                    stopWatch.start();
                } else {
                    processString += ")";
                }

                if (logger != null) {
                    logger.info(processString);
                } else {
                    System.out.println(processString);
                }
            }
        } catch (ArithmeticException e) {
            // LOGGER.error(e.getMessage());
        }

        return processString;
    }

    public static void main(String[] args) {

        int totalCount = 5000;
        int showEvery = 5;

        for (int i = 1; i <= totalCount; i++) {
            ProgressHelper.showProgress(i, totalCount, showEvery);
        }

    }
}
