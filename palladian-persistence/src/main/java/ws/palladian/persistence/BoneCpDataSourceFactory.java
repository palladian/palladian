package ws.palladian.persistence;

import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.BoneCPDataSource;

import javax.sql.DataSource;

/**
 * <p>
 * This class acts as a factory for {@link DataSource}s maintained by BoneCP connection pool.
 * </p>
 *
 * @author Philipp Katz
 * @see <a href="https://jolbox.com/">BoneCP</p>.
 * @deprecated This is currently still the default unless overridden in {@link DatabaseManagerFactory#dataSourceFactory}.
 * It's deprecated in favor of HikariCP; see {@link HikariCpDataSourceFactory}.
 */
@Deprecated
public final class BoneCpDataSourceFactory implements DataSourceFactory {
    public static final DataSourceFactory INSTANCE = new BoneCpDataSourceFactory();

    private BoneCpDataSourceFactory() {
        // singleton.
    }

    @Override
    public DataSource createDataSource(String jdbcUrl, String username, String password) {
        BoneCPConfig boneConfig = new BoneCPConfig();
        boneConfig.setJdbcUrl(jdbcUrl);
        boneConfig.setUsername(username);
        boneConfig.setPassword(password);
        boneConfig.setMinConnectionsPerPartition(5);
        boneConfig.setMaxConnectionsPerPartition(10);

        // The PalladianDatabaseManager guarantees, that connections are closed (in case, one uses the standard
        // functionality. And those fools, who don't, need punishment anyways). On the other hand, this functionality
        // has caused me some trouble since updating to version 0.8, so I disable it here completely (which should
        // slightly improve performance, too).
        boneConfig.setDisableConnectionTracking(true);

        boneConfig.setPartitionCount(3);
        return new BoneCPDataSource(boneConfig);
    }
}
