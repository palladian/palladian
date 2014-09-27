package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ws.palladian.classification.text.DictionaryModel;
import ws.palladian.core.Annotation;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableAnnotation;

public class PalladianNerTest {

    @Test
    public void testRemoveDateFragment() {
        Annotation result = PalladianNer.removeDateFragment(new ImmutableAnnotation(10, "June John Hiatt",
                StringUtils.EMPTY));
        assertEquals(15, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ImmutableAnnotation(0, "John Hiatt June", StringUtils.EMPTY));
        assertEquals(0, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ImmutableAnnotation(0, "Apr. John Hiatt", StringUtils.EMPTY));
        assertEquals(5, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = PalladianNer.removeDateFragment(new ImmutableAnnotation(0, "John Hiatt Apr.", StringUtils.EMPTY));
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

    @Test
    public void testGetLeftContexts() {
        String text = "Blistering heat blanketed much of the eastern United States for the third straight day on Sunday, after violent storms that took at least a dozen lives and knocked out power to more than 3 million customers.";
        Annotation annotation = new ImmutableAnnotation(46, "United States");
        List<String> leftContexts = NerHelper.getLeftContexts(annotation, text, 3);
        assertEquals(3, leftContexts.size());
        assertEquals("eastern", leftContexts.get(0));
        assertEquals("the eastern", leftContexts.get(1));
        assertEquals("of the eastern", leftContexts.get(2));
    }
    
    @Test
    public void testBuildCaseDictionary() {
        String text = "Despite their shared upbringing and involvement in the Spanish Treason, the conspirators chose not to reveal the plot to him until 14 October 1605, shortly after his father died, and just weeks before the planned explosion. According to his confession, the meeting took place at the home of Tresham's brother-in-law, Lord Stourton, in Clerkenwell.";
        DictionaryModel caseDictionary = PalladianNer.buildCaseDictionary(text);
        // CollectionHelper.print(caseDictionary);
        assertEquals(40, caseDictionary.getNumUniqTerms());
        assertTrue(caseDictionary.getCategoryEntries("despite") == CategoryEntries.EMPTY);
        assertTrue(caseDictionary.getCategoryEntries("according") == CategoryEntries.EMPTY);
    }

}
