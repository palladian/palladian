package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntriesMap;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CollectionHelper;

public abstract class WebBasedLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebBasedLocationExtractor.class);

    private final NamedEntityRecognizer entityRecognizer;
    private final Map<String, LocationType> mapping;

    public WebBasedLocationExtractor(NamedEntityRecognizer entityRecognizer, Map<String, LocationType> mapping) {
        this.entityRecognizer = entityRecognizer;
        this.mapping = mapping;
    }

    protected LocationType map(String value) {
        return mapping.get(value.toLowerCase());
    }

    @Override
    public List<Annotation> getAnnotations(String inputText) {
        List<Annotation> annotations = entityRecognizer.getAnnotations(inputText);
        List<Annotation> unmappedAnnotations = CollectionHelper.newArrayList();
        for (Annotation annotation : annotations) {
            LocationType mappedType = map(annotation.getTag());
            if (mappedType == null) {
                LOGGER.debug("No mapping for tag {}, will be dropped", annotation.getTag());
                unmappedAnnotations.add(annotation);
            } else {
                CategoryEntriesMap categoryEntries = new CategoryEntriesMap();
                categoryEntries.set(mappedType.toString(), 1);
                annotation.setTags(categoryEntries);
            }
        }
        annotations.removeAll(unmappedAnnotations);
        return annotations;
    }

    @Override
    public String getName() {
        return entityRecognizer.getName();
    }

}
