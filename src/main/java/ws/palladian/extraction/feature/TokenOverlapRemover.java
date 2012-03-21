package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.model.features.FeatureVector;

public class TokenOverlapRemover implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("The required feature \"" + Tokenizer.PROVIDED_FEATURE + "\" is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Annotation[] tokensArray = annotations.toArray(new Annotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length; i++) {
            for (int j = i + 1; j < tokensArray.length; j++) {
                Annotation token1 = tokensArray[i];
                Annotation token2 = tokensArray[j];
                boolean token2overlaps = token1.getStartPosition() >= token2.getStartPosition()
                        && token1.getEndPosition() <= token2.getEndPosition();
                if (token2overlaps) {
                    annotations.remove(token1);
                }
            }
        }
    }

}
