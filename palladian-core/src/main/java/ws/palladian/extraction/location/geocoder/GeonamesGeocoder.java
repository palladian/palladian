package ws.palladian.extraction.location.geocoder;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

public final class GeonamesGeocoder implements Geocoder, ReverseGeocoder {

    private final String username;

	public GeonamesGeocoder(String username) {
		if (username == null || username.isEmpty()) {
			throw new IllegalArgumentException("Username is missing.");
		}
		this.username = username;
	}

	@Override
	public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
		HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
		try {
			// http://api.geonames.org/geoCodeAddressJSON?q=Museumplein+6+amsterdam&username=qqilihq
			String url = String.format("http://api.geonames.org/geoCodeAddressJSON?q=%s&username=%s",
					UrlHelper.encodeParameter(addressValue), username);
			HttpResult result = retriever.httpGet(url);
			// {
			// "address": {
			// "adminCode2": "0363",
			// "sourceId": "0363010012084818",
			// "adminCode3": "",
			// "adminCode1": "07",
			// "lng": "4.88132",
			// "houseNumber": "6",
			// "locality": "Amsterdam",
			// "adminCode4": "",
			// "adminName2": "Gemeente Amsterdam",
			// "street": "Museumplein",
			// "postalcode": "1071 DJ",
			// "countryCode": "NL",
			// "adminName1": "North Holland",
			// "lat": "52.35792"
			// }
			// }
			String stringContent = result.getStringContent();
			var place = parseJson(stringContent);
			return place.getCoordinate();
		} catch (HttpException e) {
			throw new GeocoderException(e);
		}
	}

	static Place parseJson(String stringContent) throws GeocoderException {
		try {
			JsonObject jsonObject = new JsonObject(stringContent);
			JsonObject jsonAddress = jsonObject.getJsonObject("address");
			if (jsonAddress == null) {
				return null;
			}

			var placeBuilder = new ImmutablePlace.Builder();
			
			// sourceId

			placeBuilder.setHouseNumber(jsonAddress.tryGetString("houseNumber"));
			
			var streetNumber = jsonAddress.tryGetString("streetNumber");
			
			//placeBuilder.setCityDistrict(jsonAddress.tryGetString("locality"));
			placeBuilder.setCity(jsonAddress.tryGetString("locality"));

			placeBuilder.setState(jsonAddress.tryGetString("adminName1"));
			placeBuilder.setCounty(jsonAddress.tryGetString("adminName2"));
			// adminName3
			// adminName4
			
			placeBuilder.setStreet(jsonAddress.tryGetString("street"));
			placeBuilder.setPostalcode(jsonAddress.tryGetString("postalcode"));
			// countryCode
			
			placeBuilder.setName(jsonAddress.tryGetString("placename"));
			
			// TODO - countryCode

			double lat = jsonAddress.getDouble("lat");
			double lng = jsonAddress.getDouble("lng");
			placeBuilder.setCooordinate(GeoCoordinate.from(lat, lng));

			return placeBuilder.create();

		} catch (JsonException e) {
			throw new GeocoderException(e);
		}
	}

	@Override
	public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
		var retriever = HttpRetrieverFactory.getHttpRetriever();
		try {
			var url = String.format("http://api.geonames.org/extendedFindNearbyJSON?lat=%s&lng=%s&username=%s",
					coordinate.getLatitude(), coordinate.getLongitude(), username);
			var result = retriever.httpGet(url);
			var stringContent = result.getStringContent();
			return parseJson(stringContent);
		} catch (HttpException e) {
			throw new GeocoderException(e);
		}
	}

	// http://www.geonames.org/export/reverse-geocoding.html

	// Extended Find nearby toponym / reverse geocoding
	// http://www.geonames.org/export/web-services.html#findNearby

}
