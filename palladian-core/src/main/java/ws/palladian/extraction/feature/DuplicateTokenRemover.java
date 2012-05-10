package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;

/**
 * <p>
 * A {@link PipelineProcessor} which removes all duplicate tokens. The {@link PipelineDocument}s processed by this
 * PipelineProcessor must be tokenized in advance using an Implementation of {@link BaseTokenizer} providing a
 * {@link BaseTokenizer#PROVIDED_FEATURE_DESCRIPTOR}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DuplicateTokenRemover extends AbstractPipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    protected void processDocument(PipelineDocument document) throws DocumentUnprocessableException {
        AnnotationFeature annotationFeature = document.getFeatureVector().get(
                BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + "\" is missing.");
        }
        Set<String> tokenValues = new HashSet<String>();
        List<Annotation> resultTokens = new ArrayList<Annotation>();
        for (Annotation annotation : annotationFeature.getValue()) {
            String tokenValue = annotation.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
    }

}
