/**
 * Created on: 17.04.2012 23:59:46
 */
package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Takes all {@link Feature}s and transforms them to the output of this processor. So for example if we have the text
 * "The quick brown fox" which has an annotation on "The" and "fox" the processor concatenates "The" and "fox" to
 * "The fox".
 * </p>
 * 
 * @author Klemens
 * @version 1.0
 * @since 0.1.7
 * 
 */
public final class AnnotationsToViewProcessor<F extends Feature<?>> extends StringDocumentPipelineProcessor {

    private final FeatureDescriptor<F> featureDescriptor;

    /**
     * {@see AbstractPipelineProcessor#AbstractPipelineProcessor()}
     */
    public AnnotationsToViewProcessor(FeatureDescriptor<F> featureDescriptor) {
        super();
        this.featureDescriptor = featureDescriptor;
    }

    @Override
    public void processDocument(TextDocument document) {
        FeatureVector vector = document.getFeatureVector();

        List<F> features = vector.getAll(featureDescriptor.getType());

        StringBuilder ret = new StringBuilder();
        for (F feature : features) {
            ret.append(feature.getValue());
            ret.append(" ");
        }
        document.setContent(ret.toString());
    }
}
