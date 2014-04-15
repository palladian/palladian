package ws.palladian.core;

import org.apache.commons.lang3.Validate;

final class ImmutableStringValue extends AbstractValue implements NominalValue {

    private final String value;

    public ImmutableStringValue(String value) {
        Validate.notNull(value, "value must not be null");
        this.value = value;
    }

    @Override
    public String getString() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableStringValue other = (ImmutableStringValue)obj;
        return value.equals(other.value);
    }

}
