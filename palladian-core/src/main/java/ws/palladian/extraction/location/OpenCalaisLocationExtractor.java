package ws.palladian.extraction.location;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.helper.collection.CaseInsensitiveMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpRequest;
import ws.palladian.retrieval.HttpRequest.HttpMethod;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * <p>
 * Location extractor based on <a href="http://www.opencalais.com/calaisAPI">OpenCalais</a> API.
 * </p>
 * 
 * @author Philipp Katz
 */
public class OpenCalaisLocationExtractor extends LocationExtractor {

    // XXX plenty of copied code from OpenCalaisNer; inheritance structure does not allow common base though currently.

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(OpenCalaisLocationExtractor.class);

    /** The mapping between OpenCalais types and Palladian's {@link LocationType}. */
    private static final Map<String, LocationType> LOCATION_MAPPING;

    /** The maximum number of characters allowed to send per request (actually 100,000). */
    private final int MAXIMUM_TEXT_LENGTH = 90000;

    /** The {@link HttpRetriever} is used for performing the POST requests to the API. */
    private final HttpRetriever httpRetriever = HttpRetrieverFactory.getHttpRetriever();

    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("continent", LocationType.CONTINENT);
        temp.put("city", LocationType.CITY);
        temp.put("country", LocationType.COUNTRY);
        temp.put("facility", LocationType.POI);
        temp.put("naturalfeature", LocationType.LANDMARK);
        temp.put("region", LocationType.REGION);
        temp.put("provinceorstate", LocationType.UNIT);
        LOCATION_MAPPING = CaseInsensitiveMap.from(temp);
    }

    private final String apiKey;

    public OpenCalaisLocationExtractor(String apiKey) {
        Validate.notEmpty(apiKey, "apiKey must not be empty");
        this.apiKey = apiKey;
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {

        List<LocationAnnotation> annotations = CollectionHelper.newArrayList();

        List<String> textChunks = NerHelper.createSentenceChunks(inputText, MAXIMUM_TEXT_LENGTH);

        LOGGER.debug("sending " + textChunks.size() + " text chunks, total text length " + inputText.length());

        // since the offset is per chunk we need to add the offset for each new chunk to get the real position of the
        // entity in the original text
        int cumulatedOffset = 0;
        for (String textChunk : textChunks) {

            String response = null;

            try {

                HttpResult httpResult = getHttpResult(textChunk.toString());
                response = HttpHelper.getStringContent(httpResult);

                JSONObject json = new JSONObject(response);

                @SuppressWarnings("unchecked")
                Iterator<String> it = json.keys();

                while (it.hasNext()) {
                    String key = it.next();

                    JSONObject obj = json.getJSONObject(key);
                    if (obj.has("_typeGroup") && obj.getString("_typeGroup").equalsIgnoreCase("entities")) {

                        String entityName = obj.getString("name");
                        String entityTag = obj.getString("_type");

                        LocationType type = LOCATION_MAPPING.get(entityTag);
                        if (type == null) {
                            LOGGER.debug("Ignore type {}", entityTag);
                            continue;
                        }

                        Double latitude = null;
                        Double longitude = null;
                        int id = 0;
                        String name = entityName;
                        if (obj.has("resolutions")) {
                            JSONArray resolutions = obj.getJSONArray("resolutions");
                            if (resolutions.length() > 0) {
                                JSONObject firstResolution = resolutions.getJSONObject(0);
                                latitude = firstResolution.optDouble("latitude");
                                longitude = firstResolution.optDouble("longitude");
                                String idString = firstResolution.optString("id");
                                id = idString != null ? idString.hashCode() : 0;
                                name = firstResolution.optString("name");
                            }
                        }

                        if (obj.has("instances")) {
                            JSONArray instances = obj.getJSONArray("instances");

                            for (int i = 0; i < instances.length(); i++) {
                                JSONObject instance = instances.getJSONObject(i);

                                // take only instances that are as long as the entity name, this way we discard
                                // co-reference resolution instances
                                if (instance.getInt("length") == entityName.length()) {
                                    int offset = instance.getInt("offset");
                                    Annotation annotation = new Annotation(cumulatedOffset + offset, entityName,
                                            entityTag);
                                    Location location = new ImmutableLocation(id, name, type, latitude,
                                            longitude, null);
                                    annotations.add(new LocationAnnotation(annotation, location));
                                }
                            }
                        }
                    }
                }

            } catch (HttpException e) {
                LOGGER.error("Error performing HTTP POST: {}", e.getMessage());
            } catch (JSONException e) {
                LOGGER.error("Could not parse the JSON response: {}, exception: {}",
                        new Object[] {response, e.getMessage(), e});
            }

            cumulatedOffset += textChunk.length();
        }

        return annotations;
    }

    private HttpResult getHttpResult(String inputText) throws HttpException {
        HttpRequest request = new HttpRequest(HttpMethod.POST, "http://api.opencalais.com/tag/rs/enrich");
        request.addHeader("x-calais-licenseID", apiKey);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        request.addHeader("Accept", "application/json");
        request.addParameter("content", inputText);
        request.addParameter(
                "paramsXML",
                "<c:params xmlns:c=\"http://s.opencalais.com/1/pred/\" xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><c:processingDirectives c:contentType=\"text/raw\" c:outputFormat=\"application/json\" c:discardMetadata=\";\"></c:processingDirectives><c:userDirectives c:allowDistribution=\"true\" c:allowSearch=\"true\" c:externalID=\"calaisbridge\" c:submitter=\"calaisbridge\"></c:userDirectives><c:externalMetadata c:caller=\"GnosisFirefox\"/></c:params>");
        return httpRetriever.execute(request);
    }

    @Override
    public String getName() {
        return "OpenCalais NER (Locations)";
    }

    public static void main(String[] args) {
        OpenCalaisLocationExtractor extractor = new OpenCalaisLocationExtractor("mx2g74ej2qd4xpqdkrmnyny5");
        String text = "Dresden (Saxony) and Berlin are cities in Germany which lies in Europe on planet Earth, the middle east is somewhere else";
        List<LocationAnnotation> detectedLocations = extractor.getAnnotations(text);
        CollectionHelper.print(detectedLocations);
    }

}
