package tud.iir.helper;

import org.apache.log4j.Logger;

public class ThreadHelper {

    public static void deepSleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }

    public static void deepSleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Logger.getRootLogger().error(e.getMessage());
        }
    }
}
