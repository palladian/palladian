package ws.palladian.helper.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class MathHelperTest {

    @Test
    public void testRandomSample() {

        Set<Integer> collection = new HashSet<Integer>();
        collection.add(321);
        collection.add(98);
        collection.add(123);
        collection.add(965);
        collection.add(143);
        collection.add(328);
        collection.add(497);
        collection.add(73);
        collection.add(65);

        collection = (Set<Integer>) MathHelper.randomSample(collection, 5);
        // CollectionHelper.print(collection);
        assertEquals(5, collection.size());

        collection = (Set<Integer>) MathHelper.randomSample(collection, 1);
        assertEquals(1, collection.size());

    }

    @Test
    public void testComputeCosineSimilarity() {

        Double[] vector1 = { 10.0, 50.0 };
        Double[] vector2 = { 8.0, 66.0 };

        // System.out.println(MathHelper.computeCosineSimilarity(vector1, vector2));

        assertEquals(0.997, MathHelper.round(MathHelper.computeCosineSimilarity(vector1, vector2), 3), 0);
    }

    @Test
    public void testStandardDeviation() {
        // see Wikipedia: http://en.wikipedia.org/wiki/Standard_deviation
        TreeSet<Long> values = new TreeSet<Long>();
        values.add(2l);
        values.add(4l);
        values.add(4l);
        values.add(4l);
        values.add(5l);
        values.add(5l);
        values.add(7l);
        values.add(9l);
        assertEquals(2l, MathHelper.getStandardDeviation(values));
    }

    @Test
    public void testMedian() {

        // see Wikipedia: http://en.wikipedia.org/wiki/Median
        List<Double> values = new ArrayList<Double>();
        values.add(1.0);
        values.add(1.0);
        values.add(2.0);
        values.add(3.0);
        values.add(1035.0);
        values.add(89898.86);
        assertEquals(2.5, MathHelper.getMedian(values), 0.00001);

        TreeSet<Long> values2 = new TreeSet<Long>();
        values2.add(1l);
        values2.add(2l);
        values2.add(4l);
        values2.add(9l);
        values2.add(16l);
        values2.add(24l);
        assertEquals(5l, MathHelper.getMedianDifference(values2));
    }

    @Test
    public void testAverage() {

        List<Double> values = new ArrayList<Double>();
        values.add(1.0);
        values.add(1.0);
        values.add(2.0);
        values.add(3.0);
        values.add(1035.0);
        values.add(89898.86);
        assertEquals(15156.81, MathHelper.getAverage(values), 0.00001);

    }

    @Test
    public void testCalculateJaccardSimilarity() {

        Set<String> setA = new HashSet<String>();
        Set<String> setB = new HashSet<String>();

        setA.add("1");
        setA.add("2");
        setA.add("3");
        setA.add("4");
        setB.add("1");
        setB.add("2");
        setB.add("3");
        setB.add("6");
        assertEquals(0.6, MathHelper.computeJaccardSimilarity(setA, setB), 0);

        setA.clear();
        setB.clear();
        setA.add("1");
        setA.add("2");
        setA.add("3");
        setA.add("4");
        setB.add("1");
        setB.add("2");
        setB.add("3");
        setB.add("4");
        assertEquals(1.0, MathHelper.computeJaccardSimilarity(setA, setB), 0);

        setA.clear();
        setB.clear();
        setA.add("1");
        setA.add("2");
        setA.add("3");
        setA.add("4");
        setB.add("5");
        setB.add("6");
        setB.add("7");
        setB.add("8");
        assertEquals(0.0, MathHelper.computeJaccardSimilarity(setA, setB), 0);

    }

    @Test
    public void testLongestGap() {
        Collection<Long> values = new TreeSet<Long>();
        values.add(1l);
        values.add(2l);
        values.add(4l);
        values.add(9l);
        values.add(16l);
        values.add(24l);
        assertEquals(8l, MathHelper.getLongestGap(values));
        
        
    }

    @Test
    public void testPerformLinearRegression() {
        // test with the example data from http://en.wikipedia.org/wiki/Simple_linear_regression
        double[] weights = { 1.47, 1.5, 1.52, 1.55, 1.57, 1.6, 1.63, 1.65, 1.68, 1.70, 1.73, 1.75, 1.78, 1.80, 1.83 };
        double[] heights = { 52.21, 53.12, 54.48, 55.84, 57.20, 58.57, 59.93, 61.29, 63.11, 64.47, 66.28, 68.10, 69.92,
                72.19, 74.46 };

        double[] alphaBeta = MathHelper.performLinearRegression(weights, heights);
        assertEquals(-39.062, MathHelper.round(alphaBeta[0], 3), 0);
        assertEquals(61.272, MathHelper.round(alphaBeta[1], 3), 0);
    }

    @Test
    public void testCalculateListSimilarity() throws FileNotFoundException {

//        System.out.println(MathHelper.round(
//                MathHelper.calculateListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
//                        .getShiftSimilartiy(), 2));
//        System.out.println(MathHelper.round(
//                MathHelper.calculateListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
//                        .getSquaredShiftSimilartiy(), 2));
//        System.out.println(MathHelper.round(
//                MathHelper.calculateListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#").getRmse(), 2));

        List<String> list1 = new ArrayList<String>();
        List<String> list2 = new ArrayList<String>();
        list1.add("a");
        list1.add("b");
        list1.add("c");
        list2.add("c");
        list2.add("b");
        list2.add("a");
        assertEquals(0.0, MathHelper.computeListSimilarity(list1, list2).getShiftSimilartiy(), 0);

        list1 = new ArrayList<String>();
        list2 = new ArrayList<String>();
        list1.add("a");
        list1.add("b");
        list1.add("c");
        list2.add("a");
        list2.add("b");
        list2.add("c");
        assertEquals(1.0, MathHelper.computeListSimilarity(list1, list2).getShiftSimilartiy(), 0);

        assertEquals(0.37, MathHelper.round(
                MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
                        .getShiftSimilartiy(), 2), 0);

        assertEquals(0.57, MathHelper.round(
                MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#")
                        .getSquaredShiftSimilartiy(), 2), 0);

        assertEquals(4.16, MathHelper.round(
                MathHelper.computeListSimilarity(ResourceHelper.getResourcePath("/list.csv"), "#").getRmse(), 2), 0);

    }

    @Test
    public void testCalculateRMSE() throws FileNotFoundException {

        List<double[]> values = new ArrayList<double[]>();

        values.add(new double[] { 2, 1 });
        values.add(new double[] { 2, 1 });
        values.add(new double[] { 5, 10 });
        values.add(new double[] { 10, 8 });
        values.add(new double[] { 22, 7 });

        assertEquals(7.155, MathHelper.round(MathHelper.computeRootMeanSquareError(values), 3), 0);

        assertEquals(3.607,
                MathHelper.round(MathHelper.computeRootMeanSquareError(ResourceHelper.getResourcePath("/rmseInput.csv"), ";"), 3), 0);
    }

    @Test
    public void testCalculateAP() {

        List<Boolean> rankedList = Arrays.asList(true, false, true, true, true, true, false);

        // the total number of relevant documents for the query
        int totalNumberRelevantForQuery = 5;
        
        double[][] ap = MathHelper.computeAveragePrecision(rankedList,totalNumberRelevantForQuery);
        int k = rankedList.size() - 1;
        double prAtK = ap[k][0];
        double apAtK = ap[k][1];

        assertEquals(5. / 7, prAtK, 0);
        assertEquals((1+2./3+3./4+4./5+5./6)/(double) totalNumberRelevantForQuery, apAtK, 0);

    }
    
    @Test
    public void testGetDistances() {
        assertTrue(MathHelper.getDistances(new ArrayList<Number>()).isEmpty());
        assertTrue(MathHelper.getDistances(Arrays.asList(2)).isEmpty());
        assertTrue(Arrays.asList(1l,4l,3l).equals(MathHelper.getDistances(Arrays.asList(2, 3, 7, 10))));
    }

}
