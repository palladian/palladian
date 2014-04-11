package ws.palladian.classification.utils;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.FeatureVectorBuilder;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * Abstract {@link Normalization} functionality.
 * </p>
 * 
 * @author pk
 */
abstract class AbstractNormalization implements Normalization {

    @Override
    public FeatureVector normalize(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        FeatureVectorBuilder builder = new FeatureVectorBuilder();
        for (VectorEntry<String, Value> entry : featureVector) {
            String name = entry.key();
            Value value = entry.value();
            if (value instanceof NumericValue) {
                double normalizedValue = normalize(name, ((NumericValue)value).getDouble());
                builder.set(name, normalizedValue);
            } else {
                builder.set(name, value);
            }
        }
        return builder.create();
    }

}
