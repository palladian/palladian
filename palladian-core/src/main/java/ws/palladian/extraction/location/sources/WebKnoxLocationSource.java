package ws.palladian.extraction.location.sources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

public class WebKnoxLocationSource extends SingleQueryLocationSource {

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
        Validate.notEmpty(apiKey, "apiKey must not be empty or null");
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

        String url = String.format("http://webknox.com/api/entities/search?entityName=%s&apiKey=%s",
                UrlHelper.encodeParameter(locationName), apiKey);
        LOGGER.debug("check {}", url);
        String jsonString = documentRetriever.getText(url);
        if (jsonString == null) {
            throw new IllegalStateException("Error while retrieving " + url);
        }
        try {
            JsonArray locationCandidates = new JsonArray(jsonString);

            for (int i = 0; i < locationCandidates.size(); i++) {
                JsonObject locationCandidate = locationCandidates.getJsonObject(i);
                String concept = locationCandidate.getString("concept");
                double confidence = locationCandidate.getDouble("confidence");

                if (Arrays.asList("city", "country").contains(concept.toLowerCase()) && confidence > 0.999) {
                    int id = locationCandidate.getInt("id");
                    String entityUrl = String.format("http://webknox.com/api/entities/%s?apiKey=%s", id, apiKey);
                    String entityJson = documentRetriever.getText(entityUrl);
                    if (entityJson == null) {
                        throw new IllegalStateException("Error while retrieving " + entityUrl);
                    }

                    JsonObject jsonObject = new JsonObject(entityJson);
                    String primaryName = locationCandidate.getString("name");
                    LocationType locationType = LOCATION_MAPPING.get(concept);
                    Double latitude = null;
                    Double longitude = null;
                    Long population = null;

                    JsonArray facts = jsonObject.getJsonArray("facts");
                    for (int j = 0; j < facts.size(); j++) {
                        JsonObject fact = facts.getJsonObject(j);
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
                    GeoCoordinate coordinate = null;
                    if (latitude != null && longitude != null) {
                        coordinate = new ImmutableGeoCoordinate(latitude, longitude);
                    }
                    locations.add(new ImmutableLocation(id, primaryName, locationType, coordinate, population));
                }
            }
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing JSON: " + e.getMessage(), e);
        }

        return locations;
    }

}
