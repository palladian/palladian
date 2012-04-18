package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;

public class DuplicateTokenRemover implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    public void process(PipelineDocument document) {
        AnnotationFeature annotationFeature = document.getFeatureVector().get(
                TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("The required feature \"" + TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR
                    + "\" is missing.");
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
