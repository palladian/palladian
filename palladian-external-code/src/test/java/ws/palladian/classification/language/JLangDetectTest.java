package ws.palladian.classification.language;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import me.champeau.ld.EuroparlDetector;

import org.junit.Test;

public class JLangDetectTest {

    @Test
    public void testJLangDetect() {
        JLangDetect jLangDetect = new JLangDetect(EuroparlDetector.getInstance());
        assertEquals("en", jLangDetect.classify("hello, world!"));
        assertEquals("de", jLangDetect.classify("grüß gott"));
        assertEquals("fr", jLangDetect.classify("bonjour"));
        assertEquals("it", jLangDetect.classify("ciao bella"));
        assertNull(jLangDetect.classify("こんにちは")); // return null because we have no japanese gram tree.
    }

}
