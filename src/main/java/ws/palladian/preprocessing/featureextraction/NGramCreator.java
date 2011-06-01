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
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> annotations = annotationFeature.getValue();
        List<Annotation> gramTokens = new ArrayList<Annotation>();
        for (int i = minLength; i <= maxLength; i++) {
            List<Annotation> nGramTokens = createNGrams(document, annotations, i);            
            gramTokens.addAll(nGramTokens);
        }
        annotations.addAll(gramTokens);
    }

    private List<Annotation> createNGrams(PipelineDocument document, List<Annotation> annotations, int length) {
        List<Annotation> gramTokens = new ArrayList<Annotation>();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length - length + 1; i++) {
            Annotation gramToken = new Annotation(document,tokensArray[i].getStartPosition(),tokensArray[i + length - 1].getEndPosition());
            gramTokens.add(gramToken);
        }
        return gramTokens;
    }

}
