package ws.palladian.helper;

import java.util.Date;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.log4j.Logger;

import ws.palladian.retrieval.HttpResult;

/**
 * Some HTTP specific helper methods
 * 
 * @author Sandro Reichert
 * 
 */
public class HTTPHelper {

    /** The logger for this class. */
    public static final Logger LOGGER = Logger.getLogger(HTTPHelper.class);

    /**
     * Get a date from http header. We use {@link DateUtils}, so only dates in RFC 1123, RFC 1036 or ANSI C asctime()
     * format will be detected. We do not use palladians date recognition here.
     * 
     * @param httpResult The {@link HttpResult} to get the date from.
     * @param headerName The name of the header field to get.
     * @return The extracted date or <code>null</code> if the given header name is not present or the date is invalid.
     */
    public static final Date getDateFromHeader(HttpResult httpResult, String headerName) {
        String dateString = httpResult.getHeaderString(headerName);
        Date date = null;
        if (dateString != null && !dateString.isEmpty()) {
            try {
                date = DateUtils.parseDate(dateString);
            } catch (DateParseException e) {
                LOGGER.error("Could not parse http header value for " + headerName + ": \"" + dateString + "\". "
                        + e.getMessage());
            }
        }
        return date;
    }

}
