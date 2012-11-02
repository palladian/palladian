package ws.palladian.extraction.feature;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A {@link PipelineProcessor} for annotating <a
 * href="http://nlp.stanford.edu/IR-book/html/htmledition/tf-idf-weighting-1.html">TF-IDF</a> values. The
 * {@link PipelineDocument}s need to be processed by {@link TokenMetricsCalculator} to calculate token frequencies and
 * {@link IdfAnnotator} to calculate inverse document frequencies first.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class TfIdfAnnotator extends AbstractTokenProcessor {

    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            "ws.palladian.preprocessing.tokens.tfidf", NumericFeature.class);

    @Override
    protected void processToken(Annotation<String> annotation) throws DocumentUnprocessableException {
        NumericFeature tfFeature = annotation.getFeature(TokenMetricsCalculator.FREQUENCY);
        if (tfFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + TokenMetricsCalculator.FREQUENCY
                    + "\" is missing.");
        }
        NumericFeature idfFeature = annotation.getFeature(IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR);
        if (idfFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR
                    + "\" is missing.");
        }
        double tf = tfFeature.getValue();
        double idf = idfFeature.getValue();
        annotation.addFeature(new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, tf * idf));
    }

}
