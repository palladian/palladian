package tud.iir.helper;

import java.util.ArrayList;
import java.util.List;
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
     * Check whether two numeric intervals overlap.
     * 
     * @param start1 The start1.
     * @param end1 The end1.
     * @param start2 The start2.
     * @param end2 The end2.
     * @return True, if the intervals overlap, false otherwise.
     */
    public static boolean overlap(int start1, int end1, int start2, int end2) {
        return Math.max(start1, start2) < Math.min(end1, end2);
    }

    public static double calculateRMSE(String inputFile, String columnSeparator) {
        // array with correct and predicted values
        List<double[]> values = new ArrayList<double[]>();

        final Object[] obj = new Object[2];
        obj[0] = values;
        obj[1] = columnSeparator;

        LineAction la = new LineAction(obj) {

            @SuppressWarnings("unchecked")
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split((String) obj[1]);

                double[] pair = new double[2];
                pair[0] = Double.valueOf(parts[0]);
                pair[1] = Double.valueOf(parts[1]);

                ((List<double[]>) obj[0]).add(pair);
            }
        };

        FileHelper.performActionOnEveryLine(inputFile, la);

        return calculateRMSE(values);
    }

    public static double calculateRMSE(List<double[]> values) {
        double rmse = -1.0;

        double sum = 0.0;
        for (double[] d : values) {
            sum += Math.pow(d[0] - d[1], 2);
        }

        rmse = Math.sqrt(sum / values.size());

        return rmse;
    }
    
    /**
     * Calculate similarity of two lists of the same size. 
     * @param list1 The first list.
     * @param list2 The second list.
     * @return The similarity of the two lists.
     */
    public static double calculateListSimilarity(List<String> list1, List<String> list2) {
    	
    	double similarity = 0;
    	
    	
    	// get maximum possible distance
    	int summedMaxDistance = 0;
    	int distance = list1.size() - 1;
    	for (int i = list1.size(); i > 0; i -=2) {
    		summedMaxDistance += 2 * distance;
    		distance -= 2;
    	}
    	
    	// get real distance between lists
    	int summedRealDistance = 0;
    	int position1 = 0;
    	for (String entry1 : list1) {
    		
    		int position2 = 0;
    		for (String entry2 : list2) {
    			if (entry1.equals(entry2)) {
    				summedRealDistance += Math.abs(position1 - position2);
    				break;
    			}
    			position2++;
    		}
    		
    		position1++;    		
    	}
    	
    	similarity = 1 - ((double) summedRealDistance / (double) summedMaxDistance);
    	
    	return similarity;    	
    }

    /**
     * Calculate the parameters for a regression line. A series of x and y must be given. y = beta * x + alpha
     * TODO multiple regression model:
     * http://www.google.com/url?sa=t&source=web&cd=6&ved=0CC8QFjAF&url=http%3A%2F%2Fwww.
     * bbn-school.org%2Fus%2Fmath%2Fap_stats
     * %2Fproject_abstracts_folder%2Fproj_student_learning_folder%2Fmultiple_reg__ludlow
     * .pps&ei=NQQ7TOHNCYacOPan6IoK&usg=AFQjCNEybhIQVP2xwNGHEdYMgqNYelp1lQ&sig2=cwCNr11vMv0PHwdwu_LIAQ,
     * http://www.stat.ufl.edu/~aa/sta6127/ch11.pdf
     * 
     * See <a href="http://en.wikipedia.org/wiki/Simple_linear_regression">http://en.wikipedia.org/wiki/
     * Simple_linear_regression</a> for an explanation.
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