package ws.palladian.extraction;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import ws.palladian.model.features.FeatureVector;

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
public class PipelineDocument {

    /**
     * <p>
     * A vector of all features extracted for this document by some pipeline.
     * </p>
     */
    private FeatureVector featureVector;

    /**
     * <p>
     * A map storing different views on the content of a document. By default this map contains an entry for the
     * documents original content as well as modified content. Modified content is initialized to be the same as the
     * original content but might be changed to represent a cleaned or extended representation.
     * </p>
     */
    private final Map<String, String> views;

    /**
     * <p>
     * Creates a new {@code PipelineDocument} with initialized content. This instance is ready to be processed by a
     * {@link ProcessingPipeline}.
     * </p>
     * 
     * @param originalContent The content of this {@code PipelineDocument}.
     */
    public PipelineDocument(String originalContent) {
        super();
        this.views = new HashMap<String, String>();
        this.views.put("originalContent", originalContent);
        this.views.put("modifiedContent", originalContent);
        this.featureVector = new FeatureVector();
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
        this.featureVector = featureVector;
    }

    /**
     * <p>
     * Provides the original content of the processed document as retrieved from the web, the local file system or any
     * other source providing documents.
     * </p>
     * 
     * @return The unmodified original content representing the document.
     */
    public String getOriginalContent() {
        return this.views.get("originalContent");
    }

    /**
     * <p>
     * Resets this documents content completely overwriting any previous original content.
     * </p>
     * 
     * @param originalContent The new unmodified original content representing the document.
     */
    public void setOriginalContent(String originalContent) {
        this.views.put("originalContent", originalContent);
    }

    /**
     * <p>
     * Provides the modified content of this document. Modified content is usually inserted by {@link PipelineProcessor}
     * s.
     * </p>
     * 
     * @return The modified content of the document or {@code null} if no modified content is available yet.
     */
    public String getModifiedContent() {
        return this.views.get("modifiedContent");
    }

    /**
     * <p>
     * Resets the modified content completely overwriting any old modified content.
     * </p>
     * 
     * @param modifiedContent The content of this document modified by some {@link PipelineProcessor}.
     */
    public void setModifiedContent(String modifiedContent) {
        this.views.put("modifiedContent", modifiedContent);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PipelineDocument [featureVector=");
        builder.append(featureVector);
        builder.append(", originalContent=");
        builder.append(getOriginalContent());
        builder.append(", modifiedContent=");
        builder.append(getModifiedContent());
        builder.append("]");
        return builder.toString();
    }

    /**
     * <p>
     * Resets and overrides the content of a named view or initializes the view of it didn't exist yet.
     * </p>
     * 
     * @param viewName The name of the view.
     * @param content The text content as the new view of the document.
     */
    public void putView(String viewName, String content) {
        this.views.put(viewName, content);
    }

    /**
     * <p>
     * Provides the content of a named view.
     * </p>
     * 
     * @param viewName The name of the view, providing the requested content.
     * @return The views content or {@code null} if there is no such view available.
     */
    public String getView(String viewName) {
        return this.views.get(viewName);
    }

    /**
     * <p>
     * Checks whether this document provides a view with the provided name.
     * </p>
     * 
     * @param inputViewName The name of the requested view.
     * @return {@code true} if the document provides the requested view; {@code false} otherwise.
     */
    public boolean providesView(String inputViewName) {
        return this.views.containsKey(inputViewName);
    }

    /**
     * <p>
     * Returns a set of the names of all views this document provides currently on its content.
     * </p>
     * 
     * @return The set of all provided view names.
     */
    public Set<String> getProvidedViewNames() {
        return Collections.unmodifiableSet(this.views.keySet());
    }
    
    // FIXME for hashCode/equals to work properly, FeatureVector must also implement hashCode/equals,
    // but currently, the FeatureVector implementation's field is set to transient. Why? See issue #48
    // https://bitbucket.org/palladian/palladian/issue/48/transient-field-in-featurevector

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((featureVector == null) ? 0 : featureVector.hashCode());
        result = prime * result + ((views == null) ? 0 : views.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        PipelineDocument other = (PipelineDocument)obj;
        if (featureVector == null) {
            if (other.featureVector != null)
                return false;
        } else if (!featureVector.equals(other.featureVector))
            return false;
        if (views == null) {
            if (other.views != null)
                return false;
        } else if (!views.equals(other.views))
            return false;
        return true;
    }
    
}
