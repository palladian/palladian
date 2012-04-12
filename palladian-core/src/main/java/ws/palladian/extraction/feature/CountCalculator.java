package ws.palladian.extraction.feature;

import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates token counts.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CountCalculator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens.count";

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(TokenizerInterface.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        Bag<String> tokenBag = new HashBag<String>();
        for (Annotation annotation : tokenList) {
            tokenBag.add(annotation.getValue());
        }

        for (Annotation annotation : tokenList) {
            String value = annotation.getValue();
            double count = tokenBag.getCount(value);
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, count);
            annotation.getFeatureVector().add(frequencyFeature);
        }

    }
}
