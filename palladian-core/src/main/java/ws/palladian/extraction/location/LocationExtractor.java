package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.AbstractFeature;
import ws.palladian.processing.features.ListFeature;

public abstract class LocationExtractor extends NamedEntityRecognizer {
    
    public static final class LocationFeature extends AbstractFeature<LocationAnnotation> {
        public LocationFeature(String name, LocationAnnotation value) {
            super(name, value);
        }
    }

    @Override
    public abstract List<LocationAnnotation> getAnnotations(String inputText);
    
    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<LocationAnnotation> annotations = getAnnotations(document.getContent());
        ListFeature<LocationFeature> list = new ListFeature<LocationFeature>(PROVIDED_FEATURE);
        for (LocationAnnotation annotation : annotations) {
            list.add(new LocationFeature(annotation.getValue(), annotation));
        }
        document.getFeatureVector().add(list);
    }

}
