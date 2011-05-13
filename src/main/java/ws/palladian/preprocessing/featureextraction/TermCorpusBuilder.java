package ws.palladian.preprocessing.featureextraction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class TermCorpusBuilder implements PipelineProcessor {
    
    private final TermCorpus termCorpus;
    
    public TermCorpusBuilder() {
        this(new TroveTermCorpus());
    }
    
    public TermCorpusBuilder(TermCorpus termCorpus) {
        this.termCorpus = termCorpus;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Token> tokens = tokenFeature.getValue();
        
        Set<String> tokenValues = new HashSet<String>();

        for (Token token : tokens) {
            String tokenValue = token.getValue().toLowerCase();
            tokenValues.add(tokenValue);
        }

        termCorpus.addTermsFromDocument(tokenValues);
        
    }
    
    public TermCorpus getTermCorpus() {
        return termCorpus;
    }

}
