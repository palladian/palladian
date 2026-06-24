package ws.palladian.retrieval;

import org.apache.hc.core5.util.TimeValue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link HttpRetriever#capRetryInterval(TimeValue, TimeValue)} — the guard that stops a server-supplied
 * {@code Retry-After} header from making a request (and therefore the caller's thread pool) sleep for an unbounded
 * time. Regression test for the 2026-06-23 update-service hang, where a single absurd {@code Retry-After} made a pooled
 * worker {@code Thread.sleep} for over 30 minutes.
 *
 * @author David Urbansky
 */
public class HttpRetrieverRetryStrategyTest {

    private static final TimeValue MAX = TimeValue.ofSeconds(5);

    @Test
    public void capsAnAbsurdRetryAfter() {
        // e.g. Retry-After: 86400, or a far-future HTTP-date that Apache converts to hours — must not block us that long
        assertEquals(5, HttpRetriever.capRetryInterval(TimeValue.ofHours(24), MAX).toSeconds());
    }

    @Test
    public void keepsAnIntervalWithinTheCap() {
        assertEquals(1000, HttpRetriever.capRetryInterval(TimeValue.ofSeconds(1), MAX).toMilliseconds());
    }

    @Test
    public void keepsTheBoundaryValueUntouched() {
        assertEquals(5000, HttpRetriever.capRetryInterval(TimeValue.ofSeconds(5), MAX).toMilliseconds());
    }

    @Test
    public void normalizesNullToZero() {
        assertEquals(0, HttpRetriever.capRetryInterval(null, MAX).toMilliseconds());
    }

    @Test
    public void normalizesNegativeToZero() {
        assertEquals(0, HttpRetriever.capRetryInterval(TimeValue.NEG_ONE_MILLISECOND, MAX).toMilliseconds());
    }
}
