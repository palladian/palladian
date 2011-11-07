package ws.palladian.preprocessing.featureextraction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

// TODO rename to TokenDistributionCalculator
public class TokenSpreadCalculator implements PipelineProcessor {
    
    private static final long serialVersionUID = 1L;
    public static final String PROVIDED_FEATURE_FIRST = "ws.palladian.features.tokens.first";
    public static final String PROVIDED_FEATURE_LAST = "ws.palladian.features.tokens.last";
    public static final String PROVIDED_FEATURE_SPREAD = "ws.palladian.features.tokens.spread";

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        Map<String, Integer> firstOccurences = new HashMap<String, Integer>();
        Map<String, Integer> lastOccurences = new HashMap<String, Integer>();
        int lastPosition = 0;
        for (Annotation annotation : tokenList) {
            String value = annotation.getValue();
            int tokenPosition = annotation.getStartPosition();
            Integer firstOccurence = firstOccurences.get(value);
            if (firstOccurence == null) {
                firstOccurences.put(value, tokenPosition);
            } else {
                firstOccurences.put(value, Math.min(tokenPosition, firstOccurence));                
            }
            Integer lastOccurence = lastOccurences.get(value);
            if (lastOccurence == null) {
                lastOccurences.put(value, tokenPosition);
            } else {
                lastOccurences.put(value, Math.max(tokenPosition, lastOccurence));
            }
            lastPosition = Math.max(tokenPosition, lastPosition);
        }
        for (Annotation annotation : tokenList) {
            String value = annotation.getValue();
            double first = (double) firstOccurences.get(value) / lastPosition;
            double last = (double) lastOccurences.get(value) / lastPosition;
            double spread = first - last;
            annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE_FIRST, first));
            annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE_LAST, last));
            annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE_SPREAD, spread));
        }
    }


}
