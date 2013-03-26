package ws.palladian.retrieval;

public interface Proxy {

    String getAddress();

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
