package ws.palladian.helper.math;

/**
 * Mathematical statistics about a series of numbers.
 * 
 * @author Philipp Katz
 */
public interface Stats extends Iterable<Double> {

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
     * <p>
     * Get the p-th percentile.
     * </p>
     * 
     * @param p in range [0,100]
     * @return The p-th percentile.
     */
    double getPercentile(int p);

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
    
    /**
     * @return The relative standard deviation, aka. <a href="https://en.wikipedia.org/wiki/Coefficient_of_variation">coefficient of variation</a>.
     */
    double getRelativeStandardDeviation();
    
    /**
     * @return The variance (ie. {@link #getStandardDeviation()}^2).
     */
    double getVariance();
    
	/**
	 * @return The mode (in case, there are multiple modes, it will return one of them).
	 */
    double getMode();
    
	/**
	 * Get the <a href="https://en.wikipedia.org/wiki/Skewness">skewness</a> of
	 * the data. In case, the data is skewed to the left (i.e. the left tail is
	 * longer), the result is negative; in case the data is skewed to the right,
	 * the result is positive; in case the data is symmetric, the result is
	 * zero.
	 * 
	 * @return The skewness.
	 * @see <a href="http://brownmath.com/stat/shape.htm#Skewness">Measures of
	 *      Shape: Skewness and Kurtosis</a>
	 */
	double getSkewness();
	
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
	 *      Shape: Skewness and Kurtosis</a>
	 */
	double getKurtosis();
	
	double getMomentAboutMean(int k);
	
	/**
	 * Indicates, whether these stats are for a sample or the whole population.
	 * 
	 * @return <code>true</code> if statistics represent a sample,
	 *         <code>false</code> if they represent the whole population.
	 */
	boolean isSample();

}