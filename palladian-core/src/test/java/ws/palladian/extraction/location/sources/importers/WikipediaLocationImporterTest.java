package ws.palladian.extraction.location.sources.importers;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.location.Location;
import ws.palladian.extraction.location.LocationType;
import ws.palladian.extraction.location.sources.CollectionLocationStore;
import ws.palladian.extraction.location.sources.LocationStore;
import ws.palladian.extraction.location.sources.importers.WikipediaLocationImporter.MarkupLocation;
import ws.palladian.helper.io.ResourceHelper;

public class WikipediaLocationImporterTest {

    @Test
    public void testImport() throws FileNotFoundException, Exception {
        LocationStore locationStore = new CollectionLocationStore();
        WikipediaLocationImporter importer = new WikipediaLocationImporter(locationStore);
        importer.importLocationPages(ResourceHelper.getResourceStream("/apiResponse/WikipediaPagesDump.xml"));
        importer.importAlternativeNames(ResourceHelper.getResourceStream("/apiResponse/WikipediaPagesDump.xml"));

        Location location = locationStore.getLocation(564258);
        assertEquals("Sherkin Island", location.getPrimaryName());
        assertEquals(LocationType.LANDMARK, location.getType());
        assertEquals(51.466667, location.getLatitude(), 0.0001);
        assertEquals(-9.416667, location.getLongitude(), 0.0001);
        assertEquals(1, location.getAlternativeNames().size());
        assertEquals("Sherkin", location.getAlternativeNames().iterator().next().getName());

        location = locationStore.getLocation(1227);
        assertEquals("Ashmore and Cartier Islands", location.getPrimaryName());
        assertEquals(-12.258333, location.getLatitude(), 0.0001);
        assertEquals(123.041667, location.getLongitude(), 0.0001);
        assertEquals(0, location.getAlternativeNames().size());
    }

    @Test
    public void testExtractTag() {

        List<MarkupLocation> locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|0|N|30|W|type:waterbody_scale:100000000|display=title}}");
        assertEquals(1, locations.size());

        locations = WikipediaLocationImporter.extractCoordinateTag("{{Coord|57|18|22|N|4|27|32|W|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter.extractCoordinateTag("{{Coord|44.112|N|87.913|W|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter.extractCoordinateTag("{{Coord|44.112|-87.913|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|44.117|-87.913|dim:30_region:US-WI_type:event|display=inline,title|name=accident site}}");
        assertEquals(1, locations.size());
        assertEquals("inline,title", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{coord|61.1631|-149.9721|type:landmark_globe:earth_region:US-AK_scale:150000_source:gnis|name=Kulis Air National Guard Base}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);

        locations = WikipediaLocationImporter.extractCoordinateTag("{{coord|46|43|N|7|58|E|type:waterbody}}");
        assertEquals(1, locations.size());
        assertEquals("waterbody", locations.get(0).type);

        locations = WikipediaLocationImporter.extractCoordinateTag("{{coord|51.501|-0.142|dim:120m}}");
        assertEquals(1, locations.size());

        locations = WikipediaLocationImporter.extractCoordinateTag("{{coord|51.507222|-0.1275|dim:10km}}");
        assertEquals(1, locations.size());

        locations = WikipediaLocationImporter.extractCoordinateTag("{{coord|51.500611|N|0.124611|W|scale:500}}");
        assertEquals(1, locations.size());

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|51|28|N|9|25|W|region:IE_type:isle|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|40|43|N|74|0|W|region:US-NY|display=inline}}");
        assertEquals(1, locations.size());
        assertEquals("inline", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|51|1|41|N|13|43|36|E|type:edu_region:DE-SN|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{coord|51|3|7|N|13|44|30|E|display=it|region:DE_type:landmark}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("it", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|38.89767|-77.03655|region:US-DC_type:landmark|display=title}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("title", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|40|43|N|74|0|W|region:US-NY|display=inline}}");
        assertEquals(1, locations.size());
        assertEquals("inline", locations.get(0).display);

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{Coord|40|44|54.36|N|73|59|08.36|W|region:US-NY_type:landmark|name=Empire State Building|display=inline,title}}");
        assertEquals(1, locations.size());
        assertEquals("landmark", locations.get(0).type);
        assertEquals("inline,title", locations.get(0).display);

        locations = WikipediaLocationImporter.extractCoordinateTag("{{coord|22|S|43|W}}");
        assertEquals(1, locations.size());

        locations = WikipediaLocationImporter
                .extractCoordinateTag("{{coord|52|28|N|1|55|W|region:GB_type:city|notes=<ref>{{cite web|url=http://www.fallingrain.com/world/UK/0/Birmingham.html|title=Birmingham}}</ref>|display=inline,title}}");
        assertEquals(1, locations.size());
        assertEquals("city", locations.get(0).type);
        assertEquals("inline,title", locations.get(0).display);
    }

    @Test
    public void testCleanName() {
        assertEquals("Theater District",
                WikipediaLocationImporter.cleanName("Theater District (San Francisco, California)"));
        assertEquals("Oregon", WikipediaLocationImporter.cleanName("Oregon, Illinois"));
        assertEquals("West Seneca", WikipediaLocationImporter.cleanName("West Seneca (town), New York"));
        assertEquals("Capital of the Cocos Islands",
                WikipediaLocationImporter.cleanName("Capital of the Cocos (Keeling) Islands"));
    }

}
