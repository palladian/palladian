package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.AnnotationGroup;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;

/**
 * <p>
 * The NGramCreator creates token/word n-grams and stores them as {@link AnnotationGroup}s. For example, using an n-gram
 * length of 2, for the list of {@link Annotation}s [<i>the, quick, brown, fox, jumps, over, the, lazy, dog</i>], the
 * following {@link AnnotationGroup}s will be created: [<i>the quick</i>, <i>quick brown</i>, <i>brown fox</i>, ...].
 * {@link AnnotationGroup}s will only be created for <i>consecutive</i> {@link Annotation}s determined by their
 * {@link Annotation#getIndex()}. This means, if there are holes between the supplied annotations (e.g. by stopwords
 * which have been removed in advance), no {@link AnnotationGroup}s are created spanning these holes.
 * </p>
 * 
 * @author Philipp Katz
 */
public class NGramCreator2 implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    private final int minLength;
    private final int maxLength;

    /**
     * <p>
     * Create a new {@link NGramCreator2} which calculates 2-grams.
     * </p>
     */
    public NGramCreator2() {
        this(2, 2);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator2} which calculates (2-maxLength)-grams.
     * </p>
     * 
     * @param maxLength
     */
    public NGramCreator2(int maxLength) {
        this(2, maxLength);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator2} which calculates (minLength-maxLength)-grams.
     * </p>
     * 
     * @param minLength
     * @param maxLength
     */
    public NGramCreator2(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public void process(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature " + TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR
                    + " is missing.");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        for (int i = minLength; i <= maxLength; i++) {
            List<AnnotationGroup> nGramTokens = createNGrams(document, annotations, i);
            gramTokens.addAll(nGramTokens);
        }
        annotations.addAll(gramTokens);
    }

    /**
     * <p>
     * Create n-grams of annotations with the specified length.
     * </p>
     * 
     * @param document
     * @param annotations
     * @param length
     * @return
     */
    private List<AnnotationGroup> createNGrams(PipelineDocument document, List<Annotation> annotations, int length) {
        List<AnnotationGroup> gramTokens = new ArrayList<AnnotationGroup>();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            AnnotationGroup gramToken = new AnnotationGroup(document);
            // FIXME those extra processing steps should go to their own NGramPostprocessorAnnotator
            List<String> unstems = new ArrayList<String>();
//            List<String> posTags = new ArrayList<String>();
            for (int j = i; j < i + length; j++) {
                gramToken.add(tokensArray[j]);
                unstems.add(tokensArray[j].getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue());
//                posTags.add(tokensArray[j].getFeatureVector().get(BasePosTagger.PROVIDED_FEATURE_DESCRIPTOR).getValue());
            }
            gramToken.getFeatureVector().add(new NominalFeature(StemmerAnnotator.UNSTEM, StringUtils.join(unstems, " ")));
//            gramToken.getFeatureVector().add(new NominalFeature(BasePosTagger.PROVIDED_FEATURE_DESCRIPTOR, StringUtils.join(posTags, "")));
            if (isConsecutive(gramToken)) {
                gramTokens.add(gramToken);
            }
        }
        return gramTokens;
    }

    /**
     * <p>
     * Check, whether the {@link Annotation}s in the supplied {@link AnnotationGroup} are consecutive, i.e. the
     * difference between two following {@link Annotation}s in the group is one for each pair.
     * </p>
     * 
     * @param annotationGroup The {@link AnnotationGroup} for which to verify consecutiveness of {@link Annotation}s.
     * @return <code>true</code>, if {@link Annotation}s are consecutive, <code>false</code> otherwise.
     */
    private boolean isConsecutive(AnnotationGroup annotationGroup) {
        boolean ret = true;
        int index = -1;
        for (Annotation annotation : annotationGroup.getAnnotations()) {
            int currentIndex = annotation.getIndex();
            if (index != -1 && index + 1 != currentIndex) {
                ret = false;
                break;
            }
            index = currentIndex;
        }
        return ret;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NGramCreator2 [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append("]");
        return builder.toString();
    }

}