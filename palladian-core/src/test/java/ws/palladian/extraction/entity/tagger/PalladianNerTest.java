package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ws.palladian.extraction.entity.ContextAnnotation;

public class PalladianNerTest {

    @Test
    public void testRemoveDateFragment() {
        ContextAnnotation result = PalladianNer.removeDateFragment(new ContextAnnotation(10, "June John Hiatt",
                StringUtils.EMPTY));
        assertEquals(15, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ContextAnnotation(0, "John Hiatt June", StringUtils.EMPTY));
        assertEquals(0, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ContextAnnotation(0, "Apr. John Hiatt", StringUtils.EMPTY));
        assertEquals(5, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ContextAnnotation(0, "John Hiatt Apr.", StringUtils.EMPTY));
        assertEquals(0, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());
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
