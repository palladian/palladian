package tud.iir.classification;

import java.io.Serializable;

import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.classification.page.evaluation.FeatureSetting;

public abstract class Classifier implements Serializable {

    /** The serialize version ID. */
    private static final long serialVersionUID = -7017462894898815981L;

    /** A classifier has a name. */
    private String name = "";

    /** A classifier classifies to certain categories. */
    public Categories categories = null;

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
    }

    /**
     * Configurations for the classification type ({@link ClassificationTypeSetting.SINGLE},
     * {@link ClassificationTypeSetting.HIERARCHICAL}, or {@link ClassificationTypeSetting.TAG}).
     */
    protected ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

    /** The feature settings which should be used by the text classifier. */
    private FeatureSetting featureSetting = new FeatureSetting();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setClassificationTypeSetting(ClassificationTypeSetting classificationTypeSetting) {
        this.classificationTypeSetting = classificationTypeSetting;
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSetting;
    }

    public int getClassificationType() {
        return getClassificationTypeSetting().getClassificationType();
    }

    public void setFeatureSetting(FeatureSetting featureSetting) {
        this.featureSetting = featureSetting;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSetting;
    }

}