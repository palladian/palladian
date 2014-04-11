package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * The NGramCreator creates token/word n-grams and stores them as {@link AnnotationGroup}s. For example, using an n-gram
 * length of 2, for the list of {@link PositionAnnotation}s [<i>the, quick, brown, fox, jumps, over, the, lazy,
 * dog</i>], the following {@link AnnotationGroup}s will be created: [<i>the quick</i>, <i>quick brown</i>, <i>brown
 * fox</i>, ...]. {@link AnnotationGroup}s will only be created for <i>consecutive</i> {@link PositionAnnotation}s
 * determined by their {@link PositionAnnotation#getIndex()}. This means, if there are holes between the supplied
 * annotations (e.g. by stopwords which have been removed in advance), no {@link AnnotationGroup}s are created spanning
 * these holes.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class NGramCreator extends TextDocumentPipelineProcessor {

    private final int minLength;
    private final int maxLength;
    private final String[] considerableFeatureDescriptors;
    private final boolean unique;
    private final int limit;

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
     * @param minLength The minimal length of the n-gram.
     * @param maxLength The maximal length of the n-gram.
     * @param considerableFeatureDescriptors
     */
    public NGramCreator(int minLength, int maxLength, String... considerableFeatureDescriptors) {
        Validate.notNull(considerableFeatureDescriptors, "considerableFeatureDescriptors must not be null");
        Validate.inclusiveBetween(1, Integer.MAX_VALUE, minLength);
        Validate.inclusiveBetween(minLength, Integer.MAX_VALUE, maxLength);

        this.minLength = minLength;
        this.maxLength = maxLength;
        this.considerableFeatureDescriptors = considerableFeatureDescriptors;
        this.unique = false;
        this.limit = Integer.MAX_VALUE;
    }

    /**
     * <p>
     * Create a new {@link NGramCreator} which calculates [minLength, maxLength]-grams.
     * </p>
     * 
     * @param minLength The minimal length of the n-gram.
     * @param maxLength The maximal length of the n-gram.
     * @param unique <code>true</code> to skip duplicate values, <code>false</code> to keep duplicates.
     * @param limit Limit the number of created annotations to the specified count.
     * @param considerableFeatureDescriptors
     */
    public NGramCreator(int minLength, int maxLength, boolean unique, int limit,
            String... considerableFeatureDescriptors) {
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.unique = unique;
        this.limit = limit;
        this.considerableFeatureDescriptors = considerableFeatureDescriptors;
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        ListFeature<PositionAnnotation> annotations = AbstractTokenizer.getTokenAnnotations(document);
        if (annotations.size() < limit) {
            for (int i = minLength; i <= maxLength; i++) {
                List<PositionAnnotation> nGramTokens = createNGrams(document, annotations, i);
                annotations.addAll(nGramTokens);
                if (annotations.size() >= limit) {
                    break;
                }
            }
        }
        if (unique) {
            List<PositionAnnotation> temp = CollectionHelper.newArrayList();
            Set<String> deduplication = CollectionHelper.newHashSet();
            for (PositionAnnotation ann : annotations) {
                if (deduplication.add(ann.getValue())) {
                    temp.add(ann);
                }
            }
            annotations.clear();
            annotations.addAll(temp);
        }
        if (annotations.size() >= limit) {
            annotations.retainAll(new ArrayList<PositionAnnotation>(annotations.subList(0, limit)));
        }
    }

    /**
     * <p>
     * Create n-grams of annotations with the specified length.
     * </p>
     * 
     * @param document
     * @param annotations
     * @param n
     * @return
     */
    private List<PositionAnnotation> createNGrams(TextDocument document, List<PositionAnnotation> annotations, int n) {
        List<PositionAnnotation> gramTokens = CollectionHelper.newArrayList();
        PositionAnnotation[] tokensArray = annotations.toArray(new PositionAnnotation[annotations.size()]);
        tokenLoop: for (int i = 0; i < tokensArray.length - n + 1; i++) {
            List<PositionAnnotation> group = CollectionHelper.newArrayList();

            // for checking, if we have consecutive annotations
            int lastEnd = tokensArray[i].getEndPosition();

            for (int j = i; j < i + n; j++) {
                PositionAnnotation current = tokensArray[j];
                group.add(current);
                if (j > i) {
                    if (lastEnd + 1 != current.getStartPosition()) {
                        continue tokenLoop; // skip adding this n-gram, as its tokens were not consecutive
                    }
                    lastEnd = current.getEndPosition();
                }
            }
            PositionAnnotation mergedGroup = postProcess(group);
            if (mergedGroup != null) {
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
        int newStart = -1;
        StringBuilder newValue = new StringBuilder();
        for (int i = 0; i < gramToken.size(); i++) {
            PositionAnnotation current = gramToken.get(i);
            if (i == 0) {
                newStart = current.getStartPosition();
            }
            newValue.append(current.getValue()).append(' ');
        }

        if (newStart == -1) {
            throw new IllegalStateException("Yo, something is fucked up.");
        }

        String trimmedValue = newValue.toString().trim();
        if (!trimmedValue.isEmpty()) {
            PositionAnnotation ret = new PositionAnnotation(trimmedValue, newStart);

            // combine NominalFeatures
            for (String descriptor : considerableFeatureDescriptors) {
                List<String> components = CollectionHelper.newArrayList();
                for (PositionAnnotation annotation : gramToken) {
                    String value = annotation.getFeatureVector().get(NominalFeature.class, descriptor).getValue();
                    components.add(value);
                }
                NominalFeature newFeature = new NominalFeature(descriptor, StringUtils.join(components, ""));
                ret.getFeatureVector().add(newFeature);
            }
            return ret;
        } else {
            return null;
        }

    }

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