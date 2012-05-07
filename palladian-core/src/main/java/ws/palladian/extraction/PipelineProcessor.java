package ws.palladian.extraction;

import java.io.Serializable;

import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * The interface for components processed by processing pipelines. Each
 * component needs to implement this interface before it can be processed by a
 * pipeline.
 * </p>
 * <p>
 * Components can handle any type of information processing task and modify the
 * document given to them. In addition the document may be extended by features
 * that may be retrieved using the components feature identifier. See the
 * {@link FeatureVector} class for more information.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.0.8
 */
public interface PipelineProcessor<T> extends Serializable {

	/**
	 * <p>
	 * The name of the default original content view.
	 * </p>
	 */
	String ORIGINAL_CONTENT_VIEW_NAME = "originalContent";
	/**
	 * <p>
	 * The name of the default modified content view.
	 * </p>
	 */
	String MODIFIED_CONTENT_VIEW_NAME = "modifiedContent";

	/**
	 * <p>
	 * Execute the implemented algorithm on the provided
	 * {@link PipelineDocument}.
	 * </p>
	 * 
	 * @param document
	 *            The document to be processed by this processor.
	 * @throws DocumentUnprocessableException
	 *             If the {@code document} could not be processed by this
	 *             {@code PipelineProcessor}.
	 */
	void process(PipelineDocument<T> document)
			throws DocumentUnprocessableException;

}
