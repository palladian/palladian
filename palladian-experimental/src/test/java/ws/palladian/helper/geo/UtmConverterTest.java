package ws.palladian.helper.geo;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class UtmConverterTest {

    @Test
    public void testToUtm() {
        UtmCoordinate utm = UtmConverter.toUtm(GeoCoordinate.from(51.049259, 13.73836));
        assertEquals(33, utm.getZone());
        assertEquals('U', utm.getBand());
        assertEquals(411566.4905930299, utm.getEasting(), 0.00001);
        assertEquals(5656059.942193874, utm.getNorthing(), 0.00001);
        assertEquals("33U 411566 5656060", utm.toString());
        assertEquals("33U", utm.getGridZone());
    }

    @Test
    public void testToLatLon() {
        GeoCoordinate result = UtmConverter.toLatLon(411566, 5656059, 33, false);
        assertEquals(51.049259, result.getLatitude(), 0.0001);
        assertEquals(13.73836, result.getLongitude(), 0.0001);
    }

    @Test
    public void testUtmZone() {
        assertEquals(33, UtmConverter.utmZone(GeoCoordinate.from(51.049259, 13.73836)));
    }

    @Test
    public void testUtmBand() {
        assertEquals('C', UtmConverter.utmBand(-75));
        assertEquals('K', UtmConverter.utmBand(-23));
        assertEquals('J', UtmConverter.utmBand(-31));
    }

    @Test
    public void testGridZoneToCoordinate() {
        for (int zone = 1; zone <= 60; zone++) {
            for (char band : UtmConverter.UTM_BAND_CHARS.toCharArray()) {
                String gridZone = zone + "" + band;
                if (Arrays.asList("32X", "34X", "36X").contains(gridZone)) {
                    continue; // does not exist
                }
                GeoCoordinate coordinate = UtmConverter.gridZoneToLatLon(gridZone);
                UtmCoordinate utm = UtmConverter.toUtm(coordinate);
                assertEquals(utm.getGridZone(), gridZone);
            }
        }
    }

    @Test
    public void testGridZoneToCoordinateIllegalArguments() {
        try {
            UtmConverter.gridZoneToLatLon("");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            UtmConverter.gridZoneToLatLon("0C");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            UtmConverter.gridZoneToLatLon("61C");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            UtmConverter.gridZoneToLatLon("1A");
            fail();
        } catch (IllegalArgumentException e) {
        }
        try {
            UtmConverter.gridZoneToLatLon("1Z");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }

}
