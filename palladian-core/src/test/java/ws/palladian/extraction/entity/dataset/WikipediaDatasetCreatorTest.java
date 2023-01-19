package ws.palladian.extraction.entity.dataset;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ws.palladian.extraction.entity.dataset.WikipediaDatasetCreator.cleanPersonName;
import static ws.palladian.extraction.entity.dataset.WikipediaDatasetCreator.getUcTokenPercentage;

public class WikipediaDatasetCreatorTest {
    @Test
    public void testUcPercentage() {
        assertEquals(5. / 7, getUcTokenPercentage("White House Council on Women and Girls"), 0);
        assertEquals(1. / 4, getUcTokenPercentage("ended U.S. military involvement"), 0);
        assertEquals(2. / 5, getUcTokenPercentage("U.S. military involvement in Libya"), 0);
        assertEquals(1, getUcTokenPercentage("Wichita, Kansas"), 0);
        assertEquals(1, getUcTokenPercentage("Punahou School"), 0);
        assertEquals(3. / 4, getUcTokenPercentage("Dreams from My Father"), 0);
        assertEquals(0, getUcTokenPercentage("current"), 0);
        assertEquals(3. / 4, getUcTokenPercentage("The Audacity of Hope"), 0);
    }

    @Test
    public void testCleanPersonName() {
        assertEquals("Jasper Johns", cleanPersonName("Jasper Johns, Jr."));
        assertEquals("William King", cleanPersonName("William King"));
    }
}
