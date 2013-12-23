package ws.palladian.retrieval.helper;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * A request throttle which waits for a specified time interval between each request.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FixedIntervalRequestThrottle implements RequestThrottle {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FixedIntervalRequestThrottle.class);

    /** The time in milliseconds to wait after each request. */
    private final long pauseInterval;

    /** The time stamp of the last request. */
    private long lastRequest;

    public FixedIntervalRequestThrottle(long pauseInterval, TimeUnit timeUnit) {
        this(timeUnit.toMillis(pauseInterval));
    }

    public FixedIntervalRequestThrottle(long pauseInterval) {
        this.pauseInterval = pauseInterval;
    }

    @Override
    public synchronized void hold() {
        long sinceLast = System.currentTimeMillis() - lastRequest;
        if (sinceLast < pauseInterval) {
            try {
                long toWait = pauseInterval - sinceLast;
                LOGGER.debug("Waiting for {} milliseconds", toWait);
                Thread.sleep(toWait);
            } catch (InterruptedException e) {
                LOGGER.warn("{}", e);
            }
        }
        lastRequest = System.currentTimeMillis();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("FixedIntervalRequestThrottle [pauseInterval=");
        builder.append(pauseInterval);
        builder.append(", lastRequest=");
        builder.append(lastRequest);
        builder.append("]");
        return builder.toString();
    }

}
