package ws.palladian.extraction.date.getter;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.date.dates.ContentDate;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

public class ContentDateGetterTest {

    @Test
    public void testGetContentDate() throws ParserException, FileNotFoundException {
        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit1.htm"));
        ContentDateGetter contentDateGetter = new ContentDateGetter();
        List<ContentDate> dates = contentDateGetter.getDates(document);
        assertEquals(6, dates.size());
        assertEquals("2010-08-22", dates.get(0).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(1).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(2).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(3).getNormalizedDateString());
        assertEquals("2010-08-22", dates.get(4).getNormalizedDateString());
        assertEquals("2010-08", dates.get(5).getNormalizedDateString());

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/dateExtraction/zeit2.htm"));
        dates = contentDateGetter.getDates(document);
        assertEquals(2, dates.size());
        assertEquals("2010-09-03", dates.get(0).getNormalizedDateString());
        assertEquals("2010-09-02", dates.get(1).getNormalizedDateString());

    }

    @Test
    public void testGetFindAllDatesTime() throws FileNotFoundException {
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/texts/text01.txt"));
        List<ContentDate> dates = ContentDateGetter.findAllDates(text);
        assertEquals(142, dates.size());

        Set<String> stringPos = CollectionHelper.newHashSet();
        for (ContentDate date : dates) {
            stringPos.add(date.getDateString() + date.get(ContentDate.DATEPOS_IN_DOC));
        }
        assertEquals(119, stringPos.size());
    }

}
