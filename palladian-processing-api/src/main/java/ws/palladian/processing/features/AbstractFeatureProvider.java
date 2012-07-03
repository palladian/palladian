/**
 * Created on: 16.06.2012 16:52:40
 */
package ws.palladian.processing.features;

import java.util.List;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.AbstractPipelineProcessor;
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
public abstract class AbstractFeatureProvider<T, F extends Feature<?>> extends AbstractPipelineProcessor<T> implements
        FeatureProvider<F> {
    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set of this class changes.
     * </p>
     */
    private static final long serialVersionUID = 7816865382026826466L;
    /**
     * <p>
     * The {@link FeatureDescriptor} used to identify the provided {@code Feature}.
     * </p>
     */
    private final FeatureDescriptor<F> featureDescriptor;

    /**
     * <p>
     * Creates a new {@code FeatureProvider} providing a feature with the specified {@code featureDescriptor}.
     * </p>
     * 
     * @param featureDescriptor The {@link FeatureDescriptor} used to identify the provided {@code Feature}.
     */
    public AbstractFeatureProvider(final FeatureDescriptor<F> featureDescriptor) {
        super();

        Validate.notNull(featureDescriptor, "featureDescriptor must not be null");

        this.featureDescriptor = featureDescriptor;
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
    public AbstractFeatureProvider(final List<Port<?>> inputPorts, final List<Port<?>> outputPorts,
            final FeatureDescriptor<F> featureDescriptor) {
        super(inputPorts, outputPorts);

        Validate.notNull(featureDescriptor, "featureDescriptor must not be null");

        this.featureDescriptor = featureDescriptor;
    }

    @Override
    public FeatureDescriptor<F> getDescriptor() {
        return featureDescriptor;
    }

}
