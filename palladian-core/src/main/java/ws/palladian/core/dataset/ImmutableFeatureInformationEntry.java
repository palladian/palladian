package ws.palladian.core.dataset;

import org.apache.commons.lang.Validate;

import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.value.Value;

public final class ImmutableFeatureInformationEntry implements FeatureInformationEntry {
	private final String name;
	private final Class<? extends Value> type;

	public ImmutableFeatureInformationEntry(String name, Class<? extends Value> type) {
		Validate.notNull(name, "name must not be null");
		Validate.notNull(type, "type must not be null");
		this.name = name;
		this.type = type;
	}

	@Override
	public Class<? extends Value> getType() {
		return type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName() + ":" + getType().getSimpleName();
	}

	@Override
	public boolean isCompatible(Class<? extends Value> type) {
		Validate.notNull(type, "type must not be null");
		return type.isAssignableFrom(getType());
	}

	@Override
	public int hashCode() {
		return name.hashCode() * 31 + type.getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		ImmutableFeatureInformationEntry other = (ImmutableFeatureInformationEntry) obj;
		return name.equals(other.name) && type.getName().equals(other.type.getName());
	}
}