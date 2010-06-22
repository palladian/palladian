package tud.iir.helper;

import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.apache.log4j.Logger;

/**
 * The MathHelper adds mathematical functionality.
 * 
 * @author David Urbansky
 */
public class MathHelper {

    public static double round(double number, int digits) {
        double numberFactor = Math.pow(10.0, digits);
        return Math.round(numberFactor * number) / numberFactor;
    }

    public static int getPower(String numberString) {
        int power = -99999;
        try {
            power = (int) Math.floor(Math.log10(Double.valueOf(numberString)));
        } catch (NumberFormatException e) {
            Logger.getRootLogger().error(numberString, e);
        }
        return power;
    }

    public static boolean isWithinMargin(double value1, double value2, double margin) {
        double numMin = value1 - margin * value1;
        double numMax = value1 + margin * value1;

        if (value1 < numMax && value1 > numMin)
            return true;

        return false;
    }

    public static boolean isWithinCorrectnessMargin(double questionedValue, double correctValue, double correctnessMargin) {
        double numMin = correctValue - correctnessMargin * correctValue;
        double numMax = correctValue + correctnessMargin * correctValue;

        if (questionedValue < numMax && questionedValue > numMin)
            return true;

        return false;
    }

    public static int faculty(int number) {
        int faculty = number;
        while (number > 1) {
            number--;
            faculty *= number;
        }
        return faculty;
    }

    public static long getMedianDifference(final TreeSet<Long> valueSet) {
        Median median = new Median();
        double[] doubles = new double[valueSet.size() - 1];
        int i = 0;
        long lastValue = -1;
        for (Long entry : valueSet) {
            if (lastValue == -1) {
                lastValue = entry;
                continue;
            }
            doubles[i++] = entry - lastValue;
            lastValue = entry;
        }
        return (long) median.evaluate(doubles);
    }

    public static long getStandardDeviation(final TreeSet<Long> valueSet) {
        StandardDeviation sd = new StandardDeviation();

        double[] doubles = new double[valueSet.size()];
        int i = 0;
        for (Long entry : valueSet) {
            doubles[i++] = entry;
        }
        return (long) sd.evaluate(doubles);
    }

    public static long getLongestGap(TreeSet<Long> valueSet) {
        long longestGap = -1;

        long lastValue = -1;
        for (Long entry : valueSet) {
            if (lastValue == -1) {
                lastValue = entry;
                continue;
            }
            long gap = (entry - lastValue);
            if (gap > longestGap) {
                longestGap = gap;
            }
            lastValue = entry;
        }

        return longestGap;
    }

    /**
     * Calculate the parameters for a regression line. A series of x and y must be given. y = beta * x + alpha
     * 
     * See <a href="http://en.wikipedia.org/wiki/Simple_linear_regression">http://en.wikipedia.org/wiki/Simple_linear_regression</a> for an explanation.
     * 
     * @param x A series of x values.
     * @param y A series of y values.
     * @return The parameter alpha and beta for the regression line.
     */
    public static double[] performLinearRegression(double[] x, double[] y) {
        double[] alphaBeta = new double[2];

        if (x.length != y.length) {
            Logger.getRootLogger().warn("linear regression input is not correct, for each x, there must be a y");
        }
        double n = x.length;
        double sx = 0;
        double sy = 0;
        double sxx = 0;
        double syy = 0;
        double sxy = 0;

        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sxx += x[i] * x[i];
            syy += y[i] * y[i];
            sxy += x[i] * y[i];
        }

        double beta = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double alpha = (sy / n) - (beta * sx / n);

        alphaBeta[0] = alpha;
        alphaBeta[1] = beta;

        return alphaBeta;
    }
}