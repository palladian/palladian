package ws.palladian.processing;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>
 * Represents a document processed by a {@link PipelineProcessor}. These documents are the input for pipelines. They
 * contain the content that is processed by the pipeline and the features extracted from that content as well as some
 * modified content.
 * </p>
 * <p>
 * This class represents documents as {@code String}s and thus can be used for text documents at the moment only.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class PipelineDocument<T> {

    /**
     * <p>
     * A vector of all features extracted for this document by some pipeline.
     * </p>
     */
    private FeatureVector featureVector;

    private T content;

    /**
     * <p>
     * Creates a new {@code PipelineDocument} with initialized content. This instance is ready to be processed by a
     * {@link ProcessingPipeline}.
     * </p>
     * 
     * @param originalContent The content of this {@code PipelineDocument}.
     */
    public PipelineDocument(T content) {
        super();
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
    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    /**
     * <p>
     * Resets this documents {@code FeatureVector} overwriting all features previously extracted.
     * </p>
     * 
     * @param featureVector The new {@code FeatureVector} of this document.
     */
    public void setFeatureVector(FeatureVector featureVector) {
        Validate.notNull(featureVector);

        this.featureVector.addAll(featureVector);
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
     * Resets this documents content completely overwriting any previous original content.
     * </p>
     * 
     * @param originalContent The new unmodified original content representing the document.
     */
    public void setContent(final T content) {
        Validate.notNull(content);

        this.content = content;
    }

    public void addFeature(final Feature<?> feature) {
        Validate.notNull(feature);

        featureVector.add(feature);
    }

    public <F extends Feature<?>> F getFeature(final FeatureDescriptor<F> descriptor) {
        Validate.notNull(descriptor);

        return featureVector.get(descriptor);
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

    // FIXME for hashCode/equals to work properly, FeatureVector must also implement hashCode/equals,
    // but currently, the FeatureVector implementation's field is set to transient. Why? See issue #48
    // https://bitbucket.org/palladian/palladian/issue/48/transient-field-in-featurevector

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
