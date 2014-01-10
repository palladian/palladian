package ws.palladian.persistence;

import javax.sql.DataSource;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

/**
 * <p>
 * This class acts as a factory for {@link DataSource}s maintained by BoneCP connection pool.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://jolbox.com/">BoneCP</p>.
 */
public final class BoneCpDataSourceFactory implements DataSourceFactory {
    
    public static final DataSourceFactory INSTANCE = new BoneCpDataSourceFactory();

    private BoneCpDataSourceFactory() {
        // singleton.
    }

    /* (non-Javadoc)
     * @see ws.palladian.persistence.DataSourceFactory#createDataSource(java.lang.String, java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public DataSource createDataSource(String jdbcUrl, String username, String password) {

//        try {
//            // load the database driver (make sure this is in your classpath!)
//            Class.forName(driver);
//        } catch (ClassNotFoundException e) {
//            throw new IllegalStateException("error loading database driver : " + driver, e);
//        }

        // setup the connection pool
        BoneCPConfig boneConfig = new BoneCPConfig();

        boneConfig.setJdbcUrl(jdbcUrl);
        boneConfig.setUsername(username);
        boneConfig.setPassword(password);
        boneConfig.setMinConnectionsPerPartition(5);
        boneConfig.setMaxConnectionsPerPartition(10);

        // recommended 3-4 depending on app
        boneConfig.setPartitionCount(3);

        // only enable this for debugging purposes!
        // boneConfig.setCloseConnectionWatch(true);

        // BoneCP is reluctant/lazy to change auto-commit state. This means, that logic connections which are handed
        // back to the pool and which auto-commit state has been disabled and enabled again, might actually still be
        // in auto-commit = false, which has led to long hangs (java.sql.SQLException: Lock wait timeout exceeded;
        // try restarting transaction). This hook ensures, that all connections handed out by pool have their
        // auto-commit enabled.
        // ConnectionHook connectionHook = new AbstractConnectionHook() {
        // @Override
        // public void onCheckOut(ConnectionHandle connection) {
        // try {
        // connection.setAutoCommit(true);
        // } catch (SQLException e) {
        // throw new IllegalStateException(e);
        // }
        // }
        // };
        // boneConfig.setConnectionHook(connectionHook);

        return new BoneCPDataSource(boneConfig);
    }

}
