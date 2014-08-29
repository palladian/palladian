package ws.palladian.extraction.location;

import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.extraction.location.ContextClassifier.ClassificationMode;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;

public final class DefaultLocationTagger implements ClassifyingTagger {

    /** Long annotations exceeding the specified token count, are split up and parts of them are treated as candidates. */
    public final static int LONG_ANNOTATION_SPLIT = 3;

    private static final EntityPreprocessingTagger tagger = new EntityPreprocessingTagger(LONG_ANNOTATION_SPLIT);

    private static final AnnotationFilter filter = new AnnotationFilter();

    private static final ContextClassifier contextClassifier = new ContextClassifier(ClassificationMode.PROPAGATION);

    public static final DefaultLocationTagger INSTANCE = new DefaultLocationTagger();

    private DefaultLocationTagger() {
        // singleton
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String text) {
        List<Annotation> taggedEntities = tagger.getAnnotations(text);
        taggedEntities = filter.filter(taggedEntities);
        List<ClassifiedAnnotation> classifiedEntities = contextClassifier.classify(taggedEntities, text);

        CollectionHelper.remove(classifiedEntities, new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation item) {
                String value = item.getValue();
                // the probability, that we are wrong when tagging one or two-letter abbreviations is very high, so we
                // discard them here, except for "US" and "UK".
                return value.equals("US") || value.equals("UK") || !value.matches("[A-Z]{1,2}|[A-Z]\\.");
            }
        });
        return classifiedEntities;
    }

}
