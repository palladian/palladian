package ws.palladian.extraction.token;

import java.util.regex.Matcher;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.PositionAnnotation;

/**
 * <p>
 * A {@link BaseTokenizer} implementation based on regular expressions. Tokens are matched against the specified
 * regular expressions.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class RegExTokenizer extends BaseTokenizer {

    private static final long serialVersionUID = 1L;

    @Override
    protected void processDocument(PipelineDocument document) {
        String text = document.getOriginalContent();
        Matcher matcher = Tokenizer.SPLIT_PATTERN.matcher(text);
        AnnotationFeature annotationFeature = new AnnotationFeature(PROVIDED_FEATURE_DESCRIPTOR);
        int index = 0;
        while (matcher.find()) {
            int startPosition = matcher.start();
            int endPosition = matcher.end();
            Annotation annotation = new PositionAnnotation(document, startPosition, endPosition, index++);
            annotationFeature.add(annotation);
        }
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(annotationFeature);
    }

}
