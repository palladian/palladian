package ws.palladian.extraction.location.sources;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
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
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

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
        TYPE_MAPPING.put("sublocality_level_1", LocationType.UNIT);
        TYPE_MAPPING.put("sublocality_level_2", LocationType.UNIT);
        TYPE_MAPPING.put("sublocality_level_3", LocationType.UNIT);
        TYPE_MAPPING.put("sublocality_level_4", LocationType.UNIT);
        TYPE_MAPPING.put("sublocality_level_5", LocationType.UNIT);
        TYPE_MAPPING.put("neighborhood", LocationType.REGION);
        TYPE_MAPPING.put("premise", LocationType.POI);
        TYPE_MAPPING.put("subpremise", LocationType.POI);
        TYPE_MAPPING.put("postal_code", LocationType.ZIP);
        TYPE_MAPPING.put("natural_feature", LocationType.LANDMARK);
        TYPE_MAPPING.put("airport", LocationType.POI);
        TYPE_MAPPING.put("park", LocationType.POI);
        TYPE_MAPPING.put("point_of_interest", LocationType.POI);
        TYPE_MAPPING.put("floor", null);
        TYPE_MAPPING.put("establishment", LocationType.UNDETERMINED);
        TYPE_MAPPING.put("parking", LocationType.POI);
        TYPE_MAPPING.put("post_box", null);
        TYPE_MAPPING.put("postal_town", LocationType.CITY);
        TYPE_MAPPING.put("room", null);
        TYPE_MAPPING.put("street_number", LocationType.STREETNR);
        TYPE_MAPPING.put("train_station", LocationType.POI);
        TYPE_MAPPING.put("transit_station", LocationType.POI);
        TYPE_MAPPING.put("bus_station", LocationType.POI);
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
            JsonObject jsonResponse = new JsonObject(resultString);
            checkStatus(jsonResponse);
            locations = parseLocations(jsonResponse);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing JSON, input was '" + resultString + "'.", e);
        }
        return locations;
    }

    private List<Location> parseLocations(JsonObject jsonResponse) throws JsonException {
        List<Location> locations;
        locations = CollectionHelper.newArrayList();
        if (jsonResponse.get("results") != null) {
            JsonArray result = jsonResponse.getJsonArray("results");
            for (int i = 0; i < result.size(); i++) {
                JsonObject current = result.getJsonObject(i);
                JsonObject jsonLocation = current.getJsonObject("geometry").getJsonObject("location");
                double lat = jsonLocation.getDouble("lat");
                double lng = jsonLocation.getDouble("lng");
                GeoCoordinate coordinate = new ImmutableGeoCoordinate(lat, lng);
                LocationType type = mapType(current.getJsonArray("types"));
                String name = current.getString("formatted_address");
                int id = name.hashCode(); // not available by Google
                locations.add(new ImmutableLocation(id, name, type, coordinate, null));
            }
        }
        return locations;
    }

    private static LocationType mapType(JsonArray jsonArray) throws JsonException {
        LocationType type = LocationType.UNDETERMINED;
        if (jsonArray.size() > 0) {
            if (TYPE_MAPPING.containsKey(jsonArray.getString(0))) {
                LocationType mappedType = TYPE_MAPPING.get(jsonArray.getString(0));
                if (mappedType != null) {
                    type = mappedType;
                }
            } else {
                LOGGER.warn("Unmapped type {}", jsonArray);
            }
        }
        return type;
    }

    private static void checkStatus(JsonObject jsonResponse) throws JsonException {
        String status = jsonResponse.getString("status");
        if (Arrays.asList("OK", "ZERO_RESULTS").contains(status)) {
            return;
        }
        throw new IllegalStateException("Received status code " + status);
    }

    @Override
    public Location getLocation(int locationId) {
        throw new UnsupportedOperationException("Not supported by Google.");
    }

    @Override
    public List<Location> getLocations(GeoCoordinate coordinate, double distance) {
        String url = String.format("http://maps.googleapis.com/maps/api/geocode/json?latlng=%s,%s&sensor=true",
                coordinate.getLatitude(), coordinate.getLongitude());
        HttpResult httpResult = null;
        try {
            httpResult = httpRetriever.httpGet(url);
            JsonObject jsonResult = new JsonObject(httpResult.getStringContent());
            checkStatus(jsonResult);
            return parseLocations(jsonResult);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        } catch (JsonException e) {
            throw new IllegalStateException("Error while parsing JSON, input was '" + httpResult.getStringContent()
                    + "'.", e);
        }
    }

    public static void main(String[] args) {
        GoogleLocationSource locationSource = new GoogleLocationSource();
        // Collection<Location> locations = locationSource.getLocations("The Firehouse", null);
        // Collection<Location> locations = locationSource.getLocations("Heir Island", null);
        // Collection<Location> locations = locationSource.getLocations("Dun Aengus", null);
        Collection<Location> locations = locationSource.getLocations(new ImmutableGeoCoordinate(40.714224, -73.961452),
                0);
        CollectionHelper.print(locations);
    }

}
