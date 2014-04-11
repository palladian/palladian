package ws.palladian.core;

final class ImmutableBooleanValue implements BooleanValue {

    private final boolean value;

    public static final ImmutableBooleanValue TRUE = new ImmutableBooleanValue(true);

    public static final ImmutableBooleanValue FALSE = new ImmutableBooleanValue(false);

    public static final ImmutableBooleanValue create(boolean value) {
        return value ? TRUE : FALSE;
    }

    private ImmutableBooleanValue(boolean value) {
        this.value = value;
    }

    @Override
    public boolean getBoolean() {
        return value;
    }

    @Override
    public String getString() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

}
