package ws.palladian.retrieval;

import java.io.InputStream;

/**
 * An HTTP entity; this represents content sent during HTTP requests (typically POST, PUT).
 *
 * @author Philipp Katz
 */
public interface HttpEntity {

    /**
     * @return The length of this entity in bytes.
     */
    long length();

    /**
     * @return The stream representing this entity.
     */
    InputStream getInputStream();

    /**
     * @return The content type of this entity.
     */
    String getContentType();

}
