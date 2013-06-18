package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.Map;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaUtilTest {

    @Test
    public void testCleanName() {
        assertEquals("Theater District", WikipediaUtil.cleanTitle("Theater District (San Francisco, California)"));
        assertEquals("Oregon", WikipediaUtil.cleanTitle("Oregon, Illinois"));
        assertEquals("West Seneca", WikipediaUtil.cleanTitle("West Seneca (town), New York"));
        assertEquals("Capital of the Cocos Islands", WikipediaUtil.cleanTitle("Capital of the Cocos (Keeling) Islands"));
    }

    @Test
    public void testGetRedirect() {
        assertEquals("Los Angeles", WikipediaUtil.getRedirect("#REDIRECT [[Los Angeles]]"));
    }

    @Test
    public void testInfoboxExtraction() throws FileNotFoundException {
        String markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Dresden.wikipedia"));
        WikipediaPage page = new WikipediaPage(0, 0, "Dresden", markup);
        Map<String, String> data = WikipediaUtil.extractInfobox(page.getInfoboxMarkup());
        // CollectionHelper.print(data);
        assertEquals(34, data.size());
        assertEquals("Dresden", data.get("Name"));
        assertEquals("City", data.get("Art"));
        assertEquals("Dresden-Altstadt von der Marienbruecke-II.jpg", data.get("image_photo"));
        assertEquals("300px", data.get("imagesize"));
        assertEquals("", data.get("image_caption"));
        // ...

        markup = FileHelper.readFileToString(ResourceHelper.getResourceFile("/Stack_Overflow.wikipedia"));
        page = new WikipediaPage(0, 0, "Stack Overflow", markup);
        data = WikipediaUtil.extractInfobox(page.getInfoboxMarkup());
        // CollectionHelper.print(data);
        assertEquals(17, data.size());
        assertEquals(
                "84 ({{as of|2013|02|15|alt=February 2013}})<ref name=\"alexa\">{{cite web|url= http://www.alexa.com/siteinfo/stackoverflow.com |title= Stackoverflow.com Site Info | publisher= [[Alexa Internet]] |accessdate= 2013-02-15 }}</ref><!--Updated monthly by OKBot.-->",
                data.get("alexa"));
    }
}
