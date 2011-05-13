package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class NGramCreator implements PipelineProcessor {
    
    private final int minLength;
    private final int maxLength;

    public NGramCreator() {
        this(2, 2);
    }
    
    public NGramCreator(int maxLength) {
        this(2, maxLength);
    }
    
    public NGramCreator(int minLength, int maxLength) {
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException();
        }
        List<Token> tokens = tokenFeature.getValue();
        List<Token> gramTokens = new ArrayList<Token>();
        for (int i = minLength; i <= maxLength; i++) {
            List<Token> nGramTokens = createNGrams(document, tokens, i);            
            gramTokens.addAll(nGramTokens);
        }
        tokens.addAll(gramTokens);
    }

    private List<Token> createNGrams(PipelineDocument document, List<Token> tokens, int length) {
        List<Token> gramTokens = new ArrayList<Token>();
        Token[] tokensArray = tokens.toArray(new Token[tokens.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            Token gramToken = new Token(document);
            gramToken.setStartPosition(tokensArray[i].getStartPosition());
            gramToken.setEndPosition(tokensArray[i + length - 1].getEndPosition());
            gramTokens.add(gramToken);
        }
        return gramTokens;
    }

}
