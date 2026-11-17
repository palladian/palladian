package ws.palladian.retrieval.parser;

import org.junit.Test;
import org.w3c.dom.Document;
import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.persistence.ParserException;

import static org.junit.Assert.*;

public class JsoupParserTest {

    /**
     * Since jsoup 1.23, comments outside the root element stay document-level nodes, so the document's first and last
     * children can be comments instead of the html element.
     */
    @Test
    public void testCommentsOutsideRootElement() throws ParserException {
        String html = "<!DOCTYPE html>\n" //
                + "<!--[if lt IE 7]><html class=\"ie6\"><![endif]-->\n" //
                + "<!--[if IE 7]><html class=\"ie7\"><![endif]-->\n" //
                + "<html><head><title>Page Title</title><meta name=\"description\" content=\"Page description\"></head>\n" //
                + "<body><h1>Headline</h1><p>Body text</p></body></html>\n" //
                + "<!-- HERO SECTION -->\n";
        Document document = new JsoupParser().parse(html);
        assertEquals("html", document.getLastChild().getNodeName());

        assertTrue(XPathHelper.hasXhtmlNs(document));
        assertEquals("Page Title", XPathHelper.getXhtmlNode(document, "//title").getTextContent());
        assertEquals("Page description", XPathHelper.getXhtmlNode(document, "//meta[@name='description']/@content").getNodeValue());
        assertEquals(1, XPathHelper.getXhtmlNodes(document, "//h1").size());
        assertEquals("Body text", XPathHelper.getXhtmlNode(document, "body/p").getTextContent());
        assertTrue(HtmlHelper.getDocumentTextDump(document).contains("Body text"));
    }
}
