package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static ws.palladian.extraction.location.LocationFilters.childOf;
import static ws.palladian.extraction.location.LocationFilters.descendantOf;
import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.POI;
import static ws.palladian.extraction.location.LocationType.REGION;
import static ws.palladian.extraction.location.LocationType.UNIT;
import static ws.palladian.helper.functional.Filters.equal;
import static ws.palladian.helper.functional.Filters.invert;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.geo.GeoUtils;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;

public class LocationStatsTest {

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
    public void testWherePredicates() {
        List<Location> allLocations = Arrays.asList(l1, l2, l3, l4, l5, l6, l7);
        LocationStats stats = new LocationStats(allLocations);
        assertFalse(stats.where(descendantOf(l2)).contains(l1));
        assertFalse(stats.where(childOf(l2)).contains(l1));
        assertTrue(stats.where(descendantOf(l1)).contains(l2));
        assertTrue(stats.where(childOf(l1)).contains(l2));
        assertTrue(stats.where(descendantOf(l3)).contains(l1));
        assertFalse(stats.where(descendantOf(l1)).contains(l3));
        assertFalse(stats.where(childOf(l3)).contains(l1));
        assertEquals(3, stats.where(LocationFilters.radius(l5.getCoordinate(), 100)).count());
    }

    @Test
    public void testGetLargestDistance() {
        LocationStats stats = new LocationStats(Arrays.asList(l1, l2, l4));
        assertEquals(10656, stats.largestDistance(), 1);

        stats = new LocationStats(Arrays.asList(l1, l2));
        assertEquals(2.7, stats.largestDistance(), 0.1);

        stats = new LocationStats(Arrays.asList(l1, l2, l4, l7));
        assertEquals(GeoUtils.EARTH_MAX_DISTANCE_KM, stats.largestDistance(), 0);

        stats = new LocationStats(Arrays.asList(l1));
        assertEquals(0, stats.largestDistance(), 0);

        stats = new LocationStats(Arrays.asList(l7));
        assertEquals(0, stats.largestDistance(), 0);
    }

    @Test
    public void testExcept() {
        LocationStats stats = new LocationStats(Arrays.asList(l1, l2, l3, l4));
        assertEquals(4, stats.count());
        LocationStats statsExcept = stats.where(invert(equal(l2, l3, l5, l7)));
        assertEquals(2, statsExcept.count());
        assertTrue(statsExcept.contains(l1));
        assertTrue(statsExcept.contains(l4));
        assertEquals(3, stats.where(invert(equal(l1))).count());
    }

}
