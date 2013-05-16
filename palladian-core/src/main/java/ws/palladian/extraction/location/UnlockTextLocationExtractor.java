package ws.palladian.extraction.location;

import java.util.Iterator;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * TODO currently just a parser for the JSON data. Not trivial to integrate, as text files need to be present on a web
 * server and processing takes time.
 * 
 * @author Philipp Katz
 */
public class UnlockTextLocationExtractor extends LocationExtractor {

    static List<Location> parse(String jsonInput) {
        List<Location> locations = CollectionHelper.newArrayList();
        try {
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
                Double lng = null;
                Double lat = null;
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
                        lng = locationObj.getDouble("long");
                        lat = locationObj.getDouble("lat");
                        break;
                    }
                }
                locations
                        .add(new ImmutableLocation(id, name, altNames, LocationType.UNDETERMINED, lat, lng, pop, null));
            }

        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return locations;
    }

    static List<LocationAnnotation> annotate(String jsonResponse, String text) {
        Annotations<LocationAnnotation> annotations = new Annotations<LocationAnnotation>();
        List<Location> locations = parse(jsonResponse);
        for (Location location : locations) {
            String name = location.getPrimaryName();
            List<Integer> indices = StringHelper.getOccurrenceIndices(text, name);
            for (Integer idx : indices) {
                annotations.add(new LocationAnnotation(idx, idx + name.length(), name, location));
            }
        }
        annotations.sort();
        annotations.removeNested();
        return annotations;
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        throw new UnsupportedOperationException("No yet implemented");
        // TODO Auto-generated method stub
    }

    @Override
    public String getName() {
        return "Unlock Text";
    }

}
