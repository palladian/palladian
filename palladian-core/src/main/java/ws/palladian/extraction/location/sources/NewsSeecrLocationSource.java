package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * {@link LocationSource} from <a href="http://newsseecr.com">NewsSeecr</a>.
 * </p>
 * 
 * @see <a href="https://www.mashape.com/qqilihq/location-lab">API documentation on Mashape</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NewsSeecrLocationSource extends MultiQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrLocationSource.class);

    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/locations";

    private final String mashapeKey;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    /**
     * <p>
     * Create a new {@link NewsSeecrLocationSource} with the provided credentials from Mashape and caching.
     * </p>
     * 
     * @param mashapeKey The Mashape key, not empty or <code>null</code>.
     * @return A new {@link NewsSeecrLocationSource} with caching.
     */
    public static LocationSource newCachedLocationSource(String mashapeKey) {
        return new CachingLocationSource(new NewsSeecrLocationSource(mashapeKey));
    }

    /**
     * <p>
     * Create a new {@link NewsSeecrLocationSource} with the provided credentials from Mashape.
     * </p>
     * 
     * @param mashapeKey The Mashape key, not empty or <code>null</code>.
     * @deprecated Prefer using the cached variant, which can be obtained via {@link #newCachedLocationSource(String)}.
     */
    @Deprecated
    public NewsSeecrLocationSource(String mashapeKey) {
        Validate.notEmpty(mashapeKey, "mashapeKey must not be empty");
        this.mashapeKey = mashapeKey;
    }

    private String retrieveResult(HttpRequest request) {
        request.addHeader("X-Mashape-Authorization", mashapeKey);
        LOGGER.debug("Performing request: " + request);
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("Encountered HTTP error when executing the request: " + request + ": "
                    + e.getMessage(), e);
        }
        String resultString = HttpHelper.getStringContent(result);
        if (result.getStatusCode() != 200) {
            // TODO get message
            throw new IllegalStateException("Encountered HTTP status " + result.getStatusCode()
                    + " when executing the request: " + request + ", result: " + resultString);
        }
        return resultString;
    }

    private List<Location> parseResultArray(JsonArray resultArray) {
        List<Location> locations = CollectionHelper.newArrayList();
        for (int i = 0; i < resultArray.size(); i++) {
            JsonObject resultObject = resultArray.getJsonObject(i);
            Location location = parseSingleResult(resultObject);
            locations.add(location);
        }
        return locations;
    }

    private Location parseSingleResult(JsonObject resultObject) {
        Integer id = resultObject.getInt("id");
        Double latitude = resultObject.getDouble("latitude");
        Double longitude = resultObject.getDouble("longitude");
        String primaryName = resultObject.getString("primaryName");
        String typeString = resultObject.getString("type");
        Long population = resultObject.getLong("population");
        List<AlternativeName> altNames = CollectionHelper.newArrayList();
        JsonArray jsonArray = resultObject.getJsonArray("alternativeNames");
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject altLanguageJson = jsonArray.getJsonObject(i);
            String nameValue = altLanguageJson.getString("name");
            String langValue = altLanguageJson.getString("language");
            Language language = null;
            if (langValue != null) {
                language = Language.getByIso6391(langValue);
            }
            altNames.add(new AlternativeName(nameValue, language));
        }
        LocationType type = LocationType.valueOf(typeString);
        List<Integer> ancestors = CollectionHelper.newArrayList();
        JsonArray ancestorIds = resultObject.getJsonArray("ancestorIds");
        for (int i = 0; i < ancestorIds.size(); i++) {
            ancestors.add(ancestorIds.getInt(i));
        }
        return new ImmutableLocation(id, primaryName, altNames, type, latitude, longitude, population, ancestors);
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/" + StringUtils.join(locationIds, '+'));
        String jsonString = retrieveResult(request);
        try {
            JsonArray resultArray = new JsonObject(jsonString).getJsonArray("results");
            return parseResultArray(resultArray);
        } catch (Exception e) {
            throw new IllegalStateException("Error while parsing the JSON response '" + jsonString + "': "
                    + e.getMessage(), e);
        }
    }

    @Override
    public MultiMap<String, Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL);
        request.addParameter("names", StringUtils.join(locationNames, ','));
        if (languages != null && !languages.isEmpty()) {
            StringBuilder langParameter = new StringBuilder();
            boolean first = true;
            for (Language language : languages) {
                if (!first) {
                    langParameter.append(',');
                }
                langParameter.append(language.getIso6391());
                first = false;
            }
            request.addParameter("languages", langParameter.toString());
        }
        String jsonString = retrieveResult(request);

        // parse the bulk response
        try {
            JsonArray jsonResults = new JsonObject(jsonString).getJsonArray("results");
            MultiMap<String, Location> result = DefaultMultiMap.createWithSet();
            for (int i = 0; i < jsonResults.size(); i++) {
                JsonObject currentResult = jsonResults.getJsonObject(i);
                String query = currentResult.getString("query");
                List<Location> locations = parseResultArray(currentResult.getJsonArray("result"));
                result.put(query, locations);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Error while parsing the JSON response '" + jsonString + "': "
                    + e.getMessage(), e);
        }
    }

}
