package ws.palladian.extraction.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

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
public final class TokenMetricsCalculator extends TextDocumentPipelineProcessor {

    public static final String FIRST =  "ws.palladian.features.tokens.first";
    public static final String LAST = "ws.palladian.features.tokens.last";
    public static final String COUNT = "ws.palladian.features.tokens.count";
    public static final String FREQUENCY = "ws.palladian.features.tokens.frequency";
    public static final String SPREAD = "ws.palladian.features.tokens.spread";
    public static final String CHAR_LENGTH = "ws.palladian.features.tokens.length.char";
    public static final String WORD_LENGTH = "ws.palladian.features.tokens.length.word";

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        CountMap<String> occurrences = CountMap.create();
        Map<String, Integer> firstOccurrences = new HashMap<String, Integer>();
        Map<String, Integer> lastOccurrences = new HashMap<String, Integer>();
        int lastPosition = 0;
        int tokenPosition = 0;
        for (PositionAnnotation annotation : annotations) {
            // changed to lower case, 2012-05-01
            String value = annotation.getValue().toLowerCase();
            occurrences.add(value);
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
            tokenPosition++;
        }

        // calculate "normalized term frequency", see "Information Retrieval", Grossman/Frieder, p. 32
        int maxCount = 1;
        for (String token : occurrences.uniqueItems()) {
            maxCount = Math.max(maxCount, occurrences.getCount(token));
        }

        for (PositionAnnotation annotation : annotations) {
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
