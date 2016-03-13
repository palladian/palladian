package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableLongValue extends AbstractValue implements LongValue {
	
	public static final ValueParser PARSER = new AbstractValueParser() {
		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableLongValue(Long.parseLong(input));
			} catch (NumberFormatException e) {
				throw new ValueParserException(e);
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

	@Override
	public float getFloat() {
		return longValue;
	}

	@Override
	public int getInt() {
		return (int) longValue;
	}

	@Override
	public Number getNumber() {
		return longValue;
	}

}
