package ws.palladian.core;

import java.util.Iterator;
import java.util.List;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CompositeIterator;

public class AppendedVector extends AbstractFeatureVector {

	private final List<FeatureVector> vectors;

	public AppendedVector(List<FeatureVector> vectors) {
		this.vectors = vectors;
	}

	@Override
	public Value get(String k) {
		for (FeatureVector vector : vectors) {
			Value value = vector.get(k);
			if (value != null && !value.isNull()) {
				return value;
			}
		}
		return NullValue.NULL;
	}

	@Override
	public Iterator<VectorEntry<String, Value>> iterator() {
		return CompositeIterator.fromIterable(vectors);
	}

}
