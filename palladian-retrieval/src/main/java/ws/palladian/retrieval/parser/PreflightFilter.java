package ws.palladian.retrieval.parser;

import org.apache.commons.lang.StringUtils;
import org.apache.xerces.util.XMLChar;
import org.apache.xerces.xni.Augmentations;
import org.apache.xerces.xni.QName;
import org.apache.xerces.xni.XMLAttributes;
import org.apache.xerces.xni.XNIException;
import org.cyberneko.html.filters.DefaultFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Filter out elements and attributes from the Document parsed with NekoHTML which can cause trouble later. This
 * includes elements having a namespace prefix but no associated namespace URI, or attribute names with illegal
 * characters. The first case are usually Facebook or GooglePlus elements in webpages, which have a NS prefix, but no
 * corresponding URI.
 * </p>
 * 
 * @see http://stackoverflow.com/questions/6217434/google-1-button-not-w3c-compliant
 * @author Philipp Katz
 */
class PreflightFilter extends DefaultFilter {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PreflightFilter.class);

    @Override
    public void startElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        if (isAcceptedElement(element.prefix, element.uri)) {
            cleanAttributes(attributes);
            super.startElement(element, attributes, augs);
        } else {
            LOGGER.debug("**** start ingoring element {}", element.rawname);
        }
    }

    @Override
    public void emptyElement(QName element, XMLAttributes attributes, Augmentations augs) throws XNIException {
        if (isAcceptedElement(element.prefix, element.uri)) {
            cleanAttributes(attributes);
            super.emptyElement(element, attributes, augs);
        } else {
            LOGGER.debug("**** ignoring element {}", element.rawname);
        }
    }

    @Override
    public void endElement(QName element, Augmentations augs) throws XNIException {
        if (isAcceptedElement(element.prefix, element.uri)) {
            super.endElement(element, augs);
        } else {
            LOGGER.debug("**** end ingoring element {}", element.rawname);
        }
    }

    /**
     * <p>
     * Determine, whether the element with the specified prefix and namespace URI is accepted.
     * </p>
     * 
     * @param prefix
     * @param uri
     * @return <code>true</code> if the element shall be ignored by the parser, <code>false</code> if element shall be
     *         kept.
     */
    private boolean isAcceptedElement(String prefix, String uri) {
        // element has no prefix
        if (prefix == null || prefix.length() == 0) {
            return true;
        }
        // element has prefix and associated namespace URI
        else if (uri != null && uri.length() > 0) {
            return true;
        }
        // element has prefix, but no namespace URI
        else {
            return false;
        }
    }

    /**
     * <p>
     * Clean up the supplied attributes; check for namespace + URI and validity the attributes' names. Remove those
     * attributes, which do not conform to these conditions.
     * </p>
     * 
     * @param attributes
     */
    private void cleanAttributes(XMLAttributes attributes) {
        for (int i = attributes.getLength() - 1; i >= 0; i--) {
            if (!isAcceptedElement(attributes.getPrefix(i), attributes.getURI(i))) {
                LOGGER.debug("**** removing attribute {}:{}", attributes.getPrefix(i), attributes.getQName(i));
                attributes.removeAttributeAt(i);
            }

            // hotfix to filter out invalid attributes names starting with digit
            // according to bugtracker, this should be fixed in current SVN version
            // http://sourceforge.net/tracker/?func=detail&aid=2828534&group_id=195122&atid=952178
            // -- Philipp, 2010-05-31
            if (attributes.getQName(i) != null
                    && (attributes.getQName(i).equals(":") || !XMLChar.isValidName(attributes.getQName(i)))) {
                LOGGER.debug("**** removing invalid attribute {}", attributes.getQName(i));
                attributes.removeAttributeAt(i);
            }
            
            // http://www.w3.org/TR/REC-xml-names/
            // […] It follows that in a namespace-well-formed document:
            // All element and attribute names contain either zero or one colon; […]
            else if (StringUtils.countMatches(attributes.getQName(i), ":") > 1) {
                LOGGER.debug("**** removing invalid attribute {}", attributes.getQName(i));
                attributes.removeAttributeAt(i);
            }
        }
    }

}
