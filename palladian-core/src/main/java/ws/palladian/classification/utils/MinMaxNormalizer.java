package ws.palladian.classification.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.Stats;

/**
 * <p>
 * This class stores minimum and maximum values for a list of numeric features. It can be used to perform a Min-Max
 * normalization
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class MinMaxNormalizer extends AbstractStatsNormalizer {

    private static final class MinMaxNormalization extends AbstractNormalization {

        /** The logger for this class. */
        private static final Logger LOGGER = LoggerFactory.getLogger(MinMaxNormalizer.MinMaxNormalization.class);

        private static final long serialVersionUID = 7227377881428315427L;

        /** Hold the max value of each feature <featureIndex, maxValue> */
        private final Map<String, Double> maxValues = CollectionHelper.newHashMap();

        /** Hold the min value of each feature <featureName, minValue> */
        private final Map<String, Double> minValues = CollectionHelper.newHashMap();

        MinMaxNormalization(Map<String, Stats> statsMap) {
            for (String featureName : statsMap.keySet()) {
                maxValues.put(featureName, statsMap.get(featureName).getMax());
                minValues.put(featureName, statsMap.get(featureName).getMin());
            }
        }

        @Override
        public double normalize(String name, double value) {
            Validate.notNull(name, "name must not be null");

            Double min = minValues.get(name);
            Double max = maxValues.get(name);
            if (min == null || max == null) {
                LOGGER.debug("No normalization information for \"{}\".", name);
                return value;
            }

            double diff = max - min;
            return diff != 0 ? (value - min) / diff : value - min;
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

    @Override
    protected Normalization create(Map<String, Stats> statsMap) {
        return new MinMaxNormalization(statsMap);
    }

}
