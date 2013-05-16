package ws.palladian.retrieval;

/**
 * <p>
 * Implementations of this interface provide {@link Proxy} instances based on a given URL. This way, proxies can be
 * assigned conditionally, e.g. use proxies for external traffic, but no proxies for internal traffic, etc.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface ProxyProvider {

    /**
     * <p>
     * Default (nop) proxy provider.
     * </p>
     */
    public static final ProxyProvider DEFAULT = new ProxyProvider() {
        @Override
        public Proxy getProxy(String url) throws HttpException {
            return null;
        }
    };

    /**
     * <p>
     * Get a proxy for the specified URL.
     * </p>
     * 
     * @param url The URL for which to get the proxy.
     * @return The proxy, or <code>null</code> if no proxy exists/necessary for the given URL.
     * @throws HttpException In case of any error.
     */
    Proxy getProxy(String url) throws HttpException;

}
