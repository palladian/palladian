package ws.palladian.classification.featureselection;

import java.util.List;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.core.Instance;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;

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

    /** The list of {@link Trainable} making up the dataset handled by this formula. */
    private final List<Instance> dataset;
    /** The number of occurrences of each target class as calculated using {@link #countClassOccurrences()}. */
    private final Bag<String> classOccurrences;

    /**
     * <p>
     * Creates a new completely initialized object of this class. The provided dataset needs to provide all the features
     * you want to calculate information gain values for. If the feature is dense it should not have missing values.
     * Sparse features can be handled by using a {@link ListFeature}.
     * </p>
     * 
     * @param dataset The dataset this formula works on.
     */
    public InformationGainFormula(Iterable<Instance> dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        this.dataset = CollectionHelper.newArrayList(dataset);
        this.classOccurrences = countClassOccurrences();
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
        return entropy(dataset.size(), classOccurrences) - conditionalEntropy(featureName);
    }

    /**
     * <p>
     * Counts how often each target class occurs in the dataset.
     * </p>
     * 
     * @return A mapping from the name of the target class to a counter of how often it occurs.
     */
    private Bag<String> countClassOccurrences() {
        Bag<String> absoluteOccurrences = Bag.create();
        for (Instance dataItem : dataset) {
            absoluteOccurrences.add(dataItem.getCategory());
        }
        return absoluteOccurrences;
    }

    /**
     * <p>
     * Counts how often the values of the feature with the provided name occurs in the dataset handled by this object.
     * </p>
     * 
     * @param featureName The name of the feature to count.
     * @return A mapping from a {@link String} representation of the value to a counter of how often it occurs.
     */
    private Bag<String> countFeatureOccurrences(String featureName) {
        Bag<String> absoluteOccurrences = Bag.create();
        for (Instance dataItem : dataset) {
            Value theValue = dataItem.getVector().get(featureName);
            if (theValue != null) {
                String value = theValue.toString();
                absoluteOccurrences.add(value);
            }
        }
        return absoluteOccurrences;
    }

    /**
     * <p>
     * Counts the joint occurrences of each value of the provided feature with each target class.
     * </p>
     * 
     * @param featureName The name of the feature to calculate the joint occurrences for.
     * @return A mapping from a pair of target class and feature value to the counter of their joint occurrences.
     */
    private Bag<Pair<String, String>> countJointOccurrences(String featureName) {
        Bag<Pair<String, String>> jointAbsoluteOccurrences = Bag.create();
        for (Instance dataItem : dataset) {
            Value theValue = dataItem.getVector().get(featureName);
            if (theValue != null) {
                String value = theValue.toString();
                Pair<String, String> key = Pair.of(dataItem.getCategory(), value);
                jointAbsoluteOccurrences.add(key);
            }
        }

        return jointAbsoluteOccurrences;
    }

    /**
     * <p>
     * Calculates the entropy H for a distribution X of items in a dataset of a certain size. H(X)
     * </p>
     * 
     * @param datasetSize The size of the dataset to calculate the entropy for.
     * @param absoluteDistribution The absolute occurrences of all values of the random variable X for which the entropy
     *            should be calculated.
     * @return The entropy of the provided distribution in the dataset.
     */
    private static double entropy(int datasetSize, Bag<?> absoluteDistribution) {
        double entropy = 0;
        for (Entry<?, Integer> absoluteOccurrence : absoluteDistribution.unique()) {
            double probability = (double) absoluteOccurrence.getValue() / datasetSize;
            double summand = probability * MathHelper.log2(probability);
            entropy += summand;
        }
        return -entropy;
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
    private double conditionalEntropy(String featureName) {
        Bag<Pair<String, String>> jointOccurrences = countJointOccurrences(featureName);
        Bag<String> featureOccurrences = countFeatureOccurrences(featureName);
        return entropy(dataset.size(), jointOccurrences) - entropy(dataset.size(), featureOccurrences);
    }

}
