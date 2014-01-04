package ws.palladian.helper.math;

/**
 * <p>
 * Mathematical statistics about a series of numbers.
 * </p>
 * 
 * @author pk
 * 
 */
public interface Stats {

    /**
     * <p>
     * Add a value to this {@link Stats} collection.
     * </p>
     * 
     * @param value The {@link Number} to add, not <code>null</code>.
     * @return This instance, to allow fluent method chaining.
     */
    Stats add(Number value);

    /**
     * <p>
     * Add multiple values to this {@link Stats} collection.
     * </p>
     * 
     * @param values The {@link Number}s to add, not <code>null</code>.
     * @return This instance, to allow fluent method chaining.
     */
    Stats add(Number... values);

    /**
     * @return The mean of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    double getMean();

    /**
     * @return The standard deviation of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    double getStandardDeviation();

    /**
     * @return The median of the provided numbers, or {@link Double#NaN} in case no numbers were provided.
     */
    double getMedian();

    /**
     * @return The number of values present in this {@link Stats} collection.
     */
    int getCount();

    /**
     * @return The minimum value in this {@link Stats} collection, or {@link Double#NaN} in case no numbers were
     *         provided.
     */
    double getMin();

    /**
     * @return The maximum value in this {@link Stats} collection, or {@link Double#NaN} in case no numbers were
     *         provided.
     */
    double getMax();

    /**
     * @return Get the range for the given values in this {@link Stats} collection, i.e. the difference between the
     *         maximum and the minimum value, or {@link Double#NaN} in case no numbers were provided.
     */
    double getRange();

    /**
     * @return The sum of all values in this {@link Stats} collection. 0 in case the collection is empty.
     */
    double getSum();

    /**
     * @return Assuming that the given values were errors, return the mean squared error.
     */
    double getMse();

    /**
     * @return Assuming that the given values were errors, return the root mean squared error.
     */
    double getRmse();

    /**
     * <p>
     * Calculate the cumulative probability with a <a
     * href="http://en.wikipedia.org/wiki/Empirical_distribution_function">empirical distribution function</a>.
     * </p>
     * 
     * @param t The parameter t.
     * @return The probability for being less/equal t.
     */
    double getCumulativeProbability(double t);

}