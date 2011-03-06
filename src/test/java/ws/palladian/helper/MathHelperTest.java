package ws.palladian.helper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;

import ws.palladian.helper.math.MathHelper;

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
        Assert.assertEquals(5, collection.size());

        collection = (Set<Integer>) MathHelper.randomSample(collection, 1);
        Assert.assertEquals(1, collection.size());

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
        Assert.assertEquals(2l, MathHelper.getStandardDeviation(values));
    }

    @Test
    public void testMedian() {
        // see Wikipedia: http://en.wikipedia.org/wiki/Median
        TreeSet<Long> values = new TreeSet<Long>();
        values.add(1l);
        values.add(2l);
        values.add(4l);
        values.add(9l);
        values.add(16l);
        values.add(24l);
        Assert.assertEquals(5l, MathHelper.getMedianDifference(values));
    }

    @Test
    public void testLongestGap() {
        TreeSet<Long> values = new TreeSet<Long>();
        values.add(1l);
        values.add(2l);
        values.add(4l);
        values.add(9l);
        values.add(16l);
        values.add(24l);
        Assert.assertEquals(8l, MathHelper.getLongestGap(values));
    }

    @Test
    public void testPerformLinearRegression() {
        // test with the example data from http://en.wikipedia.org/wiki/Simple_linear_regression
        double[] weights = { 1.47, 1.5, 1.52, 1.55, 1.57, 1.6, 1.63, 1.65, 1.68, 1.70, 1.73, 1.75, 1.78, 1.80, 1.83 };
        double[] heights = { 52.21, 53.12, 54.48, 55.84, 57.20, 58.57, 59.93, 61.29, 63.11, 64.47, 66.28, 68.10, 69.92,
                72.19, 74.46 };

        double[] alphaBeta = MathHelper.performLinearRegression(weights, heights);
        Assert.assertEquals(-39.062, MathHelper.round(alphaBeta[0], 3));
        Assert.assertEquals(61.272, MathHelper.round(alphaBeta[1], 3));
    }

    @Test
    public void testCalculateListSimilarity() {

        // System.out.println(MathHelper.round(MathHelper.calculateListSimilarity("data/test/list.csv", "#")
        // .getShiftSimilartiy(), 2));
        // System.out.println(MathHelper.round(MathHelper.calculateListSimilarity("data/test/list.csv", "#")
        // .getSquaredShiftSimilartiy(), 2));
        // System.out
        // .println(MathHelper.round(MathHelper.calculateListSimilarity("data/test/list.csv", "#").getRmse(), 2));

        List<String> list1 = new ArrayList<String>();
        List<String> list2 = new ArrayList<String>();
        list1.add("a");
        list1.add("b");
        list1.add("c");
        list2.add("c");
        list2.add("b");
        list2.add("a");
        Assert.assertEquals(0.0, MathHelper.calculateListSimilarity(list1, list2).getShiftSimilartiy());

        list1 = new ArrayList<String>();
        list2 = new ArrayList<String>();
        list1.add("a");
        list1.add("b");
        list1.add("c");
        list2.add("a");
        list2.add("b");
        list2.add("c");
        Assert.assertEquals(1.0, MathHelper.calculateListSimilarity(list1, list2).getShiftSimilartiy());

        Assert.assertEquals(0.37, MathHelper.round(
                MathHelper.calculateListSimilarity(MathHelperTest.class.getResource("/list.csv").getFile(), "#")
                        .getShiftSimilartiy(), 2));

        Assert.assertEquals(0.57, MathHelper.round(
                MathHelper.calculateListSimilarity(MathHelperTest.class.getResource("/list.csv").getFile(), "#")
                        .getSquaredShiftSimilartiy(), 2));

        Assert.assertEquals(4.16, MathHelper.round(
                MathHelper.calculateListSimilarity(MathHelperTest.class.getResource("/list.csv").getFile(), "#")
                        .getRmse(), 2));

    }

    @Test
    public void testCalculateRMSE() {

        List<double[]> values = new ArrayList<double[]>();

        values.add(new double[] { 2, 1 });
        values.add(new double[] { 2, 1 });
        values.add(new double[] { 5, 10 });
        values.add(new double[] { 10, 8 });
        values.add(new double[] { 22, 7 });

        Assert.assertEquals(7.155, MathHelper.round(MathHelper.calculateRMSE(values), 3));

        Assert.assertEquals(
                3.607,
                MathHelper.round(
                        MathHelper.calculateRMSE(MathHelperTest.class.getResource("/rmseInput.csv").getFile(), ";"), 3));
    }
    
    @Test
    public void testCalculateAP() {
        
        List<Boolean> rankedList = Arrays.asList(true, false, true, true, true, true, false);
        
        double[][] ap = MathHelper.calculateAP(rankedList);
        int k = rankedList.size() - 1;
        double prAtK = ap[k][0];
        double apAtK = ap[k][1];
        
        Assert.assertEquals(5./7, prAtK);
        Assert.assertEquals((1 + 2./3 + 3./4 + 4./5 + 5./6) / 5, apAtK);
        
    }

}
