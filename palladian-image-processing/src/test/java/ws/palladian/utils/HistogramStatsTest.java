package ws.palladian.utils;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.IntConsumer;

import static org.junit.Assert.assertEquals;

public class HistogramStatsTest {

    private static final HistogramStats getTestStats() {
        HistogramStats stats = new HistogramStats(10);
        stats.add(0, 2);
        stats.add(1, 2);
        stats.add(2, 4);
        stats.add(3, 3);
        stats.add(4, 4);
        stats.add(5, 5);
        stats.add(6, 5);
        stats.add(7, 2);
        stats.add(8, 3);
        stats.add(9, 4);
        return stats;
    }

    private static final double DELTA = 0.0001;

    @Test
    public void testHistogramStats() {
        HistogramStats stats = getTestStats();
        assertEquals(0, stats.getMin(), DELTA);
        assertEquals(9, stats.getMax(), DELTA);
        assertEquals(34, stats.getCount());
        assertEquals(164, stats.getSum(), DELTA);
        assertEquals((double) 164 / 34, stats.getMean(), DELTA);
        assertEquals(9, stats.getRange(), DELTA);
        assertEquals(5, stats.getMedian(), DELTA);
        assertEquals(1, stats.getPercentile(10), DELTA);
        assertEquals(2.639859491, stats.getStandardDeviation(), DELTA);
        assertEquals(-0.048559743, stats.getSkewness(), DELTA);
        assertEquals(-0.955284212, stats.getKurtosis(), DELTA);
        assertEquals(5, stats.getMode(), DELTA);
    }

    @Test
    public void testIteration() {
        List<Integer> numbers = new ArrayList<>();
        getTestStats().iterate(new IntConsumer() {
            @Override
            public void accept(int value) {
                numbers.add(value);
            }
        });
        assertEquals(34, numbers.size());
        assertSorted(numbers);
    }

    private static <T extends Comparable<? super T>> void assertSorted(List<T> values) {
        List<T> copy = new ArrayList<>(values);
        Collections.sort(copy);
        assertEquals("expected values to be sorted", copy, values);
    }

    @Test
    public void testEdgeCases() {
        HistogramStats stats = new HistogramStats(3);
        stats.add(1, 10);
        assertEquals(0, stats.getVariance(), DELTA);
        //		assertEquals(0, stats.getKurtosis(), DELTA);
        //		assertEquals(0, stats.getSkewness(), DELTA);
    }

}
