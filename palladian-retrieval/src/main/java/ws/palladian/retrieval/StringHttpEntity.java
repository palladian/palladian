package ws.palladian.retrieval;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.Validate;

/**
 * {@link HttpEntity} which is provided as a {@link String}.
 * 
 * @author Philipp Katz
 */
public final class StringHttpEntity implements HttpEntity {

    private final String string;

    private final String contentType;

    /**
     * Create a new {@link StringHttpEntity}.
     * 
     * @param string The content, not <code>null</code>.
     * @param contentType The content type.
     */
    public StringHttpEntity(String string, String contentType) {
        Validate.notNull(string, "string must not be null");
        this.string = string;
        this.contentType = contentType;
    }

    @Override
    public long length() {
        return string.getBytes(StandardCharsets.UTF_8).length;
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(string.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("StringHttpEntity [content=");
        builder.append(string);
        if (contentType != null) {
            builder.append(", contentType=");
            builder.append(contentType);
        }
        builder.append("]");
        return builder.toString();
    }

}
