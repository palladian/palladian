package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public class ImmutableIntegerValue extends AbstractValue implements IntegerValue {

	public static final ValueParser PARSER = new AbstractValueParser() {

		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableIntegerValue(Integer.parseInt(input));
			} catch (NumberFormatException e) {
				throw new ValueParserException(e);
			}
		}

	};

	private final int integerValue;

	public ImmutableIntegerValue(int integerValue) {
		this.integerValue = integerValue;
	}

	@Override
	public double getDouble() {
		return integerValue;
	}

	@Override
	public long getLong() {
		return integerValue;
	}

	@Override
	public float getFloat() {
		return integerValue;
	}

	@Override
	public int getInt() {
		return integerValue;
	}

	@Override
	public int hashCode() {
		return Integer.valueOf(integerValue).hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableIntegerValue other = (ImmutableIntegerValue) value;
		return this.integerValue == other.integerValue;
	}

	@Override
	public String toString() {
		return String.valueOf(integerValue);
	}

	@Override
	public Number getNumber() {
		return integerValue;
	}

}
