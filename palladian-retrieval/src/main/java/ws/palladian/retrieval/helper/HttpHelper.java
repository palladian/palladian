package ws.palladian.retrieval.helper;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateParser;
import ws.palladian.helper.date.ExtractedDate;
import ws.palladian.helper.io.FileHelper;
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
    public static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);

    /** Separator between HTTP header and content payload when writing HTTP results to file. */
    private static final String HTTP_RESULT_SEPARATOR = "\n----------------- End Headers -----------------\n\n";

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

    /**
     * <p>
     * Download the content from a given URL and save it to a specified path. Can be used to download binary files.
     * </p>
     * 
     * @param httpResult The httpResult to save.
     * @param filePath the path where the downloaded contents should be saved to; if file name ends with ".gz", the file
     *            is compressed automatically.
     * @param includeHttpResponseHeaders whether to prepend the received HTTP headers for the request to the saved
     *            content.
     * @return <tt>true</tt> if everything worked properly, <tt>false</tt> otherwise.
     */
    public static boolean saveToFile(HttpResult httpResult, String filePath, boolean includeHttpResponseHeaders) {

        boolean result = false;
        boolean compress = filePath.endsWith(".gz") || filePath.endsWith(".gzip");
        OutputStream out = null;

        try {
            FileHelper.createDirectoriesAndFile(filePath);

            out = new BufferedOutputStream(new FileOutputStream(filePath));

            if (compress) {
                out = new GZIPOutputStream(out);
            }

            if (includeHttpResponseHeaders) {

                StringBuilder headerBuilder = new StringBuilder();
                headerBuilder.append("Status Code").append(":");
                headerBuilder.append(httpResult.getStatusCode()).append("\n");

                Map<String, List<String>> headers = httpResult.getHeaders();

                for (Entry<String, List<String>> headerField : headers.entrySet()) {
                    headerBuilder.append(headerField.getKey()).append(":");
                    headerBuilder.append(StringUtils.join(headerField.getValue(), ","));
                    headerBuilder.append("\n");
                }

                headerBuilder.append(HTTP_RESULT_SEPARATOR);
                out.write(headerBuilder.toString().getBytes("UTF-8"));
            }

            out.write(httpResult.getContent());
            result = true;

        } catch (IOException e) {
            LOGGER.error("Error while saving to {}", filePath, e);
        } finally {
            FileHelper.close(out);
        }

        return result;

    }
    
    /**
     * <p>
     * Load a HttpResult from a dataset file and return a {@link HttpResult}. If the file is gzipped (ending with
     * <code>.gz</code> or <code>.gzip</code>), it is decompressed automatically.
     * </p>
     * 
     * @param file
     * @return The {@link HttpResult} from file or <code>null</code> on in case an {@link IOException} was caught.
     */
    // TODO should this be extended to handle files without the written header?
    public static HttpResult loadSerializedHttpResult(File file) {

        HttpResult httpResult = null;
        InputStream inputStream = null;

        try {
            // we don't know this anymore
            String url = "from_file_system";
            Map<String, List<String>> headers = new HashMap<String, List<String>>();

            // we don't know this anymore
            long transferedBytes = -1;

            // Wrap this with a GZIPInputStream, if necessary.
            // Do not use InputStreamReader, as this works encoding specific.
            inputStream = new FileInputStream(file);
            if (file.getName().endsWith(".gz") || file.getName().endsWith(".gzip")) {
                inputStream = new GZIPInputStream(inputStream);
            }
            // inputStream = new GZIPInputStream(new FileInputStream(file));

            // Read the header information, until the HTTP_RESULT_SEPARATOR is reached.
            // We assume here, that one byte resembles one character, which is not true
            // in general, but should suffice in our case. Hopefully.
            StringBuilder headerText = new StringBuilder();
            int b;
            while ((b = inputStream.read()) != -1) {
                headerText.append((char)b);
                if (headerText.toString().endsWith(HTTP_RESULT_SEPARATOR)) {
                    break;
                }
            }
            int statusCode = parseHeaders(headerText.toString(), headers);

            // Read the payload.
            ByteArrayOutputStream payload = new ByteArrayOutputStream();
            while ((b = inputStream.read()) != -1) {
                payload.write(b);
            }
            byte[] content = payload.toByteArray();
            httpResult = new HttpResult(url, content, headers, statusCode, transferedBytes);

        } catch (FileNotFoundException e) {
            LOGGER.error("File not found: {}", file, e);
        } catch (IOException e) {
            LOGGER.error("IOException for: {}", file, e);
        } finally {
            FileHelper.close(inputStream);
        }

        return httpResult;
    }
    
    /**
     * <p>
     * Extract header information from the supplied string. The header data is put in the Map, the HTTP status code is
     * returned.
     * </p>
     * 
     * @param headerText newline separated HTTP header text.
     * @param headers out-parameter for parsed HTTP headers.
     * @return the HTTP status code.
     */
    private static int parseHeaders(String headerText, Map<String, List<String>> headers) {
        String[] headerLines = headerText.split("\n");
        int statusCode = -1;
        for (String headerLine : headerLines) {
            String[] parts = headerLine.split(":");
            if (parts.length > 1) {
                if (parts[0].equalsIgnoreCase("status code")) {
                    try {
                        String statusCodeString = parts[1];
                        statusCodeString = statusCodeString.replace("HTTP/1.1", "");
                        statusCodeString = statusCodeString.replace("OK", "");
                        statusCodeString = statusCodeString.trim();
                        statusCode = Integer.valueOf(statusCodeString);
                    } catch (Exception e) {
                        LOGGER.error("Exception while parsing header", e);
                    }
                } else {

                    StringBuilder valueString = new StringBuilder();
                    for (int i = 1; i < parts.length; i++) {
                        valueString.append(parts[i]).append(":");
                    }
                    String valueStringClean = valueString.toString();
                    if (valueStringClean.endsWith(":")) {
                        valueStringClean = valueStringClean.substring(0, valueStringClean.length() - 1);
                    }

                    List<String> values = new ArrayList<String>();

                    // in cases we have a "=" we can split on comma
                    if (valueStringClean.contains("=")) {
                        String[] valueParts = valueStringClean.split(",");
                        for (String valuePart : valueParts) {
                            values.add(valuePart.trim());
                        }
                    } else {
                        values.add(valueStringClean);
                    }

                    headers.put(parts[0], values);
                }
            }
        }
        return statusCode;
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
