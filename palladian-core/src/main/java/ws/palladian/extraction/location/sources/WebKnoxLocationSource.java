package ws.palladian.extraction.location.sources;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationSource;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.JsonObjectWrapper;

public class WebKnoxLocationSource extends SingleQueryLocationSource implements LocationSource {

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
    public Location getLocation(int locationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> getLocations(String locationName, Set<Language> languages) {
        LOGGER.warn("getLocations(String,EnumSet<Language>) is not supported, ignoring language parameter");
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

                    String primaryName = locationCandidate.getString("name");
                    LocationType locationType = LOCATION_MAPPING.get(concept);
                    Double latitude = null;
                    Double longitude = null;
                    Long population = null;

                    JSONArray facts = json.getJSONArray("facts");
                    for (int j = 0; j < facts.length(); j++) {
                        JsonObjectWrapper fact = new JsonObjectWrapper(facts.getJSONObject(j));
                        String key = fact.getString("key");
                        String value = fact.getString("value");

                        if (key.equalsIgnoreCase("latitude")) {
                            latitude = Double.valueOf(value);
                        } else if (key.equalsIgnoreCase("longitude")) {
                            longitude = Double.valueOf(value);
                        } else if (key.equalsIgnoreCase("population")) {
                            population = Long.valueOf(value);
                        }
                    }
                    locations
                            .add(new ImmutableLocation(-1, primaryName, locationType, latitude, longitude, population));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return locations;
    }

}
