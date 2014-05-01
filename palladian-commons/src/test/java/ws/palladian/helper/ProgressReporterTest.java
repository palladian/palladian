package ws.palladian.helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class ProgressReporterTest {
    private static final double DELTA = 0.0001;

    @Test
    public void testProgressReporter() {
        ProgressReporter reporter = new ProgressMonitor(0.);
        assertEquals(0, reporter.getProgress(), DELTA);
        ProgressReporter subReporter1 = reporter.createSubProgress(.5);
        subReporter1.startTask(null, 10);
        for (int i = 0; i < 10; i++) {
            subReporter1.increment();
        }
        assertEquals(0.5, reporter.getProgress(), DELTA);
        ProgressReporter subReporter2 = reporter.createSubProgress(.5);
        for (int i = 0; i < 5; i++) {
            subReporter2.add(0.1);
        }
        assertEquals(0.75, reporter.getProgress(), DELTA);
        subReporter2.finishTask();
        assertEquals(1, reporter.getProgress(), DELTA);
    }

}
