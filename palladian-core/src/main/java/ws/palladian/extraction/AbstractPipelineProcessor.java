/**
 * Created on: 18.06.2011 15:32:57
 */
package ws.palladian.extraction;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * <p>
 * Abstract base class for pipeline processors. Handles the mapping between
 * input and output views.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.0.8
 * @version 2.0
 */
public abstract class AbstractPipelineProcessor<T> implements
		PipelineProcessor<T> {
	/**
	 * <p>
	 * Unique identifier to serialize and deserialize objects of this type to
	 * and from a file.
	 * </p>
	 */
	private static final long serialVersionUID = -7030337967596448903L;

	/**
	 * <p>
	 * The mapping describing how views from the processed
	 * {@code PipelineDocument} get mapped to the input views of the component.
	 * </p>
	 */
	private final Collection<Pair<String, String>> documentToInputMapping;

	/**
	 * <p>
	 * Creates a new completely initialized {@code PipelineProcessor} working on
	 * the default views. It maps the default output view ("modifiedContent")
	 * from the previous component to the default input ("originalContent") of
	 * this component.
	 * </p>
	 */
	public AbstractPipelineProcessor() {
		super();
		this.documentToInputMapping = new HashSet<Pair<String, String>>();
		this.documentToInputMapping.add(new ImmutablePair<String, String>(
				"modifiedContent", "originalContent"));
	}

	/**
	 * <p>
	 * Creates a new completely initialized {@code PipelineComponent} with a
	 * mapping from the processed documents views to the input views of the
	 * component. This Constructor does not add the default mapping from
	 * "modifiedContent" to "originalContent"!
	 * </p>
	 * 
	 * @param documentToInputMapping
	 *            The mapping to use from {@code PipelineDocument} views to
	 *            input views of the component.
	 */
	public AbstractPipelineProcessor(
			Collection<Pair<String, String>> documentToInputMapping) {
		super();
		this.documentToInputMapping = new HashSet<Pair<String, String>>(
				documentToInputMapping);
	}

	/**
	 * <p>
	 * Provides the name of required input views in each processed
	 * {@code PipelineDocument}.
	 * </p>
	 * <p>
	 * Overwrite this method if you need other views as the default input view
	 * (named originalContent).
	 * </p>
	 * 
	 * @return The set of input view names required to be present in each
	 *         processed {@code PipelineDocument}.
	 */
	public List<String> getInputViewNames() {
		return Collections.unmodifiableList(Arrays
				.asList(new String[] { "originalContent" }));
	}

	/**
	 * <p>
	 * Provides the set of created output view names or changed input view
	 * names.
	 * </p>
	 * <p>
	 * Overwrite this method if you need other views as the default output view
	 * (named modifiedContent).
	 * </p>
	 * 
	 * @return The set of names either created views or changed input views
	 *         provided by all processed {@code PipelineDocument}s.
	 */
	public List<String> getOutputViewNames() {
		return Collections.unmodifiableList(Arrays
				.asList(new String[] { "modifiedContent" }));
	}

	@Override
	public final void process(PipelineDocument<T> document)
			throws DocumentUnprocessableException {
		if (document == null)
			throw new IllegalArgumentException("Document may not be null");

		allInputViewsAvailable(document);
		applyMapping(document);
		processDocument(document);
		allOutputViewsAvailable(document);
	}

	/**
	 * <p>
	 * Applies the mapping from {@link #documentToInputMapping}.
	 * </p>
	 * 
	 * @param document
	 *            The document to apply the mapping to.
	 * @throws DocumentUnprocessableException
	 *             In case the document does not provide the necessary input or
	 *             output views.
	 */
	private void applyMapping(PipelineDocument<T> document)
			throws DocumentUnprocessableException {
		for (Pair<String, String> mapping : documentToInputMapping) {
			// Ignore the mapping from modified content to original content if
			// modified content is not available
			// This is necessary to handle the case of the initial component in
			// a pipeline where no modifiedContent
			// exists yet as well as the case of simple annotators that do not
			// modify the content.
			if (mapping.getKey().equals("modifiedContent")
					&& mapping.getValue().equals("originalContent")) {
				if (!document.providesView(mapping.getKey())) {
					return;
				}
			}

			if (document.providesView(mapping.getKey())
					&& document.providesView(mapping.getValue())) {
				document.putView(mapping.getValue(),
						document.getView(mapping.getKey()));
			} else {
				throw new DocumentUnprocessableException(
						"Document is not processable since it either does not provide all necessary input or not all necessary output views.\n\tInputViews: "
								+ getInputViewNames()
								+ "\n\tOutputViews:"
								+ getOutputViewNames());
			}
		}
	}

	/**
	 * <p>
	 * Apply the algorithm implemented by this {@code PipelineProcessor} to a
	 * {@code PipelineDocument}. This is the central method of each
	 * {@code PipelineProcessor} providing the core functionality.
	 * </p>
	 * 
	 * @param document
	 *            The {@code PipelineDocument} to process.
	 * @throws DocumentUnprocessableException
	 *             If the {@code document} could not be processed by this
	 *             {@code PipelineProcessor}.
	 */
	protected abstract void processDocument(PipelineDocument<T> document)
			throws DocumentUnprocessableException;

	/**
	 * <p>
	 * Checks whether all output views where created in a
	 * {@code PipelineDocument} and throws an
	 * {@code DocumentUnprocessableException} if not.
	 * </p>
	 * 
	 * @param document
	 *            The {@code PipelineDocument} to check.
	 * @throws DocumentUnprocessableException
	 *             In case the document does not provide the required output
	 *             view.
	 */
	private void allOutputViewsAvailable(PipelineDocument<T> document)
			throws DocumentUnprocessableException {
		for (String outputViewName : getOutputViewNames()) {
			if (!document.providesView(outputViewName)) {
				throw new DocumentUnprocessableException(
						"Input document: "
								+ document
								+ " does not provide required output.\nRequired views: "
								+ getOutputViewNames() + "\nProvided views: "
								+ document.getProvidedViewNames());
			}
		}
	}

	/**
	 * <p>
	 * Checks whether all input views where provided with a
	 * {@code PipelineDocument} and throws an
	 * {@code DocumentUnprocessableException} if not.
	 * </p>
	 * 
	 * @param document
	 *            The {@code PipelineDocument} to check.
	 * @throws DocumentUnprocessableException
	 *             In case the document does not provide the required input
	 *             view.
	 */
	private void allInputViewsAvailable(PipelineDocument<T> document)
			throws DocumentUnprocessableException {
		for (String inputViewName : getInputViewNames()) {
			if (!document.providesView(inputViewName)) {
				throw new DocumentUnprocessableException("Input document: "
						+ document
						+ " does not provide required input.\nRequired views: "
						+ getInputViewNames() + "\nProvided views: "
						+ document.getProvidedViewNames());
			}
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}
}
