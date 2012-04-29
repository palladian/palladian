package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

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
public class TfIdfAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    public static final FeatureDescriptor<NumericFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder.build(
            "ws.palladian.preprocessing.tokens.tfidf", NumericFeature.class);

    @Override
    public void process(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \"" + TokenizerInterface.PROVIDED_FEATURE
                    + "\" is missing.");
        }
        List<Annotation> annotationList = annotationFeature.getValue();
        for (Annotation annotation : annotationList) {
            FeatureVector tokenFeatureVector = annotation.getFeatureVector();
            NumericFeature tfFeature = tokenFeatureVector.get(TokenMetricsCalculator.FREQUENCY);
            if (tfFeature == null) {
                throw new DocumentUnprocessableException("The required feature \"" + TokenMetricsCalculator.FREQUENCY
                        + "\" is missing.");
            }
            NumericFeature idfFeature = tokenFeatureVector.get(IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR);
            if (idfFeature == null) {
                throw new DocumentUnprocessableException("The required feature \"" + IdfAnnotator.PROVIDED_FEATURE_DESCRIPTOR
                        + "\" is missing.");
            }
            double tf = tfFeature.getValue();
            double idf = idfFeature.getValue();
            tokenFeatureVector.add(new NumericFeature(PROVIDED_FEATURE_DESCRIPTOR, tf * idf));
        }
    }

}
