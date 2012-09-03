package ws.palladian.classification;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.io.FileHelper;

/**
 * 
 * Example for the test case:
 * 
 * <pre>
 * 
 *                          +--------------+----------+------------------+------------+--------+
 *         Absolute values  | sanfrancisco | cablecar | goldengatebridge | california | rowSum |
 *   +---+------------------+--------------+----------+------------------+------------+--------+
 *   | 1 | sanfrancisco     |              |    5     |        3         |     7      |   15   |
 *   | 2 | cablecar         |      5       |          |        2         |            |    7   |
 *   | 3 | goldengatebridge |      3       |    2     |                  |            |    5   |
 *   | 4 | california       |      7       |          |                  |            |    7   |
 *   +---+------------------+--------------+----------+------------------+------------+--------+
 *   
 *   calculation of relative values:
 *   
 *                           rel(t1, t2) = abs(t1, t2) / ( rowSum(t1) + rowSum(t2) - abs(t1, t2) )
 *   
 *           rel(sanfrancisco, cablecar) = 5 / (15 + 7 - 5) = 0.294
 *   rel(sanfrancisco, goldengatebridge) = 3 / (15 + 5 - 3) = 0.176
 *         rel(sanfrancisco, california) = 7 / (15 + 7 - 7) = 0.467
 *       rel(cablecar, goldengatebridge) = 2 / (7 + 5 - 2)  = 0.2
 *   
 *   
 *                          +--------------+----------+------------------+------------+
 *         Relative values  | sanfrancisco | cablecar | goldengatebridge | california |
 *   +---+------------------+--------------+----------+------------------+------------+
 *   | 1 | sanfrancisco     |              |   0.294  |       0.176      |    0.467   |
 *   | 2 | cablecar         |    0.294     |          |       0.2        |            |
 *   | 3 | goldengatebridge |    0.176     |   0.2    |                  |            |
 *   | 4 | california       |    0.467     |          |                  |            |
 *   +---+------------------+--------------+----------+------------------+------------+
 * </pre>
 * 
 * @author Philipp Katz
 * 
 */
public class WordCorrelationMatrixTest {

    private WordCorrelationMatrix wcm;

    private Term term1 = new Term("sanfrancisco");
    private Term term2 = new Term("cablecar");
    private Term term3 = new Term("goldengatebridge");
    private Term term4 = new Term("california");
    private Term term5 = new Term("cali" + "fornia"); // to test proper handling of String equality

    /** get the class under test. */
    protected WordCorrelationMatrix getMatrix() {
        return new WordCorrelationMatrix();
    }

    @Before
    public void setUpMatrix() {

        wcm = getMatrix();

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

        // 2 x cablecar <-> goldengatebridge
        wcm.updatePair(term2, term3);
        wcm.updatePair(term3, term2);

        wcm.makeRelativeScores();

    }

    @Test
    public void testWordCorrelationMatrix() {

        // check absolute correlations
        assertEquals(5.0, wcm.getCorrelation(term1, term2).getAbsoluteCorrelation(), 0);
        assertEquals(5.0, wcm.getCorrelation(term2, term1).getAbsoluteCorrelation(), 0);
        assertEquals(3.0, wcm.getCorrelation(term1, term3).getAbsoluteCorrelation(), 0);
        assertEquals(7.0, wcm.getCorrelation(term1, term4).getAbsoluteCorrelation(), 0);
        assertEquals(7.0, wcm.getCorrelation(term1, term5).getAbsoluteCorrelation(), 0);
        assertEquals(2.0, wcm.getCorrelation(term2, term3).getAbsoluteCorrelation(), 0);
        // TODO can't we return a Correlation object with 0.0 when we have no correlation?
        assertEquals(null, wcm.getCorrelation(term3, term4));

        assertEquals(5.0 / (15.0 + 7.0 - 5.0), wcm.getCorrelation(term2, term1).getRelativeCorrelation(), 0);
        assertEquals(5.0 / (15.0 + 7.0 - 5.0), wcm.getCorrelation(term1, term2).getRelativeCorrelation(), 0);
        assertEquals(3.0 / (15.0 + 5.0 - 3.0), wcm.getCorrelation(term1, term3).getRelativeCorrelation(), 0);
        assertEquals(7.0 / (15.0 + 7.0 - 7.0), wcm.getCorrelation(term1, term4).getRelativeCorrelation(), 0);
        assertEquals(7.0 / (15.0 + 7.0 - 7.0), wcm.getCorrelation(term1, term5).getRelativeCorrelation(), 0);
        assertEquals(2.0 / (7.0 + 5.0 - 2.0), wcm.getCorrelation(term2, term3).getRelativeCorrelation(), 0);

        assertEquals(3, wcm.getCorrelations("sanfrancisco", -1).size());
        assertEquals(2, wcm.getCorrelations("cablecar", -1).size());
        assertEquals(1, wcm.getCorrelations("california", -1).size());
        assertEquals(0, wcm.getCorrelations("losangeles", -1).size());

        // we have 4 correlations in total (term1, term2) == (term2, term1)
        assertEquals(4, wcm.getCorrelations().size());

    }

    @Test
    public void testWordCorrelationMatrixSerialization() {

        String fileName = "data/tmp_wcm_test_" + System.currentTimeMillis() + ".ser";
        FileHelper.serialize(wcm, fileName);

        WordCorrelationMatrix deserialized = (WordCorrelationMatrix) FileHelper.deserialize(fileName);
        assertEquals(5.0 / (15.0 + 7.0 - 5.0), deserialized.getCorrelation(term1, term2).getRelativeCorrelation(), 0);

        // clean up
        FileHelper.delete(fileName);
    }

}
