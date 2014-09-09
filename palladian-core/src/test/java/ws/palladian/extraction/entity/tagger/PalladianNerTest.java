package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class PalladianNerTest {

    @Test
    public void testRemoveDateFragment() {
        Pair<String, Integer> result = PalladianNer.removeDateFragment("June John Hiatt");
        assertEquals(5, (int)result.getRight());
        assertEquals("John Hiatt", result.getLeft());

        result = PalladianNer.removeDateFragment("John Hiatt June");
        assertEquals(0, (int)result.getRight());
        assertEquals("John Hiatt", result.getLeft());

        result = PalladianNer.removeDateFragment("Apr. John Hiatt");
        assertEquals(5, (int)result.getRight());
        assertEquals("John Hiatt", result.getLeft());

        result = PalladianNer.removeDateFragment("John Hiatt Apr.");
        assertEquals(0, (int)result.getRight());
        assertEquals("John Hiatt", result.getLeft());
    }

    @Test
    public void testContainsDateFragment() {
        boolean result = PalladianNer.isDateFragment("June John Hiatt");
        assertFalse(result);

        result = PalladianNer.isDateFragment("January");
        assertTrue(result);

        result = PalladianNer.isDateFragment("JANUARY");
        assertTrue(result);

        result = PalladianNer.isDateFragment("January ");
        assertTrue(result);
    }

}
