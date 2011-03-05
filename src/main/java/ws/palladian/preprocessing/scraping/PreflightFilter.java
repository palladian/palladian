package ws.palladian.preprocessing.scraping;

import org.apache.log4j.Logger;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.filters.DefaultFilter;

/**
 * Filter out elements and attributes from the Document parsed with NekoHTML which can cause trouble later. This
 * includes elements from foreign namespaces or attribute names with illegal characters.
 * 
 * @author Philipp Katz
 * 
 */
public class PreflightFilter extends DefaultFilter {

    private Logger logger;

    public PreflightFilter(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        boolean keep = clean(element, attributes);
        if (keep) {
            super.startElement(element, attributes, augs);
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        boolean keep = clean(element, attributes);
        if (keep) {
            super.emptyElement(element, attributes, augs);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        if (filterPrefix(element.prefix)) {
            logger.debug("**** end ingoring element " + element.rawname);
        } else {
            super.endElement(element, augs);
        }
    }

    private boolean clean(QName element, XMLAttributes attributes) {
        boolean keep = true;
        if (filterPrefix(element.prefix)) {
            logger.debug("**** ignoring element " + element.rawname);
            keep = false;
        } else {
            for (int i = attributes.getLength() - 1; i >= 0; i--) {
                if (filterPrefix(attributes.getPrefix(i))) {
                    logger.debug("**** removing attribute " + attributes.getPrefix(i) + ":" + attributes.getQName(i));
                    attributes.removeAttributeAt(i);
                }
                // hotfix to filter out invalid attributes names starting with digit
                // according to bugtracker, this should be fixed in current SVN version
                // http://sourceforge.net/tracker/?func=detail&aid=2828534&group_id=195122&atid=952178
                // -- Philipp, 2010-05-31
                if (attributes.getQName(i) != null && (attributes.getQName(i).equals(":") || !XMLChar.isValidName(attributes.getQName(i)))) {
                    logger.debug("**** removing invalid attribute " + attributes.getQName(i));
                    attributes.removeAttributeAt(i);
                }
            }
        }
        return keep;
    }

    private boolean filterPrefix(String prefix) {
        if (prefix != null && prefix.length() > 0 && !prefix.equals("xml") && !prefix.equals("xmlns")) {
            return true;
        }
        return false;
    }

}
