package tud.iir.classification;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tud.iir.classification.page.Stopwords;

public class StopwordsTest {

    @Test
    public void testStopwords() {

        Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);
        assertTrue(stopwords.isStopword("and"));
        assertTrue(stopwords.isStopword("AND"));

        stopwords = new Stopwords(Stopwords.Predefined.DE);
        assertTrue(stopwords.isStopword("und"));

    }

}
