package ws.palladian.helper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * Various helper methods for working with URLs.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Sandro Reichert
 * @author Julien Schmehl
 */
public final class UrlHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

    /** Names of attributes in (X)HTML containing links. */
    private static final List<String> LINK_ATTRIBUTES = Arrays.asList("href", "src");

    /** RegEx pattern defining a session ID. */
    private static final Pattern SESSION_ID_PATTERN = Pattern
            .compile("[&;]?(?<!\\w)(jsessionid=|s=|sid=|PHPSESSID=|sessionid=)[A-Za-z_0-9\\-]{32,200}(?!\\w)");

    /** List of top level domains. */
    private static final String TOP_LEVEL_DOMAINS = "ac|ad|ae|aero|af|ag|ai|al|am|an|ao|aq|ar|as|asia|at|au|aw|ax|az|ba|bb|bd|be|" +
            "bf|bg|bh|bi|biz|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cat|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|com|coop|cr|cs|cu|cv|cx|cy|" +
            "cz|dd|de|dj|dk|dm|do|dz|ec|edu|ee|eg|eh|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gov|gp|gq|gr|" +
            "gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|info|int|io|iq|ir|is|it|je|jm|jo|jobs|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|" +
            "ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mil|mk|ml|mm|mn|mo|mobi|mp|mq|mr|ms|mt|mu|museum|mv|mw|mx|" +
            "my|mz|na|name|nc|ne|net|nf|ng|ni|nl|no|np|nr|nu|nz|om|org|pa|pe|pf|pg|ph|pk|pl|pm|pn|pr|pro|ps|pt|pw|py|qa|re|ro|rs|" +
            "ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|ss|st|su|sv|sy|sz|tc|td|tel|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|" +
            "travel|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|xxx|ye|yt|yu|za|zm|zw";

    // adapted version from <http://daringfireball.net/2010/07/improved_regex_for_matching_urls>
    // this is able to match URLs, containing (brackets), but does not include trailing brackets
    public static final Pattern URL_PATTERN = Pattern
            .compile(
                    "\\b(?:https?://)?([0-9a-zäöü-]{1,63}?\\.)+(?:"
                            + TOP_LEVEL_DOMAINS
                            + ")(?:[?/](?:\\([^\\s()<>\\[\\]\"']{0,255}\\)|[^\\s()<>\\[\\]\"']{0,255})+(?:\\([^\\s()<>\\[\\]\"']{0,255}\\)|[^\\s.,;!?:()<>\\[\\]\"'])|/|\\b)",
                            Pattern.CASE_INSENSITIVE);

    private UrlHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Remove sessions IDs from a URL.
     * </p>
     * 
     * @param originalUrl The URL from which to remove the session ID.
     * @return The URL without the session ID if one was present, or the original URL if no session ID was found,
     *         <code>null</code> in case the original URL was <code>null</code>.
     */
    public static String removeSessionId(String originalUrl) {
        if (originalUrl == null) {
            return null;
        }
        return SESSION_ID_PATTERN.matcher(originalUrl).replaceAll("").replaceAll("\\?$", "").replaceAll("\\?&", "?");
    }

    /**
     * <p>
     * Transforms all relative URLs (i.e. in attributes <tt>href</tt> and <tt>src</tt>) of an (X)HTML {@link Document}
     * to full, absolute URLs. The document's URL should be specified using {@link Document#setDocumentURI(String)}.
     * </p>
     * 
     * @param document The document for which to create absolute URLs, this will be modified in-place
     */
    public static void makeAbsoluteUrls(Document document) {

        String documentUrl = document.getDocumentURI();
        String baseUrl = getBaseUrl(document);

        for (String attribute : LINK_ATTRIBUTES) {
            String xpath = "//*[@" + attribute + "]";
            List<Node> nodes = XPathHelper.getXhtmlNodes(document, xpath);
            for (Node node : nodes) {
                Node attributeNode = node.getAttributes().getNamedItem(attribute);
                String value = attributeNode.getNodeValue();
                String fullValue = makeFullUrl(documentUrl, baseUrl, value);
                if (!fullValue.equals(value)) {
                    LOGGER.debug("{} -> {}", value, fullValue);
                    attributeNode.setNodeValue(fullValue);
                }
            }
        }
    }

    /**
     * <p>
     * Get the Base URL of the supplied (X)HTML document, which is specified via the <tt>base</tt> tag in the document's
     * header.
     * </p>
     * 
     * @param document
     * @return The base URL, if present, <code>null</code> otherwise.
     */
    public static String getBaseUrl(Document document) {
        Node baseNode = XPathHelper.getXhtmlNode(document, "//head/base/@href");
        if (baseNode != null) {
            return baseNode.getTextContent();
        }
        return null;
    }

    /**
     * <p>
     * Try to create a full/absolute URL based on the specified parameters. Handling links in HTML documents can be
     * tricky. If no absolute URL is specified in the link itself, there are two factors for which we have to take care:
     * </p>
     * <ol>
     * <li>The document's URL</li>
     * <li>If provided, a base URL inside the document, which can be as well be absolute or relative to the document's
     * URL</li>
     * </ol>
     * 
     * @see <a href="http://www.mediaevent.de/xhtml/base.html">HTML base • Basis-Adresse einer Webseite</a>
     * 
     * @param pageUrl actual URL of the document, can be <code>null</code>.
     * @param baseUrl base URL defined in document's header, can be <code>null</code> if no base URL is specified.
     * @param linkUrl link URL from the document to be made absolute, not <code>null</code>.
     * @return the absolute URL, empty String, if URL cannot be created, never <code>null</code>.
     * 
     * @author Philipp Katz
     */
    public static String makeFullUrl(String pageUrl, String baseUrl, String linkUrl) {
        if (linkUrl == null) {
            throw new NullPointerException("linkUrl must not be null");
        }
        if (baseUrl != null && !baseUrl.endsWith("/")) {
            baseUrl = baseUrl.concat("/");
        }
        String contextUrl;
        if (pageUrl != null && baseUrl != null) {
            contextUrl = makeFullUrl(pageUrl, baseUrl);
        } else if (pageUrl != null) {
            contextUrl = pageUrl;
        } else {
            contextUrl = baseUrl;
        }
        return makeFullUrl(contextUrl, linkUrl);
    }

    public static String makeFullUrl(String contextUrl, String linkUrl) {
        String result = linkUrl;
        if (contextUrl != null) {
            try {
                result = new URL(new URL(contextUrl), linkUrl).toString();
            } catch (MalformedURLException e) {
                // don't care
            }
        }
        return result;
    }

    public static String getCleanUrl(String url) {
        if (url == null) {
            url = "";
        }
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
     * <p>
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>.
     * </p>
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
                LOGGER.trace("root url for {} -> {}", url, result);
            } else {
                LOGGER.trace("no domain specified {}", url);
            }
        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine domain for {}", url);
        }
        return result;
    }

    /**
     * <p>
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>.
     * </p>
     * 
     * @param url
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url) {
        return getDomain(url, true);
    }

    /**
     * <p>
     * Returns the <i>canonical URL</i>. This URL is lowercase, with trailing slash and no index.htm*. The query
     * parameters are sorted alphabetically in ascending order, fragments (i.e. "anchor" parts) are removed.
     * </p>
     * 
     * @param url
     * @return canonical URL, or empty String if URL cannot be determined, never <code>null</code>
     * 
     */
    public static String getCanonicalUrl(String url) {

        if (url == null) {
            return "";
        }

        try {

            URL urlObj = new URL(url);

            // get all url parts
            String protocol = urlObj.getProtocol();
            String port = "";
            if (urlObj.getPort() != -1 && urlObj.getPort() != urlObj.getDefaultPort()) {
                port = ":" + urlObj.getPort();
            }
            String host = urlObj.getHost().toLowerCase();
            String path = urlObj.getPath();
            String[] query = null;
            if (urlObj.getQuery() != null) {
                query = urlObj.getQuery().split("&");

                // sort query parts alphabetically
                Arrays.sort(query);
            }


            // correct path to eliminate ".." and recreate path accordingly
            String[] parts = path.split("/");
            path = "/";

            if (parts.length > 0) {
                for (int i = 0; i < parts.length; i++) {
                    parts[i] = parts[i].trim();
                    // throw away ".." and a directory above it
                    if (parts[i].equals("..")) {
                        parts[i] = "";
                        // if there is a directory above this one in the path
                        if (parts.length > 1 && i > 0) {
                            parts[i - 1] = "";
                        }
                    }
                }
                for (String part : parts) {
                    if (part.length() > 0) {
                        path += part + "/";
                    }
                }

                // delete trailing slash if path ends with a file
                if (parts[parts.length - 1].contains(".")) {
                    path = path.substring(0, path.length() - 1);
                }
                // delete index.* if there is no query
                if (parts[parts.length - 1].contains("index") && query == null) {
                    path = path.replaceAll("index\\..+$", "");
                }

            }

            String queryPart = query != null ? "?" + StringUtils.join(query, "&") : "";
            return protocol + "://" + port + host + path + queryPart;

        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine canonical url for {}", url);
            return "";
        }

    }

    /**
     * <p>
     * Decode a String which was used as URL parameter.
     * </p>
     * 
     * @param string
     * @return
     */
    public static String decodeParameter(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unsupported. This should not happen.", e);
        }
    }

    /**
     * <p>
     * Encode a String to be used as URL parameter.
     * </p>
     * 
     * @param string
     * @return
     */
    public static String encodeParameter(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unsupported. This should not happen.", e);
        }
    }

    /**
     * <p>
     * Extracts all recognizable URLs from a given text.
     * </p>
     * 
     * @param text
     * @return List of extracted URLs, or empty List if no URLs were found, never <code>null</code>.
     */
    public static List<String> extractUrls(String text) {
        return StringHelper.getRegexpMatches(URL_PATTERN, text);
    }

    public static boolean isLocalFile(URL url) {
        String protocol = url.getProtocol();
        String host = url.getHost();

        boolean hasHost = host != null && !"".equals(host);

        return "file".equalsIgnoreCase(protocol) && !hasHost;
    }
}
