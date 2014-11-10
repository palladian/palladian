package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.math.SetSimilarities;

public class CharacterNGramSimilarityTest {

    private static final double DELTA = 0.01;

    @Test
    public void testNGramSimilarity() {
        CharacterNGramSimilarity similarity = new CharacterNGramSimilarity(3, SetSimilarities.DICE);
        assertEquals(1.0, similarity.getSimilarity("", ""), DELTA);
        assertEquals(0.0, similarity.getSimilarity("", "string"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("a", "a"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("cat", "cat"), DELTA);
        assertEquals(0.727, similarity.getSimilarity("cat", "caat"), DELTA);
        assertEquals(0.0, similarity.getSimilarity("cat", "dog"), DELTA);
        assertEquals(0.824, similarity.getSimilarity("philipp", "philip"), DELTA);
        assertEquals(0.333, similarity.getSimilarity("philipp", "p"), DELTA);
        assertEquals(0.5, similarity.getSimilarity("word", "wort"), DELTA);
    }

    @Test
    public void testNGramSimilarityJaccard() {
        CharacterNGramSimilarity similarity = new CharacterNGramSimilarity(3, SetSimilarities.JACCARD);
        assertEquals(1.0, similarity.getSimilarity("", ""), DELTA);
        assertEquals(0.0, similarity.getSimilarity("", "string"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("a", "a"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("cat", "cat"), DELTA);
        assertEquals(0.571, similarity.getSimilarity("cat", "caat"), DELTA);
        assertEquals(0.0, similarity.getSimilarity("cat", "dog"), DELTA);
        assertEquals(0.7, similarity.getSimilarity("philipp", "philip"), DELTA);
        assertEquals(0.2, similarity.getSimilarity("philipp", "p"), DELTA);
        assertEquals(0.333, similarity.getSimilarity("word", "wort"), DELTA);
    }

    @Test
    public void testNGramSimilarityOverlap() {
        CharacterNGramSimilarity similarity = new CharacterNGramSimilarity(3, SetSimilarities.OVERLAP);
        assertEquals(1.0, similarity.getSimilarity("", ""), DELTA);
        assertEquals(0.0, similarity.getSimilarity("", "string"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("a", "a"), DELTA);
        assertEquals(1.0, similarity.getSimilarity("cat", "cat"), DELTA);
        assertEquals(0.8, similarity.getSimilarity("cat", "caat"), DELTA);
        assertEquals(0.0, similarity.getSimilarity("cat", "dog"), DELTA);
        assertEquals(0.875, similarity.getSimilarity("philipp", "philip"), DELTA);
        assertEquals(0.666, similarity.getSimilarity("philipp", "p"), DELTA);
        assertEquals(0.5, similarity.getSimilarity("word", "wort"), DELTA);
    }

}
