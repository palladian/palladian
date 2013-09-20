package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaPageTest {

    @Test
    public void testWikipediaPage() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        assertEquals("german location", page.getInfoboxes().get(0).getName());
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

    @Test
    public void testInfoboxExtraction() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        List<WikipediaInfobox> infoboxes = page.getInfoboxes();
        assertEquals(1, infoboxes.size());
        WikipediaInfobox infobox = CollectionHelper.getFirst(infoboxes);
        assertEquals(34, infobox.size());
        assertEquals("Dresden", infobox.getEntry("Name"));
        assertEquals("City", infobox.getEntry("Art"));
        assertEquals("Dresden-Altstadt von der Marienbruecke-II.jpg", infobox.getEntry("image_photo"));
        assertEquals("300px", infobox.getEntry("imagesize"));
        assertEquals("", infobox.getEntry("image_caption"));
        // ...
        assertEquals("1206", infobox.getEntry("year"));

        markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Stack_Overflow.wikipedia"));
        page = new WikipediaPage(0, 0, "Stack Overflow", markup);
        infoboxes = page.getInfoboxes();
        assertEquals(1, infoboxes.size());
        infobox = CollectionHelper.getFirst(infoboxes);
        assertEquals(17, infobox.size());
        assertEquals(
                "84 ({{as of|2013|02|15|alt=February 2013}})<ref name=\"alexa\">{{cite web|url= http://www.alexa.com/siteinfo/stackoverflow.com |title= Stackoverflow.com Site Info | publisher= [[Alexa Internet]] |accessdate= 2013-02-15 }}</ref>",
                infobox.getEntry("alexa"));

        markup = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("/wikipedia/Dry_Fork_(Cheat_River).wikipedia"));
        page = new WikipediaPage(0, 0, "Dry Fork (Cheat River)", markup);
        infoboxes = page.getInfoboxes();
        assertEquals(1, infoboxes.size());
        infobox = CollectionHelper.getFirst(infoboxes);
        assertEquals("river", infobox.getName());
        assertEquals(70, infobox.size());

        markup = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("/wikipedia/Muskingum_University.wikipedia"));
        page = new WikipediaPage(0, 0, "Muskingum University", markup);
        infoboxes = page.getInfoboxes();
        assertEquals(2, infoboxes.size());
    }
    

    @Test
    public void testGetSections() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        List<String> sections = page.getSections();
        assertEquals(46, sections.size());
    }
    

    @Test
    public void testGetRedirect() {
        WikipediaPage page = new WikipediaPage(0, 0, "L.A.", "#REDIRECT [[Los Angeles]]");
        assertEquals("Los Angeles", page.getRedirectTitle());
    }

}
