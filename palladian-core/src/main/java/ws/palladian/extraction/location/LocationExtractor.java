package ws.palladian.extraction.location;

import ws.palladian.extraction.entity.NamedEntityRecognizer;

import java.util.List;

public abstract class LocationExtractor extends NamedEntityRecognizer {
    @Override
    public abstract List<LocationAnnotation> getAnnotations(String inputText);
}
