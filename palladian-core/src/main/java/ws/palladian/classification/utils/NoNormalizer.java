package ws.palladian.classification.utils;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.dataset.Dataset;

/**
 * <p>
 * No-operation normalization. The data is not modified. Use this, when you do not want to perform normalization, but a
 * classifier requires a {@link Normalizer} as parameter.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class NoNormalizer implements Normalizer {

    public static final Normalization NO_NORMALIZATION = new AbstractNormalization() {

        private static final long serialVersionUID = 1L;

        @Override
        public double normalize(String name, double value) {
            return value;
        }

        public String toString() {
            return "<no normalization>";
        }

    };

    @Override
    public Normalization calculate(Iterable<? extends FeatureVector> featureVectors) {
        return NO_NORMALIZATION;
    }

	@Override
	public Normalization calculate(Dataset dataset) {
		return NO_NORMALIZATION;
	}

}
