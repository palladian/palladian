package ws.palladian.retrieval;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Factory;


/**
 * <p>
 * Factory for creating {@link HttpRetriever} instances. Can be customized via {@link #setFactory(Factory)}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class HttpRetrieverFactory {

    private static Factory<HttpRetriever> _factory = new Factory<HttpRetriever>() {
        @Override
        public HttpRetriever create() {
            return new HttpRetriever();
        }
    };

    /**
     * <p>
     * Obtain an instance of {@link HttpRetriever}.
     * </p>
     * 
     * @return
     */
    public static HttpRetriever getHttpRetriever() {
        return _factory.create();
    }

    /**
     * <p>
     * Set the factory implementation to use when creating new {@link HttpRetriever} instances.
     * </p>
     * 
     * @param factory The factory, not <code>null</code>.
     */
    public static void setFactory(Factory<HttpRetriever> factory) {
        Validate.notNull(factory, "factory must not be null");
        _factory = factory;
    }
    
    private HttpRetrieverFactory() {
        // no instances.
    }

}
