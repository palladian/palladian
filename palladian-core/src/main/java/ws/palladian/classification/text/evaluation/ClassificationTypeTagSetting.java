package ws.palladian.classification.text.evaluation;

import java.io.Serializable;

/**
 * More specific settings for the {@link ClassificationTypeSetting.TAG} setting.
 * 
 * @author David Urbansky
 * 
 */
public class ClassificationTypeTagSetting implements Serializable {

    private static final long serialVersionUID = -8283073272037116796L;

    /** Only tags that are classified with a confidence above the threshold are assigned. */
    private double tagConfidenceThreshold = 0.0;

    /** Minimum number of tags that are assigned to a document in tagging mode. */
    private int minTags = 1;

    /** Maximum number of tags that are assigned to a document in tagging mode. */
    private int maxTags = 5;

    /** If true, the tags that appear in the text (or URL) are weighted higher. */
    private boolean tagBoost = false;

    /** Enable category co-occurrence, that works only in tag mode. */
    private boolean useCooccurrence = false;
    
    public ClassificationTypeTagSetting() {
    }

    public ClassificationTypeTagSetting(ClassificationTypeTagSetting tagSetting) {
        this.tagConfidenceThreshold = tagSetting.tagConfidenceThreshold;
        this.minTags = tagSetting.minTags;
        this.maxTags = tagSetting.maxTags;
        this.tagBoost = tagSetting.tagBoost;
        this.useCooccurrence = tagSetting.useCooccurrence;
    }

    public double getTagConfidenceThreshold() {
        return tagConfidenceThreshold;
    }

    public void setTagConfidenceThreshold(double tagConfidenceThreshold) {
        this.tagConfidenceThreshold = tagConfidenceThreshold;
    }

    public int getMinTags() {
        return minTags;
    }

    public void setMinTags(int minTags) {
        this.minTags = minTags;
    }

    public int getMaxTags() {
        return maxTags;
    }

    public void setMaxTags(int maxTags) {
        this.maxTags = maxTags;
    }

    public boolean isTagBoost() {
        return tagBoost;
    }

    public void setTagBoost(boolean tagBoost) {
        this.tagBoost = tagBoost;
    }

    public boolean isUseCooccurrence() {
        return useCooccurrence;
    }

    public void setUseCooccurrence(boolean useCooccurrence) {
        this.useCooccurrence = useCooccurrence;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClassificationTypeTagSetting [maxTags=");
        builder.append(maxTags);
        builder.append(", minTags=");
        builder.append(minTags);
        builder.append(", tagBoost=");
        builder.append(tagBoost);
        builder.append(", tagConfidenceThreshold=");
        builder.append(tagConfidenceThreshold);
        builder.append(", useCooccurrence=");
        builder.append(useCooccurrence);
        builder.append("]");
        return builder.toString();
    }

}