package ws.palladian.extraction.location.geocoder;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.geo.GeoCoordinate;

@Ignore
public class OpenCageGeocoderTest {

    private static final String API_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    @Test
    public void testGeocoder() throws GeocoderException {
        var geocoder = new OpenCageGeocoder(API_KEY);
        var result = geocoder.geoCode("Eiffel Tower");
        // System.out.println(result);
        assertEquals(48.858, result.getLatitude(), 0.001);
        assertEquals(2.295, result.getLongitude(), 0.001);
    }

    @Test
    public void testReverseGeocoder() throws GeocoderException {
        var coordinate = GeoCoordinate.from(48.858222, 2.2945);
        var geocoder = new OpenCageGeocoder(API_KEY);
        var result = geocoder.reverseGeoCode(coordinate);
        // System.out.println(result);
        assertEquals("5", result.getHouseNumber());
        assertEquals("Avenue Anatole France", result.getStreet());
        assertEquals("75007", result.getPostalcode());
        assertEquals("France", result.getCountry());
        assertEquals("Metropolitan France", result.getRegion());
        assertEquals("Paris", result.getCounty());
    }

}
