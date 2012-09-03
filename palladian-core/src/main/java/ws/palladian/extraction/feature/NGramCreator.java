package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationGroup;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.TextAnnotationFeature;

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
 * @author Klemens Muthmann
 */
public class NGramCreator extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    private final int minLength;
    private final int maxLength;
    private final FeatureDescriptor<NominalFeature>[] considerableFeatureDescriptors;

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates 2-grams.
     * </p>
     */
    public NGramCreator(final FeatureDescriptor<NominalFeature>... considerableFeatureDescriptors) {
        this(2, 2, considerableFeatureDescriptors);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates [2, maxLength]-grams.
     * </p>
     * 
     * @param maxLength
     */
    public NGramCreator(int maxLength, final FeatureDescriptor<NominalFeature>... considerableFeatureDescriptors) {
        this(2, maxLength, considerableFeatureDescriptors);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates [minLength, maxLength]-grams.
     * </p>
     * 
     * @param minLength
     * @param maxLength
     * @param considerableFeatureDescriptors
     */
    public NGramCreator(int minLength, int maxLength,
            final FeatureDescriptor<NominalFeature>... considerableFeatureDescriptors) {
        super();

        Validate.notNull(considerableFeatureDescriptors, "considerableFeatureDescriptors must not be null");
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minLength);
        Validate.inclusiveBetween(minLength, Integer.MAX_VALUE, maxLength);

        this.minLength = minLength;
        this.maxLength = maxLength;
        this.considerableFeatureDescriptors = considerableFeatureDescriptors;
    }

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        TextAnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature "
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " is missing.");
        }
        List<Annotation<String>> annotations = annotationFeature.getValue();
        List<AnnotationGroup<String>> gramTokens = new ArrayList<AnnotationGroup<String>>();
        for (int i = minLength; i <= maxLength; i++) {
            List<AnnotationGroup<String>> nGramTokens = createNGrams(document, annotations, i);
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
    private List<AnnotationGroup<String>> createNGrams(PipelineDocument<String> document, List<Annotation<String>> annotations,
            int length) {
        List<AnnotationGroup<String>> gramTokens = new ArrayList<AnnotationGroup<String>>();
        @SuppressWarnings("unchecked")
        Annotation<String>[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            AnnotationGroup<String> gramToken = new AnnotationGroup<String>(document);
            for (int j = i; j < i + length; j++) {
                gramToken.add(tokensArray[j]);
            }
            if (isConsecutive(gramToken)) {
                postProcess(gramToken);
                gramTokens.add(gramToken);
            }
        }
        return gramTokens;
    }

    /**
     * <p>
     * Re-create {@link NominalFeature}s for the created {@link AnnotationGroup}. This is done by simply concatenating
     * the {@link NominalFeature}s together. E.g. an {@link AnnotationGroup} of size two, with the
     * {@link NominalFeature}s "AT" and "JJ" for POS tags of its {@link Annotation}s get an annotation "ATJJ".
     * </p>
     * 
     * @param gramToken
     */
    //
    // TODO This method can currently be overridden by subclasses to customize the behavior, but it would be better to
    // introduce a "CombinationStrategy" which can be also applied to NumericFeatures, e.g. by taking average/min/max
    // etc. -- Philipp, 2012-06-19
    //
    protected void postProcess(AnnotationGroup<String> gramToken) {
        for (FeatureDescriptor<NominalFeature> descriptor : considerableFeatureDescriptors) {
            List<String> components = new ArrayList<String>();
            for (Annotation<String> annotation : gramToken.getAnnotations()) {
                String value = annotation.getFeature(descriptor).getValue();
                components.add(value);
            }
            NominalFeature newFeature = new NominalFeature(descriptor, StringUtils.join(components, ""));
            gramToken.addFeature(newFeature);
        }
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
    private boolean isConsecutive(AnnotationGroup<String> annotationGroup) {
        boolean ret = true;
        int index = -1;
        for (Annotation<String> annotation : annotationGroup.getAnnotations()) {
            int currentIndex = annotation.getIndex();
            if (index != -1 && index + 1 != currentIndex) {
                ret = false;
                break;
            }
            index = currentIndex;
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NGramCreator [minLength=");
        builder.append(minLength);
        builder.append(", maxLength=");
        builder.append(maxLength);
        builder.append("]");
        return builder.toString();
    }

}