package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} which removes all duplicate tokens. The {@link PipelineDocument}s processed by this
 * PipelineProcessor must be tokenized in advance using an Implementation of {@link BaseTokenizer} providing a
 * {@link BaseTokenizer#PROVIDED_FEATURE_DESCRIPTOR}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DuplicateTokenRemover extends StringDocumentPipelineProcessor {

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
//        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
//                BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("The required feature \""
//                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + "\" is missing.");
//        }
        Set<String> tokenValues = new HashSet<String>();
        FeatureVector featureVector = document.getFeatureVector();
        List<PositionAnnotation> inputTokens = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
        List<PositionAnnotation> resultTokens = CollectionHelper.newArrayList();
        for (PositionAnnotation annotation : inputTokens) {
            String tokenValue = annotation.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(annotation);
            }
        }
        featureVector.removeAll(BaseTokenizer.PROVIDED_FEATURE);
        featureVector.addAll(resultTokens);
//        annotationFeature.setValue(resultTokens);
    }

}
