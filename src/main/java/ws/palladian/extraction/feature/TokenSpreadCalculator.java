package ws.palladian.extraction.feature;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;


public class TokenSpreadCalculator implements PipelineProcessor {
    
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens.spread";
    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(PROVIDED_FEATURE, NumericFeature.class);

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
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
            double spread = (double) (lastOccurences.get(value) - firstOccurences.get(value)) / lastPosition;
            NumericFeature spreadFeature = new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, spread);
            annotation.getFeatureVector().add(spreadFeature);
        }
    }


}
