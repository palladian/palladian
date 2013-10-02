package ws.palladian.helper.math;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.FixedSizeQueue;
import ws.palladian.helper.collection.Function;

/**
 * <p>
 * Keep mathematical stats such as mean and standard deviation for a series of numbers.
 * </p>
 * 
 * @author Philipp Katz
 */
public class Stats {

    private final List<Number> values;

    public Stats() {
        this.values = CollectionHelper.newArrayList();
    }

    public Stats(Collection<? extends Number> values) {
        this();
        Validate.notNull(values, "values must not be null");
        this.values.addAll(values);
    }

    /**
     * <p>
     * Crate a new {@link Stats} object with the specified size as window, where only the last n items will be kept.
     * This way, a moving average can be calculated conveniently.
     * </p>
     * 
     * @param size The size of the window, greater zero.
     */
    public Stats(int size) {
        Validate.isTrue(size > 0);
        this.values = FixedSizeQueue.create(size);
    }

    public void add(Number value) {
        Validate.notNull(value, "value must not be null");
        values.add(value.doubleValue());
    }

    public double getMean() {
        return getSum() / getCount();
    }

    public double getStandardDeviation() {
        if (values.isEmpty()) {
            return 0.;
        }
        double mean = getMean();
        double standardDeviation = 0;
        for (Number value : values) {
            standardDeviation += Math.pow(value.doubleValue() - mean, 2);
        }
        standardDeviation /= values.size() - 1;
        standardDeviation = Math.sqrt(standardDeviation);
        return standardDeviation;
    }

    public double getMedian() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> temp = getDoubleValues();
        Collections.sort(temp);
        int numValues = temp.size();
        if (numValues % 2 == 0) {
            return 0.5 * (temp.get(numValues / 2) + temp.get(numValues / 2 - 1));
        } else {
            return temp.get(numValues / 2);
        }
    }

    private List<Double> getDoubleValues() {
        return CollectionHelper.convertList(values, new Function<Number, Double>() {
            @Override
            public Double compute(Number input) {
                return input.doubleValue();
            }
        });
    }

    public int getCount() {
        return values.size();
    }

    public double getMin() {
        Double min = Collections.min(getDoubleValues());
        return min != null ? min : Double.NaN;
    }

    public double getMax() {
        Double max = Collections.max(getDoubleValues());
        return max != null ? max : Double.NaN;
    }

    public double getSum() {
        double sum = 0;
        for (Number value : values) {
            sum += value.doubleValue();
        }
        return sum;
    }

    /**
     * @return Assuming that the given values were errors, return the mean squared error.
     */
    public double getMse() {
        double mse = 0;
        for (Number value : values) {
            mse += Math.pow(value.doubleValue(), 2);
        }
        return mse / values.size();
    }

    /**
     * @return Assuming that the given values were errors, return the root mean squared error.
     */
    public double getRmse() {
        return Math.sqrt(getMse());
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Stats [mean=");
        builder.append(getMean());
        builder.append(", standardDeviation=");
        builder.append(getStandardDeviation());
        builder.append(", median=");
        builder.append(getMedian());
        builder.append(", count=");
        builder.append(getCount());
        builder.append(", min=");
        builder.append(getMin());
        builder.append(", max=");
        builder.append(getMax());
        builder.append(", sum=");
        builder.append(getSum());
        builder.append(", MSE=");
        builder.append(getMse());
        builder.append(", RMSE=");
        builder.append(getRmse());
        builder.append("]");
        return builder.toString();
    }

}
