package ws.palladian.extraction.location;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GeoUtilsTest {

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
