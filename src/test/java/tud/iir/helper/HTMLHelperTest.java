package tud.iir.helper;

import junit.framework.TestCase;

import org.junit.Test;

/**
 * Test cases for the HTMLHelper class.
 * 
 * @author David Urbansky
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
    public void testRemoveHTMLTags() {
        String htmlContent = "<html lang=\"en-us\"> <script language=\"JavaScript\" type=\"text/javascript\">var MKTCOUNTRY = \"USA\"</script>this is relevant <!-- function open_doc (docHref) {document.location.href = '/sennheiser/home_de.nsf/' + docHref;}--> </html>";
        assertEquals("this is relevant", HTMLHelper.removeHTMLTags(htmlContent, true, true, true, false));

    }

}
