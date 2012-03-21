/**
 * Created on: 18.06.2011 15:32:57
 */
package ws.palladian.extraction;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * <p>
 * Abstract base class for pipeline processors. Handles the mapping between input and output views.
 * </p>
 * 
 * @author Klemens Muthmann
 * @since 0.8
 * @version 1.0
 */
public abstract class AbstractPipelineProcessor implements PipelineProcessor {
    /**
     * <p>
     * Unique identifier to serialize and deserialize objects of this type to and from a file.
     * </p>
     */
    private static final long serialVersionUID = -7030337967596448903L;
    /**
     * <p>
     * All input views required by this {@code PipelineProcessor} to work correctly.
     * </p>
     */
    private final List<String> inputViewNames;
    /**
     * <p>
     * All output view names created by this {@code PipelineProcessor} or input views changed by it.
     * </p>
     */
    private final List<String> outputViewNames;

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working on the default views. These are
     * "originalContent" as input view and "modifiedContent" as output.
     * </p>
     */
    public AbstractPipelineProcessor() {
        this(new String[] {"originalContent"}, new String[] {"modifiedContent"});
    }

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} working on a set of input views and creating a set
     * of output views in each processed {@code PipelineDocument}.
     * </p>
     * 
     * @param inputViewNames The set of input view names required to be present in each processed
     *            {@code PipelineDocument}.
     * @param outputViewNames The set of names either created views or changed input views provided by all processed
     *            {@code PipelineDocument}s.
     */
    public AbstractPipelineProcessor(String[] inputViewNames, String[] outputViewNames) {
        super();
        this.inputViewNames = Arrays.asList(inputViewNames);
        this.outputViewNames = Arrays.asList(outputViewNames);
    }

    /**
     * <p>
     * Creates a new completely initialized {@code PipelineProcessor} providing only the default "modifiedContent" view
     * as output but requiring
     * </p>
     * 
     * @param inputViewNames The set of input view names required to be present in each processed
     *            {@code PipelineDocument}.
     */
    public AbstractPipelineProcessor(String[] inputViewNames) {
        this(inputViewNames, new String[] {"modifiedContent"});
    }

    /**
     * <p>
     * Provides the name of required input views in each processed {@code PipelineDocument}.
     * </p>
     * 
     * @return The set of input view names required to be present in each processed {@code PipelineDocument}.
     */
    public final List<String> getInputViewNames() {
        return Collections.unmodifiableList(this.inputViewNames);
    }

    /**
     * <p>
     * Provides the set of created output view names or changed input view names.
     * </p>
     * 
     * @return The set of names either created views or changed input views provided by all processed
     *         {@code PipelineDocument}s.
     */
    public final List<String> getOutputViewNames() {
        return Collections.unmodifiableList(outputViewNames);
    }

    @Override
    public final void process(PipelineDocument document) {
        allInputViewsAvailable(document);
        processDocument(document);
        allOutputViewsAvailable(document);
    }

    /**
     * <p>
     * Apply the algorithm implemented by this {@code PipelineProcessor} to a {@code PipelineDocument}. This is the
     * central method of each {@code PipelineProcessor} providing the core functionality.
     * </p>
     * 
     * @param document The {@code PipelineDocument} to process.
     */
    protected abstract void processDocument(PipelineDocument document);

    /**
     * <p>
     * Checks whether all output views where created in a {@code PipelineDocument} and thrwos an
     * {@code IllegalStateException} if not.
     * </p>
     * 
     * @param document The {@code PipelineDocument} to check.
     */
    private void allOutputViewsAvailable(PipelineDocument document) {
        for (String outputViewName : outputViewNames) {
            if (!document.providesView(outputViewName)) {
                throw new IllegalStateException("Input document: " + document
                        + " does not provide required output.\nRequired views: " + outputViewNames
                        + "\nProvided views: " + document.getProvidedViewNames());
            }
        }
    }

    /**
     * <p>
     * Checks whether all input views where provided with a {@code PipelineDocument} and throws an
     * {@code IllegalStateException} if not.
     * </p>
     * 
     * @param document The {@code PipelineDocument} to check.
     */
    private void allInputViewsAvailable(PipelineDocument document) {
        for (String inputViewName : inputViewNames) {
            if (!document.providesView(inputViewName)) {
                throw new IllegalStateException("Input document: " + document
                        + " does not provide required input.\nRequired views: " + inputViewNames + "\nProvided views: "
                        + document.getProvidedViewNames());
            }
        }
    }
}
