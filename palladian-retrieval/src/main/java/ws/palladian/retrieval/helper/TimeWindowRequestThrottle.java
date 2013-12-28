package ws.palladian.retrieval.helper;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateHelper;

/**
 * <p>
 * Request throttle for controlling window-based policies, e.g. maximum 1000 requests/5 minutes.
 * </p>
 * 
 * @author pk
 * 
 */
public class TimeWindowRequestThrottle implements RequestThrottle {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(TimeWindowRequestThrottle.class);

    private final long timeWindow;

    private final int maximumRequests;

    private final Queue<Long> requestTimestamps;
    
    private long totalThrottledTime;

    /**
     * <p>
     * Create a new {@link TimeWindowRequestThrottle}.
     * </p>
     * 
     * @param timeWindow The time window, must be greater zero.
     * @param unit The time unit, not <code>null</code>.
     * @param maximumRequests The maximum number of requests which are allowed in the specified window, must be greater
     *            zero.
     */
    public TimeWindowRequestThrottle(long timeWindow, TimeUnit unit, int maximumRequests) {
        Validate.isTrue(timeWindow > 0, "timeWindow must be greater zero");
        Validate.notNull(unit, "unit must not be null");
        Validate.isTrue(maximumRequests > 0, "maximumRequests must be greater zero");
        this.timeWindow = unit.toMillis(timeWindow);
        this.maximumRequests = maximumRequests;
        this.requestTimestamps = new LinkedList<Long>();
        this.totalThrottledTime = 0;
    }

    @Override
    public synchronized void hold() {
        // if maximum request counts would be exceeded, we need to wait
        if (getNumRequestsInWindow() >= maximumRequests) {
            Long oldestTimestamp = requestTimestamps.poll();
            long timeToWait = oldestTimestamp - (System.currentTimeMillis() - timeWindow);
            totalThrottledTime += timeToWait;
            if (timeToWait > 5000) { // show info, when we have to wait long
                LOGGER.info("Waiting for {}", DateHelper.getTimeString(timeToWait));
            } else {
                LOGGER.debug("Waiting for {}", DateHelper.getTimeString(timeToWait));
            }
            try {
                Thread.sleep(timeToWait);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        requestTimestamps.add(System.currentTimeMillis());
    }

    /**
     * @return The number of requests which were performed in the specified time window.
     */
    public synchronized int getNumRequestsInWindow() {
        // remove expired timestamps; these are the ones, which are outside the window now
        for (Long head = requestTimestamps.peek(); head != null; head = requestTimestamps.peek()) {
            if (head < System.currentTimeMillis() - timeWindow) {
                requestTimestamps.remove();
            } else {
                break;
            }
        }
        return requestTimestamps.size();
    }
    
    /**
     * @return The total time, which this throttle was blocked in milliseconds.
     */
    public long getTotalThrottledTime() {
        return totalThrottledTime;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("TimeWindowRequestThrottle [timeWindow=");
        builder.append(timeWindow);
        builder.append(", maximumRequests=");
        builder.append(maximumRequests);
        builder.append(", numRequests=");
        builder.append(getNumRequestsInWindow());
        builder.append("]");
        return builder.toString();
    }

}
