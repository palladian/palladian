package ws.palladian.kaggle.redhat.dataset.transformers;

import java.util.Iterator;

import ws.palladian.core.AbstractFeatureVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.ImmutableFeatureVectorEntry;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.collection.Vector;
import ws.palladian.helper.functional.Function;

/** @deprecated {@link #get(String)} does not work as it should. */
@Deprecated
final class RenamedFeatureVector extends AbstractFeatureVector {

	private final Function<String, String> featureNameMapping;

	private final FeatureVector wrapped;

	RenamedFeatureVector(FeatureVector wrapped, Function<String, String> featureNameMapping) {
		this.featureNameMapping = featureNameMapping;
		this.wrapped = wrapped;
	}

	@Override
	public Value get(String k) {
		return wrapped.get(getRenamed(k));
	}

	@Override
	public Iterator<Vector.VectorEntry<String, Value>> iterator() {
		Iterator<VectorEntry<String, Value>> iterator = wrapped.iterator();
		return new AbstractIterator2<Vector.VectorEntry<String, Value>>() {
			@Override
			protected Vector.VectorEntry<String, Value> getNext() {
				if (iterator.hasNext()) {
					Vector.VectorEntry<String, Value> entry = iterator.next();
					return new ImmutableFeatureVectorEntry(getRenamed(entry.key()), entry.value());
				}
				return finished();
			}
		};
	}

	private String getRenamed(String original) {
		String renamed = featureNameMapping.compute(original);
		String result = renamed != null ? renamed : original;
		return result;
	}

}
