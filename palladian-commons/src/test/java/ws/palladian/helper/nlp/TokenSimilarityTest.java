package ws.palladian.helper.nlp;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TokenSimilarityTest {

    private static final double DELTA = 0.001;

    private static final String S1 = "Earthquake Shakes Mexico City";
    private static final String S2 = "Panic as earthquake hits Mexico City";
    private static final String S3 = "Powerful Quake Rattles Mexico";
    private static final String S4 = "Ukraine protesters reject Geneva peace deal";
    private static final String S5 = "Ukraine calls Easter truce in east ";

    @Test
    public void testJaccardSimilarity() {
        StringSimilarity similarity = new TokenSimilarity(SetSimilarities.JACCARD);
        double sim12 = similarity.getSimilarity(S1, S2);
        assertEquals(0.4286, sim12, DELTA);
        double sim23 = similarity.getSimilarity(S2, S3);
        assertEquals(0.1111, sim23, DELTA);
        double sim13 = similarity.getSimilarity(S1, S3);
        assertEquals(0.1429, sim13, DELTA);
        double sim14 = similarity.getSimilarity(S1, S4);
        assertEquals(0, sim14, DELTA);
        double sim15 = similarity.getSimilarity(S1, S5);
        assertEquals(0, sim15, DELTA);
    }

}
