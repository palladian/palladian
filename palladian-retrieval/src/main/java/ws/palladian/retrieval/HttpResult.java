package ws.palladian.retrieval;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ws.palladian.helper.collection.CaseInsensitiveMap;

/**
 * <p>
 * Represents a response for an HTTP request, e.g. GET or HEAD.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public class HttpResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final String HEADER_SEPARATOR = "; ";

    private final String url;
    private final byte[] content;
    private final Map<String, List<String>> headers;
    private final int statusCode;
    private final long transferedBytes;

    /**
     * <p>
     * Instantiate a new {@link HttpResult}.
     * </p>
     * 
     * @param url the result's URL.
     * @param content the content as byte array; empty byte array for response without content (e.g. HEAD).
     * @param headers the HTTP headers.
     * @param statusCode the HTTP response code.
     * @param transferedBytes the number of transfered bytes.
     */
    public HttpResult(String url, byte[] content, Map<String, List<String>> headers, int statusCode,
            long transferedBytes) {
        super();
        this.url = url;
        this.content = content;
        // this.headers = headers;
        
        // field names of the header are case-insensitive: http://www.ietf.org/rfc/rfc2616.txt
        // Each header field consists of a name followed by a colon (":") and the field value. Field names are case-insensitive.
        this.headers = new CaseInsensitiveMap<List<String>>(headers);
        this.statusCode = statusCode;
        this.transferedBytes = transferedBytes;
    }

    /**
     * @return the result's URL.
     */
    public String getUrl() {
        return url;
    }

    /**
     * <p>
     * Get this {@link HttpResult}'s content as byte array. For requests returning Strings, you may use
     * {@link #getStringContent()} to convert considering the correct encoding. The usage of
     * <code>new String({@link HttpResult#getContent()})</code> is discouraged, as the sytem's default encoding is used.
     * </p>
     * 
     * @return the content as byte array; empty byte array for response without content (e.g. HEAD).
     */
    public byte[] getContent() {
        return content;
    }

    /**
     * @return the HTTP headers.
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * <p>
     * Get the HTTP header for the specified name.
     * </p>
     * 
     * @param name the name of the HTTP header to get.
     * @return List of values, or <code>null</code> if no such header name.
     */
    public List<String> getHeader(String name) {
        return headers.get(name);
    }

    /**
     * <p>
     * Get the HTTP header for the specified name as String.
     * </p>
     * 
     * @param name the name of the HTTP header to get.
     * @return header value, or <code>null</code> if no such header name.
     */
    public String getHeaderString(String name) {
        return StringUtils.join(getHeader(name), HEADER_SEPARATOR);
    }

    /**
     * @return the HTTP response code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the number of transfered bytes.
     */
    public long getTransferedBytes() {
        return transferedBytes;
    }

    /**
     * <p>
     * Get the content of the supplied {@link HttpResult} as string. For conversion, the "Content-Type" HTTP header with
     * a specified charset is considered. If no default encoding is specified, <i>ISO-8859-1</i> is assumed.
     * </p>
     * 
     * @see <a href="http://www.w3.org/International/O-HTTP-charset.en.php">Setting the HTTP charset parameter</a>.
     * @return The string value of the supplied HttpResult.
     */
    public String getStringContent() {
        String foundCharset = getCharset();
        Charset charset;
        if (foundCharset != null && Charset.isSupported(foundCharset)) {
            charset = Charset.forName(foundCharset);
        } else {
            charset = Charset.forName("ISO-8859-1");
        }
        return new String(getContent(), charset);
    }

    /**
     * <p>
     * Retrieve the encoding from the supplied {@link HttpResult}, if it is specified in the "Content-Type" HTTP header.
     * </p>
     * 
     * @return The encoding of the HttpResult, nor <code>null</code> if no encoding was specified explicitly.
     */
    public String getCharset() {
        String ret = null;
        List<String> contentTypeValues = getHeader("Content-Type");
        if (contentTypeValues != null) {
            for (String contentTypeValue : contentTypeValues) {
                int index = contentTypeValue.indexOf("charset=");
                if (index != -1) {
                    ret = contentTypeValue.substring(index + "charset=".length(), contentTypeValue.length());
                    ret = ret.replace("\"", "");
                    break;
                }
            }
        }
        return ret;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("HttpResult [url=");
        builder.append(url);
        builder.append(", content=");
        builder.append(content.length);
        builder.append(" bytes");
        builder.append(", headers=");
        builder.append(headers);
        builder.append(", statusCode=");
        builder.append(statusCode);
        builder.append(", transferedBytes=");
        builder.append(transferedBytes);
        builder.append("]");
        return builder.toString();
    }

}
