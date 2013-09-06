package ws.palladian.extraction.location;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CaseInsensitiveMap;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotation;

/**
 * <p>
 * A {@link LocationAnnotation} which serves as adapter for a {@link NamedEntityRecognizer}. Named Entity Tags are
 * converted to {@link LocationType}s using a specified mapping (see
 * {@link #MappingLocationExtractor(NamedEntityRecognizer, Map)}.
 * </p>
 * 
 * @author Philipp Katz
 */
public class MappingLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MappingLocationExtractor.class);

    private final NamedEntityRecognizer entityRecognizer;
    private final CaseInsensitiveMap<LocationType> mapping;

    /**
     * <p>
     * Create a new {@link MappingLocationExtractor} with the specified NER and mapping between entity tags and
     * {@link LocationType}s. Entity tags which do not appear in the mapping are dropped from the result.
     * </p>
     * 
     * @param entityRecognizer The {@link NamedEntityRecognizer} to adapt, not <code>null</code>.
     * @param mapping Mapping between NER tag and LocationType, case insensitive, not <code>null</code>.
     */
    public MappingLocationExtractor(NamedEntityRecognizer entityRecognizer, Map<String, LocationType> mapping) {
        Validate.notNull(entityRecognizer, "entityRecognizer must not be null");
        Validate.notNull(mapping, "mapping must not be null");
        this.entityRecognizer = entityRecognizer;
        this.mapping = CaseInsensitiveMap.from(mapping);
    }

    @Override
    public List<LocationAnnotation> getAnnotations(String inputText) {
        List<LocationAnnotation> result = CollectionHelper.newArrayList();
        List<? extends Annotation> annotations = entityRecognizer.getAnnotations(inputText);
        // set of unmapped annotations, for debugging purposes
        Set<String> unmappedTags = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            LocationType mappedType = mapping.get(annotation.getTag());
            if (mappedType == null) {
                unmappedTags.add(annotation.getTag());
            } else {
                Location location = new ImmutableLocation(0, annotation.getValue(), mappedType, null, null, null);
                result.add(new LocationAnnotation(annotation, location));
            }
        }
        if (unmappedTags.size() > 0) {
            LOGGER.debug("The following tags were without a mapping: {}, annotations for them have been dropped",
                    StringUtils.join(unmappedTags, ","));
        }
        return result;
    }

    @Override
    public String getName() {
        return entityRecognizer.getName();
    }

}
