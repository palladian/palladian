package ws.palladian.classification.discretization;

import ws.palladian.core.NumericValue;

public final class NumericBin implements NumericValue {
    private final double lowerBound;
    private final double upperBound;
    private final double index;

    public NumericBin(int index, double lowerBound, double upperBound) {
        this.index = index;
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public boolean belongsToBin(double value) {
        return lowerBound <= value && upperBound > value;
    }

    public boolean isSmaller(double value) {
        return value < lowerBound;
    }

    public boolean isLarger(double value) {
        return value >= upperBound;
    }

    @Override
    public double getDouble() {
        return index;
    }

    @Override
    public String toString() {
        return String.valueOf(getDouble());
    }

}
