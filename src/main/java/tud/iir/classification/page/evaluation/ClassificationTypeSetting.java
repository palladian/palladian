package tud.iir.classification.page.evaluation;

import tud.iir.classification.page.WebPageClassifier;

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

    /** enable category co-occurrence, that works only in tag mode */
    public static final boolean CO_OCCURRENCE_IN_TAG_MODE = false;

    /** if true, the tags that appear in the text (or URL) are weighted higher */
    public static final boolean TAG_IN_URL_BOOST = false;

    /** number of tags that are assigned to a document in tagging mode */
    public static final int NUMBER_OF_TAGS = 5;

    /**
     * Set the classification type under which the classifier operates.
     * 
     * @param classificationType The classification type must be one of {@link WebPageClassifier.SINGLE},
     *            {@link WebPageClassifier.HIERARCHICAL}, or {@link WebPageClassifier.TAG}.
     */
    public void setClassificationType(int classificationType) {
        this.classificationType = classificationType;
    }

    public int getClassificationType() {
        return classificationType;
    }
}
