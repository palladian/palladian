package ws.palladian.core;

public final class NullValue extends AbstractValue {

    public static final NullValue NULL = new NullValue();

    private NullValue() {
        // use the singleton.
    }

    @Override
    public String toString() {
        return "NULL";
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    protected boolean equalsValue(Value value) {
        return true;
    }

}
