package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Place extraction using Yahoo Placespotter. Provides 2000 queries (per day, I assume, although not stated) for
 * non-commercial use.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://developer.yahoo.com/boss/geo/">Yahoo! BOSS Geo Services</a>
 * @see <a href="http://developer.yahoo.com/boss/geo/docs/free_YQL.html">Non-Commercial usage of Yahoo Geo API's</a>
 * @see <a href="http://developer.yahoo.com/geo/geoplanet/guide/concepts.html#placetypes">Overview over available
 *      types</a>
 */
public class YahooLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(YahooLocationExtractor.class);

    private static final Map<String, LocationType> TYPE_MAPPING;

    // http://developer.yahoo.com/geo/geoplanet/guide/concepts.html#placetypes
    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("Continent", LocationType.CONTINENT);
        temp.put("Country", LocationType.COUNTRY);
        temp.put("Admin", LocationType.UNIT);
        temp.put("Admin2", LocationType.UNIT);
        temp.put("Admin3", LocationType.UNIT);
        temp.put("Town", LocationType.CITY);
        temp.put("Suburb", LocationType.UNIT);
        temp.put("Postal Code", LocationType.ZIP);
        temp.put("Supername", LocationType.REGION);
        temp.put("Colloquial", LocationType.UNDETERMINED);
        temp.put("Time Zone", LocationType.UNDETERMINED);
        temp.put("State", LocationType.UNIT);
        temp.put("POI", LocationType.POI);
        temp.put("County", LocationType.UNIT);
        temp.put("Island", LocationType.LANDMARK);
        temp.put("LandFeature", LocationType.LANDMARK);
        temp.put("Drainage", LocationType.LANDMARK);
        temp.put("Airport", LocationType.POI);
        temp.put("Sea", LocationType.LANDMARK);
        temp.put("Zip", LocationType.ZIP);
        temp.put("Ocean", LocationType.LANDMARK);
        temp.put("Estate", LocationType.POI);
        temp.put("LocalAdmin", LocationType.UNIT);
        temp.put("HistoricalTown", LocationType.CITY);
        temp.put("HistoricalCounty", LocationType.COUNTRY);
        TYPE_MAPPING = Collections.unmodifiableMap(temp);
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {

        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://query.yahooapis.com/v1/public/yql");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM geo.placemaker ");
        queryBuilder.append("WHERE documentContent=\"");
        queryBuilder.append(inputText.replace("\"", "%22"));
        queryBuilder.append("\"");
        queryBuilder.append(" AND documentType=\"text/plain\"");

        request.addParameter("q", queryBuilder.toString());
        request.addParameter("format", "json");

        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult postResult;
        try {
            postResult = retriever.execute(request);
        } catch (HttpException e) {
            throw new IllegalStateException("HTTP error when accessing the service", e);
        }
        try {
            return parseJson(inputText, postResult.getStringContent());
        } catch (JsonException e) {
            throw new IllegalStateException("Error parsing the JSON: '" + postResult.getStringContent() + "'.", e);
        }
    }

    static List<LocationAnnotation> parseJson(String text, String response) throws JsonException {

        JsonObject jsonResults = new JsonObject(response).getJsonObject("query").getJsonObject("results");
        if (jsonResults.get("matches") == null) {
            return Collections.emptyList();
        }
        JsonObject jsonObject = jsonResults.getJsonObject("matches");

        // for sorting the annotations, as the web service does not return them in order
        SortedMap<Integer, JsonObject> tempReferences = new TreeMap<Integer, JsonObject>();
        Map<Integer, JsonObject> woeidDataMap = CollectionHelper.newHashMap();

        // first collect all matches; they are either in an Object or in an Array
        List<JsonObject> tempMatches = CollectionHelper.newArrayList();
        if (jsonObject.get("match") instanceof JsonArray) {
            JsonArray jsonMatches = jsonObject.getJsonArray("match");
            for (int i = 0; i < jsonMatches.size(); i++) {
                tempMatches.add(jsonMatches.getJsonObject(i));
            }
        } else {
            tempMatches.add(jsonObject.getJsonObject("match"));

        }

        for (JsonObject match : tempMatches) {
            JsonObject place = match.getJsonObject("place");
            woeidDataMap.put(place.getInt("woeId"), place);

            // sometimes its an array, sometimes an object...
            if (match.get("reference") instanceof JsonArray) {
                JsonArray referencesJson = match.getJsonArray("reference");
                for (int j = 0; j < referencesJson.size(); j++) {
                    JsonObject reference = referencesJson.getJsonObject(j);
                    tempReferences.put(reference.getInt("start"), reference);
                }
            } else {
                JsonObject reference = match.getJsonObject("reference");
                tempReferences.put(reference.getInt("start"), reference);
            }
        }

        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (JsonObject referenceJson : tempReferences.values()) {

            int woeId = referenceJson.getInt("woeIds"); // XXX there might acutally be multiple IDs

            JsonObject placeJson = woeidDataMap.get(woeId);

            int startOffset = referenceJson.getInt("start");
            int endOffset = referenceJson.getInt("end");

            String type = placeJson.getString("type");
            JsonObject jsonCentroid = placeJson.getJsonObject("centroid");
            double lng = jsonCentroid.getDouble("longitude");
            double lat = jsonCentroid.getDouble("latitude");
            GeoCoordinate coordinate = new ImmutableGeoCoordinate(lat, lng);

            String name = placeJson.getString("name");
            String actualName = text.substring(startOffset, endOffset);
            Set<AlternativeName> alternatives = null;
            if (!name.equals(actualName)) {
                alternatives = Collections.singleton(new AlternativeName(name, null));
            }

            LocationType mappedType = TYPE_MAPPING.get(type);
            if (mappedType == null) {
                LOGGER.error("Unmapped type {}", type);
                continue;
            }
            Location location = new ImmutableLocation(woeId, actualName, alternatives, mappedType, coordinate, null,
                    null);
            result.add(new LocationAnnotation(startOffset, actualName, location));
        }
        return result;
    }

    @Override
    public String getName() {
        return "Yahoo! Placespotter";
    }

    public static void main(String[] args) throws Exception {
        LocationExtractor extractor = new YahooLocationExtractor();
        String text = "They followed him to deepest Africa and found him there, in Timbuktu";
        List<LocationAnnotation> list = extractor.getAnnotations(text);
        CollectionHelper.print(list);
    }

}
