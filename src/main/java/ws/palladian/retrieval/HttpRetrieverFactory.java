package ws.palladian.retrieval;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;

/**
 * <p>
 * Factory for creating {@link HttpRetriever} instances. The factory can be customized by creating a subclass and
 * setting it via {@link #setFactory(HttpRetrieverFactory)}. This way, new instances can be customized as needed. Per
 * default, the following settings in Palladian's configuration file (see {@link ConfigHolder}) are considered when
 * creating new {@link HttpRetriever} instances:
 * </p>
 * <ul>
 * <li>documentRetriever.connectionTimeout</li>
 * <li>documentRetriever.socketTimeout</li>
 * <li>documentRetriever.numRetries</li>
 * <li>documentRetriever.numConnections</li>
 * </ul>
 * 
 * @author Philipp Katz
 */
public abstract class HttpRetrieverFactory {

    private static HttpRetrieverFactory _factory = new HttpRetrieverFactory() {
        @Override
        protected HttpRetriever createHttpRetriever() {
            HttpRetriever httpRetriever = new HttpRetriever();

            // setup the configuration; if no config available, use default values
            PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
            httpRetriever.setConnectionTimeout(config.getLong("documentRetriever.connectionTimeout",
                    HttpRetriever.DEFAULT_CONNECTION_TIMEOUT));
            httpRetriever.setSocketTimeout(config.getLong("documentRetriever.socketTimeout",
                    HttpRetriever.DEFAULT_SOCKET_TIMEOUT));
            httpRetriever.setNumRetries(config
                    .getInt("documentRetriever.numRetries", HttpRetriever.DEFAULT_NUM_RETRIES));
            httpRetriever.setNumConnections(config.getInt("documentRetriever.numConnections",
                    HttpRetriever.DEFAULT_NUM_CONNECTIONS));

            return httpRetriever;

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
