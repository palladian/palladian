package ws.palladian.helper.math;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.FixedSizeQueue;
import ws.palladian.helper.collection.Function;

/**
 * <p>
 * Keep mathematical stats such as mean and standard deviation for a series of numbers. In case you not need to
 * calculate the median or cumulative probabilities and you're not using a windows-based stats (as in
 * {@link FatStats#FatStats(int)}), consider using the {@link SlimStats} class.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FatStats implements Stats {

    /**
     * <p>
     * A factory for producing {@link Stats} instances.
     * </p>
     */
    public static final Factory<FatStats> FACTORY = new Factory<FatStats>() {
        @Override
        public FatStats create() {
            return new FatStats();
        }
    };

    private final List<Number> values;

    /**
     * <p>
     * Create a new, empty {@link Stats} collection.
     * </p>
     */
    public FatStats() {
        this.values = CollectionHelper.newArrayList();
    }

    /**
     * <p>
     * Create a new {@link Stats} collection with the provided values.
     * </p>
     * 
     * @param values The values to add to this Stats collection, not <code>null</code>.
     */
    public FatStats(Collection<? extends Number> values) {
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
    public FatStats(int size) {
        Validate.isTrue(size > 0);
        this.values = FixedSizeQueue.create(size);
    }
    
    /**
     * <p>
     * Add another {@link FatStats} to this {@link FatStats}.
     * </p>
     * 
     * @param stats The FatStats to add, not <code>null</code>.
     * @return This instance, for fluent method chaining.
     */
    public FatStats add(FatStats stats) {
        // TODO pull up, but I don't know how to implement the same for SlimStats now, have a look here?
        // http://stats.stackexchange.com/questions/25848/how-to-sum-a-standard-deviation
        Validate.notNull(stats, "stats must not be null");
        values.addAll(stats.values);
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#add(java.lang.Number)
     */
    @Override
    public Stats add(Number value) {
        Validate.notNull(value, "value must not be null");
        values.add(value.doubleValue());
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#add(java.lang.Number)
     */
    @Override
    public Stats add(Number... values) {
        Validate.notNull(values, "values must not be null");
        this.values.addAll(Arrays.asList(values));
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMean()
     */
    @Override
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getStandardDeviation()
     */
    @Override
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMedian()
     */
    @Override
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getCount()
     */
    @Override
    public int getCount() {
        return values.size();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMin()
     */
    @Override
    public double getMin() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return Collections.min(getDoubleValues());
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMax()
     */
    @Override
    public double getMax() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return Collections.max(getDoubleValues());
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getRange()
     */
    @Override
    public double getRange() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return getMax() - getMin();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getSum()
     */
    @Override
    public double getSum() {
        double sum = 0;
        for (Number value : values) {
            sum += value.doubleValue();
        }
        return sum;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMse()
     */
    @Override
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

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getRmse()
     */
    @Override
    public double getRmse() {
        return Math.sqrt(getMse());
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getCumulativeProbability(double)
     */
    @Override
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
