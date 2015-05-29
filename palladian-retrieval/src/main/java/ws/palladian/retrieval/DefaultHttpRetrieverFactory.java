package ws.palladian.retrieval;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.conn.PoolingClientConnectionManager;

import ws.palladian.helper.functional.Factory;

public class DefaultHttpRetrieverFactory implements Factory<HttpRetriever> {

    /** The default number of connections in the connection pool. */
    public static final int DEFAULT_NUM_CONNECTIONS = 100;

    /** The default number of connections per route. */
    public static final int DEFAULT_NUM_CONNECTIONS_PER_ROUTE = 10;

    /** Connection manager from Apache HttpComponents; thread safe and responsible for connection pooling. */
    private final PoolingClientConnectionManager connectionManager;

    public DefaultHttpRetrieverFactory() {
        this(false);
    }

    public DefaultHttpRetrieverFactory(boolean acceptSelfSignedCerts) {
        this(DEFAULT_NUM_CONNECTIONS, DEFAULT_NUM_CONNECTIONS_PER_ROUTE, acceptSelfSignedCerts);
    }

    public DefaultHttpRetrieverFactory(int numConnections, int numConnectionsPerRoute, boolean acceptSelfSignedCerts) {
        SchemeRegistry registry = new SchemeRegistry();
        registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
        if (acceptSelfSignedCerts) {
            try {
                // consider self-signed certificates as trusted; this is generally not a good idea,
                // however we use the HttpRetriever basically only for web scraping and data extraction,
                // therefore we may argue that it's okayish. At least do not point your finger at me
                // for doing so!
                SSLSocketFactory socketFactory = new SSLSocketFactory(new TrustSelfSignedStrategy(),
                        SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
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

}
