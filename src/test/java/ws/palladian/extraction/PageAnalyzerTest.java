package ws.palladian.extraction;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import ws.palladian.control.AllTests;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * Test cases for the xPath handling.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 */
public class PageAnalyzerTest {

    @Test
    public void testMakeMutualXPath() {
        PageAnalyzer pa = new PageAnalyzer();
        HashSet<String> xPathSet = new HashSet<String>();

        xPathSet.add("/html/body/div[1]/table/tr[3]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[4]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[5]/td[2]/a[1]");
        assertEquals("/html/body/div[1]/table/tr/td[2]/a[1]", pa.makeMutualXPath(xPathSet));

        xPathSet.clear();
        xPathSet.add("/html/body/div[1]/table/tr[3]/td[2]/a[1]");
        xPathSet.add("/html/body/div[2]/table/tr[4]/td[2]/a[1]");
        xPathSet.add("/html/body/div[1]/table/tr[5]/td[3]/a[1]");
        assertEquals("/html/body/div/table/tr/td/a[1]", pa.makeMutualXPath(xPathSet));
    }

    // TODO split in different functions
    @Test
    public void testPageAnalyzer() {
        PageAnalyzer pa = new PageAnalyzer();

        // targeted node test
        assertEquals(pa.getTargetNode("/div/table/tr/xhtml:td[6]/div[3]/p/xhtml:a[4]"), "a");

        // node in table test
        assertEquals(pa.nodeInTable("/div/table/tr/xhtml:td[6]/div[3]/p", 3), true);

        // test find last box section
        assertEquals(pa.findLastBoxSection("/xhtml:table/xhtml:tr/xhtml:td/xhtml:div[4]/xhtml:span/xhtml:b/xhtml:a"),
                "/xhtml:table/xhtml:tr/xhtml:td/xhtml:div[4]");

        // test get next sibling
        // assertEquals("/div/p/table/tr/td/a/b", pa.getNextSibling("/div/p/table/tr/td/a/b"));
        assertEquals(pa.getNextSibling("/div/p/table[4]/tr[6]/td[1]/a/b"), "/div/p/table[4]/tr[6]/td[2]/a/b");
        assertEquals(pa.getNextSibling("/div/p/table[4]/tr[6]/xhtml:th/b/a"), "/div/p/table[4]/tr[6]/xhtml:td[1]/b/a");
        assertEquals(pa.getNextTableCell("/div/p/table[4]/tr[6]/xhtml:th/div[6]/a"),
                "/div/p/table[4]/tr[6]/xhtml:td[1]/div[6]/a");
        assertEquals(pa.getNextTableCell("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i"),
                "/div/p/table[4]/tr[6]/td[2]/div[6]/p[8]/a/i");
        assertEquals("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i",
                pa.getNextTableCell("/div/p/table[4]/tr[6]/td/div[6]/p[8]/a/i"));
        assertEquals("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i",
                pa.getNextTableCell("/div/p/table[4]/tr[6]/th/div[6]/p[8]/a/i"));
        assertEquals("/div/p/table[4]/tr[6]/td[2]/div[6]/p[8]/a/i",
                pa.getNextTableCell("/div/p/table[4]/tr[6]/td[1]/div[6]/p[8]/a/i"));

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
        assertEquals("//table/tr[2]/td[2]", pa.getNextTableRow("//table/tr[1]/td[2]"));
        assertEquals("//table/tr[1]/td[2]", pa.getNextTableRow("//table/tr/td[2]"));

        // test get first table cell
        assertEquals("//table/tr/td[1]", pa.getFirstTableCell("//table/tr/td"));
        assertEquals("//table/tr/td[1]", pa.getFirstTableCell("//table/tr/td[1]"));
        assertEquals("//table/tr/th", pa.getFirstTableCell("//table/tr/th"));
        assertEquals("//table/tr/td[1]/table/tr/td[1]", pa.getFirstTableCell("//table/tr/td[1]/table/tr/td"));
        assertEquals("//table/tr/td/table/tr/td[1]", pa.getFirstTableCell("//table/tr/td/table/tr/td"));
        assertEquals("//table/tr/th/table/tr/td[1]", pa.getFirstTableCell("//table/tr/th/table/tr/td"));

        // get get html text content
        // pa.setDocument("data/benchmarkSelection/qa/training/webpage1.html");
        // assertEquals("",pa.getHTMLTextByXPath("/html/body/div/div[2]/div[1]/div[1]/div[2]/div[2]/div[1]".toUpperCase()));
    }

    @Test
    public void testGetNumberOfTableColumns() {
        DocumentRetriever crawler = new DocumentRetriever();
        PageAnalyzer pa = new PageAnalyzer();

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website3.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));
        assertEquals(6, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website3.html").getFile()),
                "/html/body/div/div/div/div/table[2]/tr/td"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website5.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P"));
        assertEquals(6, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website5.html").getFile()),
                "/html/body/center/table[1]/tr/td/blockquote/table[1]/tr/td/p"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website9.html"),"/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B"));
        assertEquals(2, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website9.html").getFile()),
                "/html/body/table/tr/td/table[1]/tr/td/table[1]/tr/td/div/ul/li/a/b"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website11.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"));
        assertEquals(
                2,
                pa.getNumberOfTableColumns(crawler.getWebDocument(PageAnalyzerTest.class.getResource(
                        "/webPages/website11.html").getFile()), "/html/body/div/div/div/div/div/div/table/tbody/tr/td"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website17.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"));
        assertEquals(
                5,
                pa.getNumberOfTableColumns(crawler.getWebDocument(PageAnalyzerTest.class.getResource(
                        "/webPages/website17.html").getFile()), "/html/body/div/div/div/div/table[4]/tr/td/ul/li/a"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website27.html"),"/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"));
        assertEquals(
                3,
                pa.getNumberOfTableColumns(
                        crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website27.html").getFile()),
                        "/html/body/form/table[1]/tr/td/div/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td/div/table[1]/tr/td/div/div/span/span/span/p/table/tbody/tr/td"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website29.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD"));
        assertEquals(5, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website29.html").getFile()),
                "/html/body/center/table[1]/tr/td/table[1]/tr/td/table[1]/tr/td"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website33.html"),"/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A"));
        assertEquals(2, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website33.html").getFile()),
                "/html/body/div/div/div/table[1]/tr/td/p/table[3]/tr/td/table/tr/td/a"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website65.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD"));
        assertEquals(
                11,
                pa.getNumberOfTableColumns(crawler.getWebDocument(PageAnalyzerTest.class.getResource(
                        "/webPages/website65.html").getFile()), "/html/body/div/div/div/div/div/table[1]/tr/td"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website67.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A"));
        assertEquals(
                3,
                pa.getNumberOfTableColumns(crawler.getWebDocument(PageAnalyzerTest.class.getResource(
                        "/webPages/website67.html").getFile()), "/html/body/div/div/div/div/table[3]/tr/td/i/a"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website69.html"),"/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A"));
        assertEquals(4, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website69.html").getFile()),
                "/html/body/div/div/layer/div/table[1]/tr/td/div/table[1]/tr/td/p/table/tr/td/font/a"));

    }

    @Test
    public void testGetTableCellPath() {
        PageAnalyzer pa = new PageAnalyzer();
        assertEquals("/div/p/table/tr/td", pa.getTableCellPath("/div/p/table/tr/td/a[5]/b"));
        assertEquals("/div/p/table/tr/td", pa.getTableCellPath("/div/p/table/tr/td"));
        assertEquals("/div/p/table/tr/td[2]", pa.getTableCellPath("/div/p/table/tr/td[2]"));
    }

    @Test
    public void testGetSiblingPage() {
        if (AllTests.ALL_TESTS) {
            PageAnalyzer pa = new PageAnalyzer();
            assertEquals("http://www.cineplex.com/Movies/AllMovies.aspx?sort=2",
                    pa.getSiblingPage("http://www.cineplex.com/Movies/AllMovies.aspx"));
            assertEquals("http://www.flashdevices.net/2008/02/",
                    pa.getSiblingPage("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
            assertEquals("http://blog.wired.com/underwire/2008/10/star-trek-trail.html",
                    pa.getSiblingPage("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));
            assertEquals("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110-2,00.htm",
                    pa.getSiblingPage("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));
            assertEquals("http://cars.about.com/od/helpforcarbuyers/tp/ag_top_fuelsave.htm",
                    pa.getSiblingPage("http://cars.about.com/od/helpforcarbuyers/tp/top10_fuel.htm"));
            assertEquals("http://www.blu-ray.com/movies/movies.php?genre=action&page=1",
                    pa.getSiblingPage("http://www.blu-ray.com/movies/movies.php?genre=action"));
            assertEquals("http://forums.whirlpool.net.au/forum-replies.cfm?t=1037458",
                    pa.getSiblingPage("http://forums.whirlpool.net.au/forum-replies-archive.cfm/1037458.html"));
        }
    }

}