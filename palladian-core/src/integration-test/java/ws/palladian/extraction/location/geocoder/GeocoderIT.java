package ws.palladian.extraction.location.geocoder;

import org.apache.commons.configuration.Configuration;
import org.junit.BeforeClass;
import org.junit.Test;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.integrationtests.ITHelper;

import javax.naming.ConfigurationException;

import static org.junit.Assert.*;

public class GeocoderIT {

    private static final String TEST_ADDRESS = "3 Abbey Rd, London NW8 9AY";

    private static final GeoCoordinate EXPECTED_COORDINATE = GeoCoordinate.from(51.5319953, -0.1782557);

    private static Configuration config;

    @BeforeClass
    public static void readConfiguration() throws ConfigurationException {
        config = ITHelper.getTestConfig();
    }

    @Test
    public void testGoogleGeocoder() {
        test(new GoogleGeocoder());
    }

    @Test
    public void testMapQuestGeocoder() {
        test(new MapQuestGeocoder(config));
    }

    @Test
    public void testMapzenGeocoder() {
        test(new MapzenGeocoder(config));
    }

    private static void test(Geocoder geocoder) {
        try {
            GeoCoordinate resultCoordinate = geocoder.geoCode(TEST_ADDRESS);
            assertNotNull(resultCoordinate);
            assertDistanceBelow(EXPECTED_COORDINATE, resultCoordinate, 0.1);
        } catch (GeocoderException e) {
            fail("failed with " + e);
        }
    }

    private static void assertDistanceBelow(GeoCoordinate expected, GeoCoordinate actual, double distance) {
        double actualDistance = expected.distance(actual);
        assertTrue("expected distance below " + distance + " km, but was " + actualDistance + " km", actualDistance < distance);
    }

}
