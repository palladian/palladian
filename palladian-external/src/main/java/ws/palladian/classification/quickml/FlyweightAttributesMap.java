package ws.palladian.classification.quickml;

import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickml.data.AttributesMap;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * More memory-efficient {@link AttributesMap}. Do create a new {@link HashMap}
 * for each training instance, but keep values in an array and store index/names
 * in one shared map.
 * 
 * @author pk
 */
class FlyweightAttributesMap extends AbstractMap<String, Serializable> {

	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(QuickMlLearner.class);

	public static final class Builder {
		private final Map<String, Integer> keysIndices;

		public Builder(Set<String> featureNames) {
			Map<String, Integer> keysIndices = new HashMap<>();
			int idx = 0;
			for (String featureName : featureNames) {
				keysIndices.put(featureName, idx++);
			}
			this.keysIndices = Collections.unmodifiableMap(keysIndices);
		}

		public AttributesMap create(FeatureVector featureVector) {
			Serializable[] data = new Serializable[keysIndices.size()];
			for (VectorEntry<String, Value> feature : featureVector) {
				int idx = keysIndices.get(feature.key());
				Value value = feature.value();
				if (value instanceof NominalValue) {
					data[idx] = ((NominalValue) value).getString();
				} else if (value instanceof NumericValue) {
					data[idx] = ((NumericValue) value).getDouble();
				} else {
					LOGGER.trace("Unsupported type for {}: {}", feature.key(), value.getClass().getName());
				}
			}
			return new AttributesMap(new FlyweightAttributesMap(keysIndices, data));
		}
	}

	private final Map<String, Integer> keysIndices;
	private final Serializable[] data;

	private FlyweightAttributesMap(Map<String, Integer> keysIndices, Serializable[] data) {
		this.keysIndices = keysIndices;
		this.data = data;
	}

	@Override
	public Serializable get(Object key) {
		return data[keysIndices.get(key)];
	}

	@Override
	public Set<Entry<String, Serializable>> entrySet() {
		return new AbstractSet<Entry<String, Serializable>>() {
			@Override
			public Iterator<Entry<String, Serializable>> iterator() {
				return new AbstractIterator<Entry<String, Serializable>>() {

					final Iterator<Entry<String, Integer>> keysIterator = keysIndices.entrySet().iterator();

					@Override
					protected Entry<String, Serializable> getNext() throws Finished {
						if (keysIterator.hasNext()) {
							Entry<String, Integer> current = keysIterator.next();
							return Pair.of(current.getKey(), data[current.getValue()]);
						}
						throw FINISHED;
					}
				};
			}

			@Override
			public int size() {
				return keysIndices.size();
			}
		};
	}

}
