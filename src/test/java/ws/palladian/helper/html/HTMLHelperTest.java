package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;
import junit.framework.Assert;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Test;
import org.w3c.dom.Document;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * Test cases for the HTMLHelper class.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class HtmlHelperTest {



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
        assertEquals(4, HtmlHelper.countTags("everybody is <b>here</b> to do some <p>work</p>"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody is <b>here</b> to do some <p>work"));
        assertEquals(4, HtmlHelper.countTags("<br />everybody<br /> is <b>here</b> to do some <p>work", true));
        assertEquals(7, HtmlHelper.countTags("<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <p>work"));
        assertEquals(6, HtmlHelper.countTags(
                "<br /><a>abc</a>everybody<br /> is <b>here</b> to do some <a>abc</a> <p>work", true));
    }

    @Test
    public void testCountTagLength() {
        assertEquals(0, HtmlHelper.countTagLength("iphone 4"));
        assertEquals(15, HtmlHelper.countTagLength("<phone>iphone 4</Phone>"));
        assertEquals(20, HtmlHelper.countTagLength("everybody is <b>here<br /></b> to do some <p>work</p>"));
    }

    @Test
    public void testStripTags() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant",
                HtmlHelper.stripHtmlTags(htmlContent, true, true, true, true).trim());

        DocumentRetriever crawler = new DocumentRetriever();
        String content = crawler.getTextDocument(HtmlHelperTest.class.getResource("/webPages/removeHTMLContentTest1.html")
                .getFile());
        String result = HtmlHelper.stripHtmlTags(content, true, true, true, false).replaceAll("(\\s){2,}", " ").trim();
        System.out.println(result);
        // System.out.println(DigestUtils.md5Hex(result));
        Assert.assertEquals("ecf0720bd7f9afc0dc40ec100ca8e96f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString() {
        DocumentRetriever c = new DocumentRetriever();
        Document doc = c.getWebDocument(HtmlHelperTest.class.getResource("/pageContentExtractor/test001.html")
                .getFile());
        String result = HtmlHelper.documentToReadableText(doc);
        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    }

    @Test
    public void testHtmlToString2() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        System.out.println(HtmlHelper.documentToReadableText(htmlContent, true));
        // assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

    @Test
    public void testReplaceHTMLSymbols() {
        String htmlText = "&nbsp; &Auml; &auml; &Ouml; &ouml; &Uuml; &uuml; &szlig; &lt; &gt; &amp; &quot;";
        String clearText = "  Ä ä Ö ö Ü ü ß < > & \"";
        System.out.println(StringEscapeUtils.unescapeHtml(htmlText));
        assertEquals(clearText, StringHelper.replaceProtectedSpace(StringEscapeUtils.unescapeHtml(htmlText)));
    }
}
