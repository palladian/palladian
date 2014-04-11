package ws.palladian.classification.utils;

import ws.palladian.core.FeatureVector;

/**
 * <p>
 * A normalizer produces a normalization information for given feature vectors. This be used to normalize new instances.
 * </p>
 * 
 * @author pk
 */
public interface Normalizer {

    /**
     * <p>
     * Calculate normalization information for the given feature vectors. <b>Note:</b> The feature vectors are not
     * modified when calling this method. In most cases, you will want to use the generated {@link Normalization} to
     * normalize the feature vectors.
     * </p>
     * 
     * @param featureVectors The {@link FeatureVector}s for which to calculate the normalization, not <code>null</code>.
     * @return The {@link Normalization}.
     */
    Normalization calculate(Iterable<? extends FeatureVector> featureVectors);

}
