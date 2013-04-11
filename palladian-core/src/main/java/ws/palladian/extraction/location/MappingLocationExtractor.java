package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CaseInsensitiveMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotated;

public class MappingLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingLocationExtractor.class);

    private final NamedEntityRecognizer entityRecognizer;
    private final CaseInsensitiveMap<LocationType> mapping;

    public MappingLocationExtractor(NamedEntityRecognizer entityRecognizer, Map<String, LocationType> mapping) {
        this.entityRecognizer = entityRecognizer;
        this.mapping = CaseInsensitiveMap.from(mapping);
    }

    protected LocationType map(String value) {
        return mapping.get(value);
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        List<? extends Annotated> annotations = entityRecognizer.getAnnotations(inputText);
        for (Annotated annotation : annotations) {
            LocationType mappedType = map(annotation.getTag());
            if (mappedType == null) {
                LOGGER.debug("No mapping for tag {}, will be dropped", annotation.getTag());
            } else {
                Location location = new ImmutableLocation(0, annotation.getValue(), mappedType, null, null, null);
                result.add(new LocationAnnotation(annotation, location));
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return entityRecognizer.getName();
    }

}
