package ws.palladian.extraction.location.geocoder;

import static java.util.concurrent.TimeUnit.SECONDS;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.geo.ImmutableGeoCoordinate;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.RequestThrottle;
import ws.palladian.retrieval.helper.TimeWindowRequestThrottle;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * {@link Geocoder} using the <a href="https://mapzen.com">Mapzen</a> API. The
 * service allows 6 requests/second (automatically enforced by this class using
 * a throttle), and a maximum of 30,000 requests/day.
 * 
 * @author Philipp Katz
 */
public final class MapzenGeocoder implements Geocoder {

	/** API allows 6 requests per second. */
	private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, SECONDS, 6);

	public static final String CONFIG_API_KEY = "api.mapzen.apikey";

	private static final String API_URL = "https://search.mapzen.com/v1/search?text=%s&api_key=%s&size=1";

	private final String apiKey;

	public MapzenGeocoder(Configuration config) {
		this(config.getString(CONFIG_API_KEY));
	}

	public MapzenGeocoder(String apiKey) {
		Validate.notEmpty(apiKey, "apiKey must not be empty");
		this.apiKey = apiKey;
	}

	@Override
	public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
		String url = String.format(API_URL, UrlHelper.encodeParameter(addressValue), apiKey);
		HttpResult result;
		try {
			THROTTLE.hold();
			result = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
		} catch (HttpException e) {
			throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
		}
		if (result.errorStatus()) {
			throw new GeocoderException("Received HTTP status code " + result.getStatusCode());
		}
		try {
			JsonObject resultJson = new JsonObject(result.getStringContent());
			JsonArray featuresJson = resultJson.getJsonArray("features");
			if (featuresJson.size() > 0) {
				JsonObject firstFeature = featuresJson.getJsonObject(0);
				JsonObject geometryJson = firstFeature.getJsonObject("geometry");
				JsonArray coordinatesJson = geometryJson.getJsonArray("coordinates");
				return new ImmutableGeoCoordinate(coordinatesJson.getDouble(1), coordinatesJson.getDouble(0));
			}
		} catch (JsonException e) {
			throw new GeocoderException("Error while parsing JSON result (" + result.getStringContent() + ").", e);
		}
		return null;
	}

}
