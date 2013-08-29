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

        return entropy(dataset, classOccurrences.values()) - conditionalEntropy(dataset, featureName);
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
            Double absoluteOccurrence = absoluteOccurrences.get(feature.getValue().toString());
            if (absoluteOccurrence == null) {
                absoluteOccurrence = .0;
            }
            absoluteOccurrence += 1.0;
            absoluteOccurrences.put(feature.getValue().toString(), absoluteOccurrence);
        }

        return absoluteOccurrences;
    }

    private Map<Pair<String, String>, Double> countJointOccurrences(List<Trainable> dataset, String featureName) {
        Map<Pair<String, String>, Double> jointAbsoluteOccurrences = new HashMap<Pair<String, String>, Double>();
        for (Trainable dataItem : dataset) {
            Feature<?> feature = dataItem.getFeatureVector().get(featureName);
            Pair<String, String> key = new ImmutablePair<String, String>(dataItem.getTargetClass(), feature.getValue()
                    .toString());

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
    private double entropy(List<Trainable> dataset, Collection<Double> absoluteOccurrences) {
        double jointEntropy = .0d;
        for (Double jointAbsoluteOccurrence : absoluteOccurrences) {
            Double probability = jointAbsoluteOccurrence / dataset.size();
            jointEntropy += (probability * ld(probability));
        }
        return -jointEntropy;
    }

    private double conditionalEntropy(List<Trainable> dataset, String featureName) {
        Map<Pair<String, String>, Double> jointOccurrences = countJointOccurrences(dataset, featureName);
        Map<String, Double> featureOccurrences = countFeatureOccurrences(dataset, featureName);
        return entropy(dataset, jointOccurrences.values()) - entropy(dataset, featureOccurrences.values());
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
        return Math.log(arg) / Math.log(2);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param dataset
     * @param featureName
     * @return
     */
    public Map<String, Double> calculateGains(List<Trainable> dataset, String featureName) {
        Map<String, Double> ret = CollectionHelper.newHashMap();

        double classEntropy = entropy(dataset, countClassOccurrences(dataset).values());

        Map<String, Double> absoluteOccurrences = CollectionHelper.newHashMap();
        Map<Pair<String, String>, Double> absoluteJointOccurrences = CollectionHelper.newHashMap();

        for (Trainable dataItem : dataset) {

            for (Feature<?> feature : ((ListFeature<Feature<?>>)dataItem.getFeatureVector().get(featureName))) {

                String value = feature.getValue().toString();
                Double counter = absoluteOccurrences.get(value);
                if (counter == null) {
                    counter = .0;
                }
                counter += 1.0;
                absoluteOccurrences.put(value, counter);

                Pair<String, String> jointOccurrencesKey = new ImmutablePair<String, String>(value,
                        dataItem.getTargetClass());
                Double jointCounter = absoluteJointOccurrences.get(jointOccurrencesKey);
                if (jointCounter == null) {
                    jointCounter = .0;
                }
                jointCounter += 1.0;
                absoluteJointOccurrences.put(jointOccurrencesKey, jointCounter);
            }
        }

        for (Entry<String, Double> absoluteOccurrence : absoluteOccurrences.entrySet()) {
            List<Double> occurrences = CollectionHelper.newArrayList();
            occurrences.add(absoluteOccurrence.getValue());
            occurrences.add(dataset.size() - absoluteOccurrence.getValue());

            List<Double> jointOccurrences = CollectionHelper.newArrayList();
            for(Entry<Pair<String, String>, Double> jointOccurrence:absoluteJointOccurrences.entrySet()) {
                if(jointOccurrence.getKey().getKey()==absoluteOccurrence.getKey()) {
                    jointOccurrences.add(jointOccurrence.getValue());
                    jointOccurrences.add(dataset.size()-jointOccurrence.getValue());
                }
            }
            
            double gain = classEntropy - entropy(dataset, jointOccurrences) - entropy(dataset, occurrences);
            ret.put(absoluteOccurrence.getKey(), gain);
        }

        return ret;
    }
}