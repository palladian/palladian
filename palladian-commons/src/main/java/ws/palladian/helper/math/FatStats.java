package ws.palladian.helper.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.helper.collection.Bag;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.FixedSizeQueue;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Function;

/**
 * <p>
 * Keep mathematical stats such as mean and standard deviation for a series of numbers. In case you not need to
 * calculate the median or cumulative probabilities and you're not using a windows-based stats (as in
 * {@link FatStats#FatStats(int)}), consider using the {@link SlimStats} class.
 * </p>
 * 
 * @author Philipp Katz
 */
public class FatStats extends AbstractStats {

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

    private final List<Double> values;
    
    private boolean sorted = false;

    /**
     * <p>
     * Create a new, empty {@link Stats} collection.
     * </p>
     */
    public FatStats() {
        this.values = new ArrayList<>();
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
        this.values.addAll(getDoubleValues(values));
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
        sorted = false;
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#add(java.lang.Number)
     */
    @Override
    public Stats add(Number value) {
        Validate.notNull(value, "value must not be null");
        values.add(value.doubleValue());
        sorted = false;
        return this;
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#add(java.lang.Number)
     */
    @Override
    public Stats add(Number... values) {
        Validate.notNull(values, "values must not be null");
        this.values.addAll(getDoubleValues(Arrays.asList(values)));
        sorted = false;
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
        for (double value : values) {
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
        for (double value : values) {
            double tmpM = m;
            m += (value - tmpM) / k;
            s += (value - tmpM) * (value - m);
            k++;
        }
        return Math.sqrt(s / (getCount() - 1));
    }
    
    @Override
    public double getPercentile(int p) {
        Validate.isTrue(p >= 0 && p <= 100, "p must be in range [0,100]");
        if (values.isEmpty()) {
            return Double.NaN;
        }
        conditionalSort();
        double n = p / 100. * values.size();
        if (n == (int)n) {
            return 0.5 * values.get((int)n - 1) + 0.5 * values.get((int)n);
        } else {
            return values.get((int)Math.ceil(n) - 1);
        }
    }

	private void conditionalSort() {
		if (!sorted) {
        	Collections.sort(values);
        	sorted = true;
        }
	}

    private static List<Double> getDoubleValues(Collection<? extends Number> values) {
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
        return Collections.min(values);
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getMax()
     */
    @Override
    public double getMax() {
        if (values.isEmpty()) {
            return Double.NaN;
        }
        return Collections.max(values);
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getSum()
     */
    @Override
    public double getSum() {
        double sum = 0;
        for (Double value : values) {
            sum += value;
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
        for (Double value : values) {
            mse += Math.pow(value, 2);
        }
        return mse / values.size();
    }

    /* (non-Javadoc)
     * @see ws.palladian.helper.math.Stats#getCumulativeProbability(double)
     */
	@Override
	public double getCumulativeProbability(double t) {
		if (values.isEmpty()) {
			return Double.NaN;
		}
		conditionalSort();
		int count = 0;
		for (Double value : values) {
			if (value > t) {
				break;
			}
			count++;
		}
		return (double) count / getCount();
	}
	
	@Override
	public double getMode() {
		double mode = Double.NaN;
		int maxCount = 0;
		Bag<Double> counts = Bag.create();
		for (Double value : values) {
			int newCount = counts.add(value, 1);
			if (newCount > maxCount) {
				maxCount = newCount;
				mode = value;
			}
		}
		return mode;
	}
	
	@Override
	public Iterator<Double> iterator() {
		return CollectionHelper.unmodifiableIterator(values.iterator());
	}

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append("Min: ").append(getMin()).append("\n");
        stringBuilder.append("Max: ").append(getMax()).append("\n");
        stringBuilder.append("Standard Deviation: ").append(getStandardDeviation()).append("\n");
        stringBuilder.append("Mean: ").append(getMean()).append("\n");
        stringBuilder.append("Mode: " ).append(getMode()).append('\n');
        for (int p = 10; p < 100; p += 10) {
            stringBuilder.append(p + "-Percentile: ").append(getPercentile(p)).append('\n');
        }
        stringBuilder.append("Count: ").append(getCount()).append("\n");
        stringBuilder.append("Range: ").append(getRange()).append("\n");
        stringBuilder.append("MSE: ").append(getMse()).append("\n");
        stringBuilder.append("RMSE: ").append(getRmse()).append("\n");
        stringBuilder.append("Sum: ").append(getSum());

        return stringBuilder.toString();
    }

}
