package ws.palladian.extraction.location.sources;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * {@link LocationSource} from <a href="http://newsseecr.com">NewsSeecr</a>.
 * </p>
 * 
 * @see <a href="https://www.mashape.com/qqilihq/location-lab">API documentation on Mashape</a>
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class NewsSeecrLocationSource implements LocationSource {

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

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        return getLocations(Collections.singletonList(locationName), languages);
    }

    @Override
    public Location getLocation(int locationId) {
        return CollectionHelper.getFirst(getLocations(Collections.singletonList(locationId)));
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

    private List<Location> parseResultArray(String jsonString) {
        List<Location> locations = CollectionHelper.newArrayList();
        try {
            JSONArray resultArray = JPathHelper.get(jsonString, "results", JSONArray.class);
            for (int i = 0; i < resultArray.length(); i++) {
                JSONObject resultObject = resultArray.getJSONObject(i);
                Location location = parseSingleResult(resultObject);
                locations.add(location);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error while parsing the JSON response '" + jsonString + "': "
                    + e.getMessage(), e);
        }
        return locations;
    }

    private Location parseSingleResult(JSONObject resultObject) throws JSONException {
        Integer id = JPathHelper.get(resultObject, "id", Integer.class);
        Double latitude = JPathHelper.get(resultObject, "latitude", Double.class);
        Double longitude = JPathHelper.get(resultObject, "longitude", Double.class);
        String primaryName = JPathHelper.get(resultObject, "primaryName", String.class);
        String typeString = JPathHelper.get(resultObject, "locationType", String.class);
        Long population = JPathHelper.get(resultObject, "population", Long.class);
        List<AlternativeName> altNames = CollectionHelper.newArrayList();
        JSONArray jsonArray = JPathHelper.get(resultObject, "alternateNames", JSONArray.class);
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject altLanguageJson = jsonArray.getJSONObject(i);
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
        String ancestorPath = JPathHelper.get(resultObject, "ancestorPath", String.class);
        if (ancestorPath != null) {
            String[] split = ancestorPath.split("/");
            for (int i = split.length - 1; i >= 0; i--) {
                String ancestorId = split[i];
                if (StringUtils.isNotBlank(ancestorId)) {
                    ancestors.add(Integer.valueOf(ancestorId));
                }
            }
        }
        return new ImmutableLocation(id, primaryName, altNames, type, latitude, longitude, population, ancestors);
    }

    @Override
    public List<Location> getLocations(List<Integer> locationIds) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/" + StringUtils.join(locationIds, '+'));
        String jsonString = retrieveResult(request);
        return parseResultArray(jsonString);
    }

    @Override
    public Collection<Location> getLocations(Collection<String> locationNames, Set<Language> languages) {
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
        return parseResultArray(jsonString);
    }

}
