package ws.palladian.classification.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
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
public class MinMaxNormalizer implements Normalizer {

    private static final class MinMaxNormalization extends AbstractNormalization {
        
        /** The logger for this class. */
        private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxNormalizer.MinMaxNormalization.class);

        private static final long serialVersionUID = 7227377881428315427L;

        /** Hold the max value of each feature <featureIndex, maxValue> */
        private final Map<String, Double> maxValues;

        /** Hold the min value of each feature <featureName, minValue> */
        private final Map<String, Double> minValues;

        MinMaxNormalization(Map<String, Double> minValues, Map<String, Double> maxValues) {
            this.maxValues = maxValues;
            this.minValues = minValues;
        }

        /*
         * (non-Javadoc)
         * @see
         * ws.palladian.classification.utils.Normalization#normalize(ws.palladian.processing.features.NumericFeature)
         */
        @Override
        public NumericFeature normalize(NumericFeature numericFeature) {
            Validate.notNull(numericFeature, "numericFeature must not be null");
            String featureName = numericFeature.getName();
            double featureValue = numericFeature.getValue();

            Double min = minValues.get(featureName);
            Double max = maxValues.get(featureName);
            if (min == null || max == null) {
//                throw new IllegalArgumentException("No normalization information for \"" + featureName
//                        + "\" available.");
                LOGGER.debug("No normalization information for \"{}\".", featureName);
                return numericFeature;
            }

            double diff = max - min;
            double normalizedValue = diff != 0 ? (featureValue - min) / diff : featureValue - min;
            return new NumericFeature(featureName, normalizedValue);
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
    @Override
    public Normalization calculate(Iterable<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");

        Map<String, Double> minValues = CollectionHelper.newHashMap();
        Map<String, Double> maxValues = CollectionHelper.newHashMap();

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

        return new MinMaxNormalization(minValues, maxValues);
    }

}
