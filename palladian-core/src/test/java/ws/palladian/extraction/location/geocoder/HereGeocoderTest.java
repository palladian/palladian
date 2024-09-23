package ws.palladian.extraction.location.geocoder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HereGeocoderTest {

    @Test
    public void testParseJson() throws GeocoderException {
        var json = "{\"items\":[{\"title\":\"Invalidenstraße 117, 10115 Berlin, Deutschland\",\"id\":\"here:af:streetsection:tVuvjJYhO86yd5jk1cmzNB:CgcIBCCf2912EAEaAzExNw\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"Invalidenstraße 117, 10115 Berlin, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"BE\",\"state\":\"Berlin\",\"countyCode\":\"B\",\"county\":\"Berlin\",\"city\":\"Berlin\",\"district\":\"Mitte\",\"street\":\"Invalidenstraße\",\"postalCode\":\"10115\",\"houseNumber\":\"117\"},\"position\":{\"lat\":52.53041,\"lng\":13.38527},\"access\":[{\"lat\":52.53105,\"lng\":13.3848}],\"mapView\":{\"west\":13.38379,\"south\":52.52951,\"east\":13.38675,\"north\":52.53131},\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"city\":1.0,\"streets\":[1.0],\"houseNumber\":1.0}}}]}";
        var result = HereGeocoder.parseGeoCodeResult(json);
        assertEquals(52.53041, result.getLeft().getLatitude(), 0.0001);
        assertEquals(13.38527, result.getLeft().getLongitude(), 0.0001);

        var json2 = "{\"items\":[{\"title\":\"Route de la Télécabine 67, 1997 Nendaz Valais, Suisse\",\"id\":\"here:af:streetsection:9z62iGVQH0LlHrihOC9UMD:CgcIBCDLq9J-EAEaAjY3\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"Route de la Télécabine 67, 1997 Nendaz Valais, Suisse\",\"countryCode\":\"CHE\",\"countryName\":\"Suisse\",\"stateCode\":\"VS\",\"state\":\"Valais\",\"county\":\"Conthey\",\"city\":\"Nendaz\",\"district\":\"Haute-Nendaz\",\"street\":\"Route de la Télécabine\",\"postalCode\":\"1997\",\"houseNumber\":\"67\"},\"position\":{\"lat\":46.18017,\"lng\":7.29134},\"access\":[{\"lat\":46.18033,\"lng\":7.29122}],\"distance\":12,\"mapView\":{\"west\":7.29039,\"south\":46.17957,\"east\":7.29552,\"north\":46.18338}}]}";
        var result2 = HereGeocoder.parseGeoCodeResult(json2);
        assertEquals("Route de la Télécabine 67, 1997 Nendaz Valais, Suisse", result2.getRight().getLabel());
        assertEquals("Suisse", result2.getRight().getCountry());
        assertEquals("Conthey", result2.getRight().getCounty());
        // assertEquals("Nendaz", result2.getRight().getCity());
        assertEquals("Route de la Télécabine", result2.getRight().getStreet());
        assertEquals("1997", result2.getRight().getPostalcode());
        assertEquals("67", result2.getRight().getHouseNumber());
    }

}
