package ws.palladian.core.value;

import ws.palladian.core.value.io.ValueParser;

public final class ValueDefinitions {

    private ValueDefinitions() {
        // not be instantiated
    }

    public static ValueParser doubleValue() {
        return ImmutableDoubleValue.PARSER;
    }

    public static ValueParser floatValue() {
        return ImmutableFloatValue.PARSER;
    }

    public static ValueParser longValue() {
        return ImmutableLongValue.PARSER;
    }

    public static ValueParser integerValue() {
        return ImmutableIntegerValue.PARSER;
    }

    public static ValueParser stringValue() {
        return ImmutableStringValue.PARSER;
    }

    public static ValueParser booleanValue() {
        return ImmutableBooleanValue.PARSER;
    }

}
