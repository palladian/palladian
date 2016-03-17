package ws.palladian.core.featurevector;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * This class keeps shared state and logic of multiple
 * {@link FlyweightFeatureVector} instances. It handles e.g. key-based lookup,
 * get, set, and iteration for {@link Value} arrays. This class is and must be
 * immutable, once constructed.
 * 
 * @author pk
 */
public class FlyweightVectorSchema {

	private final Map<String, Integer> keys;

	public FlyweightVectorSchema(String... keys) {
		this.keys = new LinkedHashMap<>();
		for (int idx = 0; idx < keys.length; idx++) {
			this.keys.put(keys[idx], idx);
		}
	}

	public Value get(String name, Value[] values) {
		return values[keys.get(name)];
	}

	public void set(String name, Value value, Value[] values) {
		values[keys.get(name)] = value;
	}

	public int size() {
		return keys.size();
	}

	public Set<String> keys() {
		return Collections.unmodifiableSet(keys.keySet());
	}

	public Iterator<VectorEntry<String, Value>> iterator(final Value[] values) {
		return new AbstractIterator<VectorEntry<String, Value>>() {
			final Iterator<Entry<String, Integer>> keyIterator = keys.entrySet().iterator();

			@Override
			protected VectorEntry<String, Value> getNext() throws Finished {
				if (keyIterator.hasNext()) {
					final Entry<String, Integer> current = keyIterator.next();
					return new VectorEntry<String, Value>() {
						@Override
						public String key() {
							return current.getKey();
						}

						@Override
						public Value value() {
							return values[current.getValue()];
						}
					};
				}
				throw FINISHED;
			}
		};
	}

	public FlyweightVectorBuilder builder() {
		return new FlyweightVectorBuilder(this);
	}

}
