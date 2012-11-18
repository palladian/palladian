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
 * Abstract base class for pipeline processors. Handles the mapping between input and output ports.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @since 0.0.8
 * @version 2.0
 */
public abstract class AbstractPipelineProcessor implements PipelineProcessor {

    /** The input {@link Port}s this processor reads {@link PipelineDocument}s from. */
    private final List<Port> inputPorts;
    
    /** The output {@link Port}s this processor writes results to. */
    private final List<Port> outputPorts;

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working with a default input and output
     * {@code Port}. The input {@code Port} is identified by {@link PipelineProcessor#DEFAULT_INPUT_PORT_IDENTIFIER}
     * while the output {@code Port} is identified by {@link PipelineProcessor#DEFAULT_OUTPUT_PORT_IDENTIFIER}.
     * </p>
     */
    public AbstractPipelineProcessor() {
        inputPorts = Collections.singletonList(new Port(DEFAULT_INPUT_PORT_IDENTIFIER));
        outputPorts = Collections.singletonList(new Port(DEFAULT_OUTPUT_PORT_IDENTIFIER));
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
    public AbstractPipelineProcessor(Port[] inputPorts, Port[] outputPorts) {
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
        for (Port inputPort : getInputPorts()) {
            if (inputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Input port: " + inputPort
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
        for (Port outputPort : getOutputPorts()) {
            if (outputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Output port: " + outputPort + " for class: "
                        + this.getClass() + " does not provide required output.");
            }
        }
    }

    private final void cleanInputPorts() {
        for (Port inputPort : getInputPorts()) {
            inputPort.setPipelineDocument(null);
        }
    }

    @Override
    public final Port getInputPort(String name) {
        for (Port inputPort : inputPorts) {
            if (name.equals(inputPort.getName())) {
                return inputPort;
            }
        }
        return null;
    }

    @Override
    public final List<Port> getInputPorts() {
        return inputPorts;
    }

    @Override
    public final Port getOutputPort(String name) {
        Validate.notEmpty(name);

        for (Port port : outputPorts) {
            if (name.equals(port.getName())) {
                return port;
            }
        }
        return null;
    }

    @Override
    public final List<Port> getOutputPorts() {
        return outputPorts;
    }

    @Override
    public final boolean isExecutable() {
        // There must be a document at each input port.
        for (Port inputPort : getInputPorts()) {
            if (inputPort.getPipelineDocument() == null) {
                return false;
            }
        }

        // Each output port needs to be empty and ready to receive data.
        for (Port outputPort : getOutputPorts()) {
            if (outputPort.getPipelineDocument() != null) {
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

    protected final void setDefaultOutput(PipelineDocument<?> document) {
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).setPipelineDocument(document);
    }

    protected final void setOutput(String outputPortName, PipelineDocument<?> document) {
        getOutputPort(outputPortName).setPipelineDocument(document);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    @Override
    public final void setInput(String inputPortIdentifier, PipelineDocument<?> document) {
        for (Port port : inputPorts) {
            if (port.getName().equals(inputPortIdentifier)) {
                port.setPipelineDocument(document);
            }
        }
    }

    /**
     * @return The default output port identified by {@code DEFAULT_OUTPUT_PORT_IDENTIFIER}.
     */
    protected final Port getDefaultOutputPort() {
        return getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER);
    }

    /**
     * @return The default input port identified by {@code DEFAULT_INPUT_PORT_IDENTIFIER}.
     */
    protected final Port getDefaultInputPort() {
        return getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER);
    }

    /**
     * <p>
     * Notifies the implementing class, that the observed {@link ProcessingPipeline} finished its work. May be
     * overridden as necessary.
     * </p>
     * 
     */
    @Override
    public void processingFinished() {
    }

}
