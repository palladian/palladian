package ws.palladian.processing;

import java.util.List;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * This interface defines components for use within {@link ProcessingPipeline}s, so called {@link PipelineProcessor}s.
 * Such processors can conduct any type of information processing task and modify {@link PipelineDocument}s given to
 * them. In addition the document may be enriched by {@link Feature}s which are attached to a PipelineDocument's
 * {@link FeatureVector}. Usually, instead of implementing the whole interface, concrete PipelineProcessor
 * implementations should inherit from {@link AbstractPipelineProcessor}, which already provides a skeleton of common
 * functionality.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 0.0.8
 */
public interface PipelineProcessor {

    /**
     * <p>
     * The identifier of the default input port.
     * </p>
     */
    static final String DEFAULT_INPUT_PORT_IDENTIFIER = "ws.palladian.inputport";

    /**
     * <p>
     * The identifier of the default output port.
     * </p>
     */
    static final String DEFAULT_OUTPUT_PORT_IDENTIFIER = "ws.palladian.outputport";

    /**
     * <p>
     * Get a list of all available input {@link Port}s.
     * </p>
     * 
     * @return List with input ports, or an empty list if no input ports exist, never <code>null</code>.
     */
    List<Port> getInputPorts();

    /**
     * <p>
     * Get a specific input {@link Port} by its identifier.
     * </p>
     * 
     * @param portIdentifier The identifier of the input port to retrieve, not <code>null</code>.
     * @return The port with the specified identifier, or <code>null</code> if no such port exists.
     */
    Port getInputPort(String portIdentifier);

    /**
     * <p>
     * Get a list of all available output {@link Port}s.
     * </p>
     * 
     * @return List with output ports, or an empty list if no output ports exist, never <code>null</code>.
     */
    List<Port> getOutputPorts();

    /**
     * <p>
     * Get a specific output {@link Port} by its identifier.
     * </p>
     * 
     * @param portIdentifier The identifier of the output port to retrieve, not <code>null</code>.
     * @return The port with the specified identifier, or <code>null</code> if no such port exists.
     */
    Port getOutputPort(String portIdentifier);

    /**
     * <p>
     * Check, whether this processor can be executed. A {@link PipelineProcessor} is executable, if all defined input
     * ports provide data and all output ports are empty.
     * </p>
     * 
     * @return <code>true</code> if this processor is ready to execute, <code>false</code> otherwise.
     */
    boolean isExecutable();

    /**
     * <p>
     * Execute the specific logic implemented by this processor. This usually means, taking the data provided by the
     * input ports (see {@link #getInputPorts()}), performing some computations and submitting the data to the available
     * output ports (see {@link #getOutputPorts()}).
     * </p>
     * 
     * @throws DocumentUnprocessableException in case the processing fails.
     */
    void process() throws DocumentUnprocessableException;

    /**
     * <p>
     * Notifies the implementing class, that the observed {@link ProcessingPipeline} finished its work.
     * </p>
     */
    void processingFinished();

}
