package ws.palladian.extraction.feature;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

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

    public static final String PROVIDED_FEATURE="ws.palladian.preprocessing.tokens.tfidf";

    @Override
    protected void processToken(PositionAnnotation annotation) throws DocumentUnprocessableException {
        NumericFeature tfFeature = annotation.getFeatureVector().getFeature(NumericFeature.class, TokenMetricsCalculator.FREQUENCY);
        if (tfFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + TokenMetricsCalculator.FREQUENCY
                    + "\" is missing.");
        }
        NumericFeature idfFeature = annotation.getFeatureVector().getFeature(NumericFeature.class, IdfAnnotator.PROVIDED_FEATURE);
        if (idfFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR
                    + "\" is missing.");
        }
        double tf = tfFeature.getValue();
        double idf = idfFeature.getValue();
        annotation.getFeatureVector().add(new NumericFeature(PROVIDED_FEATURE, tf * idf));
    }

}
