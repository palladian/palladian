package ws.palladian.core.value;

public abstract class AbstractValue implements Value {

    @Override
    public abstract int hashCode();

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Value value = (Value)obj;
        return equalsValue(value);
    }

    protected abstract boolean equalsValue(Value value);

    @Override
    public abstract String toString();

}
