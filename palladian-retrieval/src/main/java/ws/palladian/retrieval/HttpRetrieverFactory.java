package ws.palladian.retrieval;

import java.io.Closeable;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.commons.lang3.Validate;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import ws.palladian.helper.functional.Factory;

/**
 * <p>
 * Factory for creating {@link HttpRetriever} instances. If you need to customize all {@link HttpRetriever} instances
 * created within the Palladian toolkit (e.g. also the ones which are used by the searcher classes), create an instance
 * of this class, and make it the default via {@link HttpRetrieverFactory#setFactory(Factory)}. Example (specify this in
 * an early entry point of your software):
 * 
 * <pre>
 * HttpRetrieverFactory factory = new HttpRetrieverFactory() {
 *     public HttpRetriever create() {
 *         HttpRetriever retriever = super.create();
 *         // further customization
 *         return retriever;
 *     };
 * };
 * HttpRetrieverFactory.setFactory(factory);
 * </pre>
 * 
 * <p>
 * Alternatively, you can create additional instances and use them separately. In this case, take care of calling the
 * {@link #close()} method, when you're done with one factory.
 * 
 * @author Philipp Katz
 */
public class HttpRetrieverFactory implements Factory<HttpRetriever>, Closeable {

    /** The default number of connections in the connection pool. */
    public static final int DEFAULT_NUM_CONNECTIONS = 100;

    /** The default number of connections per route. */
    public static final int DEFAULT_NUM_CONNECTIONS_PER_ROUTE = 10;

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private final PoolingClientConnectionManager connectionManager;

    private static Factory<HttpRetriever> _factory = new HttpRetrieverFactory();

    /**
     * Create a new instance with the default settings.
     */
    public HttpRetrieverFactory() {
        this(false);
    }

    /**
     * Create a new instance.
     * 
     * @param acceptSelfSignedCerts <code>true</code> to accept self-signed SSL certificates. <b>Attention:</b> You must
     *            have Internetführerschein advanced level before messing around with these functionalities.
     */
    public HttpRetrieverFactory(boolean acceptSelfSignedCerts) {
        this(DEFAULT_NUM_CONNECTIONS, DEFAULT_NUM_CONNECTIONS_PER_ROUTE, acceptSelfSignedCerts);
    }

    /**
     * Create a new instance.
     * 
     * @param numConnections The maximum number of simultaneous connections for the connection pool.
     * @param numConnectionsPerRoute The maximum number of simultaneous connections per route for the connection pool.
     * @param acceptSelfSignedCerts <code>true</code> to accept self-signed SSL certificates. <b>Attention:</b> You must
     *            have Internetführerschein advanced level before messing around with these functionalities.
     */
    public HttpRetrieverFactory(int numConnections, int numConnectionsPerRoute, boolean acceptSelfSignedCerts) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        if (acceptSelfSignedCerts) {
            try {
                // consider self-signed certificates as trusted; this is generally not a good idea,
                // however we use the HttpRetriever basically only for web scraping and data extraction,
                // therefore we may argue that it's okayish. At least do not point your finger at me
                // for doing so!
                SchemeSocketFactory socketFactory = new SSLSocketFactory(new TrustSelfSignedStrategy(),
                        SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
                registry.register(new Scheme("https", 443, socketFactory));
            } catch (NoSuchAlgorithmException | KeyManagementException | UnrecoverableKeyException | KeyStoreException e) {
                throw new IllegalStateException("Exception when creating SSLSocketFactory", e);
            }
        }
        connectionManager = new PoolingClientConnectionManager(registry);
        connectionManager.setMaxTotal(numConnections);
        connectionManager.setDefaultMaxPerRoute(numConnectionsPerRoute);
    }

    @Override
    public HttpRetriever create() {
        return new HttpRetriever(connectionManager);
    }

    /** Close the connection manager (relevant, in case a new instance of the factory is created.) */
    @Override
    public void close() {
        // System.out.println("shutting down connection manager");
        connectionManager.shutdown();
    }

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

}
