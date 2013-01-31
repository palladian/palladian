package ws.palladian.extraction;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.html.HtmlHelper;
import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.PageAnalyzer;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.NekoHtmlParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;

/**
 * Test cases for the XPath handling via PageAnalyzer.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public class PageAnalyzerTest {
    
    // FIXME PageAnalyzer is in palladian-retrieval, test in palladian-core,
    // but test resources in palladian-core, so I cannot just move it.

    private final DocumentParser parser = ParserFactory.createHtmlParser();

    @Test
    public void testMakeMutualXPath() {
        Set<String> xPathSet = new HashSet<String>();

        xPathSet.add("/html/body/div[1]/table/tr[3]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[4]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[5]/td[2]/a[1]");
        assertEquals("/html/body/div[1]/table/tr/td[2]/a[1]", PageAnalyzer.makeMutualXPath(xPathSet));

        xPathSet.clear();
        xPathSet.add("/html/body/div[1]/table/tr[3]/td[2]/a[1]");
        xPathSet.add("/html/body/div[2]/table/tr[4]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[5]/td[3]/a[1]");
        assertEquals("/html/body/div/table/tr/td/a[1]", PageAnalyzer.makeMutualXPath(xPathSet));
    }

    // TODO split in different functions
    @Test
    public void testPageAnalyzer() {

        // targeted node test
        assertEquals(PageAnalyzer.getTargetNode("/div/table/tr/xhtml:td[6]/div[3]/p/xhtml:a[4]"), "a");

        // node in table test
        assertEquals(PageAnalyzer.nodeInTable("/div/table/tr/xhtml:td[6]/div[3]/p", 3), true);

        // test find last box section
        assertEquals(PageAnalyzer.findLastBoxSection("/xhtml:table/xhtml:tr/xhtml:td/xhtml:div[4]/xhtml:span/xhtml:b/xhtml:a"),
                "/xhtml:table/xhtml:tr/xhtml:td/xhtml:div[4]");

        // test get next sibling
        // assertEquals("/div/p/table/tr/td/a/b", PageAnalyzer.getNextSibling("/div/p/table/tr/td/a/b"));
        assertEquals(PageAnalyzer.getNextSibling("/div/p/table[4]/tr[6]/td[1]/a/b"), "/div/p/table[4]/tr[6]/td[2]/a/b");
        assertEquals(PageAnalyzer.getNextSibling("/div/p/table[4]/tr[6]/xhtml:th/b/a"), "/div/p/table[4]/tr[6]/xhtml:td[1]/b/a");
        assertEquals(PageAnalyzer.getNextTableCell("/div/p/table[4]/tr[6]/xhtml:th/div[6]/a"),
                "/div/p/table[4]/tr[6]/xhtml:td[1]/div[6]/a");
        assertEquals(PageAnalyzer.getNextTableCell("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i"),
                "/div/p/table[4]/tr[6]/td[2]/div[6]/p[8]/a/i");
        assertEquals("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i",
                PageAnalyzer.getNextTableCell("/div/p/table[4]/tr[6]/td/div[6]/p[8]/a/i"));
        assertEquals("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i",
                PageAnalyzer.getNextTableCell("/div/p/table[4]/tr[6]/th/div[6]/p[8]/a/i"));
        assertEquals("/div/p/table[4]/tr[6]/td[2]/div[6]/p[8]/a/i",
                PageAnalyzer.getNextTableCell("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i"));

        // test get parent node
        assertEquals(PageAnalyzer.getParentNode("/table[6]/tr/td[5]/div/a"), "/table[6]/tr/td[5]/div");

        // test remove counts
        assertEquals("/html/body/div/div/div/div/ul/li",
                PageAnalyzer.removeXPathIndices("/html/body/div[1]/div[1]/div[1]/div[2]/ul[2]/li[11]"));

        assertEquals(
                "/html/body/div[1]/div[1]/div[1]/div[2]/ul[2]/li/small",
                PageAnalyzer
                        .removeXPathIndicesFromLastCountNode("/html/body/div[1]/div[1]/div[1]/div[2]/ul[2]/li[11]/small"));

        // test remove count not
        String[] rcElements = { "ul", "div" };
        String xPath = PageAnalyzer
                .removeXPathIndicesNot(
                        "/html/body/div/div[1]/div/div[2]/table[2]/tbody/tr[3]/td/div/div/table[1]/tbody/tr/td/table/tbody/tr[13]/td[2]/div/span[7]/a",
                        rcElements);
        // System.out.println(xPath);
        assertEquals(
                "/html/body/div/div[1]/div/div[2]/table/tbody/tr/td/div/div/table/tbody/tr/td/table/tbody/tr/td/div/span/a",
                xPath);
        String[] rcElements2 = { "ul", "div" };
        xPath = PageAnalyzer.removeXPathIndicesNot("/html/body/div/div[1]/div/div[2]/ul[1]/li[1]", rcElements2);
        assertEquals("/html/body/div/div[1]/div/div[2]/ul[1]/li", xPath);

        // test get next table row
        assertEquals("//table/tr[2]/td[2]", PageAnalyzer.getNextTableRow("//table/tr[1]/td[2]"));
        assertEquals("//table/tr[1]/td[2]", PageAnalyzer.getNextTableRow("//table/tr/td[2]"));

        // test get first table cell
        assertEquals("//table/tr/td[1]", PageAnalyzer.getFirstTableCell("//table/tr/td"));
        assertEquals("//table/tr/td[1]", PageAnalyzer.getFirstTableCell("//table/tr/td[1]"));
        assertEquals("//table/tr/th", PageAnalyzer.getFirstTableCell("//table/tr/th"));
        assertEquals("//table/tr/td[1]/table/tr/td[1]", PageAnalyzer.getFirstTableCell("//table/tr/td[1]/table/tr/td"));
        assertEquals("//table/tr/td/table/tr/td[1]", PageAnalyzer.getFirstTableCell("//table/tr/td/table/tr/td"));
        assertEquals("//table/tr/th/table/tr/td[1]", PageAnalyzer.getFirstTableCell("//table/tr/th/table/tr/td"));

        // get get html text content
        // PageAnalyzer.setDocument("data/benchmarkSelection/qa/training/webpage1.html");
        // assertEquals("",PageAnalyzer.getHTMLTextByXPath("/html/body/div/div[2]/div[1]/div[1]/div[2]/div[2]/div[1]".toUpperCase()));
    }

    @Test
    public void testGetNumberOfTableColumns() throws FileNotFoundException, ParserException {

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website3.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));
        Document doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website3.html"));
        assertEquals(6, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/div/table[2]/tbody/tr/td"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website5.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website5.html"));
        assertEquals(6, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/center/table[1]/tbody/tr/td/blockquote/table[1]/tbody/tr/td/p"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website9.html"),"/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website9.html"));
        assertEquals(2,
                PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/table/tbody/tr/td/table[1]/tbody/tr/td/table[1]/tbody/tr/td/div/ul/li/a/b"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website11.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website11.html"));
        assertEquals(2, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/div/div/div/table/tbody/tr/td"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website17.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website17.html"));
        assertEquals(5, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/div/table[4]/tbody/tr/td/ul/li/a"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website27.html"),"/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website27.html"));
        assertEquals(
                3,
                PageAnalyzer.getNumberOfTableColumns(
                        doc,
                        "/html/body/form/table[1]/tbody/tr/td/div/table[1]/tbody/tr/td/table[1]/tbody/tr/td/table[1]/tbody/tr/td/div/table[1]/tbody/tr/td/div/div/span/span/span/p/table/tbody/tr/td"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website29.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website29.html"));
        assertEquals(5,
                PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/center/table[1]/tbody/tr/td/table[1]/tbody/tr/td/table[1]/tbody/tr/td"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website33.html"),"/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website33.html"));
        assertEquals(2,
                PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/table[1]/tbody/tr/td/p/table[3]/tbody/tr/td/table/tbody/tr/td/a"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website65.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website65.html"));
        assertEquals(11, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/div/div/table[1]/tbody/tr/td"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website67.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website67.html"));
        assertEquals(3, PageAnalyzer.getNumberOfTableColumns(doc, "/html/body/div/div/div/div/table[3]/tbody/tr/td/i/a"));

        // System.out.println(PageAnalyzer.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website69.html"),"/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A"));
        doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website69.html"));
        assertEquals(4, PageAnalyzer.getNumberOfTableColumns(doc,
                "/html/body/div/div/layer/div/table[1]/tbody/tr/td/div/table[1]/tbody/tr/td/p/table/tbody/tr/td/font/a"));

    }

    @Test
    public void testGetTableCellPath() {
        assertEquals("/div/p/table/tr/td", PageAnalyzer.getTableCellPath("/div/p/table/tr/td/a[5]/b"));
        assertEquals("/div/p/table/tr/td", PageAnalyzer.getTableCellPath("/div/p/table/tr/td"));
        assertEquals("/div/p/table/tr/td[2]", PageAnalyzer.getTableCellPath("/div/p/table/tr/td[2]"));
    }

//    @Test
//    @Ignore
//    public void testGetSiblingPage() {
//        PageAnalyzer pa = new PageAnalyzer();
//        assertEquals("http://www.cineplex.com/Movies/AllMovies.aspx?sort=2",
//                PageAnalyzer.getSiblingPage("http://www.cineplex.com/Movies/AllMovies.aspx"));
//        assertEquals("http://www.flashdevices.net/2008/02/",
//                PageAnalyzer.getSiblingPage("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
//        assertEquals("http://blog.wired.com/underwire/2008/10/star-trek-trail.html",
//                PageAnalyzer.getSiblingPage("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));
//        assertEquals("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110-2,00.htm",
//                PageAnalyzer.getSiblingPage("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));
//        assertEquals("http://cars.about.com/od/helpforcarbuyers/tp/ag_top_fuelsave.htm",
//                PageAnalyzer.getSiblingPage("http://cars.about.com/od/helpforcarbuyers/tp/top10_fuel.htm"));
//        assertEquals("http://www.blu-ray.com/movies/movies.php?genre=action&page=1",
//                PageAnalyzer.getSiblingPage("http://www.blu-ray.com/movies/movies.php?genre=action"));
//        assertEquals("http://forums.whirlpool.net.au/forum-replies.cfm?t=1037458",
//                PageAnalyzer.getSiblingPage("http://forums.whirlpool.net.au/forum-replies-archive.cfm/1037458.html"));
//    }
    
    @Test
    public void testGetLinks() throws FileNotFoundException, ParserException {
        
        Document doc = parser.parse(ResourceHelper.getResourceFile("/pageContentExtractor/test9.html"));
        assertEquals("http://www.example.com/test.html", HtmlHelper.getLinks(doc, true, true).iterator().next());

        doc = parser.parse(ResourceHelper.getResourceFile("/pageContentExtractor/test10.html"));
        assertEquals("http://www.example.com/test.html", HtmlHelper.getLinks(doc, true, true).iterator().next());

    }
    
    @Test
    public void testGetKeywords() throws FileNotFoundException, ParserException {
        Document doc = parser.parse(ResourceHelper.getResourceFile("/webPages/website1.html"));
        List<String> keywords = PageAnalyzer.extractKeywords(doc);
        assertEquals(11, keywords.size());
    }

}
