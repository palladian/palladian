package ws.palladian.helper.geo;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AbstractGeoCoordinateTest {

    @Test
    public void testDistance() {
        GeoCoordinate c1 = GeoCoordinate.from(33.662508, -95.547692);
        GeoCoordinate c2 = GeoCoordinate.from(48.85341, 2.3488);
        double distance = c1.distance(c2);
        assertEquals(7783, distance, 1);
        distance = c2.distance(c1);
        assertEquals(7783, distance, 1);
    }

    @Test
    public void testBoundingBox() {
        double[] boundingBox = GeoCoordinate.from(52.52437, 13.41053).getBoundingBox(10);
        assertEquals(4, boundingBox.length);
        assertEquals(52.4343, boundingBox[0], 0.001);
        assertEquals(13.2625, boundingBox[1], 0.001);
        assertEquals(52.6144, boundingBox[2], 0.001);
        assertEquals(13.5585, boundingBox[3], 0.001);
    }

    @Test
    public void testCoordinateToDms() {
        assertEquals("51°1′59″N,13°43′59″E", GeoCoordinate.from(51.033333, 13.733333).toDmsString());
        assertEquals("0°,0°", GeoCoordinate.from(0., 0.).toDmsString());
        assertEquals("40°N,4°W", GeoCoordinate.from(40, -4).toDmsString());
    }

    @Test
    public void testGetCoordinateDistanceBearing() {
        GeoCoordinate coordinate = GeoCoordinate.from(53.320556, 1.729722).getCoordinate(124.8, 96.021667);
        assertEquals(53.188333, coordinate.getLatitude(), 0.001);
        assertEquals(3.592778, coordinate.getLongitude(), 0.001);

        coordinate = GeoCoordinate.from(28.216667, -177.366667).getCoordinate(300, 270);
        assertEquals(28.182595, coordinate.getLatitude(), 0.001);
        assertEquals(179.572172, coordinate.getLongitude(), 0.001);
    }

}
