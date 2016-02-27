package ws.palladian.extraction.location.geocoder;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * {@link Geocoder} using the
 * <a href="https://developers.google.com/maps/documentation/geocoding/intro">
 * Google Maps Geocoding API</a>. The free service allows 2,500 requests/day,
 * maximum 10 requests/second (the latter is enforced by a class-internal
 * throttling).
 * 
 * @author Philipp Katz
 */
public final class GoogleGeocoder implements Geocoder {
	
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleGeocoder.class);

	private static final String API_URL = "https://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false";

	/** Assure, that we do not hit the API too often. */
	private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, SECONDS, 10);
	
	@Override
	public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
		String url = String.format(API_URL,
				UrlHelper.encodeParameter(addressValue));
		LOGGER.debug("Request URL = " + url);
		HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
		THROTTLE.hold();
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
			return new ImmutableGeoCoordinate(lat, lng);
		} catch (JsonException e) {
			throw new GeocoderException("Error while parsing JSON result ("
					+ httpResult.getStringContent() + ").", e);
		}
	}

}
