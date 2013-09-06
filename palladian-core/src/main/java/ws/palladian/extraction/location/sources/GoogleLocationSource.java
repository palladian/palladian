package ws.palladian.extraction.location.sources;

import java.util.Arrays;
import java.util.Collection;
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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

/**
 * <p>
 * <a href="http://developers.google.com/maps/documentation/geocoding/">Google Geocoding API</a> {@link LocationSource}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class GoogleLocationSource extends SingleQueryLocationSource {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleLocationSource.class);

    private static final Map<String, LocationType> TYPE_MAPPING = CollectionHelper.newHashMap();

    private final HttpRetriever httpRetriever;

    static {
        TYPE_MAPPING.put("street_address", LocationType.UNDETERMINED);
        TYPE_MAPPING.put("route", LocationType.UNDETERMINED);
        TYPE_MAPPING.put("intersection", LocationType.UNDETERMINED);
        TYPE_MAPPING.put("political", LocationType.UNIT);
        TYPE_MAPPING.put("country", LocationType.COUNTRY);
        TYPE_MAPPING.put("administrative_area_level_1", LocationType.UNIT);
        TYPE_MAPPING.put("administrative_area_level_2", LocationType.UNIT);
        TYPE_MAPPING.put("administrative_area_level_3", LocationType.UNIT);
        TYPE_MAPPING.put("colloquial_area", LocationType.UNDETERMINED);
        TYPE_MAPPING.put("locality", LocationType.CITY);
        TYPE_MAPPING.put("sublocality", LocationType.UNIT);
        TYPE_MAPPING.put("neighborhood", LocationType.REGION);
        TYPE_MAPPING.put("premise", LocationType.POI);
        TYPE_MAPPING.put("subpremise", LocationType.POI);
        TYPE_MAPPING.put("postal_code", LocationType.ZIP);
        TYPE_MAPPING.put("natural_feature", LocationType.LANDMARK);
        TYPE_MAPPING.put("airport", LocationType.POI);
        TYPE_MAPPING.put("park", LocationType.POI);
        TYPE_MAPPING.put("point_of_interest", LocationType.POI);
    }

    public GoogleLocationSource() {
        httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public Collection<Location> getLocations(String locationName, Set<Language> languages) {
        String url = String.format("http://maps.googleapis.com/maps/api/geocode/json?address=%s&sensor=false",
                UrlHelper.encodeParameter(locationName));
        List<Location> locations;
        String resultString = null;
        try {
            HttpResult httpResult = httpRetriever.httpGet(url);
            resultString = httpResult.getStringContent();
            JSONObject jsonResponse = new JSONObject(resultString);
            String status = jsonResponse.getString("status");
            checkStatus(status);
            locations = CollectionHelper.newArrayList();
            if (jsonResponse.has("results")) {
                JSONArray result = jsonResponse.getJSONArray("results");
                for (int i = 0; i < result.length(); i++) {
                    JSONObject current = result.getJSONObject(i);
                    JSONObject jsonLocation = current.getJSONObject("geometry").getJSONObject("location");
                    Double lat = jsonLocation.getDouble("lat");
                    Double lng = jsonLocation.getDouble("lng");
                    LocationType type = mapType(current.getJSONArray("types"));
                    String name = current.getString("formatted_address");
                    int id = name.hashCode(); // not available by Google
                    locations.add(new ImmutableLocation(id, name, type, lat, lng, null));
                }
            }
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JSONException e) {
            throw new IllegalStateException("Error while parsing JSON, input was '" + resultString + "'.", e);
        }
        return locations;
    }

    private static LocationType mapType(JSONArray jsonArray) throws JSONException {
        LocationType type = LocationType.UNDETERMINED;
        if (jsonArray.length() > 0) {
            LocationType mappedType = TYPE_MAPPING.get(jsonArray.getString(0));
            if (mappedType == null) {
                LOGGER.warn("Unmapped type {}", jsonArray);
            } else {
                type = mappedType;
            }
        }
        return type;
    }

    private static void checkStatus(String status) {
        if (Arrays.asList("OK", "ZERO_RESULTS").contains(status)) {
            return;
        }
        throw new IllegalStateException("Received status code " + status);
    }

    @Override
    public Location getLocation(int locationId) {
        throw new UnsupportedOperationException("Not supported by Google.");
    }

    public static void main(String[] args) {
        GoogleLocationSource locationSource = new GoogleLocationSource();
        // Collection<Location> locations = locationSource.getLocations("The Firehouse", null);
        // Collection<Location> locations = locationSource.getLocations("Heir Island", null);
        Collection<Location> locations = locationSource.getLocations("Dun Aengus", null);
        CollectionHelper.print(locations);
    }

}
