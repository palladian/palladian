package ws.palladian.extraction.location;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.helper.collection.CollectionHelper;

public abstract class WebBasedLocationExtractor extends LocationExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebBasedLocationExtractor.class);

    private final NamedEntityRecognizer entityRecognizer;

    public WebBasedLocationExtractor(NamedEntityRecognizer entityRecognizer) {
        this.entityRecognizer = entityRecognizer;
    }

    protected abstract LocationType map(String value);

    @Override
    public String getModelFileEnding() {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public boolean setsModelFileEndingAutomatically() {
        return false;
    }

    @Override
    public boolean loadModel(String configModelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public Annotations getAnnotations(String inputText, String configModelFilePath) {
        LOGGER.warn("the configModelFilePath is ignored");
        return getAnnotations(inputText);
    }

    @Override
    public boolean train(String trainingFilePath, String modelFilePath) {
        throw new UnsupportedOperationException(
                "this location detector does not support training and does not work with model files");
    }

    @Override
    public Annotations getAnnotations(String inputText) {
        Annotations annotations = entityRecognizer.getAnnotations(inputText);
        List<Annotation> unmappedAnnotations = CollectionHelper.newArrayList();
        for (Annotation annotation : annotations) {
            LocationType mappedType = map(annotation.getMostLikelyTagName());
            if (mappedType == null) {
                LOGGER.warn("No mapping for tag {}, will be dropped", annotation.getMostLikelyTagName());
                unmappedAnnotations.add(annotation);
            }
            CategoryEntries categoryEntries = new CategoryEntries();
            categoryEntries.add(new CategoryEntry(mappedType.toString(), 1));
            annotation.setTags(categoryEntries);
        }
        annotations.removeAll(unmappedAnnotations);
        return annotations;
    }

}
