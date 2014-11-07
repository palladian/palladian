package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

@Deprecated
public class GoogleSpellCheckerTest {

    @Ignore
    @Test
    public void testSpellCheck() {

        GoogleSpellChecker gsc = new GoogleSpellChecker();

        assertTrue(gsc.containsErrors("errrors in here!!!"));
        assertTrue(gsc.containsErrors("are there anny errors here?"));
        assertTrue(gsc.containsErrors("GERMANY is nott an error"));
        assertFalse(gsc.containsErrors("no errors here"));
        assertFalse(gsc.containsErrors("zero problem in this beautifully written text"));

        assertEquals("This is how the text is supposed to be",
                gsc.autoCorrect("Thas is hoow the etxt is sopossed to be"));
        assertEquals("Cool, it seems to work", gsc.autoCorrect("Coool, il seamss to workk"));
    }

}
