package ws.palladian.utils;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Function;

import java.util.Objects;

/**
 * Create binary values for numeric values; values greater zero are mapped to
 * true, values equal or smaller than zero are mapped to false.
 * 
 * @author pk
 *
 */
public class InstanceValueBinarizer implements Function<Instance, Instance> {
	private final Filter<? super String> filter;

	/**
	 * @param filter Filter for the numeric values to map.
	 */
	public InstanceValueBinarizer(Filter<? super String> filter) {
		this.filter = Objects.requireNonNull(filter);
	}

	@Override
	public Instance compute(Instance input) {
		InstanceBuilder builder = new InstanceBuilder();
		for (VectorEntry<String, Value> entry : input.getVector()) {
			if (filter.accept(entry.key())) {
				if (entry.value() instanceof NumericValue) {
					boolean binary = ((NumericValue) entry.value()).getDouble() > 0;
					builder.set(entry.key(), binary);
				} else {
					throw new IllegalArgumentException("Value " + entry.key() + " is not of type " + NumericValue.class
							+ ", but " + entry.value().getClass() + ", cannot binarize.");
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

}
