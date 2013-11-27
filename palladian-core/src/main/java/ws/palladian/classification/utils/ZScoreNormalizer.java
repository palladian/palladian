package ws.palladian.classification.utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.Stats;
import ws.palladian.processing.Classifiable;
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
public final class ZScoreNormalizer implements Normalizer {

    private static final class ZScoreNormalization extends AbstractNormalization implements Serializable {

        private static final long serialVersionUID = 1L;

        private final Map<String, Double> standardDeviations;

        private final Map<String, Double> means;

        public ZScoreNormalization(Map<String, Double> standardDeviations, Map<String, Double> means) {
            this.standardDeviations = standardDeviations;
            this.means = means;
        }

        @Override
        public NumericFeature normalize(NumericFeature numericFeature) {
            String featureName = numericFeature.getName();
            Double standardDeviation = standardDeviations.get(featureName);
            Double mean = means.get(featureName);
            if (standardDeviation == null || mean == null) {
                throw new IllegalArgumentException("No normalization information for \"" + featureName + "\".");
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
            toStringBuilder.append("MinMaxNormalization:\n");
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
    public Normalization calculate(Iterable<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");

        Map<String, Stats> statsMap = LazyMap.create(Stats.FACTORY);

        for (Classifiable instance : instances) {
            Collection<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);
            for (NumericFeature feature : numericFeatures) {
                statsMap.get(feature.getName()).add(feature.getValue());
            }
        }

        Map<String, Double> standardDeviations = CollectionHelper.newHashMap();
        Map<String, Double> means = CollectionHelper.newHashMap();

        for (String featureName : statsMap.keySet()) {
            standardDeviations.put(featureName, statsMap.get(featureName).getStandardDeviation());
            means.put(featureName, statsMap.get(featureName).getMean());
        }

        return new ZScoreNormalization(standardDeviations, means);
    }

}
