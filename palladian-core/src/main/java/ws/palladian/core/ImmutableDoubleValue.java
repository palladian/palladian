package ws.palladian.core;

final class ImmutableDoubleValue extends AbstractValue implements NumericValue {

    private final double value;

    public ImmutableDoubleValue(double value) {
        this.value = value;
    }

    @Override
    public double getDouble() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int hashCode() {
        return Double.valueOf(value).hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableDoubleValue other = (ImmutableDoubleValue)obj;
        return Double.doubleToLongBits(value) == Double.doubleToLongBits(other.value);
    }

}
