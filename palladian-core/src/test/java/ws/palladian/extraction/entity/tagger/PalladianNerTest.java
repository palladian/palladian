package ws.palladian.extraction.entity.tagger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;

public class PalladianNerTest {

    @Test
    public void testRemoveDateFragment() {
        Annotation result = NerHelper.removeDateFragment(new ImmutableAnnotation(10, "June John Hiatt", StringUtils.EMPTY));
        assertEquals(15, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = NerHelper.removeDateFragment(new ImmutableAnnotation(0, "John Hiatt June", StringUtils.EMPTY));
        assertEquals(0, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = NerHelper.removeDateFragment(new ImmutableAnnotation(0, "Apr. John Hiatt", StringUtils.EMPTY));
        assertEquals(5, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());

        result = NerHelper.removeDateFragment(new ImmutableAnnotation(0, "John Hiatt Apr.", StringUtils.EMPTY));
        assertEquals(0, result.getStartPosition());
        assertEquals("John Hiatt", result.getValue());
    }

    @Test
    public void testContainsDateFragment() {
        boolean result = NerHelper.isDateFragment("June John Hiatt");
        assertFalse(result);

        result = NerHelper.isDateFragment("January");
        assertTrue(result);

        result = NerHelper.isDateFragment("JANUARY");
        assertTrue(result);

        result = NerHelper.isDateFragment("January ");
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
        PalladianNer ner = new PalladianNer(PalladianNerTrainingSettings.Builder.english().create());
        Set<String> caseDictionary = ner.buildCaseDictionary(text);
        // CollectionHelper.print(caseDictionary);
        assertEquals(33, caseDictionary.size());
        assertFalse(caseDictionary.contains("despite"));
        assertFalse(caseDictionary.contains("according"));
        assertTrue(caseDictionary.contains("involvement"));
    }
}
