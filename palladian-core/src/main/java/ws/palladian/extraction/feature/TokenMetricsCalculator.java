package ws.palladian.extraction.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * This {@link PipelineProcessor} calculates various token specific metrics, like relative position of first/last token
 * occurrence, spread across the document, the number of characters and words for each token.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TokenMetricsCalculator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    private static final String PROVIDED_FEATURE_FIRST = "ws.palladian.features.tokens.first";
    private static final String PROVIDED_FEATURE_LAST = "ws.palladian.features.tokens.last";
    private static final String PROVIDED_FEATURE_SPREAD = "ws.palladian.features.tokens.spread";
    private static final String PROVIDED_FEATURE_CHAR_LENGTH = "ws.palladian.features.tokens.length.char";
    private static final String PROVIDED_FEATURE_WORD_LENGTH = "ws.palladian.features.tokens.length.word";

    public static final FeatureDescriptor<NumericFeature> FIRST = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE_FIRST, NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> LAST = FeatureDescriptorBuilder.build(PROVIDED_FEATURE_LAST,
            NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> SPREAD = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE_SPREAD, NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> CHAR_LENGTH = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE_CHAR_LENGTH, NumericFeature.class);
    public static final FeatureDescriptor<NumericFeature> WORD_LENGTH = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE_WORD_LENGTH, NumericFeature.class);

    @Override
    public void process(PipelineDocument document) {
        AnnotationFeature annotationFeature = document.getFeatureVector().get(
                TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Map<String, Integer> firstOccurrences = new HashMap<String, Integer>();
        Map<String, Integer> lastOccurrences = new HashMap<String, Integer>();
        int lastPosition = 0;
        for (Annotation annotation : annotations) {
            String value = annotation.getValue();
            int tokenPosition = annotation.getStartPosition();
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
        for (Annotation annotation : annotations) {
            String value = annotation.getValue();
            double first = (double)firstOccurrences.get(value) / lastPosition;
            double last = (double)lastOccurrences.get(value) / lastPosition;
            double spread = first - last;
            double charLength = value.length();
            double wordLength = value.split(" ").length;
            FeatureVector featureVector = annotation.getFeatureVector();
            featureVector.add(new NumericFeature(FIRST, first));
            featureVector.add(new NumericFeature(LAST, last));
            featureVector.add(new NumericFeature(SPREAD, spread));
            featureVector.add(new NumericFeature(CHAR_LENGTH, charLength));
            featureVector.add(new NumericFeature(WORD_LENGTH, wordLength));
        }
    }

}
