package ws.palladian.processing;

import java.util.List;

import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * The interface for components processed by processing pipelines. Each component needs to implement this interface
 * before it can be processed by a pipeline.
 * </p>
 * <p>
 * Components can handle any type of information processing task and modify the document given to them. In addition the
 * document may be extended by features that may be retrieved using the components feature identifier. See the
 * {@link FeatureVector} class for more information.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @version 3.0
 * @since 0.0.8
 */
public interface PipelineProcessor {

    static final String DEFAULT_INPUT_PORT_IDENTIFIER = "ws.palladian.inputport";
    
    static final String DEFAULT_OUTPUT_PORT_IDENTIFIER = "ws.palladian.outputport";

    void process() throws DocumentUnprocessableException;

    List<Port> getInputPorts();

    List<Port> getOutputPorts();

    Port getOutputPort(String outputPortIdentifier);

    boolean isExecutable();

    Port getInputPort(String name);

    /**
     * <p>
     * Notifies the implementing class, that the observed {@link ProcessingPipeline} finished its work.
     * </p>
     */
    void processingFinished();
}
