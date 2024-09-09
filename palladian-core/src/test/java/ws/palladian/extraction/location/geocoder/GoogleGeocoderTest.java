package ws.palladian.extraction.location.geocoder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class GoogleGeocoderTest {

    @Test
    public void testJsonParsing() throws GeocoderException {
        var result = "{ \"results\" : [ { \"address_components\" : [ { \"long_name\" : \"Brobjerg Parkvej\", \"short_name\" : \"Brobjerg Parkvej\", \"types\" : [ \"route\" ] }, { \"long_name\" : \"Egå\", \"short_name\" : \"Egå\", \"types\" : [ \"locality\", \"political\" ] }, { \"long_name\" : \"Denmark\", \"short_name\" : \"DK\", \"types\" : [ \"country\", \"political\" ] }, { \"long_name\" : \"8250\", \"short_name\" : \"8250\", \"types\" : [ \"postal_code\" ] } ], \"formatted_address\" : \"Brobjerg Parkvej, 8250 Egå, Denmark\", \"geometry\" : { \"bounds\" : { \"northeast\" : { \"lat\" : 56.2064974, \"lng\" : 10.2671539 }, \"southwest\" : { \"lat\" : 56.202914, \"lng\" : 10.2569818 } }, \"location\" : { \"lat\" : 56.204817, \"lng\" : 10.2615945 }, \"location_type\" : \"GEOMETRIC_CENTER\", \"viewport\" : { \"northeast\" : { \"lat\" : 56.2064974, \"lng\" : 10.2671539 }, \"southwest\" : { \"lat\" : 56.202914, \"lng\" : 10.2569818 } } }, \"place_id\" : \"ChIJ9bly6Iw-TEYRTu_aMUfdQGc\", \"types\" : [ \"route\" ] } ], \"status\" : \"OK\" }";
        var parsed = GoogleGeocoder.parseJson(result);
        assertEquals(parsed.getLatitude(), 56.205, 0.001);
        assertEquals(parsed.getLongitude(), 10.262, 0.001);
    }

}
