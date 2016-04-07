package ws.palladian.classification.utils;

import java.io.Serializable;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetTransformer;

/**
 * <p>
 * Normalization for {@link NumericFeature}s. Use a {@link Normalizer} implementation to obtain instances.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Normalization extends Serializable, DatasetTransformer {

    /**
     * <p>
     * Normalize the given value based on the normalization information.
     * </p>
     * 
     * @param name The name of the value to normalize, not <code>null</code>.
     * @param value The value to normalize.
     * @return The normalized value.
     * @throws IllegalArgumentException in case no normalization information for the given feature name is available.
     */
    double normalize(String name, double value);

    /**
     * <p>
     * Normalize a {@link FeatureVector} based in the normalization information.
     * </p>
     * 
     * @param featureVector The FeatureVector to normalize, not <code>null</code>.
     * @return A new FeatureVector with normalized values.
     */
    FeatureVector normalize(FeatureVector featureVector);
    
	/**
	 * Normalize a {@link Dataset} based on the normalization information.
	 * 
	 * @param dataset
	 *            The Dataset to normalize, not <code>null</code>.
	 * @return The normalized Dataset.
	 * @deprecated Use {@link Dataset#transform(DatasetTransformer)} instead.
	 */
    @Deprecated
	Dataset normalize(Dataset dataset);

}
