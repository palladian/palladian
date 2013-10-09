package ws.palladian.extraction.location;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * This class provides the functionality of {@link PalladianLocationExtractor} as web service from <a
 * href="http://newsseecr.com">NewsSeecr</a>.
 * </p>
 * 
 * @see <a href="https://www.mashape.com/qqilihq/newsseecr">API documentation on Mashape</a>
 * @author Philipp Katz
 */
public final class NewsSeecrLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrLocationExtractor.class);

    /** The name of this extractor. */
    private static final String EXTRACTOR_NAME = "Palladian/NewsSeecr";

    // private static final String BASE_URL = "http://localhost:8080/api/locations/extractor";
    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/locations/extractor";

    private final String mashapeKey;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    /**
     * <p>
     * Create a new {@link NewsSeecrLocationExtractor} with the provided credentials from Mashape.
     * </p>
     * 
     * @param mashapeKey The Mashape key, not <code>null</code> or empty.
     */
    public NewsSeecrLocationExtractor(String mashapeKey) {
        Validate.notEmpty(mashapeKey, "mashapeKey must not be empty");
        this.mashapeKey = mashapeKey;
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        HttpRequest request = new HttpRequest(HttpMethod.POST, BASE_URL);
        request.addParameter("text", inputText);
        request.addHeader("X-Mashape-Authorization", mashapeKey);
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP exception while accessing the web service: " + e.getMessage(), e);
        }
        String resultString = result.getStringContent();
        checkError(result);
        LOGGER.debug("Result JSON: {}", resultString);
        try {
            List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
            JsonObject jsonResult = new JsonObject(resultString);
            JsonArray resultArray = jsonResult.getJsonArray("results");
            for (int i = 0; i < resultArray.size(); i++) {
                JsonObject currentResult = resultArray.getJsonObject(i);
                int startPos = currentResult.getInt("startPosition");
                String name = currentResult.getString("value");

                JsonObject locationJson = currentResult.getJsonObject("location");
                int locationId = locationJson.getInt("id");
                String primaryName = locationJson.getString("primaryName");
                LocationType type = LocationType.valueOf(locationJson.getString("type"));

                GeoCoordinate coordinate = null;
                if (locationJson.get("coordinate") != null) {
                    double lat = locationJson.queryDouble("coordinate/latitude");
                    double lng = locationJson.queryDouble("coordinate/longitude");
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                }
                Long population = locationJson.tryGetLong("population");
                List<AlternativeName> alternativeNames = CollectionHelper.newArrayList();
                JsonArray altNamesJson = locationJson.getJsonArray("alternativeNames");
                for (int j = 0; j < altNamesJson.size(); j++) {
                    JsonObject altNameJson = altNamesJson.getJsonObject(j);
                    String altName = altNameJson.getString("name");
                    Language altLng = Language.getByIso6391(altNameJson.getString("language"));
                    alternativeNames.add(new AlternativeName(altName, altLng));
                }
                List<Integer> ancestorIds = CollectionHelper.newArrayList();
                JsonArray ancestorJson = locationJson.getJsonArray("ancestorIds");
                for (int j = 0; j < ancestorJson.size(); j++) {
                    ancestorIds.add(ancestorJson.getInt(j));
                }

                Location location = new ImmutableLocation(locationId, primaryName, alternativeNames, type, coordinate,
                        population, ancestorIds);
                annotations.add(new LocationAnnotation(startPos, name, location));

            }
            return annotations;
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing the JSON: " + e.getMessage() + ", JSON: "
                    + resultString, e);
        }
    }

    /**
     * <p>
     * Check for potential errors from the web service, throw Exception if error occurred.
     * </p>
     * 
     * @param result
     */
    private void checkError(HttpResult result) {
        if (result.getStatusCode() >= 300) {
            // try to get the message
            try {
                JsonObject json = new JsonObject(result.getStringContent());
                String message = json.getString("message");
                throw new IllegalStateException("Error while accessing the web service: " + message
                        + ", response code: " + result.getStatusCode());
            } catch (JsonException ignore) {
                // no message could be extracted
            }
            throw new IllegalStateException("Error while accessing the web service, response code: "
                    + result.getStatusCode());
        }
    }

    @Override
    public String getName() {
        return EXTRACTOR_NAME;
    }

}
