package ws.palladian.core.dataset;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.value.Value;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;

public class FeatureInformationBuilder implements Factory<FeatureInformation> {

	private final Map<String, Class<? extends Value>> nameValues = new LinkedHashMap<>();

	public FeatureInformationBuilder set(String name, Class<? extends Value> valueType) {
		nameValues.put(name, valueType);
		return this;
	}

	public FeatureInformationBuilder add(FeatureInformation other) {
		for (FeatureInformationEntry entry : other) {
			nameValues.put(entry.getName(), entry.getType());
		}
		return this;
	}

	public FeatureInformationBuilder remove(String name) {
		nameValues.remove(name);
		return this;
	}
	
	public FeatureInformationBuilder filter(Filter<? super String> filter) {
		Iterator<Entry<String, Class<? extends Value>>> iterator = nameValues.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, Class<? extends Value>> current = iterator.next();
			if (!filter.accept(current.getKey())) {
				iterator.remove();
			}
		}
		return this;
	}

	@Override
	public FeatureInformation create() {
		return new ImmutableFeatureInformation(new LinkedHashMap<>(nameValues));
	}

}
