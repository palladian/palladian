package ws.palladian.extraction.location;

import java.util.List;
import java.util.Set;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.tagger.AlchemyNer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.PositionAnnotation;

public class AlchemyLocationExtractor implements LocationExtractor {

    private static final Set<String> LOCATION_TYPES = CollectionHelper.newHashSet();
    private String apiKey;

    public AlchemyLocationExtractor(String apiKey) {
        this.apiKey = apiKey;
        LOCATION_TYPES.add("city");
        LOCATION_TYPES.add("country");
        LOCATION_TYPES.add("facility");
        LOCATION_TYPES.add("geographicfeature");
        LOCATION_TYPES.add("region");
        LOCATION_TYPES.add("stateorcounty");
    }

    @Override
    public List<Location> detectLocations(String text) {

        List<Location> detectedLocations = CollectionHelper.newArrayList();

        AlchemyNer alchemyNer = new AlchemyNer(apiKey);
        Annotations annotations = alchemyNer.getAnnotations(text);

        int index = 1;
        for (Annotation annotation : annotations) {
            if (LOCATION_TYPES.contains(annotation.getMostLikelyTagName().toLowerCase())) {

                // FIXME setPrimaryName and uncool positional annotation init, INDEX???
                PositionAnnotation positionAnnotation = new PositionAnnotation("location", annotation.getOffset(),
                        annotation.getEndIndex(), index, annotation.getEntity());
                Location location = new Location(positionAnnotation);
                location.addName(annotation.getEntity());
                location.setType(annotation.getMostLikelyTagName());
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
