package ws.palladian.helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class UrlHelperTest {

    @Test
    public void testGetCleanURL() {
        assertEquals("amazon.com/", UrlHelper.getCleanURL("http://www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanURL("http://amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanURL("https://www.amazon.com/"));
        assertEquals("amazon.com", UrlHelper.getCleanURL("https://amazon.com"));
        assertEquals("amazon.com/", UrlHelper.getCleanURL("www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanURL("amazon.com/"));
    }

    @Test
    public void testGetDomain() {
        // System.out.println(crawler.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html",
        // false));
        assertEquals("http://www.flashdevices.net",
                UrlHelper.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", true));
        assertEquals("www.flashdevices.net",
                UrlHelper.getDomain("http://www.flashdevices.net/2008/02/updated-flash-enabled-devices.html", false));

        assertEquals("http://blog.wired.com",
                UrlHelper.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", true));
        assertEquals("blog.wired.com",
                UrlHelper.getDomain("http://blog.wired.com/underwire/2008/10/theres-yet-anot.html", false));

        // added by Philipp
        assertEquals("https://example.com", UrlHelper.getDomain("https://example.com/index.html"));
        assertEquals("", UrlHelper.getDomain(""));
        assertEquals("", UrlHelper.getDomain(null));
        assertEquals("", UrlHelper.getDomain("file:///test.html")); // TODO return localhost here?
        assertEquals("localhost", UrlHelper.getDomain("file://localhost/test.html", false));
    }

    @Test
    public void testMakeFullURL() {

        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullURL("http://www.xyz.de", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullURL("http://www.xyz.de", null, "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de/index.html", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de/index.html", "/directory", "/page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de/index.html", "/directory", "./page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de/index.html", "/directory/directory", "../page.html"));

        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de", "", "http://www.abc.de/page.html"));
        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullURL("http://www.xyz.de", "http://www.abc.de/", "/page.html"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullURL("/some/file/path.html", "http://www.example.com/page.html"));
        assertEquals("", UrlHelper.makeFullURL("http://www.xyz.de", "mailto:example@example.com"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullURL(null, null, "http://www.example.com/page.html"));

        // when no linkUrl is supplied, we cannot determine the full URL, so just return an empty String.
        assertEquals("", UrlHelper.makeFullURL(null, "http://www.example.com", null));
        assertEquals("", UrlHelper.makeFullURL("http://www.example.com", null, null));
        assertEquals("", UrlHelper.makeFullURL(null, null, "/page.html"));

    }

}
