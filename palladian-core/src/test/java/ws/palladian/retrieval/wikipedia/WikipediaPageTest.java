package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaPageTest {

    @Test
    public void testWikipediaPage() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        assertEquals("german location", page.getInfoboxType());
        assertEquals(4, page.getCategories().size());
        assertEquals(484, page.getLinks().size());
    }

}
