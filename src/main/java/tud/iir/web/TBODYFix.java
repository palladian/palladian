package tud.iir.web;

import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.filters.DefaultFilter;

/**
 * Fix for newer NekoHTML versions which insert TBODY tags into tables, which we do not want. The main problem are not
 * the TBODY tags per se, but the fact that those tags are not in the xhtml namespace. So XPath expressions on documents
 * with xhtml namespaces will fail, as all tags except TBODY are in xhtml namespace. This fix will remove all TBODY tags
 * which have no xhtml namespace.
 * 
 * I suspect this is a bug which might get fixed in later versions of NekoHTML. There is test case
 * {@link CrawlerTest#testNekoWorkarounds()} to track this issue.
 * 
 * See:
 * http://nekohtml.sourceforge.net/changes.html (Version 1.9.13 (2 Sept 2009))
 * http://redmine.effingo.de/issues/5
 * 
 * @author Philipp Katz
 * 
 */
/* package */class TBODYFix extends DefaultFilter {

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        if (isInsertedTBODY(element)) {
            // ignore.
        } else {
            super.startElement(element, attributes, augs);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        if (isInsertedTBODY(element)) {
            // ignore.
        } else {
            super.endElement(element, augs);
        }
    }

    /**
     * Check, if TBODY element was inserted by Neko.
     * 
     * @param element
     * @return
     */
    private boolean isInsertedTBODY(QName element) {
        return element.rawname.equalsIgnoreCase("TBODY") && element.prefix == null && element.uri == null;
    }

}
