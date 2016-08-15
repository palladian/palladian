package ws.palladian.core;

import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;

/**
 * A feature vector, where features can be removed by applying a filter.
 * 
 * @author pk
 */
public class FilteredVector extends AbstractFeatureVector {

	private final FeatureVector original;

	private final Set<String> filteredNames;

	public FilteredVector(FeatureVector original, Set<String> filteredFeatures) {
		Objects.requireNonNull(original, "original must not be null");
		Objects.requireNonNull(filteredFeatures, "filteredFeatures must not be null");
		this.original = original;
		this.filteredNames = filteredFeatures;
	}

	public FilteredVector(FeatureVector original, Filter<? super String> filteredFeatures) {
		this(original, CollectionHelper.filterSet(original.keys(), filteredFeatures));
	}

	@Override
	public Value get(String k) {
		if (filteredNames.contains(k)) {
			return original.get(k);
		}
		return null;
	}

	@Override
	public Set<String> keys() {
		return Collections.unmodifiableSet(filteredNames);
	}

	@Override
	public Iterator<VectorEntry<String, Value>> iterator() {
		return new AbstractIterator<VectorEntry<String, Value>>() {
			final Iterator<VectorEntry<String, Value>> iterator = original.iterator();
			@Override
			protected VectorEntry<String, Value> getNext() throws Finished {
				while (iterator.hasNext()) {
					VectorEntry<String, Value> current = iterator.next();
					if (filteredNames.contains(current.key())) {
						return current;
					}
				}
				throw FINISHED;
			}
		};
	}

}
