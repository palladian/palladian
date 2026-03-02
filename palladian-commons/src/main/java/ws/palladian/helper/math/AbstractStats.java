package ws.palladian.helper.math;

import org.apache.commons.lang3.Validate;
import org.apache.commons.math3.util.FastMath;

import java.util.Iterator;

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
        double stdDev = getStandardDeviation();
        return stdDev * stdDev;
    }

    @Override
    public double getSkewness() {
        double n = getCount();
        if (n == 0) {
            return 0;
        }
        // http://brownmath.com/stat/shape.htm#Skewness
        double skewness = getMomentAboutMean(3) / FastMath.pow(getMomentAboutMean(2), 3. / 2);
        if (isSample()) {
            skewness = skewness * Math.sqrt(n * (n - 1)) / (n - 2);
        }
        return skewness;
    }

    @Override
    public double getKurtosis() {
        int n = getCount();
        if (n == 0) {
            return 0;
        }
        // http://brownmath.com/stat/shape.htm#Kurtosis
        double kurtosis = getMomentAboutMean(4) / FastMath.pow(getMomentAboutMean(2), 2) - 3;
        if (isSample()) {
            kurtosis = (double) (n - 1) / ((n - 2) * (n - 3)) * ((n + 1) * kurtosis + 6);
        }
        return kurtosis;
    }

    @Override
    public double getMomentAboutMean(int k) {
        if (getCount() == 0) {
            return Double.NaN;
        }
        double mean = getMean();
        double moment = 0;
        for (Double number : this) {
            moment += FastMath.pow(number - mean, k);
        }
        return moment /= getCount();
    }

    @Override
    public Iterator<Double> iterator() {
        throw new UnsupportedOperationException("Not supported by this stats implementation");
    }

}
