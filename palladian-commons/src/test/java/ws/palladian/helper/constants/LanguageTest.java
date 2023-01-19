package ws.palladian.helper.constants;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
