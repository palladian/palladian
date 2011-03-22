package ws.palladian.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.StopWatch;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.ConnectionHook;

/**
 * This class is responsible for maintaining connections to the database. The BoneCP library is used as a connection
 * pool.
 * 
 * @author Philipp Katz
 * @see http://jolbox.com/
 * 
 */
/* package */class ConnectionManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ConnectionManager.class);

    /** The pool for the connections. */
    private BoneCP connectionPool;

    /** The private constructor. */
    private ConnectionManager() {
        setup();
    }

    /** Hold the lazy initialized singleton instance. */
    private static class SingletonHolder {
        private static final ConnectionManager INSTANCE = new ConnectionManager();
    }

    /**
     * Get {@link ConnectionManager} singleton instance.
     * 
     * @return
     */
    public static final ConnectionManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    /**
     * Setup the connection pool, read configuration from properties file.
     */
    private void setup() {
        
        // The configuration file can be found under config/palladian.properties.
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();

        String driver = "";
        String jdbcUrl = "";
        String username = "";
        String password = "";

        if (config == null) {
            LOGGER.warn("could not load configuration, use defaults: jdbc:mysql://localhost:3306/tudiirdb?useServerPrepStmts=false&cachePrepStmts=false");

            driver = "com.mysql.jdbc.Driver";
            jdbcUrl = "jdbc:mysql://localhost:3306/tudiirdb?useServerPrepStmts=false&cachePrepStmts=false";
            username = "root";
            password = "";
        } else {
            driver = config.getString("db.driver");
            jdbcUrl = config.getString("db.jdbcUrl");
            username = config.getString("db.username");
            password = config.getString("db.password");
        }

        if (config != null) {
            config.setThrowExceptionOnMissing(true);
        }

        connect(driver, jdbcUrl, username, password);
    }

    /**
     * Connect to a database with the given settings.
     * 
     * @param driver The database driver, for example "com.mysql.jdbc.Driver"
     * @param jdbcUrl The URL to create the JDBC connection, for example
     *            "jdbc:mysql://localhost:3306/tudiirdb?useServerPrepStmts=false&cachePrepStmts=false"
     * @param username The username to the given database.
     * @param password The password belonging to the username.
     */
    public void connect(String driver, String jdbcUrl, String username, String password) {

        StopWatch sw = new StopWatch();

        try {
            // load the database driver (make sure this is in your classpath!)
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            LOGGER.error("error loading database driver : " + driver);
            throw new RuntimeException("error loading database driver : " + driver, e);
        }

        try {

            // setup the connection pool
            BoneCPConfig boneConfig = new BoneCPConfig();
            
            boneConfig.setJdbcUrl(jdbcUrl);
            boneConfig.setUsername(username);
            boneConfig.setPassword(password);
            boneConfig.setMinConnectionsPerPartition(5);
            boneConfig.setMaxConnectionsPerPartition(10);
            boneConfig.setPartitionCount(1);
            
            // only enable this for debugging purposes!
            // boneConfig.setCloseConnectionWatch(true);
            
            // BoneCP is reluctant/lazy to change auto-commit state. This means, that logic connections which are handed
            // back to the pool and which auto-commit state has been disabled and enabled again, might actually still be
            // in auto-commit = false, which has led to long hangs (java.sql.SQLException: Lock wait timeout exceeded;
            // try restarting transaction). This hook ensures, that all connections handed out by pool have their
            // auto-commit enabled.
            ConnectionHook connectionHook = new AbstractConnectionHook() {
                @Override
                public void onCheckOut(ConnectionHandle connection) {
                    try {
                        connection.setAutoCommit(true);
                    } catch (SQLException ignore) {
                    }
                }
            };
            boneConfig.setConnectionHook(connectionHook);

            if (connectionPool != null) {
                LOGGER.info("closing connection pool, total created connections: "
                        + connectionPool.getTotalCreatedConnections() + ", free: " + connectionPool.getTotalFree());
                connectionPool.close();
                LOGGER.info("connection pool closed, total created connections: "
                        + connectionPool.getTotalCreatedConnections() + ", free: " + connectionPool.getTotalFree());
            }

            connectionPool = new BoneCP(boneConfig);
            
            LOGGER.debug("initialized the connection pool in " + sw.getElapsedTimeString());

        } catch (SQLException e) {
            LOGGER.error("error setting up connection pool : " + e.getMessage());
            throw new RuntimeException("error setting up connection pool", e);
        }

    }

    /**
     * Get a connection.
     * 
     * @return
     * @throws SQLException
     */
    public Connection getConnection() throws SQLException {
        Connection connection = connectionPool.getConnection();
        assert connection.getAutoCommit() == true;
        // check if logging is enabled before creating the log output;
        // this is saves time, es this method might be called millions of times.
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("get pool connection; created:" + connectionPool.getTotalCreatedConnections() + " free:"
                    + connectionPool.getTotalFree() + " used:" + connectionPool.getTotalLeased());
        }
        return connection;
    }

    public static void main(String[] args) throws Exception {

        ConnectionManager.getInstance();

        /*
         * final Random random = new Random();
         * for (int i = 0; i < 1000; i++) {
         * new Thread() {
         * public void run() {
         * try {
         * Connection connection = ConnectionManager.getInstance().getConnection();
         * // pretend to do something with the connection
         * sleep(random.nextInt(10000));
         * connection.close();
         * } catch (SQLException e) {
         * LOGGER.error(e);
         * } catch (InterruptedException e) {
         * LOGGER.error(e);
         * }
         * };
         * }.start();
         * }
         */
    }

}
