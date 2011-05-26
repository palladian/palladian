/**
 * Created on: 24.05.2011 23:45:04
 */
package ws.palladian.persistence;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;

/**
 * The central Factory for all {@link DatabaseManager} instances. This is the only way to get valid {@code
 * DatabaseManager} instances. The Factory is initialized as Singleton to get access from everywhere. As always: use the
 * singleton with care to not populate all layers of your application if database access code.
 * 
 * The Factory is able to load new subclasses of {@code DatabaseManager} dynamically if they are on the class path.
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 */
public final class DatabaseManagerFactory {
    /**
     * 
     */
    private final Map<String, ConnectionManager> connectionManagerRegistry;

    /**
     * The only instance of this manager for the current class loader. This is necessary to keep a central registry for
     * {@link ConnectionManager}s so that each database connection is represented by only one {@code ConnectionManager}.
     * The singleton is initialized directly to make it thread safe.
     */
    private static final DatabaseManagerFactory INSTANCE = new DatabaseManagerFactory();

    /**
     * @return
     */
    public static DatabaseManagerFactory getInstance() {
        return INSTANCE;
    }

    /**
     * 
     */
    private DatabaseManagerFactory() {
        super();
        connectionManagerRegistry = new ConcurrentHashMap<String, ConnectionManager>();
    }

    /**
     * @param managerClassName
     * @return
     */
    public DatabaseManager create(final String managerClassName) {
        // The configuration file can be found under config/palladian.properties.
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        final String driver = config.getString("db.driver");
        final String jdbcUrl = config.getString("db.jdbcUrl");
        final String username = config.getString("db.username");
        String password = config.getString("db.password");

        if (driver == null || jdbcUrl == null || username == null) {
            throw new IllegalStateException("Database properties are missing in Palladian properties.");
        }
        if (password == null) {
            password = "";
        }

        return create(managerClassName, driver, jdbcUrl, username, password);
    }

    /**
     * @param managerClassName
     * @param jdbcDriverClassName
     * @param jdbcConnectionURL
     * @param username
     * @return
     */
    public DatabaseManager create(final String managerClassName, final String jdbcDriverClassName,
            final String jdbcConnectionURL, final String username) {
        return create(managerClassName, jdbcDriverClassName, jdbcConnectionURL, username, "");
    }

    /**
     * @param managerClassName
     * @param jdbcDriverClassName
     * @param jdbcConnectionURL
     * @param username
     * @param password
     * @return
     */
    @SuppressWarnings("unchecked")
    public DatabaseManager create(final String managerClassName, final String jdbcDriverClassName,
            final String jdbcConnectionUrl, final String username, final String password) {
        try {
            final Class<DatabaseManager> databaseManagerClass = (Class<DatabaseManager>) Class
                    .forName(managerClassName);
            final Constructor<DatabaseManager> dbManagerConstructor = databaseManagerClass
                    .getConstructor(ConnectionManager.class);
            final DatabaseManager ret = dbManagerConstructor.newInstance(getConnectionManager(jdbcDriverClassName,
                    jdbcConnectionUrl, username, password));
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate DatabaseManager", e);
        }

    }

    /**
     * @return
     */
    private synchronized Object getConnectionManager(final String jdbcDriverClassName, final String jdbcConnectionUrl,
            final String username, final String password) {
        ConnectionManager connectionManager = connectionManagerRegistry.get(jdbcConnectionUrl);
        if (connectionManager == null) {
            connectionManager = new ConnectionManager(jdbcDriverClassName, jdbcConnectionUrl, username, password);
            connectionManagerRegistry.put(jdbcConnectionUrl, connectionManager);
        }
        return connectionManager;
    }
}
