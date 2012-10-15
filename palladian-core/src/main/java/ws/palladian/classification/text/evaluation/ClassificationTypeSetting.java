package ws.palladian.classification.text.evaluation;

import java.io.Serializable;

/**
 * The settings which classification type and which settings for that should be used for a classifier.
 * 
 * @author David Urbansky
 * 
 */
public class ClassificationTypeSetting implements Serializable {

    private static final long serialVersionUID = 8603357688386384581L;

    // // classification types
    /** Take only the first category specified in the txt file. */
    public static final int SINGLE = 1;

    /** Take all categories and treat them as a hierarchy. */
    public static final int HIERARCHICAL = 2;

    /** Take all categories ant treat them as tags. */
    public static final int TAG = 3;

    /** The output variable is a number. */
    public static final int REGRESSION = 4;

    /**
     * the classification type under which the classifier operates, this must be one of {@link SINGLE},
     * {@link HIERARCHICAL}, or {@link TAG}
     */
    private int classificationType = SINGLE;

//    /** Whether or not the classifier should be serialized. */
//    private boolean serializeClassifier = false;

    /** configurations that only apply if {@link classifcationType} is set to {@link TAG} */
    private ClassificationTypeTagSetting classificationTypeTagSetting = new ClassificationTypeTagSetting();

    
    public ClassificationTypeSetting() {
        
    }
    
    public ClassificationTypeSetting(ClassificationTypeSetting cts) {
        super();
//        try {
//            PropertyUtils.copyProperties(this, cts);
//        } catch (IllegalAccessException e) {
//            Logger.getRootLogger().error(e);
//        } catch (InvocationTargetException e) {
//            Logger.getRootLogger().error(e);
//        } catch (NoSuchMethodException e) {
//            Logger.getRootLogger().error(e);
//        }
        this.classificationType = cts.classificationType;
//        this.serializeClassifier = cts.serializeClassifier;
        this.classificationTypeTagSetting = new ClassificationTypeTagSetting(cts.classificationTypeTagSetting);
    }
    
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

//    public void setSerializeClassifier(boolean serializeClassifier) {
//        this.serializeClassifier = serializeClassifier;
//    }

//    public boolean isSerializeClassifier() {
//        return serializeClassifier;
//    }

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