package ws.palladian.core.value;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.io.ValueParser;

public final class ImmutableStringValue extends AbstractValue implements NominalValue {
	
	public static final ValueParser PARSER = new ValueParser() {
		@Override
		public Value parse(String input) {
			return new ImmutableStringValue(input);
		}

		@Override
		public boolean canParse(String input) {
			return true;
		}
	};

    private final String stringValue;

    public ImmutableStringValue(String value) {
        Validate.notNull(value, "value must not be null");
        this.stringValue = value;
    }

    @Override
    public String getString() {
        return stringValue;
    }

    @Override
    public String toString() {
        return stringValue;
    }

    @Override
    public int hashCode() {
        return stringValue.hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableStringValue other = (ImmutableStringValue)value;
        return stringValue.equals(other.stringValue);
    }

}
