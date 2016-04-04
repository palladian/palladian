package ws.palladian.classification.utils;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.dataset.Dataset;

/**
 * <p>
 * A normalizer produces a normalization information for given feature vectors. This be used to normalize new instances.
 * </p>
 * 
 * @author Philipp Katz
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
     * @deprecated Use {@link #calculate(Dataset)} instead.
     */
	@Deprecated
    Normalization calculate(Iterable<? extends FeatureVector> featureVectors);
    
	/**
     * <p>
     * Calculate normalization information for the given dataset.
     * 
	 * @param dataset The dataset for which to calculate the normalization, not <code>null</code>.
	 * @return The {@link Normalization}
	 */
    Normalization calculate(Dataset dataset);

}
