package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.extraction.entity.NamedEntityRecognizer;

public abstract class LocationExtractor extends NamedEntityRecognizer {

    @Override
    public abstract List<LocationAnnotation> getAnnotations(String inputText);

}
