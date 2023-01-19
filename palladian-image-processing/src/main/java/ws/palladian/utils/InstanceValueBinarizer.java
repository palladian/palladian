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

/**
 * Create binary values for numeric values; values greater zero are mapped to
 * true, values equal or smaller than zero are mapped to false.
 *
 * @author pk
 */
public class InstanceValueBinarizer implements DatasetTransformer {
    private final Predicate<? super String> filter;

    /**
     * @param filter Filter for the numeric values to map.
     */
    public InstanceValueBinarizer(Predicate<? super String> filter) {
        this.filter = Objects.requireNonNull(filter);
    }

    @Override
    public Instance apply(Instance input) {
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : input.getVector()) {
            if (filter.test(entry.key())) {
                if (entry.value() instanceof NumericValue) {
                    boolean binary = ((NumericValue) entry.value()).getDouble() > 0;
                    builder.set(entry.key(), binary);
                } else {
                    throw new IllegalArgumentException(
                            "Value " + entry.key() + " is not of type " + NumericValue.class + ", but " + entry.value().getClass() + ", cannot binarize.");
                }
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
