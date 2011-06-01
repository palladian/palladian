package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class FrequencyCalculator implements PipelineProcessor {
    
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens.frequency";

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        Bag<String> tokenBag = new HashBag<String>();
        for (Annotation annotation : tokenList) {
            tokenBag.add(annotation.getValue());
        }
//        for (Annotation token : tokenList) {
//            double frequency = (double) tokenBag.getCount(token.getValue()) / tokenBag.size();
//            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, frequency);
//            token.getFeatureVector().add(frequencyFeature);
//        }
        
        // calculate "normalized term frequency", see "Information Retrieval", Grossman/Frieder, p. 32
        int maxCount = 1;
        for (String token : tokenBag.uniqueSet()) {
            maxCount = Math.max(maxCount, tokenBag.getCount(token));
        }
        
        for (Annotation annotation : tokenList) {
            double frequency = (double) tokenBag.getCount(annotation.getValue()) / maxCount;
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, frequency);
            annotation.getFeatureVector().add(frequencyFeature);
        }
        
    }

}
