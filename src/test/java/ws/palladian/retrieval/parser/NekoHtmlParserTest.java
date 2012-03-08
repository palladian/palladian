package ws.palladian.retrieval.parser;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.ResourceHelper;

public class NekoHtmlParserTest {

    private NekoHtmlParser htmlParser;

    @Before
    public void setUp() {
        htmlParser = new NekoHtmlParser();
    }

    /**
     * <p>
     * Test undesired behavior from NekoHTML for which we introduced workarounds/fixes.
     * </p>
     * 
     * @see NekoTbodyFix
     * @throws FileNotFoundException
     * @throws ParserException
     */
    @Test
    public void testTbodyFix() throws FileNotFoundException, ParserException {

        Document document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTableTestcase1.html"));
        assertEquals(3, XPathHelper.getXhtmlNodes(document, "//table/tr[1]/td").size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTableTestcase2.html"));
        assertEquals(3, XPathHelper.getXhtmlNodes(document, "//table/tbody/tr[1]/td").size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTableTestcase3.html"));
        assertEquals(3, XPathHelper.getXhtmlNodes(document, "//table/tbody/tr[1]/td").size());

        document = htmlParser.parse(ResourceHelper.getResourceFile("/webPages/NekoTableTestcase4.html"));
        assertEquals(3, XPathHelper.getXhtmlNodes(document, "//table/tr[1]/td").size());

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
}
