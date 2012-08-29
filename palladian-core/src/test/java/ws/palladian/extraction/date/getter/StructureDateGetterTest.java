package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.StructureDate;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

public class StructureDateGetterTest {
    
    private final DocumentParser htmlParser = ParserFactory.createHtmlParser();
    private StructureDateGetter structureDateGetter;
    
    @Before
    public void setUp() {
        this.structureDateGetter = new StructureDateGetter();
    }

    @Test
    public void testGetStructureDates1() throws Exception {
        File testPage = ResourceHelper.getResourceFile("/webPages/webPageW3C.htm");

        Document document = htmlParser.parse(testPage);

        List<StructureDate> dates = structureDateGetter.getDates(document);

        assertEquals(7, dates.size());
        assertEquals("2010-07-08T08:02:04-05:00", dates.get(0).getDateString());
        assertEquals("published", dates.get(0).getKeyword());
        assertEquals("span", dates.get(0).getTag());
        assertEquals(12, dates.get(0).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-20T11:50:47-05:00", dates.get(1).getDateString());
        assertEquals("published", dates.get(1).getKeyword());
        assertEquals("span", dates.get(1).getTag());
        assertEquals(12, dates.get(1).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-13T14:55:57-05:00", dates.get(2).getDateString());
        assertEquals("published", dates.get(2).getKeyword());
        assertEquals("span", dates.get(2).getTag());
        assertEquals(12, dates.get(2).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-13T14:46:56-05:00", dates.get(3).getDateString());
        assertEquals("published", dates.get(3).getKeyword());
        assertEquals("span", dates.get(3).getTag());
        assertEquals(12, dates.get(3).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-20", dates.get(4).getDateString());
        assertEquals("published", dates.get(4).getKeyword());
        assertEquals("abbr", dates.get(4).getTag());
        assertEquals(10, dates.get(4).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-16", dates.get(5).getDateString());
        assertEquals("published", dates.get(5).getKeyword());
        assertEquals("abbr", dates.get(5).getTag());
        assertEquals(10, dates.get(5).get(StructureDate.STRUCTURE_DEPTH));
        
        assertEquals("2010-07-07", dates.get(6).getDateString());
        assertEquals("published", dates.get(6).getKeyword());
        assertEquals("abbr", dates.get(6).getTag());
        assertEquals(10, dates.get(6).get(StructureDate.STRUCTURE_DEPTH));
        
    }
    
    @Test
    public void testGetStructureDates2() throws Exception {

        File testPage = ResourceHelper.getResourceFile("/webPages/website103.html");
        Document document = htmlParser.parse(testPage);
        List<StructureDate> dates = structureDateGetter.getDates(document);

        assertEquals(2, dates.size());
        assertEquals("2002-08-06T03:08", dates.get(0).getDateString());
        assertEquals("2002-08-06T00:00", dates.get(1).getDateString());
        
    }
    
    @Test
    public void testGetStructureDates3() throws Exception {

        // http://www.spiegel.de/schulspiegel/wissen/0,1518,706953,00.html
        File testPage = ResourceHelper.getResourceFile("/webPages/dateExtraction/spiegel.html");
        Document document = htmlParser.parse(testPage);

        List<StructureDate> dates = structureDateGetter.getDates(document);
        assertEquals(1, dates.size());
        assertEquals("2010-07-18 09:32:01", dates.get(0).getNormalizedDateString());
        assertEquals("published", dates.get(0).getKeyword());
        assertEquals(6, dates.get(0).get(StructureDate.STRUCTURE_DEPTH));
        assertEquals("[div: null]", dates.get(0).getTagNode());
        assertEquals("div", dates.get(0).getTag());
        
    }
    
    @Test
    public void testGetStructureDates4() throws Exception {

        // http://www.zeit.de/politik/deutschland/2010-07/gruene-hamburg-cdu
        File testPage = ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit3.html");
        Document document = htmlParser.parse(testPage);

        List<StructureDate> dates = structureDateGetter.getDates(document);
        assertEquals(34, dates.size());

        assertEquals("2010-07", dates.get(0).getNormalizedDateString());
        assertEquals("data-smk_path", dates.get(0).getKeyword());
        assertEquals("body", dates.get(0).getTag());
        assertEquals(0, dates.get(0).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07-19", dates.get(1).getNormalizedDateString());
        assertEquals("published", dates.get(1).getKeyword());
        assertEquals("li", dates.get(1).getTag());
        assertEquals(5, dates.get(1).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(2).getNormalizedDateString());
        assertEquals("id", dates.get(2).getKeyword());
        assertEquals("a", dates.get(2).getTag());
        assertEquals(6, dates.get(2).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(3).getNormalizedDateString());
        assertEquals("id", dates.get(3).getKeyword());
        assertEquals("a", dates.get(3).getTag());
        assertEquals(6, dates.get(3).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(4).getNormalizedDateString());
        assertEquals("id", dates.get(4).getKeyword());
        assertEquals("a", dates.get(4).getTag());
        assertEquals(6, dates.get(4).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(5).getNormalizedDateString());
        assertEquals("id", dates.get(5).getKeyword());
        assertEquals("a", dates.get(5).getTag());
        assertEquals(6, dates.get(5).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(6).getNormalizedDateString());
        assertEquals("id", dates.get(6).getKeyword());
        assertEquals("a", dates.get(6).getTag());
        assertEquals(6, dates.get(6).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(7).getNormalizedDateString());
        assertEquals("id", dates.get(7).getKeyword());
        assertEquals("a", dates.get(7).getTag());
        assertEquals(5, dates.get(7).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(8).getNormalizedDateString());
        assertEquals("id", dates.get(8).getKeyword());
        assertEquals("a", dates.get(8).getTag());
        assertEquals(6, dates.get(8).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(9).getNormalizedDateString());
        assertEquals("id", dates.get(9).getKeyword());
        assertEquals("a", dates.get(9).getTag());
        assertEquals(6, dates.get(9).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(10).getNormalizedDateString());
        assertEquals("id", dates.get(10).getKeyword());
        assertEquals("a", dates.get(10).getTag());
        assertEquals(8, dates.get(10).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(11).getNormalizedDateString());
        assertEquals("id", dates.get(11).getKeyword());
        assertEquals("a", dates.get(11).getTag());
        assertEquals(8, dates.get(11).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(12).getNormalizedDateString());
        assertEquals("id", dates.get(12).getKeyword());
        assertEquals("a", dates.get(12).getTag());
        assertEquals(8, dates.get(12).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(13).getNormalizedDateString());
        assertEquals("id", dates.get(13).getKeyword());
        assertEquals("a", dates.get(13).getTag());
        assertEquals(6, dates.get(13).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(14).getNormalizedDateString());
        assertEquals("id", dates.get(14).getKeyword());
        assertEquals("a", dates.get(14).getTag());
        assertEquals(5, dates.get(14).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(15).getNormalizedDateString());
        assertEquals("id", dates.get(15).getKeyword());
        assertEquals("a", dates.get(15).getTag());
        assertEquals(6, dates.get(15).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(16).getNormalizedDateString());
        assertEquals("id", dates.get(16).getKeyword());
        assertEquals("a", dates.get(16).getTag());
        assertEquals(6, dates.get(16).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(17).getNormalizedDateString());
        assertEquals("id", dates.get(17).getKeyword());
        assertEquals("a", dates.get(17).getTag());
        assertEquals(8, dates.get(17).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(18).getNormalizedDateString());
        assertEquals("id", dates.get(18).getKeyword());
        assertEquals("a", dates.get(18).getTag());
        assertEquals(8, dates.get(18).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(19).getNormalizedDateString());
        assertEquals("id", dates.get(19).getKeyword());
        assertEquals("a", dates.get(19).getTag());
        assertEquals(8, dates.get(19).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(20).getNormalizedDateString());
        assertEquals("id", dates.get(20).getKeyword());
        assertEquals("a", dates.get(20).getTag());
        assertEquals(6, dates.get(20).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(21).getNormalizedDateString());
        assertEquals("id", dates.get(21).getKeyword());
        assertEquals("a", dates.get(21).getTag());
        assertEquals(5, dates.get(21).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(22).getNormalizedDateString());
        assertEquals("id", dates.get(22).getKeyword());
        assertEquals("a", dates.get(22).getTag());
        assertEquals(6, dates.get(22).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(23).getNormalizedDateString());
        assertEquals("id", dates.get(23).getKeyword());
        assertEquals("a", dates.get(23).getTag());
        assertEquals(6, dates.get(23).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(24).getNormalizedDateString());
        assertEquals("id", dates.get(24).getKeyword());
        assertEquals("a", dates.get(24).getTag());
        assertEquals(8, dates.get(24).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(25).getNormalizedDateString());
        assertEquals("id", dates.get(25).getKeyword());
        assertEquals("a", dates.get(25).getTag());
        assertEquals(8, dates.get(25).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(26).getNormalizedDateString());
        assertEquals("id", dates.get(26).getKeyword());
        assertEquals("a", dates.get(26).getTag());
        assertEquals(8, dates.get(26).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(27).getNormalizedDateString());
        assertEquals("id", dates.get(27).getKeyword());
        assertEquals("a", dates.get(27).getTag());
        assertEquals(6, dates.get(27).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(28).getNormalizedDateString());
        assertEquals("id", dates.get(28).getKeyword());
        assertEquals("a", dates.get(28).getTag());
        assertEquals(5, dates.get(28).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(29).getNormalizedDateString());
        assertEquals("id", dates.get(29).getKeyword());
        assertEquals("a", dates.get(29).getTag());
        assertEquals(6, dates.get(29).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(30).getNormalizedDateString());
        assertEquals("id", dates.get(30).getKeyword());
        assertEquals("a", dates.get(30).getTag());
        assertEquals(6, dates.get(30).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(31).getNormalizedDateString());
        assertEquals("id", dates.get(31).getKeyword());
        assertEquals("a", dates.get(31).getTag());
        assertEquals(8, dates.get(31).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(32).getNormalizedDateString());
        assertEquals("id", dates.get(32).getKeyword());
        assertEquals("a", dates.get(32).getTag());
        assertEquals(8, dates.get(32).get(StructureDate.STRUCTURE_DEPTH));

        assertEquals("2010-07", dates.get(33).getNormalizedDateString());
        assertEquals("id", dates.get(33).getKeyword());
        assertEquals("a", dates.get(33).getTag());
        assertEquals(8, dates.get(33).get(StructureDate.STRUCTURE_DEPTH));
    }

}
