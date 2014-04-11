package ws.palladian.core;

public final class ImmutableDoubleValue implements NumericValue {

    private final double value;

    public ImmutableDoubleValue(double value) {
        this.value = value;
    }

    @Override
    public double getDouble() {
        return value;
    }

}
