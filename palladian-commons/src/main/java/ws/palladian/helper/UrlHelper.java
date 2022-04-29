package ws.palladian.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import ws.palladian.helper.collection.StringLengthComparator;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;
import ws.palladian.helper.nlp.PatternHelper;
import ws.palladian.helper.nlp.StringHelper;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    /**
     * The logger for this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);

    /**
     * Names of attributes in (X)HTML containing links.
     */
    private static final List<String> LINK_ATTRIBUTES = Arrays.asList("href", "src");

    /**
     * RegEx pattern defining a session ID.
     */
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("[&;]?(?<!\\w)(jsessionid=|s=|sid=|PHPSESSID=|sessionid=)[A-Za-z_0-9\\-]{12,200}(?!\\w)");

    /**
     * List of top level domains.
     */
    private static final String TOP_LEVEL_DOMAINS;

    /**
     * List of possible domain suffixes.
     */
    private static final List<String> DOMAIN_SUFFIXES;

    private static final Pattern URL_PARAM = Pattern.compile("\\?.*");

    static {
        InputStream resourceAsStream = UrlHelper.class.getResourceAsStream("/top-level-domains.txt");
        final List<String> tlds = new ArrayList<>();
        FileHelper.performActionOnEveryLine(resourceAsStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String lineString = line.trim();
                // ignore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    tlds.add(lineString.substring(1));
                }
            }
        });
        Collections.sort(tlds, StringLengthComparator.INSTANCE);
        TOP_LEVEL_DOMAINS = StringUtils.join(tlds, "|");

        resourceAsStream = UrlHelper.class.getResourceAsStream("/second-level-domains.txt");
        final List<String> slds = new ArrayList<>();
        FileHelper.performActionOnEveryLine(resourceAsStream, new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String lineString = line.trim();
                // ignore comments and empty lines ...
                if (!lineString.startsWith("#") && !lineString.isEmpty()) {
                    slds.add(lineString);
                }
            }
        });
        slds.sort(StringLengthComparator.INSTANCE);

        DOMAIN_SUFFIXES = slds;
        String[] split = TOP_LEVEL_DOMAINS.split("\\|");
        for (String tld : split) {
            DOMAIN_SUFFIXES.add("." + tld);
        }
    }

    // adapted version from <http://daringfireball.net/2010/07/improved_regex_for_matching_urls>
    // this is able to match URLs, containing (brackets), but does not include trailing brackets
    public static final Pattern URL_PATTERN = Pattern.compile("\\b(?:https?://)?([0-9a-zäöü-]{1,63}?\\.)+(?:" + TOP_LEVEL_DOMAINS
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
     * <code>null</code> in case the original URL was <code>null</code>.
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
                    LOGGER.trace("{} -> {}", value, fullValue);
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
     * @param document The document.
     * @return The base URL, if present, <code>null</code> otherwise.
     */
    public static String getBaseUrl(Document document) {
        Node baseNode = XPathHelper.getXhtmlNode(document, "//head/base/@href");
        return baseNode != null ? baseNode.getTextContent() : null;
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
     * @param pageUrl actual URL of the document, can be <code>null</code>.
     * @param baseUrl base URL defined in document's header, can be <code>null</code> if no base URL is specified.
     * @param linkUrl link URL from the document to be made absolute, not <code>null</code>.
     * @return the absolute URL, empty String, if URL cannot be created, never <code>null</code>.
     * @author Philipp Katz
     * @see <a href="http://www.mediaevent.de/xhtml/base.html">HTML base • Basis-Adresse einer Webseite</a>
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
                if (linkUrl.startsWith("?")) {
                    result = URL_PARAM.matcher(contextUrl).replaceAll("") + linkUrl;
                } else {
                    result = new URL(new URL(contextUrl), linkUrl).toString();
                    if (linkUrl.startsWith(".")) {
                        result = result.replace("../", "");
                        result = result.replace("./", "");
                    } else if (linkUrl.contains("/../")) {
                        int pathJumps = StringHelper.countOccurrences(linkUrl, "../");
                        for (int i = 0; i < pathJumps; i++) {
                            Pattern pathPattern = PatternHelper.compileOrGet("[^/]+/../");
                            Matcher matcher = pathPattern.matcher(result);
                            if (matcher.find()) {
                                result = matcher.replaceAll("");
                            }
                        }
                    }
                }
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
     * @param url             The url.
     * @param includeProtocol include protocol prefix, e.g. "http://"
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url, boolean includeProtocol, boolean includeSubdomain) {
        String result = "";
        try {
            URL urlObj = new URL(url);
            String host = urlObj.getHost();
            if (!host.isEmpty()) {
                if (includeProtocol) {
                    result = urlObj.getProtocol() + "://";
                }
                result += urlObj.getHost();

                if (!includeSubdomain) {
                    String suffix = "";
                    for (String domainSuffix : DOMAIN_SUFFIXES) {
                        if (result.endsWith(domainSuffix)) {
                            suffix = domainSuffix;
                            break;
                        }
                    }
                    result = result.substring(0, result.length() - suffix.length());
                    String[] parts = result.split("\\.");
                    result = parts[parts.length - 1] + suffix;
                }

                LOGGER.trace("root url for {} -> {}", url, result);
            } else {
                LOGGER.trace("no domain specified {}", url);
            }
        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine domain for {}", url);
        }
        return result;
    }

    public static String getDomain(String url, boolean includeProtocol) {
        return getDomain(url, includeProtocol, true);
    }

    /**
     * <p>
     * Return the root/domain URL. For example: <code>http://www.example.com/page.html</code> is converted to
     * <code>http://www.example.com</code>.
     * </p>
     *
     * @param url The URL.
     * @return root URL, or empty String if URL cannot be determined, never <code>null</code>
     */
    public static String getDomain(String url) {
        return getDomain(url, true, true);
    }

    /**
     * <p>
     * Returns the <i>canonical URL</i>. This URL is lowercase, with trailing slash and no index.htm*. The query
     * parameters are sorted alphabetically in ascending order, fragments (i.e. "anchor" parts) are removed.
     * </p>
     *
     * @param url The URL.
     * @return canonical URL, or empty String if URL cannot be determined, never <code>null</code>
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
     * @param string The string to be decoded.
     * @return The decoded string.
     */
    public static String decodeParameter(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException("UTF-8 encoding unsupported. This should not happen.", e);
        }
    }

    private static String tryDecodeParameter(String string) {
        try {
            return decodeParameter(string);
        } catch (IllegalArgumentException e) {
            return string;
        }
    }

    /**
     * <p>
     * Encode a String to be used as URL parameter.
     * </p>
     *
     * @param string The string to be encoded.
     * @return The encoded string.
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
     * @param text The text from which URLs should be extracted.
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

    /**
     * <p>
     * Creates an encoded key-value parameter string, which can e.g. be appended to a URL.
     *
     * @param parameters Map with key-value params, not <code>null</code>.
     * @return The key-value string.
     */
    public static String createParameterString(List<Pair<String, String>> parameters) {
        Validate.notNull(parameters, "parameters must not be null");
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Pair<String, String> pair : parameters) {
            if (first) {
                first = false;
            } else {
                builder.append('&');
            }
            builder.append(encodeParameter(pair.getKey()));
            String value = pair.getValue();
            if (value != null) {
                builder.append('=');
                builder.append(encodeParameter(value));
            }
        }
        return builder.toString();
    }

    /**
     * <p>
     * Parses an encoded key-value string, which can e.g. be present as a query string appended to a URL.
     *
     * @param parameterString The key-value parameter string.
     * @return A list with parsed params.
     */
    public static List<Pair<String, String>> parseParams(String parameterString) {
        Validate.notNull(parameterString, "parameterString must not be null");
        List<Pair<String, String>> params = new ArrayList<>();

        int questionIdx = parameterString.indexOf("?");
        if (questionIdx == -1) { // no parameters in URL
            return params;
        }

        // ignore everything behind #
        int hashIdx = parameterString.indexOf("#");
        int endIdx = hashIdx != -1 ? hashIdx : parameterString.length();

        String paramSubString = parameterString.substring(questionIdx + 1, endIdx);
        String[] paramSplit = paramSubString.split("&");
        for (String param : paramSplit) {
            String[] keyValue = param.split("=");
            String key = tryDecodeParameter(keyValue[0]);
            String value;
            if (keyValue.length == 1 && param.contains("=")) {
                value = StringUtils.EMPTY;
            } else if (keyValue.length == 1) {
                value = null;
            } else {
                value = tryDecodeParameter(param.substring(key.length() + 1));
            }
            params.add(Pair.of(key, value));
        }
        return params;
    }

    /**
     * <p>
     * Get the base URL from the given URL (i.e. removing all query and hash params).
     *
     * @param url The URL, not <code>null</code>.
     * @return The base URL.
     */
    public static String parseBaseUrl(String url) {
        Validate.notNull(url, "url must not be null");
        int questionIdx = url.indexOf("?");
        int cutIdx = url.indexOf("#");
        if (questionIdx != -1) {
            cutIdx = questionIdx;
        }
        return cutIdx != -1 ? url.substring(0, cutIdx) : url;
    }

    public static boolean isValidUrl(String url) {
        String[] schemes = {"http", "https"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        return urlValidator.isValid(url);
    }
}
