package ws.palladian.core.value;

public final class ImmutableBooleanValue extends AbstractValue implements BooleanValue {

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
