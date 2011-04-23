package ws.palladian.preprocessing;

import ws.palladian.model.features.FeatureVector;

public class PipelineDocument {

    private FeatureVector featureVector;

    private String originalContent;

    private String modifiedContent;

    public PipelineDocument(String originalContent) {
        this.originalContent = originalContent;
        this.modifiedContent = originalContent;
        this.featureVector = new FeatureVector();
    }

    public FeatureVector getFeatureVector() {
        return featureVector;
    }

    public void setFeatureVector(FeatureVector featureVector) {
        this.featureVector = featureVector;
    }

    /**
     * @return The original content of the document.
     */
    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    /**
     * @return The modified content of the document.
     */
    public String getModifiedContent() {
        return modifiedContent;
    }

    public void setModifiedContent(String modifiedContent) {
        this.modifiedContent = modifiedContent;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
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
