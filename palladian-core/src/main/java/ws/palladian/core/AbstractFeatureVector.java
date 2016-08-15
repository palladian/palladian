package ws.palladian.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

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
	
	// TODO hashCode + equals

}
