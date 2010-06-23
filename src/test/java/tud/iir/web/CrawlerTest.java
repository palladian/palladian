package tud.iir.web;

import junit.framework.Assert;
import junit.framework.TestCase;
import tud.iir.control.AllTests;

/**
 * Test cases for the crawler.
 * 
 * @author David Urbansky
 * @author Philipp Katz
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
        // System.out.println(crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", false));
        assertEquals("http://www.flashdevices.net", Crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", true));
        assertEquals("www.flashdevices.net", Crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", false));

        assertEquals("http://blog.wired.com", Crawler.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", true));
        assertEquals("blog.wired.com", Crawler.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", false));

        // added by Philipp
        assertEquals("https://example.com", Crawler.getDomain("https://example.com/index.html"));
        assertEquals("", Crawler.getDomain(""));
        assertEquals("", Crawler.getDomain(null));
        assertEquals("", Crawler.getDomain("file:///test.html"));
        assertEquals("localhost", Crawler.getDomain("file://localhost/test.html", false));
    }
    
    public void testMakeFullURL() {
        
        
        Assert.assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de", "", "page.html"));
        Assert.assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de", null, "page.html"));
        Assert.assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de/index.html", "", "page.html"));
        Assert.assertEquals("http://www.xyz.de/page.html", Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory", "/page.html"));
        Assert.assertEquals("http://www.xyz.de/directory/page.html", Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory", "./page.html"));
        Assert.assertEquals("http://www.xyz.de/directory/page.html", Crawler.makeFullURL("http://www.xyz.de/index.html", "/directory/directory", "../page.html"));

        Assert.assertEquals("http://www.abc.de/page.html", Crawler.makeFullURL("http://www.xyz.de", "", "http://www.abc.de/page.html"));
        Assert.assertEquals("http://www.abc.de/page.html", Crawler.makeFullURL("http://www.xyz.de", "http://www.abc.de/", "/page.html"));
        
        Assert.assertEquals("http://www.example.com/page.html", Crawler.makeFullURL("/some/file/path.html", "http://www.example.com/page.html"));
        Assert.assertEquals("", Crawler.makeFullURL("http://www.xyz.de", "mailto:example@example.com"));
        
        Assert.assertEquals("http://www.example.com/page.html", Crawler.makeFullURL(null, null, "http://www.example.com/page.html"));
        
        // when no linkUrl is supplied, we cannot determine the full URL, so just return an empty String.
        Assert.assertEquals("", Crawler.makeFullURL(null, "http://www.example.com", null));
        Assert.assertEquals("", Crawler.makeFullURL("http://www.example.com", null, null));
        Assert.assertEquals("", Crawler.makeFullURL(null, null, "/page.html"));
        
    }

    public void testGetSiblingPage() {
        if (AllTests.ALL_TESTS) {
            Crawler crawler = new Crawler();
            assertEquals("http://www.cineplex.com/Movies/AllMovies.aspx?sort=2", crawler.getSiblingPage("http://www.cineplex.com/Movies/AllMovies.aspx"));
            assertEquals("http://www.flashdevices.net/2008/02/", crawler
                    .getSiblingPage("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html"));
            assertEquals("http://blog.wired.com/underwire/2008/10/star-trek-trail.html", crawler
                    .getSiblingPage("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html"));
            assertEquals("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110-2,00.htm", crawler
                    .getSiblingPage("http://asia.cnet.com/reviews/notebooks/0,39050495,39315110,00.htm"));
            assertEquals("http://cars.about.com/od/helpforcarbuyers/tp/ag_top_fuelsave.htm", crawler
                    .getSiblingPage("http://cars.about.com/od/helpforcarbuyers/tp/top10_fuel.htm"));
            assertEquals("http://www.blu-ray.com/movies/movies.php?genre=action&page=1", crawler
                    .getSiblingPage("http://www.blu-ray.com/movies/movies.php?genre=action"));
            assertEquals("http://forums.whirlpool.net.au/forum-replies.cfm?t=1037458", crawler
                    .getSiblingPage("http://forums.whirlpool.net.au/forum-replies-archive.cfm/1037458.html"));
        }
    }

    public void testLinkHandling() {
        Crawler crawler = new Crawler();
        crawler.setDocument("data/test/pageContentExtractor/test9.html");
        assertEquals("http://www.example.com/test.html", crawler.getLinks(true, true).iterator().next());

        crawler.setDocument("data/test/pageContentExtractor/test10.html");
        assertEquals("http://www.example.com/test.html", crawler.getLinks(true, true).iterator().next());
    }

}