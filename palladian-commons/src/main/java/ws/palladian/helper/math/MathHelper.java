package ws.palladian.helper.math;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math.stat.descriptive.rank.Median;
import org.apache.log4j.Logger;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * The MathHelper adds mathematical functionality.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class MathHelper {

    /**
     * <p>
     * Calculate the Jaccard similarity between two sets. J(A,B) = A and B / A union B.
     * </p>
     * 
     * @param setA The first set.
     * @param setB The second set.
     * @return The jaccard similarity in the range [0,1].
     */
    public static double computeJaccardSimilarity(Set<String> setA, Set<String> setB) {
        double similarity = -1;

        Set<String> aUnionB = new HashSet<String>();
        aUnionB.addAll(setA);
        aUnionB.addAll(setB);

        // check items which are in both sets
        Set<String> checkSet = new HashSet<String>();
        checkSet.addAll(setA);
        int aAndB = 0;
        for (String itemB : setB) {
            if (!checkSet.add(itemB)) {
                aAndB++;
            }
        }

        similarity = aAndB / (double) aUnionB.size();

        return similarity;
    }
    
    public static double computeCosineSimilarity(Double[] vector1, Double[] vector2) {
        double similarity = 0.0;
        
        double dotProduct = computeDotProduct(vector1, vector2);
        double magnitude1 = computeMagnitude(vector1);
        double magnitude2 = computeMagnitude(vector2);

        similarity = dotProduct / (magnitude1 * magnitude2);
        
        return similarity;
    }

    public static double computeDotProduct(Double[] vector1, Double[] vector2) {
        double dotProduct = 0.0;

        for (int i = 0; i < Math.min(vector1.length, vector2.length); i++) {
            dotProduct += vector1[i] * vector2[i];
        }

        return dotProduct;
    }

    public static double computeMagnitude(Double[] vector) {
        double magnitude = 0.0;

        for (Double double1 : vector) {
            magnitude += double1 * double1;
        }

        return Math.sqrt(magnitude);
    }

    /**
     * <p>Calculate the confidence interval with a given confidence level and mean.</p>
     * 
     * <p>See here: http://www.bioconsulting.com/calculation_of_the_confidence_interval.htm</p>
     * 
     * @param samples The number of samples used.
     * @param confidenceLevel Must be one of the following: 0.75, 0.85, 0.90, 0.95, 0.99.
     * @param mean The mean, if unknown, assume worst case with mean = 0.5.
     * 
     * @return The calculated confidence interval.
     */
    public static double computeConfidenceInterval(int samples, double confidenceLevel, double mean) {
        
        Map<Double, Double> zValues = new HashMap<Double, Double>();
        zValues.put(0.75, 1.151);
        zValues.put(0.85, 1.139);
        zValues.put(0.90, 1.645);
        zValues.put(0.95, 1.96);
        zValues.put(0.99, 2.577);
        
        double chosenZ = zValues.get(confidenceLevel);
        
        double confidenceInterval = Math.sqrt(chosenZ * chosenZ * mean * (1-mean) / (samples - 1.0));
        
        return confidenceInterval;
    }

    public static double round(double number, int digits) {
        double numberFactor = Math.pow(10.0, digits);
        return Math.round(numberFactor * number) / numberFactor;
    }

    public static int getPower(String numberString) {
        int power = -99999;
        try {
            power = (int) Math.floor(Math.log10(Double.valueOf(numberString)));
        } catch (NumberFormatException e) {
            Logger.getRootLogger().error(numberString + ", " + e.getMessage());
        }
        return power;
    }

    /**
     * <p>
     * Check whether one value is in a certain range of another value. For example, value1: 5 is within the range: 2 of
     * value2: 3.
     * </p>
     * 
     * @param value1 The value to check whether it is in the range of the other value.
     * @param value2 The value for which the range is added or subtracted.
     * @param range The range.
     * @return <tt>True</tt>, if value1 <= value2 + range && value1 >= value2 - range, <tt>false</tt> otherwise.
     */
    public static boolean isWithinRange(double value1, double value2, double range) {
        double numMin = value2 - range;
        double numMax = value2 + range;

        if (value1 <= numMax && value1 >= numMin) {
            return true;
        }

        return false;
    }

    /**
     * <p>
     * Check whether one value is in a certain interval. For example, value: 5 is within the interval min: 2 to max: 8.
     * </p>
     * 
     * @param value The value to check whether it is in the interval.
     * @param min The min value of the interval.
     * @param max the max value of the interval
     * @return <tt>True</tt>, if value >= min && value <= max, <tt>false</tt> otherwise.
     */
    public static boolean isWithinInterval(double value, double min, double max) {
        if (value <= max && value >= min) {
            return true;
        }

        return false;
    }

    public static boolean isWithinMargin(double value1, double value2, double margin) {
        double numMin = value1 - margin * value1;
        double numMax = value1 + margin * value1;

        if (value1 < numMax && value1 > numMin) {
            return true;
        }

        return false;
    }

    public static boolean isWithinCorrectnessMargin(double questionedValue, double correctValue,
            double correctnessMargin) {
        double numMin = correctValue - correctnessMargin * correctValue;
        double numMax = correctValue + correctnessMargin * correctValue;

        if (questionedValue < numMax && questionedValue > numMin) {
            return true;
        }

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

    public static double getMedian(Collection<? extends Number> valueList) {
        Median median = new Median();
        List<Double> nonNullValues = new ArrayList<Double>();
        for (Number entry : valueList) {
            if (entry == null) {
                continue;
            }
            nonNullValues.add(entry.doubleValue());
        }

        double[] doubles = new double[nonNullValues.size()];
        int i = 0;
        for (double entry : nonNullValues) {
            doubles[i++] = entry;
        }

        return median.evaluate(doubles);
    }

    public static double getAverage(Collection<? extends Number> values) {
        double sum = 0;
        double count = 0;

        for (Number value : values) {
            if (value != null) {
                sum += value.doubleValue();
                count++;
            }
        }

        return sum / count;
    }

    public static long getMedianDifference(TreeSet<Long> valueSet) {
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

    public static long getMedianDifference(List<Long> sortedList) {
        Collections.sort(sortedList);
        Median median = new Median();
        double[] doubles = new double[sortedList.size() - 1];
        int i = 0;
        long lastValue = -1;
        for (Long entry : sortedList) {
            if (lastValue == -1) {
                lastValue = entry;
                continue;
            }
            doubles[i++] = entry - lastValue;
            lastValue = entry;
        }
        return (long) median.evaluate(doubles);
    }

    public static long getStandardDeviation(TreeSet<Long> valueSet) {
        StandardDeviation sd = new StandardDeviation();

        double[] doubles = new double[valueSet.size()];
        int i = 0;
        for (Long entry : valueSet) {
            doubles[i++] = entry;
        }
        return (long) sd.evaluate(doubles);
    }

    public static long getStandardDeviation(List<Long> sortedList) {
        Collections.sort(sortedList);
        StandardDeviation sd = new StandardDeviation();

        double[] doubles = new double[sortedList.size()];
        int i = 0;
        for (Long entry : sortedList) {
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
            long gap = entry - lastValue;
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

    public static double computeRootMeanSquareError(String inputFile, String columnSeparator) {
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

        return computeRootMeanSquareError(values);
    }

    public static double computeRootMeanSquareError(List<double[]> values) {
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
     * 
     * @param list1 The first list.
     * @param list2 The second list.
     * @return The similarity of the two lists.
     */
    public static ListSimilarity computeListSimilarity(List<String> list1, List<String> list2) {

        ListSimilarity ls = new ListSimilarity();

        double similarity = 0;

        // get maximum possible distance
        int summedMaxDistance = 0;
        int summedMaxSquaredDistance = 0;
        int distance = list1.size() - 1;
        for (int i = list1.size(); i > 0; i -= 2) {
            summedMaxDistance += 2 * distance;
            summedMaxSquaredDistance += 2 * Math.pow(distance, 2);
            distance -= 2;
        }

        // get real distance between lists
        int summedRealDistance = 0;
        int summedRealSquaredDistance = 0;
        int position1 = 0;
        List<double[]> positionValues = new ArrayList<double[]>();

        for (String entry1 : list1) {

            int position2 = 0;
            for (String entry2 : list2) {
                if (entry1.equals(entry2)) {
                    summedRealDistance += Math.abs(position1 - position2);
                    summedRealSquaredDistance += Math.pow(position1 - position2, 2);

                    double[] values = new double[2];
                    values[0] = position1;
                    values[1] = position2;
                    positionValues.add(values);
                    break;
                }
                position2++;
            }

            position1++;
        }

        similarity = 1 - (double) summedRealDistance / (double) summedMaxDistance;
        double squaredShiftSimilarity = 1 - (double) summedRealSquaredDistance / (double) summedMaxSquaredDistance;

        ls.setShiftSimilartiy(similarity);
        ls.setSquaredShiftSimilartiy(squaredShiftSimilarity);
        ls.setRmse(computeRootMeanSquareError(positionValues));

        return ls;
    }

    public static ListSimilarity computeListSimilarity(String listFile, String separator) {

        // two list
        List<String> list1 = new ArrayList<String>();
        List<String> list2 = new ArrayList<String>();

        final Object[] obj = new Object[3];
        obj[0] = list1;
        obj[1] = list2;
        obj[2] = separator;

        LineAction la = new LineAction(obj) {

            @SuppressWarnings({ "rawtypes", "unchecked" })
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split((String) obj[2]);

                ((List) obj[0]).add(parts[0]);
                ((List) obj[1]).add(parts[1]);

            }
        };

        FileHelper.performActionOnEveryLine(listFile, la);

        return computeListSimilarity(list1, list2);
    }

    /**
     * Transform an IP address to a number.
     * 
     * @param ipAddress The IP address given in w.x.y.z notation.
     * @return The integer of the IP address.
     */
    public static Long ipToNumber(String ipAddress) {
        String[] addrArray = ipAddress.split("\\.");

        long num = 0;
        for (int i = 0; i < addrArray.length; i++) {
            int power = 3 - i;
            num += Integer.parseInt(addrArray[i]) % 256 * Math.pow(256, power);
        }
        return num;
    }

    /**
     * Transform a number into an IP address.
     * 
     * @param number The integer to be transformed.
     * @return The IP address.
     */
    public static String numberToIp(long number) {
        return (number >> 24 & 0xFF) + "." + (number >> 16 & 0xFF) + "." + (number >> 8 & 0xFF) + "." + (number & 0xFF);
    }

    /**
     * Create a random sample from a given collection.
     * 
     * @param collection The collection from we want to sample from.
     * @param sampleSize The size of the sample.
     * @return A collection with samples from the collection.
     */
    public static <T> Collection<T> randomSample(Collection<T> collection, int sampleSize) {

        if (collection.size() < sampleSize) {
            Logger.getRootLogger().warn("tried to sample from a collection that was smaller than the sample size");
            return collection;
        } else if (collection.size() == sampleSize) {
            return collection;
        }

        Set<Integer> randomNumbers = MathHelper.createRandomNumbers(sampleSize, 0, collection.size());
        
        Set<Integer> indicesUsed = new HashSet<Integer>();
        Set<T> sampledCollection = new HashSet<T>();

        for (int randomIndex : randomNumbers) {

            int currentIndex = 0;
            for (T o : collection) {

                if (currentIndex < randomIndex) {
                    currentIndex++;
                    continue;
                }

                sampledCollection.add(o);
                indicesUsed.add(randomIndex);
                break;
            }

        }

        return sampledCollection;
    }

    /**
     * <p>
     * Create numbers random numbers between [min,max).
     * </p>
     * 
     * @param numbers Number of numbers to generate.
     * @param min The minimum number.
     * @param max The maximum number.
     * @return A set of random numbers between min and max.
     */
    public static Set<Integer> createRandomNumbers(int numbers, int min, int max) {
        Set<Integer> randomNumbers = new HashSet<Integer>();

        if (max - min < numbers) {
            Logger.getRootLogger().warn("the range between min and max is not enough to create enough random numbers");
            return randomNumbers;
        }

        while (randomNumbers.size() < numbers) {
            int randomNumber = (int) (Math.random() * max + min);
            randomNumbers.add(randomNumber);
        }

        return randomNumbers;
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
     * @return The parameter alpha [0] and beta [1] for the regression line.
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
        double alpha = sy / n - beta * sx / n;

        alphaBeta[0] = alpha;
        alphaBeta[1] = beta;

        return alphaBeta;
    }

    /**
     * <p>Calculates the Precision and Average Precision for a ranked list. Pr and AP for each rank are returned as a two
     * dimensional array, where the first dimension indicates the Rank k, the second dimension distinguishes between Pr
     * and AP. Example:</p>
     * 
     * <pre>
     * double[][] ap = MathHelper.calculateAP(rankedList);
     * int k = rankedList.size() - 1;
     * double prAtK = ap[k][0];
     * double apAtK = ap[k][1];
     * </pre>
     * 
     * @param rankedList The ranked list with Boolean values indicating the relevancies of the items.
     * @param totalNumberRelevantForQuery The total number of relevant documents for the query.
     * @return A two dimensional array containing Precision @ Rank k and Average Precision @ Rank k.
     */
    public static double[][] computeAveragePrecision(List<Boolean> rankedList, int totalNumberRelevantForQuery) {

    	// number of relevant entries at k
        int numRelevant = 0;
        
        // sum of all relevant precisions at k
        double relPrSum = 0;
        double[][] result = new double[rankedList.size()][2];

        for (int k = 0; k < rankedList.size(); k++) {

            boolean relevant = rankedList.get(k);

            if (relevant) {
                numRelevant++;
            }

            double prAtK = (double) numRelevant / (k + 1);

            if (relevant) {
                relPrSum += prAtK;
            }

            double ap = relPrSum / totalNumberRelevantForQuery;

            result[k][0] = prAtK;
            result[k][1] = ap;
        }
        
        return result;
    }

    public static double log2(double num) {
        return (Math.log(num) / Math.log(2));
    }

    public static long crossTotal(long s) {
        if (s < 10) {
            return s;
        }
        return crossTotal(s / 10) + s % 10;
    }

    public static void main(String[] a) {
        
        CollectionHelper.print(MathHelper.createRandomNumbers(10, 100, 989979));
        
    }
}