package ws.palladian.extraction.location.geocoder;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.persistence.json.JsonArray;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * {@link Geocoder} using the <a href="http://www.mapquest.com">MapQuest</a>
 * API. The <a href="https://developer.mapquest.com/plans">free plan</a>
 * currently allows 15,000 transactions/month.
 *
 * @author Philipp Katz
 */
public final class MapQuestGeocoder implements Geocoder {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MapQuestGeocoder.class);

    private static final String API_URL = "https://www.mapquestapi.com/geocoding/v1/address?location=%s&key=%s";

    public static final String CONFIG_API_KEY = "api.mapquest.apikey";

    private final String apiKey;

    public MapQuestGeocoder(Configuration config) {
        this(config.getString(CONFIG_API_KEY));
    }

    public MapQuestGeocoder(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be null or empty");
        this.apiKey = apiKey;
    }

    @Override
    public GeoCoordinate geoCode(String addressValue) throws GeocoderException {
        String url = String.format(API_URL, UrlHelper.encodeParameter(addressValue), apiKey);
        HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult httpResult;
        try {
            httpResult = httpRetriever.httpGet(url);
        } catch (HttpException e) {
            throw new GeocoderException("Encountered HTTP exception for \"" + url + "\".", e);
        }
        if (httpResult.errorStatus()) {
            throw new GeocoderException("Received HTTP status code " + httpResult.getStatusCode());
        }
        LOGGER.debug("Response = " + httpResult.getStringContent());
        try {
            JsonObject resultJson = new JsonObject(httpResult.getStringContent());
            // JsonObject infoJson = new JsonObject("info");
            JsonArray resultsArray = resultJson.getJsonArray("results");
            if (resultsArray.size() != 1) {
                throw new GeocoderException("results array contained more than one item");
            }
            JsonObject resultJsonItem = resultsArray.getJsonObject(0);
            JsonArray locationsJson = resultJsonItem.getJsonArray("locations");
            if (locationsJson.size() > 0) {
                JsonObject firstLocationJson = locationsJson.getJsonObject(0);
                JsonObject latLngJson = firstLocationJson.getJsonObject("latLng");
                double lat = latLngJson.getDouble("lat");
                double lng = latLngJson.getDouble("lng");
                return GeoCoordinate.from(lat, lng);
            }
            return null;
        } catch (JsonException e) {
            throw new GeocoderException("Error while parsing JSON result (" + httpResult.getStringContent() + ").", e);
        }
    }

}
