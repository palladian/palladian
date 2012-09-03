package ws.palladian.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.matchers.JUnitMatchers.hasItem;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Test;

/** @formatter:off */
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
        assertEquals("mailto:example@example.com", UrlHelper.makeFullUrl("http://www.xyz.de", "mailto:example@example.com"));

        assertEquals("http://www.example.com/page.html",
                UrlHelper.makeFullUrl(null, null, "http://www.example.com/page.html"));

        // when no linkUrl is supplied, we cannot determine the full URL, UrlHelper throws NPE
        try {
            assertEquals(null, UrlHelper.makeFullUrl(null, "http://www.example.com", null));
            assertEquals("", UrlHelper.makeFullUrl("http://www.example.com", null, null));
            assertEquals("", UrlHelper.makeFullUrl(null, null, "/page.html"));
            fail();
        } catch (NullPointerException e) {
            
        }
    }
    
    @Test
    public void testExtractUrls() throws FileNotFoundException {

        String text = "The quick brown fox jumps over the lazy dog. Check out: http://microsoft.com, www.apple.com, google.com. (www.tu-dresden.de), http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars.";
        List<String> urls = UrlHelper.extractUrls(text);
        assertThat(urls, hasItem("http://microsoft.com"));
        assertThat(urls, hasItem("www.apple.com"));
        assertThat(urls, hasItem("google.com"));
        assertThat(urls, hasItem("www.tu-dresden.de"));
        assertThat(urls, hasItem("http://arstechnica.com/open-source/news/2010/10/mozilla-releases-firefox-4-beta-for-maemo-and-android.ars"));
        
        // test URLs from <http://daringfireball.net/2010/07/improved_regex_for_matching_urls>
        
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("http://foo.com/blah_blah/").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("(Something like http://foo.com/blah_blah)").get(0));
        assertEquals("http://foo.com/blah_blah_(wikipedia)", UrlHelper.extractUrls("http://foo.com/blah_blah_(wikipedia)").get(0));
        assertEquals("http://foo.com/more_(than)_one_(parens)", UrlHelper.extractUrls("http://foo.com/more_(than)_one_(parens)").get(0));
        assertEquals("http://foo.com/blah_blah_(wikipedia)", UrlHelper.extractUrls("(Something like http://foo.com/blah_blah_(wikipedia))").get(0));
        assertEquals("http://foo.com/blah_(wikipedia)#cite-1", UrlHelper.extractUrls("http://foo.com/blah_(wikipedia)#cite-1").get(0));
        assertEquals("http://foo.com/blah_(wikipedia)_blah#cite-1", UrlHelper.extractUrls("http://foo.com/blah_(wikipedia)_blah#cite-1").get(0));
        assertEquals("http://foo.com/unicode_(✪)_in_parens", UrlHelper.extractUrls("http://foo.com/unicode_(✪)_in_parens").get(0));
        assertEquals("http://foo.com/(something)?after=parens", UrlHelper.extractUrls("http://foo.com/(something)?after=parens").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah.").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("http://foo.com/blah_blah/.").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("<http://foo.com/blah_blah>").get(0));
        assertEquals("http://foo.com/blah_blah/", UrlHelper.extractUrls("<http://foo.com/blah_blah/>").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("http://foo.com/blah_blah,").get(0));
        assertEquals("http://www.extinguishedscholar.com/wpglob/?p=364", UrlHelper.extractUrls("http://www.extinguishedscholar.com/wpglob/?p=364.").get(0));
        assertEquals("http://example.com", UrlHelper.extractUrls("<tag>http://example.com</tag>").get(0));
        assertEquals("www.example.com", UrlHelper.extractUrls("Just a www.example.com link.").get(0));
        assertEquals("http://example.com/something?with,commas,in,url", UrlHelper.extractUrls("http://example.com/something?with,commas,in,url, but not at end").get(0));
        assertEquals("bit.ly/foo", UrlHelper.extractUrls("bit.ly/foo").get(0));
//        assertEquals("is.gd/foo/", UrlHelper.extractUrls("“is.gd/foo/”").get(0));
        assertEquals("WWW.EXAMPLE.COM", UrlHelper.extractUrls("WWW.EXAMPLE.COM").get(0));
////        assertEquals("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))/Web_ENG/View_DetailPhoto.aspx?PicId=752", UrlHelper.extractUrls("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))/Web_ENG/View_DetailPhoto.aspx?PicId=752").get(0));
////        assertEquals("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))", UrlHelper.extractUrls("http://www.asianewsphoto.com/(S(neugxif4twuizg551ywh3f55))").get(0));
////        assertEquals("http://lcweb2.loc.gov/cgi-bin/query/h?pp/horyd:@field(NUMBER+@band(thc+5a46634))", UrlHelper.extractUrls("http://lcweb2.loc.gov/cgi-bin/query/h?pp/horyd:@field(NUMBER+@band(thc+5a46634))").get(0));
        assertEquals("http://example.com/quotes-are-“part”", UrlHelper.extractUrls("http://example.com/quotes-are-“part”").get(0));
        assertEquals("example.com", UrlHelper.extractUrls("example.com").get(0));
        assertEquals("example.com/", UrlHelper.extractUrls("example.com/").get(0));
        assertThat(UrlHelper.extractUrls("[url=http://foo.com/blah_blah]http://foo.com/blah_blah[/url]"), hasItem("http://foo.com/blah_blah"));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("'http://foo.com/blah_blah'").get(0));
        assertEquals("http://foo.com/blah_blah", UrlHelper.extractUrls("\"http://foo.com/blah_blah\"").get(0));
        
        assertEquals("cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("1-2-3.net/auctions-Are-out.jpg", UrlHelper.extractUrls("You can download it here: 1-2-3.net/auctions-Are-out.jpg but be aware of the size.").get(0));
        assertEquals("http://www.cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("www.cinefreaks.com/coolstuff.zip", UrlHelper.extractUrls("You can download it here: www.cinefreaks.com/coolstuff.zip but be aware of the size.").get(0));
        assertEquals("http://www.cinefreaks.com/", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com/").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com.").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com?").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com! Or somewhere else").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com. This is the next sentence").get(0));
        assertEquals("http://www.cinefreaks.com", UrlHelper.extractUrls("You can download it here: http://www.cinefreaks.com, this is the next...").get(0));
        assertEquals("http://www.google.com/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m", UrlHelper.extractUrls("http://www.google.com/search?tbm=isch&hl=en&source=hp&biw=1660&bih=751&q=alfred+neuman+mad+magazine&gbv=2&aq=1s&aqi=g1g-s1g-sx1&aql=&oq=alfred+newman+m").get(0));

        assertEquals("http://add.my.yahoo.com/rss?url=http://feeds.reuters.com/news/artsculture", UrlHelper.extractUrls("http://add.my.yahoo.com/rss?url=http://feeds.reuters.com/news/artsculture").get(0));
        assertEquals("http://stockscreener.us.reuters.com/Stock/US/Index?quickscreen=gaarp", UrlHelper.extractUrls("http://stockscreener.us.reuters.com/Stock/US/Index?quickscreen=gaarp").get(0));

        // no URLs
        assertEquals(0, UrlHelper.extractUrls("6:00p").size());
        assertEquals(0, UrlHelper.extractUrls("filename.txt").size());

        assertEquals(0, UrlHelper.extractUrls("16-28-33.0.backup.allcues.update.7z").size());
        assertEquals(0, UrlHelper.extractUrls("09.Sep.11").size());
        assertEquals(0, UrlHelper.extractUrls("Environment.CurrentDirectory").size());
        assertEquals(0, UrlHelper.extractUrls("zipProcess.StandardOutput.ReadToEnd()").size());
        
        assertEquals(0, UrlHelper.extractUrls("check_lang.sh").size());
    }
    
    @Test
    public void testRemoveSessionId() {
        assertEquals("http://brbb.freeforums.org/viewforum.php?f=3&", UrlHelper.removeSessionId(
                "http://brbb.freeforums.org/viewforum.php?f=3&sid=5c2676a9f621ffbadb6962da7e0c50d4"));
    }
    
    @Test
    public void testGetCanonicalUrl() {
        assertEquals("http://www.funs.co.uk/comic/",
                UrlHelper.getCanonicalUrl("http://www.funs.co.uk/comic/index.html"));
        assertEquals(
                "http://sourceforge.net/tracker/?aid=1954302&atid=377408&func=detail&group_id=23067",
                UrlHelper
                        .getCanonicalUrl("http://sourceforge.net/tracker/?func=detail&aid=1954302&group_id=23067&atid=377408"));
        assertEquals("http://sourceforge.net/", UrlHelper.getCanonicalUrl("http://sourceforge.net/"));
        assertEquals(
                "http://sourceforge.net/tracker/?aid=3492945&atid=377408&func=detail&group_id=23067",
                UrlHelper
                        .getCanonicalUrl("http://sourceforge.net/tracker/?func=detail&aid=3492945&group_id=23067&atid=377408#artifact_comment_6199621"));
    }

}
