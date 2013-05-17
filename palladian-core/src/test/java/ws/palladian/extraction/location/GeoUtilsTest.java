package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;

public class GeoUtilsTest {

    @Test
    public void testMidpoint() {
        Collection<GeoCoordinate> locations = CollectionHelper.newHashSet();
        locations.add(new ImmutableGeoCoordinate(52.52437, 13.41053));
        locations.add(new ImmutableGeoCoordinate(51.50853, -0.12574));
        locations.add(new ImmutableGeoCoordinate(47.66033, 9.17582));
        locations.add(new ImmutableGeoCoordinate(45.74846, 4.84671));
        GeoCoordinate midpoint = GeoUtils.getMidpoint(locations);
        assertEquals(49.464867, midpoint.getLatitude(), 0.01);
        assertEquals(6.7807, midpoint.getLongitude(), 0.01);

        locations = CollectionHelper.newHashSet();
        locations.add(new ImmutableGeoCoordinate(40.71427, -74.00597));
        locations.add(new ImmutableGeoCoordinate(35.68950, 139.69171));
        midpoint = GeoUtils.getMidpoint(locations);
        assertEquals(69.660652, midpoint.getLatitude(), 0.01);
        assertEquals(-153.661864, midpoint.getLongitude(), 0.01);
    }

}
