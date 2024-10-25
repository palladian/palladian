package ws.palladian.extraction.location.geocoder;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.geo.GeoCoordinate;

public class GeonamesGeocoderTest {
	
	@Test
	public void testParseJson() throws GeocoderException {
		// Eiffel Tower
		var json1 = "{\"address\":{\"adminCode2\":\"079\",\"adminCode3\":\"\",\"adminCode1\":\"TN\",\"lng\":\"-88.29646\",\"houseNumber\":\"\",\"locality\":\"Paris\",\"adminCode4\":\"\",\"adminName2\":\"Henry\",\"street\":\"Eiffel Tower Ln\",\"postalcode\":\"38242\",\"countryCode\":\"US\",\"adminName1\":\"Tennessee\",\"lat\":\"36.2932\"}}";
		// var result1 = GeonamesGeocoder.parseJson(json1);
		// System.out.println(result1);
		
		// Montmartre, Paris
		var json2 = "{\"address\":{\"adminCode2\":\"75\",\"adminCode3\":\"751\",\"adminCode1\":\"11\",\"lng\":\"2.34411\",\"houseNumber\":\"2\",\"locality\":\"Paris 2e Arrondissement\",\"adminCode4\":\"75056\",\"adminName4\":\"Paris\",\"adminName3\":\"Paris\",\"adminName2\":\"Paris\",\"street\":\"Cité Montmartre\",\"postalcode\":\"75002\",\"countryCode\":\"FR\",\"adminName1\":\"Île-de-France\",\"lat\":\"48.86585\"}}";
		var result2 = GeonamesGeocoder.parseJson(json2);
		assertEquals("2",result2.getHouseNumber());
		assertEquals("Cité Montmartre",result2.getStreet());
		assertEquals("75002",result2.getPostalcode());
		assertEquals("Île-de-France",result2.getState());
		assertEquals("Paris 2e Arrondissement",result2.getCity());
		assertEquals(48.866, result2.getCoordinate().getLatitude(),0.001);
		assertEquals(2.344, result2.getCoordinate().getLongitude(),0.001);
		// System.out.println(result2);
		
		// 0,0
		var json3 = "{\"geonames\":[{\"lng\":\"0\",\"distance\":\"0\",\"geonameId\":6295630,\"name\":\"Earth\",\"fclName\":\"parks,area, ...\",\"toponymName\":\"Earth\",\"fcodeName\":\"area\",\"adminName1\":\"\",\"lat\":\"0\",\"fcl\":\"L\",\"fcode\":\"AREA\",\"population\":6814400000}]}";
		
		// Zugspitze
		var json4 = "{\"address\":{\"adminCode2\":\"001\",\"adminCode3\":\"\",\"adminCode1\":\"WY\",\"lng\":\"-105.39569\",\"houseNumber\":\"\",\"locality\":\"\",\"adminCode4\":\"\",\"adminName2\":\"Albany\",\"street\":\"Zugspitze Rd\",\"postalcode\":\"82070\",\"countryCode\":\"US\",\"adminName1\":\"Wyoming\",\"lat\":\"41.12085\"}}";
		
		// 50.960906,14.075632
		var json6 = "{\"geonames\":[{\"adminCode1\":\"13\",\"lng\":\"14.07183\",\"distance\":\"0.28996\",\"geonameId\":2952248,\"toponymName\":\"Bastei\",\"countryId\":\"2921044\",\"fcl\":\"T\",\"population\":0,\"countryCode\":\"DE\",\"name\":\"Bastei\",\"fclName\":\"mountain,hill,rock,... \",\"adminCodes1\":{\"ISO3166_2\":\"SN\"},\"countryName\":\"Germany\",\"fcodeName\":\"promontory(-ies)\",\"adminName1\":\"Saxony\",\"lat\":\"50.96192\",\"fcode\":\"PROM\"}]}";
		
		// 37.332,-122.03
		//var json5 = "{\"geonames\":[{\"adminCode1\":\"CA\",\"lng\":\"-122.03019\",\"distance\":\"0.04317\",\"geonameId\":6301897,\"toponymName\":\"Apple Computer Headquarters\",\"countryId\":\"6252001\",\"fcl\":\"S\",\"population\":0,\"countryCode\":\"US\",\"name\":\"Apple Computer Headquarters\",\"fclName\":\"spot, building, farm\",\"adminCodes1\":{\"ISO3166_2\":\"CA\"},\"countryName\":\"United States\",\"fcodeName\":\"building(s)\",\"adminName1\":\"California\",\"lat\":\"37.33164\",\"fcode\":\"BLDG\"}]}";
		
		// Infinite Loop Cupertino
		var json5 = "{\"address\":{\"adminCode2\":\"085\",\"adminCode3\":\"\",\"adminCode1\":\"CA\",\"lng\":\"-122.03144\",\"houseNumber\":\"\",\"locality\":\"Cupertino\",\"adminCode4\":\"\",\"adminName2\":\"Santa Clara\",\"street\":\"Infinite Loop\",\"postalcode\":\"\",\"countryCode\":\"US\",\"adminName1\":\"California\",\"lat\":\"37.33168\"}}";
		var result5 = GeonamesGeocoder.parseJson(json5);
		System.out.println(result5);
		assertEquals("Infinite Loop",result5.getStreet());
		assertEquals("California",result5.getState());
		assertEquals("Cupertino",result5.getCity());
		assertEquals("Santa Clara",result5.getCounty());
		
		// 52.5309,13.3847
		var json11 ="{\"geonames\":[{\"adminCode1\":\"16\",\"lng\":\"13.38428\",\"distance\":\"0.02942\",\"geonameId\":9252348,\"toponymName\":\"Boutique Hotel I31 Berlin Mitte\",\"countryId\":\"2921044\",\"fcl\":\"S\",\"population\":0,\"countryCode\":\"DE\",\"name\":\"Boutique Hotel I31 Berlin Mitte\",\"fclName\":\"spot, building, farm\",\"adminCodes1\":{\"ISO3166_2\":\"BE\"},\"countryName\":\"Germany\",\"fcodeName\":\"hotel\",\"adminName1\":\"Berlin\",\"lat\":\"52.53097\",\"fcode\":\"HTL\"}]}";
		
		// 46.1802980,7.2913685
		var json12 ="{\"geonames\":[{\"adminCode1\":\"VS\",\"lng\":\"7.29151\",\"distance\":\"0.01539\",\"geonameId\":10393026,\"toponymName\":\"Haute-Nendaz, télécabine\",\"countryId\":\"2658434\",\"fcl\":\"S\",\"population\":0,\"countryCode\":\"CH\",\"name\":\"Haute-Nendaz, télécabine\",\"fclName\":\"spot, building, farm\",\"adminCodes1\":{\"ISO3166_2\":\"VS\"},\"countryName\":\"Switzerland\",\"fcodeName\":\"bus stop\",\"adminName1\":\"Valais\",\"lat\":\"46.1804\",\"fcode\":\"BUSTP\"}]}";
		
		var json13 = "{\"address\":{\"adminCode2\":\"0363\",\"sourceId\":\"0363010012084818\",\"adminCode3\":\"\",\"adminCode1\":\"07\",\"lng\":\"4.88132\",\"houseNumber\":\"6\",\"locality\":\"Amsterdam\",\"adminCode4\":\"\",\"adminName2\":\"Gemeente Amsterdam\",\"street\":\"Museumplein\",\"postalcode\":\"1071 DJ\",\"countryCode\":\"NL\",\"adminName1\":\"North Holland\",\"lat\":\"52.35792\"}}";
		var result13 = GeonamesGeocoder.parseJson(json13);
		//System.out.println(result13);
		assertEquals("6",result13.getHouseNumber());
		assertEquals("Museumplein",result13.getStreet());
		assertEquals("1071 DJ",result13.getPostalcode());
		assertEquals("North Holland",result13.getState());
		assertEquals("Amsterdam",result13.getCity());
		assertEquals("Gemeente Amsterdam",result13.getCounty());
		assertEquals(52.358, result13.getCoordinate().getLatitude(),0.001);
		assertEquals(4.881, result13.getCoordinate().getLongitude(),0.001);
		
		// ocean drive miami
		var json14 = "{\"address\":{\"adminCode2\":\"086\",\"adminCode3\":\"\",\"adminCode1\":\"FL\",\"lng\":\"-80.13005\",\"houseNumber\":\"\",\"locality\":\"Miami Beach\",\"adminCode4\":\"\",\"adminName2\":\"Miami-Dade County\",\"street\":\"Ocean Dr\",\"postalcode\":\"33139\",\"countryCode\":\"US\",\"adminName1\":\"Florida\",\"lat\":\"25.78329\"}}";
		var result14 = GeonamesGeocoder.parseJson(json14);
		System.out.println(result14);
		assertEquals("Ocean Dr",result14.getStreet());
		assertEquals("33139",result14.getPostalcode());
		assertEquals("Miami-Dade County",result14.getCounty());
		assertEquals("Florida",result14.getState());
		assertEquals("Miami Beach",result14.getCity());
		assertEquals(25.783, result14.getCoordinate().getLatitude(),0.001);
		assertEquals(-80.13, result14.getCoordinate().getLongitude(),0.001);
		
		
		var json15 = "{\"address\":{\"adminCode2\":\"081\",\"adminCode1\":\"CA\",\"lng\":\"-122.18032\",\"distance\":\"0.04\",\"streetNumber\":\"649\",\"mtfcc\":\"S1400\",\"placename\":\"Menlo Park\",\"adminName2\":\"San Mateo\",\"street\":\"Roble Ave\",\"postalcode\":\"94025\",\"countryCode\":\"US\",\"adminName1\":\"California\",\"lat\":\"37.45127\"}}";
		var result15 = GeonamesGeocoder.parseJson(json15);
		System.out.println(result15);
		assertEquals("Roble Ave",result15.getStreet());
		assertEquals("94025",result15.getPostalcode());
		assertEquals("San Mateo",result15.getCounty());
		assertEquals("California",result15.getState());
		assertEquals(37.451, result15.getCoordinate().getLatitude(),0.001);
		assertEquals(-122.18, result15.getCoordinate().getLongitude(),0.001);

		
		
				}
	
	@Test
	public void testReverseAPI() throws GeocoderException {
		var geocoder = new GeonamesGeocoder("qqilihq");
		var result = geocoder.reverseGeoCode(GeoCoordinate.from(37.451, -122.18));
		System.out.println(result);
		assertEquals("Menlo Park", result.getName());
	}

}
