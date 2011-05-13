package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public abstract class TokenRemover implements PipelineProcessor {

    public TokenRemover() {
        super();
    }

    protected abstract boolean remove(String tokenValue);

    @Override
    public final void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Token> tokens = tokenFeature.getValue();
        
        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        List<Token> resultTokens = new ArrayList<Token>();
        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
            Token token = tokenIterator.next();
            String tokenValue = token.getValue();
            if (!remove(tokenValue)) {
                resultTokens.add(token);
            }
        }
        tokenFeature.setValue(resultTokens);
        
//        for (Iterator<Token> tokenIterator = tokens.iterator(); tokenIterator.hasNext();) {
//            Token token = tokenIterator.next();
//            String tokenValue = token.getValue();
//            if (remove(tokenValue)) {
//                tokenIterator.remove();
//            }
//        }
    }

}