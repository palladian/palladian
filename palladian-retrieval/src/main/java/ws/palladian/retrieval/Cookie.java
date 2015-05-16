package ws.palladian.retrieval;

/**
 * A HTTP cookie.
 * 
 * @author pk
 */
public interface Cookie {

    /**
     * @return The name/key of this cookie, not <code>null</code>.
     */
    String getName();

    /**
     * @return The value of this cookie, not <code>null</code>.
     */
    String getValue();

    /**
     * @return The domain of this cookie, not <code>null</code>.
     */
    String getDomain();

    /**
     * @return The path for which this cookie is valid, or <code>null</code> in case of no restriction.
     */
    String getPath();

}
