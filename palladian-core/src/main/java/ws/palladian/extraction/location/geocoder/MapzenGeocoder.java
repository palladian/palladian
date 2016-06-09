package ws.palladian.extraction.location.geocoder;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.geocoder.ImmutablePlace.Builder;
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
public final class MapzenGeocoder implements Geocoder, ReverseGeocoder {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(MapzenGeocoder.class);

	/** API allows 6 requests per second. */
	private static final RequestThrottle THROTTLE = new TimeWindowRequestThrottle(1, SECONDS, 5);

	public static final String CONFIG_API_KEY = "api.mapzen.apikey";

	private static final String API_URL = "https://search.mapzen.com/v1/search?text=%s&api_key=%s&size=1";
	
	private static final String REVERSE_API_URL = "https://search.mapzen.com/v1/reverse?api_key=%s&point.lat=%s&point.lon=%s";

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
		HttpResult result = performRequest(url);
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

	private HttpResult performRequest(String url) throws GeocoderException {
		for (;;) {
			try {
				THROTTLE.hold();
				HttpResult result = HttpRetrieverFactory.getHttpRetriever().httpGet(url);
				if (!result.errorStatus()) {
					return result;
				}
				if (result.getStatusCode() == 429) {
					LOGGER.info("Received HTTP status 429, delaying for 10 seconds");
					try {
						Thread.sleep(TimeUnit.SECONDS.toMillis(10));
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
					continue;
				}
				throw new GeocoderException("Received HTTP status code " + result.getStatusCode());
			} catch (HttpException e) {
				throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
			}
		}
	}

	@Override
	public Place reverseGeoCode(GeoCoordinate coordinate) throws GeocoderException {
		String url = String.format(REVERSE_API_URL, apiKey, coordinate.getLatitude(), coordinate.getLongitude());
		HttpResult result = performRequest(url);
		try {
			JsonObject resultJson = new JsonObject(result.getStringContent());
			JsonArray featuresJson = resultJson.getJsonArray("features");
			if (featuresJson.size() > 0) {
				JsonObject firstFeature = featuresJson.getJsonObject(0);
				JsonObject propertiesObject = firstFeature.getJsonObject("properties");
				Builder builder = new ImmutablePlace.Builder();
				builder.setHouseNumber(propertiesObject.tryGetString("housenumber"));
				builder.setStreet(propertiesObject.tryGetString("street"));
				builder.setPostalcode(propertiesObject.tryGetString("postalcode"));
				builder.setCountry(propertiesObject.tryGetString("country"));
				builder.setRegion(propertiesObject.tryGetString("region"));
				builder.setCounty(propertiesObject.tryGetString("county"));
				builder.setLocality(propertiesObject.tryGetString("locality"));
				builder.setNeighbourhood(propertiesObject.tryGetString("neighbourhood"));
				builder.setLabel(propertiesObject.tryGetString("label"));
				return builder.create();
			}
		} catch (JsonException e) {
			throw new GeocoderException("Error while parsing JSON result (" + result.getStringContent() + ").", e);
		}
		return null;
	}

}
