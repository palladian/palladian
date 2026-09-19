package ws.palladian.retrieval;

import org.junit.Test;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Guards {@link PageAnalyzer#constructXPath(org.w3c.dom.Node)} against non-element DOM nodes.
 * <p>
 * The behaviour that matters: every XPath this method returns must be <b>compilable</b>. A DOM comment node's
 * {@code getNodeName()} is the literal string {@code #comment}, and splicing that into a path yields
 * {@code //#comment[6]} - which throws {@code "A location step was expected following the '/' or '//' token"} the
 * moment it is evaluated. The method already stripped {@code #text}; {@code #comment} and {@code #cdata-section}
 * were simply never added, and comment nodes produced 3,405 XPathExpressionExceptions in one week of news
 * extraction.
 */
public class PageAnalyzerCommentNodeTest {

    @Test
    public void anXPathBuiltFromACommentNodeIsCompilable() throws Exception {
        Document document = newHtmlDocument();
        Element body = (Element) document.getElementsByTagName("body").item(0);
        Comment comment = document.createComment(" a tracking pixel used to live here ");
        body.appendChild(comment);

        String xPath = PageAnalyzer.constructXPath(comment);

        assertFalse("the comment node's name must not survive as a location step: " + xPath, xPath.contains("#comment"));
        assertCompilable(xPath);
    }

    @Test
    public void anXPathBuiltThroughACdataNodeIsCompilable() throws Exception {
        Document document = newHtmlDocument();
        Element body = (Element) document.getElementsByTagName("body").item(0);
        body.appendChild(document.createCDATASection("var x = 1;"));

        String xPath = PageAnalyzer.constructXPath(body.getLastChild());

        assertFalse("the cdata node's name must not survive as a location step: " + xPath, xPath.contains("#cdata"));
        assertCompilable(xPath);
    }

    @Test
    public void anXPathBuiltFromATextNodeIsStillCompilable() throws Exception {
        // regression guard: #text was the one case already handled and must keep working
        Document document = newHtmlDocument();
        Element body = (Element) document.getElementsByTagName("body").item(0);
        Element paragraph = document.createElement("p");
        paragraph.appendChild(document.createTextNode("hello"));
        body.appendChild(paragraph);

        String xPath = PageAnalyzer.constructXPath(paragraph.getFirstChild());

        assertFalse(xPath.contains("#text"));
        assertCompilable(xPath);
    }

    @Test
    public void anOrdinaryElementPathIsUnchangedAndStillPointsAtTheElement() throws Exception {
        // regression guard: the normal path must be entirely unaffected
        Document document = newHtmlDocument();
        Element body = (Element) document.getElementsByTagName("body").item(0);
        Element paragraph = document.createElement("p");
        paragraph.appendChild(document.createTextNode("hello"));
        body.appendChild(paragraph);

        String xPath = PageAnalyzer.constructXPath(paragraph);

        assertTrue("expected the element to appear in " + xPath, xPath.endsWith("p") || xPath.contains("p["));
        assertCompilable(xPath);
    }

    private static void assertCompilable(String xPath) throws Exception {
        if (xPath.isEmpty()) {
            return; // constructXPath deliberately returns "" for script subtrees
        }
        XPathFactory.newInstance().newXPath().compile(xPath);
    }

    private static Document newHtmlDocument() throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        Element html = document.createElement("html");
        document.appendChild(html);
        html.appendChild(document.createElement("body"));
        return document;
    }
}
