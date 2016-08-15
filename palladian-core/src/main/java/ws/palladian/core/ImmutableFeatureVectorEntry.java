package ws.palladian.core;

import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.Vector;

public final class ImmutableFeatureVectorEntry implements Vector.VectorEntry<String, Value> {
	private final String key;
	private final Value value;

	public ImmutableFeatureVectorEntry(String key, Value value) {
		this.key = key;
		this.value = value;
	}

	@Override
	public String key() {
		return key;
	}

	@Override
	public Value value() {
		return value;
	}

	@Override
	public String toString() {
		return key() + "=" + value();
	}

	// TODO implement hashCode + equals

}