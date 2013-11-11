package ws.palladian.retrieval.helper;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Philipp Katz
 */
public class RequestThrottle {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestThrottle.class);

    /** The time in milliseconds to wait after each request. */
    private long pauseInterval;
    
    /** The time stamp of the last request. */
    private long lastRequest;

    public RequestThrottle(long pauseInterval, TimeUnit timeUnit) {
        this(timeUnit.toMillis(pauseInterval));
    }

    public RequestThrottle(long pauseInterval) {
        this.pauseInterval = pauseInterval;
    }

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

    public long getPauseInterval() {
        return pauseInterval;
    }

    public void setPauseInterval(long pauseInterval) {
        this.pauseInterval = pauseInterval;
    }
}
