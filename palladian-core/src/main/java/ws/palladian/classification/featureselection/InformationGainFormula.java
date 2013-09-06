/**
 * Created on 07.08.2013 15:49:11
 */
package ws.palladian.classification.featureselection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classified;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.ListFeature;

/**
 * <p>
 * Implements the Information Gain formula as proposed by the Weka Machine Learning Framework. A description can be
 * found under <a href="http://arxiv.org/pdf/nlin/0307015v4.pdf">http://arxiv.org/pdf/nlin/0307015v4.pdf</a> on page 47.
 * </p>
 * <p>
 * This object always works on a dataset. If you need to calculate information gain for another dataset please create a
 * new formula object. The reason for this are performance issues. Some dataset specific counts need to be calculated
 * only once that way.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public final class InformationGainFormula {

    /**
     * <p>
     * The list of {@link Trainable} making up the dataset handled by this formula.
     * </p>
     */
    private final List<Trainable> dataset;
    /**
     * <p>
     * The number of occurrences of each target class as calculated using {@link #countClassOccurrences()}.
     * </p>
     */
    private final Map<String, Double> classOccurrences;

    /**
     * <p>
     * Creates a new completely initialized object of this class. The provided dataset needs to provide all the features
     * you want to calculate information gain values for. If the feature is dense it should not have missing values.
     * Sparse features can be handled by using a {@link ListFeature}.
     * </p>
     * 
     * @param dataset The dataset this formula works on.
     */
    public InformationGainFormula(Collection<Trainable> dataset) {
        this.dataset = new ArrayList<Trainable>(dataset);
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
        return entropy(dataset.size(), classOccurrences.values()) - conditionalEntropy(featureName);
    }

    /**
     * <p>
     * Counts how often each target class occurs in the dataset.
     * </p>
     * 
     * @return A mapping from the name of the target class to a counter of how often it occurs.
     */
    private Map<String, Double> countClassOccurrences() {
        Map<String, Double> absoluteOccurrences = new HashMap<String, Double>();
        for (Classified dataItem : dataset) {
            Double absoluteOccurrence = absoluteOccurrences.get(dataItem.getTargetClass());
            if (absoluteOccurrence == null) {
                absoluteOccurrence = .0;
            }
            absoluteOccurrence += 1.0;
            absoluteOccurrences.put(dataItem.getTargetClass(), absoluteOccurrence);
        }

        return absoluteOccurrences;
    }

    // TODO make the return value to Map<Object, Double>.
    /**
     * <p>
     * Counts how often the values of the feature with the provided name occurs in the dataset handled by this object.
     * </p>
     * 
     * @param featureName The name of the feature to count.
     * @return A mapping from a {@link String} representation of the value to a counter of how often it occurs.
     */
    private Map<String, Double> countFeatureOccurrences(String featureName) {
        Map<String, Double> absoluteOccurrences = new HashMap<String, Double>();
        for (Trainable dataItem : dataset) {
            Feature<?> feature = dataItem.getFeatureVector().get(featureName);
            String value = feature.getValue().toString();

            Double absoluteOccurrence = absoluteOccurrences.get(value);
            if (absoluteOccurrence == null) {
                absoluteOccurrence = .0;
            }
            absoluteOccurrence += 1.0;
            absoluteOccurrences.put(value, absoluteOccurrence);
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
    private Map<Pair<String, String>, Double> countJointOccurrences(String featureName) {
        Map<Pair<String, String>, Double> jointAbsoluteOccurrences = new HashMap<Pair<String, String>, Double>();
        for (Trainable dataItem : dataset) {
            Feature<?> feature = dataItem.getFeatureVector().get(featureName);
            String value = feature.getValue().toString();

            Pair<String, String> key = new ImmutablePair<String, String>(dataItem.getTargetClass(), value);
            Double jointAbsoluteOccurrence = jointAbsoluteOccurrences.get(key);
            if (jointAbsoluteOccurrence == null) {
                jointAbsoluteOccurrence = .0d;
            }
            jointAbsoluteOccurrence++;
            jointAbsoluteOccurrences.put(key, jointAbsoluteOccurrence);
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
    private double entropy(double datasetSize, Collection<Double> absoluteDistribution) {
        double entropy = .0d;
        for (Double absoluteOccurrence : absoluteDistribution) {
            Double probability = absoluteOccurrence / datasetSize;
            double summand = probability * ld(probability);
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
        Map<Pair<String, String>, Double> jointOccurrences = countJointOccurrences(featureName);
        Map<String, Double> featureOccurrences = countFeatureOccurrences(featureName);
        return entropy(dataset.size(), jointOccurrences.values())
                - entropy(dataset.size(), featureOccurrences.values());
    }

    /**
     * <p>
     * Calculates the base 2 logarithm for the provided argument.
     * </p>
     * <p>
     * This method handles error cases gracefully. For example ld(0.0) results in 0.0 and not in NaN. This is true for
     * all such error cases.
     * </p>
     * 
     * @param arg The argument to calculate the logarithm for.
     * @return The base 2 logarithm for the provided argument.
     */
    private double ld(double arg) {
        double ret = Math.log(arg) / Math.log(2);
        return (Double.isNaN(ret) || Double.isInfinite(ret)) ? .0 : ret;
    }

    /**
     * <p>
     * Calculates information gain values for sparse features identified by the provided {@link ListFeature}.
     * </p>
     * 
     * @param listFeature The {@link ListFeature} that should occur in all instances of the dataset and contain the
     *            sparse features.
     * @return A mapping from a feature to an information gain value.
     */
    public Map<Feature<?>, Double> calculateGains(ListFeature<Feature<?>> listFeature) {
        Map<Feature<?>, Double> ret = CollectionHelper.newHashMap();

        double classEntropy = entropy(dataset.size(), classOccurrences.values());

        Map<Feature<?>, Double> absoluteOccurrences = CollectionHelper.newHashMap();
        Map<Pair<Feature<?>, String>, Double> absoluteJointOccurrences = CollectionHelper.newHashMap();

        // count occurrences
        for (Trainable dataItem : dataset) {

            ListFeature<Feature<?>> localListFeature = dataItem.getFeatureVector().get(ListFeature.class,
                    listFeature.getName());
            Set<Feature<?>> deduplicatedFeatures = CollectionHelper.newHashSet();
            deduplicatedFeatures.addAll(localListFeature);
            for (Feature<?> feature : deduplicatedFeatures) {

                Double counter = absoluteOccurrences.get(feature);
                if (counter == null) {
                    counter = .0;
                }
                counter += 1.0;
                absoluteOccurrences.put(feature, counter);

                Pair<Feature<?>, String> jointOccurrencesKey = new ImmutablePair<Feature<?>, String>(feature,
                        dataItem.getTargetClass());
                Double jointCounter = absoluteJointOccurrences.get(jointOccurrencesKey);
                if (jointCounter == null) {
                    jointCounter = .0;
                }
                jointCounter += 1.0;
                absoluteJointOccurrences.put(jointOccurrencesKey, jointCounter);
            }
        }

        // calculate gains
        for (Entry<Feature<?>, Double> absoluteOccurrence : absoluteOccurrences.entrySet()) {
            double occurrence = absoluteOccurrence.getValue();
            double nonOccurrence = dataset.size() - absoluteOccurrence.getValue();

            List<Double> jointOccurrences = CollectionHelper.newArrayList();
            List<Double> jointNonOccurrences = CollectionHelper.newArrayList();

            for (String targetClass : classOccurrences.keySet()) {
                Double absoluteJointOccurrence = absoluteJointOccurrences.get(new ImmutablePair<Feature<?>, String>(
                        absoluteOccurrence.getKey(), targetClass));
                if (absoluteJointOccurrence == null) {
                    absoluteJointOccurrence = 0.0;
                }
                jointOccurrences.add(absoluteJointOccurrence);

                Double absoluteJointNonOccurrence = classOccurrences.get(targetClass) - absoluteJointOccurrence;
                jointNonOccurrences.add(absoluteJointNonOccurrence);
            }

            double subsetSizeWeight = occurrence / dataset.size();
            double nonSubsetSizeWeight = nonOccurrence / dataset.size();
            double subsetEntropy = entropy(occurrence, jointOccurrences);
            double nonSubsetEntropy = entropy(nonOccurrence, jointNonOccurrences);
            double attributeEntropy = subsetSizeWeight * subsetEntropy + nonSubsetSizeWeight * nonSubsetEntropy;

            double gain = classEntropy - attributeEntropy;
            ret.put(absoluteOccurrence.getKey(), gain);
        }

        return ret;
    }
}
