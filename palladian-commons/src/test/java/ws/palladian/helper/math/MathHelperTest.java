package ws.palladian.helper.math;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class MathHelperTest {

    @Test
    public void testRandomSample() {

        Collection<Integer> collection = Arrays.asList(321, 98, 123, 965, 143, 328, 497, 73, 65);

        collection = MathHelper.randomSample(collection, 5);
        assertEquals(5, collection.size());

        collection = MathHelper.randomSample(collection, 1);
        assertEquals(1, collection.size());

    }

    @Test
    public void testComputeCosineSimilarity() {
        Double[] vector1 = {10.0, 50.0};
        Double[] vector2 = {8.0, 66.0};
        assertEquals(0.997, MathHelper.round(MathHelper.computeCosineSimilarity(vector1, vector2), 3), 0);
    }

    @Test
    public void testStandardDeviation() {
        assertEquals(2.14, MathHelper.getStandardDeviation(new double[] {2., 4., 4., 4., 5., 5., 7., 9.}, true), 0.01);
        assertEquals(2.24, MathHelper.getStandardDeviation(new double[] {4, 2, 5, 8, 6}, true), 0.01);
        assertEquals(2, MathHelper.getStandardDeviation(new double[] {2., 4., 4., 4., 5., 5., 7., 9.}, false), 0);
        assertEquals(0, MathHelper.getStandardDeviation(new double[] {1}), 0);
        assertTrue(Double.isNaN(MathHelper.getStandardDeviation(new double[] {})));
    }

    @Test
    public void testMedian() {
        assertEquals(2.5, MathHelper.getMedian(new double[] {1., 1., 2., 3., 1035., 89898.68}), 0);
        assertEquals(2., MathHelper.getMedian(new double[] {0., 1., 2., 3., 4.}), 0);
        assertEquals(2.5, MathHelper.getMedian(new double[] {0., 1., 2., 3., 4., 5.}), 0);
        assertEquals(7., MathHelper.getMedian(new double[] {9., 7., 2.}), 0.00001);
        assertEquals(0., MathHelper.getMedian(new double[] {0., 0., 0., 1.}), 0);
        assertEquals(3948348538l, MathHelper.getMedian(new long[] {1l, 2l, 3948348538l, 3948348539l, 3948348540l}), 0);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetMedianDifference() {
        assertEquals(5l, MathHelper.getMedianDifference(new long[] {1l, 2l, 4l, 9l, 16l, 24l}));
    }

    @Test
    public void testAverage() {
        assertEquals(15156.81, MathHelper.getAverage(new double[] {1., 1., 2., 3., 1035., 89898.86}), 0.00001);
        assertEquals(3948348539l, MathHelper.getAverage(new double[] {3948348538l, 3948348539l, 3948348540l}), 0.00001);
    }

    @Test
    public void testCalculateSetSimilarity() {

        Set<String> set1 = new HashSet<String>(Arrays.asList("1", "2", "3", "4"));
        Set<String> set2 = new HashSet<String>(Arrays.asList("1", "2", "3", "6"));
        Set<String> set3 = new HashSet<String>(Arrays.asList("1", "2", "3", "4"));
        Set<String> set4 = new HashSet<String>(Arrays.asList("5", "6", "7", "8"));
        Set<String> set5 = new HashSet<String>(Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10"));

        assertEquals(0.6, MathHelper.computeJaccardSimilarity(set1, set2), 0);
        assertEquals(1.0, MathHelper.computeJaccardSimilarity(set1, set3), 0);
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(set1, set4), 0);

        assertEquals(0.75, MathHelper.computeOverlapCoefficient(set1, set2), 0);
        assertEquals(1, MathHelper.computeOverlapCoefficient(set1, set5), 0);
    }

    @Test
    public void testLongestGap() {
        assertEquals(8l, MathHelper.getLongestGap(new long[] {1l, 2l, 4l, 9l, 16l, 24l}));
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

    @Test
    public void testComputeRootMeanSquareError() throws FileNotFoundException {

        List<double[]> values = new ArrayList<double[]>();

        values.add(new double[] {2, 1});
        values.add(new double[] {2, 1});
        values.add(new double[] {5, 10});
        values.add(new double[] {10, 8});
        values.add(new double[] {22, 7});

        assertEquals(7.155, MathHelper.round(MathHelper.computeRootMeanSquareError(values), 3), 0);

        assertEquals(3.607, MathHelper.round(
                MathHelper.computeRootMeanSquareError(ResourceHelper.getResourcePath("/rmseInput.csv"), ";"), 3), 0);
    }

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
    public void testGetDistances() {
        assertEquals(0, MathHelper.getDistances(new long[0]).length);
        assertEquals(0, MathHelper.getDistances(new long[] {2l}).length);
        assertArrayEquals(new long[] {1l, 4l, 3l}, MathHelper.getDistances(new long[] {2l, 3l, 7l, 10l}));
    }

    @Test
    public void testParseStringNumbers() {
        assertEquals(0.5, MathHelper.parseStringNumber("0.5 bla"), 0.001);
        assertEquals(0.5, MathHelper.parseStringNumber("1/2 bla"), 0.001);
        assertEquals(0.5, MathHelper.parseStringNumber("½ bla"), 0.001);
        assertEquals(3.125, MathHelper.parseStringNumber("3 1/8 bla"), 0.001);
        assertEquals(1.5, MathHelper.parseStringNumber("1½ bla"), 0.001);
        assertEquals(1.5, MathHelper.parseStringNumber("1 ½ bla"), 0.001);
    }

}
