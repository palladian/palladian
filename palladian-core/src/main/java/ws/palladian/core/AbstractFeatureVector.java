package ws.palladian.core;

import ws.palladian.core.value.Value;

public abstract class AbstractFeatureVector implements FeatureVector {

	@Override
	public int size() {
		return keys().size();
	}
	
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

}
