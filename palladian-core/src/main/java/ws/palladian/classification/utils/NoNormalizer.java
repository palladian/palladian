package ws.palladian.classification.utils;

import ws.palladian.core.FeatureVector;

/**
 * <p>
 * No-operation normalization. The data is not modified. Use this, when you do not want to perform normalization, but a
 * classifier requires a {@link Normalizer} as parameter.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class NoNormalizer implements Normalizer {

    private static final Normalization NOP = new AbstractNormalization() {

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
        return NOP;
    }

}
