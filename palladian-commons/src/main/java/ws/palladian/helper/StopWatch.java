package ws.palladian.helper;

import java.util.concurrent.TimeUnit;

import ws.palladian.helper.date.DateHelper;

/**
 * A simple stop watch for performance testing.
 * 
 * @author David Urbansky
 * 
 */
public class StopWatch {

    /** The start time. */
    private long startTime = 0;

    /** The time of the last break point (when start was called). */
    private long lastBreakpointTime = 0;

    /** The stop time. */
    private long stopTime = 0;

    /** Count down in milliseconds, -1 means no count down set. */
    private long countDown = -1;

    /** Whether the stop watch is running or not. */
    private boolean running = false;

    /** To which detail the output should be shown. */
    private TimeUnit outputDetail = TimeUnit.MILLISECONDS;

    /**
     * The StopWatch starts running right after object creation.
     */
    public StopWatch() {
        this.startTime = System.currentTimeMillis();
        start();
    }

    /** Start/reset the stop watch. */
    public void start() {
        this.lastBreakpointTime = System.currentTimeMillis();
        this.running = true;
    }

    /** Stop the stop watch. */
    public void stop() {
        this.stopTime = System.currentTimeMillis();
        this.running = false;
    }

    /** Set a count down in milliseconds. */
    public void setCountDown(long countDown) {
        this.countDown = countDown;
    }

    /** Get the count down. */
    public long getCountDown() {
        return countDown;
    }

    /** Check whether count down is up. */
    public boolean timeIsUp() {
        if (countDown == -1) {
            return false;
        } else if (getElapsedTime() - countDown >= 0) {
            return true;
        }
        return false;
    }

    /**
     * Get the time when the stop watch was started.
     * 
     * @return The timestamp of the start time.
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Get the elapsed time.
     * 
     * @param inSeconds If true, the elapsed time will be returned in seconds, otherwise in milliseconds.
     * @return The elapsed time.
     */
    public long getElapsedTime(boolean inSeconds) {
        long elapsed;
        if (running) {
            elapsed = System.currentTimeMillis() - lastBreakpointTime;
        } else {
            elapsed = stopTime - lastBreakpointTime;
        }
        if (inSeconds) {
            elapsed = elapsed / 1000;
        }
        return elapsed;
    }

    /**
     * <p>
     * Get the total elapsed time since the first start of the stop watch.
     * </p>
     * 
     * @return The number of milliseconds since the timer was started for the first time.
     */
    public long getTotalElapsedTime() {
        return System.currentTimeMillis() - startTime;
    }

    /**
     * Get the elapsed time in milliseconds.
     * 
     * @return The elapsed time.
     */
    public long getElapsedTime() {
        return getElapsedTime(false);
    }

    /**
     * Get the elapsed time as a string, that is, the time from the method call to the last time {@code start()} was
     * called.
     * 
     * @param output If true, the elapsed time will be printed to the console as well.
     * @return The elapsed time as a string.
     */
    public String getElapsedTimeString(boolean output) {
        String elapsed;
        if (running) {
            elapsed = DateHelper.formatDuration(lastBreakpointTime);
        } else {
            elapsed = DateHelper.formatDuration(lastBreakpointTime, stopTime);
        }

        elapsed = shortenTimeString(elapsed);

        if (output) {
            System.out.println(elapsed);
        }
        return elapsed;
    }

    private String shortenTimeString(String elapsed) {
        if (outputDetail != TimeUnit.MILLISECONDS) {
            elapsed = elapsed.replaceAll("\\:\\d+ms.*", "");
            if (outputDetail != TimeUnit.SECONDS) {
                elapsed = elapsed.replaceAll("\\:\\d+s.*", "");
                if (outputDetail != TimeUnit.MINUTES) {
                    elapsed = elapsed.replaceAll("\\:\\d+m\\:.*", "");
                }
            }
        }
        return elapsed;
    }

    /**
     * Get the elapsed time as a string without console output.
     * 
     * @return The elapsed time as a string.
     */
    public String getElapsedTimeString() {
        return getElapsedTimeString(false);
    }

    /**
     * Get the elapsed time as a string.
     * 
     * @param output If true, the elapsed time will be printed to the console as well.
     * @return The elapsed time as a string.
     */
    public String getTotalElapsedTimeString(boolean output) {
        String elapsed;
        if (running) {
            elapsed = DateHelper.formatDuration(startTime);
        } else {
            elapsed = DateHelper.formatDuration(startTime, stopTime);
        }

        elapsed = shortenTimeString(elapsed);

        if (output) {
            System.out.println(elapsed);
        }
        return elapsed;
    }

    /**
     * Get the elapsed time as a string without console output.
     * 
     * @return The elapsed time as a string.
     */
    public String getTotalElapsedTimeString() {
        return getTotalElapsedTimeString(false);
    }

    @Override
    public String toString() {
        return getElapsedTimeString();
    }

    public TimeUnit getOutputDetail() {
        return outputDetail;
    }

    public void setOutputDetail(TimeUnit outputDetail) {
        this.outputDetail = outputDetail;
    }

    public static void main(String[] args) {
        StopWatch s = new StopWatch();

        for (int i = 1; i < 100; i++) {
            int c = i * i * i;
            double d = c + c * c * c * c;
            System.out.println(d);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        s.stop();
        s.getElapsedTimeString(true);
        System.out.println(s.getElapsedTime(true));
    }
}