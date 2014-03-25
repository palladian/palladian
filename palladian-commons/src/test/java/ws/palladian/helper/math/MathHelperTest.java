package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class MathHelperTest {

    private final Set<Integer> set1 = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4));
    private final Set<Integer> set2 = new HashSet<Integer>(Arrays.asList(1, 2, 3, 6));
    private final Set<Integer> set3 = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4));
    private final Set<Integer> set4 = new HashSet<Integer>(Arrays.asList(5, 6, 7, 8));
    private final Set<Integer> set5 = new HashSet<Integer>(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
    private final Set<Integer> emptySet = Collections.emptySet();

    @Test
    public void testRandomSample() {
        Collection<Integer> collection = Arrays.asList(321, 98, 123, 965, 143, 328, 497, 73, 65);
        assertEquals(5, MathHelper.randomSample(collection, 5).size());
        assertEquals(1, MathHelper.randomSample(collection, 1).size());
    }

    @Test
    public void testNumberToFraction() {
        assertEquals("1 1/2", MathHelper.numberToFraction(1.5));
        assertEquals("-12 1/2", MathHelper.numberToFraction(-12.5));
        assertEquals("0", MathHelper.numberToFraction(0.04));
        assertEquals("1", MathHelper.numberToFraction(1.005));
        assertEquals("10", MathHelper.numberToFraction(10.));
        assertEquals("1/2", MathHelper.numberToFraction(0.5));
        assertEquals("1/3", MathHelper.numberToFraction(0.33));
        assertEquals("1/4", MathHelper.numberToFraction(0.25));
        assertEquals("1/5", MathHelper.numberToFraction(0.2));
        assertEquals("1/6", MathHelper.numberToFraction(0.16));
        assertEquals("1/7", MathHelper.numberToFraction(0.143));
        assertEquals("1/8", MathHelper.numberToFraction(0.13));
        assertEquals("1/9", MathHelper.numberToFraction(0.11));
        assertEquals("1/10", MathHelper.numberToFraction(0.105));
        assertEquals("1", MathHelper.numberToFraction(0.96));
        assertEquals("31", MathHelper.numberToFraction(31.));
    }

    @Test
    public void testComputeAllCombinations() {

        String[] items = {"a", "b", "c"};
        Collection<List<Object>> allCombinations = MathHelper.computeAllCombinations(items);
        // CollectionHelper.print(allCombinations);
        assertEquals(7, allCombinations.size());

        String[] items2 = {"a"};
        allCombinations = MathHelper.computeAllCombinations(items2);
        // CollectionHelper.print(allCombinations);
        assertEquals(1, allCombinations.size());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testComputeCosineSimilarity() {
        Double[] vector1 = {10.0, 50.0};
        Double[] vector2 = {8.0, 66.0};
        assertEquals(0.997, MathHelper.round(MathHelper.computeCosineSimilarity(vector1, vector2), 3), 0);
    }

    @Test
    public void testCalculateJaccardSimilarity() {
        assertEquals(0.6, MathHelper.computeJaccardSimilarity(set1, set2), 0);
        assertEquals(1.0, MathHelper.computeJaccardSimilarity(set1, set3), 0);
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(set1, set4), 0);
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(emptySet, emptySet), 0);
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(emptySet, set1), 0);
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(set1, emptySet), 0);
    }

    @Test
    public void testCalculateOverlapCoefficient() {
        assertEquals(0.75, MathHelper.computeOverlapCoefficient(set1, set2), 0);
        assertEquals(1, MathHelper.computeOverlapCoefficient(set1, set5), 0);
        assertEquals(0.0, MathHelper.computeOverlapCoefficient(emptySet, emptySet), 0);
        assertEquals(0.0, MathHelper.computeOverlapCoefficient(emptySet, set1), 0);
        assertEquals(0.0, MathHelper.computeOverlapCoefficient(set1, emptySet), 0);
    }

    @Test
    public void testPerformLinearRegression() {
        // test with the example data from http://en.wikipedia.org/wiki/Simple_linear_regression
        double[] weights = {1.47, 1.5, 1.52, 1.55, 1.57, 1.6, 1.63, 1.65, 1.68, 1.70, 1.73, 1.75, 1.78, 1.80, 1.83};
        double[] heights = {52.21, 53.12, 54.48, 55.84, 57.20, 58.57, 59.93, 61.29, 63.11, 64.47, 66.28, 68.10, 69.92,
                72.19, 74.46};

        double[] alphaBeta = MathHelper.performLinearRegression(weights, heights);
        assertEquals(-39.062, MathHelper.round(alphaBeta[0], 3), 0);
        assertEquals(61.272, MathHelper.round(alphaBeta[1], 3), 0);
    }

    @Test
    public void testCalculateListSimilarity() throws FileNotFoundException {

        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("c", "b", "a");
        List<String> list3 = Arrays.asList("a", "b", "c");

        assertEquals(0.0, MathHelper.computeListSimilarity(list1, list2).getShiftSimilarity(), 0);
        assertEquals(1.0, MathHelper.computeListSimilarity(list1, list3).getShiftSimilarity(), 0);

        assertEquals(0.37, MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
                .getShiftSimilarity(), 0.01);
        assertEquals(0.57, MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
                .getSquaredShiftSimilarity(), 0.01);
        assertEquals(4.16,
                MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#").getRmse(), 0.01);

    }

//    @SuppressWarnings("deprecation")
//    @Test
//    public void testComputeRootMeanSquareError() throws FileNotFoundException {
//
//        List<double[]> values = new ArrayList<double[]>();
//
//        values.add(new double[] {2, 1});
//        values.add(new double[] {2, 1});
//        values.add(new double[] {5, 10});
//        values.add(new double[] {10, 8});
//        values.add(new double[] {22, 7});
//
//        assertEquals(7.155, MathHelper.round(MathHelper.computeRootMeanSquareError(values), 3), 0);
//
//        assertEquals(3.607, MathHelper.round(
//                MathHelper.computeRootMeanSquareError(ResourceHelper.getResourcePath("/rmseInput.csv"), ";"), 3), 0);
//    }

    @Test
    public void testComputeAveragePrecision() {

        List<Boolean> rankedList = Arrays.asList(true, false, true, true, true, true, false);

        // the total number of relevant documents for the query
        int totalNumberRelevantForQuery = 5;

        double[][] ap = MathHelper.computeAveragePrecision(rankedList, totalNumberRelevantForQuery);
        int k = rankedList.size() - 1;
        double prAtK = ap[k][0];
        double apAtK = ap[k][1];

        assertEquals(5. / 7, prAtK, 0);
        assertEquals((1 + 2. / 3 + 3. / 4 + 4. / 5 + 5. / 6) / totalNumberRelevantForQuery, apAtK, 0);

    }

    @Test
    public void testGetRandomIntBetween() {
        int r = MathHelper.getRandomIntBetween(4, 80);
        assertTrue(r >= 4 && r <= 80);

        r = MathHelper.getRandomIntBetween(1, 5);
        assertTrue(r >= 1 && r <= 5);

        r = MathHelper.getRandomIntBetween(10, 11);
        assertTrue(r >= 10 && r <= 11);

        r = MathHelper.getRandomIntBetween(0, 100);
        assertTrue(r >= 0 && r <= 100);
    }

    @Test
    public void testComputePearsonCorrelationCoefficient() {
        List<Double> x = Arrays.asList(56., 56., 65., 65., 50., 25., 87., 44., 35.);
        List<Double> y = Arrays.asList(87., 91., 85., 91., 75., 28., 122., 66., 58.);
        assertEquals(0.9661943464912911, MathHelper.computePearsonCorrelationCoefficient(x, y), 0.01);
    }

    @Test
    public void testParseStringNumbers() {
        assertEquals(1.5, MathHelper.parseStringNumber("1.5 c. bowls"), 0.001);
        assertEquals(0.5, MathHelper.parseStringNumber("0.5 bla"), 0.001);
        assertEquals(0.5, MathHelper.parseStringNumber("1/2 bla"), 0.001);
        assertEquals(0.5, MathHelper.parseStringNumber("½ bla"), 0.001);
        assertEquals(3.125, MathHelper.parseStringNumber("3 1/8 bla"), 0.001);
        assertEquals(1.5, MathHelper.parseStringNumber("1½ bla"), 0.001);
        assertEquals(1.5, MathHelper.parseStringNumber("1 ½ bla"), 0.001);
    }

    @Test
    public void testGetOrderOfMagnitude() {
        assertEquals(0, MathHelper.getOrderOfMagnitude(0));
        assertEquals(0, MathHelper.getOrderOfMagnitude(1));
        assertEquals(1, MathHelper.getOrderOfMagnitude(10));
        assertEquals(2, MathHelper.getOrderOfMagnitude(100));
        assertEquals(-1, MathHelper.getOrderOfMagnitude(0.1));
        assertEquals(-2, MathHelper.getOrderOfMagnitude(0.01));
        assertEquals(5, MathHelper.getOrderOfMagnitude(123456));
    }

    @Test
    public void testRound() {
        assertEquals(0.333, MathHelper.round(1. / 3, 3), 0.);
        assertTrue(Double.isNaN(MathHelper.round(Double.NaN, 2)));
    }
    
    @Test
    public void testConfidenceInterval() {
        assertEquals(0.052, MathHelper.computeConfidenceInterval(1000, 0.999, 0.5), 0.001);
        assertEquals(0.026, MathHelper.computeConfidenceInterval(1000, 0.9, 0.5), 0.001);
        assertEquals(0.018, MathHelper.computeConfidenceInterval(1000, 0.75, 0.5), 0.001);
    }

}
