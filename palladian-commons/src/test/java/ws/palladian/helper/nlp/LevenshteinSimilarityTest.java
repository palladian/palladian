package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LevenshteinSimilarityTest {

    @Test
    public void testLevenshteinSimilarity() {
        LevenshteinSimilarity similarity = new LevenshteinSimilarity();
        assertEquals(0.64, similarity.getSimilarity("Levenshtein", "Lenvinsten"), 0.01);
        assertEquals(0.82, similarity.getSimilarity("Levenshtein", "Levensthein"), 0.01);
        assertEquals(0.91, similarity.getSimilarity("Levenshtein", "Levenshten"), 0.01);
        assertEquals(1, similarity.getSimilarity("Levenshtein", "Levenshtein"), 0.01);
        assertEquals(1, similarity.getSimilarity("", ""), 0.01);
    }

}
