package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class DuplicateTokenRemover implements PipelineProcessor {

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Token> tokens = tokenFeature.getValue();
        Set<String> tokenValues = new HashSet<String>();
        
        List<Token> resultTokens = new ArrayList<Token>();
        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
            Token token = tokenIterator.next();
            String tokenValue = token.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(token);
            }
        }
        tokenFeature.setValue(resultTokens);
        
//        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
//            Token token = tokenIterator.next();
//            String tokenValue = token.getValue().toLowerCase();
//            if (!tokenValues.add(tokenValue)) {
//                tokenIterator.remove();
//            }
//        }
    }

}
