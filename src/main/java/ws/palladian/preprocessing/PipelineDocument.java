package ws.palladian.preprocessing;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.model.features.Feature;

public class PipelineDocument {

    private List<Feature<?>> features;

    private String originalContent = "";

    private String modifiedContent = "";

    public PipelineDocument(String originalContent) {
        this.originalContent = originalContent;
        this.modifiedContent = originalContent;
        this.features = new ArrayList<Feature<?>>();
    }

    public List<Feature<?>> getFeatures() {
        return features;
    }

    public void setFeatures(List<Feature<?>> features) {
        this.features = features;
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

}
