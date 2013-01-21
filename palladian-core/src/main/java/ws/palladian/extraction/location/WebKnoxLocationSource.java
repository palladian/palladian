package ws.palladian.extraction.location;

import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.helper.JsonObjectWrapper;

public class WebKnoxLocationSource implements LocationSource {

    private String apiKey;

    public WebKnoxLocationSource(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Set<Location> retrieveLocations(String locationName) {
        Set<Location> locations = CollectionHelper.newHashSet();
        DocumentRetriever documentRetriever = new DocumentRetriever();

        JSONArray locationCandidates = documentRetriever
                .getJsonArray("http://webknox.com/api/entities/search?entityName=" + locationName + "&apiKey=" + apiKey);

        for (int i = 0; i < locationCandidates.length(); i++) {
            JsonObjectWrapper locationCandidate;
            try {
                locationCandidate = new JsonObjectWrapper(locationCandidates.getJSONObject(i));
                String concept = locationCandidate.getString("concept");
                Double confidence = locationCandidate.getDouble("confidence");
                if ((concept.equalsIgnoreCase("city") || concept.equalsIgnoreCase("country")) && confidence > 0.999) {
                    JSONObject jsonObject = documentRetriever.getJsonObject("http://webknox.com/api/entities/"
                            + locationCandidate.getString("id") + "?apiKey=" + apiKey);
                    JsonObjectWrapper json = new JsonObjectWrapper(jsonObject);

                    Location location = new Location();
                    location.addName(locationCandidate.getString("name"));
                    
                    JSONArray facts = json.getJSONArray("facts");
                    for (int j = 0; j < facts.length(); j++) {
                        JsonObjectWrapper fact = new JsonObjectWrapper(facts.getJSONObject(j));
                        String key = fact.getString("key");
                        String value = fact.getString("value");
                        
                        if (key.equalsIgnoreCase("latitude")) {
                            location.setLatitude(Double.valueOf(value));
                        } else if (key.equalsIgnoreCase("longitude")) {
                            location.setLongitude(Double.valueOf(value));
                        } else if (key.equalsIgnoreCase("population")) {
                            location.setPopulation(Integer.valueOf(value));
                        }
                    }

                    locations.add(location);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return locations;
    }
}
