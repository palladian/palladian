package ws.palladian.preprocessing.featureextraction;

import java.util.Arrays;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.ProcessingPipeline;

public class TokenOverlapRemover implements PipelineProcessor {

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Token> tokens = tokenFeature.getValue();
        Token[] tokensArray = tokens.toArray(new Token[tokens.size()]);
        for (int i = 0; i < tokensArray.length; i++) {
            for (int j = i + 1; j < tokensArray.length; j++) {
                Token token1 = tokensArray[i];
                Token token2 = tokensArray[j];
                boolean token2overlaps = 
                    token1.getStartPosition() >= token2.getStartPosition() &&
                    token1.getEndPosition() <= token2.getEndPosition();
                if (token2overlaps) {
//                    System.out.println("remove " + token1);
                    tokens.remove(token1);
                }
            }
        }
    }
    
    
    public static void main(String[] args) {
        PipelineDocument d = new PipelineDocument("the quick brown fox");
        
        ProcessingPipeline processingPipeline = new ProcessingPipeline();
        processingPipeline.add(new Tokenizer());
        processingPipeline.add(new NGramCreator(5));
        processingPipeline.add(new ControlledVocabularyFilter(Arrays.asList("quick", "brown", "brown fox")));
        processingPipeline.add(new TokenOverlapRemover());
        
        processingPipeline.process(d);
        TokenFeature tokenFeature = (TokenFeature) d.getFeatureVector().get(Tokenizer.PROVIDED_FEATURE);
        List<Token> tokens = tokenFeature.getValue();
        
        CollectionHelper.print(tokens);
    }

}
