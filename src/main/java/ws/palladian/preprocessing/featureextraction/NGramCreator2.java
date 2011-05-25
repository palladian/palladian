package ws.palladian.preprocessing.featureextraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class NGramCreator2 implements PipelineProcessor {
    private static final long serialVersionUID = 1L;
    private final int minLength;
    private final int maxLength;

    public NGramCreator2() {
        this(2, 2);
    }

    public NGramCreator2(int maxLength) {
        this(2, maxLength);
    }

    public NGramCreator2(int minLength, int maxLength) {
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
        List<TokenGroup> gramTokens = new ArrayList<TokenGroup>();
        for (int i = minLength; i <= maxLength; i++) {
            List<TokenGroup> nGramTokens = createNGrams(document, tokens, i);
            gramTokens.addAll(nGramTokens);
        }
        tokens.addAll(gramTokens);
    }

    private List<TokenGroup> createNGrams(PipelineDocument document, List<Token> tokens, int length) {
        List<TokenGroup> gramTokens = new ArrayList<TokenGroup>();
        Token[] tokensArray = tokens.toArray(new Token[tokens.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            TokenGroup gramToken = new TokenGroup(document);
            for (int j = i; j < i + length; j++) {
                gramToken.add(tokensArray[j]);
            }
            gramTokens.add(gramToken);
        }
        return gramTokens;
    }

}