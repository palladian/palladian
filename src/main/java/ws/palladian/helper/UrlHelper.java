package ws.palladian.helper;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;

public class UrlHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(UrlHelper.class);

    /**
     * Creates a full/absolute URL based on the specified parameters.
     * 
     * Handling links in HTML documents can be tricky. If no absolute URL is specified in the link itself, there are two
     * factors for which we have to take care:
     * <ol>
     * <li>The document's URL</li>
     * <li>If provided, a base URL inside the document, which can be as well be absolute or relative to the document's
     * URL</li>
     * </ol>
     * 
     * @see <a href="http://www.mediaevent.de/xhtml/base.html">HTML base • Basis-Adresse einer Webseite</a>
     * 
     * @param pageUrl actual URL of the document.
     * @param baseUrl base URL defined in document's header, can be <code>null</code> if no base URL is specified.
     * @param linkUrl link URL from the document to be made absolute.
     * @return the absolute URL, empty String, if URL cannot be created or for mailto and javascript links, never
     *         <code>null</code>.
     * 
     * @author Philipp Katz
     */
    public static String makeFullURL(String pageUrl, String baseUrl, String linkUrl) {
        // LOGGER.trace(">makeFullURL " + pageUrl + " " + baseUrl + " " + linkUrl);
        String result = "";
        if (linkUrl != null && !linkUrl.startsWith("javascript") && !linkUrl.startsWith("mailto:")) {
            // let's java.net.URL do all the conversion work from relative to absolute
            URL resultUrl = null;
            // create URL object from the supplied pageUrl
            if (pageUrl != null) {
                try {
                    resultUrl = new URL(pageUrl);
                } catch (MalformedURLException e) {
                    LOGGER.error("makeFullURL: pageUrl: " + e.getMessage());
                }
            }
            // create URL object considering baseUrl, relative to pageUrl
            try {
                if (baseUrl != null) {
                    if (!baseUrl.endsWith("/")) {
                        baseUrl = baseUrl.concat("/");
                    }
                    // this creates a new URL object with resultUrl as "context", which means that the specified baseUrl
                    // is *relative* to the "context"
                    resultUrl = new URL(resultUrl, baseUrl);
                }
            } catch (MalformedURLException e) {
                LOGGER.error("makeFullURL: baseUrl: " + e.getMessage());
            }
            // create URL object considering linkUrl, relative to pageUrl+baseUrl
            try {
                resultUrl = new URL(resultUrl, linkUrl);
            } catch (MalformedURLException e) {
                LOGGER.error("makeFullURL: linkUrl: " + e.getMessage());
            }
            if (resultUrl != null) {
                result = resultUrl.toString();
            }
        }
        // LOGGER.trace("<makeFullURL " + result);
        return result;
    }

    public static String makeFullURL(String pageUrl, String linkUrl) {
        return makeFullURL(pageUrl, null, linkUrl);
    }

    public static String getCleanURL(String url) {
        if (url.startsWith("https://")) {
            url = url.substring(8);
        }
        if (url.startsWith("http://")) {
            url = url.substring(7);
        }
        if (url.startsWith("www.")) {
            url = url.substring(4);
        }
        // if (url.endsWith("/")) url = url.substring(0,url.length()-1);
        return url;
    }

    public static String removeAnchors(String url) {
        return url.replaceAll("#.*", "");
    }

    /**
     * Check if an URL is in a valid form and the file-ending is not blacklisted (see Extractor.java for blacklist)
     * 
     * @param url the URL
     * @param checkHTTPResp the check http resp
     * @return true, if is a valid URL
     * @author Martin Werner
     */
    public static boolean isValidURL(String url) {

        // URLConnection conn = null;
        // URL url = null;
        boolean returnValue = false;

        // FIXME: URL filter for black and whitelists (currently in Extractor)
        // if (MIOExtractor.getInstance().isURLallowed(url)) {

        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_2_SLASHES);

        if (urlValidator.isValid(url)) {
            returnValue = true;
        }

        // }

        return returnValue;
    }

    /**
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>
     * 
     * @param url
     * @param includeProtocol include protocol prefix, e.g. "http://"
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url, boolean includeProtocol) {
        String result = "";
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            if (!host.isEmpty()) {
                if (includeProtocol) {
                    result = urlObj.getProtocol() + "://";
                }
                result += urlObj.getHost();
                LOGGER.trace("root url for " + url + " -> " + result);
            } else {
                LOGGER.trace("no domain specified " + url);
            }
        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine domain " + url);
        }
        return result;
    }

    /**
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>
     * 
     * @param url
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url) {
        return getDomain(url, true);
    }

    /**
     * Check URL for validness and eventually modify e.g. relative path
     * 
     * @param urlCandidate the URLCandidate
     * @param pageURL the pageURL
     * @return the verified URL
     */
    public static String verifyURL(final String urlCandidate, final String pageURL) {

        String returnValue = "";

        final String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (isValidURL(modUrlCandidate)) {
                returnValue = modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                final String modifiedURL = makeFullURL(pageURL, modUrlCandidate);
                if (isValidURL(modifiedURL)) {
                    returnValue = modifiedURL;
                }
            }

        }
        return returnValue;
    }

}
