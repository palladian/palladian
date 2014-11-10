package ws.palladian.core.value;

import org.apache.commons.lang3.Validate;

public final class ImmutableStringValue extends AbstractValue implements NominalValue {

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
