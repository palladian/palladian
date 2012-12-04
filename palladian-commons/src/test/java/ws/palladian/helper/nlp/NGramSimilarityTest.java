package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class NGramSimilarityTest {
    
    @Test
    public void testNGramSimilarity() {
        NGramSimilarity similarity = new NGramSimilarity(3);
        assertEquals(0.0, similarity.getSimilarity("", "string"), 0.01);
        assertEquals(1.0, similarity.getSimilarity("a", "a"),0.01);
        assertEquals(1.0, similarity.getSimilarity("cat", "cat"),0.01);
        assertEquals(0.727, similarity.getSimilarity("cat", "caat"),0.01);
        assertEquals(0.0, similarity.getSimilarity("cat", "dog"),0.01);
        assertEquals(0.824, similarity.getSimilarity("philipp", "philip"),0.01);
        assertEquals(0.333, similarity.getSimilarity("philipp", "p"),0.01);
        assertEquals(0.5, similarity.getSimilarity("word", "wort"),0.01);
    }

}
