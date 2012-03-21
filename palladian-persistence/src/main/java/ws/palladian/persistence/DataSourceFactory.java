package ws.palladian.persistence;

import java.sql.SQLException;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;
import com.jolbox.bonecp.ConnectionHandle;
import com.jolbox.bonecp.hooks.AbstractConnectionHook;
import com.jolbox.bonecp.hooks.ConnectionHook;

/**
 * <p>
 * This class acts as a factory for {@link DataSource}s maintained by BoneCP connection pool.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://jolbox.com/
 * 
 */
final class DataSourceFactory {
    
    private DataSourceFactory() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Create a {@link DataSource} with the specified connection parameters.
     * </p>
     * 
     * @param driver The database driver, for example "com.mysql.jdbc.Driver"
     * @param jdbcUrl The URL to create the JDBC connection, for example
     *            "jdbc:mysql://localhost:3306/tudiirdb?useServerPrepStmts=false&cachePrepStmts=false"
     * @param username The username to the given database.
     * @param password The password belonging to the username.
     * @return The {@link DataSource} for the parameters.
     */
    public static DataSource createDataSource(String driver, String jdbcUrl, String username, String password) {

        try {
            // load the database driver (make sure this is in your classpath!)
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("error loading database driver : " + driver, e);
        }

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
                } catch (SQLException e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        boneConfig.setConnectionHook(connectionHook);
        return new BoneCPDataSource(boneConfig);
    }

}
