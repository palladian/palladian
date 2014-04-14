package ws.palladian.classification.featureselection;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.text.CountingCategoryEntriesBuilder;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.Instance;
import ws.palladian.core.Value;

/**
 * <p>
 * Implements the Information Gain formula as proposed by the Weka Machine Learning Framework. A description can be
 * found under <a href="http://arxiv.org/pdf/nlin/0307015v4.pdf">http://arxiv.org/pdf/nlin/0307015v4.pdf</a> on page 47.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public final class InformationGainFormula {

    private final Iterable<? extends Instance> dataset;

    private final double entropy;

    /**
     * <p>
     * Creates a new completely initialized object of this class. The provided dataset needs to provide all the features
     * you want to calculate information gain values for. If the feature is dense it should not have missing values.
     * </p>
     * 
     * @param dataset The dataset this formula works on.
     */
    public InformationGainFormula(Iterable<? extends Instance> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.dataset = dataset;
        CategoryEntries categoryCounts = ClassificationUtils.getCategoryCounts(dataset);
        this.entropy = ClassificationUtils.entropy(categoryCounts);
    }

    /**
     * <p>
     * Calculates the information gain value for the dense feature provided by the feature name. This feature MUST occur
     * in every instance of the dataset this formula works on.
     * </p>
     * 
     * @param featureName The name of the feature to calculate information gain for.
     * @return The information gain value of the feature.
     */
    public double calculateGain(String featureName) {
        return entropy - conditionalEntropy(featureName, dataset);
    }

    /**
     * <p>
     * Counts how often the values of the feature with the provided name occurs in the dataset handled by this object.
     * </p>
     * 
     * @param featureName The name of the feature to count.
     * @return A mapping from a {@link String} representation of the value to a counter of how often it occurs.
     */
    private static CategoryEntries countFeatureOccurrences(Iterable<? extends Instance> dataset, String featureName) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Instance dataItem : dataset) {
            Value theValue = dataItem.getVector().get(featureName);
            String value = theValue.toString();
            builder.add(value, 1);
        }
        return builder.create();
    }

    /**
     * <p>
     * Counts the joint occurrences of each value of the provided feature with each target class.
     * </p>
     * 
     * @param featureName The name of the feature to calculate the joint occurrences for.
     * @return A mapping from a pair of target class and feature value to the counter of their joint occurrences.
     */
    private static CategoryEntries countJointOccurrences(Iterable<? extends Instance> dataset, String featureName) {
        CountingCategoryEntriesBuilder builder = new CountingCategoryEntriesBuilder();
        for (Instance dataItem : dataset) {
            Value theValue = dataItem.getVector().get(featureName);
            String value = theValue.toString();
            builder.add(dataItem.getCategory() + "###" + value, 1);
        }
        return builder.create();
    }

    /**
     * <p>
     * Calculates the conditional entropy of the dataset under the consideration that we know how the provided feature
     * is distributed. This is often called H(X|Y).
     * </p>
     * 
     * @param featureName The name of the feature to calculate the conditional entropy for.
     * @return The conditional entropy of the dataset knowing the distribution of Y.
     */
    private static double conditionalEntropy(String featureName, Iterable<? extends Instance> dataset) {
        CategoryEntries jointOccurrences = countJointOccurrences(dataset, featureName);
        CategoryEntries featureOccurrences = countFeatureOccurrences(dataset, featureName);
        return ClassificationUtils.entropy(jointOccurrences) - ClassificationUtils.entropy(featureOccurrences);
    }

}
