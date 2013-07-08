/**
 * Created on: 16.06.2012 16:52:40
 */
package ws.palladian.processing.features;

import java.beans.FeatureDescriptor;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.AbstractPipelineProcessor;
import ws.palladian.processing.InputPort;
import ws.palladian.processing.OutputPort;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.Port;

/**
 * <p>
 * Abstract base class for {@code PipelineProcessors} providing new {@code Feature}s to processed
 * {@link PipelineDocument}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public abstract class AbstractFeatureProvider extends AbstractPipelineProcessor implements FeatureProvider {

    private final String featureIdentifier;

    /**
     * <p>
     * Creates a new {@code FeatureProvider} providing a feature with the specified {@code featureDescriptor}.
     * </p>
     * 
     * @param featureDescriptor The {@link FeatureDescriptor} used to identify the provided {@code Feature}.
     */
    public AbstractFeatureProvider(String featureIdentifier) {
        Validate.notNull(featureIdentifier, "featureIdentifier must not be null");
        this.featureIdentifier = featureIdentifier;
    }

    /**
     * <p>
     * Creates a new {@code FeatureProvider} providing a feature with the specified {@code featureDescriptor} and a set
     * of in- and output {@link Port}s.
     * </p>
     * 
     * @param inputPorts The input {@link Port}s this processor reads {@link PipelineDocument}s from.
     * @param outputPorts The output {@link Port}s this processor writes results to.
     * @param featureDescriptor The {@link FeatureDescriptor} used to identify the provided {@code Feature}.
     */
    public AbstractFeatureProvider(InputPort[] inputPorts, OutputPort[] outputPorts, String featureIdentifier) {
        super(inputPorts, outputPorts);
        Validate.notNull(featureIdentifier, "featureIdentifier must not be null");
        this.featureIdentifier = featureIdentifier;
    }

    @Override
    public String getCreatedFeatureName() {
        return featureIdentifier;
    }

}
