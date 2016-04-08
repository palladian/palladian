package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableBooleanValue extends AbstractValue implements BooleanValue {
	
	public static final ValueParser PARSER = new AbstractValueParser(ImmutableBooleanValue.class) {

		@Override
		public Value parse(String input) throws ValueParserException {
			if ("true".equals(input)) {
				return TRUE;
			} else if ("false".equals(input)) {
				return FALSE;
			}
			throw new ValueParserException();
		}
		
	};

    private final boolean booleanValue;

    public static final ImmutableBooleanValue TRUE = new ImmutableBooleanValue(true);

    public static final ImmutableBooleanValue FALSE = new ImmutableBooleanValue(false);

    public static final ImmutableBooleanValue create(boolean value) {
        return value ? TRUE : FALSE;
    }

    private ImmutableBooleanValue(boolean value) {
        this.booleanValue = value;
    }

    @Override
    public boolean getBoolean() {
        return booleanValue;
    }

    @Override
    public String getString() {
        return String.valueOf(booleanValue);
    }

    @Override
    public String toString() {
        return String.valueOf(booleanValue);
    }

    @Override
    public int hashCode() {
        return Boolean.valueOf(booleanValue).hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableBooleanValue other = (ImmutableBooleanValue)value;
        return this.booleanValue == other.booleanValue;
    }

}
