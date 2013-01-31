package ws.palladian.retrieval;

import static org.junit.Assert.assertNotNull;

import java.util.Set;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;

public class PageAnalyzerTest {

    @Test
    public void testXPathConstruction_issue159() throws Exception {
        DocumentRetriever ret = new DocumentRetriever();
        Document wd = ret.getWebDocument("http://www.evi.com/q/who_starred_in_the_invisible_army");

        Set<String> xPaths = PageAnalyzer.constructAllXPaths(wd, "Who starred in The Invisible Army", false, true);
        XPathSet xps = new XPathSet();
        xps.add(xPaths);
        String longestXPath = xps.getLongestXPath();

        System.out.println("constructed xPath: " + longestXPath);
        Node node = XPathHelper.getXhtmlNode(wd, longestXPath);
        System.out.println("node that was found when constructing, must not be null and it is: " + node);

        assertNotNull(node);
    }


}
