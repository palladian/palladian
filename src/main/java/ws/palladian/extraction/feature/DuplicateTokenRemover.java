package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.model.features.FeatureVector;

public class DuplicateTokenRemover implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(Tokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new RuntimeException("required feature is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();
        Set<String> tokenValues = new HashSet<String>();
        
        List<Annotation> resultTokens = new ArrayList<Annotation>();
        for (Annotation annotation : annotations) {
            String tokenValue = annotation.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
    }

}
