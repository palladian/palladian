package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;
import ws.palladian.processing.features.PositionAnnotationFactory;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

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

    public YahooLocationExtractor() {
        setName("Yahoo Location Extractor");
    }

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
        TYPE_MAPPING = Collections.unmodifiableMap(temp);
    }

    @Override
    public String getModelFileEnding() {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        LOGGER.warn("the configModelFilePath is ignored");
        return getAnnotations(inputText);
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public List<Location> detectLocations(String text) {

        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://query.yahooapis.com/v1/public/yql");
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");

        StringBuilder queryBuilder = new StringBuilder();
        queryBuilder.append("SELECT * FROM geo.placemaker ");
        queryBuilder.append("WHERE documentContent=\"");
        queryBuilder.append(text.replace("\"", "%22"));
        queryBuilder.append("\"");
        queryBuilder.append(" AND documentType=\"text/plain\"");

        request.addParameter("q", queryBuilder.toString());
        request.addParameter("format", "json");

        HttpRetriever retriever = HttpRetrieverFactory.getHttpRetriever();
        HttpResult postResult;
        try {
            postResult = retriever.execute(request);
        } catch (HttpException e) {
            LOGGER.error("HTTP error when accessing the service", e);
            return Collections.emptyList();
        }
        String response = HttpHelper.getStringContent(postResult);

        List<Location> result;
        try {
            result = parseJson(text, response);
        } catch (JSONException e) {
            LOGGER.error("Error parsing the JSON: '" + response + "'.", e);
            return Collections.emptyList();
        }
        return result;
    }


    static List<Location> parseJson(String text, String response) throws JSONException {

        JSONObject jsonResult = new JSONObject(response);
        JSONObject jsonObject = jsonResult.getJSONObject("query").getJSONObject("results").getJSONObject("matches");

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

        List<Location> result = CollectionHelper.newArrayList();
        PositionAnnotationFactory annotationFactory = new PositionAnnotationFactory("location", text);
        for (JSONObject referenceJson : tempReferences.values()) {

            int woeId = referenceJson.getInt("woeIds"); // XXX there might acutally be multiple IDs

            JSONObject placeJson = woeidDataMap.get(woeId);

            int startOffset = referenceJson.getInt("start");
            int endOffset = referenceJson.getInt("end");

            String name = placeJson.getString("name");
            String type = placeJson.getString("type");
            JSONObject jsonCentroid = placeJson.getJSONObject("centroid");
            double longitude = jsonCentroid.getDouble("longitude");
            double latitude = jsonCentroid.getDouble("latitude");

            PositionAnnotation annotation = annotationFactory.create(startOffset, endOffset);
            FeatureVector featureVector = annotation.getFeatureVector();
            featureVector.add(new NominalFeature("woeId", String.valueOf(woeId)));
            featureVector.add(new NominalFeature("name", name));
            featureVector.add(new NominalFeature("type", type));
            featureVector.add(new NumericFeature("longitude", longitude));
            featureVector.add(new NumericFeature("latitude", latitude));
            Location location = new Location(annotation);
            location.setLatitude(latitude);
            location.setLongitude(longitude);
            location.setPrimaryName(name);
            LocationType mappedType = TYPE_MAPPING.get(type);
            if (mappedType == null) {
                LOGGER.error("Unmapped type {}", type);
            }
            location.setType(mappedType);

            location.setId(woeId);
            result.add(location);
        }
        return result;
    }

    public static void main(String[] args) throws Exception {
        LocationExtractor extractor = new YahooLocationExtractor();
        // String text = "They followed him to deepest Africa and found him there, in Timbuktu";
        // String text =
        // "The Prime Minister of Mali Cheick Modibo Diarra resigns himself and his government on television after his arrest hours earlier by leaders of the recent Malian coup d'état. (AFP via The Telegraph) (BBC) (Reuters)";
        // String text =
        // FileHelper.readFileToString("/Users/pk/Desktop/LocationLab/LocationExtractionDataset/text27.txt");
        String text = FileHelper.readFileToString("/Users/pk/Desktop/test.txt");
        text = HtmlHelper.stripHtmlTags(text);
        // String text = FileHelper.readFileToString("src/test/resources/testText2.txt");
        List<Location> list = extractor.detectLocations(text);
        CollectionHelper.print(list);
    }

}
