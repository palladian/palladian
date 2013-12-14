package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AbstractGeoCoordinateTest {

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
        assertEquals("0°,0°", new ImmutableGeoCoordinate(0., 0.).toDmsString());
        assertEquals("40°N,4°W", new ImmutableGeoCoordinate(40, -4).toDmsString());
    }

}
