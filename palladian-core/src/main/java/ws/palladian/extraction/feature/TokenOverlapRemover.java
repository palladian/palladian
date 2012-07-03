package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;

public final class TokenOverlapRemover extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + BaseTokenizer.PROVIDED_FEATURE + "\" is missing");
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
