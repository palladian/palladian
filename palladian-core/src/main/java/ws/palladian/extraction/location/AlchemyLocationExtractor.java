package ws.palladian.extraction.location;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.tagger.AlchemyNer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.PositionAnnotation;

public class AlchemyLocationExtractor implements LocationExtractor {

    private static final Map<String,LocationType> LOCATION_MAPPING;
    
    private final String apiKey;
    
    static {
        Map<String, LocationType> temp = CollectionHelper.newHashMap();
        temp.put("city", LocationType.CITY);
        temp.put("country", LocationType.COUNTRY);
        temp.put("facility", LocationType.POI);
        temp.put("geographicfeature", LocationType.LANDMARK);
        temp.put("region", LocationType.REGION);
        temp.put("stateorcounty", LocationType.UNIT);
        LOCATION_MAPPING = Collections.unmodifiableMap(temp);
    }
    
    public AlchemyLocationExtractor(String apiKey) {
        Validate.notNull(apiKey, "apiKey must not be null");
        this.apiKey = apiKey;
    }

    @Override
    public List<Location> detectLocations(String text) {

        List<Location> detectedLocations = CollectionHelper.newArrayList();

        AlchemyNer alchemyNer = new AlchemyNer(apiKey);
        Annotations annotations = alchemyNer.getAnnotations(text);

        int index = 1;
        for (Annotation annotation : annotations) {
            if (LOCATION_MAPPING.containsKey(annotation.getMostLikelyTagName().toLowerCase())) {

                // FIXME setPrimaryName and uncool positional annotation init, INDEX???
                PositionAnnotation positionAnnotation = new PositionAnnotation("location", annotation.getOffset(),
                        annotation.getEndIndex(), index, annotation.getEntity());
                Location location = new Location(positionAnnotation);
                location.setPrimaryName(annotation.getEntity());
                location.setType(LOCATION_MAPPING.get(annotation.getMostLikelyTagName()));
                detectedLocations.add(location);

                index++;
            }
        }

        return detectedLocations;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
