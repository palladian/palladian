package ws.palladian.retrieval.wikipedia;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.wikipedia.WikipediaUtil.MarkupLocation;

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

    @Test
    public void testExtractTag() {

        List<MarkupLocation> locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|0|N|30|W|type:waterbody_scale:100000000|display=title}}");
        assertEquals(1, locations.size());

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|57|18|22|N|4|27|32|W|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|44.112|N|87.913|W|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|44.112|-87.913|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|44.117|-87.913|dim:30_region:US-WI_type:event|display=inline,title|name=accident site}}");
        assertEquals(1, locations.size());
        assertEquals("inline,title", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{coord|61.1631|-149.9721|type:landmark_globe:earth_region:US-AK_scale:150000_source:gnis|name=Kulis Air National Guard Base}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);

        locations = WikipediaUtil.extractCoordinateTag("{{coord|46|43|N|7|58|E|type:waterbody}}");
        assertEquals(1, locations.size());
        assertEquals("waterbody", locations.get(0).type);

        locations = WikipediaUtil.extractCoordinateTag("{{coord|51.501|-0.142|dim:120m}}");
        assertEquals(1, locations.size());

        locations = WikipediaUtil.extractCoordinateTag("{{coord|51.507222|-0.1275|dim:10km}}");
        assertEquals(1, locations.size());

        locations = WikipediaUtil.extractCoordinateTag("{{coord|51.500611|N|0.124611|W|scale:500}}");
        assertEquals(1, locations.size());

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|51|28|N|9|25|W|region:IE_type:isle|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|40|43|N|74|0|W|region:US-NY|display=inline}}");
        assertEquals(1, locations.size());
        assertEquals("inline", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|51|1|41|N|13|43|36|E|type:edu_region:DE-SN|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{coord|51|3|7|N|13|44|30|E|display=it|region:DE_type:landmark}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("it", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|38.89767|-77.03655|region:US-DC_type:landmark|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("title", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|40|43|N|74|0|W|region:US-NY|display=inline}}");
        assertEquals(1, locations.size());
        assertEquals("inline", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|40|44|54.36|N|73|59|08.36|W|region:US-NY_type:landmark|name=Empire State Building|display=inline,title}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("inline,title", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{coord|22|S|43|W}}");
        assertEquals(1, locations.size());

        locations = WikipediaUtil
                .extractCoordinateTag("{{coord|52|28|N|1|55|W|region:GB_type:city|notes=<ref>{{cite web|url=http://www.fallingrain.com/world/UK/0/Birmingham.html|title=Birmingham}}</ref>|display=inline,title}}");
        assertEquals(1, locations.size());
        assertEquals("city", locations.get(0).type);
        assertEquals("inline,title", locations.get(0).display);

        locations = WikipediaUtil
                .extractCoordinateTag("{{Coord|51|30|N|9|26|W|region:IE_type:isle|display=title,inline}}");
        assertEquals(1, locations.size());
        assertEquals("isle", locations.get(0).type);
        assertEquals("title,inline", locations.get(0).display);

        locations = WikipediaUtil.extractCoordinateTag("{{Coord|43.0909158|-79.0759206|display=t|type:landmark}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("t", locations.get(0).display);
    }
}
