package ws.palladian.extraction.token;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.PositionAnnotation;

/**
 * <p>
 * A {@link TokenizerInterface} implementation based on regular expressions. Tokens are matched against the specified
 * regular expressions.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class RegExTokenizer extends AbstractPipelineProcessor implements TokenizerInterface {

    private static final long serialVersionUID = 1L;

    private static final Pattern TOKENIZE_REGEXP = Pattern
            .compile(
                    "([A-Z]\\.)+|([\\p{L}\\w]+)([-\\.,]([\\p{L}\\w]+))*|\\.([\\p{L}\\w]+)|</?([\\p{L}\\w]+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    protected void processDocument(PipelineDocument document) {
        String text = document.getOriginalContent();
        Matcher matcher = TOKENIZE_REGEXP.matcher(text);
        AnnotationFeature annotationFeature = new AnnotationFeature(PROVIDED_FEATURE);
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
