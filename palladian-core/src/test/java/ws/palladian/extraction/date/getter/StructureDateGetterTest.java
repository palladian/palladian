package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.date.dates.StructureDate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class StructureDateGetterTest {
    
    // TODO further test: http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html 
    // TODO further test: http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu 
    @Test
    public void testGetStructureDate() throws FileNotFoundException, ParserException {
        File testPage = ResourceHelper.getResourceFile("/webPages/webPageW3C.htm");

        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(testPage);

        StructureDateGetter structureDateGetter = new StructureDateGetter();
        List<StructureDate> dates = structureDateGetter.getBodyStructureDates(document);

        assertEquals(7, dates.size());
        assertEquals("2010-07-08T08:02:04-05:00", dates.get(0).getDateString());
        assertEquals("2010-07-20T11:50:47-05:00", dates.get(1).getDateString());
        assertEquals("2010-07-13T14:55:57-05:00", dates.get(2).getDateString());
        assertEquals("2010-07-13T14:46:56-05:00", dates.get(3).getDateString());
        assertEquals("2010-07-20", dates.get(4).getDateString());
        assertEquals("2010-07-16", dates.get(5).getDateString());
        assertEquals("2010-07-07", dates.get(6).getDateString());
        
        testPage = ResourceHelper.getResourceFile("/webPages/website103.html");
        document = htmlParser.parse(testPage);
        dates = structureDateGetter.getBodyStructureDates(document);
        
        // assertEquals(1, dates.size());
        assertEquals("2002-08-06T03:08", dates.get(0).getDateString());
    }

}
