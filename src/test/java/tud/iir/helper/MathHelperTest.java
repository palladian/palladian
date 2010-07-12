package tud.iir.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import junit.framework.Assert;

import org.junit.Test;

public class MathHelperTest {

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
        double[] weights = {1.47,1.5,1.52,1.55,1.57,1.6,1.63,1.65,1.68,1.70,1.73,1.75,1.78,1.80,1.83};
        double[] heights = {52.21,53.12,54.48,55.84,57.20,58.57,59.93,61.29,63.11,64.47,66.28,68.10,69.92,72.19,74.46};
        
        double[] alphaBeta = MathHelper.performLinearRegression(weights,heights);
        Assert.assertEquals(-39.062, MathHelper.round(alphaBeta[0],3));
        Assert.assertEquals(61.272, MathHelper.round(alphaBeta[1],3));
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

        Assert.assertEquals(3.607, MathHelper.round(MathHelper.calculateRMSE("data/test/rmseInput.csv", ";"), 3));
    }

}
