package ws.palladian.helper.math;

import java.util.Collection;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.functional.Factory;

/**
 * <p>
 * Keep mathematical stats such as mean, sum, min, max for a series of numbers. In contrast to {@link FatStats}, this
 * class does <b>not</b> maintain a list of all values. Therefore, the median cannot be computed.
 * </p>
 * 
 * @author Philipp Katz
 */
public class SlimStats implements Stats {

    /**
     * <p>
     * A factory for producing {@link SlimStats} instances.
     * </p>
     */
    public static final Factory<Stats> FACTORY = new Factory<Stats>() {
        @Override
        public Stats create() {
            return new SlimStats();
        }
    };

    private int count = 0;

    private double mean = 0;

    private double min = Double.MAX_VALUE;

    private double max = Double.MIN_VALUE;

    private double sum = 0;

    private double m = 0;

    private double s = 0;

    private double mse = 0;

    /**
     * <p>
     * Create a new, empty {@link SlimStats} collection.
     * </p>
     */
    public SlimStats() {
    }

    /**
     * <p>
     * Create a new {@link SlimStats} collection with the provided values.
     * </p>
     * 
     * @param values The values to add to this Stats collection, not <code>null</code>.
     */
    public SlimStats(Collection<? extends Number> values) {
        Validate.notNull(values, "values must not be null");
        for (Number value : values) {
            add(value);
        }
    }

    @Override
    public SlimStats add(Number value) {
        Validate.notNull(value, "value must not be null");
        double doubleValue = value.doubleValue();
        count++;
        mean += (doubleValue - mean) / count;
        min = Math.min(min, doubleValue);
        max = Math.max(max, doubleValue);
        sum += doubleValue;
        double tmpM = m;
        m += (doubleValue - tmpM) / count;
        s += (doubleValue - tmpM) * (doubleValue - m);
        mse += (doubleValue * doubleValue - mse) / count;
        return this;
    }

    @Override
    public SlimStats add(Number... values) {
        Validate.notNull(values, "values must not be null");
        for (Number value : values) {
            add(value);
        }
        return this;
    }

    @Override
    public double getMean() {
        return count == 0 ? Double.NaN : mean;
    }

    @Override
    public double getStandardDeviation() {
        if (count == 0) {
            return Double.NaN;
        }
        if (count == 1) {
            return 0.;
        }
        return Math.sqrt(s / (getCount() - 1));
    }

    @Override
    public double getMedian() {
        throw new UnsupportedOperationException("Calculating the median is not supported by this stats.");
    }
    
    @Override
    public double getPercentile(int p) {
        throw new UnsupportedOperationException("Calculating the percentile is not supported by this stats.");
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public double getMin() {
        return count == 0 ? Double.NaN : min;
    }

    @Override
    public double getMax() {
        return count == 0 ? Double.NaN : max;
    }

    @Override
    public double getRange() {
        return count == 0 ? Double.NaN : max - min;
    }

    @Override
    public double getSum() {
        return sum;
    }

    @Override
    public double getMse() {
        return count == 0 ? Double.NaN : mse;
    }

    @Override
    public double getRmse() {
        return Math.sqrt(getMse());
    }

    @Override
    public double getCumulativeProbability(double t) {
        throw new UnsupportedOperationException(
                "Calculating cumulative probabilities is not supported by this stats.");
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Min: ").append(getMin()).append("\n");
        stringBuilder.append("Max: ").append(getMax()).append("\n");
        stringBuilder.append("Standard Deviation: ").append(getStandardDeviation()).append("\n");
        stringBuilder.append("Mean: ").append(getMean()).append("\n");
        stringBuilder.append("Count: ").append(getCount()).append("\n");
        stringBuilder.append("Range: ").append(getRange()).append("\n");
        stringBuilder.append("MSE: ").append(getMse()).append("\n");
        stringBuilder.append("RMSE: ").append(getRmse()).append("\n");
        stringBuilder.append("Sum: ").append(getSum()).append("\n");

        return stringBuilder.toString();
    }

}
