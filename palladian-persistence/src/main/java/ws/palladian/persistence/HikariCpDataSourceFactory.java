package ws.palladian.persistence;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/**
 * Connection pool based on
 * <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a>.
 *
 * @author Philipp Katz
 * @since 2.0
 */
public final class HikariCpDataSourceFactory implements DataSourceFactory {
    public static final DataSourceFactory INSTANCE = new HikariCpDataSourceFactory();

    private HikariCpDataSourceFactory() {
        // singleton.
    }

    @Override
    public DataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "300");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.setPoolName("palladian_db_pool");
        config.setMaximumPoolSize(20);
        return new HikariDataSource(config);
    }

}
