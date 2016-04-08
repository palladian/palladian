package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableDoubleValue extends AbstractValue implements DoubleValue {
	
	public static final ValueParser PARSER = new AbstractValueParser(ImmutableDoubleValue.class) {
		@Override
		public Value parse(String input) throws ValueParserException {
			try {
				return new ImmutableDoubleValue(Double.parseDouble(input));
			} catch (NumberFormatException e) {
				throw new ValueParserException(e);
			}
		}

	};

    private final double doubleValue;

    public ImmutableDoubleValue(double value) {
        this.doubleValue = value;
    }

    @Override
    public double getDouble() {
        return doubleValue;
    }

    @Override
    public String toString() {
        return String.valueOf(doubleValue);
    }

    @Override
    public int hashCode() {
        return Double.valueOf(doubleValue).hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableDoubleValue other = (ImmutableDoubleValue)value;
        return Double.doubleToLongBits(doubleValue) == Double.doubleToLongBits(other.doubleValue);
    }

	@Override
	public long getLong() {
		return (long) doubleValue;
	}

	@Override
	public float getFloat() {
		return (float) doubleValue;
	}

	@Override
	public int getInt() {
		return (int) doubleValue;
	}

	@Override
	public Number getNumber() {
		return doubleValue;
	}

}
