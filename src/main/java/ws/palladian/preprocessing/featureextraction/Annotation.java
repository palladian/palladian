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
 */
public abstract class Annotation {

    /**
     * <p>
     * The document this {@link Annotation} points to.
     * </p>
     */
    private final PipelineDocument document;

    /**
     * <p>
     * The feature vector of this {@link Annotation}.
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
     * Creates a new completely initialized {@link Annotation}.
     * </p>
     * 
     * @param document The {@link PipelineDocument} the {@link Annotation} points to.
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
     * Creates a new document {@link Annotation} pointing on the "originalContent" view of the document.
     * </p>
     * 
     * @param document The document the {@link Annotation} points to.
     */
    public Annotation(PipelineDocument document) {
        this(document, "originalContent");
    }

    /**
     * <p>
     * Provides the {@link PipelineDocument} this {@link Annotation} points to.
     * </p>
     * 
     * @return The {@link PipelineDocument} containing the annotated content.
     */
    public final PipelineDocument getDocument() {
        return this.document;
    }

    /**
     * <p>
     * Provides the index of the first character of this {@link Annotation}.
     * </p>
     * 
     * @return the index of the first character of this {@link Annotation}.
     */
    public abstract int getStartPosition();

    /**
     * <p>
     * Provides the index of the first character after the end of this {@link Annotation}.
     * </p>
     * 
     * @return the index of the first character after the end of this {@link Annotation}.
     */
    public abstract int getEndPosition();

    /**
     * <p>
     * Provides the value of this {@link Annotation}, usually from the underlying {@link PipelineDocument}.
     * </p>
     * 
     * @return The value of this {@link Annotation} as a {@link String}.
     */
    public abstract String getValue();

    /**
     * <p>
     * Set the value of this {@link Annotation}. Usually, the value depends on supplied positions and is determined
     * directly from the associated {@link PipelineDocument}. This method provides the possibility to manually override
     * the value, which is necessary e.g. when stemming or lemmatization is applied.
     * </p>
     * 
     * @param value
     */
    public abstract void setValue(String value);

    /**
     * <p>
     * The {@link FeatureVector} of this {@link Annotation}.
     * </p>
     * 
     * @return A {@link FeatureVector} containing this {@link Annotation}.
     */
    public final FeatureVector getFeatureVector() {
        return this.featureVector;
    }

    /**
     * <p>
     * Provides the name of the view inside the {@link PipelineDocument} providing the annotated content.
     * </p>
     * 
     * @return The view's name.
     */
    public final String getViewName() {
        return this.viewName;
    }

    //
    // force subclasses to implement the following methods
    //

    @Override
    public abstract String toString();

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

}