package ws.palladian.extraction.location.sources;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Query Freebase for locations.
 * </p>
 * 
 * @author David Urbansky
 * @see <a href="http://wiki.freebase.com/wiki/How_to_obtain_an_API_key">How to obtain an API key</a>
 */
public class FreebaseLocationSource extends SingleQueryLocationSource {

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

    private final HttpRetriever httpRetriever;

    public FreebaseLocationSource(String apiKey) {
        this.apiKey = apiKey;
        this.httpRetriever = HttpRetrieverFactory.getHttpRetriever();
    }

    @Override
    public Location getLocation(int locationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Location> getLocations(String locationName, Set<Language> languages) {
        LOGGER.warn("getLocations(String,EnumSet<Language>) is not supported, ignoring language parameter");
        List<Location> locations = CollectionHelper.newArrayList();

        String url = String.format(
                "https://www.googleapis.com/freebase/v1/search?query=%s&filter=(any%%20type:/location/)&key=%s",
                UrlHelper.encodeParameter(locationName), apiKey);
        LOGGER.debug("check {}", url);

        try {
            HttpResult httpResult = httpRetriever.httpGet(url);
            JsonObject locationCandidatesObject = new JsonObject(httpResult.getStringContent());

            JsonArray locationCandidates = locationCandidatesObject.getJsonArray("result");
            for (int i = 0; i < locationCandidates.size(); i++) {
                JsonObject locationCandidate = locationCandidates.getJsonObject(i);
                JsonObject notableJson = locationCandidate.getJsonObject("notable");
                if (notableJson == null) {
                    continue;
                }
                String concept = notableJson.getString("name");

                if (Arrays.asList("city/town/village", "country", "continent").contains(concept.toLowerCase())) {

                    String topicUrl = String.format("https://www.googleapis.com/freebase/v1/topic/%s?key=%s",
                            locationCandidate.getString("id"), apiKey);
                    HttpResult topicResult = httpRetriever.httpGet(topicUrl);
                    JsonObject jsonObject = new JsonObject(topicResult.getStringContent());

                    String primaryName = locationCandidate.getString("name");
                    LocationType locationType = LOCATION_MAPPING.get(concept);
                    GeoCoordinate coordinate = null;
                    Long population = null;

                    JsonObject property = jsonObject.getJsonObject("property");

                    try {
                        // population
                        JsonObject populationObject = property.getJsonObject("/location/statistical_region/population");
                        JsonObject populationProperty = populationObject.queryJsonObject("values[0]/property");
                        JsonArray valuesArray = populationProperty.getJsonObject(
                                "/measurement_unit/dated_integer/number").getJsonArray("values");
                        population = valuesArray.getJsonObject(0).getLong("value");
                    } catch (Exception e) {
                    }

                    try {
                        JsonObject locationObject = property.getJsonObject("/location/location/geolocation");
                        JsonObject locationProperty = locationObject.queryJsonObject("values[0]/property");

                        // latitude
                        JsonArray valuesArray = locationProperty.getJsonObject("/location/geocode/latitude")
                                .getJsonArray("values");
                        double latitude = valuesArray.getJsonObject(0).getDouble("value");

                        // longitude
                        valuesArray = locationProperty.getJsonObject("/location/geocode/longitude").getJsonArray(
                                "values");
                        double longitude = valuesArray.getJsonObject(0).getDouble("value");

                        coordinate = new ImmutableGeoCoordinate(latitude, longitude);
                    } catch (Exception e) {
                    }

                    locations.add(new ImmutableLocation(-1, primaryName, locationType, coordinate, population));
                }
            }
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        } catch (HttpException e) {
            throw new IllegalStateException(e);
        }

        return locations;
    }

}
