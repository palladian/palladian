package ws.palladian.helper;

import org.apache.log4j.Logger;

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

        return showProgress(counter, totalCount, showEveryPercent, null);

    }

    public static String showProgress(long counter, long totalCount, int showEveryPercent, Logger logger) {

        String processString = "";
        if (counter % (showEveryPercent * totalCount / 100) == 0) {
            processString = MathHelper.round(100 * counter / (double)totalCount, 2) + "% (" + (totalCount - counter)
                    + " items remaining)";
            if (logger != null) {
                logger.info(processString);
            } else {
                System.out.println(processString);
            }
        }

        return processString;
    }

    public static void main(String[] args) {

        int totalCount = 1000;
        int showEvery = 5;

        for (int i = 1; i <= totalCount; i++) {
            ProgressHelper.showProgress(i, totalCount, showEvery);
        }

    }
}
