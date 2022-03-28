package ws.palladian.extraction.location.disambiguation;

import ws.palladian.extraction.location.ClassifiedAnnotation;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.MultiMap;

import java.util.Set;

public interface LocationFeatureExtractor {
    Set<ClassifiableLocation> extract(String text, MultiMap<? extends ClassifiedAnnotation, Location> locations);
}