package ws.palladian.retrieval.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;

import org.json.JSONException;
import org.junit.Ignore;
import org.junit.Test;

public class GoogleSpellCheckerTest {

    // FIXME what's up with these Google guys?
    @Ignore
    @Test
    public void testSpellCheck() throws FileNotFoundException, JSONException {

        GoogleSpellChecker gsc = new GoogleSpellChecker();

        assertTrue(gsc.containsErrors("errrors in here!!!"));
        assertTrue(gsc.containsErrors("are there anny errors here?"));
        assertTrue(gsc.containsErrors("GERMANY is nott an error"));
        assertFalse(gsc.containsErrors("no errors here"));
        assertFalse(gsc.containsErrors("zero problem in this beautifully written text"));

        assertEquals("This is how the text is supposed to be",gsc.autoCorrect("Thas is hoow the etxt is sopossed to be"));
        assertEquals("Cool, it seems to work",gsc.autoCorrect("Coool, il seamss to workk"));
    }

}
