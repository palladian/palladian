package ws.palladian.retrieval.helper;

import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.log4j.Logger;

import ws.palladian.helper.date.dates.DateParser;
import ws.palladian.helper.date.dates.ExtractedDate;
import ws.palladian.retrieval.HttpResult;

/**
 * <p>
 * Some HTTP specific helper methods.
 * </p>
 * 
 * @author Sandro Reichert
 * @author Philipp Katz
 */
public final class HttpHelper {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(HttpHelper.class);

    private HttpHelper() {
        // utility class, no instances required.
    }

    /**
     * <p>
     * Get a date from http header. According to the HTTP specification [RFC2616, 14.18], the date must be in RFC 1123
     * date format. Since this is theory, we use a two step approach here to get a date from the header. First, we try
     * it by using {@link DateUtils} from Apache that finds dates in RFC 1123, RFC 1036 or ANSI C asctime() format. This
     * fails in many cases where providers send their own format. Therefore, one may use Palladian's sophisticated date
     * recognition here, which may be expensive.
     * </p>
     * 
     * @param httpResult The {@link HttpResult} to get the date from.
     * @param headerName The name of the header field to get.
     * @param strict If <code>true</code>, use {@link DateUtils} to get only dates according to the HTTP specification.
     *            If <code>false</code>, use Palladian's sophisticated date recognition here, which may be expensive.
     * @return The extracted date or <code>null</code> if the given header name is not present or the date is invalid.
     */
    public static final Date getDateFromHeader(HttpResult httpResult, String headerName, boolean strict) {
        String dateString = httpResult.getHeaderString(headerName);
        Date date = null;
        if (dateString != null && !dateString.isEmpty()) {
            try {
                date = DateUtils.parseDate(dateString);
            } catch (DateParseException e) {

                // ignore 0 and -1 values as they are commonly used as Expires and not worth mentioning.
                if (!dateString.equalsIgnoreCase("0") && !dateString.equalsIgnoreCase("-1")) {

                    // optionally detect via palladian's date recognition
                    if (!strict) {
                        ExtractedDate ed = DateParser.findDate(dateString);
                        if (ed != null) {
                            date = ed.getNormalizedDate();
                        }
                    }

                    if (date == null) {
                        LOGGER.error("Could not parse http header value for " + headerName + ": \"" + dateString
                                + "\". " + e.getMessage());
                    }
                }
            }
        }
        return date;
    }

    /**
     * <p>
     * Get the content of the supplied {@link HttpResult} as string. For conversion, the "Content-Type" HTTP header with
     * a specified charset is considered. If no default encoding is specified, <i>ISO-8859-1</i> is assumed.
     * </p>
     * 
     * @see <a href="http://www.w3.org/International/O-HTTP-charset.en.php">Setting the HTTP charset parameter</a>.
     * @param httpResult The HttpResult for which to get the content as string, not <code>null</code>.
     * @return The string value of the supplied HttpResult.
     */
    public static String getStringContent(HttpResult httpResult) {
        Validate.notNull(httpResult, "httpResult must not be null");

        String foundCharset = getCharset(httpResult);
        Charset charset;
        if (foundCharset != null && Charset.isSupported(foundCharset)) {
            charset = Charset.forName(foundCharset);
        } else {
            charset = Charset.forName("ISO-8859-1");
        }
        return new String(httpResult.getContent(), charset);
    }

    /**
     * <p>
     * Retrieve the encoding from the supplied {@link HttpResult}, if it is specified in the "Content-Type" HTTP header.
     * </p>
     * 
     * @param httpResult The HttpResult for which to determine the encoding, not <code>null</code>.
     * @return The encoding of the HttpResult, nor <code>null</code> if no encoding was specified explicitly.
     */
    public static String getCharset(HttpResult httpResult) {
        Validate.notNull(httpResult, "httpResult must not be null");

        String ret = null;
        List<String> contentTypeValues = httpResult.getHeader("Content-Type");
        if (contentTypeValues != null) {
            for (String contentTypeValue : contentTypeValues) {
                int index = contentTypeValue.indexOf("charset=");
                if (index != -1) {
                    ret = contentTypeValue.substring(index + "charset=".length(), contentTypeValue.length());
                    break;
                }
            }
        }
        return ret;
    }

    public static void main(String[] args) {

        // DateUtils.parseDate fails here since it is not RFC 1123, RFC 1036 or ANSI C asctime()
        String dateString = "Thu, 22 Jul 2010 15:15:59GMT";
        // dateString = "Fri 08 Jul 2011 05:08:54 PM GMT GMT";
        // dateString = "Fri, Jul 08 2011 16:50:05 GMT";
        // dateString = "GMT";

        Date date = null;
        try {
            date = DateUtils.parseDate(dateString);
        } catch (DateParseException e) {
            // ignore 0 and -1 values as they are commonly used as Expires and not worth mentioning.
            if (!dateString.equalsIgnoreCase("0") && !dateString.equalsIgnoreCase("-1")) {
            }

            ExtractedDate ed = DateParser.findDate(dateString);
            if (ed != null) {
                date = ed.getNormalizedDate();
            }
        }

        System.out.println(dateString + "\n" + (date == null ? "null" : date.toGMTString()));

    }

}
