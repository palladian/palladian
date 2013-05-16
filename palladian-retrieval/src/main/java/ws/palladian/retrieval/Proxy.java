package ws.palladian.retrieval;

/**
 * <p>
 * A web proxy, optinally with authentication information.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface Proxy {

    /**
     * @return The address of this proxy, not <code>null</code> or empty.
     */
    String getAddress();

    /**
     * @return The port of this proxy, in the range of 1 to 65535.
     */
    int getPort();

    /**
     * @return The username for authentication, or <code>null</code> if proxy requires no authentication.
     */
    String getUsername();

    /**
     * @return The password for authentication, or <code>null</code> if proxy requires no authentication.
     */
    String getPassword();

}
