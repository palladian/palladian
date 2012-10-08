package ws.palladian.classification;

import java.io.Serializable;

import ws.palladian.classification.text.TextInstance;
import ws.palladian.classification.text.evaluation.ClassificationTypeSetting;
import ws.palladian.classification.text.evaluation.FeatureSetting;

@Deprecated
public abstract class ClassifierOld<T> implements Serializable {

    /** The serialize version ID. */
    private static final long serialVersionUID = -7017462894898815981L;

//    /** A classifier has a name. */
//    //why???
//    private String name = "";

    /** A classifier has training documents. */
    private transient Instances<T> trainingInstances = new Instances<T>();

//    /** A classifier has test documents that can be used to calculate recall, precision, and F-score. */
//    private transient Instances<T> testInstances = new Instances<T>();

    /** A classifier classifies to certain categories. */
    protected Categories categories;

//    protected ProcessingPipeline processingPipeline = new ProcessingPipeline();

    /**
     * Configurations for the classification type ({@link ClassificationTypeSetting.SINGLE},
     * {@link ClassificationTypeSetting.HIERARCHICAL}, or {@link ClassificationTypeSetting.TAG}).
     */
    protected ClassificationTypeSetting classificationTypeSetting = new ClassificationTypeSetting();

    /** The feature settings which should be used by the text classifier. */
    private FeatureSetting featureSetting = new FeatureSetting();

    public Categories getCategories() {
        return categories;
    }

    public void setCategories(Categories categories) {
        this.categories = categories;
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

    public Instances<T> getTrainingInstances() {
        return trainingInstances;
    }

    public void setTrainingInstances(Instances<T> trainingInstances) {
        this.trainingInstances = trainingInstances;
        getPossibleCategories(trainingInstances);
    }

//    /**
//     * <p>
//     * Add a single training instance, consider using {@link addTrainingInstances} because that is <b>a lot</b> faster.
//     * </p>
//     * 
//     * @param instance The instance to add.
//     */
//    public void addTrainingInstance(T instance) {
//        if (this.trainingInstances == null) {
//            this.trainingInstances = new Instances<T>();
//        }
//        this.trainingInstances.add(instance);
//        getPossibleCategories(trainingInstances);
//    }

//    public void addTrainingInstances(Instances<T> trainingInstances) {
//        if (this.trainingInstances == null) {
//            this.trainingInstances = new Instances<T>();
//        }
//        this.trainingInstances.addAll(trainingInstances);
//        getPossibleCategories(trainingInstances);
//    }

//    public Instances<T> getTestInstances() {
//        return testInstances;
//    }

//    public void setTestInstances(Instances<T> testInstances) {
//        this.testInstances = testInstances;
//    }

    /**
     * After training instances have been assigned, we can find out which nominal categories are possible for the
     * classifier to classify.
     */
    protected void getPossibleCategories(Instances<T> instances) {
        if (getCategories() == null) {
            setCategories(new Categories());
        } else {
            // we need to reset all frequency counts since we're counting them now
            categories.resetFrequencies();
        }
        for (T instance : instances) {
            String categoryName = ((TextInstance)instance).getInstanceCategory().getName();
            Category category = getCategories().getCategoryByName(categoryName);
            if (category == null) {
                category = new Category(categoryName);
                category.increaseFrequency();
                getCategories().add(category);
            } else {
                category.increaseFrequency();
            }
        }
        getCategories().calculatePriors();
    }

    public abstract void save(String classifierPath);

//    public void setProcessingPipeline(ProcessingPipeline processingPipeline) {
//        this.processingPipeline = processingPipeline;
//    }
//
//    public ProcessingPipeline getProcessingPipeline() {
//        return processingPipeline;
//    }

}