package ws.palladian.preprocessing;

import java.io.Serializable;

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
 */
public interface PipelineProcessor extends Serializable {

    /**
     * <p>
     * Execute the implemented algorithm on the provided {@link PipelineDocument}.
     * </p>
     * 
     * @param document The document to be processed by this processor.
     */
    void process(PipelineDocument document);

}
