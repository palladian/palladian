package ws.palladian.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;

public abstract class AbstractFeatureVector implements FeatureVector {

	@Override
	public int size() {
		return keys().size();
	}
	
	// keys + values
	
	@Override
	public Set<String> keys() {
		Set<String> keys = new LinkedHashSet<>();
		for (VectorEntry<String, Value> entry : this) {
			keys.add(entry.key());
		}
		return Collections.unmodifiableSet(keys);
	}
	
	@Override
	public Collection<Value> values() {
		Collection<Value> values = new ArrayList<>();
		for (VectorEntry<String, Value> entry : this) {
			values.add(entry.value());
		}
		return Collections.unmodifiableCollection(values);
	}
	
	@Override
	public Value get(String k) {
		for (VectorEntry<String, Value> entry : this) {
			if (entry.key().equals(k)) {
				return entry.value();
			}
		}
		return NullValue.NULL;
	}
	
	// to string
	
	@Override
	public String toString() {
		StringBuilder string = new StringBuilder();
		string.append('[');
		boolean first = true;
		for (VectorEntry<String, Value> entry : this) {
			if (first) {
				first = false;
			} else {
				string.append(", ");
			}
			string.append(entry.key()).append('=').append(entry.value());
		}
		string.append(']');
		return string.toString();
	}
	
	// hashCode + equals
	
	@Override
	public int hashCode() {
		int hashCode = 1;
		for (VectorEntry<String, Value> entry : this) {
			hashCode = 31 * hashCode + (entry.key().hashCode() ^ entry.value().hashCode());
		}
		return hashCode;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		AbstractFeatureVector other = (AbstractFeatureVector) obj;
		if (size() != other.size()) {
			return false;
		}
		for (VectorEntry<String, Value> entry : this) {
			Value otherValue = other.get(entry.key());
			if (!otherValue.equals(entry.value())) {
				return false;
			}
		}
		return true;
	}

}
