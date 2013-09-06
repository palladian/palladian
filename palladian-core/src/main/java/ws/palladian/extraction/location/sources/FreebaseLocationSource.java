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

/**
 * <p>
 * Query Freebase for locations.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class FreebaseLocationSource extends SingleQueryLocationSource implements LocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FreebaseLocationSource.class);

    private static final Map<String, LocationType> LOCATION_MAPPING;

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("Continent", LocationType.CONTINENT);
        temp.put("Country", LocationType.COUNTRY);
        temp.put("Neighborhood", LocationType.UNIT);
        temp.put("Administrative Division", LocationType.UNIT);
        temp.put("City/Town/Village", LocationType.CITY);
        temp.put("Airport", LocationType.POI);
        temp.put("National park", LocationType.POI);
        temp.put("Hospital", LocationType.POI);
        temp.put("Hotel", LocationType.POI);
        temp.put("River", LocationType.LANDMARK);
        temp.put("Body Of Water", LocationType.LANDMARK);
        temp.put("Lake", LocationType.LANDMARK);
        temp.put("Mountain range", LocationType.LANDMARK);
        temp.put("Geographical Feature", LocationType.LANDMARK);
        temp.put("Region", LocationType.REGION);
        temp.put("US County", LocationType.UNIT);
        temp.put("Membership organization", LocationType.UNIT);

        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }

    private final String apiKey;

    public FreebaseLocationSource(String apiKey) {
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

        String url = "https://www.googleapis.com/freebase/v1/search?query=" + UrlHelper.encodeParameter(locationName)
                + "&filter=(any%20type:/location/)&key=" + apiKey;
        LOGGER.debug("check {}", url);
        JSONObject locationCandidatesObject = documentRetriever.getJSONObject(url);
        if (locationCandidatesObject == null) {
            throw new IllegalStateException("Null return from DocumentRetriever");
        }

        JSONArray locationCandidates = new JSONArray();
        try {
            locationCandidates = locationCandidatesObject.getJSONArray("result");
        } catch (JSONException e1) {
            e1.printStackTrace();
        }

        for (int i = 0; i < locationCandidates.length(); i++) {
            try {
                JsonObjectWrapper locationCandidate = new JsonObjectWrapper(locationCandidates.getJSONObject(i));
                String concept = locationCandidate.get("notable/name", String.class);
                // Double score = locationCandidate.getDouble("score");

                if (concept != null
                        && (concept.equalsIgnoreCase("city/town/village")
                                || concept.equalsIgnoreCase("country")
                                || concept
                                .equalsIgnoreCase("continent"))) {

                    JSONObject jsonObject = documentRetriever
                            .getJSONObject("https://www.googleapis.com/freebase/v1/topic/"
                                    + locationCandidate.getString("id") + "?key=" + apiKey);

                    JsonObjectWrapper json = new JsonObjectWrapper(jsonObject);

                    String primaryName = locationCandidate.getString("name");
                    LocationType locationType = LOCATION_MAPPING.get(concept);
                    Double latitude = null;
                    Double longitude = null;
                    Long population = null;

                    JsonObjectWrapper property = json.getJSONObject("property");
                    try {
                    } catch (Exception e) {
                    }

                    try {
                        // population
                        JsonObjectWrapper populationObject = property
                                .getJSONObject("/location/statistical_region/population");
                        JSONObject populationProperty = populationObject.get("values[0]/property", JSONObject.class);
                        JSONArray valuesArray = populationProperty.getJSONObject(
                                "/measurement_unit/dated_integer/number").getJSONArray("values");
                        population = valuesArray.getJSONObject(0).getLong("value");
                    } catch (Exception e) {
                    }

                    try {
                        JsonObjectWrapper locationObject = property.getJSONObject("/location/location/geolocation");
                        JSONObject locationProperty = locationObject.get("values[0]/property", JSONObject.class);

                        // latitude
                        JSONArray valuesArray = locationProperty.getJSONObject("/location/geocode/latitude")
                                .getJSONArray("values");
                        latitude = valuesArray.getJSONObject(0).getDouble("value");

                        // longitude
                        valuesArray = locationProperty.getJSONObject("/location/geocode/longitude").getJSONArray(
                                "values");
                        longitude = valuesArray.getJSONObject(0).getDouble("value");

                    } catch (Exception e) {
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
