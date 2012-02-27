package de.philippkatz;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.ResourceHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.html.HtmlHelper;

public class HtmlStripperTest {

    @Test
    public void performanceCheck() throws FileNotFoundException {
        String text = FileHelper.readFileToString(ResourceHelper.getResourceFile("/webPages/bigPage.html"));

        StopWatch stopWatch = new StopWatch();
        String htmlHelperResult = HtmlHelper.stripHtmlTags(text, true, true, false, false);
        System.out.println("HtmlHelper: " + stopWatch.getTotalElapsedTimeString());

        StopWatch stopWatch2 = new StopWatch();
        String htmlStripperResult = HtmlStripper.stripHtmlComments(text);
        htmlStripperResult = HtmlStripper.stripHtmlTagAndContent(text, "script");
        htmlStripperResult = HtmlStripper.stripHtmlTagAndContent(text, "style");
        htmlStripperResult = HtmlStripper.stripHtmlTags(text);
        System.out.println("HtmlStripper: " + stopWatch2.getElapsedTimeString());

        System.out.println("speed improvement: " + stopWatch.getElapsedTime() / stopWatch2.getElapsedTime());
        System.out.println("results equal: " + htmlHelperResult.equals(htmlStripperResult));
    }

    @Test
    public void testStripTags() throws FileNotFoundException {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        String str = HtmlStripper.stripHtmlTagAndContent(htmlContent, "script");
        str = HtmlStripper.stripHtmlComments(str);
        str = HtmlStripper.stripHtmlTags(str);
        assertEquals("this is relevant", str.trim());

        String content = FileHelper.readFileToString(ResourceHelper
                .getResourceFile("/webPages/removeHTMLContentTest1.html"));
        String result = HtmlStripper.stripHtmlComments(content);
        result = HtmlStripper.stripHtmlTagAndContent(result, "style");
        result = HtmlStripper.stripHtmlTagAndContent(result, "script");
        result = HtmlStripper.stripHtmlTags(result);
        result = result.replaceAll("(\\s){2,}", " ").trim();
        result = result.replaceAll(System.getProperty("line.separator"), " ");

        String stripped = "Samsung S8500 Wave 3D view, 360&deg; spin GSMArena.com HomeNewsReviewsBlogRankingsCoverageSoftwareGlossaryFAQLinksContact us Advanced search Samsung S8500 Wave 3D view - 360&deg; spin Samsung S8500 Wave review: Hello, world!Samsung S8500 Wave preview: First lookMWC 2010: Samsung overviewSpecifications Read opinions Compare Pictures Related &nbsp;(new) Manual Check Price WElectronicsPlemixOmio (UK)Mobile City OnlineSelectGSM Popularity Daily interest 48% Total hits: 1266454 Voting results Design 9.1 Features 9.1 Performance 9.1 12345678910 12345678910 12345678910 Votes: 38011 &nbsp; Drag to rotate, double-click to spin 360&deg;. In order to see the 360&deg; rotation the Flash plugin is required. &nbsp; &nbsp; NokiaSamsungMotorolaSony EricssonLGAppleHTCi-mateO2EtenHPGarmin- AsusGigabyteAcerPalmBlackBerryMicrosoftVodafoneT-MobileSagemAlcatelPhilipsSharpToshibaBenQHuaweiPantechi-mobileZTEiNQMicromaxVertu more rumor mill Phone finder Home News Reviews Blog Forum Compare Links Glossary &nbsp;RSS feed &nbsp;Facebook Privacy policy Contact us &copy; 2000 - 2010 GSMArena.com team. Terms of use.";
        result = result.replaceAll(System.getProperty("line.separator"), " ");
        assertEquals(DigestUtils.md5Hex(stripped), DigestUtils.md5Hex(result));
    }

    @Test
    public void testStripHtmlTags() {
        assertEquals("This is some text with tags",
                HtmlStripper.stripHtmlTags("<b>This</b> is some <i>text</i> with tags"));
    }

    @Test
    public void testStripHtmlComments() {
        assertEquals("This is a text ", HtmlStripper.stripHtmlComments("This is a text <!-- with a comment -->"));
    }

    @Test
    public void testStripHtmlTagWithContent() {
        assertEquals("This is a text ",
                HtmlStripper.stripHtmlTagAndContent("This is a text <b>with bold words</b>", "b"));
    }
}
