package ws.palladian.processing;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Represents a document processed by a {@link PipelineProcessor}. These documents are the input for pipelines. They
 * contain the content that is processed by the pipeline and the features extracted from that content as well as some
 * modified content.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public abstract class PipelineDocument<T> implements Classifiable {

    /**
     * <p>
     * A vector of all features extracted for this document.
     * </p>
     */
    private final FeatureVector featureVector;

    /**
     * <p>
     * Data representing this document such as a text or image data if the document is an image.
     * </p>
     */
    private T content;

    /**
     * <p>
     * Creates a new {@code PipelineDocument} with initialized content. This instance is ready to be processed by a
     * {@link ProcessingPipeline}.
     * </p>
     * 
     * @param content The content of this {@code PipelineDocument}.
     */
    protected PipelineDocument(T content) {
        Validate.notNull(content);
        this.featureVector = new FeatureVector();
        this.content = content;
    }

    /**
     * <p>
     * Provides a special structured representation of a document as used by classifiers or clusterers.
     * </p>
     * 
     * @return A vector of all features extracted for this document by some pipeline.
     */
    @Override
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    /**
     * <p>
     * Provides the original content of the processed document as retrieved from the web, the local file system or any
     * other source providing documents.
     * </p>
     * 
     * @return The unmodified original content representing the document.
     */
    public T getContent() {
        return content;
    }

    /**
     * <p>
     * Resets this documents content completely overwriting any previous content.
     * </p>
     * 
     * @param content The new content representing the document.
     */
    public void setContent(T content) {
        Validate.notNull(content);
        this.content = content;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PipelineDocument [featureVector=");
        builder.append(featureVector);
        builder.append(", content=");
        builder.append(getContent());
        builder.append("]");
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureVector == null) ? 0 : featureVector.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PipelineDocument<?> other = (PipelineDocument<?>)obj;
        if (featureVector == null) {
            if (other.featureVector != null)
                return false;
        } else if (!featureVector.equals(other.featureVector))
            return false;
        return true;
    }

}
