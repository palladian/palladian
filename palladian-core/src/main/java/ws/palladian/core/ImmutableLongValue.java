package ws.palladian.core;

import ws.palladian.core.value.AbstractValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;

public final class ImmutableLongValue extends AbstractValue implements NumericValue {

	private final long longValue;

	public ImmutableLongValue(long value) {
		this.longValue = value;
	}

	@Override
	public double getDouble() {
		return longValue;
	}

	@Override
	public long getLong() {
		return longValue;
	}

	@Override
	public int hashCode() {
		return Long.valueOf(longValue).hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableLongValue other = (ImmutableLongValue) value;
		return longValue == other.longValue;
	}

	@Override
	public String toString() {
		return String.valueOf(longValue);
	}

}
