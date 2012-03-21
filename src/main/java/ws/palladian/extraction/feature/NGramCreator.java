package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * The NGramCreator creates token n-grams and stores them {@link AnnotationGroup}s. For example, using n-gram length of
 * 2, for the list of {@link Annotation}s [<i>the, quick, brown, fox, jumps, over, the, lazy, dog</i>], the following
 * {@link AnnotationGroup}s will be created: [<i>the quick</i>, <i>quick brown</i>, <i>brown fox</i>, ...].
 * </p>
 * 
 * @author Philipp Katz
 */
public class NGramCreator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    private final int minLength;
    private final int maxLength;

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates 2-grams.
     * </p>
     */
    public NGramCreator() {
        this(2, 2);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates (2-maxLength)-grams.
     * </p>
     * 
     * @param maxLength
     */
    public NGramCreator(int maxLength) {
        this(2, maxLength);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates (minLength-maxLength)-grams.
     * </p>
     * 
     * @param minLength
     * @param maxLength
     */
    public NGramCreator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        for (int i = minLength; i <= maxLength; i++) {
            List<AnnotationGroup> nGramTokens = createNGrams(document, annotations, i);
            gramTokens.addAll(nGramTokens);
        }
        annotations.addAll(gramTokens);
    }

    private List<AnnotationGroup> createNGrams(PipelineDocument document, List<Annotation> annotations, int length) {
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            AnnotationGroup gramToken = new AnnotationGroup(document);
            for (int j = i; j < i + length; j++) {
                gramToken.add(tokensArray[j]);
            }
            gramTokens.add(gramToken);
        }
        return gramTokens;
    }

}