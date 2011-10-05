package ws.palladian.helper;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

public class UrlHelperTest {

    @Test
    public void testGetCleanUrl() {
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("http://www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("http://amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("https://www.amazon.com/"));
        assertEquals("amazon.com", UrlHelper.getCleanUrl("https://amazon.com"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("www.amazon.com/"));
        assertEquals("amazon.com/", UrlHelper.getCleanUrl("amazon.com/"));
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
    public void testMakeFullUrl() {

        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html", UrlHelper.makeFullUrl("http://www.xyz.de", null, "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "", "page.html"));
        assertEquals("http://www.xyz.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory", "/page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory", "./page.html"));
        assertEquals("http://www.xyz.de/directory/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de/index.html", "/directory/directory", "../page.html"));

        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de", "", "http://www.abc.de/page.html"));
        assertEquals("http://www.abc.de/page.html",
                UrlHelper.makeFullUrl("http://www.xyz.de", "http://www.abc.de/", "/page.html"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullUrl("/some/file/path.html", "http://www.example.com/page.html"));
        assertEquals("", UrlHelper.makeFullUrl("http://www.xyz.de", "mailto:example@example.com"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullUrl(null, null, "http://www.example.com/page.html"));

        // when no linkUrl is supplied, we cannot determine the full URL, so just return an empty String.
        assertEquals("", UrlHelper.makeFullUrl(null, "http://www.example.com", null));
        assertEquals("", UrlHelper.makeFullUrl("http://www.example.com", null, null));
        assertEquals("", UrlHelper.makeFullUrl(null, null, "/page.html"));

    }
    
    @Test
    public void testExtractUrls() {

        String text = "The quick brown fox jumps over the lazy dog. Check out: http://microsoft.com, www.apple.com, google.com. (www.tu-dresden.de), http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars.";
        List<String> urls = UrlHelper.extractUrls(text);

        assertEquals(4, urls.size());
        assertEquals("http://microsoft.com", urls.get(0));
        assertEquals("www.apple.com", urls.get(1));
        // assertEquals("google.com", urls.get(2)); // not recognized
        assertEquals("www.tu-dresden.de", urls.get(2));
        assertEquals(
                "http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars",
                urls.get(3));

    }

}
