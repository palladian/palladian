package ws.palladian.utils;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.DatasetTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;

import java.util.Objects;
import java.util.function.Predicate;

public class InstanceValueRelativizer implements DatasetTransformer {
    private final Predicate<? super String> filter;

    public InstanceValueRelativizer(Predicate<? super String> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public Instance apply(Instance input) {
        int bowSum = 0;
        for (VectorEntry<String, Value> entry : input.getVector()) {
            if (filter.test(entry.key())) {
                if (entry.value() instanceof NumericValue) {
                    bowSum += ((NumericValue) entry.value()).getDouble();
                } else {
                    throw new IllegalArgumentException(
                            "Value " + entry.key() + " is not of type " + NumericValue.class + ", but " + entry.value().getClass() + ", cannot relativize.");
                }
            }
        }
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : input.getVector()) {
            if (filter.test(entry.key())) {
                double frequency = 0;
                if (bowSum > 0) {
                    frequency = ((NumericValue) entry.value()).getDouble() / bowSum;
                }
                builder.set(entry.key(), frequency);
            } else {
                builder.set(entry.key(), entry.value());
            }
        }
        return builder.create(input.getCategory());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (" + filter + ")";
    }

    @Override
    public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
        return featureInformation;
    }

}
