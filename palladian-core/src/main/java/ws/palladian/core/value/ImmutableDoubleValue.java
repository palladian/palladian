package ws.palladian.core.value;

public final class ImmutableDoubleValue extends AbstractValue implements NumericValue {

    private final double doubleValue;

    public ImmutableDoubleValue(double value) {
        this.doubleValue = value;
    }

    @Override
    public double getDouble() {
        return doubleValue;
    }

    @Override
    public String toString() {
        return String.valueOf(doubleValue);
    }

    @Override
    public int hashCode() {
        return Double.valueOf(doubleValue).hashCode();
    }

    @Override
    protected boolean equalsValue(Value value) {
        ImmutableDoubleValue other = (ImmutableDoubleValue)value;
        return Double.doubleToLongBits(doubleValue) == Double.doubleToLongBits(other.doubleValue);
    }

}
