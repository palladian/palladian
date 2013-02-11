package ws.palladian.extraction.location.sources;

import java.util.List;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.JPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * FIXME:
 * 52feznh45ezmjxgfzorrk6ooagyadg
 * 
 * @author Sky
 * @author Philipp Katz
 */
public class NewsSeecrLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(NewsSeecrLocationSource.class);

    /** Configuration key for the Mashape public key. */
    public static final String CONFIG_MASHAPE_PUBLIC_KEY = "api.newsseecr.mashapePublicKey";
    /** Configuration key for the Mashape private key. */
    public static final String CONFIG_MASHAPE_PRIVATE_KEY = "api.newsseecr.mashapePrivateKey";

    private static final String BASE_URL = "https://qqilihq-newsseecr.p.mashape.com/locations";

    private final String mashapePublicKey;

    private final String mashapePrivateKey;

    private final HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();

    /**
     * <p>
     * Create a new {@link NewsSeecrLocationSource} with the provided credentials from Mashape.
     * </p>
     * 
     * @param mashapePublicKey The Mashape public key, not empty or <code>null</code>.
     * @param mashapePrivateKey The Mashape private key, not empty or <code>null</code>.
     */
    public NewsSeecrLocationSource(String mashapePublicKey, String mashapePrivateKey) {
        Validate.notEmpty(mashapePublicKey, "mashapePublicKey must not be empty");
        Validate.notEmpty(mashapePrivateKey, "mashapePrivateKey must not be empty");
        this.mashapePublicKey = mashapePublicKey;
        this.mashapePrivateKey = mashapePrivateKey;
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL);
        request.addParameter("name", locationName);
        String jsonString = retrieveResult(request);
        return parseResultArray(jsonString);
    }

    @Override
    public Location retrieveLocation(int locationId) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/" + locationId);
        String jsonString = retrieveResult(request);
        try {
            return parseSingleResult(new JSONObject(jsonString));
        } catch (JSONException e) {
            throw new IllegalStateException("Error while parsing the JSON response (" + jsonString + "): "
                    + e.getMessage(), e);
        }
    }

    @Override
    public List<Location> getHierarchy(Location location) {
        HttpRequest request = new HttpRequest(HttpMethod.GET, BASE_URL + "/" + location.getId() + "/hierarchy");
        String jsonString = retrieveResult(request);
        return parseResultArray(jsonString);
    }

    private String retrieveResult(HttpRequest request) {
        MashapeUtil.signRequest(request, mashapePublicKey, mashapePrivateKey);
        LOGGER.debug("Performing request: " + request);
        HttpResult result;
        try {
            result = retriever.execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("Encountered HTTP error when executing the request: " + request + ": "
                    + e.getMessage(), e);
        }
        if (result.getStatusCode() != 200) {
            // TODO get message
            throw new IllegalStateException("Encountered HTTP status " + result.getStatusCode()
                    + " when executing the request: " + request + ", result: " + HttpHelper.getStringContent(result));
        }
        return HttpHelper.getStringContent(result);
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
            throw new IllegalStateException("Error while parsing the JSON response (" + jsonString + "): "
                    + e.getMessage(), e);
        }
        return locations;
    }

    public Location parseSingleResult(JSONObject resultObject) throws JSONException {
        Integer id = JPathHelper.get(resultObject, "id", Integer.class);
        Double latitude = JPathHelper.get(resultObject, "latitude", Double.class);
        Double longitude = JPathHelper.get(resultObject, "longitude", Double.class);
        String primaryName = JPathHelper.get(resultObject, "primaryName", String.class);
        String type = JPathHelper.get(resultObject, "locationType", String.class);
        Long population = JPathHelper.get(resultObject, "population", Long.class);
        List<String> alternativeNames = CollectionHelper.newArrayList();
        JSONArray jsonArray = JPathHelper.get(resultObject, "alternateNames", JSONArray.class);
        for (int i = 0; i < jsonArray.length(); i++) {
            alternativeNames.add(jsonArray.getString(i));
        }
        Location location = new Location();
        location.setId(id);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setPopulation(population);
        location.setPrimaryName(primaryName);
        location.setType(LocationType.valueOf(type));
        location.setAlternativeNames(alternativeNames);
        return location;
    }

    public static void main(String[] args) {
        NewsSeecrLocationSource newsSeecrLocationSource = new NewsSeecrLocationSource("52feznh45ezmjxgfzorrk6ooagyadg",
                "iwjiagid3rqhbyu5bwwevrbpyicrk2");
        List<Location> locations = newsSeecrLocationSource.retrieveLocations("Berlin");
        CollectionHelper.print(locations);

        Location loc = locations.get(53);
        System.out.println(loc);

        List<Location> hierarchyLocations = newsSeecrLocationSource.getHierarchy(loc);
        CollectionHelper.print(hierarchyLocations);

        Location loc2 = newsSeecrLocationSource.retrieveLocation(2921044);
        System.out.println(loc2);
        System.out.println(loc2.getAlternativeNames());
    }
}
