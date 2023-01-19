package ws.palladian.core.value;

import ws.palladian.core.value.io.AbstractValueParser;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.core.value.io.ValueParserException;

public class ImmutableIntegerValue extends AbstractValue implements IntegerValue {

    public static final ValueParser PARSER = new AbstractValueParser(ImmutableIntegerValue.class) {

        @Override
        public Value parse(String input) throws ValueParserException {
            try {
                return ImmutableIntegerValue.valueOf(Integer.parseInt(input));
            } catch (NumberFormatException e) {
                throw new ValueParserException(e);
            }
        }

    };

    private static final class ValueCache {
        private static final ImmutableIntegerValue[] CACHE;
        private static final int LOW = -128;
        private static final int HIGH = 127;

        static {
            CACHE = new ImmutableIntegerValue[HIGH - LOW + 1];
            int j = LOW;
            for (int i = 0; i < CACHE.length; i++) {
                CACHE[i] = new ImmutableIntegerValue(j++);
            }
        }
    }

    private final int integerValue;

    /** @deprecated Use {@link #valueOf(int)} instead, because it provides cached values. */
    @Deprecated
    public ImmutableIntegerValue(int integerValue) {
        this.integerValue = integerValue;
    }

    public static ImmutableIntegerValue valueOf(int integerValue) {
        if (integerValue >= ValueCache.LOW && integerValue <= ValueCache.HIGH) {
            return ValueCache.CACHE[integerValue + -ValueCache.LOW];
        }
        return new ImmutableIntegerValue(integerValue);
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
        return integerValue == other.integerValue;
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
