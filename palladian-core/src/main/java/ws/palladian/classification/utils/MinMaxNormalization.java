package ws.palladian.classification.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * This class stores minimum and maximum values for a list of numeric features. It can be used to perform a Min-Max
 * normalization
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class MinMaxNormalization implements Serializable {

    private static final long serialVersionUID = 7227377881428315427L;

    /** Hold the max value of each feature <featureIndex, maxValue> */
    private final Map<String, Double> maxValues;

    /** Hold the min value of each feature <featureName, minValue> */
    private final Map<String, Double> minValues;

    /**
     * <p>
     * Calculate Min-Max normalization information over the numeric values of the given features (i.e. calculate the
     * minimum and maximum values for each feature). The {@link MinMaxNormalization} instance can then be used to
     * normalize numeric instances to an interval of [0,1].
     * </p>
     * 
     * @param instances The {@code List} of {@link Instance}s to normalize, not <code>null</code>.
     * @return A {@link MinMaxNormalization} instance carrying information to normalize {@link Instance}s based on the
     *         calculated normalization information.
     */
    public MinMaxNormalization(Iterable<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");

        minValues = CollectionHelper.newHashMap();
        maxValues = CollectionHelper.newHashMap();

        // find the min and max values
        for (Classifiable instance : instances) {

            Collection<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);

            for (Feature<Double> feature : numericFeatures) {

                String featureName = feature.getName();
                double featureValue = feature.getValue();

                // check min value
                if (minValues.get(featureName) != null) {
                    double currentMin = minValues.get(featureName);
                    if (currentMin > featureValue) {
                        minValues.put(featureName, featureValue);
                    }
                } else {
                    minValues.put(featureName, featureValue);
                }

                // check max value
                if (maxValues.get(featureName) != null) {
                    double currentMax = maxValues.get(featureName);
                    if (currentMax < featureValue) {
                        maxValues.put(featureName, featureValue);
                    }
                } else {
                    maxValues.put(featureName, featureValue);
                }

            }
        }
    }

    /**
     * <p>
     * Normalize a {@link List} of {@link Instance}s based on the normalization information. The values are modified
     * directly in place.
     * </p>
     * 
     * @param instances The List of Instances, not <code>null</code>.
     */
    public void normalize(List<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");
        for (Classifiable instance : instances) {
            normalize(instance);
        }
    }

    /**
     * <p>
     * Normalize the given {@link NumericFeature} based on the normalization information. A new {@link NumericFeature}
     * with normalized value is returned.
     * </p>
     * 
     * @param numericFeature The feature to normalize, not <code>null</code>.
     * @return A normalized feature.
     * @throws IllegalArgumentException in case no normalization information for the given feature name is available.
     */
    public NumericFeature normalize(NumericFeature numericFeature) {
        Validate.notNull(numericFeature, "numericFeature must not be null");
        String featureName = numericFeature.getName();
        double featureValue = numericFeature.getValue();

        Double min = minValues.get(featureName);
        Double max = maxValues.get(featureName);
        if (min == null || max == null) {
            throw new IllegalArgumentException("No normalization information for \"" + featureName + "\" available.");
        }

        double diff = max - min;
        double normalizedValue = diff != 0 ? (featureValue - min) / diff : featureValue - min;
        return new NumericFeature(featureName, normalizedValue);
    }

    /**
     * <p>
     * Normalize a {@link FeatureVector} based in the normalization information. The values are modified directly in
     * place.
     * </p>
     * 
     * @param featureVector The FeatureVector to normalize, not <code>null</code>.
     * @return
     */
    public void normalize(Classifiable classifiable) {
        Validate.notNull(classifiable, "classifiable must not be null");
        FeatureVector featureVector = classifiable.getFeatureVector();
        for (Feature<?> feature : featureVector) {
            if (feature instanceof NumericFeature) {
                NumericFeature numericFeature = (NumericFeature)feature;
                // replace value.
                featureVector.add(normalize(numericFeature));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("MinMaxNormalization:\n");
        List<String> names = new ArrayList<String>(minValues.keySet());
        Collections.sort(names);
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) {
                toStringBuilder.append('\n');
            }
            String name = names.get(i);
            toStringBuilder.append(name).append(": ");
            toStringBuilder.append(minValues.get(name)).append("; ");
            toStringBuilder.append(maxValues.get(name));
        }
        return toStringBuilder.toString();
    }

}
