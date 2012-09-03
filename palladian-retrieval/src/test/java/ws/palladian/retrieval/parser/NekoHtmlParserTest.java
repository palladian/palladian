package ws.palladian.retrieval.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Test for various peculiarities of NekoHTML.
 * </p>
 * 
 * @author Philipp Katz
 */
public class NekoHtmlParserTest {

    private NekoHtmlParser htmlParser;

    @Before
    public void setUp() {
        htmlParser = new NekoHtmlParser();
    }

    /**
     * <p>
     * Test for {@link StackOverflowError} caused by some webpages.
     * </p>
     * 
     * @see http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
     * @throws FileNotFoundException
     * @throws ParserException
     */
    @Test
    @Ignore
    public void testNeko3109537() throws FileNotFoundException, ParserException {
        htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTestcase3109537.html"));
    }

    /**
     * <p>
     * Originally, NekoHTML does set the namespace when inserting elements. The tr element being inserted should also be
     * in XHTML namespace.
     * </p>
     * 
     * @see https://bitbucket.org/palladian/palladian/issue/29/tr-fix-for-neko-html
     * @see https://sourceforge.net/tracker/?func=detail&aid=3151253&group_id=195122&atid=952178
     * @throws FileNotFoundException
     * @throws ParserException
     */
    @Test
    public void testNekoTrNamespace() throws FileNotFoundException, ParserException {
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTrNamespaceTest.html"));
        Node node = XPathHelper.getXhtmlNode(document, "//div[1]/table[3]/tbody[1]/tr[1]/td[2]/blockquote[2]");
        assertNotNull(node);

        node = XPathHelper.getNode(document,
                "//xhtml:div[1]/xhtml:table[3]/xhtml:tbody[1]/tr[1]/xhtml:td[2]/xhtml:blockquote[2]");
        assertNull(node);
    }

    /**
     * <p>
     * Test parsing a file which contains XHTML, MathML and SVG parts.
     * </p>
     * 
     * @throws FileNotFoundException
     * @throws ParserException
     */
    @Test
    public void testParseMixedNamespaces() throws FileNotFoundException, ParserException {
        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/xhtml-mathml-svg.xhtml"));
        assertEquals(4, XPathHelper.getXhtmlNodes(document, "/html/body/ul[1]/li").size());
    }

}