/**
 * Created on: 22.11.2012 22:43:10
 */
package ws.palladian.extraction.feature;

import java.util.Collection;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.InputPort;
import ws.palladian.processing.OutputPort;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureProvider;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureCounter extends AbstractPipelineProcessor implements FeatureProvider {

    private final String featureIdentifierToCount;
    private final String providedFeatureIdentifier;

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public FeatureCounter(String providedFeatureIdentifier, String featureIdentifierToCount) {
        super();

        this.providedFeatureIdentifier = providedFeatureIdentifier;
        this.featureIdentifierToCount = featureIdentifierToCount;
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param inputPorts
     * @param outputPorts
     */
    public FeatureCounter(String providedFeatureIdentifier, String featureIdentifierToCount, InputPort[] inputPorts,
            OutputPort[] outputPorts) {
        super(inputPorts, outputPorts);

        this.providedFeatureIdentifier = providedFeatureIdentifier;
        this.featureIdentifierToCount = featureIdentifierToCount;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> inputDocument = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
        Collection<Feature<?>> features = inputDocument.getFeatureVector().getAll(featureIdentifierToCount);
        inputDocument.getFeatureVector().add(new NumericFeature(providedFeatureIdentifier, features.size()));
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(inputDocument);
    }

    @Override
    public String getCreatedFeatureName() {
        return providedFeatureIdentifier;
    }

}
