package tud.iir.classification;

import junit.framework.Assert;

import org.junit.Test;

public class WordCorrelationMatrixTest {

    @Test
    public void testWordCorrelationMatrx() {

        WordCorrelationMatrix wcm = new WordCorrelationMatrix();

        Term term1 = new Term("sanfrancisco");
        Term term2 = new Term("cablecar");
        Term term3 = new Term("goldengatebridge");
        Term term4 = new Term("california");
        Term term5 = new Term("cali" + "fornia");

        // 5 x sanfrancisco <-> cabelcar
        wcm.updatePair(term1, term2);
        wcm.updatePair(term1, term2);
        wcm.updatePair(term1, term2);
        wcm.updatePair(term2, term1);
        wcm.updatePair(term2, term1);

        // 3 x sanfrancisco <-> goldengatebridge
        wcm.updatePair(term1, term3);
        wcm.updatePair(term1, term3);
        wcm.updatePair(term3, term1);

        // 7 x sanfrancisco <-> california
        wcm.updatePair(term4, term1);
        wcm.updatePair(term1, term4);
        wcm.updatePair(term4, term1);
        wcm.updatePair(term5, term1);
        wcm.updatePair(term1, term5);
        wcm.updatePair(term4, term1);
        wcm.updatePair(term1, term4);

        wcm.makeRelativeScores();

        // check absolute correlations
        Assert.assertEquals(5.0, wcm.getCorrelation(term1, term2).getAbsoluteCorrelation());
        Assert.assertEquals(5.0, wcm.getCorrelation(term2, term1).getAbsoluteCorrelation());
        Assert.assertEquals(3.0, wcm.getCorrelation(term1, term3).getAbsoluteCorrelation());
        Assert.assertEquals(7.0, wcm.getCorrelation(term1, term4).getAbsoluteCorrelation());
        Assert.assertEquals(7.0, wcm.getCorrelation(term1, term5).getAbsoluteCorrelation());
        // TODO can't we return a Correlation object with 0.0 when we have no correlation?
        Assert.assertEquals(null, wcm.getCorrelation(term2, term3));

        Assert.assertEquals(5.0 / 15.0, wcm.getCorrelation(term1, term2).getRelativeCorrelation());
        Assert.assertEquals(5.0 / 15.0, wcm.getCorrelation(term2, term1).getRelativeCorrelation());
        Assert.assertEquals(3.0 / 15.0, wcm.getCorrelation(term1, term3).getRelativeCorrelation());
        Assert.assertEquals(7.0 / 15.0, wcm.getCorrelation(term1, term4).getRelativeCorrelation());
        Assert.assertEquals(7.0 / 15.0, wcm.getCorrelation(term1, term5).getRelativeCorrelation());

        Assert.assertEquals(3, wcm.getCorrelations("sanfrancisco", -1).size());
        Assert.assertEquals(1, wcm.getCorrelations("cablecar", -1).size());
        Assert.assertEquals(0, wcm.getCorrelations("losangeles", -1).size());

    }

}
