package ws.palladian.extraction.location;

import java.io.InputStream;
import java.util.List;

import ws.palladian.core.Annotation;
import ws.palladian.core.ClassifyingTagger;
import ws.palladian.helper.io.FileHelper;

public final class DefaultCandidateExtractor implements ClassifyingTagger {

    /** Path to the case dictionary within the resources. */
    private static final String CASE_DICTIONARY_RESOURCE = "/caseDictionary.csv";

    /** Path to the location rules withing the resources. */
    private static final String LOCATION_RULES_RESOURCE = "/location.rules";

    /** The threshold total:uppercase, above which tokens are considered being lowercase. */
    private static final double LOWERCASE_THRESHOLD = 2;

    /** Long annotations exceeding the specified token count, are split up and parts of them are treated as candidates. */
    private final static int LONG_ANNOTATION_SPLIT = 2;

    private final EntityPreprocessingTagger tagger;

    private final AnnotationRuleEngine ruleEngine;

    /** Get the singleton instance. */
    public static final DefaultCandidateExtractor INSTANCE = new DefaultCandidateExtractor();

    private DefaultCandidateExtractor() {
        InputStream inputStream = null;
        InputStream inputStream2 = null;
        try {
            inputStream = DefaultCandidateExtractor.class.getResourceAsStream(CASE_DICTIONARY_RESOURCE);
            inputStream2 = DefaultCandidateExtractor.class.getResourceAsStream(LOCATION_RULES_RESOURCE);
            tagger = new EntityPreprocessingTagger(inputStream, LOWERCASE_THRESHOLD, LONG_ANNOTATION_SPLIT);
            ruleEngine = new AnnotationRuleEngine(inputStream2);
        } finally {
            FileHelper.close(inputStream, inputStream2);
        }
    }

    @Override
    public List<ClassifiedAnnotation> getAnnotations(String text) {
        List<Annotation> taggedEntities = tagger.getAnnotations(text);
        List<ClassifiedAnnotation> classifiedEntities = ruleEngine.apply(text, taggedEntities);
        return classifiedEntities;
    }

}
