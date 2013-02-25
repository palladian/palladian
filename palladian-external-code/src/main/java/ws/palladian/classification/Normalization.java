/**
 * Created on: 13.02.2013 15:40:00
 */
package ws.palladian.classification;

import java.io.Serializable;

import ws.palladian.processing.features.NumericFeature;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class Normalization implements Serializable {

    private static final long serialVersionUID = -2726662477802322532L;
    private Double smallestValue;
    private Double largestValue;

    public Normalization() {
        this.smallestValue = null;
        this.largestValue = null;
    }

    public double apply(double value) {
        double offset = 0 - smallestValue;
        double factor = 1.0d / (largestValue + offset);
        if (Double.isInfinite(factor)) {
            factor = 1.0d;
        }
        return (value + offset) * factor;
    }

    public void add(NumericFeature numericFeature) {
        double value = numericFeature.getValue();
        if (smallestValue == null || value < smallestValue) {
            smallestValue = value;
        }
        if (largestValue == null || value > largestValue) {
            largestValue = value;
        }
    }
}
