package ws.palladian.retrieval.cooccurrence;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CooccurrenceMatrixTest {

    /*
     * Example data taken from "Language Modeling", Stanford NLP lecture, Dan Jurafsky, 2012.
     */
    static CooccurrenceMatrix createTestMatrix() {
        CooccurrenceMatrix m = new CooccurrenceMatrix();

        m.add("i", "i", 5).add("want", "i", 2).add("to", "i", 2).add("chinese", "i", 1).add("food", "i", 15);
        m.add("lunch", "i", 2).add("spend", "i", 2);

        m.add("i", "want", 827);

        m.add("want", "to", 608).add("to", "to", 4).add("eat", "to", 2).add("spend", "to", 1);

        m.add("i", "eat", 9).add("want", "eat", 91).add("to", "eat", 686);

        m.add("want", "chinese", 6).add("to", "chinese", 2).add("eat", "chinese", 16).add("food", "chinese", 1);

        m.add("want", "food", 6).add("eat", "food", 2).add("chinese", "food", 82).add("food", "food", 4);
        m.add("lunch", "food", 1);

        m.add("want", "lunch", 5).add("to", "lunch", 6).add("eat", "lunch", 42).add("chinese", "lunch", 1);

        m.add("i", "spend", 2).add("want", "spend", 1).add("to", "spend", 211);

        m.set("i", 2533).set("want", 927).set("to", 2417).set("eat", 746).set("chinese", 158).set("food", 1093);
        m.set("lunch", 341).set("spend", 278);
        return m;
    }

    @Test
    public void testPhraseProbs() {
        CooccurrenceMatrix m = createTestMatrix();
        assertEquals(158, m.getCount("chinese"));
        assertEquals(686, m.getCount("to", "eat"));
        assertEquals(0.0065, m.getConditionalProbability("chinese", "want"), 0.0001);
        assertEquals(0.28, m.getConditionalProbability("eat", "to"), 0.01);
        assertEquals(0.65, m.getConditionalProbability("to", "want"), 0.01);
        assertEquals(0, m.getConditionalProbability("food", "to"), 0.01);
        assertEquals(0, m.getConditionalProbability("want", "spend"), 0.01);
    }

}
