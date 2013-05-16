package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Simple, immutable proxy implementation.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class ImmutableProxy implements Proxy {

    private final String address;
    private final int port;
    private final String username;
    private final String password;

    /**
     * <p>
     * Create a new {@link ImmutableProxy}.
     * </p>
     * 
     * @param address The address, not empty or <code>null</code>.
     * @param port The port, in the range of 1 to 65535.
     * @param username The username for authentication, or <code>null</code> if proxy requires no authentication.
     * @param password The password for authentication, or <code>null</code> if proxy requires no authentication.
     */
    public ImmutableProxy(String address, int port, String username, String password) {
        Validate.notEmpty(address, "address must not be empty");
        Validate.inclusiveBetween(1, 65535, port);

        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     * <p>
     * Create a new {@link ImmutableProxy} without authentication.
     * </p>
     * 
     * @param address The address, not empty or <code>null</code>.
     * @param port The port, in the range of 1 to 65535.
     */
    public ImmutableProxy(String address, int port) {
        this(address, port, null, null);
    }

    @Override
    public String getAddress() {
        return address;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ImmutableProxy [address=");
        builder.append(address);
        builder.append(", port=");
        builder.append(port);
        builder.append(", username=");
        builder.append(username);
        builder.append(", password=");
        builder.append(password);
        builder.append("]");
        return builder.toString();
    }

}
