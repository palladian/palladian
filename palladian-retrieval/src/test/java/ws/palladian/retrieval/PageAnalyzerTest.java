package ws.palladian.retrieval;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserFactory;

public class PageAnalyzerTest {

    @Test
    public void testXPathConstruction_issue159() throws Exception {
        DocumentParser htmlParser = ParserFactory.createHtmlParser();
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/test_issue159.html"));

        Set<String> xPaths = PageAnalyzer
                .constructAllXPaths(document, "Who starred in The Invisible Army", false, true);
        XPathSet xPathSet = new XPathSet();
        xPathSet.add(xPaths);
        String longestXPath = xPathSet.getLongestXPath();

        // System.out.println("constructed xPath: " + longestXPath);
        Node node = XPathHelper.getXhtmlNode(document, longestXPath);
        // System.out.println("node that was found when constructing, must not be null and it is: " + node);
        // System.out.println("node that was found when constructing, must not be null and it is: "
        // + node.getTextContent());

        assertNotNull(node);
    }

}
