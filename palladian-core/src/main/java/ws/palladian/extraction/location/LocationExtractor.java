package ws.palladian.extraction.location;

import ws.palladian.extraction.entity.NamedEntityRecognizer;

public abstract class LocationExtractor extends NamedEntityRecognizer {

//    public abstract List<Location> detectLocations(String text);
//
//    @Override
//    public Annotations getAnnotations(String inputText) {
//        Annotations annotations = new Annotations();
//        List<Location> locations = detectLocations(inputText);
//        for (Location location : locations) {
//            Annotation annotation = new Annotation(location.getStartPosition(), location.getPrimaryName(), location
//                    .getType().name());
//            annotations.add(annotation);
//        }
//
//        return annotations;
//    }
}
