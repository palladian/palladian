package ws.palladian.extraction.location;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
import ws.palladian.retrieval.helper.HttpHelper;

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
        String resultString = HttpHelper.getStringContent(result);
        checkError(result);
        LOGGER.debug("Result JSON: {}", resultString);
        try {
            List<LocationAnnotation> annotations = CollectionHelper.newArrayList();
            JSONObject jsonResult = new JSONObject(resultString);
            JSONArray resultArray = jsonResult.getJSONArray("results");
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject currentResult = resultArray.getJSONObject(i);
                int startPos = currentResult.getInt("startPosition");
                String name = currentResult.getString("value");

                JSONObject locationJson = currentResult.getJSONObject("location");
                int locationId = locationJson.getInt("id");
                String primaryName = locationJson.getString("primaryName");
                LocationType type = LocationType.valueOf(locationJson.getString("type"));
                Double lat = locationJson.optDouble("latitude");
                Double lng = locationJson.optDouble("longitude");
                Long population = locationJson.optLong("population");
                List<AlternativeName> alternativeNames = CollectionHelper.newArrayList();
                JSONArray altNamesJson = locationJson.getJSONArray("alternativeNames");
                for (int j = 0; j < altNamesJson.length(); j++) {
                    JSONObject altNameJson = altNamesJson.getJSONObject(j);
                    String altName = altNameJson.getString("name");
                    Language altLng = Language.getByIso6391(altNameJson.getString("language"));
                    alternativeNames.add(new AlternativeName(altName, altLng));
                }
                List<Integer> ancestorIds = CollectionHelper.newArrayList();
                JSONArray ancestorJson = locationJson.getJSONArray("ancestorIds");
                for (int j = 0; j < ancestorJson.length(); j++) {
                    ancestorIds.add(ancestorJson.getInt(j));
                }

                Location location = new ImmutableLocation(locationId, primaryName, alternativeNames, type, lat, lng,
                        population, ancestorIds);
                annotations.add(new LocationAnnotation(startPos, name, location));

            }
            return annotations;
        } catch (JSONException e) {
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
                JSONObject json = new JSONObject(HttpHelper.getStringContent(result));
                String message = json.getString("message");
                throw new IllegalStateException("Error while accessing the web service: " + message
                        + ", response code: " + result.getStatusCode());
            } catch (JSONException ignore) {
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
