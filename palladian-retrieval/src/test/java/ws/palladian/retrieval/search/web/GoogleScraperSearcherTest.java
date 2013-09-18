package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.resources.BasicWebContent;

public class GoogleScraperSearcherTest {

    @Test
    public void testParseHtml() throws Exception {
        File file = ResourceHelper.getResourceFile("apiresponse/googleResult.html");
        Document document = ParserFactory.createHtmlParser().parse(file);
        List<BasicWebContent> webResults = GoogleScraperSearcher.parseHtml(document);
        assertEquals(10, webResults.size());
        assertEquals("Cat Products & Services", webResults.get(0).getTitle());
        assertEquals("http://www.cat.com/", webResults.get(0).getUrl());
        assertEquals("http://www.petfinder.com/cat-breeds?see-all=1", webResults.get(6).getUrl());

        file = ResourceHelper.getResourceFile("apiresponse/googleResult2.html");
        document = ParserFactory.createHtmlParser().parse(file);
        webResults = GoogleScraperSearcher.parseHtml(document);
        assertEquals(10, webResults.size());
    }

}
