package ws.palladian.core.featurevector;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import ws.palladian.core.AbstractFeatureVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.Value;

/**
 * {@link FeatureVector} following a flyweight pattern; instead of storing a fat
 * hash map for each feature vector, this implementation stores the
 * {@link Value} objects in a fixed-size array and delegates to a shared
 * {@link FlyweightVectorSchema} for lookup.
 * 
 * @author pk
 */
final class FlyweightFeatureVector extends AbstractFeatureVector {
	private final FlyweightVectorSchema schema;
	private final Value[] values;

	FlyweightFeatureVector(FlyweightVectorSchema schema, Value[] values) {
		this.schema = schema;
		this.values = values;
	}

	@Override
	public Value get(String k) {
		return schema.get(k, values);
	}

	@Override
	public int size() {
		return schema.size();
	}

	@Override
	public Set<String> keys() {
		return schema.keys();
	}

	@Override
	public Collection<Value> values() {
		return Arrays.asList(values);
	}

	@Override
	public Iterator<VectorEntry<String, Value>> iterator() {
		return schema.iterator(values);
	}
	

}
