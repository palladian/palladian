package ws.palladian.classification.utils;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.SlimStats;
import ws.palladian.helper.math.Stats;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.NumericFeature;

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
    public final Normalization calculate(Iterable<? extends Classifiable> instances) {
        Validate.notNull(instances, "instances must not be null");

        Map<String, Stats> statsMap = LazyMap.create(SlimStats.FACTORY);

        for (Classifiable instance : instances) {
            Collection<NumericFeature> numericFeatures = instance.getFeatureVector().getAll(NumericFeature.class);
            for (NumericFeature feature : numericFeatures) {
                statsMap.get(feature.getName()).add(feature.getValue());
            }
        }

        return create(statsMap);

    }

}
