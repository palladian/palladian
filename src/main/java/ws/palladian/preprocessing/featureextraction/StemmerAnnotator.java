package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NominalFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

public class StemmerAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    private SnowballStemmer stemmer;
    
    public StemmerAnnotator() {
        this(new englishStemmer());
    }
    
    public StemmerAnnotator(SnowballStemmer stemmer) {
        this.stemmer = stemmer;
    }

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        TokenFeature tokenFeature = (TokenFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (tokenFeature == null) {
            throw new RuntimeException();
        }
        List<Token> tokens = tokenFeature.getValue();
        for (Token token : tokens) {
            stemmer.setCurrent(token.getValue());
            stemmer.stem();
            String stem = stemmer.getCurrent();
            NominalFeature stemFeature = new NominalFeature("stem", stem);
            token.getFeatureVector().add(stemFeature);
        }
    }

}
