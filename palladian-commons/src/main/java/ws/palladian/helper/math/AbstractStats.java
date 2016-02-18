package ws.palladian.helper.math;

import org.apache.commons.lang.Validate;

public abstract class AbstractStats implements Stats {

	@Override
	public Stats add(Number... values) {
		Validate.notNull(values, "values must not be null");
		for (Number value : values) {
			add(value);
		}
		return this;
	}

	@Override
	public double getMedian() {
		return getPercentile(50);
	}

	@Override
	public double getRange() {
		if (getCount() == 0) {
			return Double.NaN;
		}
		return getMax() - getMin();
	}

	@Override
	public double getRmse() {
		return Math.sqrt(getMse());
	}

	@Override
	public double getRelativeStandardDeviation() {
		if (getCount() == 0) {
			return Double.NaN;
		}
		double mean = getMean();
		return mean != 0 ? getStandardDeviation() / mean : 0;
	}
	
	@Override
	public double getVariance() {
		if (getCount() == 0) {
			return Double.NaN;
		}
		return Math.sqrt(getStandardDeviation());
	}

}
