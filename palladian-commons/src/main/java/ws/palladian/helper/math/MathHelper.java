package ws.palladian.helper.math;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * The MathHelper provides mathematical functionality.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class MathHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MathHelper.class);

    private MathHelper() {
        // no instances.
    }

    /**
     * <p>
     * Calculate the Jaccard similarity between two sets. <code>J(A, B) = |A intersection B| / |A union B|</code>.
     * </p>
     * 
     * @param setA The first set.
     * @param setB The second set.
     * @return The Jaccard similarity in the range [0, 1].
     */
    public static <T> double computeJaccardSimilarity(Set<T> setA, Set<T> setB) {
        Set<T> intersection = CollectionHelper.newHashSet();
        intersection.addAll(setA);
        intersection.retainAll(setB);

        if (intersection.size() == 0) {
            return 0;
        }

        Set<T> union = CollectionHelper.newHashSet();
        union.addAll(setA);
        union.addAll(setB);


        return (double)intersection.size() / union.size();
    }

    /**
     * <p>
     * Calculate the overlap coefficient between two sets.
     * <code>Overlap(A, B) = |A intersection B| / min(|A|, |B|)</code>.
     * 
     * @param setA The first set.
     * @param setB The second set.
     * @return The overlap coefficient in the range [0, 1].
     */
    public static <T> double computeOverlapCoefficient(Set<T> setA, Set<T> setB) {
        Set<T> intersection = CollectionHelper.newHashSet();
        intersection.addAll(setA);
        intersection.retainAll(setB);

        return (double)intersection.size() / Math.min(setA.size(), setB.size());
    }

    public static double computeCosineSimilarity(Double[] vector1, Double[] vector2) {

        double dotProduct = computeDotProduct(vector1, vector2);
        double magnitude1 = computeMagnitude(vector1);
        double magnitude2 = computeMagnitude(vector2);

        return dotProduct / (magnitude1 * magnitude2);
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
     * <p>
     * Calculate the confidence interval with a given confidence level and mean.
     * </p>
     * 
     * <p>
     * See here: http://www.bioconsulting.com/calculation_of_the_confidence_interval.htm
     * </p>
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

        return Math.sqrt(chosenZ * chosenZ * mean * (1 - mean) / (samples - 1.0));
    }

    public static double round(double number, int digits) {
        double numberFactor = Math.pow(10.0, digits);
        return Math.round(numberFactor * number) / numberFactor;
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

        return value1 <= numMax && value1 >= numMin;
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
        return value <= max && value >= min;
    }

    public static boolean isWithinMargin(double value1, double value2, double margin) {
        double numMin = value1 - margin * value1;
        double numMax = value1 + margin * value1;

        return value1 < numMax && value1 > numMin;
    }

    public static boolean isWithinCorrectnessMargin(double questionedValue, double correctValue,
            double correctnessMargin) {
        double numMin = correctValue - correctnessMargin * correctValue;
        double numMax = correctValue + correctnessMargin * correctValue;

        return questionedValue < numMax && questionedValue > numMin;
    }

    public static int faculty(int number) {
        int faculty = number;
        while (number > 1) {
            number--;
            faculty *= number;
        }
        return faculty;
    }

    /**
     * <p>
     * Calculate the <a href="http://en.wikipedia.org/wiki/Median">median</a> for a list of double values. The values do
     * not have to be in sorted order in advance.
     * </p>
     * 
     * @param values The values for which to get the median.
     * @return The median.
     */
    public static double getMedian(double[] values) {
        int numValues = values.length;
        Arrays.sort(values);
        if (numValues % 2 == 0) {
            return 0.5 * (values[numValues / 2] + values[numValues / 2 - 1]);
        } else {
            return values[numValues / 2];
        }
    }

    public static double getMedian(long[] values) {
        int numValues = values.length;
        Arrays.sort(values);
        if (numValues % 2 == 0) {
            return 0.5 * (values[numValues / 2] + values[numValues / 2 - 1]);
        } else {
            return values[numValues / 2];
        }
    }

    public static double getAverage(double[] values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public static double getAverage(long[] values) {
        double sum = 0;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    public static long getMedianDifference(long[] sortedList) {
        long[] distances = getDistances(sortedList);
        return (long)getMedian(distances);
    }

    /**
     * <p>
     * Calculate the <a href="http://en.wikipedia.org/wiki/Standard_deviation">standard deviation</a>.
     * </p>
     * 
     * @param values The values for which to get the standard deviation.
     * @param biasCorrection If <code>true</code>, the <i>sample standard deviation</i> is calculated, if
     *            <code>false</code> the <i>standard deviation of the sample</i>.
     * @return The standard deviation, 0 for lists with cardinality of 1, NaN for empty lists.
     */
    public static double getStandardDeviation(double[] values, boolean biasCorrection) {
        if (values.length == 0) {
            return Double.NaN;
        }
        if (values.length == 1) {
            return 0;
        }
        double mean = getAverage(values);
        double deviationSum = 0;
        for (double value : values) {
            deviationSum += Math.pow(value - mean, 2);
        }
        if (biasCorrection) {
            return Math.sqrt(deviationSum / (values.length - 1));
        } else {
            return Math.sqrt(deviationSum / values.length);
        }
    }

    /**
     * <p>
     * Calculate the sample <a href="http://en.wikipedia.org/wiki/Standard_deviation">standard deviation</a>.
     * 
     * @param values The values for which to get the standard deviation.
     * @return The standard deviation, 0 for lists with cardinality of 1, NaN for empty lists.
     */
    public static double getStandardDeviation(double[] values) {
        return getStandardDeviation(values, true);
    }

    /**
     * <p>
     * Calculate the <a href="http://en.wikipedia.org/wiki/Standard_deviation">standard deviation</a>.
     * </p>
     * 
     * @param values The values for which to get the standard deviation.
     * @param biasCorrection If <code>true</code>, the <i>sample standard deviation</i> is calculated, if
     *            <code>false</code> the <i>standard deviation of the sample</i>.
     * @return The standard deviation, 0 for lists with cardinality of 1, NaN for empty lists.
     */
    public static double getStandardDeviation(long[] values, boolean biasCorrection) {
        if (values.length == 0) {
            return Double.NaN;
        }
        if (values.length == 1) {
            return 0;
        }
        double mean = getAverage(values);
        double deviationSum = 0;
        for (Long value : values) {
            deviationSum += Math.pow(value - mean, 2);
        }
        if (biasCorrection) {
            return Math.sqrt(deviationSum / values.length - 1);
        } else {
            return Math.sqrt(deviationSum / values.length);
        }
    }

    public static double getStandardDeviation(long[] values) {
        return getStandardDeviation(values, true);
    }

    /**
     * <p>
     * Get the largest gap in a {@link Collection} of {@link Number}s. E.g. for a Collection of [2,3,7,10] the value 4
     * is returned.
     * </p>
     * 
     * @param values The Collection of Numbers, not <code>null</code>.
     * @return The largest distance between subsequent Numbers, or -1 when an empty collection or a collection of size 1
     *         was supplied.
     */
    public static long getLongestGap(long[] values) {
        long longestGap = -1;
        if (values.length > 1) {
            long[] distances = getDistances(values);
            longestGap = Collections.max(Arrays.asList(ArrayUtils.toObject(distances)));
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

    public static double computeRootMeanSquareError(String inputFile, final String columnSeparator) {
        // array with correct and predicted values
        final List<double[]> values = new ArrayList<double[]>();

        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(columnSeparator);

                double[] pair = new double[2];
                pair[0] = Double.valueOf(parts[0]);
                pair[1] = Double.valueOf(parts[1]);

                values.add(pair);
            }
        };

        FileHelper.performActionOnEveryLine(inputFile, la);

        return computeRootMeanSquareError(values);
    }

    public static double computeRootMeanSquareError(List<double[]> values) {
        double sum = 0.0;
        for (double[] d : values) {
            sum += Math.pow(d[0] - d[1], 2);
        }

        return Math.sqrt(sum / values.size());
    }

    /**
     * Calculate similarity of two lists of the same size.
     * 
     * @param list1 The first list.
     * @param list2 The second list.
     * @return The similarity of the two lists.
     */
    public static ListSimilarity computeListSimilarity(List<String> list1, List<String> list2) {

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

        double similarity = 1 - (double)summedRealDistance / (double)summedMaxDistance;
        double squaredShiftSimilarity = 1 - (double)summedRealSquaredDistance / (double)summedMaxSquaredDistance;
        double rootMeanSquareError = computeRootMeanSquareError(positionValues);

        return new ListSimilarity(similarity, squaredShiftSimilarity, rootMeanSquareError);
    }

    public static ListSimilarity computeListSimilarity(String listFile, final String separator) {

        // two list
        final List<String> list1 = new ArrayList<String>();
        final List<String> list2 = new ArrayList<String>();

        LineAction la = new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] parts = line.split(separator);
                list1.add(parts[0]);
                list2.add(parts[1]);
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
            LOGGER.warn(
                    "tried to sample from a collection that was smaller than the sample size (Collection: {}, sample size: {}",
                    collection.size(), sampleSize);
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
     * @param seed The seed to create the random numbers. The same seed leads to the same number sequence.
     * @return A set of random numbers between min and max.
     */
    public static Set<Integer> createRandomNumbers(int numbers, int min, int max) {
        return createRandomNumbers(numbers, min, max, null);
    }

    /**
     * <p>
     * Create numbers random numbers between [min,max).
     * </p>
     * 
     * @param numbers Number of numbers to generate.
     * @param min The minimum number.
     * @param max The maximum number.
     * @param seed The seed to create the random numbers. The same seed leads to the same number sequence.
     * @return A set of random numbers between min and max.
     */
    public static Set<Integer> createRandomNumbers(int numbers, int min, int max, Long seed) {
        Set<Integer> randomNumbers = new HashSet<Integer>();

        if (max - min < numbers) {
            LOGGER.warn("the range between min ({}) and max ({}) is not enough to create enough random numbers", min, max);
            return randomNumbers;
        }
        Random random = new Random();
        if (seed != null) {
            random.setSeed(seed);
        }
        while (randomNumbers.size() < numbers) {
            double nd = random.nextDouble();
            int randomNumber = (int)(nd * max + min);
            randomNumbers.add(randomNumber);
        }

        return randomNumbers;
    }

    /**
     * <p>
     * </p>
     * 
     * @param low The minimum number that the random number
     * @param high
     * @return
     */
    public static int getRandomIntBetween(int low, int high) {
        int hl = high - low;
        return (int)Math.round(Math.random() * hl + low);
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
            LOGGER.warn("linear regression input is not correct, for each x, there must be a y");
        }
        double n = x.length;
        double sx = 0;
        double sy = 0;
        double sxx = 0;
        // double syy = 0;
        double sxy = 0;

        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sxx += x[i] * x[i];
            // syy += y[i] * y[i];
            sxy += x[i] * y[i];
        }

        double beta = (n * sxy - sx * sy) / (n * sxx - sx * sx);
        double alpha = sy / n - beta * sx / n;

        alphaBeta[0] = alpha;
        alphaBeta[1] = beta;

        return alphaBeta;
    }

    /**
     * <p>
     * Calculates the Precision and Average Precision for a ranked list. Pr and AP for each rank are returned as a two
     * dimensional array, where the first dimension indicates the Rank k, the second dimension distinguishes between Pr
     * and AP. Example:
     * </p>
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

            double prAtK = (double)numRelevant / (k + 1);

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
        return Math.log(num) / Math.log(2);
    }

    public static long crossTotal(long s) {
        if (s < 10) {
            return s;
        }
        return crossTotal(s / 10) + s % 10;
    }

    /**
     * <p>
     * Compute distances between subsequent {@link Longs}s in a {@link Collection}. E.g. for a Collection of [2,3,7,10]
     * a result of [1,4,3] is returned.
     * </p>
     * 
     * @param values The Collection of Numbers, not <code>null</code>.
     * @return The distances between the subsequent Numbers in the Collection, or empty array for empty input array or
     *         arrays of size 1.
     */
    public static long[] getDistances(long[] values) {
        Validate.notNull(values, "values must not be null");

        if (values.length < 1) {
            return new long[0];
        }

        long[] ret = new long[values.length - 1];
        for (int i = 1; i < values.length; i++) {
            ret[i - 1] = values[i] - values[i - 1];
        }
        return ret;
    }

    /**
     * <p>
     * Computes the distance between two coordinates (given in latitude and longitude) in kilometers.
     * </p>
     * 
     * @param lat1 The latitude of the first place.
     * @param lng1 The longitude of the first place.
     * @param lat2 The latitude of the second place.
     * @param lng2 The longitude of the second place.
     * @return The distance between the points in kilometers.
     */
    public static double computeDistanceBetweenWorldCoordinates(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6384;
        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double sindLat = Math.sin(dLat / 2);
        double sindLng = Math.sin(dLng / 2);
        double a = Math.pow(sindLat, 2) + Math.pow(sindLng, 2) * Math.cos(lat1Rad) * Math.cos(lat2Rad);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = earthRadius * c;

        return distance;
    }

    /**
     * <p>
     * Compute the Pearson's correlation coefficient between to variables.
     * </p>
     * 
     * @param x A list of double values from the data series of the first variable.
     * @param y A list of double values from the data series of the second variable.
     * @return The Pearson correlation coefficient.
     */
    public static double computePearsonCorrelationCoefficient(List<Double> x, List<Double> y) {

        double sumX = 0.;
        double sumY = 0.;

        for (Double v : x) {
            sumX += v;
        }
        for (Double v : y) {
            sumY += v;
        }

        double avgX = sumX / x.size();
        double avgY = sumY / y.size();

        double nominator = 0.;
        double denominatorX = 0.;
        double denominatorY = 0.;

        for (int i = 0; i < x.size(); i++) {
            nominator += ((x.get(i) - avgX) * (y.get(i) - avgY));
            denominatorX += Math.pow((x.get(i) - avgX), 2);
            denominatorY += Math.pow((y.get(i) - avgY), 2);
        }

        double denominator = Math.sqrt(denominatorX * denominatorY);

        return nominator / denominator;
    }

    /**
     * <p>
     * Parse a numeric expression in a string to a double.
     * </p>
     * 
     * <pre>
     * "0.5" => 0.5
     * "1/2" => 0.5
     * "½" => 0.5
     * "3 1/8" => 3.125
     * "1½" => 1.5
     * "1 ½" => 1.5
     * </pre>
     * 
     * @param stringNumber The string containing the numeric expression.
     * @return The parsed double.
     */
    public static double parseStringNumber(String stringNumber) {
        Validate.notNull(stringNumber);

        stringNumber = stringNumber.toLowerCase();

        double value = 0.;

        // find fraction characters
        Set<String> remove = new HashSet<String>();
        if (stringNumber.contains("¼")) {
            value += 1 / 4.;
            remove.add("¼");
        }
        if (stringNumber.contains("½")) {
            value += 1 / 2.;
            remove.add("½");
        }
        if (stringNumber.contains("¾")) {
            value += 3 / 4.;
            remove.add("¾");
        }
        if (stringNumber.contains("⅓")) {
            value += 1 / 3.;
            remove.add("⅓");
        }
        if (stringNumber.contains("⅔")) {
            value += 2 / 3.;
            remove.add("⅔");
        }
        if (stringNumber.contains("⅕")) {
            value += 1 / 5.;
            remove.add("⅕");
        }
        if (stringNumber.contains("⅖")) {
            value += 2 / 5.;
            remove.add("⅖");
        }
        if (stringNumber.contains("⅗")) {
            value += 3 / 5.;
            remove.add("⅗");
        }
        if (stringNumber.contains("⅘")) {
            value += 4 / 5.;
            remove.add("⅘");
        }
        if (stringNumber.contains("⅙")) {
            value += 1 / 6.;
            remove.add("⅙");
        }
        if (stringNumber.contains("⅚")) {
            value += 5 / 6.;
            remove.add("⅚");
        }
        if (stringNumber.contains("⅛")) {
            value += 1 / 8.;
            remove.add("⅛");
        }
        if (stringNumber.contains("⅜")) {
            value += 3 / 8.;
            remove.add("⅜");
        }
        if (stringNumber.contains("⅝")) {
            value += 5 / 8.;
            remove.add("⅝");
        }
        if (stringNumber.contains("⅞")) {
            value += 7 / 8.;
            remove.add("⅞");
        }

        for (String string : remove) {
            stringNumber = stringNumber.replace(string, "");
        }

        // resolve fractions like "1/2"
        Matcher matcher = Pattern.compile("(\\d+)/(\\d+)").matcher(stringNumber);
        if (matcher.find()) {
            int nominator = Integer.parseInt(matcher.group(1));
            int denominator = Integer.parseInt(matcher.group(2));
            value += nominator / (double)denominator;
            stringNumber = stringNumber.replace(matcher.group(), "");
        }

        // parse the rest
        stringNumber = stringNumber.replaceAll("[^0-9.]", "");
        stringNumber = stringNumber.trim();
        if (!stringNumber.isEmpty()) {
            value += Double.parseDouble(stringNumber);
        }

        return value;
    }

}