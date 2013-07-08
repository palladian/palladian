/**
 * Created on: 22.11.2012 22:43:10
 */
package ws.palladian.extraction.feature;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureProvider;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Counts how often a {@link Feature} is associated with a {@link FeatureVector}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureCounter extends AbstractPipelineProcessor implements FeatureProvider {

    /**
     * <p>
     * The name of the {@link Feature} to count.
     * </p>
     */
    private final String featureNameToCount;
    /**
     * <p>
     * The name of the {@link Feature} this {@link PipelineProcessor} stores its result at.
     * </p>
     */
    private final String providedFeatureName;

    /**
     * <p>
     * Creates a new completely initalized {@link FeatureCounter}.
     * </p>
     * 
     * @param providedFeatureName The name of the {@link Feature} this {@link PipelineProcessor} stores its result at.
     * @param featureNameToCount The name of the {@link Feature} to count.
     */
    public FeatureCounter(String providedFeatureName, String featureNameToCount) {
        super();

        this.providedFeatureName = providedFeatureName;
        this.featureNameToCount = featureNameToCount;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> inputDocument = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
        Feature<?> feature = inputDocument.get(featureNameToCount);
        Integer count = 0;
        if(feature instanceof ListFeature) {
            count = ((ListFeature)feature).size();
        } else {
            count = 1;
        }
        inputDocument.getFeatureVector().add(new NumericFeature(providedFeatureName, count));
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(inputDocument);
    }

    @Override
    public String getCreatedFeatureName() {
        return providedFeatureName;
    }

}
