package ws.palladian.classification.utils;

import ws.palladian.processing.Classifiable;

/**
 * <p>
 * A normalizer produces a normalization information for given instances. This be used to normalize new instances.
 * </p>
 * 
 * @author pk
 */
public interface Normalizer {

    /**
     * <p>
     * Calculate normalization information for the given instances. <b>Note:</b> The instances are not modified when
     * calling this method. In most cases, you will want to use the generated {@link Normalization} to normalize the
     * instances.
     * </p>
     * 
     * @param instances The instances for which to calculate the normalization, not <code>null</code>.
     * @return The {@link Normalization}.
     */
    Normalization calculate(Iterable<? extends Classifiable> instances);

}
