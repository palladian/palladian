package ws.palladian.helper.html;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.apache.commons.codec.digest.DigestUtils;
import org.junit.Test;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.ResourceHelper;

/**
 * <p>
 * Test cases for the {@link HtmlHelper} class.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Martin Werner
 */
public class HtmlHelperTest {

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
    public void testHtmlToReadableText() {
        String s = "Event: Dropkick Murphys<br>Venue: Aragon Ballroom<br>Start: 2/22/2013 7:00:00 PM<br>Category: CONCERTS ALTERNATIVE";
        assertEquals(4, HtmlHelper.htmlToReadableText(s).split("\n").length);
        s = "<p class=\"standfirst\">David Cameron describes dismissal of claims that EU law gives prisoners right to vote as 'a great victory for common sense'</p><p>Two convicted murderers who argued that European Union law gave them the right to vote in UK elections have had their appeals dismissed by the supreme court at Westminster.</p>";
        assertEquals(2, HtmlHelper.htmlToReadableText(s).split("\n\n").length);
        s = "<li><b><a href=\"http://www.foxnews.com/politics/2013/10/28/white-house-grants-extension-on-obamacare-signup/\">White House grants extension for ObamaCare signup</a></b></li><li><b><a href=\"http://www.foxnews.com/politics/2013/10/28/obamacare-sites-fail-to-rate-insurance-plans/\">States balk at posting plan ratings</a></b></li><li><b><a href=\"http://www.foxnews.com/politics/2013/10/28/oh-really-healthcaregov-down-as-white-house-declares-site-up-and-running/\">HealthCare.gov 'down,' as White House declares site 'up and running'</a></b></li><li><b><a href=\"http://www.foxnews.com/politics/2013/10/28/krauthammer-obamacare-will-collapse-on-its-own/\">ObamaCare will collapse on its own, Krauthammer says</a></b></li>  <li><b><a href=\"http://www.foxnews.com/politics/2013/10/29/national-review-to-gop-lost-try-winning-some-elections/\">KURTZ: National Review to GOP: You lost! Try winning some elections</a></b></li>  <li><b><a href=\"http://www.foxnews.com/politics/2013/10/29/national-review-jimgeraghty-president-did-not-know/\">TOP TWITTER TALK: 'The president did not know...'</a></b></li>";
        assertEquals(6, HtmlHelper.htmlToReadableText(s).split("\n").length);
    }

    @Test
    public void testStripTags() throws IOException {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        htmlContent = HtmlHelper.joinTagsAndRemoveNewLines(htmlContent);
        assertEquals("this is relevant", HtmlHelper.stripHtmlTags(htmlContent).trim());

        String content = FileHelper.readFileToString(ResourceHelper.getResourceFile("removeHtmlTest.html"));
        String result = HtmlHelper.stripHtmlTags(content);

        assertEquals("65efc6cba6ae65e3e53e15c07e491fc4", DigestUtils.md5Hex(result));

        //        String stripped = "Samsung S8500 Wave 3D view, 360&deg; spin GSMArena.com HomeNewsReviewsBlogRankingsCoverageSoftwareGlossaryFAQLinksContact us Advanced search Samsung S8500 Wave 3D view - 360&deg; spin Samsung S8500 Wave review: Hello, world!Samsung S8500 Wave preview: First lookMWC 2010: Samsung overviewSpecifications Read opinions Compare Pictures Related &nbsp;(new) Manual Check Price WElectronicsPlemixOmio (UK)Mobile City OnlineSelectGSM Popularity Daily interest 48% Total hits: 1266454 Voting results Design 9.1 Features 9.1 Performance 9.1 12345678910 12345678910 12345678910 Votes: 38011 &nbsp; Drag to rotate, double-click to spin 360&deg;. In order to see the 360&deg; rotation the Flash plugin is required. &nbsp; &nbsp; NokiaSamsungMotorolaSony EricssonLGAppleHTCi-mateO2EtenHPGarmin- AsusGigabyteAcerPalmBlackBerryMicrosoftVodafoneT-MobileSagemAlcatelPhilipsSharpToshibaBenQHuaweiPantechi-mobileZTEiNQMicromaxVertu more rumor mill Phone finder Home News Reviews Blog Forum Compare Links Glossary &nbsp;RSS feed &nbsp;Facebook Privacy policy Contact us &copy; 2000 - 2010 GSMArena.com team. Terms of use.";
        //        result = result.replaceAll(System.getProperty("line.separator")," ");
        // System.out.println(DigestUtils.md5Hex(stripped));
        //        Assert.assertEquals(DigestUtils.md5Hex(stripped), DigestUtils.md5Hex(result));
        //        assertThat(result,is(stripped));

        htmlContent = HtmlHelper
                .joinTagsAndRemoveNewLines("<style type=\"text/css\">#abca{}</style><a>some text\n1</a><br />\n\n\n<script>another text</script>");
        assertEquals("some text1", HtmlHelper.stripHtmlTags(htmlContent));
        htmlContent = HtmlHelper
                .joinTagsAndRemoveNewLines("<style type=\"text/css\">#abca{}</style><a>some text\n 2</a><br />");
        assertEquals("some text 2", HtmlHelper.stripHtmlTags(htmlContent));
    }

    //    @Test
    //    public void testDocumentToReadableText() throws FileNotFoundException {
    //        DocumentParser htmlParser = ParserFactory.createHtmlParser();
    //        Document doc = htmlParser.parse(ResourceHelper.getResourceFile("/pageContentExtractor/test001.html"));
    //        String result = HtmlHelper.documentToReadableText(doc);
    //        Assert.assertEquals("489eb91cf94343d0b62e69c396bc6b6f", DigestUtils.md5Hex(result));
    //    }


}
