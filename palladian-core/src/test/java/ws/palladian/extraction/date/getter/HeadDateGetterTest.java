package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.MetaDate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class HeadDateGetterTest {
    
    @Test
    public void testGetHeadDates2() throws FileNotFoundException, ParserException {
        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("webPages/website104.html"));
        HeadDateGetter headDateGetter = new HeadDateGetter();
        List<MetaDate> headDates = headDateGetter.getDates(document);
        
        assertEquals(3, headDates.size());
        assertEquals("2009-01-15", headDates.get(0).getNormalizedDateString());
        assertEquals("2009-01-15 20:39", headDates.get(1).getNormalizedDateString());
        assertEquals("2009-01-16", headDates.get(2).getNormalizedDateString());
    }

    @Test
    public void testGetHeadDates() throws FileNotFoundException, ParserException {
        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm"));
        HeadDateGetter headDateGetter = new HeadDateGetter();

        List<MetaDate> dates = headDateGetter.getDates(document);
        assertEquals(6, dates.size());
        assertEquals("2010-09-03 09:43:13", dates.get(0).getNormalizedDateString());
        assertEquals("2010-09-02 06:00:00", dates.get(1).getNormalizedDateString());
        assertEquals("2010-09-03 09:44:12", dates.get(2).getNormalizedDateString());
        assertEquals("2010-09-03 09:41:54", dates.get(3).getNormalizedDateString());
        assertEquals("2010-09-03 09:43:13", dates.get(4).getNormalizedDateString());
        assertEquals("2010-09-02 06:00:00", dates.get(5).getNormalizedDateString());
    }

}
