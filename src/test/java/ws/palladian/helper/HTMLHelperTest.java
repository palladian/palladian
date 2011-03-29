package ws.palladian.helper;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.web.DocumentRetriever;

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

    // @Test
    // public void testGetHTMLContent() {
    //
    // Crawler crawler = new Crawler();
    // Document document = crawler.getWebDocument(HTMLHelperTest.class.getResource("/webPages/newsPage1.html")
    // .getFile());
    //
    // // System.out.println(PageAnalyzer.getRawMarkup(document));
    // System.out.println(HTMLHelper.htmlToReadableText(document));
    //
    // }

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
        assertEquals("this is relevant", HTMLHelper.stripHTMLTags(htmlContent, true, true, true, false));

        DocumentRetriever crawler = new DocumentRetriever();
        String content = crawler.download(HTMLHelperTest.class.getResource("/webPages/removeHTMLContentTest1.html")
                .getFile());
        String result = HTMLHelper.stripHTMLTags(content, true, true, true, false);
        // System.out.println(result);
        // System.out.println(DigestUtils.md5Hex(result));
        Assert.assertEquals("ecf0720bd7f9afc0dc40ec100ca8e96f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString() {
        DocumentRetriever c = new DocumentRetriever();
        Document doc = c.getWebDocument(HTMLHelperTest.class.getResource("/pageContentExtractor/test001.html")
                .getFile());
        String result = HTMLHelper.documentToReadableText(doc);
        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString2() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        System.out.println(HTMLHelper.documentToReadableText(htmlContent, true));
        // assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

    @Test
    public void testReplaceHTMLSymbols() {
        String htmlText = "&nbsp; &Auml; &auml; &Ouml; &ouml; &Uuml; &uuml; &szlig; &lt; &gt; &amp; &quot;";
        String clearText = "  Ä ä Ö ö Ü ü ß < > & \"";
        assertEquals(clearText, HTMLHelper.replaceHTMLSymbols(htmlText));
    }
}
