package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.wikipedia.WikipediaPage.WikipediaInfobox;

public class WikipediaPageTest {

    @Test
    public void testWikipediaPage() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        assertEquals("german location", page.getInfoboxType());
        assertEquals(4, page.getCategories().size());
        assertEquals(484, page.getLinks().size());
    }

    @Test
    public void testGetInfoboxes() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("/wikipedia/Dry_Fork_(Cheat_River).wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dry Fork (Cheat River)", markup);
        List<WikipediaInfobox> infoboxes = page.getInfoboxes();
        assertEquals(1, infoboxes.size());
        assertEquals("river", infoboxes.get(0).getName());
    }

}
