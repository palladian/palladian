package ws.palladian.preprocessing.featureextraction;

import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;

/**
 * <p>
 * Calculates token frequencies for each {@link PipelineDocument} based on a <i>normalized term frequency</i>, as
 * described in "Information Retrieval", Grossman, Frieder, p. 32. This means, that the frequencies are normalized by
 * the maximum term frequency which appears in the considered document.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FrequencyCalculator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens.frequency";

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature)featureVector.get(Tokenizer.PROVIDED_FEATURE);
        if (annotationFeature == null) {
            throw new RuntimeException();
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        Bag<String> tokenBag = new HashBag<String>();
        for (Annotation annotation : tokenList) {
            tokenBag.add(annotation.getValue());
        }

        // calculate "normalized term frequency", see "Information Retrieval", Grossman/Frieder, p. 32
        int maxCount = 1;
        for (String token : tokenBag.uniqueSet()) {
            maxCount = Math.max(maxCount, tokenBag.getCount(token));
        }

        for (Annotation annotation : tokenList) {
            double frequency = (double)tokenBag.getCount(annotation.getValue()) / maxCount;
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, frequency);
            annotation.getFeatureVector().add(frequencyFeature);
        }

    }

}
