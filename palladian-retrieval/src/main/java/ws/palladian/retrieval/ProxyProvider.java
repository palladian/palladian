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

        @Override
        public void removeProxy(Proxy proxy) {
        }

        @Override
        public void promoteProxy(Proxy proxy) {
        }

        @Override
        public void removeProxy(Proxy proxy, int statusCode) {
        }

        @Override
        public void removeProxy(Proxy proxy, Throwable error) {
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

    /**
     * <p>
     * Tell the proxy provider to remove a proxy (e.g. because of malfunction).
     * </p>
     * 
     * @param proxy The proxy to remove.
     * @param statusCode The status code that was returned when using the proxy.
     */
    void removeProxy(Proxy proxy, int statusCode);

    void removeProxy(Proxy proxy, Throwable error);
    void removeProxy(Proxy proxy);

    /**
     * <p>
     * Tell the proxy provider that the given proxy worked.
     * </p>
     * 
     * @param proxy The proxy to promote.
     */
    void promoteProxy(Proxy proxy);
}