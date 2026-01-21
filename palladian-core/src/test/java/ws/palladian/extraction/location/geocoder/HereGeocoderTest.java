package ws.palladian.extraction.location.geocoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class HereGeocoderTest {

    @Test
    public void testParseJson() throws GeocoderException {
        // Eiffel Tower
        var json1 = "{\"items\":[{\"title\":\"Tour Eiffel\",\"id\":\"here:pds:place:250u09tu-4561b8da952f4fd79c4e1998c3fcf032\",\"resultType\":\"place\",\"address\":{\"label\":\"Tour Eiffel, 5 Avenue Anatole France, 75007 Paris, France\",\"countryCode\":\"FRA\",\"countryName\":\"France\",\"stateCode\":\"IDF\",\"state\":\"Île-de-France\",\"county\":\"Paris\",\"city\":\"Paris\",\"district\":\"7e Arrondissement\",\"street\":\"Avenue Anatole France\",\"postalCode\":\"75007\",\"houseNumber\":\"5\"},\"position\":{\"lat\":48.85824,\"lng\":2.2945},\"access\":[{\"lat\":48.8589,\"lng\":2.29328}],\"categories\":[{\"id\":\"300-3000-0025\",\"name\":\"Monument historique\",\"primary\":true},{\"id\":\"300-3000-0000\",\"name\":\"Lieu d'intérêt/Attraction\"},{\"id\":\"300-3000-0023\",\"name\":\"Attraction touristique\"},{\"id\":\"800-8600-0180\",\"name\":\"Stade ou complexe sportif\"}],\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"placeName\":1.0}}}]}";
        var result1 = HereGeocoder.parseJson(json1);
        System.out.println(result1);
        assertEquals("5", result1.getHouseNumber());
        assertEquals("Avenue Anatole France", result1.getStreet());
        assertEquals("75007", result1.getPostalcode());
        assertEquals("France", result1.getCountry());
        assertEquals("Paris", result1.getCounty());
        assertEquals("Tour Eiffel, 5 Avenue Anatole France, 75007 Paris, France", result1.getLabel());
        assertEquals("Île-de-France", result1.getState());
        assertEquals("Paris", result1.getCity());
        assertEquals("7e Arrondissement", result1.getCityDistrict());
        // ((48.858,2.295),Place [houseNumber=5, street=Avenue Anatole France, postalcode=75007, country=France, county=Paris, label=Tour Eiffel, 5 Avenue Anatole France, 75007 Paris, France, state=Île-de-France, city=Paris, cityDistrict=7e Arrondissement, ])
        assertEquals(48.858,result1.getCoordinate().getLatitude(),0.001);
        assertEquals(2.295,result1.getCoordinate().getLongitude(),0.001);

        // Montmartre, Paris
        var json2 = "{\"items\":[{\"title\":\"Montmartre, Paris, Île-de-France, France\",\"id\":\"here:cm:namedplace:23009867\",\"resultType\":\"locality\",\"localityType\":\"district\",\"address\":{\"label\":\"Montmartre, Paris, Île-de-France, France\",\"countryCode\":\"FRA\",\"countryName\":\"France\",\"stateCode\":\"IDF\",\"state\":\"Île-de-France\",\"county\":\"Paris\",\"city\":\"Paris\",\"district\":\"Montmartre\",\"postalCode\":\"75018\"},\"position\":{\"lat\":48.88617,\"lng\":2.33796},\"mapView\":{\"west\":2.32946,\"south\":48.88193,\"east\":2.34797,\"north\":48.8898},\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"city\":1.0,\"district\":1.0}}}]}";
        var result2 = HereGeocoder.parseJson(json2);
        System.out.println(result2);
        assertEquals("75018", result2.getPostalcode());
        assertEquals("France", result2.getCountry());
        assertEquals("Paris", result2.getCounty());
        assertEquals("Montmartre, Paris, Île-de-France, France", result2.getLabel());
        assertEquals("Île-de-France", result2.getState());
        assertEquals("Paris", result2.getCity());
        assertEquals("Montmartre", result2.getCityDistrict());
        // ((48.886,2.338),Place [postalcode=75018, country=France, county=Paris, label=Montmartre, Paris, Île-de-France, France, state=Île-de-France, city=Paris, cityDistrict=Montmartre, ])

        // 0,0
        var json3 = "{\"items\":[]}";
        var result3 = HereGeocoder.parseJson(json3);
        // System.out.println(result3);
        assertNull(result3);

        // Zugspitze
        var json4 = "{\"items\":[{\"title\":\"Zugspitze\",\"id\":\"here:pds:place:276u0rvf-4368f26a26f84fc580f0a2e85e9cfb59\",\"resultType\":\"place\",\"address\":{\"label\":\"Zugspitze, Seefeldweg, 82491 Grainau, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"BY\",\"state\":\"Bayern\",\"countyCode\":\"GAP\",\"county\":\"Garmisch-Partenkirchen\",\"city\":\"Grainau\",\"district\":\"Eibsee\",\"street\":\"Seefeldweg\",\"postalCode\":\"82491\"},\"position\":{\"lat\":47.45661,\"lng\":10.99403},\"access\":[{\"lat\":47.45658,\"lng\":10.99384}],\"categories\":[{\"id\":\"300-3000-0023\",\"name\":\"Touristenattraktion\",\"primary\":true},{\"id\":\"350-3550-0336\",\"name\":\"Natürlich oder geografisch\"}],\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"placeName\":1.0}}}]}";
        var result4 = HereGeocoder.parseJson(json4);
        System.out.println(result4);
        // ((47.457,10.994),Place [street=Seefeldweg, postalcode=82491, country=Deutschland, county=Garmisch-Partenkirchen, label=Zugspitze, Seefeldweg, 82491 Grainau, Deutschland, state=Bayern, city=Grainau, cityDistrict=Eibsee, ])
        assertEquals("Seefeldweg", result4.getStreet());
        assertEquals("82491", result4.getPostalcode());
        assertEquals("Deutschland", result4.getCountry());
        assertEquals("Garmisch-Partenkirchen", result4.getCounty());
        assertEquals("Zugspitze, Seefeldweg, 82491 Grainau, Deutschland", result4.getLabel());
        assertEquals("Bayern", result4.getState());
        assertEquals("Grainau", result4.getCity());
        assertEquals("Eibsee", result4.getCityDistrict());

        // 1 Infinite Loop, Cupertino
        var json5 = "{\"items\":[{\"title\":\"1 Infinite Loop, Cupertino, CA 95014-2083, United States\",\"id\":\"here:af:streetsection:kH-3XMcPVNqQBqrs8md1zC:CgcIBCD5x_lPEAEaATE\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"1 Infinite Loop, Cupertino, CA 95014-2083, United States\",\"countryCode\":\"USA\",\"countryName\":\"United States\",\"stateCode\":\"CA\",\"state\":\"California\",\"county\":\"Santa Clara\",\"city\":\"Cupertino\",\"street\":\"Infinite Loop\",\"postalCode\":\"95014-2083\",\"houseNumber\":\"1\"},\"position\":{\"lat\":37.33177,\"lng\":-122.03042},\"access\":[{\"lat\":37.33178,\"lng\":-122.0308}],\"mapView\":{\"west\":-122.03155,\"south\":37.33087,\"east\":-122.02929,\"north\":37.33267},\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"city\":1.0,\"streets\":[1.0],\"houseNumber\":1.0}}}]}";
        var result5 = HereGeocoder.parseJson(json5);
        System.out.println(result5);
        // ((37.332,-122.03),Place [houseNumber=1, street=Infinite Loop, postalcode=95014-2083, country=United States, county=Santa Clara, label=1 Infinite Loop, Cupertino, CA 95014-2083, United States, state=California, city=Cupertino, ])
        assertEquals("1", result5.getHouseNumber());
        assertEquals("Infinite Loop", result5.getStreet());
        assertEquals("95014-2083", result5.getPostalcode());
        assertEquals("United States", result5.getCountry());
        assertEquals("Santa Clara", result5.getCounty());
        assertEquals("1 Infinite Loop, Cupertino, CA 95014-2083, United States", result5.getLabel());
        assertEquals("California", result5.getState());
        assertEquals("Cupertino", result5.getCity());

        // 50.960906,14.075632
        // (https://de.wikipedia.org/wiki/Tiedgestein)
        var json6 = "{\"items\":[{\"title\":\"Basteiweg, 01824 Rathen, Deutschland\",\"id\":\"here:af:streetsection:5Khugt3YaDVgK4UukaH.6C\",\"resultType\":\"street\",\"address\":{\"label\":\"Basteiweg, 01824 Rathen, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"SN\",\"state\":\"Sachsen\",\"countyCode\":\"PIR\",\"county\":\"Sächsische Schweiz-Osterzgebirge\",\"city\":\"Rathen\",\"street\":\"Basteiweg\",\"postalCode\":\"01824\"},\"position\":{\"lat\":50.96112,\"lng\":14.07589},\"distance\":30,\"mapView\":{\"west\":14.07578,\"south\":50.95895,\"east\":14.08141,\"north\":50.96127}}]}";
        var result6 = HereGeocoder.parseJson(json6);
        System.out.println(result6);
        // ((50.961,14.076),Place [street=Basteiweg, postalcode=01824, country=Deutschland, county=Sächsische Schweiz-Osterzgebirge, label=Basteiweg, 01824 Rathen, Deutschland, state=Sachsen, city=Rathen, ])
        assertEquals("Basteiweg", result6.getStreet());
        assertEquals("01824", result6.getPostalcode());
        assertEquals("Deutschland", result6.getCountry());
        assertEquals("Sächsische Schweiz-Osterzgebirge", result6.getCounty());
        assertEquals("Basteiweg, 01824 Rathen, Deutschland", result6.getLabel());
        assertEquals("Sachsen", result6.getState());
        assertEquals("Rathen", result6.getCity());

        // Adam's Peak
        var json7 = "{\"items\":[{\"title\":\"Adams Point, Oakland, CA, United States\",\"id\":\"here:cm:namedplace:23027556\",\"resultType\":\"locality\",\"localityType\":\"district\",\"address\":{\"label\":\"Adams Point, Oakland, CA, United States\",\"countryCode\":\"USA\",\"countryName\":\"United States\",\"stateCode\":\"CA\",\"state\":\"California\",\"county\":\"Alameda\",\"city\":\"Oakland\",\"district\":\"Adams Point\",\"postalCode\":\"94610\"},\"position\":{\"lat\":37.81201,\"lng\":-122.25536},\"mapView\":{\"west\":-122.26145,\"south\":37.80478,\"east\":-122.24902,\"north\":37.81718},\"scoring\":{\"queryScore\":0.67,\"fieldScore\":{\"district\":0.81}}}]}";
        var result7 = HereGeocoder.parseJson(json7);
        System.out.println(result7);
        // ((37.812,-122.255),Place [postalcode=94610, country=United States, county=Alameda, label=Adams Point, Oakland, CA, United States, state=California, city=Oakland, cityDistrict=Adams Point, ])
        assertEquals("94610", result7.getPostalcode());
        assertEquals("United States", result7.getCountry());
        assertEquals("Alameda", result7.getCounty());
        assertEquals("Adams Point, Oakland, CA, United States", result7.getLabel());
        assertEquals("California", result7.getState());
        assertEquals("Oakland", result7.getCity());
        assertEquals("Adams Point", result7.getCityDistrict());

        // Royal Observatory, Greenwich
        var json8 = "{\"items\":[{\"title\":\"Old Royal Observatory\",\"id\":\"here:pds:place:826gcpuz-c6aa342c631640e2bda3d9dcd21a013c\",\"resultType\":\"place\",\"address\":{\"label\":\"Old Royal Observatory, Blackheath Avenue, London, SE10 8, United Kingdom\",\"countryCode\":\"GBR\",\"countryName\":\"United Kingdom\",\"state\":\"England\",\"countyCode\":\"LDN\",\"county\":\"London\",\"city\":\"London\",\"district\":\"Greenwich\",\"street\":\"Blackheath Avenue\",\"postalCode\":\"SE10 8\"},\"position\":{\"lat\":51.47783,\"lng\":-0.00143},\"access\":[{\"lat\":51.47701,\"lng\":-1.9E-4}],\"categories\":[{\"id\":\"300-3000-0025\",\"name\":\"Historical Monument\",\"primary\":true}],\"scoring\":{\"queryScore\":0.99,\"fieldScore\":{\"district\":1.0,\"placeName\":0.84}}}]}";
        var result8 = HereGeocoder.parseJson(json8);
        System.out.println(result8);
        // ((51.478,-0.001),Place [street=Blackheath Avenue, postalcode=SE10 8, country=United Kingdom, county=London, label=Old Royal Observatory, Blackheath Avenue, London, SE10 8, United Kingdom, state=England, city=London, cityDistrict=Greenwich, ])
        assertEquals("Blackheath Avenue", result8.getStreet());
        assertEquals("SE10 8", result8.getPostalcode());
        assertEquals("United Kingdom", result8.getCountry());
        assertEquals("London", result8.getCounty());
        assertEquals("England", result8.getState());
        assertEquals("London", result8.getCity());
        assertEquals("Greenwich", result8.getCityDistrict());

        var json9 = "{\"items\":[{\"title\":\"Schulstraße 35, 74223 Flein, Deutschland\",\"id\":\"here:af:streetsection:.d5PArJv4XJkwpXQLEU3fB:CgcIBCC7hKdVEAEaAjM1\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"Schulstraße 35, 74223 Flein, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"BW\",\"state\":\"Baden-Württemberg\",\"countyCode\":\"HN\",\"county\":\"Heilbronn (Landkreis)\",\"city\":\"Flein\",\"district\":\"Flein\",\"street\":\"Schulstraße\",\"postalCode\":\"74223\",\"houseNumber\":\"35\"},\"position\":{\"lat\":49.09971,\"lng\":9.21259},\"access\":[{\"lat\":49.09979,\"lng\":9.21272}],\"mapView\":{\"west\":9.21122,\"south\":49.09881,\"east\":9.21396,\"north\":49.10061},\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"city\":1.0,\"streets\":[1.0],\"houseNumber\":1.0,\"postalCode\":1.0}}}]}";
        var result9 = HereGeocoder.parseJson(json9);
        System.out.println(result9);
        // ((49.1,9.213),Place [houseNumber=35, street=Schulstraße, postalcode=74223, country=Deutschland, county=Heilbronn (Landkreis), label=Schulstraße 35, 74223 Flein, Deutschland, state=Baden-Württemberg, city=Flein, cityDistrict=Flein, ])

        assertEquals("35", result9.getHouseNumber());
        assertEquals("Schulstraße", result9.getStreet());
        assertEquals("74223", result9.getPostalcode());
        assertEquals("Deutschland", result9.getCountry());
        assertEquals("Heilbronn (Landkreis)", result9.getCounty());
        assertEquals("Schulstraße 35, 74223 Flein, Deutschland", result9.getLabel());
        assertEquals("Baden-Württemberg", result9.getState());
        assertEquals("Flein", result9.getCity());
        assertEquals("Flein", result9.getCityDistrict());
        // assertEquals("Europe", result9.getContinent());
        // assertEquals("Verwaltungsverband Flein-Talheim", result9.getMunicipality());
        // assertEquals("European Union", result9.getPoliticalUnion());

        //Altes Theater, Sontheim, Heilbronn
        var json10 = "{\"items\":[{\"title\":\"Altes Theater Heilbronn\",\"id\":\"here:pds:place:276u0wx3-d50dc589f6bd41eabf02b07127225209\",\"resultType\":\"place\",\"address\":{\"label\":\"Altes Theater Heilbronn, Lauffener Straße 2, 74081 Heilbronn, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"BW\",\"state\":\"Baden-Württemberg\",\"countyCode\":\"HN\",\"county\":\"Heilbronn (Stadt)\",\"city\":\"Heilbronn\",\"district\":\"Sontheim\",\"street\":\"Lauffener Straße\",\"postalCode\":\"74081\",\"houseNumber\":\"2\"},\"position\":{\"lat\":49.11728,\"lng\":9.18858},\"access\":[{\"lat\":49.11721,\"lng\":9.18863}],\"categories\":[{\"id\":\"500-5000-0053\",\"name\":\"Hotel\",\"primary\":true},{\"id\":\"100-1000-0000\",\"name\":\"Restaurant\"},{\"id\":\"200-2000-0015\",\"name\":\"Live-Entertainment/Livemusik\"},{\"id\":\"200-2200-0000\",\"name\":\"Theater, Musik und Kultur\"},{\"id\":\"200-2200-0020\",\"name\":\"Theater\"}],\"foodTypes\":[{\"id\":\"302-000\",\"name\":\"Deutsch\",\"primary\":true}],\"scoring\":{\"queryScore\":1.0,\"fieldScore\":{\"city\":1.0,\"district\":1.0,\"placeName\":1.0}}}]}";
        var result10 = HereGeocoder.parseJson(json10);
        System.out.println(result10);
        // ((49.117,9.189),Place [houseNumber=2, street=Lauffener Straße, postalcode=74081, country=Deutschland, county=Heilbronn (Stadt), label=Altes Theater Heilbronn, Lauffener Straße 2, 74081 Heilbronn, Deutschland, state=Baden-Württemberg, city=Heilbronn, cityDistrict=Sontheim, ])
        assertEquals("2", result10.getHouseNumber());
        assertEquals("Lauffener Straße", result10.getStreet());
        assertEquals("74081", result10.getPostalcode());
        assertEquals("Deutschland", result10.getCountry());
        assertEquals("Heilbronn (Stadt)", result10.getCounty());
        assertEquals("Altes Theater Heilbronn, Lauffener Straße 2, 74081 Heilbronn, Deutschland", result10.getLabel());
        assertEquals("Baden-Württemberg", result10.getState());
        assertEquals("Heilbronn", result10.getCity());
        assertEquals("Sontheim", result10.getCityDistrict());

        // 52.5309,13.3847
        var json11 = "{\"items\":[{\"title\":\"Invalidenstraße 116, 10115 Berlin, Deutschland\",\"id\":\"here:af:streetsection:tVuvjJYhO86yd5jk1cmzNB:CgcIBCCE59BeEAEaAzExNg\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"Invalidenstraße 116, 10115 Berlin, Deutschland\",\"countryCode\":\"DEU\",\"countryName\":\"Deutschland\",\"stateCode\":\"BE\",\"state\":\"Berlin\",\"countyCode\":\"B\",\"county\":\"Berlin\",\"city\":\"Berlin\",\"district\":\"Mitte\",\"street\":\"Invalidenstraße\",\"postalCode\":\"10115\",\"houseNumber\":\"116\"},\"position\":{\"lat\":52.53086,\"lng\":13.38469},\"access\":[{\"lat\":52.531,\"lng\":13.38461}],\"distance\":4,\"mapView\":{\"west\":13.37418,\"south\":52.5279,\"east\":13.39892,\"north\":52.53262}}]}";
        var result11 = HereGeocoder.parseJson(json11);
        System.out.println(result11);
        // Place [houseNumber=116, street=Invalidenstraße, postalcode=10115, country=Deutschland, county=Berlin, label=Invalidenstraße 116, 10115 Berlin, Deutschland, state=Berlin, city=Berlin, cityDistrict=Mitte, ]
        assertEquals("116", result11.getHouseNumber());
        assertEquals("Invalidenstraße", result11.getStreet());
        assertEquals("10115", result11.getPostalcode());
        assertEquals("Deutschland", result11.getCountry());
        assertEquals("Berlin", result11.getCounty());
        assertEquals("Invalidenstraße 116, 10115 Berlin, Deutschland", result11.getLabel());
        assertEquals("Berlin", result11.getState());
        assertEquals("Berlin", result11.getCity());
        assertEquals("Mitte", result11.getCityDistrict());

        // 46.1802980,7.2913685
        var json12 = "{\"items\":[{\"title\":\"Route de la Télécabine 67, 1997 Nendaz Valais, Suisse\",\"id\":\"here:af:streetsection:9z62iGVQH0LlHrihOC9UMD:CgcIBCDLq9J-EAEaAjY3\",\"resultType\":\"houseNumber\",\"houseNumberType\":\"PA\",\"address\":{\"label\":\"Route de la Télécabine 67, 1997 Nendaz Valais, Suisse\",\"countryCode\":\"CHE\",\"countryName\":\"Suisse\",\"stateCode\":\"VS\",\"state\":\"Valais\",\"county\":\"Conthey\",\"city\":\"Nendaz\",\"district\":\"Haute-Nendaz\",\"street\":\"Route de la Télécabine\",\"postalCode\":\"1997\",\"houseNumber\":\"67\"},\"position\":{\"lat\":46.18017,\"lng\":7.29134},\"access\":[{\"lat\":46.18033,\"lng\":7.29122}],\"distance\":12,\"mapView\":{\"west\":7.29039,\"south\":46.17957,\"east\":7.29552,\"north\":46.18338}}]}";
        var result12 = HereGeocoder.parseJson(json12);
        System.out.println(result12);
        // Place [houseNumber=67, street=Route de la Télécabine, postalcode=1997, country=Suisse, county=Conthey, label=Route de la Télécabine 67, 1997 Nendaz Valais, Suisse, state=Valais, city=Nendaz, cityDistrict=Haute-Nendaz, ]
        assertEquals("67", result12.getHouseNumber());
        assertEquals("Route de la Télécabine", result12.getStreet());
        assertEquals("1997", result12.getPostalcode());
        assertEquals("Suisse", result12.getCountry());
        assertEquals("Conthey", result12.getCounty());
        assertEquals("Route de la Télécabine 67, 1997 Nendaz Valais, Suisse", result12.getLabel());
        assertEquals("Valais", result12.getState());
        assertEquals("Nendaz", result12.getCity());
        assertEquals("Haute-Nendaz", result12.getCityDistrict());
    }

}
