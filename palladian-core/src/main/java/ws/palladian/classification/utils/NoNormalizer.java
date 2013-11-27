package ws.palladian.classification.utils;

import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.NumericFeature;

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

        @Override
        public NumericFeature normalize(NumericFeature numericFeature) {
            return numericFeature;
        }
        
        public String toString() {
            return "<no normalization>";
        };

    };

    @Override
    public Normalization calculate(Iterable<? extends Classifiable> instances) {
        return NOP;
    }

}
