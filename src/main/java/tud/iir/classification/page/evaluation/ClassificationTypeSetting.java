package tud.iir.classification.page.evaluation;

import tud.iir.classification.page.TextClassifier;

public class ClassificationTypeSetting {

    // // classification types
    /** take only the first category specified in the txt file */
    public static final int SINGLE = 1;

    /** take all categories and treat them as a hierarchy */
    public static final int HIERARCHICAL = 2;

    /** take all categories ant treat them as tags */
    public static final int TAG = 3;

    /**
     * the classification type under which the classifier operates, this must be one of {@link SINGLE},
     * {@link HIERARCHICAL}, or {@link TAG}
     */
    private int classificationType = SINGLE;

    /** Whether or not the classifier should be serialized. */
    private boolean serializeClassifier = false;

    /** configurations that only apply if {@link classifcationType} is set to {@link TAG} */
    private ClassificationTypeTagSetting classificationTypeTagSetting;

    /**
     * Set the classification type under which the classifier operates.
     * 
     * @param classificationType The classification type must be one of {@link TextClassifier.SINGLE},
     *            {@link TextClassifier.HIERARCHICAL}, or {@link TextClassifier.TAG}.
     */
    public void setClassificationType(int classificationType) {
        this.classificationType = classificationType;
    }

    public int getClassificationType() {
        return classificationType;
    }

    public void setClassificationTypeTagSetting(ClassificationTypeTagSetting classificationTypeTagSetting) {
        this.classificationTypeTagSetting = classificationTypeTagSetting;
    }

    public ClassificationTypeTagSetting getClassificationTypeTagSetting() {
        return classificationTypeTagSetting;
    }

    public void setSerializeClassifier(boolean serializeClassifier) {
        this.serializeClassifier = serializeClassifier;
    }

    public boolean isSerializeClassifier() {
        return serializeClassifier;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("classificationType=");
        builder.append(classificationType);
        if (getClassificationType() == TAG) {
            builder.append(", classificationTypeTagSetting=");
            builder.append(classificationTypeTagSetting);
        }
        return builder.toString();
    }

}
