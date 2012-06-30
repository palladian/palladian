package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ThresholdAnalyzerTest {

    private static final double DELTA = 0.000001;

    @Test
    public void testThresholdStats() {

        ThresholdAnalyzer stats = new ThresholdAnalyzer(5);

        assertEquals(0, stats.getBin(0.0));
        assertEquals(1, stats.getBin(0.1));
        assertEquals(1, stats.getBin(0.2));
        assertEquals(4, stats.getBin(0.8));
        assertEquals(5, stats.getBin(0.9));
        assertEquals(5, stats.getBin(1.0));

        stats.add(true, 1);
        stats.add(false, 0.895);
        stats.add(false, 0.894);
        stats.add(true, 0.856);
        stats.add(true, 0.833);
        stats.add(true, 0.723);
        stats.add(true, 0.703);
        stats.add(false, 0.674);
        stats.add(true, 0.651);
        stats.add(true, 0.589);
        stats.add(true, 0.548);
        stats.add(false, 0.37);
        stats.add(false, 0.363);
        stats.add(false, 0.338);
        stats.add(true, 0);

        assertEquals(15, stats.getRetrievedAt(0.0));
        assertEquals(11, stats.getRetrievedAt(0.5));
        assertEquals(1, stats.getRetrievedAt(1));

        assertEquals(9, stats.getTruePositiveAt(0.0));
        assertEquals(8, stats.getTruePositiveAt(0.5));
        assertEquals(1, stats.getTruePositiveAt(1));

        assertEquals(9. / 15, stats.getPrecision(0), DELTA);
        assertEquals(8. / 11, stats.getPrecision(0.548), DELTA);
        assertEquals(1., stats.getPrecision(1), DELTA);

        assertEquals(1, stats.getRecall(0), DELTA);
        assertEquals(8. / 9, stats.getRecall(0.548), DELTA);
        assertEquals(1. / 9, stats.getRecall(1), DELTA);

        assertEquals((2 * 8. / 11 * 8. / 9) / (8. / 11 + 8. / 9), stats.getF1(0.548), DELTA);

    }

}
