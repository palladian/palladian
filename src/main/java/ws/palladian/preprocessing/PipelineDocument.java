package ws.palladian.preprocessing;

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
 * @version 1.0
 * @since 1.0
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
     * The unmodified original content representing the document.
     * </p>
     */
    private String originalContent;

    /**
     * <p>
     * The content of this document modified by some {@link PipelineProcessor}.
     * </p>
     */
    private String modifiedContent;

    /**
     * <p>
     * Creates a new {@code PipelineDocument} with initialized content. This instance is ready to be processed by a
     * {@link ProcessingPipeline}.
     * </p>
     * 
     * @param originalContent The content of this {@code PipelineDocument}.
     */
    public PipelineDocument(String originalContent) {
        this.originalContent = originalContent;
        this.modifiedContent = originalContent;
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
        return originalContent;
    }

    /**
     * <p>
     * Resets this documents content completely overwriting any previous original content.
     * </p>
     * 
     * @param originalContent The new unmodified original content representing the document.
     */
    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
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
        return modifiedContent;
    }

    /**
     * <p>
     * Resets the modified content completely overwriting any old modified content.
     * </p>
     * 
     * @param modifiedContent The content of this document modified by some {@link PipelineProcessor}.
     */
    public void setModifiedContent(String modifiedContent) {
        this.modifiedContent = modifiedContent;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PipelineDocument [featureVector=");
        builder.append(featureVector);
        builder.append(", originalContent=");
        builder.append(originalContent);
        builder.append(", modifiedContent=");
        builder.append(modifiedContent);
        builder.append("]");
        return builder.toString();
    }

}
