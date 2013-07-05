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
 * @version 2.0
 */
public abstract class PipelineDocument<T> extends FeatureVector {

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
        this.content = content;
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
        return "PipelineDocument [content=" + content + ", getAll()=" + getAll() + "]";
    }

    // Adapted method. Do not change if you don't know what you are doing.
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((content == null) ? 0 : content.hashCode());
        return result;
    }

    // Adapted method. Do not change if you don't know what you are doing.
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PipelineDocument<?> other = (PipelineDocument<?>)obj;
        if (!super.equals(obj)) {
            return false;
        }
        if (content == null) {
            if (other.content != null)
                return false;
        } else if (!content.equals(other.content))
            return false;
        return true;
    }

}
