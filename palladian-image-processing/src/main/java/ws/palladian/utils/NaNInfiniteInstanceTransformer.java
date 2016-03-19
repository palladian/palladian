package ws.palladian.utils;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Function;

public enum NaNInfiniteInstanceTransformer implements Function<Instance,Instance> {
	TRANSFORMER;

	@Override
	public Instance compute(Instance input) {
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

}
