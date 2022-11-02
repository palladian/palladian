package ws.palladian.extraction.location.geocoder;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * {@link Geocoder} using the
 * <a href="https://developers.google.com/maps/documentation/geocoding/intro">
 * Google Maps Geocoding API</a>. Usage of an API key is <a href=
 * "https://developers.google.com/maps/documentation/javascript/get-api-key">required</a>.
 * 
 * @author Philipp Katz
 */
public final class GoogleGeocoder implements Geocoder {
	
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleGeocoder.class);

	private static final String API_URL = "https://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false&key=%s";

	private final String apiKey;
	
	/**
	 * @deprecated The Google API requires an API now. Using this constructor will
	 *             throw an exception from now on.
	 * 
	 * @throws IllegalStateException
	 *             when being invoked.
	 */
	 // TODO remove with next version bump
	@Deprecated
	public GoogleGeocoder() {
		throw new IllegalStateException("Google API requires to use an API key. Please use the other constructor.");
	}
	
	/**
	 * Create a new Google geocoder with the given API key.
	 * 
	 * @param apiKey
	 *            The API key to use.
	 */
	public GoogleGeocoder(String apiKey) {
		Validate.notEmpty(apiKey, "An apiKey must be given.");
		this.apiKey = apiKey;
	}
	
	@Override
	public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
		String url = String.format(API_URL,
				UrlHelper.encodeParameter(addressValue), apiKey);
		LOGGER.debug("Request URL = " + url);
		HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
		HttpResult httpResult;
		try {
			httpResult = httpRetriever.httpGet(url);
		} catch (HttpException e) {
			throw new GeocoderException("Encountered HTTP exception for \""
					+ url + "\".", e);
		}
		if (httpResult.errorStatus()) {
			throw new GeocoderException("Received HTTP status code "
					+ httpResult.getStatusCode());
		}
		LOGGER.debug("Response = " + httpResult.getStringContent());
		try {
			JsonObject resultJson = new JsonObject(
					httpResult.getStringContent());
			String status = resultJson.getString("status");
			if (!status.equals("OK") && !status.equals("ZERO_RESULTS")) {
				throw new GeocoderException("Received status code " + status);
			}
			// XXX for now, simply try to extract the first entry
			JsonArray resultJsonArray = resultJson.getJsonArray("results");
			if (resultJsonArray.size() == 0) {
				return null;
			}
			JsonObject firstJsonEntry = resultJsonArray.getJsonObject(0);
			JsonObject geometryJson = firstJsonEntry.getJsonObject("geometry");
			JsonObject locationJson = geometryJson.getJsonObject("location");
			double lat = locationJson.getDouble("lat");
			double lng = locationJson.getDouble("lng");
			return GeoCoordinate.from(lat, lng);
		} catch (JsonException e) {
			throw new GeocoderException("Error while parsing JSON result ("
					+ httpResult.getStringContent() + ").", e);
		}
	}

}
