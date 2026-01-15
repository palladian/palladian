package ws.palladian.utils;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;
import ws.palladian.helper.math.AbstractStats;
import ws.palladian.helper.math.FatStats;
import ws.palladian.helper.math.Stats;

import java.util.Objects;
import java.util.function.IntConsumer;

/**
 * Optimized {@link Stats} for image processing (use int array with fixed size
 * for storage, much faster than {@link FatStats}).
 *
 * @author Philipp Katz
 */
public class HistogramStats extends AbstractStats {

    private final int[] numbers;

    private int count;

    /**
     * @param maxValue The maximum value, typically 256 for image processing.
     */
    public HistogramStats(int maxValue) {
        Validate.isTrue(maxValue > 0, "maxValue must be greater zero");
        numbers = new int[maxValue];
    }

    /**
     * Create a new instance with a maximum value of 255.
     */
    public HistogramStats() {
        this(256);
    }

    @Override
    public Stats add(Number value) {
        Validate.notNull(value, "value must not be null");
        Validate.isTrue(value instanceof Integer, "value must be an Integer");
        add(value.intValue(), 1);
        return this;
    }

    /**
     * Adds a given value n times.
     *
     * @param value The value.
     * @param times The count.
     * @return The current instance.
     */
    public Stats add(int value, int times) {
        Validate.isTrue(times >= 0, "times must be greater equal zero");
        numbers[value] += times;
        count += times;
        return this;
    }

    @Override
    public double getMean() {
        if (count == 0) {
            return Double.NaN;
        }
        return getSum() / getCount();
    }

    // XXX this is currently implemented differently from other Stats classes;
    // as we're using the population standard deviation, and not the sample. Not
    // sure why I originally implemented the "sample" variant, maybe we can
    // switch other classes to using the population, too (or provide both
    // variants)

    @Override
    public double getStandardDeviation() {
        return Math.sqrt(momentAboutMean(2));
    }

    @Override
    public double getPercentile(int p) {
        Validate.isTrue(p >= 0 && p <= 100, "p must be in range [0,100]");
        if (count == 0) {
            return Double.NaN;
        }
        double n = p / 100. * count;
        if (n == (int) n) {
            return 0.5 * getValueAtIndex((int) n - 1) + 0.5 * getValueAtIndex((int) n);
        } else {
            return getValueAtIndex((int) Math.ceil(n) - 1);
        }
    }

    /**
     * Get the value at the given index (values are implicitly sorted by
     * histogram).
     *
     * @param index the index.
     * @return the value at the given index.
     */
    private double getValueAtIndex(int index) {
        int cumulatedIndex = 0;
        for (int i = 0; i < numbers.length; i++) {
            int valueBefore = cumulatedIndex;
            cumulatedIndex += numbers[i];
            if (valueBefore <= index && index <= cumulatedIndex) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public double getMin() {
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] > 0) {
                return i;
            }
        }
        return Double.NaN;
    }

    @Override
    public double getMax() {
        for (int i = numbers.length - 1; i >= 0; i--) {
            if (numbers[i] > 0) {
                return i;
            }
        }
        return Double.NaN;
    }

    @Override
    public double getSum() {
        int sum = 0;
        for (int i = 0; i < numbers.length; i++) {
            sum += i * numbers[i];
        }
        return sum;
    }

    @Override
    public double getMse() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getCumulativeProbability(double t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getMode() {
        double mode = Double.NaN;
        int maxCount = 0;
        for (int i = 0; i < numbers.length; i++) {
            if (numbers[i] > maxCount) {
                maxCount = numbers[i];
                mode = i;
            }
        }
        return mode;
    }

    /**
     * Iterate all numbers in this histogram in ascending order.
     *
     * @param consumer The consumer, not <code>null</code>.
     */
    public void iterate(IntConsumer consumer) {
        Objects.requireNonNull(consumer);
        for (int number = 0; number < numbers.length; number++) {
            for (int i = 0; i < numbers[number]; i++) {
                consumer.accept(number);
            }
        }
    }

    /**
     * Get the <a href="https://en.wikipedia.org/wiki/Skewness">skewness</a> of
     * the data. In case, the data is skewed to the left (i.e. the left tail is
     * longer), the result is negative; in case the data is skewed to the right,
     * the result is positive; in case the data is symmetric, the result is
     * zero.
     *
     * @return The skewness.
     * @see <a href="http://brownmath.com/stat/shape.htm#Skewness">Measures of
     * Shape: Skewness and Kurtosis</a>
     */
    public double getSkewness() {
        double variance = getVariance();
        if (variance == 0) {
            return 0;
        }
        return momentAboutMean(3) / FastMath.pow(variance, 3. / 2);
    }

    /**
     * Get the excess
     * <a href="https://en.wikipedia.org/wiki/Kurtosis">kurtosis</a> of the
     * data. The kurtosis measures the peak's height and sharpness relative to
     * the rest of the data. Higher values indicate a higher and sharper peak.
     * This method calculates the <i>excess</i> kurtosis; the excess kurtosis of
     * a normal distribution is zero.
     *
     * @return The excess kurtosis.
     * @see <a href="http://brownmath.com/stat/shape.htm#Kurtosis">Measures of
     * Shape: Skewness and Kurtosis</a>
     */
    public double getKurtosis() {
        double variance = getVariance();
        if (variance == 0) {
            return 0;
        }
        return momentAboutMean(4) / FastMath.pow(variance, 2) - 3;
    }

    @Override
    public boolean isSample() {
        return false;
    }

    private double momentAboutMean(int k) {
        if (count == 0) {
            return Double.NaN;
        }
        double mean = getMean();
        double moment = 0;
        for (int i = 0; i < numbers.length; i++) {
            moment += numbers[i] * FastMath.pow(i - mean, k);
        }
        return moment /= count;
    }

}
