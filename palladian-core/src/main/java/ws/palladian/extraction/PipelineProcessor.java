package ws.palladian.extraction;

import java.io.Serializable;
import java.util.List;

import ws.palladian.model.features.FeatureVector;

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
 * @version 3.0
 * @since 0.0.8
 */
public interface PipelineProcessor<T> extends Serializable {

    // /**
    // * <p>
    // * Execute the implemented algorithm on the provided {@link PipelineDocument}.
    // * </p>
    // *
    // * @param document
    // * The document to be processed by this processor.
    // * @throws DocumentUnprocessableException
    // * If the {@code document} could not be processed by this {@code PipelineProcessor}.
    // */
    // void process(PipelineDocument<T> document) throws DocumentUnprocessableException;

    void process() throws DocumentUnprocessableException;

    List<Port<?>> getInputPorts();

    List<Port<?>> getOutputPorts();

    Boolean isExecutable();
}
