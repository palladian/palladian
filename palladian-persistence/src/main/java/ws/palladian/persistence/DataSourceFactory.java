package ws.palladian.persistence;

import javax.sql.DataSource;

/**
 * <p>
 * Factory producing a {@link DataSource}.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface DataSourceFactory {

    /**
     * <p>
     * Create a {@link DataSource} with the specified connection parameters.
     * </p>
     * 
     * @param jdbcUrl The URL to create the JDBC connection, for example
     *            "jdbc:mysql://localhost:3306/tudiirdb?useServerPrepStmts=false&cachePrepStmts=false"
     * @param username The username for accessing the given database.
     * @param password The password belonging to the username.
     * @return The {@link DataSource} for the parameters.
     */
    DataSource createDataSource(String jdbcUrl, String username, String password);

}
