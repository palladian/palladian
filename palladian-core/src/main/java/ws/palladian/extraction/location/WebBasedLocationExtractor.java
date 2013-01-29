package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.PositionAnnotation;

public abstract class WebBasedLocationExtractor implements LocationExtractor {

    private NamedEntityRecognizer ner;
    protected static Map<String, LocationType> LOCATION_MAPPING;

    public WebBasedLocationExtractor(NamedEntityRecognizer ner) {
        this.ner = ner;
    }

    @Override
    public List<Location> detectLocations(String text) {

        List<Location> detectedLocations = CollectionHelper.newArrayList();

        Annotations annotations = ner.getAnnotations(text);

        int index = 1;
        for (Annotation annotation : annotations) {
            if (LOCATION_MAPPING.containsKey(annotation.getMostLikelyTagName().toLowerCase())) {

                // FIXME setPrimaryName and uncool positional annotation init, INDEX???
                PositionAnnotation positionAnnotation = new PositionAnnotation("location", annotation.getOffset(),
                        annotation.getEndIndex(), index, annotation.getEntity());
                Location location = new Location(positionAnnotation);
                location.setPrimaryName(annotation.getEntity());
                location.setType(LOCATION_MAPPING.get(annotation.getMostLikelyTagName().toLowerCase()));
                detectedLocations.add(location);

                index++;
            }
        }

        return detectedLocations;
    }

}
