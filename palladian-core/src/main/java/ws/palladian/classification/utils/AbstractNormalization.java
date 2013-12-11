package ws.palladian.classification.utils;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Abstract {@link Normalization} functionality.
 * </p>
 * 
 * @author pk
 */
abstract class AbstractNormalization implements Normalization {

    @Override
    public final void normalize(Classifiable classifiable) {
        Validate.notNull(classifiable, "classifiable must not be null");
        FeatureVector featureVector = classifiable.getFeatureVector();
        for (Feature<?> feature : featureVector) {
            if (feature instanceof NumericFeature) {
                NumericFeature numericFeature = (NumericFeature)feature;
                // replace value.
                featureVector.add(normalize(numericFeature));
            }
        }
    }

}
