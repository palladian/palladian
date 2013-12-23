package ws.palladian.retrieval.helper;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import ws.palladian.helper.StopWatch;

public class TimeWindowRequestThrottleTest {
    
    @Test
    public void testTimeWindowRequestThrottle() throws InterruptedException {
        TimeWindowRequestThrottle throttle = new TimeWindowRequestThrottle(1, TimeUnit.SECONDS, 3);
        StopWatch stopWatch = new StopWatch();

        // ||| block 1000 ms || sleep 100 ms | block 900 ms | sleep 100 ms | sleep 100 ms | block 800 ms | done.
        for (int millisToSleep : Arrays.asList(0, 0, 0, 0, 0, 100, 0, 100, 100, 100)) {
            throttle.hold();
            Thread.sleep(millisToSleep);
            assertTrue(throttle.getNumRequestsInWindow() <= 3);
        }
        
        // time spent blocking must be 1000 ms + 900 ms + 800 ms = 2700 ms
        assertTrue(2600 < throttle.getTotalThrottledTime());
        assertTrue(2800 > throttle.getTotalThrottledTime());
        
        // total time spent must be 1000 ms + 100 ms + 900 ms + 100 ms + 100 ms + 800 ms + 100 = 3100 ms
        assertTrue(2900 < stopWatch.getElapsedTime());
        assertTrue(3200 > stopWatch.getElapsedTime());
    }

}
