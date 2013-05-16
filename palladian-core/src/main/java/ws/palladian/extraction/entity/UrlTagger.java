package ws.palladian.extraction.entity;

import ws.palladian.helper.UrlHelper;

/**
 * <p>
 * Tag URLs in a text.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class UrlTagger extends RegExTagger {

    /** The tag name for URLs. */
    public static final String URI_TAG_NAME = "URI";

    public UrlTagger() {
        super(UrlHelper.URL_PATTERN, URI_TAG_NAME);
    }

}