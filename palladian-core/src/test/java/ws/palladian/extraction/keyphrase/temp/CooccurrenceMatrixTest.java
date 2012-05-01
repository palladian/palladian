package ws.palladian.extraction.keyphrase.temp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Philipp Katz
 */
public class CooccurrenceMatrixTest {
    @Test
    public void testCoocurrenceMatrix() {
        CooccurrenceMatrix<String> cooccurrenceMatrix = new CooccurrenceMatrix<String>();
        cooccurrenceMatrix.addAll("cooking", "apple", "zucchini");
        cooccurrenceMatrix.addAll("apple", "mac", "osx");
        cooccurrenceMatrix.addAll("apple", "newyork");
        cooccurrenceMatrix.addAll("apple", "zucchini", "recipe");
        cooccurrenceMatrix.addAll("recipe", "zucchini");

        assertEquals(1, cooccurrenceMatrix.getCount("cooking"));
        assertEquals(4, cooccurrenceMatrix.getCount("apple"));
        assertEquals(3, cooccurrenceMatrix.getCount("zucchini"));
        assertEquals(2, cooccurrenceMatrix.getCount("recipe"));

        assertEquals(4. / 13, cooccurrenceMatrix.getProbability("apple"), 0);
        assertEquals(2. / 11, cooccurrenceMatrix.getJointProbability("recipe", "zucchini"), 0);

        assertEquals(2. / 3, cooccurrenceMatrix.getConditionalProbability("recipe", "zucchini"), 0);
        assertEquals(1, cooccurrenceMatrix.getConditionalProbability("zucchini", "recipe"), 0);
        assertEquals(0, cooccurrenceMatrix.getConditionalProbability("orange", "recipe"), 0);
        assertEquals(0, cooccurrenceMatrix.getConditionalProbability("recipe", "orange"), 0);

        assertEquals(3. / 10, cooccurrenceMatrix.getConditionalProbabilityLaplace("recipe", "zucchini"), 0);
        assertEquals(3. / 9, cooccurrenceMatrix.getConditionalProbabilityLaplace("zucchini", "recipe"), 0);
        assertEquals(1. / 9, cooccurrenceMatrix.getConditionalProbabilityLaplace("orange", "recipe"), 0);
        assertEquals(1. / 7, cooccurrenceMatrix.getConditionalProbabilityLaplace("recipe", "orange"), 0);

        assertEquals("recipe", cooccurrenceMatrix.getHighest("zucchini").getLeft());
        assertEquals(3, cooccurrenceMatrix.getHighest("zucchini", 10).size());
    }

}
