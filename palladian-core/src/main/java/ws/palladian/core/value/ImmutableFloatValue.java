package ws.palladian.core.value;

import ws.palladian.core.value.io.ValueParser;

public final class ImmutableFloatValue extends AbstractValue implements NumericValue {

	public static final ValueParser PARSER = new ValueParser() {

		@Override
		public Value parse(String input) {
			return new ImmutableFloatValue(Float.parseFloat(input));
		}

		@Override
		public boolean canParse(String input) {
			try {
				Float.parseFloat(input);
				return true;
			} catch (NumberFormatException e) {
				return false;
			}
		}

	};

	private final float value;

	public ImmutableFloatValue(float value) {
		this.value = value;
	}

	@Override
	public int hashCode() {
		return Float.valueOf(value).hashCode();
	}

	@Override
	protected boolean equalsValue(Value value) {
		ImmutableFloatValue other = (ImmutableFloatValue) value;
		return Float.floatToIntBits(this.value) == Float.floatToIntBits(other.value);
	}

	@Override
	public String toString() {
		return String.valueOf(value);
	}

	@Override
	public double getDouble() {
		return value;
	}

	@Override
	public long getLong() {
		return (long) value;
	}

}
