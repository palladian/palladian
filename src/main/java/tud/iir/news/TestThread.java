package tud.iir.news;

import tud.iir.helper.DateHelper;
import tud.iir.helper.ThreadHelper;
import tud.iir.helper.TimerThread;

/**
 * The {@link FeedChecker} schedules {@link FeedTask}s for each {@link Feed}. The {@link FeedTask} will run every time
 * the feed is checked and also performs all
 * set {@link FeedProcessingAction}s.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @see FeedChecker
 * 
 */
class TestThread extends TimerThread implements Runnable {

    public TestThread() {

    }

    @Override
    public void run() {
        while (true) {
            ThreadHelper.sleep(2000);
            System.out.println("abc");
        }
    }

    @Override
    protected void stopThread() {
        LOGGER.debug("stopping thread after " + DateHelper.getTimeString(getTimeout()));
        Thread.currentThread().interrupt();
        System.exit(0);
    }

    public static void main(String[] abc) {
        TestThread t = new TestThread();
        t.setTimeout(4500);
        t.run();

        TestThread t2 = new TestThread();
        t2.setTimeout(10000);
        t2.run();
    }


}