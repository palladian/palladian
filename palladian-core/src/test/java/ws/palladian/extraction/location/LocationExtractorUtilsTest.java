package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.location.LocationExtractorUtils.LOCATION_COORDINATE_FUNCTION;
import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.POI;
import static ws.palladian.extraction.location.LocationType.REGION;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class LocationExtractorUtilsTest {

    private final Location l1 = new ImmutableLocation(2028461, "Ulaanbaatar Hot", null, UNIT,
            new ImmutableGeoCoordinate(47.91667, 106.91667), 844818l, Arrays.asList(2029969, 6255147, 6295630));
    private final Location l2 = new ImmutableLocation(2028462, "Ulaanbaatar", null, CITY, new ImmutableGeoCoordinate(
            47.90771, 106.88324), 844818l, Arrays.asList(2028461, 2029969, 6255147, 6295630));
    private final Location l3 = new ImmutableLocation(6295630, "Earth", null, REGION,
            new ImmutableGeoCoordinate(0., 0.), 6814400000l, Collections.<Integer> emptyList());
    private final Location l4 = new ImmutableLocation(4653031, "Richmond", CITY, new ImmutableGeoCoordinate(35.38563,
            -86.59194), 0l);
    private final Location l5 = new ImmutableLocation(4074277, "Madison County", UNIT, new ImmutableGeoCoordinate(
            34.73342, -86.56666), 0l);
    private final Location l6 = new ImmutableLocation(100080784, "Madison County", UNIT, new ImmutableGeoCoordinate(
            34.76583, -86.55778), null);
    private final Location l7 = new ImmutableLocation(8468884, "Fayetteville State University", POI, null, null);

    @Test
    public void testIsChildOf() {
        assertFalse(l1.descendantOf(l2));
        assertFalse(l1.childOf(l2));
        assertTrue(l2.descendantOf(l1));
        assertTrue(l2.childOf(l1));

        assertTrue(l1.descendantOf(l3));
        assertFalse(l3.descendantOf(l1));
        assertFalse(l1.childOf(l3));
    }

    @Test
    public void testDifferentNames() {
        assertTrue(LocationExtractorUtils.differentNames(Arrays.asList(l4, l5, l6)));
        assertFalse(LocationExtractorUtils.differentNames(Arrays.asList(l5, l6)));
    }

    @Test
    public void testGetLargestDistance() {
        List<Location> locations = Arrays.asList(l1, l2, l4);
        List<GeoCoordinate> coordinates = CollectionHelper.convertList(locations, LOCATION_COORDINATE_FUNCTION);
        assertEquals(10656, LocationExtractorUtils.getLargestDistance(coordinates), 1);
        assertFalse(LocationExtractorUtils.largestDistanceBelow(50, coordinates));

        locations = Arrays.asList(l1, l2);
        coordinates = CollectionHelper.convertList(locations, LOCATION_COORDINATE_FUNCTION);
        assertEquals(2.7, LocationExtractorUtils.getLargestDistance(coordinates), 0.1);
        assertTrue(LocationExtractorUtils.largestDistanceBelow(50, coordinates));

        locations = Arrays.asList(l1, l2, l4, l7);
        coordinates = CollectionHelper.convertList(locations, LOCATION_COORDINATE_FUNCTION);
        assertEquals(Double.MAX_VALUE, LocationExtractorUtils.getLargestDistance(coordinates), 0);
        assertFalse(LocationExtractorUtils.largestDistanceBelow(50, coordinates));
    }

}
