package ws.palladian.extraction.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * This {@link PipelineProcessor} calculates various token specific metrics, like relative position of first/last token
 * occurrence, spread across the document, the number of characters and words for each token, the count token, the
 * frequency of each token. Token frequencies are calculated based on a <i>normalized term frequency</i>, as described
 * in "Information Retrieval", Grossman, Frieder, p. 32. This means, that the frequencies are normalized by the maximum
 * term frequency which appears in the considered document. The calculation is performed <b>case insensitively</b>.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TokenMetricsCalculator extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final FeatureDescriptor<NumericFeature> FIRST = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.first", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> LAST = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.last", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> COUNT = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.count", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> FREQUENCY = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.frequency", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> SPREAD = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.spread", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> CHAR_LENGTH = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.length.char", NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> WORD_LENGTH = FeatureDescriptorBuilder.build(
            "ws.palladian.features.tokens.length.word", NumericFeature.class);

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        AnnotationFeature annotationFeature = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature "
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " is missing.");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Bag<String> occurrences = new HashBag<String>();
        Map<String, Integer> firstOccurrences = new HashMap<String, Integer>();
        Map<String, Integer> lastOccurrences = new HashMap<String, Integer>();
        int lastPosition = 0;
        for (Annotation annotation : annotations) {
            // changed to lower case, 2012-05-01
            String value = annotation.getValue().toLowerCase();
            occurrences.add(value);
            int tokenPosition = annotation.getIndex();
            if (tokenPosition == -1) {
                throw new DocumentUnprocessableException(
                        "Token index is missing, looks like the used Tokenizer implementation needs to be updated for supplying indices.");
            }
            Integer firstOccurrence = firstOccurrences.get(value);
            if (firstOccurrence == null) {
                firstOccurrences.put(value, tokenPosition);
            } else {
                firstOccurrences.put(value, Math.min(tokenPosition, firstOccurrence));
            }
            Integer lastOccurrence = lastOccurrences.get(value);
            if (lastOccurrence == null) {
                lastOccurrences.put(value, tokenPosition);
            } else {
                lastOccurrences.put(value, Math.max(tokenPosition, lastOccurrence));
            }
            lastPosition = Math.max(tokenPosition, lastPosition);
        }

        // calculate "normalized term frequency", see "Information Retrieval", Grossman/Frieder, p. 32
        int maxCount = 1;
        for (String token : occurrences.uniqueSet()) {
            maxCount = Math.max(maxCount, occurrences.getCount(token));
        }

        for (Annotation annotation : annotations) {
            // changed to lower case, 2012-05-01
            String value = annotation.getValue().toLowerCase();
            double first = (double)firstOccurrences.get(value) / lastPosition;
            double last = (double)lastOccurrences.get(value) / lastPosition;
            double count = occurrences.getCount(value);
            double frequency = count / maxCount;
            double spread = last - first;
            double charLength = value.length();
            double wordLength = value.split(" ").length;
            FeatureVector featureVector = annotation.getFeatureVector();
            featureVector.add(new NumericFeature(FIRST, first));
            featureVector.add(new NumericFeature(LAST, last));
            featureVector.add(new NumericFeature(COUNT, count));
            featureVector.add(new NumericFeature(FREQUENCY, frequency));
            featureVector.add(new NumericFeature(SPREAD, spread));
            featureVector.add(new NumericFeature(CHAR_LENGTH, charLength));
            featureVector.add(new NumericFeature(WORD_LENGTH, wordLength));
        }
    }

}
