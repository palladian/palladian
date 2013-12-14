package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.BeforeClass;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class GeoUtilsTest {

    private static Collection<GeoCoordinate> coordinates1;
    private static Collection<GeoCoordinate> coordinates2;

    @BeforeClass
    public static void setUp() {
        coordinates1 = CollectionHelper.newHashSet();
        coordinates1.add(new ImmutableGeoCoordinate(52.52437, 13.41053));
        coordinates1.add(new ImmutableGeoCoordinate(51.50853, -0.12574));
        coordinates1.add(new ImmutableGeoCoordinate(47.66033, 9.17582));
        coordinates1.add(new ImmutableGeoCoordinate(45.74846, 4.84671));

        coordinates2 = CollectionHelper.newHashSet();
        coordinates2.add(new ImmutableGeoCoordinate(40.71427, -74.00597));
        coordinates2.add(new ImmutableGeoCoordinate(35.68950, 139.69171));
    }

    @Test
    public void testMidpoint() {
        GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates1);
        assertEquals(49.464867, midpoint.getLatitude(), 0.01);
        assertEquals(6.7807, midpoint.getLongitude(), 0.01);

        midpoint = GeoUtils.getMidpoint(coordinates2);
        assertEquals(69.660652, midpoint.getLatitude(), 0.01);
        assertEquals(-153.661864, midpoint.getLongitude(), 0.01);
    }

    @Test
    public void testCenterOfMinimumDistance() {
        GeoCoordinate center = GeoUtils.getCenterOfMinimumDistance(coordinates1);
        assertEquals(48.337076, center.getLatitude(), 0.01);
        assertEquals(7.758056, center.getLongitude(), 0.01);
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
    }

    @Test
    public void testGetTestPoints() {
        GeoCoordinate origin = new ImmutableGeoCoordinate(53.320556, 1.729722);
        GeoCoordinate[] points = GeoUtils.getTestPoints(origin, 100);
        for (GeoCoordinate point : points) {
            assertEquals(100, point.distance(origin), 0.001);
        }
    }

    @Test
    public void testGetCoordinateDistanceBearing() {
        GeoCoordinate coordinate = GeoUtils.getCoordinate(new ImmutableGeoCoordinate(53.320556, 1.729722), 124.8,
                96.021667);
        assertEquals(53.188333, coordinate.getLatitude(), 0.001);
        assertEquals(3.592778, coordinate.getLongitude(), 0.001);
    }

}
