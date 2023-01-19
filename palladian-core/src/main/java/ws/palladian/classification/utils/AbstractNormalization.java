package ws.palladian.classification.utils;

import org.apache.commons.lang3.Validate;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * Abstract {@link Normalization} functionality.
 * </p>
 *
 * @author Philipp Katz
 */
public abstract class AbstractNormalization extends AbstractDatasetFeatureVectorTransformer implements Normalization {

    @Override
    public FeatureVector normalize(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : featureVector) {
            String name = entry.key();
            Value value = entry.value();
            if (value instanceof NumericValue) {
                double normalizedValue = normalize(name, ((NumericValue) value).getDouble());
                builder.set(name, normalizedValue);
            } else {
                builder.set(name, value);
            }
        }
        return builder.create();
    }

    @Override
    public Dataset normalize(final Dataset dataset) {
        Validate.notNull(dataset, "dataset must not be null");
        return dataset.transform(this);
    }

    @Override
    public FeatureVector apply(FeatureVector featureVector) {
        return normalize(featureVector);
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        return featureInformation;
    }

}
