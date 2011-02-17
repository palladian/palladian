package ws.palladian.extraction;

import java.util.HashSet;

import junit.framework.TestCase;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.web.Crawler;

/**
 * Test cases for the xPath handling.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 */
public class PageAnalyzerTest extends TestCase {

    public PageAnalyzerTest(String name) {
        super(name);
    }

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
        assertEquals("//TABLE/TR[2]/TD[2]", pa.getNextTableRow("//TABLE/TR[1]/TD[2]"));
        assertEquals("//TABLE/TR[1]/TD[2]", pa.getNextTableRow("//TABLE/TR/TD[2]"));

        // test get first table cell
        assertEquals("//TABLE/TR/TD[1]", pa.getFirstTableCell("//TABLE/TR/TD"));
        assertEquals("//TABLE/TR/TD[1]", pa.getFirstTableCell("//TABLE/TR/TD[1]"));
        assertEquals("//TABLE/TR/TH", pa.getFirstTableCell("//TABLE/TR/TH"));
        assertEquals("//TABLE/TR/TD[1]/TABLE/TR/TD[1]", pa.getFirstTableCell("//TABLE/TR/TD[1]/TABLE/TR/TD"));
        assertEquals("//TABLE/TR/TD/TABLE/TR/TD[1]", pa.getFirstTableCell("//TABLE/TR/TD/TABLE/TR/TD"));
        assertEquals("//TABLE/TR/TH/TABLE/TR/TD[1]", pa.getFirstTableCell("//TABLE/TR/TH/TABLE/TR/TD"));

        // get get html text content
        // pa.setDocument("data/benchmarkSelection/qa/training/webpage1.html");
        // assertEquals("",pa.getHTMLTextByXPath("/html/body/div/div[2]/div[1]/div[1]/div[2]/div[2]/div[1]".toUpperCase()));
    }

    public void testGetNumberOfTableColumns() {
        Crawler crawler = new Crawler();
        PageAnalyzer pa = new PageAnalyzer();

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website3.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));
        assertEquals(6, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website3.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[2]/TR/TD"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website5.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P"));
        assertEquals(6, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website5.html").getFile()),
                "/HTML/BODY/CENTER/TABLE[1]/TR/TD/BLOCKQUOTE/TABLE[1]/TR/TD/P"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website9.html"),"/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B"));
        assertEquals(2, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website9.html").getFile()),
                "/HTML/BODY/TABLE/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/UL/LI/A/B"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website11.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"));
        assertEquals(2, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website11.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/DIV/TABLE/TBODY/TR/TD"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website17.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"));
        assertEquals(5, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website17.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[4]/TR/TD/UL/LI/A"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website27.html"),"/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"));
        assertEquals(
                3,
                pa.getNumberOfTableColumns(
                        crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website27.html").getFile()),
                        "/HTML/BODY/FORM/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/DIV/DIV/SPAN/SPAN/SPAN/P/TABLE/TBODY/TR/TD"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website29.html"),"/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD"));
        assertEquals(5, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website29.html").getFile()),
                "/HTML/BODY/CENTER/TABLE[1]/TR/TD/TABLE[1]/TR/TD/TABLE[1]/TR/TD"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website33.html"),"/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A"));
        assertEquals(2, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website33.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/TABLE[1]/TR/TD/P/TABLE[3]/TR/TD/TABLE/TR/TD/A"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website65.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD"));
        assertEquals(11, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website65.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/DIV/DIV/TABLE[1]/TR/TD"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website67.html"),"/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A"));
        assertEquals(3, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website67.html").getFile()),
                "/HTML/BODY/DIV/DIV/DIV/DIV/TABLE[3]/TR/TD/I/A"));

        // System.out.println(pa.getNumberOfTableColumns(crawler.getDocument("data/test/webPages/website69.html"),"/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A"));
        assertEquals(4, pa.getNumberOfTableColumns(
                crawler.getWebDocument(PageAnalyzerTest.class.getResource("/webPages/website69.html").getFile()),
                "/HTML/BODY/DIV/DIV/LAYER/DIV/TABLE[1]/TR/TD/DIV/TABLE[1]/TR/TD/P/TABLE/TR/TD/FONT/A"));

    }

    public void testGetTableCellPath() {
        PageAnalyzer pa = new PageAnalyzer();
        assertEquals("/div/p/table/tr/td", pa.getTableCellPath("/div/p/table/tr/td/a[5]/b"));
        assertEquals("/div/p/table/tr/td", pa.getTableCellPath("/div/p/table/tr/td"));
        assertEquals("/div/p/table/tr/td[2]", pa.getTableCellPath("/div/p/table/tr/td[2]"));
    }
}