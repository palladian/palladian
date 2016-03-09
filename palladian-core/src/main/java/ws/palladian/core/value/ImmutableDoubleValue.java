package ws.palladian.core.value;

import ws.palladian.core.value.io.ValueParser;

public final class ImmutableDoubleValue extends AbstractValue implements NumericValue {
	
	public static final ValueParser PARSER = new ValueParser() {
		@Override
		public Value parse(String input) {
			return new ImmutableDoubleValue(Double.parseDouble(input));
		}

		@Override
		public boolean canParse(String input) {
			try {
				Double.parseDouble(input);
				return true;
			} catch (NumberFormatException e) {
				return false;
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

}
