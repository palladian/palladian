package ws.palladian.web;

import junit.framework.TestCase;
import ws.palladian.control.AllTests;
import ws.palladian.helper.XPathHelper;
import ws.palladian.web.Crawler;
import ws.palladian.web.TBODYFix;

/**
 * Test cases for the crawler.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class CrawlerTest extends TestCase {

    public CrawlerTest(String name) {
        super(name);
    }

    public void testGetCleanURL() {
        assertEquals("amazon.com/", Crawler.getCleanURL("http://www.amazon.com/"));
        assertEquals("amazon.com/", Crawler.getCleanURL("http://amazon.com/"));
        assertEquals("amazon.com/", Crawler.getCleanURL("https://www.amazon.com/"));
        assertEquals("amazon.com", Crawler.getCleanURL("https://amazon.com"));
        assertEquals("amazon.com/", Crawler.getCleanURL("www.amazon.com/"));
        assertEquals("amazon.com/", Crawler.getCleanURL("amazon.com/"));
    }

    public void testGetDomain() {
        // System.out.println(crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html",
        // false));
        assertEquals("http://www.flashdevices.net",
                Crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", true));
        assertEquals("www.flashdevices.net",
                Crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", false));

        assertEquals("http://blog.wired.com",
                Crawler.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", true));
        assertEquals("blog.wired.com",
                Crawler.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", false));

        // added by Philipp
        assertEquals("https://example.com", Crawler.getDomain("https://example.com/index.html"));
        assertEquals("", Crawler.getDomain(""));
        assertEquals("", Crawler.getDomain(null));
        assertEquals("", Crawler.getDomain("file:///test.html")); // TODO return localhost here?
        assertEquals("localhost", Crawler.getDomain("file://localhost/test.html", false));
    }

    public void testMakeFullURL() {

        assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de", null, "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                Crawler.makeFullURL("http://www.xyz.de/index.html", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory", "/page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory", "./page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory/directory", "../page.html"));

        assertEquals("http://www.abc.de/page.html",
                Crawler.makeFullURL("http://www.xyz.de", "", "http://www.abc.de/page.html"));
        assertEquals("http://www.abc.de/page.html",
                Crawler.makeFullURL("http://www.xyz.de", "http://www.abc.de/", "/page.html"));

        assertEquals("http://www.example.com/page.html",
                Crawler.makeFullURL("/some/file/path.html", "http://www.example.com/page.html"));
        assertEquals("", Crawler.makeFullURL("http://www.xyz.de", "mailto:example@example.com"));

        assertEquals("http://www.example.com/page.html",
                Crawler.makeFullURL(null, null, "http://www.example.com/page.html"));

        // when no linkUrl is supplied, we cannot determine the full URL, so just return an empty String.
        assertEquals("", Crawler.makeFullURL(null, "http://www.example.com", null));
        assertEquals("", Crawler.makeFullURL("http://www.example.com", null, null));
        assertEquals("", Crawler.makeFullURL(null, null, "/page.html"));

    }

    public void testGetSiblingPage() {
        if (AllTests.ALL_TESTS) {
            Crawler crawler = new Crawler();
            assertEquals("http://www.cineplex.com/Movies/AllMovies.aspx?sort=2",
                    crawler.getSiblingPage("http://www.cineplex.com/Movies/AllMovies.aspx"));
            assertEquals("http://www.flashdevices.net/2008/02/",
                    crawler.getSiblingPage("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
            assertEquals("http://blog.wired.com/underwire/2008/10/star-trek-trail.html",
                    crawler.getSiblingPage("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));
            assertEquals("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110-2,00.htm",
                    crawler.getSiblingPage("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));
            assertEquals("http://cars.about.com/od/helpforcarbuyers/tp/ag_top_fuelsave.htm",
                    crawler.getSiblingPage("http://cars.about.com/od/helpforcarbuyers/tp/top10_fuel.htm"));
            assertEquals("http://www.blu-ray.com/movies/movies.php?genre=action&page=1",
                    crawler.getSiblingPage("http://www.blu-ray.com/movies/movies.php?genre=action"));
            assertEquals("http://forums.whirlpool.net.au/forum-replies.cfm?t=1037458",
                    crawler.getSiblingPage("http://forums.whirlpool.net.au/forum-replies-archive.cfm/1037458.html"));
        }
    }

    public void testLinkHandling() {
        Crawler crawler = new Crawler();
        crawler.setDocument(CrawlerTest.class.getResource("/pageContentExtractor/test9.html").getFile());
        assertEquals("http://www.example.com/test.html", crawler.getLinks(true, true).iterator().next());

        crawler.setDocument(CrawlerTest.class.getResource("/pageContentExtractor/test10.html").getFile());
        assertEquals("http://www.example.com/test.html", crawler.getLinks(true, true).iterator().next());
    }

    /**
     * Test undesired behavior from NekoHTML for which we introduced workarounds/fixes.
     * See {@link TBODYFix}.
     */
    public void testNekoWorkarounds() {

        Crawler crawler = new Crawler();
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(CrawlerTest.class.getResource("/webPages/NekoTableTestcase1.html")
                                .getFile()), "//TABLE/TR[1]/TD").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(CrawlerTest.class.getResource("/webPages/NekoTableTestcase2.html")
                                .getFile()), "//TABLE/TBODY/TR[1]/TD").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(CrawlerTest.class.getResource("/webPages/NekoTableTestcase3.html")
                                .getFile()), "//TABLE/TBODY/TR[1]/TD").size());
        assertEquals(
                3,
                XPathHelper.getXhtmlNodes(
                        crawler.getWebDocument(CrawlerTest.class.getResource("/webPages/NekoTableTestcase4.html")
                                .getFile()), "//TABLE/TR[1]/TD").size());

    }

    public void testNekoBugs() {

        // produces a StackOverflowError -- see
        // http://sourceforge.net/tracker/?func=detail&aid=3109537&group_id=195122&atid=952178
        // Crawler crawler = new Crawler();
        // Document doc =
        // crawler.getWebDocument(CrawlerTest.class.getResource("/webPages/NekoTestcase3109537.html").getFile());
        // assertNotNull(doc);

    }

    public void testParseXml() {

        Crawler crawler = new Crawler();

        // parse errors will yield in a null return
        assertNotNull(crawler.getXMLDocument(CrawlerTest.class.getResource("/xmlDocuments/invalid-chars.xml").getFile()));
        assertNotNull(crawler.getXMLDocument(CrawlerTest.class.getResource("/feeds/sourceforge02.xml").getFile()));
        assertNotNull(crawler.getXMLDocument(CrawlerTest.class.getResource("/feeds/feed061.xml").getFile()));

    }

}