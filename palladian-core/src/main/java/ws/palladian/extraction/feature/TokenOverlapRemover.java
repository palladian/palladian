package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotated;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

public final class TokenOverlapRemover extends TextDocumentPipelineProcessor {

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
//        TextAnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("The required feature \"" + BaseTokenizer.PROVIDED_FEATURE + "\" is missing");
//        }
        List<PositionAnnotation> annotations = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        Annotated[] tokensArray = annotations.toArray(new PositionAnnotation[annotations.size()]);
        for (int i = 0; i < tokensArray.length; i++) {
            for (int j = i + 1; j < tokensArray.length; j++) {
                Annotated token1 = tokensArray[i];
                Annotated token2 = tokensArray[j];
                boolean token2overlaps = token1.getStartPosition() >= token2.getStartPosition()
                        && token1.getEndPosition() <= token2.getEndPosition();
                if (token2overlaps) {
                    annotations.remove(token1);
                }
            }
        }
    }

}
