/**
 * Created on: 18.06.2011 15:32:57
 */
package ws.palladian.processing;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Abstract base class for {@link PipelineProcessor} implementations.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 0.0.8
 * @version 2.0
 */
public abstract class AbstractPipelineProcessor implements PipelineProcessor {

    /** The input {@link Port}s this processor reads {@link PipelineDocument}s from. */
    private final List<InputPort> inputPorts;

    /** The output {@link Port}s this processor writes results to. */
    private final List<OutputPort> outputPorts;

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working with a default input and output
     * {@code Port}. The input {@code Port} is identified by {@link PipelineProcessor#DEFAULT_INPUT_PORT_IDENTIFIER}
     * while the output {@code Port} is identified by {@link PipelineProcessor#DEFAULT_OUTPUT_PORT_IDENTIFIER}.
     * </p>
     */
    public AbstractPipelineProcessor() {
        inputPorts = Collections.singletonList(new InputPort(DEFAULT_INPUT_PORT_IDENTIFIER));
        outputPorts = Collections.singletonList(new OutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER));
    }

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor}
     * </p>
     * 
     * @param inputPorts The input {@link Port}s this processor reads {@link PipelineDocument}s from. Empty array if
     *            this processor has no inputs, not <code>null</code>.
     * @param outputPorts The output {@link Port}s this processor writes results to. Empty array if this processor has
     *            no outputs, not <code>null</code>.
     */
    public AbstractPipelineProcessor(InputPort[] inputPorts, OutputPort[] outputPorts) {
        Validate.notNull(inputPorts, "inputPorts must not be null");
        Validate.notNull(outputPorts, "outputPorts must not be null");

        this.inputPorts = Arrays.asList(inputPorts);
        this.outputPorts = Arrays.asList(outputPorts);
    }

    /**
     * <p>
     * Checks whether all input ports were provided with a {@link PipelineDocument}.
     * </p>
     * 
     * @throws DocumentUnprocessableException In case the document does not provide the required input port.
     */
    private final void allInputPortsAvailable() throws DocumentUnprocessableException {
        for (InputPort inputPort : getInputPorts()) {
            if (!inputPort.hasDocument()) {
                throw new DocumentUnprocessableException("Input port " + inputPort + " at " + toString()
                        + " does not provide required input.");
            }
        }
    }

    /**
     * <p>
     * Checks whether all output ports were supplied with a {@link PipelineDocument}.
     * </p>
     * 
     * @throws DocumentUnprocessableException In case the document does not provide the required output port.
     */
    private final void allOutputPortsAvailable() throws DocumentUnprocessableException {
        for (OutputPort outputPort : getOutputPorts()) {
            if (!outputPort.hasDocument()) {
                throw new DocumentUnprocessableException("Output port " + outputPort + " at " + toString()
                        + " does not provide required output.");
            }
        }
    }

    private final void cleanInputPorts() {
        for (InputPort inputPort : getInputPorts()) {
            inputPort.poll();
        }
    }

    @Override
    public final InputPort getInputPort(String portIdentifier) {
        Validate.notEmpty(portIdentifier, "portIdentifier must not be empty");
        for (InputPort inputPort : inputPorts) {
            if (portIdentifier.equals(inputPort.getIdentifier())) {
                return inputPort;
            }
        }
        return null;
    }

    @Override
    public final List<InputPort> getInputPorts() {
        return inputPorts;
    }

    @Override
    public final OutputPort getOutputPort(String portIdentifier) {
        Validate.notEmpty(portIdentifier, "portIdentifier must not be empty");
        for (OutputPort port : outputPorts) {
            if (portIdentifier.equals(port.getIdentifier())) {
                return port;
            }
        }
        return null;
    }

    @Override
    public final List<OutputPort> getOutputPorts() {
        return outputPorts;
    }

    @Override
    public boolean isExecutable() {
        // There must be a document at each input port.
        for (Port inputPort : getInputPorts()) {
            if (!inputPort.hasDocument()) {
                return false;
            }
        }

        // Each output port needs to be empty and ready to receive data.
        for (Port outputPort : getOutputPorts()) {
            if (outputPort.hasDocument()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final void process() throws DocumentUnprocessableException {
        allInputPortsAvailable();
        processDocument();
        allOutputPortsAvailable();
        cleanInputPorts();
        fireOutputPorts();
    }

    private void fireOutputPorts() {
        for (OutputPort outputPort : outputPorts) {
            if (outputPort.hasDocument()) {
                outputPort.fire();
            }
        }
    }

    /**
     * <p>
     * Apply the algorithm implemented by this {@code PipelineProcessor} to a {@code PipelineDocument}. This is the
     * central method of each {@code PipelineProcessor} providing the core functionality.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             If the {@code document} could not be processed by this {@code PipelineProcessor}.
     */
    protected abstract void processDocument() throws DocumentUnprocessableException;

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public void processingFinished() {
    }

    /**
     * <p>
     * Provides the {@link PipelineDocument} from the default input port if one is available or {@code null} if not.
     * Also resets the input port.
     * </p>
     * 
     * @return The {@link PipelineDocument} from the default input port or {@code null}.
     */
    protected final PipelineDocument<?> getInput() {
        return getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).poll();
    }

    /**
     * <p>
     * Sets the provided {@link PipelineDocument} as the default output of this {@link PipelineProcessor}.
     * </p>
     * 
     * @param document The {@link PipelineDocument}, which is the output of this {@link PipelineProcessor}.
     */
    protected final void setOutput(PipelineDocument<?> document) {
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(document);
    }

}
