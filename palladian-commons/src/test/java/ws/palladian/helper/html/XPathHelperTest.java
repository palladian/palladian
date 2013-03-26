package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ws.palladian.helper.ParseUtil;
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
        assertEquals("//xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]", XPathHelper.addXhtmlNsToXPath("//TABLE/TR/TD/A[4]"));
        assertEquals("/xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]", XPathHelper.addXhtmlNsToXPath("/TABLE/TR/TD/A[4]"));
        assertEquals("/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A", XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A"));
        assertEquals("/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A/text()", XPathHelper.addXhtmlNsToXPath("/TABLE/TR[2]/TD/A/text()"));

        assertEquals("//xhtml:a[xhtml:img]", XPathHelper.addXhtmlNsToXPath("//a[img]"));
        assertEquals("xhtml:img", XPathHelper.addXhtmlNsToXPath("img"));
        assertEquals("//xhtml:*", XPathHelper.addXhtmlNsToXPath("//*"));
        
        assertEquals("//xhtml:link[@rel='application/atom+xml']", XPathHelper.addXhtmlNsToXPath("//link[@rel='application/atom+xml']"));
        assertEquals(
                "//xhtml:link[contains(translate(@rel, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'alternate') and (translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/atom+xml' or translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/rss+xml')]", 
                XPathHelper.addXhtmlNsToXPath("//link[contains(translate(@rel, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz'), 'alternate') and (translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/atom+xml' or translate(@type, 'ABCDEFGHIJKLMNOPQRSTUVWXYZ', 'abcdefghijklmnopqrstuvwxyz')='application/rss+xml')]"));
        
        assertEquals("//xhtml:div|//xhtml:p", XPathHelper.addXhtmlNsToXPath("//div|//p"));
        assertEquals("//xhtml:div/xhtml:a|//xhtml:p/xhtml:a", XPathHelper.addXhtmlNsToXPath("//div/a|//p/a"));
        
        assertEquals("//xhtml:a[not(.//xhtml:img)]", XPathHelper.addXhtmlNsToXPath("//a[not(.//img)]"));
        assertEquals("//xhtml:h2[./xhtml:span[@id='Kritik']]", XPathHelper.addXhtmlNsToXPath("//h2[./span[@id='Kritik']]"));
        assertEquals("//xhtml:h2[./xhtml:span[@id='Kritik' or @id='Kritiken']]", XPathHelper.addXhtmlNsToXPath("//h2[./span[@id='Kritik' or @id='Kritiken']]"));
    }

    @Test
    public void testGetElementById() throws ParserConfigurationException, SAXException, IOException {
        Document doc = ParseUtil.parseXhtml(ResourceHelper.getResourceFile("events.xml"));

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
        Document doc = ParseUtil.parseXhtml(ResourceHelper.getResourceFile("/multipleNamespaces.xml"));
        Map<String, String> mapping = new HashMap<String, String>();
        mapping.put("h", "http://www.w3.org/TR/html4/");
        mapping.put("f", "http://www.w3schools.com/furniture");

        List<Node> nodes = XPathHelper.getNodes(doc, "//h:td", mapping);
        assertEquals(2, nodes.size());

        nodes = XPathHelper.getNodes(doc, "/root/f:table/f:name", mapping);
        assertEquals(1, nodes.size());

    }
    
    @Test
    public void testGetNodes() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        Document doc = ParseUtil.parseXhtml(ResourceHelper.getResourceFile("/w3c_xhtml_strict.html"));
        List<Node> tocNodes = XPathHelper.getXhtmlNodes(doc, "//div[@class='toc']");
        assertEquals(2, tocNodes.size());
        
        Node firstTocNode = XPathHelper.getXhtmlNode(doc, "//body/div[@class='toc']");
        assertNotNull(firstTocNode);
        
        List<Node> tocItems = XPathHelper.getXhtmlNodes(firstTocNode, "ul/li");
        assertEquals(10, tocItems.size());
        
        List<Node> tocItems2 = XPathHelper.getXhtmlNodes(firstTocNode, "ul/*");
        assertEquals(10, tocItems2.size());
    }

}
