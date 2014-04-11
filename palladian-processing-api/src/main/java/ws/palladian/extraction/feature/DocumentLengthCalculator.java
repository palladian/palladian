/**
 * Created on: 16.06.2012 19:16:09
 */
package ws.palladian.extraction.feature;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureProvider;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Calculates the length of a {@link PipelineDocument}s content and provides this length as a {@link NumericFeature} of
 * the document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class DocumentLengthCalculator extends TextDocumentPipelineProcessor implements FeatureProvider {

    /**
     * <p>
     * The identifier for the {@link Feature} extracted by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.documentlength";

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        int length = document.getContent().length();
        NumericFeature feature = new NumericFeature(PROVIDED_FEATURE, length);
        document.getFeatureVector().add(feature);
    }

    @Override
    public String getCreatedFeatureName() {
        return PROVIDED_FEATURE;
    }

}
