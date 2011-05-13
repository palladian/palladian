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
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException();
        }
        List<Token> tokenList = tokenFeature.getValue();
        Bag<String> tokenBag = new HashBag<String>();
        for (Token token : tokenList) {
            tokenBag.add(token.getValue());
        }
        for (Token token : tokenList) {
            double frequency = (double) tokenBag.getCount(token.getValue()) / tokenBag.size();
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, frequency);
            token.getFeatureVector().add(frequencyFeature);
        }
    }

}
