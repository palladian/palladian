package ws.palladian.extraction.location;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

public class GeonamesLocationSourceTest {

    @Test
    public void testParse() throws Exception {
        DocumentParser xmlParser = ParserFactory.createXmlParser();
        Document document = xmlParser.parse(ResourceHelper.getResourceFile("/apiResponse/geonamesResult.xml"));
        List<Location> result = GeonamesLocationSource.parseLocations(document);

        assertEquals(7, result.size());
        Location location = CollectionHelper.getLast(result);
        assertEquals("Stuttgart", location.getPrimaryName());
        assertEquals((Double)48.78232, location.getLatitude());
        assertEquals((Double)9.17702, location.getLongitude());
        assertEquals(2825297, location.getId());
    }

}
