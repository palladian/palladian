package tud.iir.classification.numeric;

import org.apache.log4j.Logger;

import tud.iir.classification.Categories;
import tud.iir.classification.Category;
import tud.iir.classification.Classifier;

/**
 * The classifier is an abstract class that provides basic methods used by concrete classifiers.
 * 
 * @author David Urbansky
 */
public abstract class NumericClassifier extends Classifier {

    /** The serialize version ID. */
    private static final long serialVersionUID = -8370153238631532469L;

    /** The logger for this class. */
    protected static final Logger LOGGER = Logger.getLogger(NumericClassifier.class);

    /** A classifier has training documents. */
    private transient NumericInstances trainingInstances = new NumericInstances();


    /** A classifier has test documents that can be used to calculate recall, precision, and F-score. */
    private transient NumericInstances testInstances = new NumericInstances();

    /**
     * The constructor, initiate members.
     */
    public NumericClassifier() {
        reset();
    }

    /**
     * Reset the classifier.
     */
    public void reset() {
        categories = new Categories();
    }

    public NumericInstances getTrainingInstances() {
        return trainingInstances;
    }

    public void setTrainingInstances(NumericInstances trainingInstances) {
        this.trainingInstances = trainingInstances;
        getPossibleCategories();
    }

    /**
     * After training instances have been assigned, we can find out which nominal categories are possible for the
     * classifier to classify.
     */
    private void getPossibleCategories() {
        for (NumericInstance instance : trainingInstances) {
            String categoryName = ((Category) instance.getInstanceClass()).getName();
            Category category = categories.getCategoryByName(categoryName);
            if (category == null) {
                category = new Category(categoryName);
                category.increaseFrequency();
                categories.add(category);
            } else {
                category.increaseFrequency();
            }
        }
        categories.calculatePriors();
    }

    public NumericInstances getTestInstances() {
        return testInstances;
    }

    public void setTestInstances(NumericInstances testInstances) {
        this.testInstances = testInstances;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("NumericClassifier [name=");
        builder.append(getName());
        builder.append("]");
        return builder.toString();
    }

    public abstract void classify(NumericInstance instance);
    public abstract void save(String path);

}