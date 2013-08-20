/**
 * Created on 07.08.2013 15:49:11
 */
package ws.palladian.classification.featureselection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.processing.Classified;
import ws.palladian.processing.Trainable;
import ws.palladian.processing.features.Feature;

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
        return entropy(dataset) - conditionalEntropy(dataset, featureName);
    }

    private double entropy(List<Trainable> dataset) {
        Map<String, Double> absoluteOccurrences = new HashMap<String, Double>();
        for (Classified dataItem : dataset) {
            Double absoluteOccurrence = absoluteOccurrences.get(dataItem.getTargetClass());
            if (absoluteOccurrence == null) {
                absoluteOccurrence = .0;
            }
            absoluteOccurrence += 1.0;
            absoluteOccurrences.put(dataItem.getTargetClass(), absoluteOccurrence);
        }

        double entropy = .0;
        for (Entry<String, Double> absoluteOccurrence : absoluteOccurrences.entrySet()) {
            Double probability = absoluteOccurrence.getValue() / dataset.size();
            entropy += (probability * ld(probability));
        }
        return -entropy;
    }

    private double entropy(List<Trainable> dataset, String featureName) {
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

        // TODO is always the same --> move to one method.
        double entropy = .0;
        for (Entry<String, Double> absoluteOccurrence : absoluteOccurrences.entrySet()) {
            Double probability = absoluteOccurrence.getValue() / dataset.size();
            entropy += (probability * ld(probability));
        }
        return -entropy;
    }

    private double jointEntropy(List<Trainable> dataset, String featureName) {
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

        double jointEntropy = .0d;
        for (Entry<Pair<String, String>, Double> jointAbsoluteOccurrence : jointAbsoluteOccurrences.entrySet()) {
            Double probability = jointAbsoluteOccurrence.getValue() / dataset.size();
            jointEntropy += (probability * ld(probability));
        }
        return -jointEntropy;
    }

    private double conditionalEntropy(List<Trainable> dataset, String featureName) {
        return jointEntropy(dataset, featureName) - entropy(dataset, featureName);
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
}
