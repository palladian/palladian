package ws.palladian.classification.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.Stats;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Normalization for {@link NumericFeature}s using Z-Score (standard deviation and mean):
 * <code>normalizedValue = (value - mean) / standardDeviation</code>.
 * </p>
 * 
 * @see <a href="http://www.utdallas.edu/~herve/abdi-Normalizing2010-pretty.pdf">Normalizing Data; Hervé Abdi</a>
 * @author pk
 */
public final class ZScoreNormalizer extends AbstractStatsNormalizer {

    private static final class ZScoreNormalization extends AbstractNormalization {
        
        /** The logger for this class. */
        private static final Logger LOGGER = LoggerFactory.getLogger(ZScoreNormalizer.ZScoreNormalization.class);

        private static final long serialVersionUID = 1L;

        private final Map<String, Double> standardDeviations = CollectionHelper.newHashMap();

        private final Map<String, Double> means = CollectionHelper.newHashMap();

        ZScoreNormalization(Map<String, Stats> statsMap) {
            for (String featureName : statsMap.keySet()) {
                standardDeviations.put(featureName, statsMap.get(featureName).getStandardDeviation());
                means.put(featureName, statsMap.get(featureName).getMean());
            }
        }

        @Override
        public NumericFeature normalize(NumericFeature numericFeature) {
            String featureName = numericFeature.getName();
            Double standardDeviation = standardDeviations.get(featureName);
            Double mean = means.get(featureName);
            if (standardDeviation == null || mean == null) {
                // throw new IllegalArgumentException("No normalization information for \"" + featureName + "\".");
                LOGGER.warn("No normalization information for \"{}\".", featureName);
                return numericFeature;
            }
            double normalizedValue = numericFeature.getValue() - mean;
            if (standardDeviation != 0) {
                normalizedValue /= standardDeviation;
            }
            return new NumericFeature(featureName, normalizedValue);
        }

        @Override
        public String toString() {
            StringBuilder toStringBuilder = new StringBuilder();
            toStringBuilder.append("ZScoreNormalization:\n");
            toStringBuilder.append("featureName: stdDev; mean\n");
            List<String> names = new ArrayList<String>(standardDeviations.keySet());
            Collections.sort(names);
            for (int i = 0; i < names.size(); i++) {
                if (i > 0) {
                    toStringBuilder.append('\n');
                }
                String name = names.get(i);
                toStringBuilder.append(name).append(": ");
                toStringBuilder.append(standardDeviations.get(name)).append("; ");
                toStringBuilder.append(means.get(name));
            }
            return toStringBuilder.toString();
        }

    }

    @Override
    protected Normalization create(Map<String, Stats> statsMap) {
        return new ZScoreNormalization(statsMap);
    }

}
