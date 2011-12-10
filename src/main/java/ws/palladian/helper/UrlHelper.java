package ws.palladian.helper;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.validator.UrlValidator;
import org.apache.log4j.Logger;

import ws.palladian.retrieval.DocumentRetriever;

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
public class UrlHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(UrlHelper.class);

    /** A format string must not be preceded by a word character [a-zA-Z0-9] */
    private static final String START_PATTERN = "(?<!\\w)";

    /** A format string must not be followed by a word character [a-zA-Z0-9] */
    private static final String STOP_PATTERN = "(?!\\w)";

    /** Identifiers that are typically used for sessionIDs */
    private static final String[] SESSIONID_IDENTIFIER_PATTERN = { "jsessionid=", "s=", "sid=", "PHPSESSID=",
            "sessionid=" };

    private static final String SESSIONID_PATTERN = "[a-f0-9]{32}";

    /** The compiled pattern for all sessionIDs. */
    private static Pattern sessionIDPattern;

    private UrlHelper() {

    }

    /**
     * Compiles the {@link #sessionIDPattern} pattern. Pattern should look like
     * (?<!\w)(jsessionid=|s=|sid=|PHPSESSID=|sessionid=)[a-f0-9]{32}(?!\w)
     */
    private static void compilePattern() {
        StringBuilder formatPatternBuilder = new StringBuilder();
        formatPatternBuilder.append(START_PATTERN).append("(");
        for (String identifiers : SESSIONID_IDENTIFIER_PATTERN) {
            formatPatternBuilder.append(identifiers).append("|");
        }
        formatPatternBuilder.deleteCharAt(formatPatternBuilder.length() - 1);
        formatPatternBuilder.append(")");
        formatPatternBuilder.append(SESSIONID_PATTERN);
        formatPatternBuilder.append(STOP_PATTERN);
        LOGGER.debug(formatPatternBuilder.toString());
        sessionIDPattern = Pattern.compile(formatPatternBuilder.toString(), Pattern.CASE_INSENSITIVE);
    }

    /**
     * Tries to remove a session ID from URL if it can be found.
     * 
     * @param original The URL to remove the sessionID from.
     * @return The URL without the sessionID if it could be found or the original URL else wise. <code>null</code> if
     *         original was <code>null</code>.
     */
    public static URL removeSessionId(URL original) {
        URL replacedURL = original;
        if (original != null) {
            String origURL = original.toString();
            compilePattern();
            Matcher matcher = sessionIDPattern.matcher(origURL);
            String sessionID = null;
            String newURL = null;
            while (matcher.find()) {
                sessionID = matcher.group();
                LOGGER.debug("   sessionID : " + sessionID);
                newURL = origURL.replaceAll(sessionIDPattern.toString(), "");
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Original URL: " + origURL);
                LOGGER.debug("Cleaned URL : " + newURL);
            }
            if (newURL != null) {
                try {
                    replacedURL = new URL(newURL);
                } catch (MalformedURLException e) {
                    LOGGER.error("Could not replace sessionID in URL \"" + origURL + "\", returning original value.");
                }
            }
        }
        return replacedURL;
    }

    /**
     * Convenience method to remove a sessionID from a url string if it can be found.
     * 
     * @param originalUrl The URL to remove the sessionID from.
     * @param silent If <code>true</code>, do not log errors.
     * @return The string representation of the url without the sessionID if it could be found or the original string
     *         else wise. <code>null</code> if original was <code>null</code>.
     */
    public static String removeSessionId(String originalUrl, boolean silent) {
        String replacedURL = originalUrl;
        if (originalUrl != null) {
            try {
                replacedURL = removeSessionId(new URL(originalUrl)).toString();
            } catch (MalformedURLException e) {
                if (!silent) {
                    LOGGER.error("Could not create URL from \"" + originalUrl + "\". " + e.getLocalizedMessage());
                }
            }
        }
        return replacedURL;
    }

    // public static void main(String[] args) {
    // String news = UrlHelper
    // .removeSessionID("http://brbb.freeforums.org/viewforum.php?f=3&amp;sid=5c2676a9f621ffbadb6962da7e0c50d4");
    // System.out.println(news);
    // }

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
    public static String makeFullUrl(String pageUrl, String baseUrl, String linkUrl) {
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
                    LOGGER.trace("makeFullURL: pageUrl: " + e.getMessage());
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
                LOGGER.trace("makeFullURL: baseUrl: " + e.getMessage());
            }
            // create URL object considering linkUrl, relative to pageUrl+baseUrl
            try {
                resultUrl = new URL(resultUrl, linkUrl);
            } catch (MalformedURLException e) {
                LOGGER.trace("makeFullURL: linkUrl: " + e.getMessage());
            }
            if (resultUrl != null) {
                result = resultUrl.toString();
            }
        }
        // LOGGER.trace("<makeFullURL " + result);
        return result;
    }

    public static String makeFullUrl(String pageUrl, String linkUrl) {
        return makeFullUrl(pageUrl, null, linkUrl);
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
     * Check if an URL is in a valid form and the file-ending is not blacklisted (see Extractor.java for blacklist)
     * 
     * @param url the URL
     * @param checkHTTPResp the check http resp
     * @return true, if is a valid URL
     * @author Martin Werner
     */
    public static boolean isValidUrl(String url) {

        boolean returnValue = false;

        String[] schemes = { "http", "https" };
        UrlValidator urlValidator = new UrlValidator(schemes, UrlValidator.ALLOW_2_SLASHES);

        if (urlValidator.isValid(url)) {
            returnValue = true;
        }

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
     * @param pageUrl the pageURL
     * @return the verified URL
     */
    public static String verifyUrl(final String urlCandidate, final String pageUrl) {

        String returnValue = "";

        final String modUrlCandidate = urlCandidate.trim();
        if (modUrlCandidate.startsWith("http://")) {
            if (isValidUrl(modUrlCandidate)) {
                returnValue = modUrlCandidate;
            }
        } else {

            if (modUrlCandidate.length() > 2) {
                final String modifiedURL = makeFullUrl(pageUrl, modUrlCandidate);
                if (isValidUrl(modifiedURL)) {
                    returnValue = modifiedURL;
                }
            }

        }
        return returnValue;
    }

    /**
     * Returns the canonical URL. This URL is lowercase, with trailing slash and
     * no index.htm* and is the redirected URL if input URL is redirecting.
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

            // get redirect url if it exists and continue with this url
            DocumentRetriever dr = new DocumentRetriever();
            String redirectUrl = dr.getRedirectUrl(url);

            if (isValidUrl(redirectUrl))
                url = redirectUrl;

            URL urlObj = new URL(url);

            // get all url parts
            String protocol = urlObj.getProtocol();
            String port = "";
            if (urlObj.getPort() != -1 && urlObj.getPort() != urlObj.getDefaultPort())
                port = ":" + urlObj.getPort();
            String host = urlObj.getHost().toLowerCase();
            String path = urlObj.getPath();
            String query = "";
            if (urlObj.getQuery() != null)
                query = "?" + urlObj.getQuery();

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
                for (int i = 0; i < parts.length; i++)
                    if (parts[i].length() > 0)
                        path += parts[i] + "/";

                // delete trailing slash if path ends with a file
                if (parts[parts.length - 1].contains("."))
                    path = path.substring(0, path.length() - 1);
                // delete index.* if there is no query
                if (parts[parts.length - 1].contains("index") && query.isEmpty())
                    if (query.isEmpty())
                        path = path.replaceAll("index\\..+$", "");

            }

            return protocol + "://" + port + host + path + query;

        } catch (MalformedURLException e) {
            LOGGER.trace("could not determine canonical url for" + url);
            return "";
        }

    }

    /**
     * URLDecode a String.
     * 
     * @param string
     * @return
     */
    public static String urlDecode(String string) {
        String result;
        try {
            result = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // this will not happen, as we always use UTF-8 as encoding
            throw new IllegalStateException("houston, we have a problem");
        }
        return result;
    }

    /**
     * URLEncode a String.
     * 
     * @param string
     * @return
     */
    public static String urlEncode(String string) {
        String result;
        try {
            result = URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // this will not happen, as we always use UTF-8 as encoding
            throw new IllegalStateException("houston, we have a problem");
        }
        return result;
    }

    /**
     * Extract URLs from a given text. The used RegEx is very liberal, for example it will extract URLs with/without
     * protocol, mailto: links, etc. The result are the URLs, directly from the supplied text. There is no further post
     * processing of the extracted URLs.
     * 
     * The RegEx was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls
     * and alternative one can be found on http://flanders.co.nz/2009/11/08/a-good-url-regular-expression-repost/
     * 
     * @param text
     * @return List of extracted URLs, or empty List if no URLs were found, never <code>null</code>.
     */
    public static List<String> extractUrls(String text) {
        List<String> urls = new ArrayList<String>();
        Pattern p = Pattern
        // .compile("\\b(?:(?:ht|f)tp(?:s?)\\:\\/\\/|~\\/|\\/)?(?:\\w+:\\w+@)?(?:(?:[-\\w]+\\.)+(?:com|org|net|gov|mil|biz|info|mobi|name|aero|jobs|museum|travel|[a-z]{2}))(?::[\\d]{1,5})?(?:(?:(?:\\/(?:[-\\w~!$+|.,=]|%[a-f\\d]{2})+)+|\\/)+|\\?|#)?(?:(?:\\?(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)(?:&(?:[-\\w~!$+|.,*:]|%[a-f\\d{2}])+=?(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)*)*(?:#(?:[-\\w~!$+|.,*:=]|%[a-f\\d]{2})*)?\\b");
                .compile("(?i)\\b((?:[a-z][\\w-]+:(?:/{1,3}|[a-z0-9%])|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?«»“”‘’]))");

        Matcher m = p.matcher(text);
        while (m.find()) {
            urls.add(m.group());
        }
        return urls;
    }
    
    public static boolean isLocalFile(URL url) {
        String protocol = url.getProtocol();
        String host = url.getHost();
        
        boolean hasHost = host != null && !"".equals(host);
        
        return "file".equalsIgnoreCase(protocol) && !hasHost;
    }
}
