package ws.palladian.utils;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.DatasetTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

public enum NaNInfiniteInstanceTransformer implements DatasetTransformer {
    TRANSFORMER;

    @Override
    public Instance apply(Instance input) {
        FeatureVector featureVector = input.getVector();
        InstanceBuilder builder = new InstanceBuilder().add(featureVector);
        for (VectorEntry<String, Value> entry : featureVector) {
            Value value = entry.value();
            if (value instanceof NumericValue) {
                double doubleValue = ((NumericValue) value).getDouble();
                if (Double.isNaN(doubleValue)) {
                    doubleValue = 0;
                }
                if (Double.isInfinite(doubleValue)) {
                    doubleValue = 0;
                }
                builder.set(entry.key(), doubleValue);
            }
        }
        return builder.create(input.getCategory());
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        return featureInformation;
    }

}
