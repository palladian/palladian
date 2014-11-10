package ws.palladian.classification.utils;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;

/**
 * Common code for normalizers which calculate their {@link Normalization} based on {@link Stats} data.
 * 
 * @author pk
 */
abstract class AbstractStatsNormalizer implements Normalizer {

    /**
     * <p>
     * Subclasses calculate the normalization data from the given {@link Map} with stats.
     * </p>
     * 
     * @param statsMap The stats for the numeric features, key is the feature name, {@link Stats} contains statistical
     *            properties.
     * @return A {@link Normalization} for the given map.
     */
    protected abstract Normalization create(Map<String, Stats> statsMap);

    @Override
    public final Normalization calculate(Iterable<? extends FeatureVector> featureVectors) {
        Validate.notNull(featureVectors, "featureVectors must not be null");

        Map<String, Stats> statsMap = LazyMap.create(SlimStats.FACTORY);

        for (FeatureVector vector : featureVectors) {
            for (VectorEntry<String, Value> vectorEntry : vector) {
                Value value = vectorEntry.value();
                if (value instanceof NumericValue) {
                    statsMap.get(vectorEntry.key()).add(((NumericValue)value).getDouble());
                }
            }
        }

        return create(statsMap);
    }

}
