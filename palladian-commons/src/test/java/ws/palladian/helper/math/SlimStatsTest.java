package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.Test;

public class SlimStatsTest {

    @Test
    public void testRunningStats() {
        Stats stats = new SlimStats().add(2., 1., 6., 10., 23., 7.);
        assertEquals(6, stats.getCount());
        assertEquals(8.167, stats.getMean(), 0.001);
        assertEquals(23, stats.getMax(), 0);
        assertEquals(1, stats.getMin(), 0);
        assertEquals(7.985, stats.getStandardDeviation(), 0.001);
        assertEquals(119.833, stats.getMse(), 0.001);
        assertEquals(10.947, stats.getRmse(), 0.001);
        assertEquals(22, stats.getRange(), 0);
    }

    @Test
    public void testStandardDeviation() {
        assertEquals(2.14, new SlimStats().add(2., 4., 4., 4., 5., 5., 7., 9.).getStandardDeviation(), 0.01);
        assertEquals(2.24, new SlimStats().add(4, 2, 5, 8, 6).getStandardDeviation(), 0.01);
        assertEquals(0, new SlimStats().add(1).getStandardDeviation(), 0);
        assertTrue(Double.isNaN(new SlimStats().getStandardDeviation()));
    }

    @Test
    public void testNoValues() {
        Stats stats = new SlimStats();
        assertEquals(0, stats.getCount());
        assertTrue(Double.isNaN(stats.getMax()));
        assertTrue(Double.isNaN(stats.getMin()));
        assertTrue(Double.isNaN(stats.getMean()));
        assertTrue(Double.isNaN(stats.getStandardDeviation()));
        assertEquals(0, stats.getSum(), 0);
        assertTrue(Double.isNaN(stats.getMse()));
        assertTrue(Double.isNaN(stats.getRmse()));
        assertTrue(Double.isNaN(stats.getRange()));
    }

    @Test
    public void testOverflow() {
        Double[] temp = new Double[10000];
        Arrays.fill(temp, Double.MAX_VALUE);
        Stats stats = new SlimStats().add(temp);
        assertEquals(Double.MAX_VALUE, stats.getMax(), 0);
        assertEquals(Double.MAX_VALUE, stats.getMin(), 0);
        assertEquals(Double.MAX_VALUE, stats.getMean(), 0);
        assertEquals(0, stats.getStandardDeviation(), 0);
        assertEquals(10000, stats.getCount());
    }

}
