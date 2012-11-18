/**
 * Created on: 18.06.2011 15:32:57
 */
package ws.palladian.processing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Abstract base class for pipeline processors. Handles the mapping between input and output views.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.8
 * @version 2.0
 */
public abstract class AbstractPipelineProcessor<T> implements PipelineProcessor {

    /**
     * <p>
     * The input {@link Port}s this processor reads {@link PipelineDocument}s from.
     * </p>
     */
    private List<Port> inputPorts;
    /**
     * <p>
     * The output {@link Port}s this processor writes results to.
     * </p>
     */
    private List<Port> outputPorts;

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working with a default input and output
     * {@code Port}. The input {@code Port} is identified by {@link PipelineProcessor#DEFAULT_INPUT_PORT_IDENTIFIER}
     * while the output {@code Port} is identified by {@link PipelineProcessor#DEFAULT_OUTPUT_PORT_IDENTIFIER}.
     * </p>
     */
    public AbstractPipelineProcessor() {
        super();

        inputPorts = new ArrayList<Port>();
        outputPorts = new ArrayList<Port>();

        inputPorts.add(new Port(DEFAULT_INPUT_PORT_IDENTIFIER));
        outputPorts.add(new Port(DEFAULT_OUTPUT_PORT_IDENTIFIER));
    }

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor}
     * </p>
     * 
     * @param inputPorts The input {@link Port}s this processor reads {@link PipelineDocument}s from.
     * @param outputPorts The output {@link Port}s this processor writes results to.
     */
    public AbstractPipelineProcessor(List<Port> inputPorts, List<Port> outputPorts) {
        super();

        this.inputPorts = new ArrayList<Port>(inputPorts);
        this.outputPorts = new ArrayList<Port>(outputPorts);
    }

    /**
     * <p>
     * Checks whether all input views were provided with a {@code PipelineDocument} and throws an
     * {@code DocumentUnprocessableException} if not.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             In case the document does not provide the required input
     *             view.
     */
    private void allInputPortsAvailable() throws DocumentUnprocessableException {
        for (Port inputPort : getInputPorts()) {
            if (inputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Input port: " + inputPort
                        + " does not provide required input.");
            }
        }
    }

    /**
     * <p>
     * Checks whether all output views were created in a {@code PipelineDocument} and throws an
     * {@code DocumentUnprocessableException} if not.
     * </p>
     * 
     * @throws DocumentUnprocessableException
     *             In case the document does not provide the required output
     *             view.
     */
    private void allOutputPortsAvailable() throws DocumentUnprocessableException {
        for (Port outputPort : getOutputPorts()) {
            if (outputPort.getPipelineDocument() == null) {
                throw new DocumentUnprocessableException("Output port: " + outputPort + " for class: "
                        + this.getClass() + " does not provide required output.");
            }
        }
    }

    private void cleanInputPorts() {
        for (Port inputPort : getInputPorts()) {
            inputPort.setPipelineDocument(null);
        }
    }

    protected PipelineDocument<T> getDefaultInput() {
        return (PipelineDocument<T>)getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER).getPipelineDocument();
    }

    @Override
    public Port getInputPort(final String name) {
        for (Port inputPort : inputPorts) {
            if (name.equals(inputPort.getName())) {
                return inputPort;
            }
        }
        return null;
    }

    @Override
    public List<Port> getInputPorts() {
        return inputPorts;
    }

    @Override
    public Port getOutputPort(final String name) {
        Validate.notEmpty(name);

        for (Port port : outputPorts) {
            if (name.equals(port.getName())) {
                return port;
            }
        }
        return null;
    }

    @Override
    public List<Port> getOutputPorts() {
        return outputPorts;
    }

    @Override
    public boolean isExecutable() {
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

    protected void setDefaultOutput(PipelineDocument<T> document) {
        ((Port)getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER)).setPipelineDocument(document);
    }

//    @Override
//    public void setInput(final Integer inputPortIndex, final PipelineDocument<?> document) {
//        Validate.notNull(inputPortIndex);
//        Validate.notNull(document);
//        Validate.inclusiveBetween(0, inputPorts.size() - 1, inputPortIndex);
//
//        Port port = inputPorts.get(inputPortIndex);
//        port.setPipelineDocument(document);
//    }

    protected void setOutput(final String outputPortName, final PipelineDocument<?> document) {
        Port port = getOutputPort(outputPortName);
        port.setPipelineDocument(document);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public void setInput(final String inputPortIdentifier, final PipelineDocument<?> document) {
        for (Port port : inputPorts) {
            if (port.getName().equals(inputPortIdentifier)) {
                port.setPipelineDocument(document);
            }
        }
    }

    /**
     * @return The default output port identified by {@code DEFAULT_OUTPUT_PORT_IDENTIFIER}.
     */
    public Port getDefaultOutputPort() {
        Port defaultOutputPort = getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER);
        if (defaultOutputPort == null) {
            return null;
        } else {
            return (Port)defaultOutputPort;
        }
    }

    /**
     * @return The default input port identified by {@code DEFAULT_INPUT_PORT_IDENTIFIER}.
     */
    public Port getDefaultInputPort() {
        Port defaultInputPort = getInputPort(DEFAULT_INPUT_PORT_IDENTIFIER);
        if (defaultInputPort == null) {
            return null;
        } else {
            return (Port)defaultInputPort;
        }
    }

    /**
     * <p>
     * Notifies the implementing class, that the observed {@link ProcessingPipeline} finished its work.
     * </p>
     * 
     */
    @Override
    public void processingFinished() {
        // TODO subclasses can insert their own code here.
    }

//    @Override
//    public PipelineDocument<?> getOutput(final String outputPortIdentifier) {
//        return getOutputPort(outputPortIdentifier).getPipelineDocument();
//    }
}
