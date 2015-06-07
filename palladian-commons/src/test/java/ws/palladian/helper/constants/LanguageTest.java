package ws.palladian.helper.constants;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class LanguageTest {

    @Test
    public void testGetByName() {
        assertEquals(Language.GERMAN, Language.getByName("German"));
        assertEquals(Language.GERMAN, Language.getByName("GERMAN"));
        assertEquals(Language.GERMAN, Language.getByName("german"));
        assertNull(Language.getByName("does_not_exist"));
        assertNull(Language.getByName(null));
    }

}
