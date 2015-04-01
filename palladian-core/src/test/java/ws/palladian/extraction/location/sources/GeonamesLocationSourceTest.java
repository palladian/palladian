package ws.palladian.extraction.location.sources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.location.AlternativeName;
import ws.palladian.extraction.location.Location;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.geo.GeoCoordinate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

public class GeonamesLocationSourceTest {

    private final DocumentParser xmlParser = ParserFactory.createXmlParser();

    @Test
    public void testParse() throws Exception {
        Document document = xmlParser.parse(ResourceHelper.getResourceFile("/apiResponse/geonamesResult.xml"));
        List<Location> result = GeonamesLocationSource.parseLocations(document);

        assertEquals(8, result.size());
        Location location = CollectionHelper.getLast(result);
        assertEquals("Stuttgart", location.getPrimaryName());
        GeoCoordinate coordinate = location.getCoordinate();
        assertNotNull(coordinate);
        assertEquals(48.78232, coordinate.getLatitude(), 0);
        assertEquals(9.17702, coordinate.getLongitude(), 0);
        assertEquals(2825297, location.getId());
        assertEquals(58, location.getAlternativeNames().size());
        assertTrue(location.getAlternativeNames().contains(new AlternativeName("Stoccarda", Language.ITALIAN)));
    }

    @Test(expected = IllegalStateException.class)
    public void testParseLimitExceeded() throws Exception {
        Document document = xmlParser.parse(ResourceHelper.getResourceFile("/apiResponse/geonamesResultExceeded.xml"));
        GeonamesLocationSource.parseLocations(document);
    }

}
