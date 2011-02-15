package tud.iir.helper;

import static org.junit.Assert.*;

import org.junit.Test;

public class XPathHelperTest {

    @Test
    public void testAddNamespaceToXPath() {

        // test add XMLNS
        assertEquals(XPathHelper.addNameSpaceToXPath("//TABLE/TR/TD/A[4]"),
                "//xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addNameSpaceToXPath("/TABLE/TR/TD/A[4]"), "/xhtml:TABLE/xhtml:TR/xhtml:TD/xhtml:A[4]");
        assertEquals(XPathHelper.addNameSpaceToXPath("/TABLE/TR[2]/TD/A"), "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A");
        assertEquals(XPathHelper.addNameSpaceToXPath("/TABLE/TR[2]/TD/A/text()"),
                "/xhtml:TABLE/xhtml:TR[2]/xhtml:TD/xhtml:A/text()");

    }

}
