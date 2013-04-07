/**
 * Created on: 05.02.2013 15:57:23
 */
package ws.palladian.classification.discretization;

import ws.palladian.processing.features.NumericFeature;

public final class NumericBin extends NumericFeature {
    private final Double lowerBound;
    private final Double upperBound;

    public NumericBin(String name, Double lowerBound, Double upperBound, Double index) {
        super(name, index);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    public boolean belongsToBin(NumericFeature feature) {
        return lowerBound <= feature.getValue() && upperBound > feature.getValue();
    }

    public boolean isSmaller(NumericFeature feature) {
        return feature.getValue() < lowerBound;
    }

    public boolean isLarger(NumericFeature feature) {
        return feature.getValue() >= upperBound;
    }

}