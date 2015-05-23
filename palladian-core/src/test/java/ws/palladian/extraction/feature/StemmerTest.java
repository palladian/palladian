package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.constants.Language;

public class StemmerTest {
    @Test
    public void testStemmer() {
        Stemmer stemmer = new Stemmer(Language.ENGLISH);
        assertEquals("walk", stemmer.stem("walk"));
        assertEquals("walk", stemmer.stem("walked"));
        assertEquals("walk", stemmer.stem("walking"));
    }

}
