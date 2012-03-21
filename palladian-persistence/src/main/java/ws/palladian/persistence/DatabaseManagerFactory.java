/**
 * Created on: 24.05.2011 23:45:04
 */
package ws.palladian.persistence;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.configuration.PropertiesConfiguration;

import ws.palladian.helper.ConfigHolder;

/**
 * <p>
 * The central Factory for all {@link DatabaseManager} instances. This is the only way to get valid
 * {@code DatabaseManager} instances. The Factory is initialized as Singleton to get access from everywhere. As always:
 * use the singleton with care to not populate all layers of your application with database access code.
 * </p>
 * 
 * <p>
 * The Factory is able to load new subclasses of {@code DatabaseManager} dynamically if they are on the class path.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class DatabaseManagerFactory {

    private final static Map<String, DataSource> dataSourceRegistry = new ConcurrentHashMap<String, DataSource>();

    private DatabaseManagerFactory() {
        super();
    }

    /**
     * <p>
     * Create a DatabaseManager with the configuration obtained from the Palladian configuration file. See
     * {@link ConfigHolder} for a documentation about the location of this configuration file. The configuration file
     * must supply the following fields:
     * </p>
     * <ul>
     * <li>db.driver</li>
     * <li>db.jdbcUrl</li>
     * <li>db.username</li>
     * <li>db.password</li>
     * </ul>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The fully qualified name of the DatabaseManager class.
     * @return A configured DatabaseManager with access to a connection pool
     * @deprecated Use one of the create methods which explicitly requires to supply a configuration.
     */
    @Deprecated
    public static <D extends DatabaseManager> D create(Class<D> managerClass) {
        // The configuration file can be found under
        // config/palladian.properties.
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        return create(managerClass, config);
    }

    /**
     * <p>
     * Create a DatabaseManager with configurations from a {@link PropertiesConfiguration}. The PropertiesConfiguration
     * must at least provide the following fields:
     * </p>
     * <ul>
     * <li>db.driver</li>
     * <li>db.jdbcUrl</li>
     * <li>db.username</li>
     * <li>db.password</li>
     * </ul>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClassName The fully qualified name of the DatabaseManager class.
     * @param config The PropertiesConfiguration containing the four required fields.
     * @return A configured DatabaseManager with access to a connection pool
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, PropertiesConfiguration config) {

        String driver = config.getString("db.driver");
        String jdbcUrl = config.getString("db.jdbcUrl");
        String username = config.getString("db.username");
        String password = config.getString("db.password");

        if (driver == null || jdbcUrl == null || username == null) {
            throw new IllegalStateException("Database properties are missing in the supplied PropertiesConfiguration.");
        }
        if (password == null) {
            password = "";
        }

        return create(managerClass, driver, jdbcUrl, username, password);
    }

    /**
     * <p>
     * Create a DatabaseManager with the supplied configuration.
     * </p>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The fully qualified name of the DatabaseManager class.
     * @param jdbcDriverClassName The fully qualified name of the JDBC driver class.
     * @param jdbcConnectionUrl The JDBC connection URL.
     * @param username The username for accessing the database.
     * @return A configured DatabaseManager with access to a connection pool
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, String jdbcDriverClassName,
            String jdbcConnectionURL, String username) {
        return create(managerClass, jdbcDriverClassName, jdbcConnectionURL, username, "");
    }

    /**
     * <p>
     * Create a DatabaseManager with the supplied configuration.
     * </p>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The fully qualified name of the DatabaseManager class.
     * @param jdbcDriverClassName The fully qualified name of the JDBC driver class.
     * @param jdbcConnectionUrl The JDBC connection URL.
     * @param username The username for accessing the database.
     * @param password The password for accessing the database.
     * @return A configured DatabaseManager with access to a connection pool
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, String jdbcDriverClassName,
            String jdbcConnectionUrl, String username, String password) {
        try {
            Constructor<D> dbManagerConstructor = managerClass.getDeclaredConstructor(DataSource.class);
            dbManagerConstructor.setAccessible(true);
            D ret = dbManagerConstructor.newInstance(getDataSource(jdbcDriverClassName, jdbcConnectionUrl, username,
                    password));
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate DatabaseManager", e);
        }

    }

    private static synchronized DataSource getDataSource(String jdbcDriverClassName, String jdbcConnectionUrl,
            String username, String password) {
        DataSource dataSource = dataSourceRegistry.get(jdbcConnectionUrl);
        if (dataSource == null) {
            dataSource = DataSourceFactory.createDataSource(jdbcDriverClassName, jdbcConnectionUrl, username, password);
            dataSourceRegistry.put(jdbcConnectionUrl, dataSource);
        }
        return dataSource;
    }
}
