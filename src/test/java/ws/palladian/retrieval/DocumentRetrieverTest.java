package ws.palladian.retrieval;

import junit.framework.TestCase;

import org.w3c.dom.Document;

import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.html.XPathHelper;

/**
 * Test cases for the {@link DocumentRetriever}.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class DocumentRetrieverTest extends TestCase {

    public DocumentRetrieverTest(String name) {
        super(name);
    }

    public void testLinkHandling() {
        DocumentRetriever documentRetriever = new DocumentRetriever();
        Document doc = documentRetriever.getWebDocument(DocumentRetrieverTest.class.getResource(
                "/pageContentExtractor/test9.html").getFile());
        assertEquals("http://www.example.com/test.html", PageAnalyzer.getLinks(doc, true, true).iterator().next());

        doc = documentRetriever.getWebDocument(DocumentRetrieverTest.class.getResource(
                "/pageContentExtractor/test10.html").getFile());
        assertEquals("http://www.example.com/test.html", PageAnalyzer.getLinks(doc, true, true).iterator().next());
    }

    public void testNekoBugs() {

        // produces a StackOverflowError -- see
        // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
        // Crawler crawler = new Crawler();
        // Document doc =
        // crawler.getWebDocument(DocumentRetrieverTest.class.getResource("/webPages/NekoTestcase3109537.html").getFile());
        // assertNotNull(doc);

    }

    /**
     * Test undesired behavior from NekoHTML for which we introduced workarounds/fixes.
     * See {@link TBODYFix}.
     */
    public void testNekoWorkarounds() {

        DocumentRetriever crawler = new DocumentRetriever();
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(DocumentRetrieverTest.class.getResource(
                                "/webPages/NekoTableTestcase1.html").getFile()), "//table/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(DocumentRetrieverTest.class.getResource(
                                "/webPages/NekoTableTestcase2.html").getFile()), "//table/tbody/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(DocumentRetrieverTest.class.getResource(
                                "/webPages/NekoTableTestcase3.html").getFile()), "//table/tbody/tr[1]/td").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(DocumentRetrieverTest.class.getResource(
                                "/webPages/NekoTableTestcase4.html").getFile()), "//table/tr[1]/td").size());

    }

    public void testParseXml() {

        DocumentRetriever crawler = new DocumentRetriever();

        // parse errors will yield in a null return
        assertNotNull(crawler.getXMLDocument(DocumentRetrieverTest.class.getResource("/xmlDocuments/invalid-chars.xml")
                .getFile()));
        assertNotNull(crawler.getXMLDocument(DocumentRetrieverTest.class.getResource("/feeds/sourceforge02.xml")
                .getFile()));
        assertNotNull(crawler.getXMLDocument(DocumentRetrieverTest.class.getResource("/feeds/feed061.xml").getFile()));

    }

}