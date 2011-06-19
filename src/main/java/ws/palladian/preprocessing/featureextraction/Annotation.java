package ws.palladian.preprocessing.featureextraction;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.preprocessing.PipelineDocument;

/**
 * <p>
 * Abstract super class defining an Annotation.
 * </p>
 * 
 * @author Philipp Katz
 * @author Klemens Muthmann
 * @since 0.8
 * @version 1.0
 */
public abstract class Annotation {

    /**
     * <p>
     * The document this {@code Annotation} points to.
     * </p>
     */
    private final PipelineDocument document;

    /**
     * <p>
     * The feature vector this {@code Annotation} is part of.
     * </p>
     */
    private final FeatureVector featureVector;

    /**
     * <p>
     * The name of the view on the {@link #document} holding the annotated content.
     * </p>
     */
    private final String viewName;

    /**
     * <p>
     * Creates a new completely initialized {@code Annotation}.
     * </p>
     * 
     * @param document The {@code PipelineDocument} the {@code Annotation} points to.
     * @param viewName The name of the view in the document containing the annotated content.
     */
    public Annotation(PipelineDocument document, String viewName) {
        super();
        this.document = document;
        this.viewName = viewName;
        this.featureVector = new FeatureVector();
    }

    /**
     * <p>
     * Creates a new document {@code Annotation} pointing on the "originalContent" view of the document.
     * </p>
     * 
     * @param document The document the {@code Annotation} points to.
     */
    public Annotation(PipelineDocument document) {
        this(document, "originalContent");
    }

    /**
     * <p>
     * Provides the {@code PipelineDocument} this {@code Annotation} points to.
     * </p>
     * 
     * @return The {@code PipelineDocument} containing the annotated content.
     */
    public final PipelineDocument getDocument() {
        return this.document;
    }

    /**
     * <p>
     * Provides the index of the first character of this {@code Annotation}.
     * </p>
     * 
     * @return the index of the first character of this {@code Annotation}.
     */
    public abstract int getStartPosition();

    /**
     * <p>
     * Provides the index of the first character after the end of this {@code Annotation}.
     * </p>
     * 
     * @return the index of the first character after the end of this {@code Annotation}.
     */
    public abstract int getEndPosition();

    /**
     * <p>
     * Provides the value of this {@code Annotation} from the underlying {@link PipelineDocument}.
     * </p>
     * 
     * @return The value of this {@code Annotation} as a {@code String}.
     */
    public abstract String getValue();

    /**
     * <p>
     * The {@code FeatureVector} this {@code Annotation} is part of.
     * </p>
     * 
     * @return A {@code FeatureVector} containing this {@code Annotation}.
     */
    public final FeatureVector getFeatureVector() {
        return this.featureVector;
    }

    /**
     * <p>
     * Provides the name of the view inside the {@link PipelineDocument} providing the annotated content.
     * </p>
     * 
     * @return The views name.
     */
    public final String getViewName() {
        return this.viewName;
    }

}