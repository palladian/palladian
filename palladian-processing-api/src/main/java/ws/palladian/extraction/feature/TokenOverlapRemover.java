package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

public final class TokenOverlapRemover extends TextDocumentPipelineProcessor {

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        Annotation[] tokensArray = annotations.toArray(new PositionAnnotation[annotations.size()]);
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
