package ws.palladian.extraction.feature;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * The NGramCreator creates token/word n-grams and stores them as {@link AnnotationGroup}s. For example, using an n-gram
 * length of 2, for the list of {@link PositionAnnotation}s [<i>the, quick, brown, fox, jumps, over, the, lazy, dog</i>], the
 * following {@link AnnotationGroup}s will be created: [<i>the quick</i>, <i>quick brown</i>, <i>brown fox</i>, ...].
 * {@link AnnotationGroup}s will only be created for <i>consecutive</i> {@link PositionAnnotation}s determined by their
 * {@link PositionAnnotation#getIndex()}. This means, if there are holes between the supplied annotations (e.g. by stopwords
 * which have been removed in advance), no {@link AnnotationGroup}s are created spanning these holes.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class NGramCreator extends StringDocumentPipelineProcessor {

    private final int minLength;
    private final int maxLength;
    private final String[] considerableFeatureDescriptors;

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates 2-grams.
     * </p>
     */
    public NGramCreator(String... considerableFeatureDescriptors) {
        this(2, 2, considerableFeatureDescriptors);
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates [2, maxLength]-grams.
     * </p>
     * 
     * @param maxLength
     */
    public NGramCreator(int maxLength, String... considerableFeatureDescriptors) {
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
    public NGramCreator(int minLength, int maxLength, String... considerableFeatureDescriptors) {
        super();

        Validate.notNull(considerableFeatureDescriptors, "considerableFeatureDescriptors must not be null");
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minLength);
        Validate.inclusiveBetween(minLength, Integer.MAX_VALUE, maxLength);

        this.minLength = minLength;
        this.maxLength = maxLength;
        this.considerableFeatureDescriptors = considerableFeatureDescriptors;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
//        FeatureVector featureVector = document.getFeatureVector();
//        TextAnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("The required feature "
//                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " is missing.");
//        }
        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
//        List<AnnotationGroup<String>> gramTokens = new ArrayList<AnnotationGroup<String>>();
        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
        for (int i = minLength; i <= maxLength; i++) {
            List<PositionAnnotation> nGramTokens = createNGrams(document, annotations, i);
            gramTokens.addAll(nGramTokens);
        }
//        annotations.addAll(gramTokens);
        document.getFeatureVector().addAll(gramTokens);
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
    private List<PositionAnnotation> createNGrams(PipelineDocument<String> document, List<PositionAnnotation> annotations,
            int length) {
        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
        PositionAnnotation[] tokensArray = annotations.toArray(new PositionAnnotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            List<PositionAnnotation> group = CollectionHelper.newArrayList();
            for (int j = i; j < i + length; j++) {
                group.add(tokensArray[j]);
            }
            if (isConsecutive(group)) {
                PositionAnnotation mergedGroup = postProcess(group);
                gramTokens.add(mergedGroup);
            }
        }
        return gramTokens;
    }

    /**
     * <p>
     * Re-create {@link NominalFeature}s for the created {@link AnnotationGroup}. This is done by simply concatenating
     * the {@link NominalFeature}s together. E.g. an {@link AnnotationGroup} of size two, with the
     * {@link NominalFeature}s "AT" and "JJ" for POS tags of its {@link PositionAnnotation}s get an annotation "ATJJ".
     * </p>
     * 
     * @param gramToken
     */
    //
    // TODO This method can currently be overridden by subclasses to customize the behavior, but it would be better to
    // introduce a "CombinationStrategy" which can be also applied to NumericFeatures, e.g. by taking average/min/max
    // etc. -- Philipp, 2012-06-19
    //
    protected PositionAnnotation postProcess(List<PositionAnnotation> gramToken) {
        String name = null;
        int newStart = -1;
        int newEnd = -1;
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < gramToken.size(); i++) {
            PositionAnnotation current = gramToken.get(i);
            if (i == 0) {
                name = current.getName();
                newStart = current.getStartPosition();
            }
            if (i == gramToken.size() - 1) {
                newEnd = current.getEndPosition();
            }
            newValue.append(current.getValue()).append(' ');
        }
        
        if (name == null || newStart == -1 || newEnd == -1) {
            throw new IllegalStateException("Yo, something is fucked up.");
        }
        
        PositionAnnotation ret = new PositionAnnotation(name, newStart, newEnd, 0, newValue.toString().trim());
        
        // combine NominalFeatures
        for (String descriptor : considerableFeatureDescriptors) {
            List<String> components = CollectionHelper.newArrayList();
            for (PositionAnnotation annotation : gramToken) {
                String value = annotation.getFeatureVector().getFeature(NominalFeature.class, descriptor).getValue();
                components.add(value);
            }
            NominalFeature newFeature = new NominalFeature(descriptor, StringUtils.join(components, ""));
            ret.getFeatureVector().add(newFeature);
        }
        
        
//        PositionAnnotation firstAnnotation = CollectionHelper.getFirst(gramToken);
//        PositionAnnotation lastAnnotation = CollectionHelper.getLast(gramToken);
//        PositionAnnotation ret = new PositionAnnotation(firstAnnotation.getName(), firstAnnotation.getStartPosition(), lastAnnotation.getEndPosition(), firstAnnotation.getIndex());
//        for (String descriptor : considerableFeatureDescriptors) {
//            List<String> components = new ArrayList<String>();
//            for (PositionAnnotation annotation : gramToken) {
//                String value = annotation.getFeatureVector().getFeature(NominalFeature.class, descriptor).getValue();
//                components.add(value);
//            }
//            NominalFeature newFeature = new NominalFeature(descriptor, StringUtils.join(components, ""));
//            ret.getFeatureVector().add(newFeature);
//            //gramToken.addFeature(newFeature);
//        }
        
        return ret;
    }

    /**
     * <p>
     * Check, whether the {@link PositionAnnotation}s in the supplied {@link AnnotationGroup} are consecutive, i.e. the
     * difference between two following {@link PositionAnnotation}s in the group is one for each pair.
     * </p>
     * 
     * @param annotationGroup The {@link AnnotationGroup} for which to verify consecutiveness of {@link PositionAnnotation}s.
     * @return <code>true</code>, if {@link PositionAnnotation}s are consecutive, <code>false</code> otherwise.
     */
    private boolean isConsecutive(List<PositionAnnotation> annotationGroup) {
        boolean ret = true;
        int index = -1;
        for (PositionAnnotation annotation : annotationGroup) {
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