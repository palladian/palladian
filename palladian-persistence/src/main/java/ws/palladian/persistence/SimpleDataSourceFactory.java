package ws.palladian.persistence;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Very simple {@link DataSourceFactory}, no pooling.
 * 
 * @author pk
 */
public final class SimpleDataSourceFactory implements DataSourceFactory {

    public static final SimpleDataSourceFactory INSTANCE = new SimpleDataSourceFactory();

    private static final class SimpleDataSource implements DataSource {
        private final String jdbcUrl;
        private final String password;
        private final String username;
        private int loginTimeout;
        private PrintWriter logWriter;

        private SimpleDataSource(String jdbcUrl, String password, String username) {
            this.jdbcUrl = jdbcUrl;
            this.password = password;
            this.username = username;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) throws SQLException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setLoginTimeout(int seconds) throws SQLException {
            this.loginTimeout = seconds;
        }

        @Override
        public void setLogWriter(PrintWriter out) throws SQLException {
            this.logWriter = out;
        }

        @Override
        public int getLoginTimeout() throws SQLException {
            return loginTimeout;
        }

        @Override
        public PrintWriter getLogWriter() throws SQLException {
            return logWriter;
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            LOGGER.trace(">getConnection({}, ...)", username);
            Properties connectionProps = new Properties();
            connectionProps.put("user", username);
            connectionProps.put("password", password);
            return DriverManager.getConnection(jdbcUrl, connectionProps);
        }

        @Override
        public Connection getConnection() throws SQLException {
            return getConnection(username, password);
        }

        @SuppressWarnings("unused")
        // no @Override; method was added in JDBC 4.1; allow to compile on old and new versions
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            throw new SQLFeatureNotSupportedException();
        }

    }

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleDataSourceFactory.class);

    private SimpleDataSourceFactory() {
        // singleton
    }

    @Override
    public DataSource createDataSource(String jdbcUrl, String username, String password) {
        return new SimpleDataSource(jdbcUrl, password, username);
    }

}
