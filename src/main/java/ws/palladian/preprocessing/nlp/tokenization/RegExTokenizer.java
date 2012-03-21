package ws.palladian.preprocessing.nlp.tokenization;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.PositionAnnotation;

/**
 * <p>
 * A {@link Tokenizer} implementation based on regular expressions. Tokens are matched against the specified regular
 * expressions.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class RegExTokenizer implements Tokenizer {

    private static final long serialVersionUID = 1L;

    private static final Pattern TOKENIZE_REGEXP = Pattern
            .compile(
                    "([A-Z]\\.)+|([\\p{L}\\w]+)([-\\.,]([\\p{L}\\w]+))*|\\.([\\p{L}\\w]+)|</?([\\p{L}\\w]+)>|(\\$\\d+\\.\\d+)|([^\\w\\s<]+)",
                    Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    @Override
    public void process(PipelineDocument document) {
        String text = document.getOriginalContent();
        Matcher matcher = TOKENIZE_REGEXP.matcher(text);
        AnnotationFeature annotationFeature = new AnnotationFeature(PROVIDED_FEATURE);
        while (matcher.find()) {
            int startPosition = matcher.start();
            int endPosition = matcher.end();
            Annotation annotation = new PositionAnnotation(document, startPosition, endPosition);
            annotationFeature.add(annotation);
        }
        FeatureVector featureVector = document.getFeatureVector();
        featureVector.add(annotationFeature);
    }

}
