package ws.palladian.classification.utils;

import java.util.List;

import ws.palladian.classification.Instance;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Normalization for {@link NumericFeature}s. Use a {@link Normalizer} implementation to obtain instances.
 * </p>
 * 
 * @author pk
 */
public interface Normalization {

    /**
     * <p>
     * Normalize a {@link List} of {@link Instance}s based on the normalization information. The values are modified
     * directly in place.
     * </p>
     * 
     * @param instances The List of Instances, not <code>null</code>.
     */
    void normalize(Iterable<? extends Classifiable> instances);

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
    NumericFeature normalize(NumericFeature numericFeature);

    /**
     * <p>
     * Normalize a {@link FeatureVector} based in the normalization information. The values are modified directly in
     * place.
     * </p>
     * 
     * @param featureVector The FeatureVector to normalize, not <code>null</code>.
     * @return
     */
    void normalize(Classifiable classifiable);

}
