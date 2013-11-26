package ws.palladian.helper.math;

import java.util.Arrays;
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

    /**
     * <p>
     * Create a new, empty {@link Stats} collection.
     * </p>
     */
    public Stats() {
        this.values = CollectionHelper.newArrayList();
    }

    /**
     * <p>
     * Create a new {@link Stats} collection with the provided values.
     * </p>
     * 
     * @param values The values to add to this Stats collection, not <code>null</code>.
     */
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

    /**
     * <p>
     * Add a value to this {@link Stats} collection.
     * </p>
     * 
     * @param value The {@link Number} to add, not <code>null</code>.
     * @return This instance, to allow fluent method chaining.
     */
    public Stats add(Number value) {
        Validate.notNull(value, "value must not be null");
        values.add(value.doubleValue());
        return this;
    }

    /**
     * <p>
     * Add multiple values to this {@link Stats} collection.
     * </p>
     * 
     * @param values The {@link Number}s to add, not <code>null</code>.
     * @return This instance, to allow fluent method chaining.
     */
    public Stats add(Number... values) {
        Validate.notNull(values, "values must not be null");
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    /**
     * @return The mean of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    public double getMean() {
        // return getSum() / getCount();
        
        if (values.isEmpty()) {
            return Double.NaN;
        }

        // prevent overflows; calculate iteratively as suggested in Knuth, The Art of Computer Programming Vol 2, section 4.2.2. See:
        // http://stackoverflow.com/questions/1930454/what-is-a-good-solution-for-calculating-an-average-where-the-sum-of-all-values-e
        
        double mean = 0;
        int t = 1;
        for (double value : getDoubleValues()) {
            mean += (value - mean) / t;
            t++;
        }
        return mean;
    }

    /**
     * @return The standard deviation of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    public double getStandardDeviation() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        if (values.size() == 1) {
            return 0.;
        }
//        double mean = getMean();
//        double standardDeviation = 0;
//        for (Number value : values) {
//            standardDeviation += Math.pow(value.doubleValue() - mean, 2);
//        }
//        standardDeviation /= values.size() - 1;
//        standardDeviation = Math.sqrt(standardDeviation);
//        return standardDeviation;

        // Welford's method for calculation, see:
        // http://stackoverflow.com/questions/895929/how-do-i-determine-the-standard-deviation-stddev-of-a-set-of-values/897463#897463

        double m = 0;
        double s = 0;
        int k = 1;
        for (double value : getDoubleValues()) {
            double tmpM = m;
            m += (value - tmpM) / k;
            s += (value - tmpM) * (value - m);
            k++;
        }
        return Math.sqrt(s / (getCount() - 1));
    }

    /**
     * @return The median of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    public double getMedian() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        List<Double> temp = getDoubleValues();
        Collections.sort(temp);
        int numValues = temp.size();
        if (numValues % 2 == 0) {
            // return 0.5 * (temp.get(numValues / 2) + temp.get(numValues / 2 - 1));
            return 0.5 * temp.get(numValues / 2) + 0.5 * temp.get(numValues / 2 - 1);
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

    /**
     * @return The number of values present in this {@link Stats} collection.
     */
    public int getCount() {
        return values.size();
    }

    /**
     * @return The minimum value in this {@link Stats} collection, or {@link Double#NaN} in case no numbers were
     *         provided.
     */
    public double getMin() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return Collections.min(getDoubleValues());
    }

    /**
     * @return The maximum value in this {@link Stats} collection, or {@link Double#NaN} in case no numbers were
     *         provided.
     */
    public double getMax() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return Collections.max(getDoubleValues());
    }

    /**
     * @return Get the range for the given values in this {@link Stats} collection, i.e. the difference between the
     *         maximum and the minimum value, or {@link Double#NaN} in case no numbers were provided.
     */
    public double getRange() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return getMax() - getMin();
    }

    /**
     * @return The sum of all values in this {@link Stats} collection. 0 in case the collection is empty.
     */
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
        if (values.isEmpty()) {
            return Double.NaN;
        }
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

    /**
     * <p>
     * Calculate the cumulative probability with a <a
     * href="http://en.wikipedia.org/wiki/Empirical_distribution_function">empirical distribution function</a>.
     * </p>
     * 
     * @param t The parameter t.
     * @return The probability for being less/equal t.
     */
    public double getCumulativeProbability(double t) {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        int count = 0;
        for (Number value : values) {
            if (value.doubleValue() <= t) {
                count++;
            }
        }
        return (double)count / getCount();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Min: ").append(getMin()).append("\n");
        stringBuilder.append("Max: ").append(getMax()).append("\n");
        stringBuilder.append("Standard Deviation: ").append(getStandardDeviation()).append("\n");
        stringBuilder.append("Mean: ").append(getMean()).append("\n");
        stringBuilder.append("Median: ").append(getMedian()).append("\n");
        stringBuilder.append("Count: ").append(getCount()).append("\n");
        stringBuilder.append("Range: ").append(getRange()).append("\n");
        stringBuilder.append("MSE: ").append(getMse()).append("\n");
        stringBuilder.append("RMSE: ").append(getRmse()).append("\n");
        stringBuilder.append("Sum: ").append(getSum()).append("\n");

        return stringBuilder.toString();
    }

}
