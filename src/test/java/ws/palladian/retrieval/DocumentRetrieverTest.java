package ws.palladian.retrieval;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.ResourceHelper;
import ws.palladian.helper.html.XPathHelper;

/**
 * Test cases for the {@link DocumentRetriever}.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class DocumentRetrieverTest {

    @Test
    public void testLinkHandling() throws FileNotFoundException {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        Document doc = documentRetriever.getWebDocument(ResourceHelper
                .getResourcePath("/pageContentExtractor/test9.html"));
        assertEquals("http://www.example.com/test.html", PageAnalyzer.getLinks(doc, true, true).iterator().next());

        doc = documentRetriever.getWebDocument(ResourceHelper.getResourcePath("/pageContentExtractor/test10.html"));
        assertEquals("http://www.example.com/test.html", PageAnalyzer.getLinks(doc, true, true).iterator().next());
    }

    @Test
    public void testNekoBugs() {

        // produces a StackOverflowError -- see
        // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
        // Crawler crawler = new Crawler();
        // Document doc =
        // crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/NekoTestcase3109537.html"));
        // assertNotNull(doc);

    }

    /**
     * Test undesired behavior from NekoHTML for which we introduced workarounds/fixes.
     * See {@link NekoTbodyFix}.
     * 
     * @throws FileNotFoundException
     */
    @Test
    public void testNekoWorkarounds() throws FileNotFoundException {

        DocumentRetriever crawler = new DocumentRetriever();
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/NekoTableTestcase1.html")),
                        "//table/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/NekoTableTestcase2.html")),
                        "//table/tbody/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/NekoTableTestcase3.html")),
                        "//table/tbody/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(ResourceHelper.getResourcePath("/webPages/NekoTableTestcase4.html")),
                        "//table/tr[1]/td").size());

    }

    @Test
    public void testParseXml() throws FileNotFoundException {

        DocumentRetriever crawler = new DocumentRetriever();

        // parse errors will yield in a null return
        assertNotNull(crawler.getXMLDocument(ResourceHelper.getResourcePath("/xmlDocuments/invalid-chars.xml")));
        assertNotNull(crawler.getXMLDocument(ResourceHelper.getResourcePath("/feeds/sourceforge02.xml")));
        assertNotNull(crawler.getXMLDocument(ResourceHelper.getResourcePath("/feeds/feed061.xml")));

    }

}