package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;

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
        } catch (JSONException e) {
            throw new IllegalStateException("Error parsing the JSON: '" + postResult.getStringContent() + "'.", e);
        }
    }

    static List<LocationAnnotation> parseJson(String text, String response) throws JSONException {

        JSONObject jsonResults = new JSONObject(response).getJSONObject("query").getJSONObject("results");
        if (jsonResults.isNull("matches")) {
            return Collections.emptyList();
        }
        JSONObject jsonObject = jsonResults.getJSONObject("matches");

        // for sorting the annotations, as the web service does not return them in order
        SortedMap<Integer, JSONObject> tempReferences = new TreeMap<Integer, JSONObject>();
        Map<Integer, JSONObject> woeidDataMap = CollectionHelper.newHashMap();

        // first collect all matches; they are either in an Object or in an Array
        List<JSONObject> tempMatches = CollectionHelper.newArrayList();
        if (jsonObject.get("match") instanceof JSONArray) {
            JSONArray jsonMatches = jsonObject.getJSONArray("match");
            for (int i = 0; i < jsonMatches.length(); i++) {
                tempMatches.add(jsonMatches.getJSONObject(i));
            }
        } else {
            tempMatches.add(jsonObject.getJSONObject("match"));

        }

        for (JSONObject match : tempMatches) {
            JSONObject place = match.getJSONObject("place");
            woeidDataMap.put(place.getInt("woeId"), place);

            // sometimes its an array, sometimes an object...
            if (match.get("reference") instanceof JSONArray) {
                JSONArray referencesJson = match.getJSONArray("reference");
                for (int j = 0; j < referencesJson.length(); j++) {
                    JSONObject reference = referencesJson.getJSONObject(j);
                    tempReferences.put(reference.getInt("start"), reference);
                }
            } else {
                JSONObject reference = match.getJSONObject("reference");
                tempReferences.put(reference.getInt("start"), reference);
            }
        }

        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        for (JSONObject referenceJson : tempReferences.values()) {

            int woeId = referenceJson.getInt("woeIds"); // XXX there might acutally be multiple IDs

            JSONObject placeJson = woeidDataMap.get(woeId);

            int startOffset = referenceJson.getInt("start");
            int endOffset = referenceJson.getInt("end");

            String type = placeJson.getString("type");
            JSONObject jsonCentroid = placeJson.getJSONObject("centroid");
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
