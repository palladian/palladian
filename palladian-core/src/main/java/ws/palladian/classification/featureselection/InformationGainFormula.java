/**
 * Created on 07.08.2013 15:49:11
 */
package ws.palladian.classification.featureselection;

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
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public final class InformationGainFormula {

    public double calculateGain(List<Trainable> dataset, String featureName) {
        Map<String, Double> classOccurrences = countClassOccurrences(dataset);

        return entropy(dataset.size(), classOccurrences.values()) - conditionalEntropy(dataset, featureName);
    }

    private Map<String, Double> countClassOccurrences(List<Trainable> dataset) {
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

    private Map<String, Double> countFeatureOccurrences(List<Trainable> dataset, String featureName) {
        Map<String, Double> absoluteOccurrences = new HashMap<String, Double>();
        for (Trainable dataItem : dataset) {
            Feature<?> feature = dataItem.getFeatureVector().get(featureName);
            String value = null;
            if (feature == null) { // sparse features may not be available
                value = featureName + "Missing";
            } else {
                value = feature.getValue().toString();
            }
            Double absoluteOccurrence = absoluteOccurrences.get(value);
            if (absoluteOccurrence == null) {
                absoluteOccurrence = .0;
            }
            absoluteOccurrence += 1.0;
            absoluteOccurrences.put(value, absoluteOccurrence);
        }

        return absoluteOccurrences;
    }

    private Map<Pair<String, String>, Double> countJointOccurrences(List<Trainable> dataset, String featureName) {
        Map<Pair<String, String>, Double> jointAbsoluteOccurrences = new HashMap<Pair<String, String>, Double>();
        for (Trainable dataItem : dataset) {
            Feature<?> feature = dataItem.getFeatureVector().get(featureName);
            String value = null;
            if (feature == null) { // sparse features might not be available
                value = featureName + "Missing";
            } else {
                value = feature.getValue().toString();
            }
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
     * 
     * </p>
     * 
     * @param dataset
     * @param absoluteOccurrences
     * @return
     */
    private double entropy(double datasetSize, Collection<Double> absoluteOccurrences) {
        double entropy = .0d;
        for (Double absoluteOccurrence : absoluteOccurrences) {
            Double probability = absoluteOccurrence / datasetSize;
            double summand = probability * ld(probability);
            entropy += summand;
        }
        return -entropy;
    }

    private double conditionalEntropy(List<Trainable> dataset, String featureName) {
        Map<Pair<String, String>, Double> jointOccurrences = countJointOccurrences(dataset, featureName);
        Map<String, Double> featureOccurrences = countFeatureOccurrences(dataset, featureName);
        return entropy(dataset.size(), jointOccurrences.values())
                - entropy(dataset.size(), featureOccurrences.values());
    }

    /**
     * <p>
     * Calculates the base 2 logarithm for the provided argument.
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
     * 
     * </p>
     * 
     * @param dataset
     * @param listFeature
     * @return
     */
    public Map<Feature<?>, Double> calculateGains(List<Trainable> dataset, ListFeature<Feature<?>> listFeature) {
        Map<Feature<?>, Double> ret = CollectionHelper.newHashMap();

        Map<String, Double> classOccurrences = countClassOccurrences(dataset);
        double classEntropy = entropy(dataset.size(), classOccurrences.values());

        Map<Feature<?>, Double> absoluteOccurrences = CollectionHelper.newHashMap();
        Map<Pair<Feature<?>, String>, Double> absoluteJointOccurrences = CollectionHelper.newHashMap();

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

        for (Entry<Feature<?>, Double> absoluteOccurrence : absoluteOccurrences.entrySet()) {
            List<Double> occurrences = CollectionHelper.newArrayList();
            double occurrence = absoluteOccurrence.getValue();
            double nonOccurrence = dataset.size() - absoluteOccurrence.getValue();

            // List<Double> jointOccurrences = CollectionHelper.newArrayList();
            // for (Entry<Pair<Feature<?>, String>, Double> jointOccurrence : absoluteJointOccurrences.entrySet()) {
            // if (jointOccurrence.getKey().getKey() == absoluteOccurrence.getKey()) {
            // jointOccurrences.add(jointOccurrence.getValue());
            // double classOccurrence = classOccurrences.get(jointOccurrence.getKey().getRight());
            // jointOccurrences.add(classOccurrence - jointOccurrence.getValue());
            // }
            // }

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
            // for (Entry<Pair<Feature<?>, String>, Double> jointOccurrence : absoluteJointOccurrences.entrySet()) {
            // if (jointOccurrence.getKey().getKey() == absoluteOccurrence.getKey()) {
            // jointOccurrences.add(jointOccurrence.getValue());
            // double classOccurrence = classOccurrences.get(jointOccurrence.getKey().getRight());
            // jointOccurrences.add(classOccurrence - jointOccurrence.getValue());
            // }
            // }

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