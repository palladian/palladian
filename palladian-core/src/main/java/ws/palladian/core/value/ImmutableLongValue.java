package ws.palladian.core.value;

import ws.palladian.core.value.io.ValueParser;

public final class ImmutableLongValue extends AbstractValue implements NumericValue {
	
	public static final ValueParser PARSER = new ValueParser() {
		@Override
		public Value parse(String input) {
			return new ImmutableLongValue(Long.parseLong(input));
		}

		@Override
		public boolean canParse(String input) {
			try {
				Long.parseLong(input);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}
	};

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
