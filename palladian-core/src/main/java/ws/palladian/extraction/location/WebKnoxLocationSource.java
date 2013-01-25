package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.JsonObjectWrapper;

public class WebKnoxLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebKnoxLocationSource.class);

    private static final Map<String, LocationType> LOCATION_MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("Country", LocationType.COUNTRY);
        temp.put("Nation", LocationType.COUNTRY);
        temp.put("County", LocationType.UNIT);
        temp.put("City", LocationType.CITY);
        temp.put("Metropole", LocationType.CITY);
        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }

    private final String apiKey;

    public WebKnoxLocationSource(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public List<Location> retrieveLocations(String locationName) {
        List<Location> locations = CollectionHelper.newArrayList();
        DocumentRetriever documentRetriever = new DocumentRetriever();

        String url = "http://webknox.com/api/entities/search?entityName=" + UrlHelper.encodeParameter(locationName)
                + "&apiKey=" + apiKey;
        LOGGER.debug("check {}", url);
        JSONArray locationCandidates = documentRetriever.getJsonArray(url);
        if (locationCandidates == null) {
            throw new IllegalStateException("Null return from DocumentRetriever");
        }

        for (int i = 0; i < locationCandidates.length(); i++) {
            try {
                JsonObjectWrapper locationCandidate = new JsonObjectWrapper(locationCandidates.getJSONObject(i));
                String concept = locationCandidate.getString("concept");
                Double confidence = locationCandidate.getDouble("confidence");
                if ((concept.equalsIgnoreCase("city") || concept.equalsIgnoreCase("country")) && confidence > 0.999) {
                    JSONObject jsonObject = documentRetriever.getJsonObject("http://webknox.com/api/entities/"
                            + locationCandidate.getString("id") + "?apiKey=" + apiKey);
                    JsonObjectWrapper json = new JsonObjectWrapper(jsonObject);

                    Location location = new Location();
                    location.setPrimaryName(locationCandidate.getString("name"));
                    location.setType(LOCATION_MAPPING.get(concept));

                    JSONArray facts = json.getJSONArray("facts");
                    for (int j = 0; j < facts.length(); j++) {
                        JsonObjectWrapper fact = new JsonObjectWrapper(facts.getJSONObject(j));
                        String key = fact.getString("key");
                        String value = fact.getString("value");

                        if (key.equalsIgnoreCase("latitude")) {
                            location.setLatitude(Double.valueOf(value));
                        } else if (key.equalsIgnoreCase("longitude")) {
                            location.setLongitude(Double.valueOf(value));
                        } else if (key.equalsIgnoreCase("population")) {
                            location.setPopulation(Long.valueOf(value));
                        }
                    }

                    locations.add(location);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return locations;
    }

    @Override
    public void save(Location location) {
        throw new UnsupportedOperationException("Read only!");
    }
}
