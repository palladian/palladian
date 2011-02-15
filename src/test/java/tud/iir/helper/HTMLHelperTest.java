package tud.iir.helper;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import tud.iir.web.Crawler;

/**
 * Test cases for the HTMLHelper class.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class HTMLHelperTest extends TestCase {

    public HTMLHelperTest(String name) {
        super(name);
    }

    @Test
    public void testCountTags() {
        assertEquals(4, HTMLHelper.countTags("everybody is <b>here</b> to do some <p>work</p>"));
        assertEquals(4, HTMLHelper.countTags("<br />everybody is <b>here</b> to do some <p>work"));
        assertEquals(4, HTMLHelper.countTags("<br />everybody<br /> is <b>here</b> to do some <p>work", true));
        assertEquals(7, HTMLHelper.countTags("<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <p>work"));
        assertEquals(6, HTMLHelper.countTags(
                "<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <a>abc</a> <p>work", true));
    }

    @Test
    public void testCountTagLength() {
        assertEquals(0, HTMLHelper.countTagLength("iphone 4"));
        assertEquals(15, HTMLHelper.countTagLength("<phone>iphone 4</Phone>"));
        assertEquals(20, HTMLHelper.countTagLength("everybody is <b>here<br /></b> to do some <p>work</p>"));
    }

    @Test
    public void testStripTags() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

        Crawler crawler = new Crawler();
        String content = crawler.download(HTMLHelperTest.class.getResource("/webPages/removeHTMLContentTest1.html")
                .getFile());
        String result = HTMLHelper.removeHTMLTags(content, true, true, true, false);
        // System.out.println(result);
        // System.out.println(DigestUtils.md5Hex(result));
        Assert.assertEquals("c104399f6ad077a642161ba03be83bdb", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString() {
        Crawler c = new Crawler();
        Document doc = c.getWebDocument(HTMLHelperTest.class.getResource("/pageContentExtractor/test001.html")
                .getFile());
        String result = HTMLHelper.htmlToString(doc);
        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString2() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        System.out.println(HTMLHelper.htmlToString(htmlContent, true));
        // assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

    @Test
    public void testReplaceHTMLSymbols() {
        String htmlText = "&nbsp; &Auml; &auml; &Ouml; &ouml; &Uuml; &uuml; &szlig; &lt; &gt; &amp; &quot;";
        String clearText = "  Ä ä Ö ö Ü ü ß < > & \"";
        assertEquals(clearText, HTMLHelper.replaceHTMLSymbols(htmlText));
    }
}
