/**
 * Created on: 24.05.2011 23:45:04
 */
package ws.palladian.persistence;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManagerFactory.class);
    
    public static final String DB_CONFIG_FILE = "database.xml";

    private final static Map<String, DataSource> dataSourceRegistry = new ConcurrentHashMap<String, DataSource>();
    
    /** Specify the {@link DataSourceFactory} to use, if you need a custom one. */
    public static DataSourceFactory dataSourceFactory = BoneCpDataSourceFactory.INSTANCE;
    
    private static HierarchicalConfiguration configuration;

    /**
     * <p>
     * Obtain the database configuration; lazy loaded.
     * </p>
     * 
     * @return The database configuration.
     */
    private static HierarchicalConfiguration getConfig() {
        if (configuration == null) {
            try {
                File configFile = new File(DB_CONFIG_FILE);
                LOGGER.debug("Trying to load configuration from {}", configFile.getAbsolutePath());
                configuration = new XMLConfiguration(DB_CONFIG_FILE);
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Error loading the configuration file from \"" + DB_CONFIG_FILE + "\": "
                        + e.getMessage());
            }
        }
        return configuration;
    }

//    /**
//     * <p>
//     * Create a DatabaseManager with the configuration obtained from the Palladian configuration file. See
//     * {@link ConfigHolder} for a documentation about the location of this configuration file. The configuration file
//     * must supply the following fields:
//     * </p>
//     * <ul>
//     * <li>db.driver</li>
//     * <li>db.jdbcUrl</li>
//     * <li>db.username</li>
//     * <li>db.password</li>
//     * </ul>
//     * 
//     * @param <D> Type of the DataManager (sub)class to create.
//     * @param managerClass The fully qualified name of the DatabaseManager class.
//     * @return A configured DatabaseManager with access to a connection pool
//     * @deprecated Use one of the create methods which explicitly requires to supply a configuration.
//     */
//    @Deprecated
//    public static <D extends DatabaseManager> D create(Class<D> managerClass) {
//        // The configuration file can be found under
//        // config/palladian.properties.
//        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
//        return create(managerClass, config);
//    }
    
    /**
     * <p>
     * Create a DatabaseManager with the configuration obtained from a persistence configuration file (
     * {@value #DB_CONFIG_FILE}). This configuration file allows to configure several data sources and is structured as
     * follows:
     * </p>
     * 
     * <pre>
     * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
     * &lt;databases&gt;
     *     &lt;database&gt;
     *         &lt;name&gt;default&lt;/name&gt;
     *         &lt;driver&gt;com.mysql.jdbc.Driver&lt;/driver&gt;
     *         &lt;url&gt;jdbc:mysql://localhost:3306/myDatabase&lt;/url&gt;
     *         &lt;username&gt;root&lt;/username&gt;
     *         &lt;password&gt;topsecret&lt;/password&gt;
     *     &lt;/database&gt;
     *     […]
     * &lt;/databases&gt;
     * </pre>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The type of the DatabaseManager class.
     * @param persistenceName The name of persistence configuration provided by {@value #DB_CONFIG_FILE} and specified
     *            in <code>name</code> element (see example above).
     * @return A configured DatabaseManager with access to a connection pool.
     * @throws IllegalStateException In case the initialization fails.
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, String persistenceName) {
        Validate.notEmpty(persistenceName, "persistenceName must not be empty");

        HierarchicalConfiguration rootConfig = getConfig();
        List<HierarchicalConfiguration> dbConfigs = rootConfig.configurationsAt("database");
        for (HierarchicalConfiguration dbConfig : dbConfigs) {
            String name = dbConfig.getString("name");
            if (name.equals(persistenceName)) {
                String driver = dbConfig.getString("driver");
                String url = dbConfig.getString("url");
                String username = dbConfig.getString("username");
                String password = dbConfig.getString("password");
                return create(managerClass, driver, url, username, password);
            }
        }
        throw new IllegalStateException("The persistence configuration with the name \"" + persistenceName
                + "\" was not found.");
    }

    /**
     * <p>
     * Create a DatabaseManager with configurations from a {@link Configuration}. The Configuration
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
     * @param managerClass The type of the DatabaseManager class.
     * @param config The PropertiesConfiguration containing the four required fields.
     * @return A configured DatabaseManager with access to a connection pool.
     * @throws IllegalStateException In case the initialization fails.
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, Configuration config) {

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

//    /**
//     * <p>
//     * Create a DatabaseManager with the supplied configuration.
//     * </p>
//     * 
//     * @param <D> Type of the DataManager (sub)class to create.
//     * @param managerClass The type of the DatabaseManager class.
//     * @param jdbcDriverClassName The fully qualified name of the JDBC driver class.
//     * @param jdbcConnectionUrl The JDBC connection URL.
//     * @param username The user name for accessing the database.
//     * @return A configured DatabaseManager with access to a connection pool
//     * @throws IllegalStateException In case the initialization fails.
//     */
//    public static <D extends DatabaseManager> D create(Class<D> managerClass, String jdbcDriverClassName,
//            String jdbcConnectionUrl, String username) {
//        return create(managerClass, jdbcDriverClassName, jdbcConnectionUrl, username, "");
//    }

    /**
     * <p>
     * Create a DatabaseManager with the supplied configuration.
     * </p>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The type of the DatabaseManager class.
     * @param jdbcDriverClassName The fully qualified name of the JDBC driver class.
     * @param jdbcConnectionUrl The JDBC connection URL.
     * @param username The user name for accessing the database.
     * @param password The password for accessing the database.
     * @return A configured DatabaseManager with access to a connection pool
     * @throws IllegalStateException In case the initialization fails.
     * @deprecated It is not necessary any longer to specify the JDBC driver class, you can use
     *             {@link #create(Class, String, String, String)} instead
     */
    @Deprecated
    public static <D extends DatabaseManager> D create(Class<D> managerClass, String jdbcDriverClassName,
            String jdbcConnectionUrl, String username, String password) {
        try {
            Constructor<D> dbManagerConstructor = managerClass.getDeclaredConstructor(DataSource.class);
            dbManagerConstructor.setAccessible(true);
            D ret = dbManagerConstructor.newInstance(getDataSource(jdbcConnectionUrl, username, password));
            return ret;
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to instantiate DatabaseManager", e);
        }

    }

    /**
     * <p>
     * Create a DatabaseManager with the supplied configuration.
     * </p>
     * 
     * @param <D> Type of the DataManager (sub)class to create.
     * @param managerClass The type of the DatabaseManager class.
     * @param jdbcConnectionUrl The JDBC connection URL.
     * @param username The user name for accessing the database.
     * @param password The password for accessing the database.
     * @return A configured DatabaseManager with access to a connection pool
     * @throws IllegalStateException In case the initialization fails.
     */
    public static <D extends DatabaseManager> D create(Class<D> managerClass, String jdbcConnectionUrl,
            String username, String password) {
        return create(managerClass, null, jdbcConnectionUrl, username, password);
    }

    private static synchronized DataSource getDataSource(String jdbcConnectionUrl, String username, String password) {
        Validate.notEmpty(jdbcConnectionUrl, "jdbcConnectionUrl must not be empty");
        Validate.notEmpty(username, "username must not be empty");
        
        DataSource dataSource = dataSourceRegistry.get(jdbcConnectionUrl);
        if (dataSource == null) {
            dataSource = dataSourceFactory.createDataSource(jdbcConnectionUrl, username, password);
            dataSourceRegistry.put(jdbcConnectionUrl, dataSource);
        }
        return dataSource;
    }
}
