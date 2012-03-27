package ws.palladian.retrieval;


/**
 * <p>
 * Factory for creating {@link HttpRetriever} instances. The factory can be customized by creating a subclass and
 * setting it via {@link #setFactory(HttpRetrieverFactory)}. This way, new instances can be customized as needed.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class HttpRetrieverFactory {

    private static HttpRetrieverFactory _factory = new HttpRetrieverFactory() {
        @Override
        protected HttpRetriever createHttpRetriever() {
            return new HttpRetriever();
        }
    };

    /**
     * <p>
     * Override, to customize the {@link HttpRetriever} instances which are created.
     * </p>
     * 
     * @return
     */
    protected abstract HttpRetriever createHttpRetriever();

    /**
     * <p>
     * Obtain an instance of {@link HttpRetriever}.
     * </p>
     * 
     * @return
     */
    public static HttpRetriever getHttpRetriever() {
        return _factory.createHttpRetriever();
    }

    /**
     * <p>
     * Set the factory implementation to use when creating new {@link HttpRetriever} instances.
     * </p>
     * 
     * @param factory
     */
    public static void setFactory(HttpRetrieverFactory factory) {
        _factory = factory;
    }

}
