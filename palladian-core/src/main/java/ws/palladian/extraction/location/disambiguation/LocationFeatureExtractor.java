package ws.palladian.extraction.location.disambiguation;

import java.util.Set;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.ContextClassifier.ClassifiedAnnotation;
import ws.palladian.helper.collection.MultiMap;

public interface LocationFeatureExtractor {

    Set<ClassifiableLocation> extract(String text, MultiMap<ClassifiedAnnotation, Location> locations);

}