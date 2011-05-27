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
 * The central Factory for all {@link DatabaseManager} instances. This is the
 * only way to get valid {@code DatabaseManager} instances. The Factory is
 * initialized as Singleton to get access from everywhere. As always: use the
 * singleton with care to not populate all layers of your application if
 * database access code.
 * 
 * The Factory is able to load new subclasses of {@code DatabaseManager}
 * dynamically if they are on the class path.
 * 
 * FIXME: fill out these fucking empty javadocs
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @author David Urbansky
 */
public final class DatabaseManagerFactory {
	/**
     * 
     */
	private final Map<String, ConnectionManager> connectionManagerRegistry;

	/**
	 * The only instance of this manager for the current class loader. This is
	 * necessary to keep a central registry for {@link ConnectionManager}s so
	 * that each database connection is represented by only one
	 * {@code ConnectionManager}. The singleton is initialized directly to make
	 * it thread safe.
	 */
	private static final DatabaseManagerFactory INSTANCE = new DatabaseManagerFactory();

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

	public DatabaseManager create(String managerClassName) {
		// The configuration file can be found under
		// config/palladian.properties.
		PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

		String driver = config.getString("db.driver");
		String jdbcUrl = config.getString("db.jdbcUrl");
		String username = config.getString("db.username");
		String password = config.getString("db.password");

		if (driver == null || jdbcUrl == null || username == null) {
			throw new IllegalStateException(
					"Database properties are missing in Palladian properties.");
		}
		if (password == null) {
			password = "";
		}

		return create(managerClassName, driver, jdbcUrl, username, password);
	}

	/**
	 * Create a DatabaseManager with configurations from a config file. The
	 * configuration file must at least provide the following fields:
	 * <ul>
	 * <li>db.driver</li>
	 * <li>db.jdbcUrl</li>
	 * <li>db.username</li>
	 * <li>db.password</li>
	 * </ul>
	 * 
	 * @param managerClassName The fully qualified name of the DatabaseManager class.
	 * @param config The config file containing the four required fields.
	 * @return A configured DatabaseManager with access to a connection pool 
	 */
	public DatabaseManager create(String managerClassName,
			PropertiesConfiguration config) {

		String driver = config.getString("db.driver");
		String jdbcUrl = config.getString("db.jdbcUrl");
		String username = config.getString("db.username");
		String password = config.getString("db.password");

		if (driver == null || jdbcUrl == null || username == null) {
			throw new IllegalStateException(
					"Database properties are missing in Palladian properties.");
		}
		if (password == null) {
			password = "";
		}

		return create(managerClassName, driver, jdbcUrl, username, password);
	}

	public DatabaseManager create(String managerClassName,String jdbcDriverClassName, String jdbcConnectionURL,String username) {
		return create(managerClassName, jdbcDriverClassName, jdbcConnectionURL,username, "");
	}

	@SuppressWarnings("unchecked")
	public DatabaseManager create(String managerClassName,String jdbcDriverClassName, String jdbcConnectionUrl,
			String username, String password) {
		try {
			Class<DatabaseManager> databaseManagerClass = (Class<DatabaseManager>) Class
					.forName(managerClassName);
			Constructor<DatabaseManager> dbManagerConstructor = databaseManagerClass
					.getDeclaredConstructor(ConnectionManager.class);
			dbManagerConstructor.setAccessible(true);
			DatabaseManager ret = dbManagerConstructor
					.newInstance(getConnectionManager(jdbcDriverClassName,
							jdbcConnectionUrl, username, password));
			return ret;
		} catch (Exception e) {
			throw new IllegalArgumentException(
					"Unable to instantiate DatabaseManager", e);
		}

	}

	private synchronized Object getConnectionManager(
			String jdbcDriverClassName, String jdbcConnectionUrl,
			String username, String password) {
		ConnectionManager connectionManager = connectionManagerRegistry
				.get(jdbcConnectionUrl);
		if (connectionManager == null) {
			connectionManager = new ConnectionManager(jdbcDriverClassName,
					jdbcConnectionUrl, username, password);
			connectionManagerRegistry.put(jdbcConnectionUrl, connectionManager);
		}
		return connectionManager;
	}
}
