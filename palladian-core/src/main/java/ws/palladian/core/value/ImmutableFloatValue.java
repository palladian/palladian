package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public final class ImmutableFloatValue extends AbstractValue implements FloatValue {

    public static final ValueParser PARSER = new AbstractValueParser(ImmutableFloatValue.class) {

        @Override
        public Value parse(String input) throws ValueParserException {
            try {
                return new ImmutableFloatValue(Float.parseFloat(input));
            } catch (NumberFormatException e) {
                throw new ValueParserException(e);
            }
        }

    };

    private final float floatValue;

    public ImmutableFloatValue(float value) {
        this.floatValue = value;
    }

    @Override
    public int hashCode() {
        return Float.valueOf(floatValue).hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableFloatValue other = (ImmutableFloatValue) value;
        return Float.floatToIntBits(this.floatValue) == Float.floatToIntBits(other.floatValue);
    }

    @Override
    public String toString() {
        return String.valueOf(floatValue);
    }

    @Override
    public double getDouble() {
        return floatValue;
    }

    @Override
    public long getLong() {
        return (long) floatValue;
    }

    @Override
    public float getFloat() {
        return floatValue;
    }

    @Override
    public int getInt() {
        return (int) floatValue;
    }

    @Override
    public Number getNumber() {
        return floatValue;
    }

}
