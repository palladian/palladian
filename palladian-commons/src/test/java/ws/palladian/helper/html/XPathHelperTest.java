package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Test cases for {@link XPathHelper}.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public class XPathHelperTest {

    @Test
    public void testAddNamespaceToXPath() {
        assertEquals(XPathHelper.addXhtmlNsToXPath("//TABLE/TR/TD/A[4]"), "//xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR/TD/A[4]"), "/xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A"), "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A");
        assertEquals(XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A/text()"),
                "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A/text()");

        // TODO assertEquals(XPathHelper.addXhtmlNsToXPath("//a[img]"), "//xhtml:a[xhtml:img]");

    }

    // FIXME needs to be re-added
    // @Test
    // public void testGetXhtmlChildNodes() throws FileNotFoundException, ParserException {
    //
    // Document doc = htmlParser.parse(ResourceHelper.getResourceStream("/webPages/NekoTableTestcase1.html"));
    //
    // List<Node> rows = XPathHelper.getXhtmlNodes(doc, "//table/tr");
    // assertEquals(3, rows.size());
    //
    // for (Node row : rows) {
    //
    // // iterate over TDs
    // List<Node> cells = XPathHelper.getXhtmlChildNodes(row, "//td"); // does not work EDIT: now it does
    // assertEquals(3, cells.size());
    //
    // cells = XPathHelper.getXhtmlChildNodes(row, "*"); // infinite loop? EDIT: yes, stupid me :) solved.
    // assertEquals(3, cells.size());
    // }
    // }

    @Test
    public void testGetElementById() throws ParserConfigurationException, SAXException, IOException {
        Document doc = parse(ResourceHelper.getResourceFile("events.xml"));

        assertEquals("event", XPathHelper.getNodeByID(doc, "e01").getNodeName());
        assertEquals("events", XPathHelper.getParentNodeByID(doc, "e01").getNodeName());
        
        assertEquals(2, XPathHelper.getNodes(doc, "//participant[@events=\"e02\"]").size());
        
        // XPath 1.0 way, no lower-case available
        assertEquals(1, XPathHelper.getNodes(doc, "//participant[translate(@name, \"ABCDEFGHIJKLMNOPQRSTUVWXYZ\", \"abcdefghijklmnopqrstuvwxyz\")=\"otto lieb\"]").size());
        
        // XPath 2.0 way, not supported by jaxax.xml.xpath, but by org.jaxen
        // assertEquals(1, XPathHelper.getNodes(doc, "//participant[lowercase(@name)=\"otto lieb\"]").size());
        
    }

    @Test
    public void testNamespace() throws ParserConfigurationException, SAXException, IOException {
        Document doc = parse(ResourceHelper.getResourceFile("/multipleNamespaces.xml"));
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("h", "http://www.w3.org/TR/html4/");
        mapping.put("f", "http://www.w3schools.com/furniture");

        List<Node> nodes = XPathHelper.getNodes(doc, "//h:td", mapping);
        assertEquals(2, nodes.size());

        nodes = XPathHelper.getNodes(doc, "/root/f:table/f:name", mapping);
        assertEquals(1, nodes.size());

    }

    private final Document parse(File file) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
        return documentBuilder.parse(file);
    }

}
