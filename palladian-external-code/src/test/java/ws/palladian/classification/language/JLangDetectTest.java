package ws.palladian.classification.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import me.champeau.ld.EuroparlDetector;

import org.junit.Test;

import ws.palladian.helper.constants.Language;

public class JLangDetectTest {

    @Test
    public void testJLangDetect() {
        JLangDetect jLangDetect = new JLangDetect(EuroparlDetector.getInstance());
        assertEquals(Language.ENGLISH, jLangDetect.classify("hello, world!"));
        assertEquals(Language.GERMAN, jLangDetect.classify("grüß gott"));
        assertEquals(Language.FRENCH, jLangDetect.classify("bonjour"));
        assertEquals(Language.ITALIAN, jLangDetect.classify("ciao bella"));
        assertNull(jLangDetect.classify("こんにちは")); // return null because we have no japanese gram tree.
    }

}
