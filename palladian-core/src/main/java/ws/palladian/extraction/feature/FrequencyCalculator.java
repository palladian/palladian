package ws.palladian.extraction.feature;

import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates token frequencies for each {@link PipelineDocument} based on a <i>normalized term frequency</i>, as
 * described in "Information Retrieval", Grossman, Frieder, p. 32. This means, that the frequencies are normalized by
 * the maximum term frequency which appears in the considered document.
 * </p>
 * 
 * @author Philipp Katz
 * @deprecated Integrated into {@link TokenMetricsCalculator}.
 */
@Deprecated
public class FrequencyCalculator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens.frequency";

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            PROVIDED_FEATURE, NumericFeature.class);

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("The required feature " + TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR
                    + " is missing.");
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
            double frequency = (double) tokenBag.getCount(annotation.getValue()) / maxCount;
            NumericFeature frequencyFeature = new NumericFeature(PROVIDED_FEATURE, frequency);
            annotation.getFeatureVector().add(frequencyFeature);
        }

    }

}
