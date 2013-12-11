package ws.palladian.classification.utils;

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
     * Normalize the given {@link NumericFeature} based on the normalization information. A new {@link NumericFeature}
     * with normalized value is returned.
     * </p>
     * 
     * @param numericFeature The feature to normalize, not <code>null</code>.
     * @return A normalized feature.
     * @throws IllegalArgumentException in case no normalization information for the given feature name is available.
     */
    NumericFeature normalize(NumericFeature numericFeature);

    // XXX it would be better to return a copy of the classifiable, as we might want to keep the original values, or
    // normalizing several times will cause unexpected results
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
