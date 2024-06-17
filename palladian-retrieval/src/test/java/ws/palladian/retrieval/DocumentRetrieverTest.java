package ws.palladian.retrieval;

import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.helper.io.StringInputStream;
import ws.palladian.persistence.ParserException;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class DocumentRetrieverTest {
    @Ignore
    @Test
    public void testDocumentUriInCaseOfRedirects() {
        // will redirect to http://example.com
        String redirectingUrl = "https://httpbingo.org/redirect-to?url=http%3A%2F%2Fexample.com%2F"; // TODO returns 403 now, forbidden link redirect
        Document document = new DocumentRetriever().getWebDocument(redirectingUrl);
        assertEquals("http://example.com/", document.getDocumentURI());
    }

    @Test
    public void testGetLinks() throws ParserException, FileNotFoundException {
        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(
                new StringInputStream("<html><body><a href=\"test.pdf\">Test 1</a><a href=\"test2.pdf\" data-pdf-title=\"A title bla bla\">Test 2</a></body></html>"));
        Set<String> links = HtmlHelper.getLinks(document, "localhost", true, true, "", true, true, new HashSet<>(Collections.singletonList("data-pdf-title")));
        assertEquals(2, links.size());
        assertEquals(true, links.contains("test2.pdf?data-pdf-title=A+title+bla+bla"));

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/apple.html"));
        List<Node> linkNodes = XPathHelper.getXhtmlNodes(document, "//a");
        assertEquals(358, linkNodes.size());
    }
}
