package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class GeoUtilsTest {

    @Test
    public void testDistance() {
        GeoCoordinate c1 = new ImmutableGeoCoordinate(33.662508, -95.547692);
        GeoCoordinate c2 = new ImmutableGeoCoordinate(48.85341, 2.3488);
        double distance = c1.distance(c2);
        assertEquals(7783, distance, 1);
        distance = c2.distance(c1);
        assertEquals(7783, distance, 1);
    }

    @Test
    public void testMidpoint() {
        Collection<GeoCoordinate> coordinates = CollectionHelper.newHashSet();
        coordinates.add(new ImmutableGeoCoordinate(52.52437, 13.41053));
        coordinates.add(new ImmutableGeoCoordinate(51.50853, -0.12574));
        coordinates.add(new ImmutableGeoCoordinate(47.66033, 9.17582));
        coordinates.add(new ImmutableGeoCoordinate(45.74846, 4.84671));
        GeoCoordinate midpoint = GeoUtils.getMidpoint(coordinates);
        assertEquals(49.464867, midpoint.getLatitude(), 0.01);
        assertEquals(6.7807, midpoint.getLongitude(), 0.01);

        coordinates = CollectionHelper.newHashSet();
        coordinates.add(new ImmutableGeoCoordinate(40.71427, -74.00597));
        coordinates.add(new ImmutableGeoCoordinate(35.68950, 139.69171));
        midpoint = GeoUtils.getMidpoint(coordinates);
        assertEquals(69.660652, midpoint.getLatitude(), 0.01);
        assertEquals(-153.661864, midpoint.getLongitude(), 0.01);
        
         coordinates = CollectionHelper.newHashSet();
         coordinates.add(new AbstractGeoCoordinate() {
            @Override
            public Double getLongitude() {
                return null;
            }
            @Override
            public Double getLatitude() {
                return null;
            }
        });
         midpoint = GeoUtils.getMidpoint(coordinates);
         assertEquals(0, midpoint.getLatitude(), 0.01);
         assertEquals(0, midpoint.getLongitude(), 0.01);
    }

    @Test
    public void testBoundingBox() {
        double[] boundingBox = new ImmutableGeoCoordinate(52.52437, 13.41053).getBoundingBox(10);
        assertEquals(4, boundingBox.length);
        assertEquals(52.4343, boundingBox[0], 0.001);
        assertEquals(13.2625, boundingBox[1], 0.001);
        assertEquals(52.6144, boundingBox[2], 0.001);
        assertEquals(13.5585, boundingBox[3], 0.001);
    }

    @Test
    public void testCoordinateToDms() {
        assertEquals("51°1′59″N,13°43′59″E", new ImmutableGeoCoordinate(51.033333, 13.733333).toDmsString());
        assertEquals("0°0′0″,0°0′0″", new ImmutableGeoCoordinate(0., 0.).toDmsString());
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

}
