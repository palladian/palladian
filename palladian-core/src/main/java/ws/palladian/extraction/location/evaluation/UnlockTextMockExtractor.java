package ws.palladian.extraction.location.evaluation;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.tagger.NerHelper;
import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.GeoCoordinate;
import ws.palladian.extraction.location.ImmutableGeoCoordinate;
import ws.palladian.extraction.location.ImmutableLocation;
import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationAnnotation;
import ws.palladian.extraction.location.LocationExtractor;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

final class UnlockTextMockExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UnlockTextMockExtractor.class);

    static List<Location> parseLocations(String jsonInput) throws JSONException {
        List<Location> locations = CollectionHelper.newArrayList();
        JSONArray resultArray = new JSONArray(jsonInput);
        JSONObject placesJson = null;
        for (int i = 0; i < resultArray.length(); i++) {
            JSONObject temp = resultArray.getJSONObject(i);
            if (temp.has("places")) {
                placesJson = temp.getJSONObject("places");
                break;
            }
        }
        if (placesJson == null) {
            throw new IllegalStateException("No places found.");
        }
        @SuppressWarnings("unchecked")
        Iterator<String> keyIterator = placesJson.keys();
        while (keyIterator.hasNext()) {
            String name = keyIterator.next();
            GeoCoordinate coordinate = null;
            Long pop = null;
            int id = -1;
            List<AlternativeName> altNames = CollectionHelper.newArrayList();

            JSONArray locationJson = placesJson.getJSONArray(name);
            for (int i = 0; i < locationJson.length(); i++) {
                JSONObject locationObj = locationJson.getJSONObject(i);
                if (locationObj.has("id")) {
                    // use internal ID here, this means, IDs are not unique for multiple requests
                    id = Integer.valueOf(locationObj.getString("id").replace("rb", ""));
                    String abbrevName = locationObj.optString("abbrev-for", null);
                    if (abbrevName != null) {
                        altNames.add(new AlternativeName(abbrevName, null));
                    }
                    String altName = locationObj.optString("altname", null);
                    if (altName != null) {
                        altNames.add(new AlternativeName(altName, null));
                    }
                }
                if (locationObj.has("pop")) {
                    pop = locationObj.getLong("pop");
                }
                if (locationObj.has("long")) {
                    Double lng = locationObj.getDouble("long");
                    Double lat = locationObj.getDouble("lat");
                    coordinate = new ImmutableGeoCoordinate(lat, lng);
                    break;
                }
            }
            locations.add(new ImmutableLocation(id, name, altNames, LocationType.UNDETERMINED, coordinate, pop, null));
        }
        return locations;
    }

    static List<LocationAnnotation> createAnnotations(String jsonResponse, String text) throws JSONException {
        Annotations<LocationAnnotation> annotations = new Annotations<LocationAnnotation>();
        List<Location> locations = parseLocations(jsonResponse);
        for (Location location : locations) {
            String name = location.getPrimaryName();
            // List<Integer> indices = StringHelper.getOccurrenceIndices(text, name);
            List<Integer> indices = NerHelper.getEntityOffsets(text, name);
            for (Integer idx : indices) {
                annotations.add(new LocationAnnotation(idx, name, location));
            }
        }
        annotations.sort();
        annotations.removeNested();
        return annotations;
    }

    private final Map<Integer, List<LocationAnnotation>> data = CollectionHelper.newHashMap();

    public UnlockTextMockExtractor(File pathToTexts, File pathToJsonResults) {
        Validate.notNull(pathToTexts, "pathToTexts must not be null");
        Validate.notNull(pathToJsonResults, "pathToJsonResults must not be null");

        File[] textFiles = FileHelper.getFiles(pathToTexts.getPath(), "text");
        for (File textFile : textFiles) {
            String text = FileHelper.readFileToString(textFile);
            File jsonFile = new File(pathToJsonResults, textFile.getName().replace(".txt", ".json"));
            String jsonResponse = FileHelper.readFileToString(jsonFile);
            try {
                List<LocationAnnotation> annotations = createAnnotations(jsonResponse, text);
                data.put(text.hashCode(), annotations);
            } catch (JSONException e) {
                LOGGER.warn("Error while parsing JSON for result file {}", jsonFile);
                data.put(text.hashCode(), Collections.<LocationAnnotation> emptyList());
            }
        }
        LOGGER.info("Loaded {} texts for evaluation.", data.size());
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        if (data.containsKey(inputText.hashCode())) {
            return data.get(inputText.hashCode());
        }
        throw new IllegalStateException(
                "No cached text available. Texts to be tagged must be loaded in advance, as this extractor is only used for evaluation purposes.");
    }

    @Override
    public String getName() {
        return "Unlock Text";
    }

}
