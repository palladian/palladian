package ws.palladian.helper.geo;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;

import ws.palladian.helper.StopWatch;

public class GeoUtilsTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    private static Collection<GeoCoordinate> coordinates1;
    private static Collection<GeoCoordinate> coordinates2;
    private static Collection<GeoCoordinate> coordinates3;
    private static Collection<GeoCoordinate> coordinates4;

    @BeforeClass
    public static void setUp() {
        coordinates1 = new HashSet<>();
        coordinates1.add(GeoCoordinate.from(52.52437, 13.41053));
        coordinates1.add(GeoCoordinate.from(51.50853, -0.12574));
        coordinates1.add(GeoCoordinate.from(47.66033, 9.17582));
        coordinates1.add(GeoCoordinate.from(45.74846, 4.84671));

        coordinates2 = new HashSet<>();
        coordinates2.add(GeoCoordinate.from(40.71427, -74.00597));
        coordinates2.add(GeoCoordinate.from(35.68950, 139.69171));

        coordinates3 = new HashSet<>();
        coordinates3.add(GeoCoordinate.from(52.52437, 13.41053));

        coordinates4 = new HashSet<>();
        coordinates4.add(GeoCoordinate.from(39.00027, -105.50083));
        coordinates4.add(GeoCoordinate.from(52.16045, -0.70312));
        coordinates4.add(GeoCoordinate.from(-33, -56));
        coordinates4.add(GeoCoordinate.from(39.5, -8));
        coordinates4.add(GeoCoordinate.from(54.75844, -2.69531));
        coordinates4.add(GeoCoordinate.from(39.76, -98.5));
        coordinates4.add(GeoCoordinate.from(51.297, 1.069));
        coordinates4.add(GeoCoordinate.from(52.5, -3.5));
        coordinates4.add(GeoCoordinate.from(38.89511, -77.03637));
    }

    @Test
    public void testMidpoint() {
        GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates1);
        assertEquals(49.464867, midpoint.getLatitude(), 0.01);
        assertEquals(6.7807, midpoint.getLongitude(), 0.01);

        midpoint = GeoUtils.getMidpoint(coordinates2);
        assertEquals(69.660652, midpoint.getLatitude(), 0.01);
        assertEquals(-153.661864, midpoint.getLongitude(), 0.01);

        midpoint = GeoUtils.getMidpoint(coordinates3);
        assertEquals(GeoCoordinate.from(52.52437, 13.41053), midpoint);

        midpoint = GeoUtils.getMidpoint(coordinates4);
        assertEquals(47.703117, midpoint.getLatitude(), 0.01);
        assertEquals(-41.737184, midpoint.getLongitude(), 0.01);
    }

    @Test
    public void testCenterOfMinimumDistance() {
        GeoCoordinate center = GeoUtils.getCenterOfMinimumDistance(coordinates1);
        assertEquals(48.337076, center.getLatitude(), 0.01);
        assertEquals(7.758056, center.getLongitude(), 0.01);

        center = GeoUtils.getCenterOfMinimumDistance(coordinates3);
        assertEquals(GeoCoordinate.from(52.52437, 13.41053), center);

        center = GeoUtils.getCenterOfMinimumDistance(coordinates4);
        assertEquals(52.52425, center.getLatitude(), 0.01);
        assertEquals(-5.220439, center.getLongitude(), 0.01);
    }

    @Test
    public void testDmsToDec() {
        assertEquals(40, GeoUtils.parseDms("40°"), 0);
        assertEquals(-73.94, GeoUtils.parseDms("73°56.4′W"), 0);
        assertEquals(40.446195, GeoUtils.parseDms("40:26:46.302N"), 0.05);
        assertEquals(40.446195, GeoUtils.parseDms("40d 26′ 47″ N"), 0.05);
        assertEquals(40.446195, GeoUtils.parseDms("40°26′47″N"), 0.05);
        assertEquals(33.676176, GeoUtils.parseDms("33.676176° N"), 0.05);
        assertEquals(33.575, GeoUtils.parseDms("33°34'30\" N"), 0.05);
        assertEquals(42.443333, GeoUtils.parseDms("42° 26' 36'' N"), 0.05);
        assertEquals(42.7335, GeoUtils.parseDms("42° 44′ 0.6″ N"), 0.05);
        assertEquals(42.904722, GeoUtils.parseDms("42°54'17\" N"), 0.05);
        assertEquals(39.716667, GeoUtils.parseDms("39°43' North"), 0.05);
        assertEquals(42.904722, GeoUtils.parseDms("42°54'17\" N"), 0.05);
        assertEquals(36.466667, GeoUtils.parseDms("36º28' N"), 0.05);
    }

    @Test
    public void testGetTestPoints() {
        GeoCoordinate origin = GeoCoordinate.from(53.320556, 1.729722);
        GeoCoordinate[] points = GeoUtils.getTestPoints(origin, 100);
        for (GeoCoordinate point : points) {
            assertEquals(100, point.distance(origin), 0.001);
        }
    }

    @Test
    public void testComputeDistance() {
        collector.checkThat(GeoUtils.computeDistance(52.52437, 13.41053, 51.50853, -0.12574), Matchers.closeTo(931.75,0.05));
        System.out.println(GeoUtils.approximateDistance(52.52437, 13.41053, 51.50853, -0.12574));
    }

    @Test
    public void testApproximateDistance() {
        GeoCoordinate c1 = GeoCoordinate.from(33.662508, -95.547692);
        GeoCoordinate c2 = GeoCoordinate.from(48.85341, 2.3488);
        GeoCoordinate c3 = GeoCoordinate.from(49.265278, 4.028611);
        GeoCoordinate c4 = GeoCoordinate.from(48.858222, 2.2945);

        // should be: exact < approximate < 1.1 * exact
        double exact = c1.distance(c2);
        double approximate = GeoUtils.approximateDistance(c1, c2);
        assertTrue(exact < approximate);
        assertTrue(approximate < exact * 1.1);

        exact = c2.distance(c3);
        approximate = GeoUtils.approximateDistance(c2, c3);
        assertTrue(exact < approximate);
        assertTrue(approximate < exact * 1.1);

        exact = c2.distance(c4);
        approximate = GeoUtils.approximateDistance(c2, c4);
        assertTrue(exact < approximate);
        assertTrue(approximate < exact * 1.1);

        collector.checkThat(GeoUtils.approximateDistance(c1, c2), Matchers.is(GeoUtils.approximateDistance(c1.getLatitude(), c1.getLongitude(), c2.getLatitude(), c2.getLongitude())));

    }

    @Test
    @Ignore
    public void performanceTest() {
        GeoCoordinate c1 = GeoCoordinate.from(33.662508, -95.547692);
        GeoCoordinate c2 = GeoCoordinate.from(48.85341, 2.3488);

        StopWatch stopExact = new StopWatch();
        for (int i = 0; i < 10000000; i++) {
            c1.distance(c2);
        }
        System.out.println("Time with exact distance: " + stopExact);

        StopWatch stopApproximate = new StopWatch();
        for (int i = 0; i < 10000000; i++) {
            GeoUtils.approximateDistance(c1, c2);
        }
        System.out.println("Time with approximate distance: " + stopApproximate);
        System.out.println("Speedup: " + (double)stopExact.getElapsedTime() / stopApproximate.getElapsedTime());
        // speedup is about 90x!
    }

    @Test
    public void testValidateRange() {
        assertTrue(GeoUtils.isValidCoordinateRange(45, 175));
        assertFalse(GeoUtils.isValidCoordinateRange(45, 195));
        assertFalse(GeoUtils.isValidCoordinateRange(-95, 175));
    }

    @Test
    public void testNormalizeLatitude() {
        assertEquals(-90, GeoUtils.normalizeLatitude(-90), 0);
        assertEquals(90, GeoUtils.normalizeLatitude(90), 0);
        assertEquals(45, GeoUtils.normalizeLatitude(45), 0);
        assertEquals(90, GeoUtils.normalizeLatitude(95), 0);
        assertEquals(-90, GeoUtils.normalizeLatitude(-95), 0);
    }

    @Test
    public void testNormalizeLongitude() {
        assertEquals(-180, GeoUtils.normalizeLongitude(-180), 0);
        assertEquals(180, GeoUtils.normalizeLongitude(180), 0);
        assertEquals(0, GeoUtils.normalizeLongitude(0), 0);
        assertEquals(-175, GeoUtils.normalizeLongitude(185), 0);
        assertEquals(175, GeoUtils.normalizeLongitude(-185), 0);
    }

    @Test
    public void testGetLargestDistance() {
        assertEquals(976.3, GeoUtils.getLargestDistance(coordinates1), 0.1);
        assertEquals(10848.7, GeoUtils.getLargestDistance(coordinates2), 0.1);
        assertEquals(0, GeoUtils.getLargestDistance(coordinates3), 0.1);
        assertEquals(0, GeoUtils.getLargestDistance(Collections.<GeoCoordinate> singleton(null)), 0.1);
        assertEquals(GeoUtils.EARTH_MAX_DISTANCE_KM,
                GeoUtils.getLargestDistance(Arrays.<GeoCoordinate> asList(null, null)), 0.1);
    }
    
    @Test
    public void testGetGeohash() {
        // https://en.wikipedia.org/wiki/Geohash#Typical_and_main_usages
        GeoCoordinate coordinate = GeoCoordinate.from(42.605, -5.603);
        assertEquals("e", GeoUtils.getGeohash(coordinate, 1));
        assertEquals("ez", GeoUtils.getGeohash(coordinate, 2));
        assertEquals("ezs", GeoUtils.getGeohash(coordinate, 3));
        assertEquals("ezs4", GeoUtils.getGeohash(coordinate, 4));
        assertEquals("ezs42", GeoUtils.getGeohash(coordinate, 5));
        assertEquals("ezs42s000esks2q2dh8y", GeoUtils.getGeohash(coordinate, 20));

        GeoCoordinate coordinate2 = GeoCoordinate.from(57.64911, 10.40744);
        assertEquals("u4pruydqqvj", GeoUtils.getGeohash(coordinate2, 11));
    }

    @Test
    public void testParseGeohash() {
        GeoCoordinate coordinate = GeoUtils.parseGeohash("ezs42");
        assertEquals(42.605, coordinate.getLatitude(), 0.0001);
        assertEquals(-5.603, coordinate.getLongitude(), 0.0001);

        GeoCoordinate coordinate3 = GeoUtils.parseGeohash("ezs42s000esks2q2dh8y");
        assertEquals(42.605, coordinate3.getLatitude(), 0.0001);
        assertEquals(-5.603, coordinate3.getLongitude(), 0.0001);

        // https://en.wikipedia.org/wiki/Geohash#Digits_and_precision_in_km
        GeoCoordinate coordinate2 = GeoCoordinate.from(57.64911, 10.40744);
        assertTrue(2500 > coordinate2.distance(GeoUtils.parseGeohash("u")));
        assertTrue(630 > coordinate2.distance(GeoUtils.parseGeohash("u4")));
        assertTrue(78 > coordinate2.distance(GeoUtils.parseGeohash("u4p")));
        assertTrue(20 > coordinate2.distance(GeoUtils.parseGeohash("u4pr")));
        assertTrue(2.4 > coordinate2.distance(GeoUtils.parseGeohash("u4pru")));
        assertTrue(0.61 > coordinate2.distance(GeoUtils.parseGeohash("u4pruy")));
        assertTrue(0.076 > coordinate2.distance(GeoUtils.parseGeohash("u4pruyd")));
        assertTrue(0.019 > coordinate2.distance(GeoUtils.parseGeohash("u4pruydq")));
        assertTrue(0.002 > coordinate2.distance(GeoUtils.parseGeohash("u4pruydqq")));
        assertTrue(0.001 > coordinate2.distance(GeoUtils.parseGeohash("u4pruydqqvj")));
    }

}
